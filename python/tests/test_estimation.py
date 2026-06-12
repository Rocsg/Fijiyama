import numpy as np

from blockmatching import estimation as est


def _apply(M, pts):
    return (M[:3, :3] @ pts.T).T + M[:3, 3]


def test_translation_recovers_offset():
    rng = np.random.default_rng(0)
    ref = rng.normal(size=(20, 3))
    offset = np.array([1.5, -2.0, 0.7])
    mov = ref + offset
    M = est.estimate_translation(ref, mov)
    assert np.allclose(M[:3, 3], offset)
    assert np.allclose(_apply(M, ref), mov)


def test_rigid_recovers_rotation_translation():
    rng = np.random.default_rng(1)
    ref = rng.normal(size=(30, 3))
    theta = 0.3
    R = np.array([[np.cos(theta), -np.sin(theta), 0],
                  [np.sin(theta),  np.cos(theta), 0],
                  [0, 0, 1.0]])
    t = np.array([2.0, -1.0, 3.0])
    mov = _apply(np.block([[R, t[:, None]], [np.zeros(3), 1]]), ref)
    M = est.estimate_rigid(ref, mov)
    assert np.allclose(_apply(M, ref), mov, atol=1e-9)
    # pure rigid: no scaling
    assert np.isclose(np.linalg.det(M[:3, :3]), 1.0, atol=1e-9)


def test_similarity_recovers_scale():
    rng = np.random.default_rng(2)
    ref = rng.normal(size=(40, 3))
    s = 1.7
    theta = -0.4
    R = np.array([[np.cos(theta), 0, np.sin(theta)],
                  [0, 1, 0],
                  [-np.sin(theta), 0, np.cos(theta)]])
    t = np.array([0.5, 0.5, -0.5])
    mov = s * _apply(np.block([[R, np.zeros((3, 1))], [np.zeros(3), 1]]), ref) + t
    M = est.estimate_similarity(ref, mov)
    assert np.allclose(_apply(M, ref), mov, atol=1e-8)
    assert np.isclose(np.linalg.det(M[:3, :3]) ** (1 / 3), s, atol=1e-6)


def test_affine_is_similarity():
    assert est.estimate_affine is est.estimate_similarity


def test_trim_by_score_keeps_top_pct():
    ref = np.zeros((10, 3))
    mov = np.zeros((10, 3))
    scores = np.linspace(0.0, 0.9, 10)
    r, m, sc, mean = est.trim_by_score(ref, mov, scores, pct_keep=80)
    assert sc.shape[0] == 8  # bottom 20% removed
    assert sc.min() >= 0.2 - 1e-9  # the two lowest (0.0, 0.1) dropped


def test_trim_by_lts_keeps_small_residuals():
    rng = np.random.default_rng(3)
    ref = rng.normal(size=(10, 3))
    M = np.eye(4)
    M[:3, 3] = [1.0, 0.0, 0.0]
    mov = _apply(M, ref).copy()
    # corrupt two correspondences with large residuals
    mov[0] += 50
    mov[1] += 50
    r, m, sc, mean_neg = est.trim_by_lts(ref, mov, np.ones(10), M, pct_keep=80)
    assert r.shape[0] == 8
    # the two outliers should be gone -> residuals all ~0
    res = np.linalg.norm(_apply(M, r) - m, axis=1)
    assert np.all(res < 1e-6)


def test_is_identity_quirks():
    assert est.is_identity(np.eye(4))
    scaled = np.eye(4)
    scaled[0, 0] = scaled[1, 1] = scaled[2, 2] = 2.0  # pure scaling
    assert est.is_identity(scaled)  # Java quirk: diagonal not checked
    rot = np.eye(4)
    rot[0, 1] = 0.01
    assert not est.is_identity(rot)
    trans = np.eye(4)
    trans[0, 3] = 0.01
    assert not est.is_identity(trans, eps_translation=0.001)
