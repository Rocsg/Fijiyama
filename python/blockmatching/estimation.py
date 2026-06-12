"""Transform estimation from correspondence point sets, and the score / LTS
trimming steps.

Ports of ItkTransform.estimateBest*3D (which delegate to vib.FastMatrix.bestRigid,
Horn's quaternion method) and of getCorrespondanceListAsTrimmedPointArray.

All estimators take (ref_pts, mov_pts) in *physical* coordinates and return a
4x4 homogeneous matrix mapping ref -> mov (the resampling pull-back transform),
matching the Java call estimateBest*(corr[1]=mov, corr[0]=ref) -> bestRigid(ref, mov).

Fijiyama quirks reproduced
--------------------------
* "affine" and "similarity" both estimate a *similarity* (rigid + isotropic
  scale): Java AFFINE calls bestRigid(..., allowScaling=true), like SIMILARITY.
* is_identity only checks off-diagonal rotation terms and translation; the
  diagonal check is commented out in Java (so a pure scaling reads as identity).
"""
import numpy as np


def _best_rigid(set1, set2, allow_scaling):
    """Horn's quaternion absolute-orientation method. Maps set1 -> set2.

    Returns a 4x4 homogeneous matrix M such that M @ [set1;1] ~= [set2;1].
    """
    set1 = np.asarray(set1, dtype=np.float64)
    set2 = np.asarray(set2, dtype=np.float64)
    n = set1.shape[0]
    c1 = set1.mean(axis=0)
    c2 = set2.mean(axis=0)
    p1 = set1 - c1
    p2 = set2 - c2

    s = 1.0
    if allow_scaling:
        r1 = np.sum(p1 * p1)
        r2 = np.sum(p2 * p2)
        s = np.sqrt(r2 / r1) if r1 > 0 else 1.0

    # S = s * p1^T p2  (columns x,y,z)
    S = s * (p1.T @ p2)
    Sxx, Sxy, Sxz = S[0]
    Syx, Syy, Syz = S[1]
    Szx, Szy, Szz = S[2]

    N = np.array([
        [Sxx + Syy + Szz, Syz - Szy,       Szx - Sxz,        Sxy - Syx],
        [Syz - Szy,       Sxx - Syy - Szz, Sxy + Syx,        Szx + Sxz],
        [Szx - Sxz,       Sxy + Syx,       -Sxx + Syy - Szz, Syz + Szy],
        [Sxy - Syx,       Szx + Sxz,       Syz + Szy,        -Sxx - Syy + Szz],
    ], dtype=np.float64)

    eigvals, eigvecs = np.linalg.eigh(N)  # ascending; columns are eigenvectors
    q = eigvecs[:, int(np.argmax(eigvals))]
    q0, qx, qy, qz = q

    M = s * np.array([
        [q0*q0 + qx*qx - qy*qy - qz*qz, 2*(qx*qy - q0*qz),             2*(qx*qz + q0*qy)],
        [2*(qy*qx + q0*qz),             q0*q0 - qx*qx + qy*qy - qz*qz, 2*(qy*qz - q0*qx)],
        [2*(qz*qx - q0*qy),             2*(qz*qy + q0*qx),             q0*q0 - qx*qx - qy*qy + qz*qz],
    ], dtype=np.float64)

    t = c2 - M @ c1
    out = np.eye(4)
    out[:3, :3] = M
    out[:3, 3] = t
    return out


def estimate_translation(ref_pts, mov_pts):
    ref_pts = np.asarray(ref_pts, dtype=np.float64)
    mov_pts = np.asarray(mov_pts, dtype=np.float64)
    t = (mov_pts - ref_pts).mean(axis=0)
    out = np.eye(4)
    out[:3, 3] = t
    return out


def estimate_rigid(ref_pts, mov_pts):
    return _best_rigid(ref_pts, mov_pts, allow_scaling=False)


def estimate_similarity(ref_pts, mov_pts):
    return _best_rigid(ref_pts, mov_pts, allow_scaling=True)


# Fijiyama "affine" is in fact a similarity.
estimate_affine = estimate_similarity


def estimate_transform(transform_type, ref_pts, mov_pts):
    if transform_type == "translation":
        return estimate_translation(ref_pts, mov_pts)
    if transform_type == "rigid":
        return estimate_rigid(ref_pts, mov_pts)
    if transform_type in ("similarity", "affine"):
        return estimate_similarity(ref_pts, mov_pts)
    raise ValueError(f"{transform_type!r} is not a linear transform type")


def _java_round(x):
    import math
    return int(math.floor(x + 0.5))


def trim_by_score(ref_pts, mov_pts, scores, pct_keep):
    """Keep the top ``pct_keep`` % correspondences by score.

    Returns (ref_kept, mov_kept, scores_kept, mean_score_after).
    """
    n = scores.shape[0]
    if n == 0:
        return ref_pts, mov_pts, scores, 0.0
    order = np.argsort(scores, kind="stable")  # ascending (lowest first)
    last_removal = _java_round(n * ((100 - pct_keep) / 100.0))
    keep = order[last_removal:]
    sc = scores[keep]
    return ref_pts[keep], mov_pts[keep], sc, float(sc.mean()) if sc.size else 0.0


def trim_by_lts(ref_pts, mov_pts, scores, matrix, pct_keep):
    """Least-trimmed-squares: keep the ``pct_keep`` % with smallest residual
    distance after applying ``matrix`` to the reference points.

    Returns (ref_kept, mov_kept, scores_kept, mean_neg_distance_after).
    """
    n = ref_pts.shape[0]
    if n == 0:
        return ref_pts, mov_pts, scores, 0.0
    transformed = (matrix[:3, :3] @ ref_pts.T).T + matrix[:3, 3]
    dist = np.linalg.norm(transformed - mov_pts, axis=1)
    # Java sorts by z=-distance ascending (largest distance first) and removes
    # the first lastRemoval -> keep smallest distances.
    order = np.argsort(-dist, kind="stable")
    last_removal = _java_round(n * ((100 - pct_keep) / 100.0))
    keep = order[last_removal:]
    d = dist[keep]
    mean_neg = float((-d).mean()) if d.size else 0.0
    return ref_pts[keep], mov_pts[keep], scores[keep], mean_neg


def is_identity(matrix, eps_rotation=1e-6, eps_translation=0.0):
    """Port of ItkTransform.isIdentityAffineTransform (diagonal check disabled)."""
    M = np.asarray(matrix, dtype=np.float64)
    for i in range(3):
        for j in range(3):
            if i != j and abs(M[i, j]) > eps_rotation:
                return False
    for i in range(3):
        if abs(M[i, 3]) > eps_translation:
            return False
    return True
