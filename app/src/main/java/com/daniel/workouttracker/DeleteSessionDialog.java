package com.daniel.workouttracker;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Class for the DeleteSessionDialog
 *
 * @author Daniel Johansson
 */
public class DeleteSessionDialog extends DialogFragment {
    DeleteSessionDialogListener mListener;

    public interface DeleteSessionDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(getContext());

        try {
            mListener = (DeleteSessionDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement DeleteSessionDialogListener");
        }
    }

    /** Creates the DeleteSessionDialog */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Delete session?")
                .setPositiveButton("Delete session", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(DeleteSessionDialog.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(DeleteSessionDialog.this);
                    }
                });

        return builder.create();
    }
}


