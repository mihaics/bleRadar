# BleRadar - BLE Tracker Detection App

BleRadar is a native Android application designed to detect and analyze nearby Bluetooth Low Energy (BLE) devices to identify potential unauthorized trackers. It runs a background service to continuously scan for BLE devices, logs their proximity and location, and uses a sophisticated analysis engine to detect suspicious behavior, such as a device consistently following the user.

## Features

*   **Background BLE Scanning:** Runs a persistent background service to scan for BLE devices even when the app is not in the foreground.
*   **Device Logging:** Records all detected BLE devices, including their signal strength (RSSI), manufacturer, and the time and location of each detection.
*   **Location-based Analysis:** Correlates device detections with the user's location history to identify devices that are consistently nearby.
*   **Suspicious Behavior Detection:** Employs a `FollowingDetector` to analyze detection patterns and calculate a "following score" based on:
    *   **Detection Frequency:** How often a device is detected.
    *   **Location Correlation:** Proximity of the device to the user over time.
    *   **Movement Similarity:** Compares the movement patterns of the device and the user.
    *   **Temporal Proximity:** How closely in time detections occur relative to the user's presence.
*   **Alerts for Potential Trackers:** Notifies the user when a device exhibits behavior indicative of a tracker.
*   **Interactive Map View:** Visualizes the locations of BLE device detections on a map (powered by OpenStreetMap).
*   **Device List:** Shows a comprehensive list of all detected devices, with details about each one.
*   **Modern Android Tech Stack:** Built with Kotlin, Jetpack Compose, Hilt, Room, and Coroutines.

## Tech Stack

*   **UI:** Jetpack Compose with Material 3
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **Dependency Injection:** Hilt (Dagger)
*   **Database:** Room
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Background Processing:** WorkManager and a foreground service
*   **Navigation:** Compose Navigation
*   **Mapping:** OSMdroid
*   **Permissions:** Accompanist Permissions

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

## Permissions

The application requires the following permissions to function correctly:

*   **Bluetooth:** To scan for and connect to BLE devices.
*   **Location:** To get the user's location for correlating with BLE detections.
*   **Storage (Older Android Versions):** May be required for map caching.

## Future Enhancements

The `TRACKER_DETECTION_IMPROVEMENTS.md` file in this repository outlines a roadmap for future development, including:

*   **Advanced Tracker Signature Detection:** More sophisticated identification of known trackers like AirTags, SmartTags, and Tiles.
*   **Enhanced Database and Risk Assessment:** A more detailed database schema to store richer analysis data and a multi-level risk assessment system.
*   **Real-time Analysis:** More frequent and comprehensive analysis of BLE data to provide real-time alerts.
*   **Machine Learning Integration:** The potential to use machine learning for more accurate pattern recognition and threat detection.
*   **Cloud-based Threat Intelligence:** A system for sharing and receiving threat information from a central service.
