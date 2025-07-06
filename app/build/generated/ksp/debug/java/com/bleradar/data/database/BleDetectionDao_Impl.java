package com.bleradar.data.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
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
public final class BleDetectionDao_Impl implements BleDetectionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BleDetection> __insertionAdapterOfBleDetection;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldDetections;

  private final SharedSQLiteStatement __preparedStmtOfDeleteDetectionsForDevice;

  public BleDetectionDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBleDetection = new EntityInsertionAdapter<BleDetection>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `ble_detections` (`id`,`deviceAddress`,`timestamp`,`rssi`,`latitude`,`longitude`,`accuracy`,`altitude`,`speed`,`bearing`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, BleDetection value) {
        stmt.bindLong(1, value.getId());
        if (value.getDeviceAddress() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getDeviceAddress());
        }
        stmt.bindLong(3, value.getTimestamp());
        stmt.bindLong(4, value.getRssi());
        stmt.bindDouble(5, value.getLatitude());
        stmt.bindDouble(6, value.getLongitude());
        stmt.bindDouble(7, value.getAccuracy());
        if (value.getAltitude() == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindDouble(8, value.getAltitude());
        }
        if (value.getSpeed() == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindDouble(9, value.getSpeed());
        }
        if (value.getBearing() == null) {
          stmt.bindNull(10);
        } else {
          stmt.bindDouble(10, value.getBearing());
        }
      }
    };
    this.__preparedStmtOfDeleteOldDetections = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM ble_detections WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteDetectionsForDevice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM ble_detections WHERE deviceAddress = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertDetection(final BleDetection detection,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBleDetection.insert(detection);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object insertDetections(final List<BleDetection> detections,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBleDetection.insert(detections);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object deleteOldDetections(final long cutoffTime,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldDetections.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfDeleteOldDetections.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object deleteDetectionsForDevice(final String address,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteDetectionsForDevice.acquire();
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
          __preparedStmtOfDeleteDetectionsForDevice.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Flow<List<BleDetection>> getDetectionsForDevice(final String address) {
    final String _sql = "SELECT * FROM ble_detections WHERE deviceAddress = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_detections"}, new Callable<List<BleDetection>>() {
      @Override
      public List<BleDetection> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final List<BleDetection> _result = new ArrayList<BleDetection>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDetection _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final Double _tmpAltitude;
            if (_cursor.isNull(_cursorIndexOfAltitude)) {
              _tmpAltitude = null;
            } else {
              _tmpAltitude = _cursor.getDouble(_cursorIndexOfAltitude);
            }
            final Float _tmpSpeed;
            if (_cursor.isNull(_cursorIndexOfSpeed)) {
              _tmpSpeed = null;
            } else {
              _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            }
            final Float _tmpBearing;
            if (_cursor.isNull(_cursorIndexOfBearing)) {
              _tmpBearing = null;
            } else {
              _tmpBearing = _cursor.getFloat(_cursorIndexOfBearing);
            }
            _item = new BleDetection(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing);
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
  public Flow<List<BleDetection>> getDetectionsSince(final long startTime) {
    final String _sql = "SELECT * FROM ble_detections WHERE timestamp >= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_detections"}, new Callable<List<BleDetection>>() {
      @Override
      public List<BleDetection> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final List<BleDetection> _result = new ArrayList<BleDetection>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDetection _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final Double _tmpAltitude;
            if (_cursor.isNull(_cursorIndexOfAltitude)) {
              _tmpAltitude = null;
            } else {
              _tmpAltitude = _cursor.getDouble(_cursorIndexOfAltitude);
            }
            final Float _tmpSpeed;
            if (_cursor.isNull(_cursorIndexOfSpeed)) {
              _tmpSpeed = null;
            } else {
              _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            }
            final Float _tmpBearing;
            if (_cursor.isNull(_cursorIndexOfBearing)) {
              _tmpBearing = null;
            } else {
              _tmpBearing = _cursor.getFloat(_cursorIndexOfBearing);
            }
            _item = new BleDetection(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing);
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
  public Flow<List<BleDetection>> getDetectionsForDeviceSince(final String address,
      final long startTime) {
    final String _sql = "SELECT * FROM ble_detections WHERE deviceAddress = ? AND timestamp >= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, startTime);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"ble_detections"}, new Callable<List<BleDetection>>() {
      @Override
      public List<BleDetection> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final List<BleDetection> _result = new ArrayList<BleDetection>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final BleDetection _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceAddress;
            if (_cursor.isNull(_cursorIndexOfDeviceAddress)) {
              _tmpDeviceAddress = null;
            } else {
              _tmpDeviceAddress = _cursor.getString(_cursorIndexOfDeviceAddress);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final Double _tmpAltitude;
            if (_cursor.isNull(_cursorIndexOfAltitude)) {
              _tmpAltitude = null;
            } else {
              _tmpAltitude = _cursor.getDouble(_cursorIndexOfAltitude);
            }
            final Float _tmpSpeed;
            if (_cursor.isNull(_cursorIndexOfSpeed)) {
              _tmpSpeed = null;
            } else {
              _tmpSpeed = _cursor.getFloat(_cursorIndexOfSpeed);
            }
            final Float _tmpBearing;
            if (_cursor.isNull(_cursorIndexOfBearing)) {
              _tmpBearing = null;
            } else {
              _tmpBearing = _cursor.getFloat(_cursorIndexOfBearing);
            }
            _item = new BleDetection(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpRssi,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing);
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
  public Object getDetectionCount(final String address, final long startTime,
      final Continuation<? super Integer> continuation) {
    final String _sql = "SELECT COUNT(*) FROM ble_detections WHERE deviceAddress = ? AND timestamp >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, startTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if(_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, continuation);
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
