package com.daniel.workouttracker;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.daniel.workouttracker.helper.HelperUtils;
import com.daniel.workouttracker.model.WorkoutSession;

import java.util.List;

/**
 * ArrayAdapter class for the listview in ViewWorkoutSessionsActivity
 *
 * @author Daniel Johansson
 */
public class WorkoutSessionArrayAdapter extends ArrayAdapter<WorkoutSession> {

    public WorkoutSessionArrayAdapter(Context context, List<WorkoutSession> sessions) {
        super(context, 0, sessions);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Gets the data item for this position
        WorkoutSession session = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.session_item, parent, false);
        }

        TextView textviewSessionId = (TextView) convertView.findViewById(R.id.textviewSessionId);
        TextView textviewSessionStartDate = (TextView) convertView.findViewById(R.id.textviewSessionStartDate);
        TextView textviewSessionStartTime = (TextView) convertView.findViewById(R.id.textviewSessionStartTime);
        TextView textviewSessionDuration = (TextView) convertView.findViewById(R.id.textviewSessionDuration);
        TextView textviewSessionDistance = (TextView) convertView.findViewById(R.id.textviewSessionDistance);

        //Sets the text in the textviews
        textviewSessionId.setText("Session " + Integer.toString(session.getId()) + ":");
        textviewSessionStartDate.setText(session.getStartDate());
        textviewSessionStartTime.setText(session.getStartTime());
        textviewSessionDuration.setText(DateUtils.formatElapsedTime(session.getDuration()));

        //Shows the distance in km, formatted with 1 decimal
        float distance = HelperUtils.formatFloat(session.getDistance() / 1000);
        textviewSessionDistance.setText(Float.toString(distance) + " km");

        return convertView;

    }
}
