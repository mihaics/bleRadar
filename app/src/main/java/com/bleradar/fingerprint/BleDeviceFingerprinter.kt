package com.bleradar.fingerprint

import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.util.SparseArray
import com.bleradar.data.database.*
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * BLE Device Fingerprinter - Creates unique device fingerprints based on advertising characteristics
 * Handles MAC address randomization by identifying devices through stable characteristics
 */
@Singleton
class BleDeviceFingerprinter @Inject constructor(
    private val deviceFingerprintDao: DeviceFingerprintDao,
    private val deviceMacAddressDao: DeviceMacAddressDao,
    private val deviceAdvertisingDataDao: DeviceAdvertisingDataDao,
    private val fingerprintPatternDao: FingerprintPatternDao,
    private val fingerprintMatchDao: FingerprintMatchDao
) {
    
    companion object {
        private const val FINGERPRINT_MATCH_THRESHOLD = 0.7f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val ADVERTISING_INTERVAL_TOLERANCE = 500L // milliseconds
        private const val MANUFACTURER_DATA_WEIGHT = 0.3f
        private const val SERVICE_UUID_WEIGHT = 0.25f
        private const val ADVERTISING_TIMING_WEIGHT = 0.2f
        private const val SIGNAL_PATTERN_WEIGHT = 0.15f
        private const val DEVICE_NAME_WEIGHT = 0.1f
        
        private val KNOWN_TRACKER_SERVICES = setOf(
            "0000FE9F-0000-1000-8000-00805F9B34FB", // Apple AirTag
            "0000FD5A-0000-1000-8000-00805F9B34FB", // Samsung SmartTag
            "0000FEED-0000-1000-8000-00805F9B34FB", // Tile
            "0000180F-0000-1000-8000-00805F9B34FB", // Battery Service (common in trackers)
            "0000180A-0000-1000-8000-00805F9B34FB"  // Device Information Service
        )
    }
    
    /**
     * Process a BLE scan result for unknown MAC addresses - runs fingerprinting
     */
    suspend fun processScanResult(scanResult: ScanResult, timestamp: Long): DeviceProcessingResult {
        val macAddress = scanResult.device.address
        val scanRecord = scanResult.scanRecord
        
        // NOTE: This method should only be called for unknown MAC addresses
        // Fast path for known MAC addresses should use processKnownDevice()
        
        android.util.Log.d("BleDeviceFingerprinter", "Processing unknown MAC: $macAddress")
        
        // Create candidate fingerprint for new device
        val candidateFingerprint = createCandidateFingerprint(scanResult, timestamp)
        android.util.Log.d("BleDeviceFingerprinter", "Created candidate fingerprint: MAC=$macAddress, Name=${candidateFingerprint.deviceName}, ManufacturerData=${candidateFingerprint.manufacturerData.size} entries")
        
        // Try to match with existing devices using fingerprinting
        val matchResult = findMatchingDevice(candidateFingerprint, scanRecord)
        android.util.Log.d("BleDeviceFingerprinter", "Fingerprint match result: isMatch=${matchResult.isMatch}, deviceUuid=${matchResult.deviceUuid}, score=${matchResult.matchScore}")
        
        return if (matchResult.isMatch) {
            // Merge with existing device (different MAC, same device)
            android.util.Log.d("BleDeviceFingerprinter", "Merging with existing device: ${matchResult.deviceUuid}")
            mergeWithExistingDevice(matchResult.deviceUuid!!, candidateFingerprint, scanResult, timestamp)
        } else {
            // Create completely new device
            android.util.Log.d("BleDeviceFingerprinter", "Creating new device for MAC: $macAddress")
            createNewDevice(candidateFingerprint, scanResult, timestamp)
        }
    }
    
    /**
     * Fast path for known MAC addresses - bypasses fingerprinting entirely
     */
    suspend fun processKnownDevice(deviceUuid: String, scanResult: ScanResult, timestamp: Long): DeviceProcessingResult {
        android.util.Log.d("BleDeviceFingerprinter", "Processing known device: UUID=$deviceUuid, MAC=${scanResult.device.address}")
        return updateExistingDevice(deviceUuid, scanResult, timestamp)
    }
    
    /**
     * Create a candidate fingerprint from scan result
     */
    private fun createCandidateFingerprint(scanResult: ScanResult, timestamp: Long): CandidateFingerprint {
        val scanRecord = scanResult.scanRecord
        val macAddress = scanResult.device.address
        
        return CandidateFingerprint(
            macAddress = macAddress,
            deviceName = scanResult.device.name ?: scanRecord?.deviceName,
            manufacturerData = extractManufacturerData(scanRecord),
            serviceUuids = extractServiceUuids(scanRecord),
            serviceData = extractServiceData(scanRecord),
            txPower = scanRecord?.txPowerLevel,
            appearance = null, // ScanRecord doesn't have appearance property
            flags = scanRecord?.advertiseFlags,
            rssi = scanResult.rssi,
            timestamp = timestamp
        )
    }
    
    /**
     * Find matching device based on fingerprint characteristics
     */
    private suspend fun findMatchingDevice(
        candidate: CandidateFingerprint,
        @Suppress("UNUSED_PARAMETER") scanRecord: ScanRecord?
    ): MatchResult {
        val existingDevices = deviceFingerprintDao.getAllDeviceFingerprints().first()
        val candidateHash = generateFingerprintHash(candidate)

        var bestMatch: String? = null
        var bestScore = 0f

        for (device in existingDevices) {
            val matchScore = if (device.fingerprintHash == candidateHash) {
                1.0f
            } else {
                calculateMatchScore(candidate, device)
            }

            if (matchScore > bestScore && matchScore >= FINGERPRINT_MATCH_THRESHOLD) {
                bestScore = matchScore
                bestMatch = device.deviceUuid
            }
        }

        return MatchResult(
            isMatch = bestMatch != null,
            deviceUuid = bestMatch,
            matchScore = bestScore
        )
    }
    
    /**
     * Calculate match score between candidate and existing device
     */
    private suspend fun calculateMatchScore(
        candidate: CandidateFingerprint,
        existing: DeviceFingerprint
    ): Float {
        var totalScore = 0f
        var totalWeight = 0f
        
        // Compare manufacturer data
        val existingMfgData = deviceAdvertisingDataDao.getAdvertisingDataByType(
            existing.deviceUuid,
            AdvertisingDataType.MANUFACTURER_DATA
        )
        if (existingMfgData != null && candidate.manufacturerData.isNotEmpty()) {
            val mfgScore = compareManufacturerData(candidate.manufacturerData, existingMfgData.dataValue)
            totalScore += mfgScore * MANUFACTURER_DATA_WEIGHT
            totalWeight += MANUFACTURER_DATA_WEIGHT
        }
        
        // Compare service UUIDs
        val existingServiceUuids = deviceAdvertisingDataDao.getAdvertisingDataByType(
            existing.deviceUuid,
            AdvertisingDataType.SERVICE_UUIDS
        )
        if (existingServiceUuids != null && candidate.serviceUuids.isNotEmpty()) {
            val serviceScore = compareServiceUuids(candidate.serviceUuids, existingServiceUuids.dataValue)
            totalScore += serviceScore * SERVICE_UUID_WEIGHT
            totalWeight += SERVICE_UUID_WEIGHT
        }
        
        // Compare device name
        if (candidate.deviceName != null && existing.deviceName != null) {
            val nameScore = if (candidate.deviceName == existing.deviceName) 1f else 0f
            totalScore += nameScore * DEVICE_NAME_WEIGHT
            totalWeight += DEVICE_NAME_WEIGHT
        }
        
        // Compare advertising timing pattern
        val timingPattern = fingerprintPatternDao.getPatternByType(
            existing.deviceUuid,
            FingerprintPatternType.ADVERTISING_TIMING_PATTERN
        )
        if (timingPattern != null) {
            val timingScore = compareAdvertisingTiming(candidate, timingPattern)
            totalScore += timingScore * ADVERTISING_TIMING_WEIGHT
            totalWeight += ADVERTISING_TIMING_WEIGHT
        }
        
        return if (totalWeight > 0) totalScore / totalWeight else 0f
    }
    
    /**
     * Compare manufacturer data between candidate and existing
     */
    private fun compareManufacturerData(
        candidateData: Map<Int, ByteArray>,
        existingDataJson: String
    ): Float {
        try {
            val existingData = parseManufacturerDataJson(existingDataJson)
            var matches = 0
            var total = 0
            
            for ((companyId, candidateBytes) in candidateData) {
                total++
                val existingBytes = existingData[companyId]
                if (existingBytes != null && candidateBytes.contentEquals(existingBytes)) {
                    matches++
                }
            }
            
            return if (total > 0) matches.toFloat() / total else 0f
        } catch (e: Exception) {
            return 0f
        }
    }
    
    /**
     * Compare service UUIDs between candidate and existing
     */
    private fun compareServiceUuids(
        candidateUuids: List<String>,
        existingUuidsJson: String
    ): Float {
        try {
            val existingUuids = parseServiceUuidsJson(existingUuidsJson)
            val candidateSet = candidateUuids.toSet()
            val existingSet = existingUuids.toSet()
            
            val intersection = candidateSet.intersect(existingSet).size
            val union = candidateSet.union(existingSet).size
            
            return if (union > 0) intersection.toFloat() / union else 0f
        } catch (e: Exception) {
            return 0f
        }
    }
    
    /**
     * Compare advertising timing patterns
     */
    private fun compareAdvertisingTiming(
        @Suppress("UNUSED_PARAMETER") candidate: CandidateFingerprint,
        @Suppress("UNUSED_PARAMETER") timingPattern: FingerprintPattern
    ): Float {
        // This would compare advertising intervals, but requires historical data
        // For now, return moderate score if pattern exists
        return 0.5f
    }
    
    /**
     * Update existing device with new detection
     */
    private suspend fun updateExistingDevice(
        deviceUuid: String,
        scanResult: ScanResult,
        timestamp: Long
    ): DeviceProcessingResult {
        val macAddress = scanResult.device.address
        
        // Update MAC address activity
        deviceMacAddressDao.updateMacAddressActivity(macAddress, timestamp)
        
        // Update device fingerprint
        val device = deviceFingerprintDao.getDeviceFingerprint(deviceUuid)
        if (device != null) {
            val updatedDevice = device.copy(
                lastSeen = timestamp,
                totalDetections = device.totalDetections + 1
            )
            deviceFingerprintDao.updateDeviceFingerprint(updatedDevice)
        }
        
        // Update advertising data
        updateAdvertisingData(deviceUuid, scanResult.scanRecord, timestamp)
        
        return DeviceProcessingResult(
            deviceUuid = deviceUuid,
            isNewDevice = false,
            macAddress = macAddress,
            matchScore = 1f
        )
    }
    
    /**
     * Merge candidate with existing device
     */
    private suspend fun mergeWithExistingDevice(
        existingDeviceUuid: String,
        candidate: CandidateFingerprint,
        scanResult: ScanResult,
        timestamp: Long
    ): DeviceProcessingResult {
        // Add new MAC address to existing device
        val macAddress = DeviceMacAddress(
            deviceUuid = existingDeviceUuid,
            macAddress = candidate.macAddress,
            firstSeen = timestamp,
            lastSeen = timestamp,
            detectionCount = 1,
            isActive = true,
            addressType = determineMacAddressType(candidate.macAddress)
        )
        deviceMacAddressDao.insertMacAddress(macAddress)
        
        // Update device fingerprint
        val device = deviceFingerprintDao.getDeviceFingerprint(existingDeviceUuid)
        if (device != null) {
            val updatedDevice = device.copy(
                lastSeen = timestamp,
                totalDetections = device.totalDetections + 1,
                macAddressCount = device.macAddressCount + 1
            )
            deviceFingerprintDao.updateDeviceFingerprint(updatedDevice)
        }
        
        // Update advertising data
        updateAdvertisingData(existingDeviceUuid, scanResult.scanRecord, timestamp)
        
        return DeviceProcessingResult(
            deviceUuid = existingDeviceUuid,
            isNewDevice = false,
            macAddress = candidate.macAddress,
            matchScore = 1f
        )
    }
    
    /**
     * Create new device fingerprint
     */
    private suspend fun createNewDevice(
        candidate: CandidateFingerprint,
        @Suppress("UNUSED_PARAMETER") scanResult: ScanResult,
        timestamp: Long
    ): DeviceProcessingResult {
        val deviceUuid = UUID.randomUUID().toString()
        
        // Create device fingerprint
        val fingerprintHash = generateFingerprintHash(candidate)
        val deviceType = determineDeviceType(candidate)
        val isKnownTracker = isKnownTrackerDevice(candidate)
        
        val device = DeviceFingerprint(
            deviceUuid = deviceUuid,
            deviceName = candidate.deviceName,
            manufacturer = extractManufacturerName(candidate.manufacturerData),
            deviceType = deviceType,
            isKnownTracker = isKnownTracker,
            trackerType = if (isKnownTracker) determineTrackerType(candidate) else null,
            firstSeen = timestamp,
            lastSeen = timestamp,
            suspiciousScore = if (isKnownTracker) 0.8f else 0.1f,
            followingScore = 0f,
            totalDetections = 1,
            macAddressCount = 1,
            fingerprintHash = fingerprintHash,
            confidence = calculateInitialConfidence(candidate)
        )
        
        deviceFingerprintDao.insertDeviceFingerprint(device)
        
        // Add MAC address
        val macAddress = DeviceMacAddress(
            deviceUuid = deviceUuid,
            macAddress = candidate.macAddress,
            firstSeen = timestamp,
            lastSeen = timestamp,
            detectionCount = 1,
            isActive = true,
            addressType = determineMacAddressType(candidate.macAddress)
        )
        deviceMacAddressDao.insertMacAddress(macAddress)
        
        // Store advertising data
        storeAdvertisingData(deviceUuid, candidate, timestamp)
        
        // Create fingerprint patterns
        createFingerprintPatterns(deviceUuid, candidate, timestamp)
        
        return DeviceProcessingResult(
            deviceUuid = deviceUuid,
            isNewDevice = true,
            macAddress = candidate.macAddress,
            matchScore = 1f
        )
    }
    
    /**
     * Update advertising data for existing device
     */
    private suspend fun updateAdvertisingData(
        deviceUuid: String,
        scanRecord: ScanRecord?,
        timestamp: Long
    ) {
        scanRecord ?: return
        
        // Update manufacturer data
        val manufacturerData = extractManufacturerData(scanRecord)
        if (manufacturerData.isNotEmpty()) {
            deviceAdvertisingDataDao.updateAdvertisingDataActivity(
                deviceUuid,
                AdvertisingDataType.MANUFACTURER_DATA,
                timestamp
            )
        }
        
        // Update service UUIDs
        val serviceUuids = extractServiceUuids(scanRecord)
        if (serviceUuids.isNotEmpty()) {
            deviceAdvertisingDataDao.updateAdvertisingDataActivity(
                deviceUuid,
                AdvertisingDataType.SERVICE_UUIDS,
                timestamp
            )
        }
        
        // Update other advertising data types as needed
    }
    
    /**
     * Store advertising data for new device
     */
    private suspend fun storeAdvertisingData(
        deviceUuid: String,
        candidate: CandidateFingerprint,
        timestamp: Long
    ) {
        // Store manufacturer data
        if (candidate.manufacturerData.isNotEmpty()) {
            val manufacturerDataJson = serializeManufacturerData(candidate.manufacturerData)
            val advertisingData = DeviceAdvertisingData(
                deviceUuid = deviceUuid,
                dataType = AdvertisingDataType.MANUFACTURER_DATA,
                dataValue = manufacturerDataJson,
                firstSeen = timestamp,
                lastSeen = timestamp,
                occurrenceCount = 1,
                isStable = true
            )
            deviceAdvertisingDataDao.insertAdvertisingData(advertisingData)
        }
        
        // Store service UUIDs
        if (candidate.serviceUuids.isNotEmpty()) {
            val serviceUuidsJson = serializeServiceUuids(candidate.serviceUuids)
            val advertisingData = DeviceAdvertisingData(
                deviceUuid = deviceUuid,
                dataType = AdvertisingDataType.SERVICE_UUIDS,
                dataValue = serviceUuidsJson,
                firstSeen = timestamp,
                lastSeen = timestamp,
                occurrenceCount = 1,
                isStable = true
            )
            deviceAdvertisingDataDao.insertAdvertisingData(advertisingData)
        }
        
        // Store device name
        candidate.deviceName?.let { name ->
            val advertisingData = DeviceAdvertisingData(
                deviceUuid = deviceUuid,
                dataType = AdvertisingDataType.DEVICE_NAME,
                dataValue = name,
                firstSeen = timestamp,
                lastSeen = timestamp,
                occurrenceCount = 1,
                isStable = true
            )
            deviceAdvertisingDataDao.insertAdvertisingData(advertisingData)
        }
    }
    
    /**
     * Create fingerprint patterns for new device
     */
    private suspend fun createFingerprintPatterns(
        deviceUuid: String,
        candidate: CandidateFingerprint,
        timestamp: Long
    ) {
        // Create service UUID pattern
        if (candidate.serviceUuids.isNotEmpty()) {
            val pattern = FingerprintPattern(
                deviceUuid = deviceUuid,
                patternType = FingerprintPatternType.SERVICE_UUID_PATTERN,
                patternData = serializeServiceUuids(candidate.serviceUuids),
                confidence = 0.9f,
                lastUpdated = timestamp,
                isMatchable = true
            )
            fingerprintPatternDao.insertPattern(pattern)
        }
        
        // Create manufacturer data pattern
        if (candidate.manufacturerData.isNotEmpty()) {
            val pattern = FingerprintPattern(
                deviceUuid = deviceUuid,
                patternType = FingerprintPatternType.MANUFACTURER_DATA_PATTERN,
                patternData = serializeManufacturerData(candidate.manufacturerData),
                confidence = 0.8f,
                lastUpdated = timestamp,
                isMatchable = true
            )
            fingerprintPatternDao.insertPattern(pattern)
        }
        
        // Create combined signature pattern
        val combinedPattern = FingerprintPattern(
            deviceUuid = deviceUuid,
            patternType = FingerprintPatternType.COMBINED_SIGNATURE,
            patternData = generateCombinedSignature(candidate),
            confidence = calculateInitialConfidence(candidate),
            lastUpdated = timestamp,
            isMatchable = true
        )
        fingerprintPatternDao.insertPattern(combinedPattern)
    }
    
    // Helper functions
    private fun extractManufacturerData(scanRecord: ScanRecord?): Map<Int, ByteArray> {
        val manufacturerData = mutableMapOf<Int, ByteArray>()
        scanRecord?.let { record ->
            for (i in 0 until record.manufacturerSpecificData.size()) {
                val companyId = record.manufacturerSpecificData.keyAt(i)
                val data = record.manufacturerSpecificData.valueAt(i)
                manufacturerData[companyId] = data
            }
        }
        return manufacturerData
    }
    
    private fun extractServiceUuids(scanRecord: ScanRecord?): List<String> {
        val serviceUuids = mutableListOf<String>()
        scanRecord?.serviceUuids?.forEach { uuid ->
            serviceUuids.add(uuid.toString().uppercase())
        }
        return serviceUuids
    }
    
    private fun extractServiceData(scanRecord: ScanRecord?): Map<String, ByteArray> {
        val serviceData = mutableMapOf<String, ByteArray>()
        scanRecord?.serviceData?.forEach { (uuid, data) ->
            serviceData[uuid.toString().uppercase()] = data
        }
        return serviceData
    }
    
    private fun generateFingerprintHash(candidate: CandidateFingerprint): String {
        val digest = MessageDigest.getInstance("SHA-256")

        // Sort manufacturer data by company ID for stable order
        val sortedMfgData = candidate.manufacturerData.toSortedMap()
            .map { (id, bytes) -> "$id:${bytes.joinToString("") { "%02x".format(it) }}" }
            .joinToString(";")

        // Sort service UUIDs for stable order
        val sortedServiceUuids = candidate.serviceUuids.sorted().joinToString(",")

        val combined = "mfg:$sortedMfgData|svc:$sortedServiceUuids|name:${candidate.deviceName ?: "null"}"

        return digest.digest(combined.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    
    private fun determineDeviceType(candidate: CandidateFingerprint): DeviceType {
        // Check for known tracker service UUIDs
        for (serviceUuid in candidate.serviceUuids) {
            when {
                serviceUuid.contains("FE9F") -> return DeviceType.AIRTAG
                serviceUuid.contains("FD5A") -> return DeviceType.SMARTTAG
                serviceUuid.contains("FEED") -> return DeviceType.TILE
            }
        }
        
        // Check manufacturer data
        for ((companyId, _) in candidate.manufacturerData) {
            when (companyId) {
                0x004C -> return DeviceType.AIRTAG // Apple
                0x0075 -> return DeviceType.SMARTTAG // Samsung
                0x00B3 -> return DeviceType.TILE // Tile
            }
        }
        
        return DeviceType.UNKNOWN
    }
    
    private fun isKnownTrackerDevice(candidate: CandidateFingerprint): Boolean {
        return candidate.serviceUuids.any { uuid ->
            KNOWN_TRACKER_SERVICES.contains(uuid)
        }
    }
    
    private fun determineTrackerType(candidate: CandidateFingerprint): String? {
        for (serviceUuid in candidate.serviceUuids) {
            when {
                serviceUuid.contains("FE9F") -> return "AirTag"
                serviceUuid.contains("FD5A") -> return "SmartTag"
                serviceUuid.contains("FEED") -> return "Tile"
            }
        }
        return null
    }
    
    private fun calculateInitialConfidence(candidate: CandidateFingerprint): Float {
        var confidence = 0.5f
        
        if (candidate.serviceUuids.isNotEmpty()) confidence += 0.2f
        if (candidate.manufacturerData.isNotEmpty()) confidence += 0.2f
        if (candidate.deviceName != null) confidence += 0.1f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun determineMacAddressType(macAddress: String): MacAddressType {
        // Check if MAC address is randomized based on the first octet
        val firstOctet = macAddress.substring(0, 2).toInt(16)
        val isRandomized = (firstOctet and 0x02) != 0
        
        return if (isRandomized) {
            MacAddressType.RANDOM_RESOLVABLE
        } else {
            MacAddressType.STATIC
        }
    }
    
    private fun extractManufacturerName(manufacturerData: Map<Int, ByteArray>): String? {
        return when {
            manufacturerData.containsKey(0x004C) -> "Apple"
            manufacturerData.containsKey(0x0075) -> "Samsung"
            manufacturerData.containsKey(0x00B3) -> "Tile"
            else -> null
        }
    }
    
    private fun serializeManufacturerData(data: Map<Int, ByteArray>): String {
        // Simple JSON serialization for manufacturer data
        return data.map { (key, value) ->
            "\"$key\":\"${value.joinToString("") { "%02x".format(it) }}\""
        }.joinToString(",", "{", "}")
    }
    
    private fun serializeServiceUuids(uuids: List<String>): String {
        return uuids.joinToString(",", "[", "]") { "\"$it\"" }
    }
    
    private fun parseManufacturerDataJson(json: String): Map<Int, ByteArray> {
        // Simple JSON parsing - in production, use proper JSON library
        val result = mutableMapOf<Int, ByteArray>()
        try {
            // Basic parsing for the format we use: {"key":"hexvalue"}
            if (json.startsWith("{") && json.endsWith("}")) {
                val content = json.substring(1, json.length - 1)
                val pairs = content.split(",")
                for (pair in pairs) {
                    val keyValue = pair.split(":")
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim().removeSurrounding("\"")
                        val value = keyValue[1].trim().removeSurrounding("\"")
                        try {
                            val keyInt = key.toInt()
                            val bytes = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            result[keyInt] = bytes
                        } catch (e: Exception) {
                            // Skip invalid entries
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty map on parse error
        }
        return result
    }
    
    private fun parseServiceUuidsJson(json: String): List<String> {
        // Simple JSON parsing - in production, use proper JSON library
        try {
            if (json.startsWith("[") && json.endsWith("]")) {
                val content = json.substring(1, json.length - 1)
                if (content.isBlank()) return emptyList()
                return content.split(",").map { it.trim().removeSurrounding("\"") }
            }
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        return emptyList()
    }
    
    private fun generateCombinedSignature(candidate: CandidateFingerprint): String {
        return mapOf(
            "manufacturer" to candidate.manufacturerData.keys.joinToString(","),
            "services" to candidate.serviceUuids.joinToString(","),
            "name" to (candidate.deviceName ?: ""),
            "appearance" to (candidate.appearance?.toString() ?: "")
        ).toString()
    }
}

data class CandidateFingerprint(
    val macAddress: String,
    val deviceName: String?,
    val manufacturerData: Map<Int, ByteArray>,
    val serviceUuids: List<String>,
    val serviceData: Map<String, ByteArray>,
    val txPower: Int?,
    val appearance: Int?,
    val flags: Int?,
    val rssi: Int,
    val timestamp: Long
)

data class MatchResult(
    val isMatch: Boolean,
    val deviceUuid: String?,
    val matchScore: Float
)

data class DeviceProcessingResult(
    val deviceUuid: String,
    val isNewDevice: Boolean,
    val macAddress: String,
    val matchScore: Float
)