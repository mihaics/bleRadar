package com.bleradar.di

import android.content.Context
import com.bleradar.data.database.BleRadarDatabase
import com.bleradar.preferences.SettingsManager
import com.bleradar.repository.SimpleAnalyticsRepository
import com.bleradar.analysis.ImprovedTrackerDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BleRadarDatabase {
        return BleRadarDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSimpleAnalyticsRepository(database: BleRadarDatabase): SimpleAnalyticsRepository {
        return SimpleAnalyticsRepository(database)
    }
    
    @Provides
    @Singleton
    fun provideImprovedTrackerDetector(deviceRepository: com.bleradar.repository.DeviceRepository): ImprovedTrackerDetector {
        return ImprovedTrackerDetector(deviceRepository)
    }
}