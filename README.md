# BLE Guardian - BLE Tracker Detection App

BLE Guardian is a native Android application designed to detect and analyze nearby Bluetooth Low Energy (BLE) devices to identify potential unauthorized trackers. It runs a background service to continuously scan for BLE devices, logs their proximity and location, and uses a sophisticated analysis engine to detect suspicious behavior, such as a device consistently following the user.

## Features

### Core BLE Detection
*   **Background BLE Scanning:** Runs a persistent background service to scan for BLE devices even when the app is not in the foreground.
*   **Device Logging:** Records all detected BLE devices, including their signal strength (RSSI), manufacturer, and the time and location of each detection.
*   **Emergency Scanning:** Tap-to-scan functionality for immediate BLE device detection.

### Advanced Device Management
*   **Smart Device List:** Comprehensive device list with multiple sorting options (last seen, first seen, name, signal strength, threat level).
*   **New Device Detection:** Visual indicators for devices discovered within the last 24 hours.
*   **Device Search & Filtering:** Search by name/address and filter to show only new devices.
*   **Device Labeling:** Custom labels for organizing and identifying specific devices.
*   **Device Tracking:** Mark devices for enhanced monitoring and analysis.

### Individual Device Analysis
*   **Detailed Device Profiles:** Complete device information including manufacturer, type, and detection statistics.
*   **Location History Timeline:** Full chronological history of where each device was detected.
*   **Threat Assessment:** Visual threat level indicators with detailed following scores and risk analysis.
*   **Detection Statistics:** Comprehensive analytics including average RSSI, signal variation, and movement patterns.

### Intelligent Threat Detection
*   **Advanced Following Detection:** Sophisticated analysis engine that calculates "following scores" based on:
    *   **Detection Frequency:** How often a device is detected.
    *   **Location Correlation:** Proximity of the device to the user over time.
    *   **Movement Similarity:** Compares the movement patterns of the device and the user.
    *   **Temporal Proximity:** How closely in time detections occur relative to the user's presence.
    *   **Multi-factor Analysis:** Combines multiple behavioral indicators for accurate threat assessment.
*   **Real-time Alerts:** Notifications when devices exhibit suspicious tracking behavior.
*   **Known Tracker Detection:** Identifies common tracker types (AirTags, SmartTags, etc.).

### Interactive Mapping
*   **Enhanced Map View:** Visualizes BLE device detections on an interactive map (powered by OpenStreetMap).
*   **Device-Map Synchronization:** Seamless navigation between device list and map locations.
*   **Location Focusing:** Jump directly to specific device locations on the map.
*   **Historical Tracking:** View movement patterns and detection history geographically.

### Data Management & Privacy
*   **Configurable Data Retention:** Customizable retention periods for detection data (1 week to 1 year).
*   **Automatic Data Cleanup:** Scheduled cleanup of old data to prevent database bloat.
*   **Manual Data Management:** On-demand database clearing and size monitoring.
*   **Privacy Controls:** Local data storage with user-controlled retention policies.

### Modern User Experience
*   **Material 3 Design:** Modern, intuitive interface following Google's latest design guidelines.
*   **Responsive Navigation:** Smooth transitions between screens with contextual actions.
*   **Real-time Updates:** Live scanning status and device information updates.
*   **Accessibility Features:** Designed for users with various accessibility needs.

## Tech Stack

*   **UI:** Jetpack Compose with Material 3
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Hilt (Dagger)
*   **Database:** Room with automatic migrations
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Background Processing:** WorkManager and foreground service
*   **Navigation:** Compose Navigation with type-safe arguments
*   **Mapping:** OSMdroid for interactive maps
*   **Permissions:** Accompanist Permissions
*   **Data Persistence:** SharedPreferences with SettingsManager
*   **Scheduled Tasks:** WorkManager for automated data cleanup

## Application Architecture

### Core Components
*   **BLE GuardianService:** Background service for continuous BLE scanning with configurable intervals
*   **SettingsManager:** Persistent configuration management using SharedPreferences
*   **DeviceRepository:** Data access layer with Room database operations
*   **LocationTracker:** GPS location tracking with accuracy filtering
*   **AdvancedTrackerDetector:** AI-powered threat detection with multi-factor analysis
*   **WorkManagerScheduler:** Automated data cleanup and maintenance tasks

