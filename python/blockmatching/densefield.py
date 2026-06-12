"""Dense displacement field from sparse correspondences.

Faithful port of the ``zeroPaddingOutside == false`` branch of
``ItkTransform.computeDenseFieldFromSparseCorrespondancePoints`` (the branch
used by ``BlockMatchingRegistration``).

The field is built by splatting each correspondence's physical displacement
``mov - ref`` into a weight volume and three component volumes at the rounded
voxel position of the *reference* point, Gaussian-smoothing weight and
components with the same kernel, then dividing components by the smoothed
weight.  The result is a per-voxel physical displacement that maps reference
coordinates towards moving coordinates.
"""
from __future__ import annotations

import math
import time as _time

import numpy as np
from scipy.ndimage import gaussian_filter, uniform_filter as _uniform_filter


def _smooth(arr, sigma_zyx, uniform):
    """Apply a separable smoothing kernel.

    uniform=False  → Gaussian (sigma_zyx in voxels)
    uniform=True   → box filter whose half-width matches the Gaussian variance:
                     size = max(1, round(sigma * 2*sqrt(3)))  per axis
    """
    if not uniform:
        return gaussian_filter(arr, sigma_zyx, mode="nearest")
    # Uniform filter size that matches Gaussian variance: var(uniform[-W/2,W/2]) = W²/12
    # → W = sigma * 2*sqrt(3)
    _s3 = 2.0 * math.sqrt(3.0)
    size = tuple(max(1, int(s * _s3 + 0.5)) for s in sigma_zyx)
    return _uniform_filter(arr, size=size, mode="nearest")

_EPSILON = 1e-19


def _java_round(x: float) -> int:
    return int(math.floor(x + 0.5))


def compute_dense_field(
    ref_pts,
    mov_pts,
    ref_dims,
    ref_vox_sizes,
    sigma_mm,
    mode="nearest",
    _verbose=False,
    uniform=False,
):
    """Compute a dense displacement field from sparse correspondences.

    Parameters
    ----------
    ref_pts, mov_pts : (N, 3) array_like
        Physical (mm) coordinates ``[x, y, z]`` of the reference (splat
        positions) and moving points.  Displacement = ``mov - ref``.
    ref_dims : (X, Y, Z) int tuple
        Dimensions of the reference image.
    ref_vox_sizes : (vx, vy, vz) float tuple
        Voxel sizes (mm) of the reference image.
    sigma_mm : float
        Smoothing sigma (mm), applied isotropically in physical space — each
        axis gets ``sigma_mm / voxel_size`` in voxels.
    mode : str
        Boundary mode passed to ``scipy.ndimage.gaussian_filter``.  The same
        mode is used for weight and components so its choice largely cancels in
        the final ratio.

    Returns
    -------
    field : (Z, Y, X, 3) float ndarray
        Per-voxel physical displacement, last axis ordered ``[dx, dy, dz]``.
    """
    ref_pts = np.asarray(ref_pts, dtype=np.float64).reshape(-1, 3)
    mov_pts = np.asarray(mov_pts, dtype=np.float64).reshape(-1, 3)
    dim_x, dim_y, dim_z = (int(d) for d in ref_dims)
    vx, vy, vz = (float(v) for v in ref_vox_sizes)

    weights = np.zeros((dim_z, dim_y, dim_x), dtype=np.float32)
    field_x = np.zeros_like(weights)
    field_y = np.zeros_like(weights)
    field_z = np.zeros_like(weights)

    disp = mov_pts - ref_pts  # physical displacement, columns [dx, dy, dz]

    _ts = _time.perf_counter()
    for i in range(ref_pts.shape[0]):
        ix = _java_round(ref_pts[i, 0] / vx)
        iy = _java_round(ref_pts[i, 1] / vy)
        iz = _java_round(ref_pts[i, 2] / vz)
        if 0 <= iz < dim_z and 0 <= ix < dim_x and 0 <= iy < dim_y:
            # last write wins, mirroring ImageProcessor.setf
            weights[iz, iy, ix] = 1.0
            field_x[iz, iy, ix] = disp[i, 0]
            field_y[iz, iy, ix] = disp[i, 1]
            field_z[iz, iy, ix] = disp[i, 2]

    _t_splat = _time.perf_counter() - _ts

    # Isotropic Gaussian kernel in physical space: same sigma_mm on each axis,
    # converted to voxels per-axis via the (potentially anisotropic) voxel size.
    sig_x = sigma_mm / vx
    sig_y = sigma_mm / vy
    sig_z = sigma_mm / vz
    sigma_zyx = (sig_z, sig_y, sig_x)

    _ts = _time.perf_counter()
    weights = _smooth(weights, sigma_zyx, uniform)
    field_x = _smooth(field_x, sigma_zyx, uniform)
    field_y = _smooth(field_y, sigma_zyx, uniform)
    field_z = _smooth(field_z, sigma_zyx, uniform)
    _t_gauss = _time.perf_counter() - _ts

    if _verbose:
        method = "uniform" if uniform else "gaussian"
        print(f"[BM/dense/field]  splat={_t_splat*1000:.1f}ms"
              f"  {method}={_t_gauss*1000:.1f}ms", flush=True)

    valid = weights >= _EPSILON
    inv = np.zeros_like(weights)
    np.divide(1.0, weights, out=inv, where=valid)
    field_x = np.where(valid, field_x * inv, 0.0)
    field_y = np.where(valid, field_y * inv, 0.0)
    field_z = np.where(valid, field_z * inv, 0.0)

    return np.stack([field_x, field_y, field_z], axis=-1).astype(np.float64)


def to_sitk_displacement_transform(field, ref_vox_sizes, origin=(0.0, 0.0, 0.0)):
    """Wrap a ``(Z, Y, X, 3)`` displacement field as a SimpleITK transform.

    Imported lazily so the rest of the module does not require SimpleITK.
    """
    import SimpleITK as sitk

    field = np.ascontiguousarray(np.asarray(field, dtype=np.float64))
    # sitk.GetImageFromArray treats the last axis as the vector component when
    # isVector=True; the leading axes are interpreted as (z, y, x).
    img = sitk.GetImageFromArray(field, isVector=True)
    img.SetSpacing([float(v) for v in ref_vox_sizes])
    img.SetOrigin([float(o) for o in origin])
    return sitk.DisplacementFieldTransform(sitk.Cast(img, sitk.sitkVectorFloat64))
