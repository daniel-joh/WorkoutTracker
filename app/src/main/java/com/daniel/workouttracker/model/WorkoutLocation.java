package com.daniel.workouttracker.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class for the WorkoutLocation
 *
 * @author Daniel Johansson
 */
public class WorkoutLocation implements Parcelable {

    /**
     * Id of the WorkoutLocation
     */
    private int mId;

    /**
     * Id of the WorkoutSession (used as foreign key)
     */
    private int mSessionId;

    /**
     * Latitude of the WorkoutLocation
     */
    private String mLatitude;

    /**
     * Longitude of the WorkoutLocation
     */
    private String mLongitude;

    /**
     * Speed registered in the WorkoutLocation, in m/s
     */
    private float mSpeed;

    /**
     * Altitude registered in the WorkoutLocation, in metres
     */
    private double mAltitude;

    /**
     * Elapsed time of the WorkoutSession, when the WorkoutLocation was registered
     */
    private long mElapsedTime;

    public WorkoutLocation() {
    }

    /**
     * Constructor
     *
     * @param latitude    latitude of the WorkoutLocation
     * @param longitude   longitude of the WorkoutLocation
     * @param speed       speed registered in the WorkoutLocation
     * @param altitude    altitude registered in the WorkoutLocation
     * @param elapsedTime elapsed time of the WorkoutSession, when the WorkoutLocation was registered
     */
    public WorkoutLocation(String latitude, String longitude, float speed, double altitude, long elapsedTime) {
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mSpeed = speed;
        this.mAltitude = altitude;
        this.mElapsedTime = elapsedTime;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public String getLatitude() {
        return mLatitude;
    }

    public void setLatitude(String latitude) {
        this.mLatitude = latitude;
    }

    public String getLongitude() {
        return mLongitude;
    }

    public void setLongitude(String longitude) {
        this.mLongitude = longitude;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float speed) {
        this.mSpeed = speed;
    }

    public double getAltitude() {
        return mAltitude;
    }

    public void setAltitude(double altitude) {
        this.mAltitude = altitude;
    }

    public int getSessionId() {
        return mSessionId;
    }

    public void setSessionId(int sessionId) {
        this.mSessionId = sessionId;
    }

    public long getElapsedTime() {
        return mElapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.mElapsedTime = elapsedTime;
    }

    /**
     * Writes to parcel
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mLatitude);
        out.writeString(mLongitude);
        out.writeFloat(mSpeed);
        out.writeDouble(mAltitude);
        out.writeLong(mElapsedTime);
    }

    public static final Parcelable.Creator<WorkoutLocation> CREATOR = new Parcelable.Creator<WorkoutLocation>() {
        public WorkoutLocation createFromParcel(Parcel in) {
            return new WorkoutLocation(in);
        }

        public WorkoutLocation[] newArray(int size) {
            return new WorkoutLocation[size];
        }
    };

    /**
     * Constructor used by Parcelable.Creator
     */
    private WorkoutLocation(Parcel in) {
        mLatitude = in.readString();
        mLongitude = in.readString();
        mSpeed = in.readFloat();
        mAltitude = in.readDouble();
        mElapsedTime = in.readLong();
    }

    public int describeContents() {
        return 0;
    }

}
