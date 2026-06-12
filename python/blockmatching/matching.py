"""Core block-matching search: for each fixed block, find the moving-image
offset (within the neighbourhood) that maximises the similarity score.

Faithful, vectorised port of the multi-threaded core loop in
BlockMatchingRegistration.runBlockMatching.

Key fidelity points
-------------------
* Fixed blocks are sampled on the integer grid (VitimageUtils.valuesOfBlock).
* Moving blocks are sampled with a constant sub-voxel offset per block, which is
  exactly trilinear interpolation at integer-spaced coordinates starting from the
  (possibly fractional) corner (VitimageUtils.valuesOfBlockDouble). We reproduce
  this with scipy.ndimage.map_coordinates(order=1, mode='nearest').
* The search visits offsets in the Java nested order (xPlus outer, yPlus, zPlus
  inner) and keeps the best with the Java update rule:
  update if (score > best) or (score == best and distance < best_distance),
  with best initialised to (-inf, distance 0).

Two Java bugs are reproducible via ``replicate_java_bugs`` (default False = fixed):
  1. the Y term of the tie-break distance uses stepFactorZ instead of stepFactorY;
  2. the moving point's Y coordinate uses yMax*stepFactorZ instead of *stepFactorY.
These only matter for anisotropic step factors (e.g. with no_subscale_z).
"""
import time as _time
import concurrent.futures as _cf
import numpy as np
from scipy.ndimage import map_coordinates

from .metrics import block_score, block_score_from_sums


def _block_template(block_size):
    """Voxel offsets (iz, iy, ix) for a block, in (z, x, y) Java flatten order."""
    bx, by, bz = block_size
    iz, ix, iy = np.meshgrid(np.arange(bz), np.arange(bx), np.arange(by), indexing="ij")
    return iz.ravel(), iy.ravel(), ix.ravel()


def extract_fixed_blocks(arr, corners, block_size):
    """Integer-grid extraction. corners: (N,3) [x0,y0,z0]. Returns (N, M) float64."""
    tz, ty, tx = _block_template(block_size)
    x0 = corners[:, 0].astype(np.intp)[:, None]
    y0 = corners[:, 1].astype(np.intp)[:, None]
    z0 = corners[:, 2].astype(np.intp)[:, None]
    Z, Y, X = arr.shape
    zz = np.clip(z0 + tz[None, :], 0, Z - 1)
    yy = np.clip(y0 + ty[None, :], 0, Y - 1)
    xx = np.clip(x0 + tx[None, :], 0, X - 1)
    return arr[zz, yy, xx].astype(np.float64, copy=False)


def extract_moving_blocks(arr, corners_f, block_size):
    """Trilinear extraction at fractional corners. corners_f: (N,3) [x0,y0,z0]."""
    tz, ty, tx = _block_template(block_size)
    Z = arr.shape[0]
    x = corners_f[:, 0][:, None] + tx[None, :]
    y = corners_f[:, 1][:, None] + ty[None, :]
    z = corners_f[:, 2][:, None] + tz[None, :]
    if Z == 1:
        z = np.zeros_like(z)
    coords = np.stack([z.ravel(), y.ravel(), x.ravel()], axis=0)
    vals = map_coordinates(arr, coords, order=1, mode="nearest", prefilter=False)
    return vals.reshape(corners_f.shape[0], -1).astype(np.float64, copy=False)


def _offsets(neighbourhood, stride_moving):
    """Offsets (xPlus, yPlus, zPlus) in the Java nested iteration order."""
    nx, ny, nz = neighbourhood
    xs = range(-nx * stride_moving, nx * stride_moving + 1, stride_moving)
    ys = range(-ny * stride_moving, ny * stride_moving + 1, stride_moving)
    zs = range(-nz * stride_moving, nz * stride_moving + 1, stride_moving)
    return [(x, y, z) for x in xs for y in ys for z in zs]


def _integral(a):
    """3D summed-area table with a zero border: S[z,y,x] = sum(a[:z,:y,:x])."""
    S = np.zeros((a.shape[0] + 1, a.shape[1] + 1, a.shape[2] + 1))
    S[1:, 1:, 1:] = a.cumsum(0).cumsum(1).cumsum(2)
    return S


def _box_sum(S, x0, y0, z0, bsize):
    """Box sums over blocks of size bsize=(bx,by,bz) at integer corners."""
    bx, by, bz = bsize
    x1, y1, z1 = x0 + bx, y0 + by, z0 + bz
    return (S[z1, y1, x1] - S[z0, y1, x1] - S[z1, y0, x1] - S[z1, y1, x0]
            + S[z0, y0, x1] + S[z0, y1, x0] + S[z1, y0, x0] - S[z0, y0, x0])


