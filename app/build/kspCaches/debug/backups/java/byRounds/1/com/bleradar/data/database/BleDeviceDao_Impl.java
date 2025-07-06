package com.bleradar.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BleDeviceDao_Impl implements BleDeviceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BleDevice> __insertionAdapterOfBleDevice;

  private final EntityDeletionOrUpdateAdapter<BleDevice> __updateAdapterOfBleDevice;

  private final SharedSQLiteStatement __preparedStmtOfIgnoreDevice;

  private final SharedSQLiteStatement __preparedStmtOfLabelDevice;

  private final SharedSQLiteStatement __preparedStmtOfSetDeviceTracked;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFollowingScore;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDeviceMetrics;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastAlertTime;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSuspiciousScore;

  private final SharedSQLiteStatement __preparedStmtOfDeleteDevice;

  public BleDeviceDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBleDevice = new EntityInsertionAdapter<BleDevice>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR REPLACE INTO `ble_devices` (`deviceAddress`,`deviceName`,`rssi`,`firstSeen`,`lastSeen`,`isIgnored`,`label`,`isTracked`,`followingScore`,`deviceType`,`manufacturer`,`services`,`detectionCount`,`consecutiveDetections`,`maxConsecutiveDetections`,`averageRssi`,`rssiVariation`,`lastMovementTime`,`isStationary`,`detectionPattern`,`suspiciousActivityScore`,`lastAlertTime`,`isKnownTracker`,`trackerType`,`advertisingInterval`,`rotatingIdentifier`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, BleDevice value) {
        if (value.getDeviceAddress() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getDeviceAddress());
        }
        if (value.getDeviceName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getDeviceName());
        }
        stmt.bindLong(3, value.getRssi());
        stmt.bindLong(4, value.getFirstSeen());
        stmt.bindLong(5, value.getLastSeen());
        final int _tmp = value.isIgnored() ? 1 : 0;
        stmt.bindLong(6, _tmp);
        if (value.getLabel() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.getLabel());
        }
        final int _tmp_1 = value.isTracked() ? 1 : 0;
        stmt.bindLong(8, _tmp_1);
        stmt.bindDouble(9, value.getFollowingScore());
        if (value.getDeviceType() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.getDeviceType());
        }
        if (value.getManufacturer() == null) {
          stmt.bindNull(11);
        } else {
          stmt.bindString(11, value.getManufacturer());
        }
        if (value.getServices() == null) {
          stmt.bindNull(12);
        } else {
          stmt.bindString(12, value.getServices());
        }
        stmt.bindLong(13, value.getDetectionCount());
        stmt.bindLong(14, value.getConsecutiveDetections());
        stmt.bindLong(15, value.getMaxConsecutiveDetections());
        stmt.bindDouble(16, value.getAverageRssi());
        stmt.bindDouble(17, value.getRssiVariation());
        stmt.bindLong(18, value.getLastMovementTime());
        final int _tmp_2 = value.isStationary() ? 1 : 0;
        stmt.bindLong(19, _tmp_2);
        if (value.getDetectionPattern() == null) {
          stmt.bindNull(20);
        } else {
          stmt.bindString(20, value.getDetectionPattern());
        }
        stmt.bindDouble(21, value.getSuspiciousActivityScore());
        stmt.bindLong(22, value.getLastAlertTime());
        final int _tmp_3 = value.isKnownTracker() ? 1 : 0;
        stmt.bindLong(23, _tmp_3);
        if (value.getTrackerType() == null) {
          stmt.bindNull(24);
        } else {
          stmt.bindString(24, value.getTrackerType());
        }
        stmt.bindLong(25, value.getAdvertisingInterval());
        final int _tmp_4 = value.getRotatingIdentifier() ? 1 : 0;
        stmt.bindLong(26, _tmp_4);
      }
    };
    this.__updateAdapterOfBleDevice = new EntityDeletionOrUpdateAdapter<BleDevice>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR ABORT `ble_devices` SET `deviceAddress` = ?,`deviceName` = ?,`rssi` = ?,`firstSeen` = ?,`lastSeen` = ?,`isIgnored` = ?,`label` = ?,`isTracked` = ?,`followingScore` = ?,`deviceType` = ?,`manufacturer` = ?,`services` = ?,`detectionCount` = ?,`consecutiveDetections` = ?,`maxConsecutiveDetections` = ?,`averageRssi` = ?,`rssiVariation` = ?,`lastMovementTime` = ?,`isStationary` = ?,`detectionPattern` = ?,`suspiciousActivityScore` = ?,`lastAlertTime` = ?,`isKnownTracker` = ?,`trackerType` = ?,`advertisingInterval` = ?,`rotatingIdentifier` = ? WHERE `deviceAddress` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, BleDevice value) {
        if (value.getDeviceAddress() == null) {
          stmt.bindNull(1);
        } else {
          stmt.bindString(1, value.getDeviceAddress());
        }
        if (value.getDeviceName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getDeviceName());
        }
        stmt.bindLong(3, value.getRssi());
        stmt.bindLong(4, value.getFirstSeen());
        stmt.bindLong(5, value.getLastSeen());
        final int _tmp = value.isIgnored() ? 1 : 0;
        stmt.bindLong(6, _tmp);
        if (value.getLabel() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindString(7, value.getLabel());
        }
        final int _tmp_1 = value.isTracked() ? 1 : 0;
        stmt.bindLong(8, _tmp_1);
        stmt.bindDouble(9, value.getFollowingScore());
        if (value.getDeviceType() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindString(10, value.getDeviceType());
        }
        if (value.getManufacturer() == null) {
          stmt.bindNull(11);
        } else {
          stmt.bindString(11, value.getManufacturer());
        }
        if (value.getServices() == null) {
          stmt.bindNull(12);
        } else {
          stmt.bindString(12, value.getServices());
        }
        stmt.bindLong(13, value.getDetectionCount());
        stmt.bindLong(14, value.getConsecutiveDetections());
        stmt.bindLong(15, value.getMaxConsecutiveDetections());
        stmt.bindDouble(16, value.getAverageRssi());
        stmt.bindDouble(17, value.getRssiVariation());
        stmt.bindLong(18, value.getLastMovementTime());
        final int _tmp_2 = value.isStationary() ? 1 : 0;
        stmt.bindLong(19, _tmp_2);
        if (value.getDetectionPattern() == null) {
          stmt.bindNull(20);
        } else {
          stmt.bindString(20, value.getDetectionPattern());
        }
        stmt.bindDouble(21, value.getSuspiciousActivityScore());
        stmt.bindLong(22, value.getLastAlertTime());
        final int _tmp_3 = value.isKnownTracker() ? 1 : 0;
        stmt.bindLong(23, _tmp_3);
        if (value.getTrackerType() == null) {
          stmt.bindNull(24);
        } else {
          stmt.bindString(24, value.getTrackerType());
        }
        stmt.bindLong(25, value.getAdvertisingInterval());
        final int _tmp_4 = value.getRotatingIdentifier() ? 1 : 0;
        stmt.bindLong(26, _tmp_4);
        if (value.getDeviceAddress() == null) {
          stmt.bindNull(27);
        } else {
          stmt.bindString(27, value.getDeviceAddress());
        }
      }
    };
    this.__preparedStmtOfIgnoreDevice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET isIgnored = 1 WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfLabelDevice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET label = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetDeviceTracked = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET isTracked = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFollowingScore = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET followingScore = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateDeviceMetrics = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET detectionCount = ?, consecutiveDetections = ?, maxConsecutiveDetections = ?, averageRssi = ?, rssiVariation = ?, lastMovementTime = ?, isStationary = ?, suspiciousActivityScore = ?, isKnownTracker = ?, trackerType = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastAlertTime = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET lastAlertTime = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSuspiciousScore = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "UPDATE ble_devices SET suspiciousActivityScore = ? WHERE deviceAddress = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteDevice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM ble_devices WHERE deviceAddress = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertDevice(final BleDevice device,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBleDevice.insert(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object updateDevice(final BleDevice device,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBleDevice.handle(device);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object ignoreDevice(final String address, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIgnoreDevice.acquire();
        int _argIndex = 1;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfIgnoreDevice.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object labelDevice(final String address, final String label,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfLabelDevice.acquire();
        int _argIndex = 1;
        if (label == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, label);
        }
        _argIndex = 2;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfLabelDevice.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object setDeviceTracked(final String address, final boolean tracked,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetDeviceTracked.acquire();
        int _argIndex = 1;
        final int _tmp = tracked ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfSetDeviceTracked.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object updateFollowingScore(final String address, final float score,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFollowingScore.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, score);
        _argIndex = 2;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfUpdateFollowingScore.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object updateDeviceMetrics(final String address, final int count, final int consecutive,
      final int maxConsecutive, final float avgRssi, final float rssiVar, final long lastMovement,
      final boolean stationary, final float suspiciousScore, final boolean knownTracker,
      final String trackerType, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDeviceMetrics.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, count);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, consecutive);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, maxConsecutive);
        _argIndex = 4;
        _stmt.bindDouble(_argIndex, avgRssi);
        _argIndex = 5;
        _stmt.bindDouble(_argIndex, rssiVar);
        _argIndex = 6;
        _stmt.bindLong(_argIndex, lastMovement);
        _argIndex = 7;
        final int _tmp = stationary ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 8;
        _stmt.bindDouble(_argIndex, suspiciousScore);
        _argIndex = 9;
        final int _tmp_1 = knownTracker ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp_1);
        _argIndex = 10;
        if (trackerType == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, trackerType);
        }
        _argIndex = 11;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfUpdateDeviceMetrics.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object updateLastAlertTime(final String address, final long alertTime,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastAlertTime.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, alertTime);
        _argIndex = 2;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfUpdateLastAlertTime.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object updateSuspiciousScore(final String address, final float score,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSuspiciousScore.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, score);
        _argIndex = 2;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfUpdateSuspiciousScore.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object deleteDevice(final String address, final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteDevice.acquire();
        int _argIndex = 1;
        if (address == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, address);
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfDeleteDevice.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Flow<List<BleDevice>> getAllDevices() {
    final String _sql = "SELECT * FROM ble_devices WHERE isIgnored = 0 ORDER BY lastSeen DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_devices"}, new Callable<List<BleDevice>>() {
      @Override
      public List<BleDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final List<BleDevice> _result = new ArrayList<BleDevice>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDevice _item;
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _item = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<BleDevice>> getTrackedDevices() {
    final String _sql = "SELECT * FROM ble_devices WHERE isTracked = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_devices"}, new Callable<List<BleDevice>>() {
      @Override
      public List<BleDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final List<BleDevice> _result = new ArrayList<BleDevice>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDevice _item;
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _item = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<BleDevice>> getSuspiciousDevices(final float threshold) {
    final String _sql = "SELECT * FROM ble_devices WHERE followingScore > ? ORDER BY followingScore DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, threshold);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_devices"}, new Callable<List<BleDevice>>() {
      @Override
      public List<BleDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final List<BleDevice> _result = new ArrayList<BleDevice>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDevice _item;
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _item = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDevice(final String address,
      final Continuation<? super BleDevice> continuation) {
    final String _sql = "SELECT * FROM ble_devices WHERE deviceAddress = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BleDevice>() {
      @Override
      public BleDevice call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final BleDevice _result;
          if(_cursor.moveToFirst()) {
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _result = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, continuation);
  }

  @Override
  public Flow<List<BleDevice>> getKnownTrackers() {
    final String _sql = "SELECT * FROM ble_devices WHERE isKnownTracker = 1 ORDER BY suspiciousActivityScore DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_devices"}, new Callable<List<BleDevice>>() {
      @Override
      public List<BleDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final List<BleDevice> _result = new ArrayList<BleDevice>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDevice _item;
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _item = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<BleDevice>> getSuspiciousDevicesByActivity(final float threshold) {
    final String _sql = "SELECT * FROM ble_devices WHERE suspiciousActivityScore > ? ORDER BY suspiciousActivityScore DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, threshold);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_devices"}, new Callable<List<BleDevice>>() {
      @Override
      public List<BleDevice> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfDeviceName = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceName");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFirstSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "firstSeen");
          final int _cursorIndexOfLastSeen = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSeen");
          final int _cursorIndexOfIsIgnored = CursorUtil.getColumnIndexOrThrow(_cursor, "isIgnored");
          final int _cursorIndexOfLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "label");
          final int _cursorIndexOfIsTracked = CursorUtil.getColumnIndexOrThrow(_cursor, "isTracked");
          final int _cursorIndexOfFollowingScore = CursorUtil.getColumnIndexOrThrow(_cursor, "followingScore");
          final int _cursorIndexOfDeviceType = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceType");
          final int _cursorIndexOfManufacturer = CursorUtil.getColumnIndexOrThrow(_cursor, "manufacturer");
          final int _cursorIndexOfServices = CursorUtil.getColumnIndexOrThrow(_cursor, "services");
          final int _cursorIndexOfDetectionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionCount");
          final int _cursorIndexOfConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveDetections");
          final int _cursorIndexOfMaxConsecutiveDetections = CursorUtil.getColumnIndexOrThrow(_cursor, "maxConsecutiveDetections");
          final int _cursorIndexOfAverageRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "averageRssi");
          final int _cursorIndexOfRssiVariation = CursorUtil.getColumnIndexOrThrow(_cursor, "rssiVariation");
          final int _cursorIndexOfLastMovementTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastMovementTime");
          final int _cursorIndexOfIsStationary = CursorUtil.getColumnIndexOrThrow(_cursor, "isStationary");
          final int _cursorIndexOfDetectionPattern = CursorUtil.getColumnIndexOrThrow(_cursor, "detectionPattern");
          final int _cursorIndexOfSuspiciousActivityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "suspiciousActivityScore");
          final int _cursorIndexOfLastAlertTime = CursorUtil.getColumnIndexOrThrow(_cursor, "lastAlertTime");
          final int _cursorIndexOfIsKnownTracker = CursorUtil.getColumnIndexOrThrow(_cursor, "isKnownTracker");
          final int _cursorIndexOfTrackerType = CursorUtil.getColumnIndexOrThrow(_cursor, "trackerType");
          final int _cursorIndexOfAdvertisingInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "advertisingInterval");
          final int _cursorIndexOfRotatingIdentifier = CursorUtil.getColumnIndexOrThrow(_cursor, "rotatingIdentifier");
          final List<BleDevice> _result = new ArrayList<BleDevice>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDevice _item;
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final String _tmpDeviceName;
            if (_cursor.isNull(_cursorIndexOfDeviceName)) {
              _tmpDeviceName = null;
            } else {
              _tmpDeviceName = _cursor.getString(_cursorIndexOfDeviceName);
            }
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final long _tmpFirstSeen;
            _tmpFirstSeen = _cursor.getLong(_cursorIndexOfFirstSeen);
            final long _tmpLastSeen;
            _tmpLastSeen = _cursor.getLong(_cursorIndexOfLastSeen);
            final boolean _tmpIsIgnored;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIgnored);
            _tmpIsIgnored = _tmp != 0;
            final String _tmpLabel;
            if (_cursor.isNull(_cursorIndexOfLabel)) {
              _tmpLabel = null;
            } else {
              _tmpLabel = _cursor.getString(_cursorIndexOfLabel);
            }
            final boolean _tmpIsTracked;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsTracked);
            _tmpIsTracked = _tmp_1 != 0;
            final float _tmpFollowingScore;
            _tmpFollowingScore = _cursor.getFloat(_cursorIndexOfFollowingScore);
            final String _tmpDeviceType;
            if (_cursor.isNull(_cursorIndexOfDeviceType)) {
              _tmpDeviceType = null;
            } else {
              _tmpDeviceType = _cursor.getString(_cursorIndexOfDeviceType);
            }
            final String _tmpManufacturer;
            if (_cursor.isNull(_cursorIndexOfManufacturer)) {
              _tmpManufacturer = null;
            } else {
              _tmpManufacturer = _cursor.getString(_cursorIndexOfManufacturer);
            }
            final String _tmpServices;
            if (_cursor.isNull(_cursorIndexOfServices)) {
              _tmpServices = null;
            } else {
              _tmpServices = _cursor.getString(_cursorIndexOfServices);
            }
            final int _tmpDetectionCount;
            _tmpDetectionCount = _cursor.getInt(_cursorIndexOfDetectionCount);
            final int _tmpConsecutiveDetections;
            _tmpConsecutiveDetections = _cursor.getInt(_cursorIndexOfConsecutiveDetections);
            final int _tmpMaxConsecutiveDetections;
            _tmpMaxConsecutiveDetections = _cursor.getInt(_cursorIndexOfMaxConsecutiveDetections);
            final float _tmpAverageRssi;
            _tmpAverageRssi = _cursor.getFloat(_cursorIndexOfAverageRssi);
            final float _tmpRssiVariation;
            _tmpRssiVariation = _cursor.getFloat(_cursorIndexOfRssiVariation);
            final long _tmpLastMovementTime;
            _tmpLastMovementTime = _cursor.getLong(_cursorIndexOfLastMovementTime);
            final boolean _tmpIsStationary;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsStationary);
            _tmpIsStationary = _tmp_2 != 0;
            final String _tmpDetectionPattern;
            if (_cursor.isNull(_cursorIndexOfDetectionPattern)) {
              _tmpDetectionPattern = null;
            } else {
              _tmpDetectionPattern = _cursor.getString(_cursorIndexOfDetectionPattern);
            }
            final float _tmpSuspiciousActivityScore;
            _tmpSuspiciousActivityScore = _cursor.getFloat(_cursorIndexOfSuspiciousActivityScore);
            final long _tmpLastAlertTime;
            _tmpLastAlertTime = _cursor.getLong(_cursorIndexOfLastAlertTime);
            final boolean _tmpIsKnownTracker;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsKnownTracker);
            _tmpIsKnownTracker = _tmp_3 != 0;
            final String _tmpTrackerType;
            if (_cursor.isNull(_cursorIndexOfTrackerType)) {
              _tmpTrackerType = null;
            } else {
              _tmpTrackerType = _cursor.getString(_cursorIndexOfTrackerType);
            }
            final long _tmpAdvertisingInterval;
            _tmpAdvertisingInterval = _cursor.getLong(_cursorIndexOfAdvertisingInterval);
            final boolean _tmpRotatingIdentifier;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfRotatingIdentifier);
            _tmpRotatingIdentifier = _tmp_4 != 0;
            _item = new BleDevice(_tmpDeviceAddress,_tmpDeviceName,_tmpRssi,_tmpFirstSeen,_tmpLastSeen,_tmpIsIgnored,_tmpLabel,_tmpIsTracked,_tmpFollowingScore,_tmpDeviceType,_tmpManufacturer,_tmpServices,_tmpDetectionCount,_tmpConsecutiveDetections,_tmpMaxConsecutiveDetections,_tmpAverageRssi,_tmpRssiVariation,_tmpLastMovementTime,_tmpIsStationary,_tmpDetectionPattern,_tmpSuspiciousActivityScore,_tmpLastAlertTime,_tmpIsKnownTracker,_tmpTrackerType,_tmpAdvertisingInterval,_tmpRotatingIdentifier);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
