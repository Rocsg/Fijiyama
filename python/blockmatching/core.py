"""Level/iteration orchestration of the 3D block-matching registration.

Faithful port of the scale/iteration loops of
``BlockMatchingRegistration.runBlockMatching`` (standard, non-stress path).

The pure-numpy pieces (``collect_correspondences``, ``linear_increment``) operate
on ``(Z, Y, X)`` arrays and are independently testable.  ``register`` ties them
together using the SimpleITK backend (resampling, transform composition).

Excluded by scope: GUI/updateViews, timing scaffolding, stress test,
interruption handling, R^2 logging and the OLD_BEHAVIOUR / correspondence-at-start
branches.
"""
from __future__ import annotations

import numpy as np

from . import blocks as B
from . import densefield as D
from . import estimation as E
from . import matching as M

# 0.05 * min(voxel size) translation tolerance, like the Java convergence test.
_TRANS_TOL_FACTOR = 0.05
_ROT_TOL = 1e-6
_MIN_POINTS = 5


def _to_physical(pts_vox, vox_sizes):
    out = np.asarray(pts_vox, dtype=np.float64).copy()
    out[:, 0] *= vox_sizes[0]
    out[:, 1] *= vox_sizes[1]
    out[:, 2] *= vox_sizes[2]
    return out


def _java_round(x):
    import math
    return int(math.floor(x + 0.5))


def level_geometry(params, levels, lev, ref_dims):
    """Per-level derived quantities matching the head of the Java level loop."""
    cur_dims = levels["dims"][lev]
    cur_vox = levels["vox_sizes"][lev]
    step_n = levels["step_factors"][lev]
    vox_min = min(cur_vox)
    step_factors = (
        step_n * vox_min / cur_vox[0],
        step_n * vox_min / cur_vox[1],
        step_n * vox_min / cur_vox[2],
    )
    # Per-axis subsampling factors as Java computes them: round(origDim/curDim).
    # This naturally yields a z-factor of 1 when no_subscale_z keeps the full
    # z resolution, while x/y use the pyramid factor.
    sub_axis = (
        max(1, _java_round(ref_dims[0] / cur_dims[0])),
        max(1, _java_round(ref_dims[1] / cur_dims[1])),
        max(1, _java_round(ref_dims[2] / cur_dims[2])),
    )
    level_stride = B.level_strides(params.stride, sub_axis)
    return cur_dims, cur_vox, step_factors, level_stride


def collect_correspondences(ref_arr, mov_arr, params, level_stride, step_factors,
                            vox_sizes, mask_arr=None, random_selection=False,
                            rng_seed=12345, replicate_java_bugs=False,
                            _verbose=False):
    """Enumerate, select and match blocks. Returns (fixed_vox, moving_vox, scores)."""
    import time as _time
    # Compute summed-area tables of ref once; reused for variance selection AND
    # for the fixed-block sums inside match_blocks (avoids recomputing them there).
    _ts = _time.perf_counter()
    _SX = M._integral(ref_arr)
    _SXX = M._integral(ref_arr * ref_arr)
    _ti = _time.perf_counter()
    tmp = B.generate_blocks(ref_arr, params.block_size, params.neighbourhood,
                            params.stride_moving, level_stride,
                            _SX=_SX, _SXX=_SXX)
    _tgen = _time.perf_counter() - _ti
    if mask_arr is not None:
        tmp = B.trim_using_mask(tmp, mask_arr)
    _ti = _time.perf_counter()
    blocks_ref = B.select_by_variance(tmp, params.pct_variance,
                                      params.min_block_variance)
    _tvar = _time.perf_counter() - _ti
    if random_selection:
        blocks_ref = B.random_selection(blocks_ref, rng_seed)
    if _verbose:
        _tsat = _ti - _ts
        print(f"[BM/corr] sat={_tsat*1000:.1f}ms  generate={_tgen*1000:.1f}ms  "
              f"select_var={_tvar*1000:.1f}ms  "
              f"n_before={len(tmp)}  n_after={len(blocks_ref)}", flush=True)
    return M.match_blocks(ref_arr, mov_arr, blocks_ref, params, step_factors,
                          vox_sizes, replicate_java_bugs=replicate_java_bugs,
                          _verbose=_verbose, _SX=_SX, _SXX=_SXX)


