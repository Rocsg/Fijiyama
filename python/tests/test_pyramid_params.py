import pytest

from blockmatching import pyramid
from blockmatching.params import BlockMatchingParams


def test_sub_sampling_factors():
    assert pyramid.sub_sampling_factors(1, 3) == [4, 2, 1]
    assert pyramid.sub_sampling_factors(2, 2) == [2]
    # higher accuracy (level_min = -1): clamped to 1
    assert pyramid.sub_sampling_factors(-1, 2) == [2, 1, 1, 1]


def test_step_factors():
    assert pyramid.step_factors(1, 3) == [1.0, 1.0, 1.0]
    # subpixel steps at finest levels when level_min < 1
    assert pyramid.step_factors(-1, 2) == [1.0, 1.0, 0.5, 0.25]


def test_image_dims_uses_integer_division():
    assert pyramid.image_dims([511, 255, 40], [4, 2, 1], True) == [
        [127, 63, 40], [255, 127, 40], [511, 255, 40]]
    assert pyramid.image_dims([100, 100, 64], [2], False) == [[50, 50, 32]]


def test_vox_sizes_and_sigmas():
    assert pyramid.vox_sizes([0.5, 0.5, 1.0], [4, 2, 1], True) == [
        [2.0, 2.0, 1.0], [1.0, 1.0, 1.0], [0.5, 0.5, 1.0]]
    assert pyramid.smoothing_sigmas([0.5, 0.5, 1.0], [4, 2, 1], 2.0) == [4.0, 2.0, 1.0]
    assert pyramid.dense_field_sigmas([4, 2, 1], 0.8) == [0.8, 0.8, 0.8]


def test_params_validation_and_derived():
    p = BlockMatchingParams(level_min=3, level_max=2)  # Java clamps
    assert p.level_min == 2 and p.nb_levels == 1
    assert BlockMatchingParams().block_size == (15, 15, 3)
    with pytest.raises(ValueError):
        BlockMatchingParams(metric="ssim")
    with pytest.raises(ValueError):
        BlockMatchingParams(stride=(0, 1, 1))


def test_params_auto_rules_and_levels():
    p = BlockMatchingParams(level_min=1, level_max=2, sigma_smoothing_pixels=1.0)
    p.resolve_auto((100, 100, 40))  # 100 < 4*40 => subscale Z too
    assert p.no_subscale_z is False
    lv = p.levels([100, 100, 40], [0.5, 0.5, 0.5])
    assert lv["sub_factors"] == [2, 1]
    assert lv["dims"] == [[50, 50, 20], [100, 100, 40]]
    assert lv["smoothing_sigmas"] == [1.0, 0.5]
