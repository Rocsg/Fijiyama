"""End-to-end validation on a synthetic 3D volume.

A textured reference volume is warped by a known rigid transform to build the
moving image; the registration must realign it (correlation with the reference
recovered close to 1, and far above the misaligned overlay).
"""
import numpy as np
import pytest
from scipy.ndimage import gaussian_filter

from blockmatching import core
from blockmatching import itk_io as io
from blockmatching.params import BlockMatchingParams

sitk = pytest.importorskip("SimpleITK")


def _textured_volume(shape=(24, 64, 64), seed=7):
    rng = np.random.default_rng(seed)
    vol = gaussian_filter(rng.normal(size=shape), sigma=(1.5, 3.0, 3.0))
    # add a few bright blobs to give strong, localizable structure
    zz, yy, xx = np.mgrid[0:shape[0], 0:shape[1], 0:shape[2]]
    for cz, cy, cx, r in [(12, 24, 24, 6), (10, 40, 44, 5), (16, 30, 18, 4)]:
        vol += 4.0 * np.exp(-(((zz-cz)/r)**2 + ((yy-cy)/r)**2 + ((xx-cx)/r)**2))
    return vol.astype(np.float32)


def _corr(a, b):
    a = a.ravel() - a.mean()
    b = b.ravel() - b.mean()
    return float(a @ b / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-12))


def test_rigid_recovery_on_synthetic_volume():
    arr = _textured_volume()
    ref = sitk.GetImageFromArray(arr)            # spacing (1,1,1)
    cx = [d * s / 2.0 for d, s in zip(ref.GetSize(), ref.GetSpacing())]

    known = sitk.Euler3DTransform()
    known.SetCenter(cx)
    known.SetRotation(0.0, 0.0, np.deg2rad(4.0))  # 4 deg about z
    known.SetTranslation((2.5, -1.5, 0.0))        # mm

    mov = sitk.Resample(ref, ref, known, sitk.sitkLinear, 0.0)

    params = BlockMatchingParams(
        transform_type="rigid",
        level_min=0, level_max=1, n_iterations=8,
        block_half_size=(5, 5, 1), neighbourhood=(3, 3, 0), stride=(3, 3, 3),
    )
    tr = core.register(ref, mov, params)

    ref_dims, ref_vox = io.get_dims(ref), io.get_vox_sizes(ref)
    warped = io.resample(mov, ref_vox, ref_dims, tr, 0.0)

    ref_a = io.image_to_array(ref)[4:-4, 8:-8, 8:-8]
    mov_a = io.image_to_array(mov)[4:-4, 8:-8, 8:-8]
    war_a = io.image_to_array(warped)[4:-4, 8:-8, 8:-8]

    before = _corr(ref_a, mov_a)
    after = _corr(ref_a, war_a)

    assert after > before
    assert after > 0.9
