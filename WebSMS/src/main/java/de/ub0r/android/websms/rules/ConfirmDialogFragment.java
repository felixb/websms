package de.ub0r.android.websms.rules;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDialogFragment;


/**
 * Dialog Fragment with a message and Confirm / Decline buttons.
 * Target Fragment must implement the DialogListener interface.
 */
public class ConfirmDialogFragment
        extends AppCompatDialogFragment {

    public static final String FRAG_TAG  = "ConfirmDialogFragment";

    public static final int ACTION_NONE   = 0;
    public static final int ACTION_OK     = 1;
    public static final int ACTION_CANCEL = 2;

    private static final String ARG_DIALOG_ID         = "dialogId";
    private static final String ARG_CONTENT           = "content";
    private static final String ARG_CONFIRM_RESID     = "confirmResId";
    private static final String ARG_DECLINE_RESID     = "declineResId";
    private static final String ARG_ACTION_ON_DISMISS = "actionOnDismiss";


    public interface DialogListener {
        void onConfirmOk(int dialogId);
        void onConfirmCancelled(int dialogId);
    }


    public static ConfirmDialogFragment newInstance(int dialogId,
            String content, int confirmResId, int declineResId,
            int actionOnDismiss,
            Fragment targetFragment /*must implement the DialogListener interface*/ ) {

        ConfirmDialogFragment confDialogFragment = new ConfirmDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_ID, dialogId);
        args.putString(ARG_CONTENT, content);
        args.putInt(ARG_CONFIRM_RESID, confirmResId);
        args.putInt(ARG_DECLINE_RESID, declineResId);
        args.putInt(ARG_ACTION_ON_DISMISS, actionOnDismiss);
        confDialogFragment.setArguments(args);

        confDialogFragment.setTargetFragment(targetFragment, 0);

        return confDialogFragment;
    }


    private DialogListener getListener() {
        return (DialogListener) getTargetFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(null)
            .setIcon(null)
            .setMessage(getArguments().getString(ARG_CONTENT))
            .setPositiveButton(getArguments().getInt(ARG_CONFIRM_RESID), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getListener().onConfirmOk(getArguments().getInt(ARG_DIALOG_ID));
                }
            })
            .setNegativeButton(getArguments().getInt(ARG_DECLINE_RESID), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getListener().onConfirmCancelled(getArguments().getInt(ARG_DIALOG_ID));
                }
            });
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        int actionOnDismiss = getArguments().getInt(ARG_ACTION_ON_DISMISS);
        int dialogId = getArguments().getInt(ARG_DIALOG_ID);

        if (actionOnDismiss == ACTION_OK) {
            getListener().onConfirmOk(dialogId);
        } else if (actionOnDismiss == ACTION_CANCEL) {
            getListener().onConfirmCancelled(dialogId);
        } else if (actionOnDismiss == ACTION_NONE) {
            // nothing to do
        }
        super.onCancel(dialog);
    }

}
