package com.daniel.workouttracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.MenuItem;
import android.widget.TextView;

import com.daniel.workouttracker.helper.HelperUtils;
import com.daniel.workouttracker.model.WorkoutLocation;
import com.daniel.workouttracker.model.WorkoutSession;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Activity class for viewing WorkoutSession statistics
 *
 * @author Daniel Johansson
 */
public class ViewSessionStatisticsActivity extends AppCompatActivity {

    /**
     * Speed graph
     */
    private GraphView mSpeedGraph;

    /**
     * Altitude graph
     */
    private GraphView mAltitudeGraph;

    /**
     * Speed&Altitude graph
     */
    private GraphView mSpeedAltitudeGraph;

    /**
     * Altitude series
     */
    private LineGraphSeries<DataPoint> mAltitudeSeries;

    /**
     * Speed series
     */
    private LineGraphSeries<DataPoint> mSpeedSeries;

    /**
     * Start time TextView
     */
    private TextView mTextviewStartTime;

    /**
     * Start date TextView
     */
    private TextView mTextviewDate;

    /**
     * Duration TextView
     */
    private TextView mTextviewDuration;

    /**
     * Distance TextView
     */
    private TextView mTextviewDistance;

    /**
     * Average speed TextView
     */
    private TextView mTextviewAverageSpeed;

    /**
     * Max speed TextView
     */
    private TextView mTextviewMaxSpeed;

    /**
     * WorkoutSession
     */
    private WorkoutSession mSession;

    /**
     * Elapsed time
     */
    private double mElapsedTime;

    /**
     * Time unit, for the graphs
     */
    private static String timeUnit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSession = (WorkoutSession) getIntent().getParcelableExtra("session");

        initUI();

        setTimeUnit(mSession.getDuration());

        initSpeedGraph();

        initAltitudeGraph();

        initSpeedAltitudeGraph();

