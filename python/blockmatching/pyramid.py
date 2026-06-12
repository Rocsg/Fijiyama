"""Pyramid-level helpers, faithful ports of BlockMatchingRegistration.java
``*AtSuccessiveLevels`` methods (doubleIterAtCoarsestLevels=false path only,
which is the only one used by the algorithm).

Levels are ordered coarsest first, like in the Java code.
"""
import math


def _java_round(x):
    """Java Math.round(double): floor(x + 0.5)."""
    return int(math.floor(x + 0.5))


def sub_sampling_factors(level_min, level_max, basis=2.0):
    """Port of subSamplingFactorsAtSuccessiveLevels (integer factors, clamped >= 1)."""
    if level_max < level_min:
        level_max = level_min
    n = level_max - level_min + 1
    ret = [0] * n
    for i in range(n):
        ret[n - 1 - i] = max(1, _java_round(basis ** (level_min - 1 + i)))
    return ret


def step_factors(level_min, level_max, basis=2.0):
    """Port of successiveStepFactorsAtSuccessiveLevels (fractional below 1 => subpixel search).

    The -0.0001 offset on the two finest entries reproduces the Java intent that
    a raw factor of 0.5 is not rounded up to 1.
    """
    if level_max < level_min:
        level_max = level_min
    n = level_max - level_min + 1
    ret = [0.0] * n
    for i in range(n):
        p = basis ** (level_min - 1 + i)
        r = _java_round((p - 0.0001) if i < 2 else p)
        if r < 1:
            # Java quirk kept as-is: for i >= 2 the fallback is the rounded int, not p
            ret[n - 1 - i] = p if i < 2 else float(r)
        else:
            ret[n - 1 - i] = 1.0
    return ret


def image_dims(dims, sub_factors, no_subscale_z):
    """Port of imageDimsAtSuccessiveLevels.

    Note: the Java code does `(int)Math.ceil(int/int)` which is *integer division*
    (truncation), not a true ceil. Reproduced with //.
    """
    out = []
    for f in sub_factors:
        if no_subscale_z:
            out.append([dims[0] // f, dims[1] // f, dims[2]])
        else:
            out.append([dims[0] // f, dims[1] // f, dims[2] // f])
    return out


def vox_sizes(voxs, sub_factors, no_subscale_z):
    """Port of imageVoxSizesAtSuccessiveLevels."""
    out = []
    for f in sub_factors:
        if no_subscale_z:
            out.append([voxs[0] * f, voxs[1] * f, voxs[2]])
        else:
            out.append([voxs[0] * f, voxs[1] * f, voxs[2] * f])
    return out


def smoothing_sigmas(voxs, sub_factors, sigma_ratio):
    """Port of sigmaFactorsAtSuccessiveLevels (physical units)."""
    vox_min = min(voxs)
    return [vox_min * f * sigma_ratio for f in sub_factors]


def dense_field_sigmas(sub_factors, sigma):
    """Port of sigmaDenseFieldAtSuccessiveLevels (constant across levels)."""
    return [sigma for _ in sub_factors]