def _shifted_volume(arr_pad, frac, out_shape):
    """Trilinear sample of the edge-padded volume at +frac (fz, fy, fx in [0,1)).

    arr_pad has one extra trailing voxel per axis beyond out_shape; the result
    V[i,j,k] equals the clamped trilinear interpolation of the original volume
    at (i - P + fz, j - P + fy, k - P + fx).
    """
    fz, fy, fx = frac
    Z, Y, X = out_shape
    out = np.zeros(out_shape)
    for dz, wz in ((0, 1.0 - fz), (1, fz)):
        if wz == 0.0:
            continue
        for dy, wy in ((0, 1.0 - fy), (1, fy)):
            if wy == 0.0:
                continue
            for dx, wx in ((0, 1.0 - fx), (1, fx)):
                w = wz * wy * wx
                if w == 0.0:
                    continue
                out += w * arr_pad[dz:dz + Z, dy:dy + Y, dx:dx + X]
    return out


def match_blocks(ref_arr, mov_arr, blocks, params, step_factors, vox_sizes,
                 replicate_java_bugs=False, _verbose=False, _SX=None, _SXX=None):
    """Run the core search.

    Parameters
    ----------
    ref_arr, mov_arr : (Z, Y, X) float arrays (already resampled/warped to level).
    blocks : (N, 4) array with columns [x0, y0, z0, var] (blocksRef).
    params : BlockMatchingParams (uses block_half_size, block_size, neighbourhood,
             stride_moving, metric, min_block_score).
    step_factors : (sx, sy, sz) per-axis search step factors for this level.
    vox_sizes : (vx, vy, vz) voxel sizes for this level (for the tie-break distance).
    _verbose : if True, print a per-phase timing summary after each call.
    _SX, _SXX : optional precomputed summed-area tables of ref and ref²
                (Z+1,Y+1,X+1). When provided the setup phase is skipped.

    Returns
    -------
    fixed_pts, moving_pts : (K, 3) voxel coordinates (block centres / matches).
    scores : (K,) best scores. Only blocks with score >= min_block_score are kept.
    """
    n = blocks.shape[0]
    bsize = params.block_size
    half = params.block_half_size
    sx, sy, sz = step_factors
    vx, vy, vz = vox_sizes
    bx, by, bz = bsize

    if n == 0:
        empty = np.empty((0, 3))
        return empty, empty.copy(), np.empty((0,))

    corners = blocks[:, :3]
    Z, Y, X = ref_arr.shape
    M = bx * by * bz

    _t = {"setup": 0.0, "shift": 0.0, "group_integrals": 0.0,
          "cross_batch": 0.0, "update": 0.0}

    # Integer block corners.
    cx0 = np.clip(corners[:, 0].astype(np.intp), 0, max(0, X - bx))
    cy0 = np.clip(corners[:, 1].astype(np.intp), 0, max(0, Y - by))
    cz0 = np.clip(corners[:, 2].astype(np.intp), 0, max(0, Z - bz))
    x1f, y1f, z1f = cx0 + bx, cy0 + by, cz0 + bz  # box corners for fixed blocks

    # Fixed-block sums (use precomputed SATs when available).
    _ts = _time.perf_counter()
    if _SX is None:
        _SX = _integral(ref_arr)
    if _SXX is None:
        _SXX = _integral(ref_arr * ref_arr)
    sum_x = _box_sum(_SX, cx0, cy0, cz0, bsize)
    sum_xx = _box_sum(_SXX, cx0, cy0, cz0, bsize)
    _t["setup"] += _time.perf_counter() - _ts

    offsets = _offsets(params.neighbourhood, params.stride_moving)
    decomp = []
    groups = {}
    for idx, (xp, yp, zp) in enumerate(offsets):
        off = (xp * sx, yp * sy, zp * sz)
        ioff = tuple(int(np.floor(o)) for o in off)
        frac = tuple(o - i for o, i in zip(off, ioff))
        decomp.append((ioff, frac))
        key = tuple(round(f, 9) for f in frac)
        groups.setdefault(key, []).append(idx)

    P = max(1, max(max(abs(c) for c in ioff) for ioff, _ in decomp))
    pad_shape = (Z + 2 * P, Y + 2 * P, X + 2 * P)
    mov_pad = np.pad(mov_arr, ((P, P + 1), (P, P + 1), (P, P + 1)), mode="edge")

    all_scores = [None] * len(offsets)

    # ---------- per-group worker (runs in a thread) --------------------------
    def _run_group(idxs):
        K = len(idxs)
        frac = decomp[idxs[0]][1]
        V = _shifted_volume(mov_pad, (frac[2], frac[1], frac[0]), pad_shape)
        SY  = _integral(V)
        SYY = _integral(V * V)

        int_off = np.array([decomp[idx][0] for idx in idxs], dtype=np.intp)  # (K,3)
        ox_k, oy_k, oz_k = int_off[:, 0:1], int_off[:, 1:2], int_off[:, 2:3]

        # Moving block corners: (K, N)
        mx0 = cx0 + ox_k + P;  mx1 = mx0 + bx
        my0 = cy0 + oy_k + P;  my1 = my0 + by
        mz0 = cz0 + oz_k + P;  mz1 = mz0 + bz

        all_sum_y  = (SY[mz1,my1,mx1]  - SY[mz0,my1,mx1]  - SY[mz1,my0,mx1]
                      - SY[mz1,my1,mx0] + SY[mz0,my0,mx1]  + SY[mz0,my1,mx0]
                      + SY[mz1,my0,mx0] - SY[mz0,my0,mx0])
        all_sum_yy = (SYY[mz1,my1,mx1] - SYY[mz0,my1,mx1] - SYY[mz1,my0,mx1]
                      - SYY[mz1,my1,mx0]+ SYY[mz0,my0,mx1] + SYY[mz0,my1,mx0]
                      + SYY[mz1,my0,mx0]- SYY[mz0,my0,mx0])

        # Batch cross-term: stack K shifted views, multiply with ref, integral.
        all_Vv = np.empty((K, Z, Y, X), dtype=np.float64)
        for k, idx in enumerate(idxs):
            ox, oy, oz = decomp[idx][0]
            all_Vv[k] = V[P+oz:P+oz+Z, P+oy:P+oy+Y, P+ox:P+ox+X]

        all_prod = ref_arr.astype(np.float64, copy=False) * all_Vv  # (K,Z,Y,X)

        S = np.zeros((K, Z+1, Y+1, X+1), dtype=np.float64)
        S[:, 1:, 1:, 1:] = all_prod.cumsum(axis=3).cumsum(axis=2).cumsum(axis=1)

        all_sum_xy = (S[:, z1f, y1f, x1f] - S[:, cz0, y1f, x1f]
                      - S[:, z1f, cy0, x1f] - S[:, z1f, y1f, cx0]
                      + S[:, cz0, cy0, x1f] + S[:, cz0, y1f, cx0]
                      + S[:, z1f, cy0, cx0] - S[:, cz0, cy0, cx0])

        scores_batch = block_score_from_sums(
            sum_x, all_sum_y, all_sum_xy, sum_xx, all_sum_yy, M, params.metric)

        return idxs, scores_batch  # (K,N) scores, caller fills all_scores

    # ---------- parallel dispatch --------------------------------------------
    _ts = _time.perf_counter()
    n_workers = min(len(groups), params.n_threads)
    group_items = list(groups.values())

    if n_workers > 1:
        with _cf.ThreadPoolExecutor(max_workers=n_workers) as ex:
            futs = [ex.submit(_run_group, idxs) for idxs in group_items]
            for fut in _cf.as_completed(futs):
                idxs, scores_batch = fut.result()
                for k, idx in enumerate(idxs):
                    all_scores[idx] = scores_batch[k]
    else:
        for idxs in group_items:
            idxs, scores_batch = _run_group(idxs)
            for k, idx in enumerate(idxs):
                all_scores[idx] = scores_batch[k]
    _t["cross_batch"] += _time.perf_counter() - _ts

    _ts = _time.perf_counter()
    best_score = np.full(n, -np.inf)
    best_dist = np.zeros(n)
    best_off = np.zeros((n, 3))

    for idx, (xp, yp, zp) in enumerate(offsets):
        score = all_scores[idx]
        if replicate_java_bugs:
            dy = (yp * vy * sy) * (yp * vy * sz)
        else:
            dy = (yp * vy * sy) ** 2
        dist = np.sqrt((xp * vx * sx) ** 2 + dy + (zp * vz * sz) ** 2)
        upd = (score > best_score) | ((score == best_score) & (dist < best_dist))
        best_score = np.where(upd, score, best_score)
        best_dist = np.where(upd, dist, best_dist)
        best_off[upd] = (xp, yp, zp)
    _t["update"] += _time.perf_counter() - _ts

    if _verbose:
        total = sum(_t.values())
        parts = "  ".join(f"{k}={v*1000:.1f}ms" for k, v in _t.items())
        print(f"[BM/match] n={n} offsets={len(offsets)} groups={len(groups)}  "
              f"{parts}  total={total*1000:.1f}ms", flush=True)

    cx = corners[:, 0] + half[0]
    cy = corners[:, 1] + half[1]
    cz = corners[:, 2] + half[2]
    fixed_pts = np.stack([cx, cy, cz], axis=1)
    mov_y_step = sz if replicate_java_bugs else sy
    moving_pts = np.stack([
        cx + best_off[:, 0] * sx,
        cy + best_off[:, 1] * mov_y_step,
        cz + best_off[:, 2] * sz,
    ], axis=1)

    keep = best_score >= params.min_block_score
    return fixed_pts[keep], moving_pts[keep], best_score[keep]
