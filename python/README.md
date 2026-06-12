# Pyjiyama — Python block-matching registration

Python port of the Fijiyama block-matching registration algorithm (3D rigid,
similarity, affine and dense displacement-field).  Outputs are fully compatible
with the Java Fijiyama plugin.

---

## Requirements

```
numpy  scipy  SimpleITK  tifffile
```

Voxel calibration is read automatically from ImageJ-TIFF and OME-TIFF metadata.

---

## Quickstart — two-step rigid → dense workflow

### Step 1 — Rigid registration

```bash
python -m blockmatching.cli \
    ref.tif  mov.tif \
    --transform-type rigid \
    --level-min 0  --level-max 2 \
    --iterations 12 \
    -o  results/rigid.txt \
    --warp-output  results/mov_rigid.tif
```

Output:
- `results/rigid.txt` — ITK `AffineTransform` (readable by Fijiyama and SimpleITK)
- `results/mov_rigid.tif` — moving image resampled onto the reference grid

---

### Step 2 — Dense registration (starting from the rigid result)

Pass the rigid transform as `--initial-transform` so the dense step only
corrects the residual non-rigid deformation.

```bash
python -m blockmatching.cli \
    ref.tif  results/mov_rigid.tif \
    --transform-type dense \
    --level-min 1  --level-max 1 \
    --iterations 6 \
    --uniform-field-interp \
    -o  results/dense.transform.tif \
    --warp-output  results/mov_dense.tif
```

> **`--uniform-field-interp`** uses a box filter instead of a Gaussian to
> interpolate the sparse block correspondences into a dense field.  18× faster
> for large smoothing kernels at no visible quality cost.

The dense output follows Fijiyama's four-file layout:
```
results/dense.transform.tif   ← norm image (transform identifier)
results/dense.x.tif           ← X displacement component (mm, float32)
results/dense.y.tif           ← Y displacement component (mm, float32)
results/dense.z.tif           ← Z displacement component (mm, float32)
```

---

### Step 3 — Apply a saved transform to another image

#### Apply a linear transform

```bash
python -m blockmatching.cli \
    ref.tif  other_channel.tif \
    --transform-type rigid \
    --iterations 0 \
    --initial-transform  results/rigid.txt \
    --warp-output  results/other_channel_rigid.tif
```

Setting `--iterations 0` skips registration entirely — the initial transform is
applied as-is.

#### Apply a dense displacement field

Dense fields are composed with the CompositeTransform already stored in the ITK
`.h5` / `.tfm` format.  For the Fijiyama four-file layout use SimpleITK directly:

```python
import SimpleITK as sitk
from blockmatching.itk_io import read_image, get_dims, get_vox_sizes, resample
from blockmatching.itk_io import save_dense_field_as_tif

ref   = read_image("ref.tif")
other = read_image("other_channel.tif")
field = sitk.ReadTransform("results/dense.transform.tif")   # via ITK reader

warped = resample(other,
                  vox_sizes=get_vox_sizes(ref),
                  out_dims=get_dims(ref),
                  transform=field,
                  default_value=0.0)
sitk.WriteImage(warped, "results/other_channel_dense.tif")
```

---

## All CLI options

```
usage: blockmatching REF MOV [options]

Positional arguments
  reference                 Fixed / reference image (.tif)
  moving                    Moving image (.tif)

Output
  -o / --output PATH        Transform output.
                              Linear  → ITK AffineTransform (.txt / .tfm)
                              Dense   → must end with .transform.tif
  --save-dense-field PATH   Also flatten the composite as a Java-readable
                            dense field (path must end with .transform.tif).
  --warp-output PATH        Write moving image warped onto the reference grid.

Transform type
  --transform-type          translation | rigid | similarity | affine | dense
                            (default: rigid)
  --metric                  squared_correlation | correlation | meansquare
                            (default: squared_correlation)

Pyramid
  --level-min N             Finest pyramid level (0 = native resolution).
  --level-max N             Coarsest pyramid level (default 2 → ×4 in XY).
  --iterations N            Iterations per level (default 8).
  --subscale-z              Allow Z subsampling (default: Z is never subsampled).

Block matching
  --block-half-size X Y Z   Half-size of matching blocks in voxels (default 7 7 1).
  --neighbourhood   X Y Z   Search radius in stride units (default 2 2 1).
  --stride          X Y Z   Block grid spacing in voxels (default 3 3 3).
  --stride-moving   N       Moving-image search step (default 1).
  --2d-matching             Force neighZ = 0 (in-plane matching only).

Block selection
  --pct-variance    N       Keep top N % of blocks by variance (default 50).
  --pct-score       N       Keep top N % of matches by score (default 80).
  --pct-lts         N       LTS trimming percentage (default 70).
  --min-block-variance F    Minimum variance threshold (default: auto).
  --min-block-score F       Minimum correlation score (default 0.1).

Dense-field smoothing
  --sigma-dense-field MM          Gaussian/box σ in mm (default: auto = image_x/12).
  --sigma-dense-field-by-ratio R  σ = physical_x / R  (e.g. 5 → coarser, 20 → finer).
  --uniform-field-interp          Use box filter instead of Gaussian (18× faster).

Performance
  --threads N               CPU threads for parallel block groups (default 12).

Misc
  --initial-transform PATH  Seed registration with a prior transform.
  --mask PATH               Binary mask restricting the fixed blocks.
  --sigma-smoothing PX      Pre-smoothing sigma in pixels before matching.
  --ref-default-value F     Fill value for reference outside FOV.
  --mov-default-value F     Fill value for moving outside FOV.
  --replicate-java-bugs     Reproduce two anisotropic-step quirks from Java.
```

---

## Typical commands for anisotropic lightsheet / confocal data

Images are read with correct voxel calibration from ImageJ-TIFF metadata
automatically (no need to specify voxel sizes manually).

```bash
# Quick rigid (coarse pyramid, 8 iter)
python -m blockmatching.cli ref.tif mov.tif \
    --transform-type rigid  --level-min 1 --level-max 2 --iterations 8 \
    -o rigid.txt  --warp-output mov_rigid.tif

# Careful rigid (native + coarse, 20 iter each)
python -m blockmatching.cli ref.tif mov.tif \
    --transform-type rigid  --level-min 0 --level-max 2 --iterations 20 \
    -o rigid.txt  --warp-output mov_rigid.tif

# Dense refinement on top of rigid
python -m blockmatching.cli ref.tif mov_rigid.tif \
    --transform-type dense  --level-min 1 --level-max 1 --iterations 6 \
    --sigma-dense-field-by-ratio 5  --uniform-field-interp \
    -o dense.transform.tif  --warp-output mov_dense.tif
```
