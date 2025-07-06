package com.bleradar.ui.viewmodel;

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
public final class AlertsViewModel_Factory implements Factory<AlertsViewModel> {
  private final Provider<DeviceRepository> deviceRepositoryProvider;

  public AlertsViewModel_Factory(Provider<DeviceRepository> deviceRepositoryProvider) {
    this.deviceRepositoryProvider = deviceRepositoryProvider;
  }

  @Override
  public AlertsViewModel get() {
    return newInstance(deviceRepositoryProvider.get());
  }

  public static AlertsViewModel_Factory create(
      Provider<DeviceRepository> deviceRepositoryProvider) {
    return new AlertsViewModel_Factory(deviceRepositoryProvider);
  }

  public static AlertsViewModel newInstance(DeviceRepository deviceRepository) {
    return new AlertsViewModel(deviceRepository);
  }
}
