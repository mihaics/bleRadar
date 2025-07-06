package com.bleradar.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.bleradar.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    val suspiciousDevices = deviceRepository.getSuspiciousDevices(0.5f)
    val trackedDevices = deviceRepository.getTrackedDevices()
}