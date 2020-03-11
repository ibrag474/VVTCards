package com.bjobs.vvtcards.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;

import com.bjobs.vvtcards.R;

/**
 * Created by Ibragim on 2017-08-01.
 */

public class CreateDelCard extends DialogFragment {

    private OnCompleteDelListener mDelListener;

    Bundle mArgs;
    String name;
    String id;

    public interface OnCompleteDelListener {
        void onCompleteDel(String number, String id);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mDelListener = (CreateDelCard.OnCompleteDelListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArgs = getArguments();
        name = mArgs.getString("name");
        id = mArgs.getString("id");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_deletecard, null))
                .setTitle(R.string.delCardTitle)
                // Add action buttons
                .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        onPressDel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CreateDelCard.this.getDialog().cancel();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    public void onPressDel() {
        this.mDelListener.onCompleteDel(name, id);
    }
}
