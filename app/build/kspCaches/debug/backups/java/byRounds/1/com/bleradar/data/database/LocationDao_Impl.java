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
public final class LocationDao_Impl implements LocationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocationRecord> __insertionAdapterOfLocationRecord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldLocations;

  public LocationDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocationRecord = new EntityInsertionAdapter<LocationRecord>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `location_records` (`id`,`timestamp`,`latitude`,`longitude`,`accuracy`,`altitude`,`speed`,`bearing`,`provider`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, LocationRecord value) {
        stmt.bindLong(1, value.getId());
        stmt.bindLong(2, value.getTimestamp());
        stmt.bindDouble(3, value.getLatitude());
        stmt.bindDouble(4, value.getLongitude());
        stmt.bindDouble(5, value.getAccuracy());
        if (value.getAltitude() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindDouble(6, value.getAltitude());
        }
        if (value.getSpeed() == null) {
          stmt.bindNull(7);
        } else {
          stmt.bindDouble(7, value.getSpeed());
        }
        if (value.getBearing() == null) {
          stmt.bindNull(8);
        } else {
          stmt.bindDouble(8, value.getBearing());
        }
        if (value.getProvider() == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.getProvider());
        }
      }
    };
    this.__preparedStmtOfDeleteOldLocations = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM location_records WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLocation(final LocationRecord location,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocationRecord.insert(location);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object deleteOldLocations(final long cutoffTime,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldLocations.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfDeleteOldLocations.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object getLastLocation(final Continuation<? super LocationRecord> continuation) {
    final String _sql = "SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocationRecord>() {
      @Override
      public LocationRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final LocationRecord _result;
          if(_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
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
            final String _tmpProvider;
            if (_cursor.isNull(_cursorIndexOfProvider)) {
              _tmpProvider = null;
            } else {
              _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            }
            _result = new LocationRecord(_tmpId,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing,_tmpProvider);
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
  public Flow<List<LocationRecord>> getLocationsSince(final long startTime) {
    final String _sql = "SELECT * FROM location_records WHERE timestamp >= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"location_records"}, new Callable<List<LocationRecord>>() {
      @Override
      public List<LocationRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final List<LocationRecord> _result = new ArrayList<LocationRecord>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final LocationRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
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
            final String _tmpProvider;
            if (_cursor.isNull(_cursorIndexOfProvider)) {
              _tmpProvider = null;
            } else {
              _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            }
            _item = new LocationRecord(_tmpId,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing,_tmpProvider);
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
  public Flow<List<LocationRecord>> getRecentLocations(final int limit) {
    final String _sql = "SELECT * FROM location_records ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"location_records"}, new Callable<List<LocationRecord>>() {
      @Override
      public List<LocationRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfAltitude = CursorUtil.getColumnIndexOrThrow(_cursor, "altitude");
          final int _cursorIndexOfSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "speed");
          final int _cursorIndexOfBearing = CursorUtil.getColumnIndexOrThrow(_cursor, "bearing");
          final int _cursorIndexOfProvider = CursorUtil.getColumnIndexOrThrow(_cursor, "provider");
          final List<LocationRecord> _result = new ArrayList<LocationRecord>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final LocationRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
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
            final String _tmpProvider;
            if (_cursor.isNull(_cursorIndexOfProvider)) {
              _tmpProvider = null;
            } else {
              _tmpProvider = _cursor.getString(_cursorIndexOfProvider);
            }
            _item = new LocationRecord(_tmpId,_tmpTimestamp,_tmpLatitude,_tmpLongitude,_tmpAccuracy,_tmpAltitude,_tmpSpeed,_tmpBearing,_tmpProvider);
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
