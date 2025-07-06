package com.bleradar.ui.viewmodel;

import com.bleradar.location.LocationTracker;
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
public final class MapViewModel_Factory implements Factory<MapViewModel> {
  private final Provider<DeviceRepository> deviceRepositoryProvider;

  private final Provider<LocationTracker> locationTrackerProvider;

  public MapViewModel_Factory(Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<LocationTracker> locationTrackerProvider) {
    this.deviceRepositoryProvider = deviceRepositoryProvider;
    this.locationTrackerProvider = locationTrackerProvider;
  }

  @Override
  public MapViewModel get() {
    return newInstance(deviceRepositoryProvider.get(), locationTrackerProvider.get());
  }

  public static MapViewModel_Factory create(Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<LocationTracker> locationTrackerProvider) {
    return new MapViewModel_Factory(deviceRepositoryProvider, locationTrackerProvider);
  }

  public static MapViewModel newInstance(DeviceRepository deviceRepository,
      LocationTracker locationTracker) {
    return new MapViewModel(deviceRepository, locationTracker);
  }
}
