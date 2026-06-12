"""Block similarity metrics, batched ports of BlockMatchingRegistration.java.

All functions accept arrays of shape (..., n) where n is the flattened block
size, and return scores of shape (...).
"""
import numpy as np

_EPS_SUM = 1e-19  # Java: epsilon = 10E-20
_EPS_DEN = 1e-9   # Java: 10E-10 guard on denominator terms


def correlation_from_sums(sx, sy, sxy, sxx, syy, n):
    """Pearson correlation from block sums, with the Java guards."""
    vx = n * sxx - sx * sx
    vy = n * syy - sy * sy
    with np.errstate(divide="ignore", invalid="ignore"):
        r = (n * sxy - sx * sy) / np.sqrt(vx * vy)
    bad = ((sxx < _EPS_SUM) | (syy < _EPS_SUM)
           | (np.abs(vx) < _EPS_DEN) | (np.abs(vy) < _EPS_DEN)
           | ~np.isfinite(r))
    return np.where(bad, 0.0, r)


def block_score_from_sums(sx, sy, sxy, sxx, syy, n, metric):
    """computeBlockScore from precomputed block sums: higher is always better."""
    if metric == "correlation":
        return correlation_from_sums(sx, sy, sxy, sxx, syy, n)
    if metric == "squared_correlation":
        r = correlation_from_sums(sx, sy, sxy, sxx, syy, n)
        return r * r
    if metric == "meansquare":
        return -(sxx - 2.0 * sxy + syy) / n
    raise ValueError(f"unknown metric {metric!r}")


def correlation(x, y):
    """Pearson correlation with the Java guards (degenerate blocks -> 0)."""
    x = np.asarray(x, dtype=np.float64)
    y = np.asarray(y, dtype=np.float64)
    n = x.shape[-1]
    sx = x.sum(-1)
    sy = y.sum(-1)
    sxy = np.einsum("...i,...i->...", x, y)
    sxx = np.einsum("...i,...i->...", x, x)
    syy = np.einsum("...i,...i->...", y, y)
    r = correlation_from_sums(sx, sy, sxy, sxx, syy, n)
    return r if r.ndim else float(r)


def mean_square_difference(x, y):
    x = np.asarray(x, dtype=np.float64)
    y = np.asarray(y, dtype=np.float64)
    d = x - y
    out = np.einsum("...i,...i->...", d, d) / x.shape[-1]
    return out if out.ndim else float(out)


def block_score(x, y, metric):
    """Port of computeBlockScore: higher is always better."""
    if metric == "correlation":
        return correlation(x, y)
    if metric == "squared_correlation":
        r = correlation(x, y)
        return r * r
    if metric == "meansquare":
        return -mean_square_difference(x, y)
    raise ValueError(f"unknown metric {metric!r}")
