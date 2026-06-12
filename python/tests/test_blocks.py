import numpy as np

from blockmatching import blocks


def _brute_force_blocks(ref, block_size, neigh, stride_moving, level_stride):
    """Reference reimplementation of the Java triple loop (blX,blY,blZ)."""
    Z, Y, X = ref.shape
    dims = (X, Y, Z)
    nbx, nby, nbz = blocks.block_counts(dims, block_size, neigh, stride_moving, level_stride)
    bx, by, bz = block_size
    rows = []
    for blx in range(nbx):
        for bly in range(nby):
            for blz in range(nbz):
                x0 = blx * level_stride[0] + neigh[0] * stride_moving
                y0 = bly * level_stride[1] + neigh[1] * stride_moving
                z0 = blz * level_stride[2] + neigh[2] * stride_moving
                blk = ref[z0:z0 + bz, y0:y0 + by, x0:x0 + bx]
                rows.append([blk.std(), x0, y0, z0])
    return np.array(rows, dtype=np.float64)


def test_level_strides():
    assert blocks.level_strides((3, 3, 3), [1, 1, 1]) == [3, 3, 3]
    # 3 / 8**(1/3) = 1.5, but -EPSILON makes it round DOWN to 1 (Java intent)
    assert blocks.level_strides((3, 3, 3), [8, 8, 1]) == [1, 1, 3]
    assert blocks.level_strides((4, 4, 4), [8, 8, 8]) == [2, 2, 2]


def test_generate_blocks_matches_bruteforce_3d():
    rng = np.random.default_rng(3)
    ref = rng.normal(size=(20, 40, 50)).astype(np.float64)  # Z,Y,X
    bs, nb, sm, ls = (5, 5, 3), (2, 2, 1), 1, (3, 3, 2)
    got = blocks.generate_blocks(ref, bs, nb, sm, ls)
    ref_tab = _brute_force_blocks(ref, bs, nb, sm, ls)
    assert got.shape == ref_tab.shape
    assert np.allclose(got, ref_tab)


def test_generate_blocks_single_slice():
    rng = np.random.default_rng(4)
    ref = rng.normal(size=(1, 30, 30)).astype(np.float64)
    bs, nb, sm, ls = (5, 5, 1), (2, 2, 0), 1, (2, 2, 1)
    got = blocks.generate_blocks(ref, bs, nb, sm, ls)
    assert np.all(got[:, 3] == 0)  # all z0 == 0
    assert got.shape[0] > 0


def test_trim_using_mask_corner_voxel():
    tmp = np.array([[1.0, 2, 3, 0], [2.0, 5, 5, 0]], dtype=np.float64)
    mask = np.zeros((1, 10, 10))
    mask[0, 3, 2] = 1.0  # only first block's corner is inside
    kept = blocks.trim_using_mask(tmp, mask)
    assert kept.shape[0] == 1 and kept[0, 1] == 2


def test_select_by_variance_threshold_and_pct():
    # vars 0.1..0.5; pct=60 removes bottom 40% (2 of 5); min_var=0.25
    tmp = np.array([[0.5, 0, 0, 0], [0.1, 1, 0, 0], [0.3, 2, 0, 0],
                    [0.2, 3, 0, 0], [0.4, 4, 0, 0]], dtype=np.float64)
    out = blocks.select_by_variance(tmp, pct_variance=60, min_block_variance=0.25)
    # ascending sort: 0.1,0.2,0.3,0.4,0.5 ; bottom 2 -> -1 ; keep var>=0.25 -> 0.3,0.4,0.5
    assert np.allclose(np.sort(out[:, 3]), [0.3, 0.4, 0.5])
    assert out.shape[1] == 4  # [x0,y0,z0,var]


def test_log_number_of_blocks_and_random_selection():
    assert blocks.log_number_of_blocks(100) == 300
    assert blocks.log_number_of_blocks(500) == 300 + int((500 - 300) * 0.42)
    big = np.zeros((5000, 4))
    sel = blocks.random_selection(big, seed=1)
    target = blocks.log_number_of_blocks(5000)
    assert abs(sel.shape[0] - target) < 0.2 * target  # roughly the target count
