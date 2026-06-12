"""SimpleITK image/transform helpers (org.itk.simple backend).

Isolates all SimpleITK usage so the rest of the algorithm stays pure-numpy and
testable without ITK.  Arrays exchanged with the algorithm are ``(Z, Y, X)``
float64; physical coordinates and voxel sizes are ordered ``(x, y, z)``.
"""
from __future__ import annotations

import numpy as np
import SimpleITK as sitk


def _tiff_spacing(path):
    """Read pixel spacing from ImageJ/OME TIFF metadata via tifffile.

    Returns (vx, vy, vz) in mm, or None if not found / not a TIFF.
    """
    path = str(path)
    if not path.lower().endswith((".tif", ".tiff")):
        return None
    try:
        import tifffile
        with tifffile.TiffFile(path) as tf:
            # ---- ImageJ TIFF ----
            if tf.is_imagej:
                ij = tf.imagej_metadata or {}
                page = tf.pages[0]
                xr = page.tags.get("XResolution")
                yr = page.tags.get("YResolution")
                ru = page.tags.get("ResolutionUnit")
                if xr is None or yr is None:
                    return None
                nx, dx = xr.value
                ny, dy = yr.value
                if dx == 0 or nx == 0 or dy == 0 or ny == 0:
                    return None
                res_unit = int(ru.value) if ru is not None else 1
                ij_unit = (ij.get("unit") or "").lower().replace("\u00b5", "u")

                _UNIT_SCALES = {
                    "um": 1e-3, "micron": 1e-3, "microns": 1e-3,
                    "mm": 1.0, "cm": 10.0, "nm": 1e-6,
                }

                def _ppu_to_mm(ppu):
                    if res_unit == 2:  return 25.4 / ppu          # inch
                    if res_unit == 3:  return 10.0 / ppu           # cm
                    sc = _UNIT_SCALES.get(ij_unit)                  # no-unit
                    return (sc / ppu) if sc is not None else None

                vx = _ppu_to_mm(nx / dx)
                vy = _ppu_to_mm(ny / dy)
                vz_raw = ij.get("spacing")
                if vx is None or vy is None or vz_raw is None:
                    return None

                # spacing is in the same physical unit as ij_unit
                sc = _UNIT_SCALES.get(ij_unit)
                if sc is not None:
                    vz = float(vz_raw) * sc
                elif res_unit == 2:
                    vz = float(vz_raw) * 25.4   # stored in inches
                elif res_unit == 3:
                    vz = float(vz_raw) * 10.0   # stored in cm
                else:
                    return None
                return (float(vx), float(vy), float(vz))

            # ---- OME-TIFF ----
            if tf.is_ome:
                import xml.etree.ElementTree as ET
                ome = tf.ome_metadata
                if ome:
                    root = ET.fromstring(ome)
                    ns = root.tag.split("}")[0].lstrip("{") if "}" in root.tag else ""
                    pfx = f"{{{ns}}}" if ns else ""
                    px = root.find(f".//{pfx}Pixels")
                    if px is not None:
                        vx = float(px.get("PhysicalSizeX") or 0)
                        vy = float(px.get("PhysicalSizeY") or 0)
                        vz = float(px.get("PhysicalSizeZ") or 0)
                        ux = (px.get("PhysicalSizeXUnit") or "µm").replace("\u00b5", "u")
                        sc = {"um": 1e-3, "mm": 1.0, "cm": 10.0, "nm": 1e-6}.get(ux, 1e-3)
                        if vx > 0 and vy > 0 and vz > 0:
                            return (vx * sc, vy * sc, vz * sc)
    except Exception:
        pass
    return None


def read_image(path):
    img = sitk.ReadImage(str(path), sitk.sitkFloat32)
    spacing = _tiff_spacing(path)
    if spacing is not None:
        cur = img.GetSpacing()
        if any(abs(cur[i] - spacing[i]) > 1e-6 * max(spacing[i], 1e-9)
               for i in range(3)):
            img.SetSpacing([float(s) for s in spacing])
    return img


def write_image(img, path):
    sitk.WriteImage(img, str(path))


def image_to_array(img):
    """Return a ``(Z, Y, X)`` float64 array from a scalar sitk image."""
    return sitk.GetArrayFromImage(img).astype(np.float64, copy=False)


def get_dims(img):
    """Return image size as ``(X, Y, Z)``."""
    sz = img.GetSize()
    if len(sz) == 2:
        return (int(sz[0]), int(sz[1]), 1)
    return (int(sz[0]), int(sz[1]), int(sz[2]))


def get_vox_sizes(img):
    """Return voxel spacing as ``(vx, vy, vz)``."""
    sp = img.GetSpacing()
    if len(sp) == 2:
        return (float(sp[0]), float(sp[1]), 1.0)
    return (float(sp[0]), float(sp[1]), float(sp[2]))


