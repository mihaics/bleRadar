package com.bleradar.ui.viewmodel;

import com.bleradar.analysis.FollowingDetector;
import com.bleradar.repository.DeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class DeviceListViewModel_Factory implements Factory<DeviceListViewModel> {
  private final Provider<DeviceRepository> deviceRepositoryProvider;

  private final Provider<FollowingDetector> followingDetectorProvider;

  public DeviceListViewModel_Factory(Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<FollowingDetector> followingDetectorProvider) {
    this.deviceRepositoryProvider = deviceRepositoryProvider;
    this.followingDetectorProvider = followingDetectorProvider;
  }

  @Override
  public DeviceListViewModel get() {
    return newInstance(deviceRepositoryProvider.get(), followingDetectorProvider.get());
  }

  public static DeviceListViewModel_Factory create(
      Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<FollowingDetector> followingDetectorProvider) {
    return new DeviceListViewModel_Factory(deviceRepositoryProvider, followingDetectorProvider);
  }

  public static DeviceListViewModel newInstance(DeviceRepository deviceRepository,
      FollowingDetector followingDetector) {
    return new DeviceListViewModel(deviceRepository, followingDetector);
  }
}
