package com.daniel.workouttracker.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.daniel.workouttracker.model.WorkoutLocation;
import com.daniel.workouttracker.model.WorkoutSession;

import java.util.ArrayList;
import java.util.List;

/**
 * The class handles the SQLite database used in the app
 *
 * @author Daniel Johansson
 */
public class DBHelper extends SQLiteOpenHelper {

    /**
     * SQLiteDatabase object
     */
    private SQLiteDatabase mDb;

    //Database Version
    private static final int DATABASE_VERSION = 13;

    //Database Name
    private static final String DATABASE_NAME = "locations.db";

    //Table names
    private static final String TABLE_SESSIONS = "sessions";
    private static final String TABLE_LOCATIONS = "locations";

    //Sessions table - column names
    private static final String KEY_SESSIONS_ID = "id";
    private static final String KEY_STARTTIME = "start_time";
    private static final String KEY_STARTDATE = "start_date";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_DISTANCE = "distance";

    //Locations table - column names
    private static final String KEY_LOCATIONS_ID = "id";
    private static final String KEY_LOCATIONS_SESSIONS_ID = "session_id";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_ALTITUDE = "altitude";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_ELAPSED_TIME = "elapsed_time";

    //Sessions table Create statement
    private static final String CREATE_TABLE_SESSIONS = "CREATE TABLE "
            + TABLE_SESSIONS + "(" + KEY_SESSIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_STARTTIME
            + " TEXT," + KEY_STARTDATE + " TEXT," + KEY_DURATION
            + " INTEGER," + KEY_DISTANCE + " REAL" + ")";

