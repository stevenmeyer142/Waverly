package com.brazedblue.waverly;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by stevemeyer on 8/27/15.
 */
public class ResultViewChoice extends DialogFragment {

    public interface ResultViewChoiceListener {
        void editText();
        void editPicture();
        void editPhotoAlbum();
    }

    // Use this instance of the interface to deliver action events
    ResultViewChoiceListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ResultViewChoiceListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(com.brazedblue.waverly.R.string.result_edit)
                .setItems(com.brazedblue.waverly.R.array.result_edit_choices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        switch (which)
                        {
                            case 0:
                                mListener.editText();
                                break;
                            case 1:
                                mListener.editPicture();
                                break;
                            case 2:
                                mListener.editPhotoAlbum();
                                break;
                        }
                     }
                });
        return builder.create();
    }
}
