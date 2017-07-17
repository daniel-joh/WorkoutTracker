package com.daniel.workouttracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.daniel.workouttracker.helper.DBHelper;
import com.daniel.workouttracker.helper.HelperUtils;
import com.daniel.workouttracker.model.WorkoutLocation;
import com.daniel.workouttracker.model.WorkoutSession;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * MainActivity class
 *
 * @author Daniel Johansson
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        ShowDetailsDialog.SaveDialogListener {

    /**
     * GoogleMap
     */
    private GoogleMap mMap;

    /**
     * GoogleApiClient
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Last position of the user
     */
    private LatLng mLastPosition;

    /**
     * Permissions request flag
     */
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    /**
     * Boolean for permission (ACCESS_FINE_LOCATION) granted status
     */
    private boolean mLocationPermissionGranted = false;

    /**
     * Total distance of the WorkoutSession
     */
    private float mTotalDistance;

    /**
     * Elapsed time of the WorkoutSession in seconds
     */
    private long mElapsedTime;

    /**
     * Distance TextView
     */
    private TextView mTextviewDistance;

    /**
     * Elapsed time TextView
     */
    private TextView mTextviewTime;

    /**
     * Start session Button
     */
    private Button mButtonStart;

    /**
     * Handler for the elapsed time timer
     */
    private Handler mTimerHandler;

    /**
     * Timer Runnable
     */
    private Runnable mTimerRunnable;

    /**
     * WorkoutSession
     */
    private WorkoutSession mWorkoutSession;

    /**
     * Session id of the WorkoutSession
     */
    private long mSessionId;

    /**
     * DBHelper
     */
    private DBHelper mDb;

    /**
     * RouteBroadCastReceiver
     */
    private RouteBroadCastReceiver mRouteReceiver;

    /**
     * LocationRequest
     */
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        //Creates the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRouteReceiver = new RouteBroadCastReceiver();

        //If the TrackingService is running, stop it and set the SessionStatus to "readyToStart"
        if (TrackingService.isServiceRunning() == true) {
            stopService();
            setSessionStatus("readyToStart");
        }
        //If the TrackingService is not running, set the SessionStatus to "readyToStart"
        else {
            setSessionStatus("readyToStart");
        }

        mDb = new DBHelper(this.getApplicationContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();

        //If TrackingService isnt running - stop location updates
        if (mGoogleApiClient.isConnected() && TrackingService.isServiceRunning() == false) {
            stopLocationUpdates();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRouteReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mDb == null)
            mDb = new DBHelper(this.getApplicationContext());

        if (mRouteReceiver == null) {
            mRouteReceiver = new RouteBroadCastReceiver();
        }

        //Registers the RouteBroadcastReceiver
        IntentFilter filter = new IntentFilter("com.daniel.workouttracker.TrackingService");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRouteReceiver, filter);

        //If TrackingService isnt running - start location updates that arent part of the session
        if (mGoogleApiClient.isConnected() && TrackingService.isServiceRunning() == false) {
            startLocationUpdates();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopService();

        mDb.closeDB();
        mDb = null;
    }

    /**
     * Inits several UI components
     */
    public void initUI() {
        setContentView(R.layout.activity_main);

        //Sets up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();

        //Keeps the screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mTextviewDistance = (TextView) findViewById(R.id.textviewDistance);
        mTextviewTime = (TextView) findViewById(R.id.textviewTime);
        mButtonStart = (Button) findViewById(R.id.buttonStart);
    }

    /**
     * Sets the WorkoutSession´s status in SharedPreferences
     *
     * @param status status of the WorkoutSession. "readyToStart", "started" or "paused".
     */
    public void setSessionStatus(String status) {
        SharedPreferences sharedPref = getSharedPreferences("com.daniel.workouttracker.PREFERENCES",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("sessionStatus", status);
        editor.commit();
    }

    /**
     * Gets the WorkoutSession´s status from SharedPreferences
     *
     * @return the status of the WorkoutSession
     */
    public String getSessionStatus() {
        SharedPreferences sharedPref = getSharedPreferences("com.daniel.workouttracker.PREFERENCES",
                Context.MODE_PRIVATE);
        String status = sharedPref.getString("sessionStatus", "");
        return status;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Handles the action "View Sessions"
        if (id == R.id.action_view_sessions) {
            //If a workout session has been started, paused or resumed - show Toast message.
            //Otherwise start activity to view workout sessions
            if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")
                    || getSessionStatus().equals("paused")) {
                Toast.makeText(this, "A workout session is active. Stop it first!", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, ViewWorkoutSessionsActivity.class);
                startActivity(intent);
            }
        }

        //Handles the action "Reset Session"
        if (id == R.id.action_reset_session) {
            //If a workout session has been started, paused or resumed - show Toast message.
            //Otherwise reset session
            if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")
                    || getSessionStatus().equals("paused")) {
                Toast.makeText(this, "A workout session is active. Stop it first!", Toast.LENGTH_SHORT).show();
            } else {
                clearSession();         //Resets the session
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a LocationRequest that is set to the update interval of 5 seconds
     */
    public void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("google_connection", "Play services connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("google_connection", "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    //When the GoogleApiClient has connected
    @Override
    public void onConnected(Bundle connectionHint) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        //If permission ACCESS_FINE_LOCATION is granted, sets mLocationPermissionGranted
                        //and calls the initMap method
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = true;

                            initMap();

                            //If permission ACCESS_FINE_LOCATION is not granted, request permission from user
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {

                //If permission ACCESS_FINE_LOCATION is granted, sets mLocationPermissionGranted
                //and calls the initMap method
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    initMap();
                }
                //If the user refuses the permission request, the app is finished
                else {
                    Toast.makeText(this, "Permission not granted. Exiting app!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    /**
     * Inits the GoogleMap with the device´s last known location and other settings
     */
    @SuppressWarnings({"MissingPermission"})
    public void initMap() {
        if (mLocationPermissionGranted) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                LatLng position = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

                mMap.clear();
                mMap.resetMinMaxZoomPreference();

                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
        }
    }

    /**
     * Starts location updates when a WorkoutSession hasn´t been started
     */
    @SuppressWarnings({"MissingPermission"})
    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stops location updates (for when a WorkoutSession hasn´t been started)
     */
    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocationOnMap(location);
    }

    /**
     * Updates the GoogleMap with a new location (only for when a WorkoutSession hasn´t been started)
     *
     * @param location location to update map with
     */
    public void updateLocationOnMap(Location location) {
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
    }

    public void startWorkoutSession(View v) {
        startWorkoutSession();
    }

    /**
     * Starts the WorkoutSession
     */
    public void startWorkoutSession() {
        //When sessionStatus=="readyToStart" - Session has not been started before
        if (getSessionStatus().equals("readyToStart")) {
            setSessionStatus("started");

            //Location updates are removed (the TrackingService takes over this responsibility)
            stopLocationUpdates();

            String startTime = DateFormat.getTimeInstance().format(new Date());
            String startDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date());

            //Creates new WorkoutSession object and saves it to db
            mWorkoutSession = new WorkoutSession();
            mWorkoutSession.setStartTime(startTime);
            mWorkoutSession.setStartDate(startDate);
            mSessionId = mDb.createWorkoutSession(mWorkoutSession);        //Saves session to db
            mWorkoutSession.setId((int) mSessionId);

            mButtonStart.setText("Pause session");
            handleTimer();                                      //Handles the elapsed time timer
            startService();                                     //Starts the tracking service
        }

        //When sessionStatus=="paused - Session is paused and will be resumed
        //Timer for mElapsedTime is also restarted
        else if (getSessionStatus().equals("paused")) {
            setSessionStatus("resumed");
            mButtonStart.setText("Pause session");
            handleTimer();                                       //Handles the elapsed time timer
            startService();                                     //Starts the tracking service
        }

        //When sessionStatus=="started" or "resumed" - Session will be paused
        //The mElapsedTime timer is stopped
        else if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")) {
            setSessionStatus("paused");
            mButtonStart.setText("Resume session");
            handleTimer();                                      //Handles the elapsed time timer
            stopService();                                      //Starts the tracking service
        }
    }

    /**
     * Handles the elapsed time timer
     */
    public void handleTimer() {
        if (getSessionStatus().equals("started")) {
            mElapsedTime = 0;
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }
            };
            timerCount();
        } else if (getSessionStatus().equals("resumed")) {
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }
            };
            timerCount();
        } else
            mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    /**
     * Increments mElapsedTime each second and sets the TextView
     */
    public void timerCount() {
        mElapsedTime++;
        mTextviewTime.setText(DateUtils.formatElapsedTime(mElapsedTime));
        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }

    public void stopWorkoutSession(View v) {
        stopWorkoutSession();
    }

    /**
     * Stops the WorkoutSession
     */
    public void stopWorkoutSession() {
        //If a WorkoutSession has been started, paused or resumed - stop the WorkoutSession
        if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")
                || getSessionStatus().equals("paused")) {
            stopService();

            //Sets the WorkoutSession status to "readyToStart" (so that new sessions can be started)
            setSessionStatus("readyToStart");
            mButtonStart.setText("Start session");

            mTimerHandler.removeCallbacks(mTimerRunnable);

            updateSessionInDb();

            drawEndMarker();            //Draws the end marker on the map

            //Shows a dialog that lets the user view details of the WorkoutSession
            DialogFragment dialog = new ShowDetailsDialog();
            dialog.show(getSupportFragmentManager(), "ShowDetailsDialog");

            startLocationUpdates();     //Starts location updates (without a active WorkoutSession)
        }
        //If no WorkoutSession is active - show Toast message
        else
            Toast.makeText(this, "No workout session active!", Toast.LENGTH_SHORT).show();
    }

    //For the ShowDetailsDialog
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        startViewSessionDetails();
    }

    //For the ShowDetailsDialog
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }

    /**
     * Updates the WorkoutSession in the database
     */
    public void updateSessionInDb() {
        //Saves the last location to the database
        saveLastLocationToDb();

        mWorkoutSession.setDuration(mElapsedTime);

        //Formats and rounds the distance to 1 decimal
        float formattedDistance = HelperUtils.formatFloat(mTotalDistance);
        mWorkoutSession.setDistance(formattedDistance);

        //Updates the WorkoutSession in db with duration and distance
        mDb.updateWorkoutSession(mWorkoutSession);

        //Gets the list of locations from db and adds it to the WorkoutSession
        List<WorkoutLocation> locations = mDb.getLocationsFromSession(mSessionId);
        mWorkoutSession.setLocations(locations);
    }

    /**
     * Saves the last location of the WorkoutSession to the database
     */
    @SuppressWarnings({"MissingPermission"})
    public void saveLastLocationToDb() {
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng endPosition = new LatLng(lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude());

        mDb.createWorkoutLocation(mSessionId, String.valueOf(lastKnownLocation.getLatitude()),
                String.valueOf(lastKnownLocation.getLongitude()), HelperUtils.formatDouble(lastKnownLocation.getAltitude()),
                HelperUtils.convertSpeed(lastKnownLocation.getSpeed()), mElapsedTime);
    }

    /**
     * Starts the TrackingService
     */
    public void startService() {
        Intent serviceIntent = new Intent(this, TrackingService.class);
        serviceIntent.putExtra("sessionId", Long.toString(mSessionId));
        serviceIntent.putExtra("elapsedTime", Long.toString(mElapsedTime));

        this.startService(serviceIntent);
    }

    /**
     * Stops the TrackingService
     */
    public void stopService() {
        Intent stopServiceIntent = new Intent(MainActivity.this, TrackingService.class);
        stopService(stopServiceIntent);
    }

    /**
     * Clears the GoogleMap and the TextViews in the activity
     */
    public void clearSession() {
        mMap.clear();
        mTextviewTime.setText("");
        mTextviewDistance.setText("");
    }

    /**
     * Starts the ViewSessionDetailsActivity
     */
    public void startViewSessionDetails() {
        Intent intent = new Intent(this, ViewSessionDetailsActivity.class);
        intent.putExtra("session", mWorkoutSession);

        //Calculates average speed (in km/h) and puts it as an Extra in the intent
        intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(mWorkoutSession));
        startActivity(intent);
    }

    /**
     * Class for receiving location broadcasts from TrackingService
     */
    private class RouteBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.daniel.workouttracker.TrackingService")) {
                long sessionId = Long.parseLong(intent.getExtras().getString("sessionId"));

                WorkoutSession session = mDb.getWorkoutSession(sessionId);
                List<WorkoutLocation> locations = session.getLocations();

                if (locations.size() > 0) {
                    //Gets all LatLng points from the list of WorkoutLocations
                    List<LatLng> points = getPoints(locations);
                    mMap.clear();

                    drawRoute(points);
                    showDistance(points);

                }
            }
        }
    }

    /**
     * Gets a list of LatLng´s from the @param
     *
     * @param locations list of WorkoutLocations
     * @return list of LatLng
     */
    public List<LatLng> getPoints(List<WorkoutLocation> locations) {
        List<LatLng> points = new ArrayList<>();

        for (WorkoutLocation location : locations)
            points.add(new LatLng(Double.parseDouble(location.getLatitude()),
                    Double.parseDouble(location.getLongitude())));

        return points;
    }

    /**
     * Draws a polyline between all locations in the WorkoutSession, and adds a marker for the
     * start position
     *
     * @param points list of LatLng objects
     */
    public void drawRoute(List<LatLng> points) {
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(Color.RED);
        polyOptions.width(5);
        polyOptions.geodesic(true);

        //Adds a marker for the starting position
        LatLng startPosition = points.get(0);
        mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("START"));

        //Draws a polyline between all locations in the list
        polyOptions.addAll(points);
        mMap.addPolyline(polyOptions);

        mLastPosition = points.get(points.size() - 1);
        CameraUpdate cUpdate = CameraUpdateFactory.newLatLng(mLastPosition);
        mMap.animateCamera(cUpdate);

    }

    /**
     * Draws the end marker
     */
    public void drawEndMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(mLastPosition)
                .title("STOP"));
    }

    /**
     * Calculates the total distance between all locations
     *
     * @param points list of LatLngs
     * @return total distance
     */
    public float calculateDistance(List<LatLng> points) {
        float distance = 0;

        for (int i = 1; i < points.size(); i++) {
            float[] results = new float[2];
            Location.distanceBetween(points.get(i - 1).latitude, points.get(i - 1).longitude,
                    points.get(i).latitude, points.get(i).longitude, results);
            distance += results[0];
        }
        return distance;
    }

    /**
     * Shows the total distance of the WorkoutSession in the TextView
     *
     * @param points list of LatLngs
     */
    public void showDistance(List<LatLng> points) {
        mTotalDistance = calculateDistance(points);
        float displayDistance = mTotalDistance / 1000;       //Converts metres to km
        mTextviewDistance.setText(Float.toString(displayDistance) + " km");

        //Locale used for "." in formatting of String
        mTextviewDistance.setText(String.format(java.util.Locale.US, "%.2f", displayDistance) + " km");
    }

}

