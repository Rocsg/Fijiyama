"""Fixed-block enumeration, scoring-variable computation and selection.

Faithful port of the block preparation stage in
BlockMatchingRegistration.runBlockMatching (the blX/blY/blZ enumeration,
variance sort, mask trimming and "gofast" random selection) plus the
logNumberOfBlocks / randomSelection helpers.

Conventions
-----------
* Image arrays are numpy float32/float64 of shape (Z, Y, X).
* A "block" is described by its lower corner (x0, y0, z0) in level-pixel space.
* The scoring variable stored per block (called "variance" in the Java code) is
  in fact the population standard deviation of the block voxels, as returned by
  VitimageUtils.statistics1D()[1]. We keep the Java name but compute std (ddof=0).

Block table layouts (matching the Java arrays):
* tmp layout  : columns [var, x0, y0, z0]   (blocksRefTmp)
* final layout: columns [x0, y0, z0, var]   (blocksRef)
"""
import math

import numpy as np

_EPSILON = 1e-8  # Java EPSILON used in stride rounding


def _java_round(x):
    return int(math.floor(x + 0.5))


def level_strides(base_stride, sub_sampling_factors):
    """Port of the per-level stride computation in runBlockMatching:

        levelStride = round(max(1, -EPSILON + stride / subFactor**(1/3)))
    """
    out = []
    for s, f in zip(base_stride, sub_sampling_factors):
        out.append(_java_round(max(1.0, -_EPSILON + s / (f ** (1.0 / 3)))))
    return out


def block_counts(dims, block_size, neighbourhood, stride_moving, level_stride):
    """Port of nbBlocksX/Y/Z. dims/block_size/... are (x, y, z) ordered."""
    nbx = 1 + (dims[0] - block_size[0] - 2 * neighbourhood[0] * stride_moving) // level_stride[0]
    nby = 1 + (dims[1] - block_size[1] - 2 * neighbourhood[1] * stride_moving) // level_stride[1]
    if dims[2] > 1:
        nbz = 1 + (dims[2] - block_size[2] - 2 * neighbourhood[2] * stride_moving) // level_stride[2]
    else:
        nbz = 1
    return nbx, nby, nbz


def generate_blocks(ref_arr, block_size, neighbourhood, stride_moving, level_stride,
                    _SX=None, _SXX=None):
    """Enumerate candidate fixed blocks and their scoring std.

    Returns an (N, 4) float64 array with columns [var, x0, y0, z0] in the same
    iteration order as the Java triple loop (blX outer, blY, blZ inner), i.e. the
    `blocksRefTmp` array before mask trimming and sorting.

    If ``_SX`` and ``_SXX`` (precomputed summed-area tables of ref and ref²) are
    provided, variance is computed via SAT box-sums (O(N_blocks)) instead of the
    sliding_window_view path (O(N_blocks × block_size)), giving a large speedup.
    """
    dims = (ref_arr.shape[2], ref_arr.shape[1], ref_arr.shape[0])  # x, y, z
    nbx, nby, nbz = block_counts(dims, block_size, neighbourhood, stride_moving, level_stride)
    if nbx <= 0 or nby <= 0 or nbz <= 0:
        return np.empty((0, 4), dtype=np.float64)

    bx, by, bz = block_size
    ox = neighbourhood[0] * stride_moving
    oy = neighbourhood[1] * stride_moving
    oz = neighbourhood[2] * stride_moving

    zc = oz + np.arange(nbz) * level_stride[2]
    yc = oy + np.arange(nby) * level_stride[1]
    xc = ox + np.arange(nbx) * level_stride[0]
    # Java order: blX outer, blY, blZ inner -> iterate x slowest, z fastest.
    XX, YY, ZZ = np.meshgrid(xc, yc, zc, indexing="ij")  # each (nbx, nby, nbz)

    if _SX is not None and _SXX is not None:
        # Fast path: variance = mean(x²) - mean(x)² via summed-area tables.
        # _SX / _SXX are (Z+1, Y+1, X+1) prefix-sum arrays of ref and ref².
        M = bx * by * bz
        cx0 = XX.ravel().astype(np.intp)
        cy0 = YY.ravel().astype(np.intp)
        cz0 = ZZ.ravel().astype(np.intp)
        x1, y1, z1 = cx0 + bx, cy0 + by, cz0 + bz
        def _bsum(S):
            return (S[z1, y1, x1] - S[cz0, y1, x1] - S[z1, cy0, x1] - S[z1, y1, cx0]
                    + S[cz0, cy0, x1] + S[cz0, y1, cx0] + S[z1, cy0, cx0]
                    - S[cz0, cy0, cx0])
        mean_x = _bsum(_SX) / M
        mean_xx = _bsum(_SXX) / M
        std_flat = np.sqrt(np.maximum(0.0, mean_xx - mean_x * mean_x))
        # std_flat follows (nbx, nby, nbz) ravel order from meshgrid(indexing="ij")
        var_3d = std_flat.reshape(nbx, nby, nbz)
    else:
        # Original path: sliding_window_view + std (backward compat / no SATs yet).
        win = np.lib.stride_tricks.sliding_window_view(ref_arr, (bz, by, bx))
        sub = win[oz:oz + nbz * level_stride[2]:level_stride[2],
                  oy:oy + nby * level_stride[1]:level_stride[1],
                  ox:ox + nbx * level_stride[0]:level_stride[0]]
        sub = sub[:nbz, :nby, :nbx]
        flat = sub.reshape(nbz, nby, nbx, -1).astype(np.float64, copy=False)
        std = flat.std(axis=-1)          # (nbz, nby, nbx), population std
        var_3d = np.transpose(std, (2, 1, 0))  # → (nbx, nby, nbz)

    out = np.stack([var_3d.ravel(), XX.ravel().astype(np.float64),
                    YY.ravel().astype(np.float64), ZZ.ravel().astype(np.float64)], axis=1)
    return out


