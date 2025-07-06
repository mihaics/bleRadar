package com.bleradar;

import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedEntryPoint;

@OriginatingElement(
    topLevelClass = BleRadarApplication.class
)
@GeneratedEntryPoint
@InstallIn(SingletonComponent.class)
public interface BleRadarApplication_GeneratedInjector {
  void injectBleRadarApplication(BleRadarApplication bleRadarApplication);
}