def linear_increment(fixed_vox, moving_vox, scores, params, vox_sizes):
    """Two-pass (score then LTS) estimation of one linear increment.

    Returns a 4x4 matrix mapping fixed -> moving (physical), as in the Java
    affine branch.  Fewer than 5 score-selected points yields identity.
    """
    fixed = _to_physical(fixed_vox, vox_sizes)
    moving = _to_physical(moving_vox, vox_sizes)

    f1, m1, s1, _ = E.trim_by_score(fixed, moving, scores, params.pct_score)
    if f1.shape[0] < _MIN_POINTS:
        return np.eye(4)

    first = E.estimate_transform(params.transform_type, f1, m1)
    f2, m2, s2, _ = E.trim_by_lts(f1, m1, s1, first, params.pct_lts)
    return E.estimate_transform(params.transform_type, f2, m2)


def dense_increment(fixed_vox, moving_vox, scores, params, cur_dims, vox_sizes,
                    sigma_mm, _verbose=False):
    """One dense displacement field increment (score selection only).

    Returns a ``(Z, Y, X, 3)`` physical displacement field, or None if there are
    no surviving correspondences.
    """
    import time as _time
    _t0 = _time.perf_counter()
    fixed = _to_physical(fixed_vox, vox_sizes)
    moving = _to_physical(moving_vox, vox_sizes)
    f1, m1, _, _ = E.trim_by_score(fixed, moving, scores, params.pct_score)
    if f1.shape[0] == 0:
        return None
    _t1 = _time.perf_counter()
    field = D.compute_dense_field(f1, m1, cur_dims, vox_sizes, sigma_mm,
                                  _verbose=_verbose,
                                  uniform=params.uniform_field_interp)
    _t2 = _time.perf_counter()
    if _verbose:
        vx, vy, vz = vox_sizes
        sig_x = sigma_mm / vx
        sig_y = sigma_mm / vy
        sig_z = sigma_mm / vz
        print(f"[BM/dense] n_corr={f1.shape[0]}  sigma={sigma_mm:.3f}mm"
              f"  ({sig_x:.1f},{sig_y:.1f},{sig_z:.1f})vox"
              f"  trim={(_t1-_t0)*1000:.1f}ms"
              f"  dense_field={(_t2-_t1)*1000:.1f}ms", flush=True)
    return field


def _is_converged(matrix, vox_sizes):
    tol = _TRANS_TOL_FACTOR * min(vox_sizes)
    return E.is_identity(matrix, _ROT_TOL, tol)


# --------------------------------------------------------------------------- #
# SimpleITK orchestration
# --------------------------------------------------------------------------- #

def _estimate_background(arr):
    """Approximate VitimageUtils.caracterizeBackgroundOfImage: mean of the outer
    shell of the volume (a cheap, reasonable stand-in for the background value
    used as the resampling default)."""
    mask = np.zeros(arr.shape, dtype=bool)
    mask[0] = mask[-1] = True
    mask[:, 0] = mask[:, -1] = True
    mask[:, :, 0] = mask[:, :, -1] = True
    return float(arr[mask].mean())


def _resolve_defaults(params, ref_arr, ref_dims):
    """Fill in auto (None) params from the reference image, mirroring the Java
    constructor."""
    if params.min_block_variance is None:
        zc = ref_dims[2] // 2
        params.min_block_variance = float(ref_arr[zc].mean()) / 10.0
    if params.ref_default_value is None:
        params.ref_default_value = _estimate_background(ref_arr)