        initTextViews();
    }

    /**
     * Inits several UI components
     */
    public void initUI() {
        setContentView(R.layout.activity_view_session_statistics);

        //Sets up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarShowStatistics);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);         //Enables the Up button

        mSpeedGraph = (GraphView) findViewById(R.id.speedGraph);
        mAltitudeGraph = (GraphView) findViewById(R.id.altitudeGraph);
        mSpeedAltitudeGraph = (GraphView) findViewById(R.id.speedAltitudeGraph);
        mTextviewStartTime = (TextView) findViewById(R.id.textviewStartTime);
        mTextviewDate = (TextView) findViewById(R.id.textviewDate);
        mTextviewDuration = (TextView) findViewById(R.id.textviewDuration);
        mTextviewDistance = (TextView) findViewById(R.id.textviewDistance);
        mTextviewAverageSpeed = (TextView) findViewById(R.id.textviewAverageSpeed);
        mTextviewMaxSpeed = (TextView) findViewById(R.id.textviewMaxSpeed);
    }

    /**
     * Calculates the time unit of the graphs (seconds/minutes/hours)
     *
     * @param duration duration of the WorkoutSession
     */
    public static void setTimeUnit(long duration) {
        timeUnit = "";
        if (duration < 60)
            timeUnit = "seconds";
        else if (duration > 60 && duration < 60 * 60)
            timeUnit = "minutes";
        else if (duration >= 60 * 60)
            timeUnit = "hours";
    }

    /**
     * Converts the elapsed time to the right time unit
     *
     * @param elapsedTime elapsed time
     * @return converted elapsed time (in the right time unit)
     */
    public double convertElapsedTime(long elapsedTime) {
        double time = 0;

        if (timeUnit.equals("minutes"))
            time = (double) elapsedTime / 60;
        else if (timeUnit.equals("hours"))
            time = (double) elapsedTime / (60 * 60);

        return time;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Navigates to parent activity with the Extras
        if (id == R.id.home) {
            Intent intent = NavUtils.getParentActivityIntent(this);
            intent.putExtra("session", mSession);
            intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(mSession));
            NavUtils.navigateUpTo(this, intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Inits the Speed graph
     */
    public void initSpeedGraph() {
        DataPoint[] dp = new DataPoint[mSession.getLocations().size()];

        int i = 0;

        //Adds datapoints (speeds and time) from all WorkoutLocations in the WorkoutSession
        for (WorkoutLocation location : mSession.getLocations()) {

            //If timeUnit is minutes or hours, convert the elapsedTime to the right time unit,
            //then create the DataPoint
            if (timeUnit.equals("minutes") || timeUnit.equals("hours")) {
                mElapsedTime = convertElapsedTime(location.getElapsedTime());
                dp[i] = new DataPoint(mElapsedTime, location.getSpeed());
            }
            //If timeUnit is seconds, just create the DataPoint
            else {
                mElapsedTime = location.getElapsedTime();
                dp[i] = new DataPoint(mElapsedTime, location.getSpeed());
            }
            i++;
        }

        //Creates the speed series from the DataPoints
        mSpeedSeries = new LineGraphSeries<>(dp);

        //Sets up the speed series
        mSpeedSeries.setAnimated(true);
        mSpeedSeries.setColor(Color.argb(255, 255, 60, 60));

        //Sets the x axis bounds of the graph manually
        mSpeedGraph.getViewport().setXAxisBoundsManual(true);
        mSpeedGraph.getViewport().setMaxX(mElapsedTime);

        //Sets up the speed graph
        mSpeedGraph.setTitle("Speed");
        mSpeedGraph.setTitleTextSize(50);
        mSpeedGraph.getGridLabelRenderer().setHorizontalAxisTitle(timeUnit);
        mSpeedGraph.getGridLabelRenderer().setVerticalAxisTitle("km/h");
        mSpeedGraph.getGridLabelRenderer().setLabelVerticalWidth(30);
        mSpeedGraph.getGridLabelRenderer().setPadding(45);
        mSpeedGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30);
        mSpeedGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30);

        //Adds the speed series to the speed graph
        mSpeedGraph.addSeries(mSpeedSeries);
    }

    /**
     * Inits the Altitude graph
     */
    public void initAltitudeGraph() {
        DataPoint[] dp = new DataPoint[mSession.getLocations().size()];

        int i = 0;

        //Adds datapoints (altitude and time) from all WorkoutLocations in the WorkoutSession
        for (WorkoutLocation location : mSession.getLocations()) {

            //If timeUnit is minutes or hours, convert the elapsedTime to the right time unit,
            //then create the DataPoint
            if (timeUnit.equals("minutes") || timeUnit.equals("hours")) {
                mElapsedTime = convertElapsedTime(location.getElapsedTime());
                dp[i] = new DataPoint(mElapsedTime, location.getAltitude());
            }
            //If timeUnit is seconds, just create the DataPoint
            else {
                mElapsedTime = location.getElapsedTime();
                dp[i] = new DataPoint(mElapsedTime, location.getAltitude());
            }
            i++;
        }

        //Creates the altitude series from the DataPoints
        mAltitudeSeries = new LineGraphSeries<>(dp);

        //Sets up the altitude series
        mAltitudeSeries.setAnimated(true);
        mAltitudeSeries.setColor(Color.argb(255, 255, 60, 60));

        //Sets the x axis bounds of the graph manually
        mAltitudeGraph.getViewport().setXAxisBoundsManual(true);
        mAltitudeGraph.getViewport().setMaxX(mElapsedTime);

        //Sets up the altitude graph
        mAltitudeGraph.setTitle("Altitude");
        mAltitudeGraph.setTitleTextSize(50);
        mAltitudeGraph.getGridLabelRenderer().setHorizontalAxisTitle(timeUnit);
        mAltitudeGraph.getGridLabelRenderer().setVerticalAxisTitle("metres");
        mAltitudeGraph.getGridLabelRenderer().setLabelVerticalWidth(30);
        mAltitudeGraph.getGridLabelRenderer().setPadding(60);
        mAltitudeGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30);
        mAltitudeGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30);

        //Adds the altitude series to the altitude graph
        mAltitudeGraph.addSeries(mAltitudeSeries);
    }

    /**
     * Inits the Speed&Altitude graph
     */
    public void initSpeedAltitudeGraph() {

        //Sets up the graph
        mSpeedAltitudeGraph.getGridLabelRenderer().setHorizontalAxisTitle(timeUnit);
        mSpeedAltitudeGraph.getGridLabelRenderer().setVerticalAxisTitle("km/h");
        mSpeedAltitudeGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30);
        mSpeedAltitudeGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30);
        mSpeedAltitudeGraph.getGridLabelRenderer().setLabelVerticalWidth(30);
        mSpeedAltitudeGraph.getGridLabelRenderer().setPadding(50);
        mSpeedAltitudeGraph.setTitle("Speed & Altitude");
        mSpeedAltitudeGraph.setTitleTextSize(50);

        //Sets the speed series for the graph
        mSpeedAltitudeGraph.addSeries(mSpeedSeries);

        //Sets the altitude series for the second scale
        mSpeedAltitudeGraph.getSecondScale().addSeries(mAltitudeSeries);

        //Sets the max x axis value of the graph
        mSpeedAltitudeGraph.getViewport().setXAxisBoundsManual(true);
        mSpeedAltitudeGraph.getViewport().setMaxX(mElapsedTime);

        //Sets max Y (altitude) of the second scale. Uses integer division to round up to nearest 50
        int maxYAltitude = ((int) (HelperUtils.getMaxAltitude(mSession) + 49) / 50) * 50;
        mSpeedAltitudeGraph.getSecondScale().setMaxY(maxYAltitude);

        //Sets y axis min value for the second scale
        mSpeedAltitudeGraph.getSecondScale().setMinY(0);

        //Sets color of the series
        mAltitudeSeries.setColor(Color.GREEN);
        mSpeedAltitudeGraph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GREEN);
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

        //Sets the max speed
        String maxSpeed = getIntent().getStringExtra("maxSpeed");
        mTextviewMaxSpeed.setText("Max speed: " + maxSpeed + " km/h");
    }
}
