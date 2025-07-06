package com.bleradar.ui.viewmodel;

import android.content.Context;
import com.bleradar.repository.DeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<DeviceRepository> deviceRepositoryProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<DeviceRepository> deviceRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.deviceRepositoryProvider = deviceRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), deviceRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<DeviceRepository> deviceRepositoryProvider) {
    return new SettingsViewModel_Factory(contextProvider, deviceRepositoryProvider);
  }

  public static SettingsViewModel newInstance(Context context, DeviceRepository deviceRepository) {
    return new SettingsViewModel(context, deviceRepository);
  }
}
