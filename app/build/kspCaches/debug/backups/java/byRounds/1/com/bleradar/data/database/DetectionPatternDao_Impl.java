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
public final class DetectionPatternDao_Impl implements DetectionPatternDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DetectionPattern> __insertionAdapterOfDetectionPattern;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldPatterns;

  private final SharedSQLiteStatement __preparedStmtOfDeletePatternsForDevice;

  public DetectionPatternDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDetectionPattern = new EntityInsertionAdapter<DetectionPattern>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `detection_patterns` (`id`,`deviceAddress`,`timestamp`,`patternType`,`confidence`,`metadata`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, DetectionPattern value) {
        stmt.bindLong(1, value.getId());
        if (value.getDeviceAddress() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getDeviceAddress());
        }
        stmt.bindLong(3, value.getTimestamp());
        if (value.getPatternType() == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.getPatternType());
        }
        stmt.bindDouble(5, value.getConfidence());
        if (value.getMetadata() == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.getMetadata());
        }
      }
    };
    this.__preparedStmtOfDeleteOldPatterns = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM detection_patterns WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePatternsForDevice = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM detection_patterns WHERE deviceAddress = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPattern(final DetectionPattern pattern,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDetectionPattern.insert(pattern);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object insertPatterns(final List<DetectionPattern> patterns,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDetectionPattern.insert(patterns);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, continuation);
  }

  @Override
  public Object deleteOldPatterns(final long cutoffTime,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldPatterns.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cutoffTime);
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
          __preparedStmtOfDeleteOldPatterns.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Object deletePatternsForDevice(final String address,
      final Continuation<? super Unit> continuation) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePatternsForDevice.acquire();
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
          __preparedStmtOfDeletePatternsForDevice.release(_stmt);
        }
      }
    }, continuation);
  }

  @Override
  public Flow<List<DetectionPattern>> getPatternsForDevice(final String address) {
    final String _sql = "SELECT * FROM detection_patterns WHERE deviceAddress = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[]{"detection_patterns"}, new Callable<List<DetectionPattern>>() {
      @Override
      public List<DetectionPattern> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatternType = CursorUtil.getColumnIndexOrThrow(_cursor, "patternType");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfMetadata = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata");
          final List<DetectionPattern> _result = new ArrayList<DetectionPattern>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final DetectionPattern _item;
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
            final String _tmpPatternType;
            if (_cursor.isNull(_cursorIndexOfPatternType)) {
              _tmpPatternType = null;
            } else {
              _tmpPatternType = _cursor.getString(_cursorIndexOfPatternType);
            }
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpMetadata;
            if (_cursor.isNull(_cursorIndexOfMetadata)) {
              _tmpMetadata = null;
            } else {
              _tmpMetadata = _cursor.getString(_cursorIndexOfMetadata);
            }
            _item = new DetectionPattern(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpPatternType,_tmpConfidence,_tmpMetadata);
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
  public Flow<List<DetectionPattern>> getPatternsByType(final String type) {
    final String _sql = "SELECT * FROM detection_patterns WHERE patternType = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (type == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, type);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[]{"detection_patterns"}, new Callable<List<DetectionPattern>>() {
      @Override
      public List<DetectionPattern> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatternType = CursorUtil.getColumnIndexOrThrow(_cursor, "patternType");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfMetadata = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata");
          final List<DetectionPattern> _result = new ArrayList<DetectionPattern>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final DetectionPattern _item;
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
            final String _tmpPatternType;
            if (_cursor.isNull(_cursorIndexOfPatternType)) {
              _tmpPatternType = null;
            } else {
              _tmpPatternType = _cursor.getString(_cursorIndexOfPatternType);
            }
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpMetadata;
            if (_cursor.isNull(_cursorIndexOfMetadata)) {
              _tmpMetadata = null;
            } else {
              _tmpMetadata = _cursor.getString(_cursorIndexOfMetadata);
            }
            _item = new DetectionPattern(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpPatternType,_tmpConfidence,_tmpMetadata);
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
  public Flow<List<DetectionPattern>> getPatternsSince(final long startTime) {
    final String _sql = "SELECT * FROM detection_patterns WHERE timestamp >= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    return CoroutinesRoom.createFlow(__db, false, new String[]{"detection_patterns"}, new Callable<List<DetectionPattern>>() {
      @Override
      public List<DetectionPattern> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatternType = CursorUtil.getColumnIndexOrThrow(_cursor, "patternType");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfMetadata = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata");
          final List<DetectionPattern> _result = new ArrayList<DetectionPattern>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final DetectionPattern _item;
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
            final String _tmpPatternType;
            if (_cursor.isNull(_cursorIndexOfPatternType)) {
              _tmpPatternType = null;
            } else {
              _tmpPatternType = _cursor.getString(_cursorIndexOfPatternType);
            }
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpMetadata;
            if (_cursor.isNull(_cursorIndexOfMetadata)) {
              _tmpMetadata = null;
            } else {
              _tmpMetadata = _cursor.getString(_cursorIndexOfMetadata);
            }
            _item = new DetectionPattern(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpPatternType,_tmpConfidence,_tmpMetadata);
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
  public Object getLastPatternForDevice(final String address, final String type,
      final Continuation<? super DetectionPattern> continuation) {
    final String _sql = "SELECT * FROM detection_patterns WHERE deviceAddress = ? AND patternType = ? ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (address == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, address);
    }
    _argIndex = 2;
    if (type == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, type);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DetectionPattern>() {
      @Override
      public DetectionPattern call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceAddress");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfPatternType = CursorUtil.getColumnIndexOrThrow(_cursor, "patternType");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfMetadata = CursorUtil.getColumnIndexOrThrow(_cursor, "metadata");
          final DetectionPattern _result;
          if(_cursor.moveToFirst()) {
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
            final String _tmpPatternType;
            if (_cursor.isNull(_cursorIndexOfPatternType)) {
              _tmpPatternType = null;
            } else {
              _tmpPatternType = _cursor.getString(_cursorIndexOfPatternType);
            }
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpMetadata;
            if (_cursor.isNull(_cursorIndexOfMetadata)) {
              _tmpMetadata = null;
            } else {
              _tmpMetadata = _cursor.getString(_cursorIndexOfMetadata);
            }
            _result = new DetectionPattern(_tmpId,_tmpDeviceAddress,_tmpTimestamp,_tmpPatternType,_tmpConfidence,_tmpMetadata);
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

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