def gaussian_smooth(img, sigma_mm):
    """Recursive Gaussian smoothing with sigma given in physical units (mm).

    A zero sigma is a no-op (mirrors BlockMatchingRegistration skipping the
    blur when successiveSmoothingSigma[lev] == 0).
    """
    if sigma_mm is None or sigma_mm <= 0:
        return img
    return sitk.SmoothingRecursiveGaussian(img, float(sigma_mm))


def resample(img, out_spacing, out_size, transform, default_value,
             interpolator=sitk.sitkLinear):
    """Resample ``img`` onto a grid (origin 0, identity direction).

    out_spacing/out_size are ``(x, y, z)`` ordered.
    """
    r = sitk.ResampleImageFilter()
    r.SetOutputSpacing([float(s) for s in out_spacing])
    r.SetSize([int(s) for s in out_size])
    r.SetOutputOrigin(img.GetOrigin())
    r.SetOutputDirection(img.GetDirection())
    r.SetDefaultPixelValue(float(default_value))
    r.SetInterpolator(interpolator)
    r.SetTransform(transform)
    return r.Execute(img)


def affine_from_matrix(matrix):
    """Build a 3D ``AffineTransform`` from a 4x4 homogeneous matrix (maps p->Mp)."""
    matrix = np.asarray(matrix, dtype=np.float64)
    t = sitk.AffineTransform(3)
    t.SetMatrix(matrix[:3, :3].ravel().tolist())
    t.SetTranslation(matrix[:3, 3].tolist())
    return t


def composite_to_affine_matrix(transform):
    """Collapse a linear (composite) transform into a single 4x4 matrix by
    sampling the origin and the three unit basis points."""
    o = np.array(transform.TransformPoint((0.0, 0.0, 0.0)))
    cols = [np.array(transform.TransformPoint(e)) - o
            for e in ((1.0, 0.0, 0.0), (0.0, 1.0, 0.0), (0.0, 0.0, 1.0))]
    m = np.eye(4)
    m[:3, :3] = np.array(cols).T
    m[:3, 3] = o
    return m


def write_transform(transform, path):
    sitk.WriteTransform(transform, str(path))



def new_composite(initial=None):
    """Create a ``CompositeTransform`` optionally seeded with an initial transform.

    The initial transform is added first, so (per ITK's last-added-applied-first
    rule) it is applied last - it is the base ref->mov mapping, exactly as in
    BlockMatchingRegistration where currentTransform starts as a copy of trInit.
    """
    c = sitk.CompositeTransform(3)
    if initial is not None:
        c.AddTransform(initial)
    return c


def flatten_to_displacement_field(transform, ref_img):
    """Sample ``transform`` (ref->mov) into a dense displacement field on the
    reference grid, as ``ItkTransform.getFlattenDenseField`` does."""
    f = sitk.TransformToDisplacementFieldFilter()
    f.SetReferenceImage(ref_img)
    f.SetOutputPixelType(sitk.sitkVectorFloat64)
    return f.Execute(transform)


_DENSE_SUFFIX = ".transform.tif"


def save_dense_field_as_tif(transform, ref_img, path):
    """Write a dense displacement field in Fijiyama's Java-readable layout.

    Produces ``<short>.x.tif``, ``<short>.y.tif``, ``<short>.z.tif`` (32-bit float
    component stacks with mm calibration) and ``<short>.transform.tif`` (an 8-bit
    norm image used only as the selectable identifier).  ``path`` must end with
    ``.transform.tif`` so that ``ItkTransform.readAsDenseField`` accepts it.
    """
    import tifffile

    path = str(path)
    if not path.endswith(_DENSE_SUFFIX):
        raise ValueError(f"dense field path must end with {_DENSE_SUFFIX!r}")
    short = path[: -len(_DENSE_SUFFIX)]

    field_img = flatten_to_displacement_field(transform, ref_img)
    vx, vy, vz = get_vox_sizes(field_img)
    arr = sitk.GetArrayFromImage(field_img)  # (Z, Y, X, 3) components [x, y, z]

    def _write(component, suffix):
        data = np.ascontiguousarray(component.astype(np.float32))
        tifffile.imwrite(
            short + suffix, data, imagej=True,
            resolution=(1.0 / vx, 1.0 / vy),
            metadata={"spacing": float(vz), "unit": "mm"},
        )

    _write(arr[..., 0], ".x.tif")
    _write(arr[..., 1], ".y.tif")
    _write(arr[..., 2], ".z.tif")

    norm = np.sqrt((arr.astype(np.float64) ** 2).sum(axis=-1))
    peak = float(norm.max())
    gray = np.zeros(norm.shape, dtype=np.uint8) if peak <= 0 else \
        np.clip(norm / peak * 255.0, 0, 255).astype(np.uint8)
    tifffile.imwrite(
        short + _DENSE_SUFFIX, gray, imagej=True,
        resolution=(1.0 / vx, 1.0 / vy),
        metadata={"spacing": float(vz), "unit": "mm"},
    )
    return short + _DENSE_SUFFIX
