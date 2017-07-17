package com.daniel.workouttracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.daniel.workouttracker.helper.DBHelper;
import com.daniel.workouttracker.helper.HelperUtils;
import com.daniel.workouttracker.model.WorkoutSession;

import java.util.List;

/**
 * Activity class for viewing WorkoutSessions
 *
 * @author Daniel Johansson
 */
public class ViewWorkoutSessionsActivity extends AppCompatActivity implements
        DeleteSessionDialog.DeleteSessionDialogListener {

    /** ArrayAdapter */
    private WorkoutSessionArrayAdapter mSessionArrayAdapter;

    /** List of WorkoutSessions */
    private List<WorkoutSession> mSessions;

    /** Id of WorkoutSession */
    private long mSessionId;

    /** DBHelper object */
    private DBHelper mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sessions);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarViewSessions);
        setSupportActionBar(toolbar);
        toolbar.showOverflowMenu();

        //Gets a support ActionBar corresponding to this toolbar. For a child activity only.
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);         //Enables the Up button

        ListView listviewSessions = (ListView) findViewById(R.id.list);
        registerForContextMenu(listviewSessions);

        //Constructs the data source
        mDb = new DBHelper(this.getApplicationContext());
        mSessions = mDb.getAllWorkoutSessions();

        //Creates the adapter to convert the array to views
        mSessionArrayAdapter = new WorkoutSessionArrayAdapter(this, mSessions);

        //Attaches adapter to the listview
        listviewSessions.setAdapter(mSessionArrayAdapter);

        //Listener for the listview´s itemclicks
        listviewSessions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
                //Gets the WorkoutSession that was clicked on
                WorkoutSession session = (WorkoutSession) adapter.getItemAtPosition(position);

                //Starts the activity ViewSessionDetailsActivity with the extras
                Intent intent = new Intent(ViewWorkoutSessionsActivity.this, ViewSessionDetailsActivity.class);
                intent.putExtra("session", session);
                intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(session));
                intent.putExtra("maxSpeed", Float.toString(HelperUtils.getMaxSpeed(session)));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu_list, menu);
        }
    }

    /**
     * Handles the Contextmenu
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int listPosition = info.position;
        mSessionId = mSessions.get(listPosition).getId();

        if (item.getItemId() == R.id.delete) {
            DialogFragment dialog = new DeleteSessionDialog();
            dialog.show(getSupportFragmentManager(), "DeleteSessionDialog");
        }

        return true;
    }

    /**
     * Handles a positive click on the DeleteSessionDialog. Deletes the selected WorkoutSession,
     * updates data in the adapter and notifies that the data has changed
     *
     * @param dialog DeleteSessionDialog
     */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        mDb.deleteWorkoutSession(mSessionId);                     //Delete the selected WorkoutSession

        //Updates data in the adapter
        mSessions = mDb.getAllWorkoutSessions();            //Gets all WorkoutSessions from db
        mSessionArrayAdapter.clear();
        mSessionArrayAdapter.addAll(mSessions);

        //Notifies that the data set has changed to refresh the listview
        mSessionArrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
    }
}

