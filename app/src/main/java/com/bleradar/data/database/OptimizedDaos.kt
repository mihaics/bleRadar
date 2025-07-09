package com.bleradar.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OptimizedDeviceDao {
    @Query("SELECT * FROM optimized_devices WHERE deviceAddress = :address")
    suspend fun getDevice(address: String): OptimizedDevice?

    @Query("SELECT * FROM optimized_devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<OptimizedDevice>>

    @Query("SELECT * FROM optimized_devices WHERE suspiciousScore > :threshold ORDER BY suspiciousScore DESC")
    fun getSuspiciousDevices(threshold: Float = 0.5f): Flow<List<OptimizedDevice>>

    @Query("SELECT * FROM optimized_devices WHERE isKnownTracker = 1")
    fun getKnownTrackers(): Flow<List<OptimizedDevice>>

    @Query("SELECT * FROM optimized_devices WHERE multiLocationScore > :threshold ORDER BY multiLocationScore DESC")
    fun getMultiLocationDevices(threshold: Float = 0.6f): Flow<List<OptimizedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: OptimizedDevice)

    @Update
    suspend fun updateDevice(device: OptimizedDevice)

    @Query("UPDATE optimized_devices SET suspiciousScore = :score WHERE deviceAddress = :address")
    suspend fun updateSuspiciousScore(address: String, score: Float)

    @Query("UPDATE optimized_devices SET multiLocationScore = :score WHERE deviceAddress = :address")
    suspend fun updateMultiLocationScore(address: String, score: Float)

    @Query("UPDATE optimized_devices SET totalDetections = :count WHERE deviceAddress = :address")
    suspend fun updateDetectionCount(address: String, count: Int)

    @Query("UPDATE optimized_devices SET lastSeen = :timestamp WHERE deviceAddress = :address")
    suspend fun updateLastSeen(address: String, timestamp: Long)

    @Query("DELETE FROM optimized_devices WHERE lastSeen < :cutoffTime")
    suspend fun deleteOldDevices(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM optimized_devices")
    suspend fun getDeviceCount(): Int

    @Query("SELECT COUNT(*) FROM optimized_devices WHERE suspiciousScore > :threshold")
    suspend fun getSuspiciousDeviceCount(threshold: Float = 0.5f): Int
}

@Dao
interface LocationClusterDao {
    @Query("SELECT * FROM location_clusters WHERE clusterId = :clusterId")
    suspend fun getCluster(clusterId: String): LocationCluster?

    @Query("SELECT * FROM location_clusters ORDER BY lastSeen DESC")
    fun getAllClusters(): Flow<List<LocationCluster>>

    @Query("SELECT * FROM location_clusters WHERE isUserLocation = 1 ORDER BY lastSeen DESC")
    fun getUserLocations(): Flow<List<LocationCluster>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: LocationCluster)

    @Update
    suspend fun updateCluster(cluster: LocationCluster)

    @Query("UPDATE location_clusters SET lastSeen = :timestamp, deviceCount = :deviceCount, detectionCount = :detectionCount WHERE clusterId = :clusterId")
    suspend fun updateClusterStats(clusterId: String, timestamp: Long, deviceCount: Int, detectionCount: Int)

    @Query("DELETE FROM location_clusters WHERE lastSeen < :cutoffTime")
    suspend fun deleteOldClusters(cutoffTime: Long)

    // Find nearby clusters within specified distance
    @Query("""
        SELECT * FROM location_clusters 
        WHERE (centerLat BETWEEN :minLat AND :maxLat) 
        AND (centerLon BETWEEN :minLon AND :maxLon)
        ORDER BY 
            ((centerLat - :lat) * (centerLat - :lat) + (centerLon - :lon) * (centerLon - :lon)) ASC
        LIMIT 10
    """)
    suspend fun findNearbyClusters(
        lat: Double, 
        lon: Double, 
        minLat: Double, 
        maxLat: Double, 
        minLon: Double, 
        maxLon: Double
    ): List<LocationCluster>
}

