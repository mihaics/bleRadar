package com.bleradar.location;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class LocationTracker_Factory implements Factory<LocationTracker> {
  private final Provider<Context> contextProvider;

  public LocationTracker_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LocationTracker get() {
    return newInstance(contextProvider.get());
  }

  public static LocationTracker_Factory create(Provider<Context> contextProvider) {
    return new LocationTracker_Factory(contextProvider);
  }

  public static LocationTracker newInstance(Context context) {
    return new LocationTracker(context);
  }
}
