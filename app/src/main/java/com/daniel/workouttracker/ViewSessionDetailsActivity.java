package com.daniel.workouttracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.daniel.workouttracker.helper.HelperUtils;
import com.daniel.workouttracker.model.WorkoutLocation;
import com.daniel.workouttracker.model.WorkoutSession;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Activity class for viewing WorkoutSession details
 *
 * @author Daniel Johansson
 */
public class ViewSessionDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * GoogleMap
     */
    private GoogleMap mMap;

    /**
     * WorkoutSession
     */
    private WorkoutSession mSession;

    /**
     * Start time TextView
     */
    private TextView mTextviewStartTime;

    /**
     * Start date TextView
     */
    private TextView mTextviewDate;

    /**
     * Distance TextView
     */
    private TextView mTextviewDistance;

    /**
     * Duration TextView
     */
    private TextView mTextviewDuration;

    /**
     * Average speed TextView
     */
    private TextView mTextviewAverageSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();

        initTextViews();
    }

    /**
     * Inits several UI components
     */
    public void initUI() {
        setContentView(R.layout.activity_view_session_details);

        //Sets up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarMapView);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();

        //Gets a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);         //Enables the Up button

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        supportMapFragment.getMapAsync(this);

        mSession = (WorkoutSession) getIntent().getParcelableExtra("session");

        mTextviewStartTime = (TextView) findViewById(R.id.textviewStartTime);
        mTextviewDate = (TextView) findViewById(R.id.textviewDate);
        mTextviewDistance = (TextView) findViewById(R.id.textviewDistance);
        mTextviewDuration = (TextView) findViewById(R.id.textviewDuration);
        mTextviewAverageSpeed = (TextView) findViewById(R.id.textviewAverageSpeed);
    }

    /**
     * When the GoogleMap is ready - sets options and calls initMap()
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);

        //Zooming and all types of gestures are not allowed
        mMap.getUiSettings().setAllGesturesEnabled(false);

        initMap();
    }

    /**
     * Draws a polyline between all WorkoutLocations in the WorkoutSession. Also adds start
     * and end markers and bounds for the GoogleMap.
     */
    public void initMap() {
        //Gets all WorkoutLocations from the WorkoutSession
        List<WorkoutLocation> locations = mSession.getLocations();

        PolylineOptions pOptions = new PolylineOptions();
        pOptions.color(Color.RED);
        pOptions.width(5);

        LatLng startPosition = null;
        LatLng endPosition = null;
        LatLng latlng = null;

        int i = 0;

        //Loops through all WorkoutLocations, sets startPosition and endPosition and adds
        //all locations to the PolylineOptions
        for (WorkoutLocation location : locations) {
            latlng = new LatLng(Double.parseDouble(location.getLatitude()),
                    Double.parseDouble(location.getLongitude()));
            if (i == 0)
                startPosition = latlng;
            else if (i == locations.size() - 1)
                endPosition = latlng;

            pOptions.add(latlng);
            i++;
        }

        //Adds Markers for the startPosition and endPosition and the polyline
        mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("START"));
        mMap.addPolyline(pOptions);
        mMap.addMarker(new MarkerOptions()
                .position(endPosition)
                .title("END"));

        List<LatLng> pointsList = pOptions.getPoints();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        //Sets bounds (includes all locations)
        for (LatLng point : pointsList) {
            bounds.include(point);
        }

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds.build(), 400, 400, 0);
        mMap.moveCamera(cu);                    //Moves camera to include all bounds
    }

    /**
     * Starts the ViewSessionStatistics activity
     */
    public void viewStatistics(View v) {
        Intent intent = new Intent(this, ViewSessionStatisticsActivity.class);
        intent.putExtra("session", mSession);
        intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(mSession));
        intent.putExtra("maxSpeed", Float.toString(HelperUtils.getMaxSpeed(mSession)));

        intent.putExtra("parentActivity", new String("main"));

        startActivity(intent);
    }

    /**
     * Inits the textviews
     */
    public void initTextViews() {

        //Sets the start date
        String startDate = mSession.getStartDate();
        mTextviewDate.setText("Start date: " + startDate);

        //Sets the start time
        String startTime = mSession.getStartTime();
        mTextviewStartTime.setText("Start time: " + startTime);

        //Sets and formats the duration
        mTextviewDuration.setText("Duration: " + DateUtils.formatElapsedTime(mSession.getDuration()));

        //Sets the distance in km, formatted with 1 decimal
        float distance = HelperUtils.formatFloat(mSession.getDistance() / 1000);
        mTextviewDistance.setText("Distance: " + Float.toString(distance) + " km");

        //Sets the average speed
        double averageSpeed = getIntent().getDoubleExtra("averageSpeed", 0.0);
        mTextviewAverageSpeed.setText("Avg. speed: " + averageSpeed + " km/h");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_main) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_sessions, menu);
        return true;
    }
}