package com.bleradar.repository;

import com.bleradar.data.database.BleRadarDatabase;
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
public final class DeviceRepository_Factory implements Factory<DeviceRepository> {
  private final Provider<BleRadarDatabase> databaseProvider;

  public DeviceRepository_Factory(Provider<BleRadarDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public DeviceRepository get() {
    return newInstance(databaseProvider.get());
  }

  public static DeviceRepository_Factory create(Provider<BleRadarDatabase> databaseProvider) {
    return new DeviceRepository_Factory(databaseProvider);
  }

  public static DeviceRepository newInstance(BleRadarDatabase database) {
    return new DeviceRepository(database);
  }
}
