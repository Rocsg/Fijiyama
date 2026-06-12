"""Parameters of the 3D block-matching registration.

Defaults mirror RegistrationAction.java / BlockMatchingRegistration.java.
"""
from dataclasses import dataclass, field

from . import pyramid

TRANSFORM_TYPES = ("translation", "rigid", "similarity", "affine", "dense")
METRIC_TYPES = ("squared_correlation", "correlation", "meansquare")


@dataclass
class BlockMatchingParams:
    transform_type: str = "rigid"
    metric: str = "squared_correlation"

    # Pyramid
    level_min: int = 2
    level_max: int = 2
    n_iterations: int = 8
    no_subscale_z: bool = True  # auto-forced to False when width < 4 * nslices

    # Blocks (x, y, z)
    block_half_size: tuple = (7, 7, 1)
    neighbourhood: tuple = (2, 2, 1)
    stride: tuple = (3, 3, 3)
    stride_moving: int = 1
    force_2d: bool = False  # if True, override neighbourhood[2]=0

    # Selection
    pct_variance: int = 50
    pct_score: int = 80
    pct_lts: int = 70
    min_block_variance: float = None  # None => auto from reference image
    min_block_score: float = 0.1

    # Smoothing
    sigma_smoothing_pixels: float = 0.0
    sigma_dense_field_mm: float = 0.0
    sigma_dense_ratio: float = None  # if set, sigma_mm = physical_x / ratio
    uniform_field_interp: bool = False  # use box filter instead of Gaussian

    # Execution
    n_threads: int = 12

    # Backgrounds (None => auto from images)
    ref_default_value: float = None
    mov_default_value: float = None

    def __post_init__(self):
        if self.transform_type not in TRANSFORM_TYPES:
            raise ValueError(f"transform_type must be one of {TRANSFORM_TYPES}")
        if self.metric not in METRIC_TYPES:
            raise ValueError(f"metric must be one of {METRIC_TYPES}")
        if self.level_min > self.level_max:  # Java clamps silently
            self.level_min = self.level_max
        for name in ("block_half_size", "neighbourhood", "stride"):
            v = tuple(getattr(self, name))
            if len(v) != 3 or any(int(c) != c for c in v):
                raise ValueError(f"{name} must be 3 integers")
            setattr(self, name, tuple(int(c) for c in v))
        if self.force_2d:
            self.neighbourhood = (self.neighbourhood[0], self.neighbourhood[1], 0)
        if any(s < 1 for s in self.stride):
            raise ValueError("stride components must be >= 1")
        for name in ("pct_variance", "pct_score", "pct_lts"):
            if not 0 <= getattr(self, name) <= 100:
                raise ValueError(f"{name} must be in [0, 100]")
        if self.n_iterations < 1:
            raise ValueError("n_iterations must be >= 1")

    @property
    def nb_levels(self):
        return self.level_max - self.level_min + 1

    @property
    def block_size(self):
        return tuple(2 * b + 1 for b in self.block_half_size)

    def resolve_auto(self, ref_dims, ref_vox_sizes=None):
        """Apply Java auto-rules requiring reference dims: (width, height, nslices)."""
        if ref_dims[0] < ref_dims[2] * 4:
            self.no_subscale_z = False
        # Auto sigma for dense field: physical_x / ratio (default ratio=12,
        # matching Java RegistrationAction l.364: sigmaDense = sizeX / 12).
        if self.sigma_dense_field_mm == 0.0 and self.transform_type == 'dense' \
                and ref_vox_sizes is not None:
            ratio = self.sigma_dense_ratio if self.sigma_dense_ratio is not None else 12.0
            self.sigma_dense_field_mm = ref_dims[0] * ref_vox_sizes[0] / ratio

    def levels(self, ref_dims, ref_vox_sizes):
        """Per-level derived quantities, coarsest first.

        Returns dict with sub_factors, step_factors, dims, vox_sizes,
        smoothing_sigmas, dense_field_sigmas.
        """
        sub = pyramid.sub_sampling_factors(self.level_min, self.level_max)
        return {
            "sub_factors": sub,
            "step_factors": pyramid.step_factors(self.level_min, self.level_max),
            "dims": pyramid.image_dims(ref_dims, sub, self.no_subscale_z),
            "vox_sizes": pyramid.vox_sizes(ref_vox_sizes, sub, self.no_subscale_z),
            "smoothing_sigmas": pyramid.smoothing_sigmas(
                ref_vox_sizes, sub, self.sigma_smoothing_pixels),
            "dense_field_sigmas": pyramid.dense_field_sigmas(
                sub, self.sigma_dense_field_mm),
        }
