package com.bleradar.di

import android.content.Context
import com.bleradar.data.database.BleRadarDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BleRadarDatabase {
        return BleRadarDatabase.getDatabase(context)
    }
}