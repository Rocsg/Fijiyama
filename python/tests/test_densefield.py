import numpy as np

from blockmatching import densefield as df


def test_single_point_field_recovers_displacement_at_center():
    dims = (21, 21, 1)
    vox = (1.0, 1.0, 1.0)
    ref = np.array([[10.0, 10.0, 0.0]])
    disp = np.array([2.0, -3.0, 0.0])
    mov = ref + disp
    field = df.compute_dense_field(ref, mov, dims, vox, sigma_mm=2.0)
    assert field.shape == (1, 21, 21, 3)
    # at the splat voxel the normalized displacement equals the input
    np.testing.assert_allclose(field[0, 10, 10], disp, atol=1e-5)


def test_empty_regions_are_zero():
    dims = (15, 15, 1)
    vox = (1.0, 1.0, 1.0)
    ref = np.array([[7.0, 7.0, 0.0]])
    mov = ref + np.array([1.0, 1.0, 0.0])
    field = df.compute_dense_field(ref, mov, dims, vox, sigma_mm=1.0)
    # far corner, well outside the gaussian support -> zero
    assert np.allclose(field[0, 0, 0], 0.0)


def test_out_of_bounds_points_ignored():
    dims = (11, 11, 1)
    vox = (1.0, 1.0, 1.0)
    ref = np.array([[100.0, 100.0, 0.0]])  # rounds outside the volume
    mov = ref + np.array([5.0, 5.0, 0.0])
    field = df.compute_dense_field(ref, mov, dims, vox, sigma_mm=2.0)
    assert np.allclose(field, 0.0)


def test_anisotropic_voxels_scale_sigma():
    # smoke test: anisotropic voxel sizes must not raise and produce finite field
    dims = (12, 12, 4)
    vox = (0.5, 0.5, 2.0)
    ref = np.array([[3.0, 3.0, 4.0]])
    mov = ref + np.array([1.0, 0.0, -1.0])
    field = df.compute_dense_field(ref, mov, dims, vox, sigma_mm=1.0)
    assert np.all(np.isfinite(field))
    # value at the splat voxel ~ displacement
    iz = round(4.0 / 2.0)
    np.testing.assert_allclose(field[iz, 6, 6], [1.0, 0.0, -1.0], atol=1e-4)
