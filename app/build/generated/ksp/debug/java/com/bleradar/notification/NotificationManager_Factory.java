package com.bleradar.notification;

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
public final class NotificationManager_Factory implements Factory<NotificationManager> {
  private final Provider<Context> contextProvider;

  public NotificationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public NotificationManager get() {
    return newInstance(contextProvider.get());
  }

  public static NotificationManager_Factory create(Provider<Context> contextProvider) {
    return new NotificationManager_Factory(contextProvider);
  }

  public static NotificationManager newInstance(Context context) {
    return new NotificationManager(context);
  }
}