def trim_using_mask(blocks_tmp, mask_arr):
    """Port of trimUsingMaskNEW: keep blocks whose *corner* voxel is inside mask.

    blocks_tmp: (N, 4) columns [var, x0, y0, z0]. mask_arr: (Z, Y, X).
    """
    if blocks_tmp.shape[0] == 0:
        return blocks_tmp
    eps = 1e-3  # Java 10E-4
    x = blocks_tmp[:, 1].astype(int)
    y = blocks_tmp[:, 2].astype(int)
    z = blocks_tmp[:, 3].astype(int)
    vals = mask_arr[z, y, x]
    keep = vals >= 1 - eps
    return blocks_tmp[keep]


def select_by_variance(blocks_tmp, pct_variance, min_block_variance):
    """Port of the variance sort + bottom removal + min-variance threshold.

    Input  layout: [var, x0, y0, z0] (blocksRefTmp).
    Output layout: [x0, y0, z0, var] (blocksRef), sorted ascending by var.
    """
    n = blocks_tmp.shape[0]
    if n == 0:
        return np.empty((0, 4), dtype=np.float64)
    # Stable ascending sort by var (Arrays.sort is stable on objects).
    order = np.argsort(blocks_tmp[:, 0], kind="stable")
    s = blocks_tmp[order]
    last_removal = (n * (100 - pct_variance)) // 100
    var = s[:, 0].copy()
    var[:last_removal] = -1.0
    keep = var >= min_block_variance
    s = s[keep]
    out = np.empty((s.shape[0], 4), dtype=np.float64)
    out[:, 0] = s[:, 1]  # x0
    out[:, 1] = s[:, 2]  # y0
    out[:, 2] = s[:, 3]  # z0
    out[:, 3] = s[:, 0]  # var
    return out


def log_number_of_blocks(in_n):
    """Port of logNumberOfBlocks (target count for the gofast random selection)."""
    out_n = 300
    for thresh, factor in ((1_000_000, 0.0001), (100_000, 0.0007), (10_000, 0.009),
                           (4_000, 0.07), (2_000, 0.12), (1_000, 0.23), (300, 0.42)):
        if in_n > thresh:
            out_n += (in_n - thresh) * factor
            in_n -= (in_n - thresh)
    return int(out_n)


def random_selection(blocks, seed=12345):
    """Port of randomSelection (gofast). Behavioural port, not RNG-stream identical.

    Keeps each block with probability log_number_of_blocks(N)/N using a seeded RNG.
    """
    n = blocks.shape[0]
    if n == 0:
        return blocks
    ratio = log_number_of_blocks(n) / float(n)
    rng = np.random.default_rng(seed)
    keep = rng.random(n) < ratio
    return blocks[keep]
