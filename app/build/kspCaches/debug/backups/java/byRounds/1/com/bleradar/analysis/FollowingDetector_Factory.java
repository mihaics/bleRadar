package com.bleradar.analysis;

import com.bleradar.repository.DeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class FollowingDetector_Factory implements Factory<FollowingDetector> {
  private final Provider<DeviceRepository> deviceRepositoryProvider;

  public FollowingDetector_Factory(Provider<DeviceRepository> deviceRepositoryProvider) {
    this.deviceRepositoryProvider = deviceRepositoryProvider;
  }

  @Override
  public FollowingDetector get() {
    return newInstance(deviceRepositoryProvider.get());
  }

  public static FollowingDetector_Factory create(
      Provider<DeviceRepository> deviceRepositoryProvider) {
    return new FollowingDetector_Factory(deviceRepositoryProvider);
  }

  public static FollowingDetector newInstance(DeviceRepository deviceRepository) {
    return new FollowingDetector(deviceRepository);
  }
}
