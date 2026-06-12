"""Ad-hoc check: batched match_blocks vs the previous sequential implementation
(equivalence + speed at the real problem size)."""
import time
import numpy as np

from blockmatching import blocks as B, matching as M
from blockmatching.metrics import block_score
from blockmatching.params import BlockMatchingParams


def match_blocks_reference(ref_arr, mov_arr, blk, params, step_factors, vox_sizes,
                           replicate_java_bugs=False):
    """Previous (direct interpolation) implementation, kept for comparison."""
    n = blk.shape[0]
    bsize = params.block_size
    half = params.block_half_size
    sx, sy, sz = step_factors
    vx, vy, vz = vox_sizes
    corners = blk[:, :3]
    fixed_vals = M.extract_fixed_blocks(ref_arr, corners, bsize)
    best_score = np.full(n, -np.inf)
    best_dist = np.zeros(n)
    best_off = np.zeros((n, 3))
    for (xp, yp, zp) in M._offsets(params.neighbourhood, params.stride_moving):
        mov_corners = np.empty_like(corners, dtype=np.float64)
        mov_corners[:, 0] = corners[:, 0] + xp * sx
        mov_corners[:, 1] = corners[:, 1] + yp * sy
        mov_corners[:, 2] = corners[:, 2] + zp * sz
        mov_vals = M.extract_moving_blocks(mov_arr, mov_corners, bsize)
        score = block_score(fixed_vals, mov_vals, params.metric)
        dy = (yp * vy * sy) ** 2
        dist = np.sqrt((xp * vx * sx) ** 2 + dy + (zp * vz * sz) ** 2)
        upd = (score > best_score) | ((score == best_score) & (dist < best_dist))
        best_score = np.where(upd, score, best_score)
        best_dist = np.where(upd, dist, best_dist)
        best_off[upd] = (xp, yp, zp)
    cx = corners[:, 0] + half[0]
    cy = corners[:, 1] + half[1]
    cz = corners[:, 2] + half[2]
    fixed_pts = np.stack([cx, cy, cz], axis=1)
    moving_pts = np.stack([cx + best_off[:, 0] * sx, cy + best_off[:, 1] * sy,
                           cz + best_off[:, 2] * sz], axis=1)
    keep = best_score >= params.min_block_score
    return fixed_pts[keep], moving_pts[keep], best_score[keep]


rng = np.random.default_rng(1)
from scipy.ndimage import gaussian_filter
ref = gaussian_filter(rng.normal(size=(40, 151, 148)), 1.5)
mov = np.roll(ref, shift=(0, -2, 3), axis=(0, 1, 2)) + 0.01 * rng.normal(size=ref.shape)

p = BlockMatchingParams(min_block_score=0.1)
step_factors = (0.5, 0.5, 1.0)   # fractional, like the user's level (vox 2,2,1)
vox = (2.0, 2.0, 1.0)
ls = (2, 2, 3)

blk = B.select_by_variance(
    B.generate_blocks(ref, p.block_size, p.neighbourhood, p.stride_moving, ls),
    p.pct_variance, 0.0)
print("blocks:", blk.shape[0])

t0 = time.perf_counter()
f_new, m_new, s_new = M.match_blocks(ref, mov, blk, p, step_factors, vox)
t_new = time.perf_counter() - t0

t0 = time.perf_counter()
f_old, m_old, s_old = match_blocks_reference(ref, mov, blk, p, step_factors, vox)
t_old = time.perf_counter() - t0

print(f"new: {t_new:.2f}s   old: {t_old:.2f}s   speedup: {t_old/t_new:.1f}x")
print("same number kept:", len(s_new) == len(s_old))
same_pts = np.array_equal(f_new, f_old) and np.allclose(m_new, m_old)
print("points identical:", same_pts)
print("max |score diff|:", np.abs(s_new - s_old).max() if len(s_new) == len(s_old) else "n/a")
