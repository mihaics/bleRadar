package com.bleradar.service;

import com.bleradar.analysis.AdvancedTrackerDetector;
import com.bleradar.data.database.BleRadarDatabase;
import com.bleradar.location.LocationTracker;
import com.bleradar.repository.DeviceRepository;
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

  private final Provider<DeviceRepository> deviceRepositoryProvider;

  private final Provider<AdvancedTrackerDetector> advancedTrackerDetectorProvider;

  public BleRadarService_MembersInjector(Provider<BleRadarDatabase> databaseProvider,
      Provider<LocationTracker> locationTrackerProvider,
      Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<AdvancedTrackerDetector> advancedTrackerDetectorProvider) {
    this.databaseProvider = databaseProvider;
    this.locationTrackerProvider = locationTrackerProvider;
    this.deviceRepositoryProvider = deviceRepositoryProvider;
    this.advancedTrackerDetectorProvider = advancedTrackerDetectorProvider;
  }

  public static MembersInjector<BleRadarService> create(Provider<BleRadarDatabase> databaseProvider,
      Provider<LocationTracker> locationTrackerProvider,
      Provider<DeviceRepository> deviceRepositoryProvider,
      Provider<AdvancedTrackerDetector> advancedTrackerDetectorProvider) {
    return new BleRadarService_MembersInjector(databaseProvider, locationTrackerProvider, deviceRepositoryProvider, advancedTrackerDetectorProvider);
  }

  @Override
  public void injectMembers(BleRadarService instance) {
    injectDatabase(instance, databaseProvider.get());
    injectLocationTracker(instance, locationTrackerProvider.get());
    injectDeviceRepository(instance, deviceRepositoryProvider.get());
    injectAdvancedTrackerDetector(instance, advancedTrackerDetectorProvider.get());
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

  @InjectedFieldSignature("com.bleradar.service.BleRadarService.deviceRepository")
  public static void injectDeviceRepository(BleRadarService instance,
      DeviceRepository deviceRepository) {
    instance.deviceRepository = deviceRepository;
  }

  @InjectedFieldSignature("com.bleradar.service.BleRadarService.advancedTrackerDetector")
  public static void injectAdvancedTrackerDetector(BleRadarService instance,
      AdvancedTrackerDetector advancedTrackerDetector) {
    instance.advancedTrackerDetector = advancedTrackerDetector;
  }
}
