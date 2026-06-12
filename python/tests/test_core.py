import numpy as np
import pytest
from scipy.ndimage import gaussian_filter

from blockmatching import core
from blockmatching.params import BlockMatchingParams

sitk = pytest.importorskip("SimpleITK")


def _smooth_field(shape, seed=0, sigma=2.0):
    rng = np.random.default_rng(seed)
    f = rng.normal(size=shape)
    return gaussian_filter(f, sigma).astype(np.float64)


def test_linear_increment_recovers_translation():
    # big smooth field; ref and mov are two windows offset by +2 voxels in x.
    field = _smooth_field((1, 70, 70), seed=1, sigma=2.5)
    ref = field[:, 10:58, 10:58]
    mov = field[:, 10:58, 12:60]  # mov[x] = ref[x+2]
    params = BlockMatchingParams(
        transform_type="translation",
        block_half_size=(3, 3, 0),
        neighbourhood=(2, 2, 0),
        stride=(3, 3, 3),
        pct_variance=100, pct_score=100, pct_lts=100,
        min_block_variance=1e-9, min_block_score=-1.0,
    )
    vox = (1.0, 1.0, 1.0)
    step = (1.0, 1.0, 1.0)
    stride = [3, 3, 3]
    fx, mv, sc = core.collect_correspondences(ref, mov, params, stride, step, vox)
    assert fx.shape[0] > 10
    inc = core.linear_increment(fx, mv, sc, params, vox)
    # mov(c) = ref(c+2)  ->  best offset d = -2 in x
    assert np.isclose(inc[0, 3], -2.0, atol=0.4)
    assert abs(inc[1, 3]) < 0.4 and abs(inc[2, 3]) < 0.4


def test_linear_increment_identity_when_too_few_points():
    params = BlockMatchingParams(transform_type="rigid")
    fx = np.zeros((3, 3))
    mv = np.zeros((3, 3))
    sc = np.ones(3)
    inc = core.linear_increment(fx, mv, sc, params, (1.0, 1.0, 1.0))
    assert np.allclose(inc, np.eye(4))


def test_is_converged_uses_voxel_tolerance():
    m = np.eye(4)
    m[0, 3] = 0.01
    assert core._is_converged(m, (1.0, 1.0, 1.0))      # 0.01 < 0.05
    assert not core._is_converged(m, (0.1, 0.1, 0.1))  # 0.01 > 0.005


def test_register_recovers_translation_end_to_end():
    # 3D volume with a smooth texture; moving = reference shifted by +2 vox in x.
    field = _smooth_field((6, 40, 40), seed=3, sigma=2.0)
    ref_arr = field[:, 4:36, 4:36]
    mov_arr = field[:, 4:36, 6:38]  # mov[x] = ref[x+2]

    ref_img = sitk.GetImageFromArray(ref_arr.astype(np.float32))
    mov_img = sitk.GetImageFromArray(mov_arr.astype(np.float32))
    ref_img.SetSpacing((1.0, 1.0, 1.0))
    mov_img.SetSpacing((1.0, 1.0, 1.0))

    params = BlockMatchingParams(
        transform_type="translation",
        level_min=0, level_max=0, n_iterations=3,
        block_half_size=(3, 3, 1),
        neighbourhood=(2, 2, 0),
        stride=(2, 2, 2),
        no_subscale_z=True,
        pct_variance=100, pct_score=90, pct_lts=100,
        min_block_variance=1e-9, min_block_score=0.3,
    )
    tr = core.register(ref_img, mov_img, params)
    # ref -> mov mapping should shift x by about -2 (mov[x] = ref[x+2]).
    p = np.array(tr.TransformPoint((16.0, 16.0, 3.0)))
    assert np.isclose(p[0] - 16.0, -2.0, atol=0.7)
    assert abs(p[1] - 16.0) < 0.7 and abs(p[2] - 3.0) < 0.7
