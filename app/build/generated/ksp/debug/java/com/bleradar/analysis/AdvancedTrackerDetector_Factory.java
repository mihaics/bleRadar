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
public final class AdvancedTrackerDetector_Factory implements Factory<AdvancedTrackerDetector> {
  private final Provider<DeviceRepository> deviceRepositoryProvider;

  public AdvancedTrackerDetector_Factory(Provider<DeviceRepository> deviceRepositoryProvider) {
    this.deviceRepositoryProvider = deviceRepositoryProvider;
  }

  @Override
  public AdvancedTrackerDetector get() {
    return newInstance(deviceRepositoryProvider.get());
  }

  public static AdvancedTrackerDetector_Factory create(
      Provider<DeviceRepository> deviceRepositoryProvider) {
    return new AdvancedTrackerDetector_Factory(deviceRepositoryProvider);
  }

  public static AdvancedTrackerDetector newInstance(DeviceRepository deviceRepository) {
    return new AdvancedTrackerDetector(deviceRepository);
  }
}
