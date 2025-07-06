package com.bleradar.service;

import com.bleradar.data.database.BleRadarDatabase;
import com.bleradar.location.LocationTracker;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BleRadarService_MembersInjector implements MembersInjector<BleRadarService> {
  private final Provider<BleRadarDatabase> databaseProvider;

  private final Provider<LocationTracker> locationTrackerProvider;

  public BleRadarService_MembersInjector(Provider<BleRadarDatabase> databaseProvider,
      Provider<LocationTracker> locationTrackerProvider) {
    this.databaseProvider = databaseProvider;
    this.locationTrackerProvider = locationTrackerProvider;
  }

  public static MembersInjector<BleRadarService> create(Provider<BleRadarDatabase> databaseProvider,
      Provider<LocationTracker> locationTrackerProvider) {
    return new BleRadarService_MembersInjector(databaseProvider, locationTrackerProvider);
  }

  @Override
  public void injectMembers(BleRadarService instance) {
    injectDatabase(instance, databaseProvider.get());
    injectLocationTracker(instance, locationTrackerProvider.get());
  }

  @InjectedFieldSignature("com.bleradar.service.BleRadarService.database")
  public static void injectDatabase(BleRadarService instance, BleRadarDatabase database) {
    instance.database = database;
  }

  @InjectedFieldSignature("com.bleradar.service.BleRadarService.locationTracker")
  public static void injectLocationTracker(BleRadarService instance,
      LocationTracker locationTracker) {
    instance.locationTracker = locationTracker;
  }
}
