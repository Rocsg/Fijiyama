"""Command-line interface for the 3D block-matching registration.

Mirrors the BlockMatchingParams fields and uses the SimpleITK backend for
image I/O.  Linear results are written as a single ITK AffineTransform (readable
by Fijiyama's matrix reader); dense / requested fields are written in Fijiyama's
Java-readable 4-file layout (``<short>.x/.y/.z.tif`` + ``<short>.transform.tif``)
so they can be applied by the Java version.
"""
from __future__ import annotations

import argparse
import sys
import time

from .params import BlockMatchingParams, METRIC_TYPES, TRANSFORM_TYPES


def build_parser():
    p = argparse.ArgumentParser(
        prog="blockmatching",
        description="3D block-matching registration (core algorithm port).",
    )
    p.add_argument("reference", help="Reference (fixed) image path.")
    p.add_argument("moving", help="Moving image path.")

    p.add_argument("-o", "--output",
                   help="Output transform path. Linear -> ITK AffineTransform "
                        "(.txt/.tfm/.h5). For a dense transform this must end "
                        "with '.transform.tif'.")
    p.add_argument("--save-dense-field", metavar="PATH",
                   help="Also flatten the result on the reference grid and write "
                        "it as a Java-readable dense field (PATH must end with "
                        "'.transform.tif').")
    p.add_argument("--warp-output", metavar="PATH",
                   help="Optionally write the moving image resampled onto the "
                        "reference grid with the estimated transform.")

    p.add_argument("--transform-type", choices=TRANSFORM_TYPES, default="rigid")
    p.add_argument("--metric", choices=METRIC_TYPES, default="squared_correlation")

    p.add_argument("--level-min", type=int, default=2)
    p.add_argument("--level-max", type=int, default=2)
    p.add_argument("--iterations", type=int, default=8)

    p.add_argument("--block-half-size", type=int, nargs=3, metavar=("X", "Y", "Z"),
                   default=[7, 7, 1])
    p.add_argument("--neighbourhood", type=int, nargs=3, metavar=("X", "Y", "Z"),
                   default=[2, 2, 0])
    p.add_argument("--stride", type=int, nargs=3, metavar=("X", "Y", "Z"),
                   default=[3, 3, 3])
    p.add_argument("--stride-moving", type=int, default=1)

    p.add_argument("--pct-variance", type=int, default=50)
    p.add_argument("--pct-score", type=int, default=80)
    p.add_argument("--pct-lts", type=int, default=70)
    p.add_argument("--min-block-variance", type=float, default=None,
                   help="Default: auto (mean of central slice / 10).")
    p.add_argument("--min-block-score", type=float, default=0.1)

    p.add_argument("--sigma-smoothing", type=float, default=0.0,
                   help="Image resampling smoothing sigma, in pixels.")
    p.add_argument("--sigma-dense-field", type=float, default=0.0,
                   help="Dense-field interpolation sigma, in mm.")
    p.add_argument("--sigma-dense-field-by-ratio", type=float, default=None,
                   dest="sigma_dense_ratio",
                   help=("Dense-field sigma = physical_x / RATIO. "
                         "Overrides --sigma-dense-field when transform is dense. "
                         "Default auto-ratio is 12 (Java default)."))
    p.add_argument("--uniform-field-interp", action="store_true",
                   default=False,
                   help=("Use a box filter instead of Gaussian to interpolate "
                         "the dense displacement field (faster for large sigma)."))
    p.add_argument("--2d-matching", dest="force_2d", action="store_true",
                   default=False,
                   help=("Force 2-D block matching (neighbourhood Z = 0). "
                         "Default: neighbourhood Z = 1 to allow axial search."))

    p.add_argument("--threads", type=int, default=12)
    p.add_argument("--ref-default-value", type=float, default=None)
    p.add_argument("--mov-default-value", type=float, default=None)

    z = p.add_mutually_exclusive_group()
    z.add_argument("--no-subscale-z", dest="no_subscale_z", action="store_true",
                   default=True, help="Keep full z resolution (default).")
    z.add_argument("--subscale-z", dest="no_subscale_z", action="store_false",
                   help="Allow subsampling along z.")

    p.add_argument("--mask", help="Optional mask image restricting fixed blocks.")
    p.add_argument("--initial-transform", help="Optional initial transform file.")
    p.add_argument("--random-selection", action="store_true",
                   help="Enable the gofast random block selection.")
    p.add_argument("--replicate-java-bugs", action="store_true",
                   help="Reproduce the two anisotropic-step Java quirks.")
    return p


