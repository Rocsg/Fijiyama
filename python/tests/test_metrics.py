import numpy as np

from blockmatching import metrics


def test_correlation_matches_numpy():
    rng = np.random.default_rng(0)
    x = rng.normal(size=(5, 27))
    y = rng.normal(size=(5, 27))
    got = metrics.correlation(x, y)
    ref = [np.corrcoef(a, b)[0, 1] for a, b in zip(x, y)]
    assert np.allclose(got, ref)


def test_correlation_degenerate_blocks_return_zero():
    flat = np.zeros(27)
    const = np.full(27, 3.0)
    x = np.arange(27.0)
    assert metrics.correlation(flat, x) == 0.0
    assert metrics.correlation(const, x) == 0.0  # zero variance
    assert metrics.correlation(x, x) == 1.0


def test_block_score():
    rng = np.random.default_rng(1)
    x = rng.normal(size=27)
    y = -x  # anticorrelated
    r = metrics.correlation(x, y)
    assert np.isclose(metrics.block_score(x, y, "squared_correlation"), r * r)
    assert metrics.block_score(x, y, "correlation") < 0
    assert metrics.block_score(x, x, "meansquare") == 0.0
    assert metrics.block_score(x, y, "meansquare") < 0