@Dao
interface DeviceClusterDetectionDao {
    @Query("SELECT * FROM device_cluster_detections WHERE deviceAddress = :address")
    suspend fun getDetectionsForDevice(address: String): List<DeviceClusterDetection>

    @Query("SELECT * FROM device_cluster_detections WHERE clusterId = :clusterId")
    suspend fun getDetectionsForCluster(clusterId: String): List<DeviceClusterDetection>

    @Query("SELECT * FROM device_cluster_detections WHERE deviceAddress = :address AND clusterId = :clusterId")
    suspend fun getDetection(address: String, clusterId: String): DeviceClusterDetection?

    @Query("SELECT COUNT(DISTINCT clusterId) FROM device_cluster_detections WHERE deviceAddress = :address")
    suspend fun getLocationCountForDevice(address: String): Int

    @Query("SELECT clusterId FROM device_cluster_detections WHERE deviceAddress = :address ORDER BY lastSeen DESC")
    suspend fun getLocationHistoryForDevice(address: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetection(detection: DeviceClusterDetection)

    @Update
    suspend fun updateDetection(detection: DeviceClusterDetection)

    @Query("""
        UPDATE device_cluster_detections 
        SET detectionCount = :count, avgRssi = :avgRssi, lastSeen = :timestamp
        WHERE deviceAddress = :address AND clusterId = :clusterId
    """)
    suspend fun updateDetectionStats(
        address: String, 
        clusterId: String, 
        count: Int, 
        avgRssi: Float, 
        timestamp: Long
    )

    @Query("DELETE FROM device_cluster_detections WHERE lastSeen < :cutoffTime")
    suspend fun deleteOldDetections(cutoffTime: Long)

    // Key query for multi-location tracking detection
    @Query("""
        SELECT deviceAddress, COUNT(DISTINCT clusterId) as locationCount,
               MAX(lastSeen) - MIN(firstSeen) as timeSpan,
               AVG(detectionCount) as avgDetections
        FROM device_cluster_detections 
        WHERE lastSeen > :since
        GROUP BY deviceAddress
        HAVING locationCount > 1
        ORDER BY locationCount DESC, timeSpan DESC
    """)
    suspend fun getMultiLocationDevices(since: Long): List<MultiLocationResult>
}

@Dao
interface TrackingEvidenceDao {
    @Query("SELECT * FROM tracking_evidence WHERE deviceAddress = :address")
    suspend fun getEvidenceForDevice(address: String): List<TrackingEvidence>

    @Query("SELECT * FROM tracking_evidence WHERE evidenceType = :type AND confidence > :threshold")
    suspend fun getEvidenceByType(type: EvidenceType, threshold: Float = 0.5f): List<TrackingEvidence>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: TrackingEvidence)

    @Query("DELETE FROM tracking_evidence WHERE deviceAddress = :address AND evidenceType = :type")
    suspend fun deleteEvidence(address: String, type: EvidenceType)

    @Query("DELETE FROM tracking_evidence WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldEvidence(cutoffTime: Long)
}

@Dao
interface DeviceSummaryDao {
    @Query("SELECT * FROM device_summary WHERE deviceAddress = :address ORDER BY date DESC")
    suspend fun getSummaryForDevice(address: String): List<DeviceSummary>

    @Query("SELECT * FROM device_summary WHERE date = :date ORDER BY riskLevel DESC")
    suspend fun getSummaryForDate(date: String): List<DeviceSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: DeviceSummary)

    @Query("DELETE FROM device_summary WHERE date < :cutoffDate")
    suspend fun deleteOldSummaries(cutoffDate: String)
}

// Helper data class for multi-location query results
data class MultiLocationResult(
    val deviceAddress: String,
    val locationCount: Int,
    val timeSpan: Long,
    val avgDetections: Double
)