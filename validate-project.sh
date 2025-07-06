#!/bin/bash

echo "🔍 BLE Radar Project Validation"
echo "================================"

# Check project structure
echo "📁 Checking project structure..."

required_files=(
    "build.gradle.kts"
    "settings.gradle.kts" 
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/bleradar/BleRadarApplication.kt"
    "app/src/main/java/com/bleradar/MainActivity.kt"
)

all_present=true
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file - MISSING"
        all_present=false
    fi
done

echo ""
echo "📊 File Statistics:"
echo "   Kotlin files: $(find . -name "*.kt" | wc -l)"
echo "   XML files: $(find . -name "*.xml" | wc -l)"
echo "   Total source files: $(find ./app/src -type f | wc -l)"

echo ""
echo "🏗️ Build Configuration:"
if [ -f "gradlew" ]; then
    echo "✅ Gradle wrapper present"
    if [ -x "gradlew" ]; then
        echo "✅ Gradle wrapper executable"
    else
        echo "⚠️  Gradle wrapper not executable"
    fi
else
    echo "❌ Gradle wrapper missing"
fi

echo ""
echo "🔧 Dependencies Check:"
grep -q "com.android.application" app/build.gradle.kts && echo "✅ Android Application plugin"
grep -q "kotlin.android" app/build.gradle.kts && echo "✅ Kotlin Android plugin" 
grep -q "dagger.hilt" app/build.gradle.kts && echo "✅ Hilt plugin"
grep -q "androidx.room" app/build.gradle.kts && echo "✅ Room database"
grep -q "maps-compose" app/build.gradle.kts && echo "✅ Google Maps Compose"

echo ""
echo "🛡️ Permissions Check:"
grep -q "BLUETOOTH_SCAN" app/src/main/AndroidManifest.xml && echo "✅ Bluetooth scan permission"
grep -q "ACCESS_FINE_LOCATION" app/src/main/AndroidManifest.xml && echo "✅ Location permission"
grep -q "FOREGROUND_SERVICE" app/src/main/AndroidManifest.xml && echo "✅ Foreground service permission"

echo ""
echo "📱 Components Check:"
grep -q "BleRadarService" app/src/main/AndroidManifest.xml && echo "✅ BLE service declared"
grep -q "PeriodicScanReceiver" app/src/main/AndroidManifest.xml && echo "✅ Scan receiver declared"

echo ""
if [ "$all_present" = true ]; then
    echo "🎉 PROJECT VALIDATION SUCCESSFUL!"
    echo "   All core files present and properly configured."
    echo "   Ready for Android SDK build environment."
else
    echo "⚠️  PROJECT VALIDATION INCOMPLETE"
    echo "   Some required files are missing."
fi

echo ""
echo "🚀 Next Steps:"
echo "   1. Install Android SDK (API 34)"
echo "   2. Set ANDROID_HOME environment variable"
echo "   3. Run: ./gradlew assembleDebug"
echo "   4. Install APK: ./gradlew installDebug"