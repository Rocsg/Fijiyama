import numpy as np

from blockmatching import blocks, matching
from blockmatching.params import BlockMatchingParams


def test_extract_fixed_blocks_matches_manual():
    rng = np.random.default_rng(7)
    arr = rng.normal(size=(6, 10, 12))
    corners = np.array([[2, 3, 1], [0, 0, 0]], dtype=float)
    bs = (3, 2, 2)  # bx,by,bz
    vals = matching.extract_fixed_blocks(arr, corners, bs)
    # first block: x0=2,y0=3,z0=1
    manual = arr[1:3, 3:5, 2:5]  # z,y,x
    assert vals.shape == (2, 3 * 2 * 2)
    assert np.isclose(vals[0].sum(), manual.sum())


def test_extract_moving_integer_offset_equals_gather():
    rng = np.random.default_rng(8)
    arr = rng.normal(size=(6, 10, 12))
    corners = np.array([[3, 4, 2]], dtype=float)
    bs = (3, 3, 2)
    mov = matching.extract_moving_blocks(arr, corners, bs)
    fix = matching.extract_fixed_blocks(arr, corners.astype(int), bs)
    assert np.allclose(mov, fix)  # integer corner -> trilinear == exact


def _params(**kw):
    base = dict(metric="squared_correlation", block_half_size=(2, 2, 1),
                neighbourhood=(3, 3, 0), stride_moving=1, min_block_score=-1e9)
    base.update(kw)
    return BlockMatchingParams(**base)


def test_match_recovers_known_translation():
    rng = np.random.default_rng(9)
    ref = rng.normal(size=(5, 50, 50)).astype(np.float64)
    shift = (2, -1, 0)  # x, y, z within neighbourhood
    mov = np.roll(ref, shift=(shift[2], shift[1], shift[0]), axis=(0, 1, 2))

    p = _params()
    bs, nb, sm, ls = p.block_size, p.neighbourhood, p.stride_moving, (4, 4, 1)
    blk = blocks.select_by_variance(
        blocks.generate_blocks(ref, bs, nb, sm, ls), 100, -1.0)
    fixed, moving, scores = matching.match_blocks(
        ref, mov, blk, p, step_factors=(1.0, 1.0, 1.0), vox_sizes=(1.0, 1.0, 1.0))

    disp = moving - fixed
    # the dominant displacement should equal the true shift for the majority
    med = np.median(disp, axis=0)
    assert np.allclose(med, [shift[0], shift[1], shift[2]])
    assert np.median(scores) > 0.9  # strong match


def test_java_bug_flag_changes_anisotropic_y():
    rng = np.random.default_rng(10)
    ref = rng.normal(size=(5, 40, 40)).astype(np.float64)
    mov = np.roll(ref, shift=(0, -1, 2), axis=(0, 1, 2))
    p = _params()
    blk = blocks.select_by_variance(
        blocks.generate_blocks(ref, p.block_size, p.neighbourhood, p.stride_moving, (4, 4, 1)),
        100, -1.0)
    # anisotropic step factors (sy != sz) -> the Y-coordinate bug shows up
    kw = dict(step_factors=(1.0, 1.0, 2.0), vox_sizes=(1.0, 1.0, 1.0))
    _, mov_fixed, _ = matching.match_blocks(ref, mov, blk, p, replicate_java_bugs=False, **kw)
    _, mov_buggy, _ = matching.match_blocks(ref, mov, blk, p, replicate_java_bugs=True, **kw)
    # same offsets found, but Y coordinate scaled differently between the two
    assert not np.allclose(mov_fixed[:, 1], mov_buggy[:, 1])
