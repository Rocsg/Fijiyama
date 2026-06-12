import numpy as np
import pytest
from scipy.ndimage import gaussian_filter

from blockmatching import cli
from blockmatching import itk_io as io

sitk = pytest.importorskip("SimpleITK")
tifffile = pytest.importorskip("tifffile")


def _smooth_field(shape, seed=0, sigma=2.0):
    rng = np.random.default_rng(seed)
    return gaussian_filter(rng.normal(size=shape), sigma).astype(np.float64)


def test_parser_maps_to_params():
    args = cli.build_parser().parse_args(
        ["ref.tif", "mov.tif", "--transform-type", "affine",
         "--block-half-size", "5", "5", "2", "--stride", "4", "4", "4",
         "--subscale-z", "--iterations", "3"])
    p = cli.params_from_args(args)
    assert p.transform_type == "affine"
    assert p.block_half_size == (5, 5, 2)
    assert p.stride == (4, 4, 4)
    assert p.no_subscale_z is False
    assert p.n_iterations == 3


def test_save_dense_field_layout_and_roundtrip(tmp_path):
    # constant translation transform -> displacement equals the translation
    t = sitk.TranslationTransform(3, (1.5, -2.0, 0.5))
    ref = sitk.GetImageFromArray(np.zeros((4, 8, 8), dtype=np.float32))
    ref.SetSpacing((0.5, 0.5, 2.0))

    path = str(tmp_path / "result.transform.tif")
    io.save_dense_field_as_tif(t, ref, path)

    short = str(tmp_path / "result")
    for suffix in (".x.tif", ".y.tif", ".z.tif", ".transform.tif"):
        assert (tmp_path / ("result" + suffix)).exists()

    fx = tifffile.imread(short + ".x.tif")
    fy = tifffile.imread(short + ".y.tif")
    fz = tifffile.imread(short + ".z.tif")
    assert fx.shape == (4, 8, 8)
    np.testing.assert_allclose(fx, 1.5, atol=1e-4)
    np.testing.assert_allclose(fy, -2.0, atol=1e-4)
    np.testing.assert_allclose(fz, 0.5, atol=1e-4)


def test_dense_path_must_have_suffix(tmp_path):
    t = sitk.TranslationTransform(3, (0.0, 0.0, 0.0))
    ref = sitk.GetImageFromArray(np.zeros((2, 4, 4), dtype=np.float32))
    with pytest.raises(ValueError):
        io.save_dense_field_as_tif(t, ref, str(tmp_path / "bad.tif"))


def test_cli_end_to_end_writes_outputs(tmp_path):
    field = _smooth_field((6, 40, 40), seed=5, sigma=2.0)
    ref_arr = field[:, 4:36, 4:36]
    mov_arr = field[:, 4:36, 6:38]  # mov[x] = ref[x+2]

    ref_path = str(tmp_path / "ref.tif")
    mov_path = str(tmp_path / "mov.tif")
    sitk.WriteImage(sitk.GetImageFromArray(ref_arr.astype(np.float32)), ref_path)
    sitk.WriteImage(sitk.GetImageFromArray(mov_arr.astype(np.float32)), mov_path)

    out = str(tmp_path / "tr.txt")
    dense = str(tmp_path / "tr.transform.tif")
    warp = str(tmp_path / "warped.tif")

    rc = cli.main([
        ref_path, mov_path,
        "--transform-type", "translation",
        "--level-min", "0", "--level-max", "0", "--iterations", "3",
        "--block-half-size", "3", "3", "1",
        "--neighbourhood", "2", "2", "0",
        "--stride", "2", "2", "2",
        "--pct-score", "90", "--pct-lts", "100",
        "--min-block-score", "0.3",
        "-o", out, "--save-dense-field", dense, "--warp-output", warp,
    ])
    assert rc == 0
    assert (tmp_path / "tr.txt").exists()
    assert (tmp_path / "tr.x.tif").exists()
    assert (tmp_path / "warped.tif").exists()

    tr = sitk.ReadTransform(out)
    p = np.array(tr.TransformPoint((16.0, 16.0, 3.0)))
    assert np.isclose(p[0] - 16.0, -2.0, atol=0.7)