    //Locations table Create statement
    private static final String CREATE_TABLE_LOCATIONS = "CREATE TABLE " + TABLE_LOCATIONS
            + "(" + KEY_LOCATIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_LATITUDE + " TEXT,"
            + KEY_LONGITUDE + " TEXT," + KEY_ALTITUDE + " REAL," + KEY_SPEED + " REAL,"
            + KEY_LOCATIONS_SESSIONS_ID + " INTEGER, " + KEY_ELAPSED_TIME + " INTEGER" + ")";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        getDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SESSIONS);
        db.execSQL(CREATE_TABLE_LOCATIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);

        onCreate(db);
    }

    /**
     * Gets a SQLiteDatabase object.
     *
     * @return writeable Database
     */
    public SQLiteDatabase getDatabase() {
        mDb = this.getWritableDatabase();

        return mDb;
    }

    /**
     * Creates a WorkoutSession in db.
     *
     * @param session the WorkoutSession to be stored in the db
     * @return the id nr of the WorkoutSession
     */
    public long createWorkoutSession(WorkoutSession session) {
        ContentValues values = new ContentValues();
        values.put(KEY_STARTTIME, session.getStartTime());
        values.put(KEY_STARTDATE, session.getStartDate());

        long sessionId = 0;

        try {
            //Inserts the WorkoutSession data in the db and returns an id
            sessionId = mDb.insert(TABLE_SESSIONS, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return sessionId;
    }

    /**
     * Creates a WorkoutLocation in db.
     *
     * @param sessionId   id nr of the WorkoutSession
     * @param latitude    latitude of the WorkoutLocation
     * @param longitude   longitude of the WorkoutLocation
     * @param altitude    altitude of the WorkoutLocation in metres
     * @param speed       speed in m/s, when the WorkoutLocation was registered
     * @param elapsedTime elapsed time of the WorkoutSession when the WorkoutLocation was registered
     * @return the id nr of the WorkoutLocation
     */
    public long createWorkoutLocation(long sessionId, String latitude, String longitude, double altitude,
                                      float speed, long elapsedTime) {
        ContentValues values = new ContentValues();
        values.put(KEY_LOCATIONS_SESSIONS_ID, sessionId);
        values.put(KEY_LATITUDE, latitude);
        values.put(KEY_LONGITUDE, longitude);
        values.put(KEY_ALTITUDE, altitude);
        values.put(KEY_SPEED, speed);
        values.put(KEY_ELAPSED_TIME, elapsedTime);

        long id = 0;

        try {
            //Inserts the WorkoutLocation data in the db and returns an id
            id = mDb.insert(TABLE_LOCATIONS, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Gets a list of the specific WorkoutSession´s WorkoutLocations from the db.
     *
     * @param sessionId id nr of the WorkoutSession
     * @return list of WorkoutLocations
     */
    public List<WorkoutLocation> getLocationsFromSession(long sessionId) {
        List<WorkoutLocation> locations = new ArrayList<>();

        //Selects all from Locations where session_id (foreign key)=sessionId
        String selectQuery = "SELECT  * FROM " + TABLE_LOCATIONS + " WHERE "
                + KEY_LOCATIONS_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = mDb.rawQuery(selectQuery, null);

            //Loops through all rows, creates a WorkoutLocation object from the data, then adds it to the list
            if (c.moveToFirst()) {
                do {
                    WorkoutLocation location = new WorkoutLocation();
                    location.setId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_ID)));
                    location.setLatitude((c.getString(c.getColumnIndex(KEY_LATITUDE))));
                    location.setLongitude(c.getString(c.getColumnIndex(KEY_LONGITUDE)));
                    location.setAltitude((c.getDouble(c.getColumnIndex(KEY_ALTITUDE))));
                    location.setSpeed(c.getFloat(c.getColumnIndex(KEY_SPEED)));
                    location.setElapsedTime(c.getLong(c.getColumnIndex(KEY_ELAPSED_TIME)));
                    location.setSessionId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_SESSIONS_ID)));

                    locations.add(location);
                } while (c.moveToNext());
            }
            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return locations;

    }

    /**
     * Gets a specific WorkoutSession
     *
     * @param sessionId id nr of the specific WorkoutSession
     * @return the WorkoutSession
     */
    public WorkoutSession getWorkoutSession(long sessionId) {
        WorkoutSession session = null;
        List<WorkoutLocation> locations = new ArrayList<>();

        //Selects all from sessions table where session_id=sessionId
        String selectQuery = "SELECT  * FROM " + TABLE_SESSIONS + " WHERE "
                + KEY_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = mDb.rawQuery(selectQuery, null);

            if (c != null)
                c.moveToFirst();

            //Creates a WorkoutSession
            session = new WorkoutSession();
            session.setId(c.getInt(c.getColumnIndex(KEY_SESSIONS_ID)));
            session.setStartDate((c.getString(c.getColumnIndex(KEY_STARTDATE))));
            session.setStartTime(c.getString(c.getColumnIndex(KEY_STARTTIME)));
            session.setDistance((c.getFloat(c.getColumnIndex(KEY_DISTANCE))));
            session.setDuration(c.getLong(c.getColumnIndex(KEY_DURATION)));

            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        //Selects all from locations table where session_id (foreign key)=sessionId
        selectQuery = "SELECT  * FROM " + TABLE_LOCATIONS + " WHERE "
                + KEY_LOCATIONS_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = mDb.rawQuery(selectQuery, null);

            //Loops through all rows, creates a WorkoutLocation from each row and adds it to the list of locations
            if (c.moveToFirst()) {
                do {

                    WorkoutLocation location = new WorkoutLocation();
                    location.setId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_ID)));
                    location.setLatitude((c.getString(c.getColumnIndex(KEY_LATITUDE))));
                    location.setLongitude(c.getString(c.getColumnIndex(KEY_LONGITUDE)));
                    location.setAltitude((c.getDouble(c.getColumnIndex(KEY_ALTITUDE))));
                    location.setSpeed(c.getFloat(c.getColumnIndex(KEY_SPEED)));
                    location.setElapsedTime(c.getLong(c.getColumnIndex(KEY_ELAPSED_TIME)));
                    location.setSessionId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_SESSIONS_ID)));

                    locations.add(location);
                } while (c.moveToNext());
            }

            session.setLocations(locations);

            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return session;
    }

    /**
     * Gets a list of all WorkoutSessions
     *
     * @return list of WorkoutSessions
     */
    public List<WorkoutSession> getAllWorkoutSessions() {
        List<WorkoutSession> sessions = new ArrayList<>();

        //Selects session_id from sessions table
        String selectQuery = "SELECT " + KEY_SESSIONS_ID + " FROM " + TABLE_SESSIONS;

        try {
            Cursor c = mDb.rawQuery(selectQuery, null);

            // Loops through all session_id´s and calls getWorkoutSession for each,
            // then adds the WorkoutSession to the list of WorkoutSessions
            if (c.moveToFirst()) {
                do {
                    WorkoutSession session = getWorkoutSession(c.getInt(c.getColumnIndex(KEY_SESSIONS_ID)));
                    sessions.add(session);
                } while (c.moveToNext());
            }
            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return sessions;
    }


    /**
     * Deletes a specific WorkoutSession
     *
     * @param sessionId id nr of the WorkoutSession
     */
    public void deleteWorkoutSession(long sessionId) {
        try {
            //Deletes the WorkoutSession data from table sessions where session_id=sessionId
            int sessionNr = mDb.delete(TABLE_SESSIONS, KEY_SESSIONS_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            //Deletes all WorkoutLocations data from table locations where session_id(foreign key)=sessionId
            int locationsNr = mDb.delete(TABLE_LOCATIONS, KEY_LOCATIONS_SESSIONS_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a WorkoutSession
     *
     * @param session the WorkoutSession to be updated
     * @return number of rows affected
     */
    public int updateWorkoutSession(WorkoutSession session) {
        ContentValues values = new ContentValues();
        values.put(KEY_DURATION, session.getDuration());
        values.put(KEY_DISTANCE, session.getDistance());

        int rowsAffected = 0;

        try {
            rowsAffected = mDb.update(TABLE_SESSIONS, values,
                    KEY_SESSIONS_ID + "=" + session.getId(), null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return rowsAffected;
    }

    /**
     * Closes the database
     */
    public void closeDB() {
        if (mDb != null && mDb.isOpen())
            mDb.close();
    }
}