### Key Screens
*   **DeviceListScreen:** Enhanced device management with sorting, filtering, and search
*   **DeviceDetailScreen:** Individual device analysis with location history timeline
*   **MapScreen:** Interactive mapping with device location visualization
*   **SettingsScreen:** Comprehensive configuration including data retention policies

### Data Flow
1. **BLE Scanning:** Background service continuously scans for devices
2. **Data Collection:** Device detections stored with location and timestamp
3. **Analysis Engine:** Advanced algorithms calculate threat scores and patterns
4. **User Interface:** Real-time updates across all screens with reactive UI
5. **Data Management:** Automated cleanup based on user-configured retention policies

## Building the Project

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/mihaics/bleRadar.git
    ```
2.  **Open in Android Studio:**
    Open the project in the latest version of Android Studio.
3.  **Build the project:**
    Android Studio should automatically sync the Gradle files and download the required dependencies. Once the sync is complete, you can build the project by clicking `Build > Make Project`.
4.  **Run the app:**
    You can run the app on an Android emulator or a physical device. Note that BLE scanning requires a physical device with Bluetooth enabled.

### Build Commands
```bash
# Project validation (works without Android SDK)
./validate-project.sh

# Build with Android SDK
./gradlew build
./gradlew assembleDebug
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## Permissions

The application requires the following permissions to function correctly:

*   **ACCESS_FINE_LOCATION:** For precise location tracking and BLE scanning
*   **ACCESS_COARSE_LOCATION:** For general location services
*   **BLUETOOTH_SCAN:** For BLE device scanning (Android 12+)
*   **BLUETOOTH_CONNECT:** For BLE device connections (Android 12+)
*   **FOREGROUND_SERVICE:** For background scanning service
*   **RECEIVE_BOOT_COMPLETED:** For auto-start on device boot
*   **WAKE_LOCK:** For maintaining scanning during device sleep

### Important Notes
*   BLE scanning requires a physical device with Bluetooth enabled
*   Location permissions are essential for threat detection algorithms
*   Battery optimization should be disabled for optimal performance

## Configuration

### Default Settings
*   **Scan Interval:** 5 minutes (configurable 1-60 minutes)
*   **Service Auto-start:** Disabled by default
*   **Data Retention:** 30 days (configurable 1 week to 1 year)
*   **Auto Cleanup:** Enabled by default

### Configurable Options
*   **Scan Frequency:** Adjust how often the app scans for devices
*   **Data Retention Policies:** Separate retention for detection data and location history
*   **Automatic Cleanup:** Schedule automatic removal of old data
*   **Device Tracking:** Mark specific devices for enhanced monitoring

## Recent Enhancements (v2.0)

### ✅ Completed Features
*   **Enhanced Device Management:** Smart sorting, filtering, and search capabilities
*   **Individual Device Analysis:** Detailed device profiles with location history timelines
*   **Advanced UI/UX:** Material 3 design with improved navigation and accessibility
*   **Data Management:** Configurable retention policies and automated cleanup
*   **Map Integration:** Seamless device-map synchronization with location focusing

### 🚧 Current Development
*   **Enhanced Tracker Detection:** Advanced multi-factor analysis algorithms
*   **Storage Optimization:** Improved database performance and storage monitoring
*   **Real-time Analysis:** More frequent threat assessment updates

### 🔮 Future Roadmap
*   **Advanced Tracker Signature Detection:** More sophisticated identification of known trackers like AirTags, SmartTags, and Tiles
*   **Machine Learning Integration:** AI-powered pattern recognition for improved threat detection
*   **Cloud-based Threat Intelligence:** Shared threat database and community-driven detection
*   **Enhanced Analytics:** Advanced reporting and trend analysis
*   **Export Capabilities:** Data export for security analysis and reporting

## Version History

### v2.0 (Current)
- Complete UI overhaul with Material 3 design
- Advanced device management and analysis
- Configurable data retention and cleanup
- Enhanced map integration
- Improved threat detection algorithms

### v1.0 (Original)
- Basic BLE scanning and device detection
- Simple device list and map view
- Background service implementation
- Initial threat detection capabilities
