package com.bleradar.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BleRadarDatabase_Impl extends BleRadarDatabase {
  private volatile BleDeviceDao _bleDeviceDao;

  private volatile BleDetectionDao _bleDetectionDao;

  private volatile LocationDao _locationDao;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `ble_devices` (`deviceAddress` TEXT NOT NULL, `deviceName` TEXT, `rssi` INTEGER NOT NULL, `firstSeen` INTEGER NOT NULL, `lastSeen` INTEGER NOT NULL, `isIgnored` INTEGER NOT NULL, `label` TEXT, `isTracked` INTEGER NOT NULL, `followingScore` REAL NOT NULL, `deviceType` TEXT, `manufacturer` TEXT, `services` TEXT, PRIMARY KEY(`deviceAddress`))");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `ble_detections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deviceAddress` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `rssi` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL, `altitude` REAL, `speed` REAL, `bearing` REAL, FOREIGN KEY(`deviceAddress`) REFERENCES `ble_devices`(`deviceAddress`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        _db.execSQL("CREATE INDEX IF NOT EXISTS `index_ble_detections_deviceAddress_timestamp` ON `ble_detections` (`deviceAddress`, `timestamp`)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `location_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `accuracy` REAL NOT NULL, `altitude` REAL, `speed` REAL, `bearing` REAL, `provider` TEXT NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '7d8a537de330ad2b2229d57c2090794d')");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `ble_devices`");
        _db.execSQL("DROP TABLE IF EXISTS `ble_detections`");
        _db.execSQL("DROP TABLE IF EXISTS `location_records`");
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onDestructiveMigration(_db);
          }
        }
      }

      @Override
      public void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        _db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
      }

      @Override
      public void onPostMigrate(SupportSQLiteDatabase _db) {
      }

      @Override
      public RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsBleDevices = new HashMap<String, TableInfo.Column>(12);
        _columnsBleDevices.put("deviceAddress", new TableInfo.Column("deviceAddress", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("deviceName", new TableInfo.Column("deviceName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("rssi", new TableInfo.Column("rssi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("firstSeen", new TableInfo.Column("firstSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("lastSeen", new TableInfo.Column("lastSeen", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("isIgnored", new TableInfo.Column("isIgnored", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("label", new TableInfo.Column("label", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("isTracked", new TableInfo.Column("isTracked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("followingScore", new TableInfo.Column("followingScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("deviceType", new TableInfo.Column("deviceType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("manufacturer", new TableInfo.Column("manufacturer", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDevices.put("services", new TableInfo.Column("services", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBleDevices = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBleDevices = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBleDevices = new TableInfo("ble_devices", _columnsBleDevices, _foreignKeysBleDevices, _indicesBleDevices);
        final TableInfo _existingBleDevices = TableInfo.read(_db, "ble_devices");
        if (! _infoBleDevices.equals(_existingBleDevices)) {
          return new RoomOpenHelper.ValidationResult(false, "ble_devices(com.bleradar.data.database.BleDevice).\n"
                  + " Expected:\n" + _infoBleDevices + "\n"
                  + " Found:\n" + _existingBleDevices);
        }
        final HashMap<String, TableInfo.Column> _columnsBleDetections = new HashMap<String, TableInfo.Column>(10);
        _columnsBleDetections.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("deviceAddress", new TableInfo.Column("deviceAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("rssi", new TableInfo.Column("rssi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("accuracy", new TableInfo.Column("accuracy", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("altitude", new TableInfo.Column("altitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("speed", new TableInfo.Column("speed", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBleDetections.put("bearing", new TableInfo.Column("bearing", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBleDetections = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysBleDetections.add(new TableInfo.ForeignKey("ble_devices", "CASCADE", "NO ACTION",Arrays.asList("deviceAddress"), Arrays.asList("deviceAddress")));
        final HashSet<TableInfo.Index> _indicesBleDetections = new HashSet<TableInfo.Index>(1);
        _indicesBleDetections.add(new TableInfo.Index("index_ble_detections_deviceAddress_timestamp", false, Arrays.asList("deviceAddress","timestamp"), Arrays.asList("ASC","ASC")));
        final TableInfo _infoBleDetections = new TableInfo("ble_detections", _columnsBleDetections, _foreignKeysBleDetections, _indicesBleDetections);
        final TableInfo _existingBleDetections = TableInfo.read(_db, "ble_detections");
        if (! _infoBleDetections.equals(_existingBleDetections)) {
          return new RoomOpenHelper.ValidationResult(false, "ble_detections(com.bleradar.data.database.BleDetection).\n"
                  + " Expected:\n" + _infoBleDetections + "\n"
                  + " Found:\n" + _existingBleDetections);
        }
        final HashMap<String, TableInfo.Column> _columnsLocationRecords = new HashMap<String, TableInfo.Column>(9);
        _columnsLocationRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("latitude", new TableInfo.Column("latitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("longitude", new TableInfo.Column("longitude", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("accuracy", new TableInfo.Column("accuracy", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("altitude", new TableInfo.Column("altitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("speed", new TableInfo.Column("speed", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("bearing", new TableInfo.Column("bearing", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocationRecords.put("provider", new TableInfo.Column("provider", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocationRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocationRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocationRecords = new TableInfo("location_records", _columnsLocationRecords, _foreignKeysLocationRecords, _indicesLocationRecords);
        final TableInfo _existingLocationRecords = TableInfo.read(_db, "location_records");
        if (! _infoLocationRecords.equals(_existingLocationRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "location_records(com.bleradar.data.database.LocationRecord).\n"
                  + " Expected:\n" + _infoLocationRecords + "\n"
                  + " Found:\n" + _existingLocationRecords);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "7d8a537de330ad2b2229d57c2090794d", "79341ed5b4f169f6a44c6626e7f8b441");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "ble_devices","ble_detections","location_records");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `ble_devices`");
      _db.execSQL("DELETE FROM `ble_detections`");
      _db.execSQL("DELETE FROM `location_records`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BleDeviceDao.class, BleDeviceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BleDetectionDao.class, BleDetectionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LocationDao.class, LocationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  public List<Migration> getAutoMigrations(
      @NonNull Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecsMap) {
    return Arrays.asList();
  }

  @Override
  public BleDeviceDao bleDeviceDao() {
    if (_bleDeviceDao != null) {
      return _bleDeviceDao;
    } else {
      synchronized(this) {
        if(_bleDeviceDao == null) {
          _bleDeviceDao = new BleDeviceDao_Impl(this);
        }
        return _bleDeviceDao;
      }
    }
  }

  @Override
  public BleDetectionDao bleDetectionDao() {
    if (_bleDetectionDao != null) {
      return _bleDetectionDao;
    } else {
      synchronized(this) {
        if(_bleDetectionDao == null) {
          _bleDetectionDao = new BleDetectionDao_Impl(this);
        }
        return _bleDetectionDao;
      }
    }
  }

  @Override
  public LocationDao locationDao() {
    if (_locationDao != null) {
      return _locationDao;
    } else {
      synchronized(this) {
        if(_locationDao == null) {
          _locationDao = new LocationDao_Impl(this);
        }
        return _locationDao;
      }
    }
  }
}