def register(ref_img, mov_img, params, initial_transform=None,
             mask_img=None, random_selection=False, replicate_java_bugs=False):
    """Run the full multi-level block-matching registration.

    Parameters
    ----------
    ref_img, mov_img : SimpleITK scalar images.
    params : BlockMatchingParams.
    initial_transform : optional SimpleITK transform (base ref->mov mapping).
    mask_img : optional SimpleITK image restricting fixed blocks.

    Returns
    -------
    SimpleITK CompositeTransform mapping reference -> moving coordinates
    (including the initial transform).
    """
    import SimpleITK as sitk

    from . import itk_io as io

    ref_dims = io.get_dims(ref_img)
    ref_vox = io.get_vox_sizes(ref_img)
    params.resolve_auto(ref_dims, ref_vox)

    ref_full_arr = io.image_to_array(ref_img)
    _resolve_defaults(params, ref_full_arr, ref_dims)
    if params.mov_default_value is None:
        params.mov_default_value = _estimate_background(io.image_to_array(mov_img))

    levels = params.levels(ref_dims, ref_vox)
    current = io.new_composite(initial_transform)

    import time as _time
    _t0 = _time.perf_counter()
    def _dt(): return _time.perf_counter() - _t0

    nb_levels = params.nb_levels
    print(f"[BM +{_dt():.2f}s] ref  dims={ref_dims}  "
          f"vox={tuple(f'{v:.4f}' for v in ref_vox)} mm", flush=True)
    print(f"[BM +{_dt():.2f}s] {nb_levels} level(s), {params.n_iterations} iter/level, "
          f"type={params.transform_type}", flush=True)

    for lev in range(nb_levels):
        cur_dims, cur_vox, step_factors, level_stride = level_geometry(
            params, levels, lev, ref_dims)
        sigma = levels["smoothing_sigmas"][lev]
        dense_sigma = levels["dense_field_sigmas"][lev]

        print(f"[BM +{_dt():.2f}s] --- level {lev+1}/{nb_levels}  "
              f"dims={cur_dims}  vox={tuple(f'{v:.4f}' for v in cur_vox)} mm  "
              f"stride={level_stride}  sigma={sigma:.4f}", flush=True)

        print(f"[BM +{_dt():.2f}s]   smoothing + resampling ref ...", flush=True)
        ref_lev = io.resample(io.gaussian_smooth(ref_img, sigma), cur_vox, cur_dims,
                              sitk.Transform(3, sitk.sitkIdentity),
                              params.ref_default_value)
        ref_arr = io.image_to_array(ref_lev)
        print(f"[BM +{_dt():.2f}s]   ref resampled to {ref_arr.shape}", flush=True)

        mask_arr = None
        if mask_img is not None:
            print(f"[BM +{_dt():.2f}s]   resampling mask ...", flush=True)
            mask_lev = io.resample(mask_img, cur_vox, cur_dims,
                                   sitk.Transform(3, sitk.sitkIdentity), 1.0)
            mask_arr = io.image_to_array(mask_lev)

        print(f"[BM +{_dt():.2f}s]   smoothing mov ...", flush=True)
        smoothed_mov = io.gaussian_smooth(mov_img, sigma)
        print(f"[BM +{_dt():.2f}s]   mov smoothed. starting iterations.", flush=True)

        for it in range(params.n_iterations):
            _ti = _time.perf_counter()
            print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                  f"resampling mov ...", flush=True)
            mov_lev = io.resample(smoothed_mov, cur_vox, cur_dims, current,
                                  params.mov_default_value)
            mov_arr = io.image_to_array(mov_lev)
            print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                  f"mov resampled ({_time.perf_counter()-_ti:.2f}s)  "
                  f"collecting correspondences ...", flush=True)

            fixed_vox, moving_vox, scores = collect_correspondences(
                ref_arr, mov_arr, params, level_stride, step_factors, cur_vox,
                mask_arr=mask_arr, random_selection=random_selection,
                rng_seed=12345, replicate_java_bugs=replicate_java_bugs,
                _verbose=(it == 0 and lev == 0))

            n_corr = len(scores)
            print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                  f"corr={n_corr} ({_time.perf_counter()-_ti:.2f}s)  "
                  f"estimating ...", flush=True)

            if params.transform_type == "dense":
                field = dense_increment(fixed_vox, moving_vox, scores, params,
                                        cur_dims, cur_vox, dense_sigma,
                                        _verbose=(it == 0 and lev == 0))
                if field is None:
                    print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                          f"no dense field (skipping)", flush=True)
                    continue
                current.AddTransform(D.to_sitk_displacement_transform(
                    field, cur_vox))
                print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                      f"dense field added  ({_time.perf_counter()-_ti:.2f}s total)",
                      flush=True)
            else:
                inc = linear_increment(fixed_vox, moving_vox, scores, params,
                                       cur_vox)
                converged = _is_converged(inc, cur_vox)
                t = inc[:3, 3]
                R = inc[:3, :3]
                det_R = float(np.linalg.det(R))
                ortho_err = float(np.abs(R.T @ R - np.eye(3)).max())
                angle_deg = float(np.degrees(np.arccos(
                    np.clip((np.trace(R) - 1.0) / 2.0, -1.0, 1.0))))
                print(f"[BM +{_dt():.2f}s]   iter {it+1}/{params.n_iterations}  "
                      f"corr={n_corr}  "
                      f"t=({t[0]:.3f},{t[1]:.3f},{t[2]:.3f}) mm  "
                      f"rot={angle_deg:.3f}°  det={det_R:.6f}  ortho_err={ortho_err:.2e}  "
                      f"({_time.perf_counter()-_ti:.2f}s)  "
                      f"{'CONVERGED' if converged else ''}", flush=True)
                if converged:
                    break
                current.AddTransform(io.affine_from_matrix(inc))

    print(f"[BM +{_dt():.2f}s] done.", flush=True)
    return current