def params_from_args(args):
    return BlockMatchingParams(
        transform_type=args.transform_type,
        metric=args.metric,
        level_min=args.level_min,
        level_max=args.level_max,
        n_iterations=args.iterations,
        no_subscale_z=args.no_subscale_z,
        block_half_size=tuple(args.block_half_size),
        neighbourhood=tuple(args.neighbourhood),
        stride=tuple(args.stride),
        stride_moving=args.stride_moving,
        pct_variance=args.pct_variance,
        pct_score=args.pct_score,
        pct_lts=args.pct_lts,
        min_block_variance=args.min_block_variance,
        min_block_score=args.min_block_score,
        sigma_smoothing_pixels=args.sigma_smoothing,
        sigma_dense_field_mm=args.sigma_dense_field,
        sigma_dense_ratio=args.sigma_dense_ratio,
        uniform_field_interp=args.uniform_field_interp,
        force_2d=args.force_2d,
        n_threads=args.threads,
        ref_default_value=args.ref_default_value,
        mov_default_value=args.mov_default_value,
    )


def main(argv=None):
    args = build_parser().parse_args(argv)

    import SimpleITK as sitk

    from . import core
    from . import itk_io as io

    params = params_from_args(args)

    print(f"[BM] reference : {args.reference}", flush=True)
    ref_img = io.read_image(args.reference)
    print(f"[BM] moving    : {args.moving}", flush=True)
    mov_img = io.read_image(args.moving)
    print(f"[BM] images loaded. params: type={args.transform_type}  "
          f"levels={args.level_min}-{args.level_max}  "
          f"iter={args.iterations}  block_half={args.block_half_size}  "
          f"neigh={args.neighbourhood}  stride={args.stride}", flush=True)
    mask_img = io.read_image(args.mask) if args.mask else None
    initial = sitk.ReadTransform(args.initial_transform) if args.initial_transform \
        else None

    t0 = time.perf_counter()
    transform = core.register(
        ref_img, mov_img, params,
        initial_transform=initial, mask_img=mask_img,
        random_selection=args.random_selection,
        replicate_java_bugs=args.replicate_java_bugs,
    )
    elapsed = time.perf_counter() - t0
    print(f"Registration time: {elapsed:.3f} s")

    is_dense = params.transform_type == "dense"

    if args.output:
        if is_dense or args.output.endswith(io._DENSE_SUFFIX):
            io.save_dense_field_as_tif(transform, ref_img, args.output)
            print(f"Wrote dense field: {args.output}")
        else:
            affine = io.affine_from_matrix(io.composite_to_affine_matrix(transform))
            io.write_transform(affine, args.output)
            print(f"Wrote transform: {args.output}")

    if args.save_dense_field:
        io.save_dense_field_as_tif(transform, ref_img, args.save_dense_field)
        print(f"Wrote dense field: {args.save_dense_field}")

    if args.warp_output:
        ref_dims = io.get_dims(ref_img)
        ref_vox = io.get_vox_sizes(ref_img)
        default = params.mov_default_value if params.mov_default_value is not None \
            else 0.0
        warped = io.resample(mov_img, ref_vox, ref_dims, transform, default)
        io.write_image(warped, args.warp_output)
        print(f"Wrote warped moving image: {args.warp_output}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
