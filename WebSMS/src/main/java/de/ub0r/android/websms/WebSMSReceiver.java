/*
 * Copyright (C) 2010-2011 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * Fetch all incoming Broadcasts and forward them to WebSMS.
 *
 * @author flx
 */
public final class WebSMSReceiver extends BroadcastReceiver {

    /**
     * Tag for debug output.
     */
    private static final String TAG = "bcr";

    /**
     * {@link Uri} for saving messages.
     */
    private static final Uri URI_SMS = Uri.parse("content://sms");

    /**
     * {@link Uri} for saving sent messages.
     */
    private static final Uri URI_SENT = Uri.parse("content://sms/sent");

    /**
     * Projection for getting the id.
     */
    private static final String[] PROJECTION_ID = new String[]{BaseColumns._ID};

    /**
     * Intent's scheme to send sms.
     */
    private static final String INTENT_SCHEME_SMSTO = "smsto";

    /**
     * ACTION for publishing information about sent websms.
     */
    private static final String ACTION_CM_WEBSMS = "de.ub0r.android.callmeter.SAVE_WEBSMS";

    private static final String ACTION_SMSDROID_WEBSMS = "de.ub0r.android.websms.SEND_SUCCESSFUL";

    /**
     * Extra holding uri of sent sms.
     */
    private static final String EXTRA_WEBSMS_URI = "uri";

    /**
     * Extra holding name of connector.
     */
    private static final String EXTRA_WEBSMS_CONNECTOR = "connector";

    /**
     * Vibrate x seconds on send.
     */
    private static final long VIBRATOR_SEND = 100L;

    /**
     * SMS DB: address.
     */
    static final String ADDRESS = "address";
    /** SMS DB: person. */
    // private static final String PERSON = "person";

    /**
     * SMS DB: date.
     */
    private static final String DATE = "date";

    /**
     * SMS DB: read.
     */
    static final String READ = "read";
    /** SMS DB: status. */
    // private static final String STATUS = "status";

    /**
     * SMS DB: type.
     */
    static final String TYPE = "type";

    /**
     * SMS DB: body.
     */
    static final String BODY = "body";

    /**
     * SMS DB: type - sent.
     */
    static final int MESSAGE_TYPE_SENT = 2;

    /**
     * SMS DB: type - draft.
     */
    static final int MESSAGE_TYPE_DRAFT = 3;

    /**
     * Next notification ID.
     */
    private static int nextNotificationID = 1;

    /**
     * LED color for notification.
     */
    private static final int NOTIFICATION_LED_COLOR = 0xffff0000;

    /**
     * LED blink on (ms) for notification.
     */
    private static final int NOTIFICATION_LED_ON = 500;

    /**
     * LED blink off (ms) for notification.
     */
    private static final int NOTIFICATION_LED_OFF = 2000;

    /**
     * Tag for notification about resending
     */
    private static final String NOTIFICATION_RESENDING_TAG = "resending";

    /**
     * Tag for notification about cancelling a resend
     */
    private static final String NOTIFICATION_CANCELLING_RESEND_TAG = "cancelling_resend";

    private static final long RESEND_DELAY_MS = 5000;

    /**
     * List of ids of messages that should not be resent any more.
     */
    private static List<Long> resendCancelledMsgIds = new ArrayList<Long>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        Log.d(TAG, "action: " + action);
        if (action == null) {
            return;
        }
        if (Connector.ACTION_INFO.equals(action)) {
            WebSMSReceiver.handleInfoAction(context, intent);

        } else if (Connector.ACTION_CAPTCHA_REQUEST.equals(action)) {
            final Intent i = new Intent(context, CaptchaActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //noinspection ConstantConditions
            i.putExtras(intent.getExtras());
            context.startActivity(i);

        } else if (Connector.ACTION_CANCEL.equals(action)) {
            WebSMSReceiver.handleCancelAction(context, intent);

        } else if (Connector.ACTION_RESEND.equals(action)) {
            WebSMSReceiver.handleResendAction(context, intent);
        }
    }

    /**
     * Fetch INFO broadcast.
     *
     * @param context context
     * @param intent  intent
     */
    private static void handleInfoAction(final Context context, final Intent intent) {
        final ConnectorSpec specs = new ConnectorSpec(intent);
        final ConnectorCommand command = new ConnectorCommand(intent);

        if (specs.getBundle().isEmpty()) {
            // security check. some other apps may send faulty broadcasts
            return;
        }

        try {
            WebSMS.addConnector(specs, command);
        } catch (Exception e) {
            Log.e(TAG, "error while receiving broadcast", e);
        }
        // save send messages
        if (command.getType() == ConnectorCommand.TYPE_SEND) {
            handleSendCommand(context, specs, command);
        }
    }

    /**
     * Handle result of message sending.
     *
     * @param context context
     * @param specs   {@link de.ub0r.android.websms.connector.common.ConnectorSpec}
     * @param command {@link de.ub0r.android.websms.connector.common.ConnectorCommand}
     */
    static void handleSendCommand(final Context context, final ConnectorSpec specs,
            final ConnectorCommand command) {

        boolean isHandled = false;
        final SharedPreferences p = PreferenceManager
                .getDefaultSharedPreferences(context);

        if (!specs.hasStatus(ConnectorSpec.STATUS_ERROR)) {
            // Sent successfully
            saveMessage(context, specs, command, MESSAGE_TYPE_SENT);
            if (p.getBoolean(WebSMS.PREFS_SEND_VIBRATE, false)) {
                final Vibrator v = (Vibrator) context
                        .getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(VIBRATOR_SEND);
                    v.cancel();
                }
            }
            isHandled = true;
            messageCompleted(context, command);
        }

        if (!isHandled) {
            // Resend if possible (network might be down temporarily or an odd
            // failure on the provider's web side)
            final int maxResendCount = de.ub0r.android.lib.Utils.parseInt(
                    p.getString(WebSMS.PREFS_MAX_RESEND_COUNT, "0"), 0);
            if (maxResendCount > 0) {
                int wasResendCount = command.getResendCount();

                if (wasResendCount < maxResendCount
                        && !isResendCancelled(command.getMsgId())) {

                    // schedule resend
                    command.setResendCount(wasResendCount + 1);
                    displayResendingNotification(context, command);
                    scheduleMessageResend(context, specs, command);

                    isHandled = true;
                }
            }
        }

        if (!isHandled) {
            // Display notification if sending failed
            displaySendingFailedNotification(context, specs, command);
            messageCompleted(context, command);
        }
    }

    /**
     * Handle cancellation request.
     *
     * @param context context
     * @param intent  intent
     */
    private static void handleCancelAction(final Context context, final Intent intent) {
        final ConnectorCommand command = new ConnectorCommand(intent);
        cancelResend(command.getMsgId());
        displayCancellingResendNotification(context, command);
    }

    /**
     * Handle resend request.
     *
     * @param context context
     * @param intent  intent
     */
    private static void handleResendAction(final Context context, final Intent intent) {

        final ConnectorSpec connector = new ConnectorSpec(intent);
        final ConnectorCommand command = new ConnectorCommand(intent);
        long msgId = command.getMsgId();

        if (!isResendCancelled(msgId)) {
            WebSMS.runCommand(context, connector, command);
        } else {
            displaySendingFailedNotification(context, connector, command);
            messageCompleted(context, command);
        }
    }

    /**
     * Displays notification if sending failed
     *
     * @param context context
     * @param specs   {@link de.ub0r.android.websms.connector.common.ConnectorSpec}
     * @param command {@link de.ub0r.android.websms.connector.common.ConnectorCommand}
     */
    private static void displaySendingFailedNotification(final Context context,
            final ConnectorSpec specs, final ConnectorCommand command) {

        final SharedPreferences p = PreferenceManager
                .getDefaultSharedPreferences(context);

        String to = Utils.joinRecipients(command.getRecipients(), ", ");

        Notification n = new Notification(R.drawable.stat_notify_sms_failed,
                context.getString(R.string.notify_failed_),
                System.currentTimeMillis());
        final Intent i = new Intent(Intent.ACTION_SENDTO,
                Uri.parse(INTENT_SCHEME_SMSTO + ":" + Uri.encode(to)), context,
                WebSMS.class);
        // add pending intent
        i.putExtra(Intent.EXTRA_TEXT, command.getText());
        i.putExtra(WebSMS.EXTRA_ERRORMESSAGE, specs.getErrorMessage());
        i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        final PendingIntent cIntent = PendingIntent.getActivity(context, 0, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        n.setLatestEventInfo(context, context.getString(R.string.notify_failed)
                        + " " + specs.getErrorMessage(), to + ": " + command.getText(),
                cIntent
        );
        n.flags |= Notification.FLAG_AUTO_CANCEL;

        n.flags |= Notification.FLAG_SHOW_LIGHTS;
        n.ledARGB = NOTIFICATION_LED_COLOR;
        n.ledOnMS = NOTIFICATION_LED_ON;
        n.ledOffMS = NOTIFICATION_LED_OFF;

        final boolean vibrateOnFail = p.getBoolean(WebSMS.PREFS_FAIL_VIBRATE,
                false);
        final String s = p.getString(WebSMS.PREFS_FAIL_SOUND, null);
        Uri soundOnFail;
        if (s == null || s.length() <= 0) {
            soundOnFail = null;
        } else {
            soundOnFail = Uri.parse(s);
        }

        if (vibrateOnFail) {
            n.defaults |= Notification.DEFAULT_VIBRATE;
        }
        n.sound = soundOnFail;

        NotificationManager mNotificationMgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationMgr.notify(getNotificationID(), n);

        // show a toast as well
        final String em = specs.getErrorMessage();
        if (em != null) {
            Toast.makeText(context, em, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Displays (or updates) notification about resending a failed message.
     *
     * @param context context
     * @param command {@link ConnectorCommand}
     */
    private static void displayResendingNotification(final Context context,
            final ConnectorCommand command) {

        long msgId = command.getMsgId();

        Notification n = new Notification(R.drawable.stat_notify_resending,
                context.getString(R.string.notify_failed_now_resending),
                System.currentTimeMillis());

        // Clicking on the notification will send a cancellation request
        final Intent i = new Intent(Connector.ACTION_CANCEL);
        command.setToIntent(i);
        PendingIntent pIntent = PendingIntent.getBroadcast(context,
                (int) msgId, i, PendingIntent.FLAG_UPDATE_CURRENT);
        n.setLatestEventInfo(context,
                context.getString(R.string.resending_failed_msg_),
                getResendSummary(context, command), pIntent);

        n.flags |= Notification.FLAG_NO_CLEAR;
        n.flags |= Notification.FLAG_ONGOING_EVENT;

        NotificationManager mNotificationMgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // There might be several messages being resent,
        // so we use msgId to distinguish them
        mNotificationMgr.notify(NOTIFICATION_RESENDING_TAG, (int) msgId, n);
    }

    /**
     * Displays notification about cancelling a resend.
     *
     * @param context context
     * @param command {@link ConnectorCommand}
     */
    private static void displayCancellingResendNotification(final Context context,
            final ConnectorCommand command) {

        long msgId = command.getMsgId();

        Notification n = new Notification(R.drawable.stat_notify_resending,
                context.getString(R.string.cancelling_resend),
                System.currentTimeMillis());

        // on click, do nothing
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) msgId,
                new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);

        n.setLatestEventInfo(context,
                context.getString(R.string.cancelling_resend),
                getResendSummary(context, command), pIntent);

        n.flags |= Notification.FLAG_AUTO_CANCEL;
        n.flags |= Notification.FLAG_ONGOING_EVENT;

        NotificationManager mNotificationMgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationMgr.cancel(NOTIFICATION_RESENDING_TAG, (int) msgId);
        mNotificationMgr.notify(NOTIFICATION_CANCELLING_RESEND_TAG,
                (int) msgId, n);
    }

    /**
     * Returns a brief description of a resend attempt.
     *
     * @param context context
     * @param command {@link ConnectorCommand}
     * @return description
     */
    private static String getResendSummary(final Context context,
            final ConnectorCommand command) {
        String to = Utils.joinRecipients(command.getRecipients(), ", ");
        return context.getString(R.string.attempt) + ": "
                + command.getResendCount() + ", "
                + context.getString(R.string.to) + ": " + to;
    }

    /**
     * Cleans up after a message sending has been completed (successfully or not).
     *
     * @param context context
     * @param command {@link ConnectorCommand}
     */
    private static void messageCompleted(final Context context,
            final ConnectorCommand command) {
        long msgId = command.getMsgId();

        // clear notification
        NotificationManager mNotificationMgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationMgr.cancel(NOTIFICATION_RESENDING_TAG, (int) msgId);
        mNotificationMgr
                .cancel(NOTIFICATION_CANCELLING_RESEND_TAG, (int) msgId);

        // clear flags
        resendCancelledMsgIds.remove(msgId);
    }

    /**
     * Checks if this message should not be sent any more.
     *
     * @param msgId message id
     * @return cancelled
     */
    private static boolean isResendCancelled(final long msgId) {
        return resendCancelledMsgIds.contains(msgId);
    }

    /**
     * Marks the message as cancelled so that it does not get resent any more.
     *
     * @param msgId message id
     */
    private static void cancelResend(final long msgId) {
        resendCancelledMsgIds.add(msgId);
    }

    /**
     * Get a fresh and unique ID for a new notification.
     *
     * @return return the ID
     */
    private static synchronized int getNotificationID() {
        ++nextNotificationID;
        return nextNotificationID;
    }

    /**
     * Save Message to internal database.
     *
     * @param context {@link android.content.Context}
     * @param specs   {@link de.ub0r.android.websms.connector.common.ConnectorSpec}
     * @param command {@link de.ub0r.android.websms.connector.common.ConnectorCommand}
     * @param msgType sent or draft?
     */
    static void saveMessage(final Context context, final ConnectorSpec specs,
            final ConnectorCommand command, final int msgType) {
        if (command.getType() != ConnectorCommand.TYPE_SEND) {
            return;
        }
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                WebSMS.PREFS_DROP_SENT, false)) {
            Log.i(TAG, "drop sent messages");
            return;
        }

        // save message to android's internal sms database
        final ContentResolver cr = context.getContentResolver();
        assert cr != null;
        final ContentValues values = new ContentValues();
        values.put(TYPE, msgType);

        if (msgType == MESSAGE_TYPE_SENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                if (isRealSMS(specs)) {
                    // drop messages from "SMS" connector. it gets saved internally.
                    return;
                }

                try {
                    // API19+ does not allow writing to content://sms anymore
                    // anyway, give it a try, if SMSdroid is not installed
                    // AppOps might let the app write the message
                    if (Telephony.Sms.getDefaultSmsPackage(context)
                            .equals("de.ub0r.android.smsdroid")) {
                        sendMessageToSMSdroid(context, specs, command);
                        return;
                    }
                } catch (NullPointerException e) {
                    Log.w(TAG, "there is no telephony service!");
                    // fall back saving the message the old fashion way. it might work..
                }
            }

            final String[] uris = command.getMsgUris();
            if (uris != null && uris.length > 0) {
                for (String s : uris) {
                    final Uri u = Uri.parse(s);
                    try {
                        final int updated = cr.update(u, values, null, null);
                        Log.d(TAG, "updated: " + updated);
                        if (updated > 0
                                && specs != null
                                && !isRealSMS(specs)) {
                            sendMessageToCallMeter(context, specs, u);
                        }
                    } catch (SQLiteException e) {
                        Log.e(TAG, "error updating sent message: " + u, e);
                        Toast.makeText(context,
                                R.string.log_error_saving_message,
                                Toast.LENGTH_LONG).show();
                    }
                }
                return; // skip legacy saving
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return; // skip saving drafts on API19+
        }

        final String text = command.getText();

        Log.d(TAG, "save message(s):");
        Log.d(TAG, "type: " + msgType);
        Log.d(TAG, "TEXT: " + text);
        values.put(READ, 1);
        values.put(BODY, text);
        if (command.getSendLater() > 0) {
            values.put(DATE, command.getSendLater());
            Log.d(TAG, "DATE: " + command.getSendLater());
        }
        final String[] recipients = command.getRecipients();
        final ArrayList<String> inserted = new ArrayList<String>(
                recipients.length);
        for (String recipient : recipients) {
            if (recipient == null || recipient.trim().length() == 0) {
                continue; // skip empty recipients

            }
            String address = Utils.getRecipientsNumber(recipient);
            Log.d(TAG, "TO: " + address);
            try {
                final Cursor c = cr.query(URI_SMS, PROJECTION_ID,
                        TYPE + " = " + MESSAGE_TYPE_DRAFT + " AND " + ADDRESS
                                + " = '" + address + "' AND " + BODY
                                + " like '" + text.replace("'", "_") + "'",
                        null, DATE + " DESC"
                );
                if (c != null && c.moveToFirst()) {
                    final Uri u = URI_SENT.buildUpon()
                            .appendPath(c.getString(0)).build();
                    assert u != null;
                    Log.d(TAG, "skip saving draft: " + u);
                    inserted.add(u.toString());
                } else {
                    final ContentValues cv = new ContentValues(values);
                    cv.put(ADDRESS, address);
                    // save sms to content://sms/sent
                    Uri u = cr.insert(URI_SENT, cv);
                    if (u != null) {
                        inserted.add(u.toString());
                        if (msgType == MESSAGE_TYPE_SENT) {
                            // API19+ code may reach this point
                            // SMSdroid is not default app
                            // but message was saved as sent somehow
                            sendMessageToCallMeter(context, specs, u);
                        }
                    }
                }
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "failed saving message", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "failed saving message", e);
                Toast.makeText(context, R.string.log_error_saving_message,
                        Toast.LENGTH_LONG).show();
            }
        }
        if (msgType == MESSAGE_TYPE_DRAFT && inserted.size() > 0) {
            command.setMsgUris(inserted.toArray(new String[inserted.size()]));
        }
    }

    private static boolean isRealSMS(final ConnectorSpec specs) {
        return specs.getPackage().equals("de.ub0r.android.websms.connector.sms");
    }

    private static void sendMessageToSMSdroid(final Context context, final ConnectorSpec specs,
            final ConnectorCommand command) {
        Log.d(TAG, "send broadcast to SMSdroid");
        Intent intent = new Intent(ACTION_SMSDROID_WEBSMS);
        intent.putExtra("address", command.getRecipients());
        intent.putExtra("body", command.getText());
        intent.putExtra("connector_name", specs.getName());
        context.sendBroadcast(intent);
    }

    @SuppressLint("InlinedApi")
    private static void sendMessageToCallMeter(final Context context, final ConnectorSpec specs,
            final Uri u) {
        Log.d(TAG, "send broadcast to CallMeter3G");
        Intent intent = new Intent(ACTION_CM_WEBSMS);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra(EXTRA_WEBSMS_URI, u.toString());
        intent.putExtra(EXTRA_WEBSMS_CONNECTOR, specs.getName().toLowerCase());
        context.sendBroadcast(intent);
    }

    /**
     * Schedules resend of a message.
     *
     * @param context context
     * @param specs   {@link de.ub0r.android.websms.connector.common.ConnectorSpec}
     * @param command {@link de.ub0r.android.websms.connector.common.ConnectorCommand}
     */
    private static void scheduleMessageResend(final Context context, final ConnectorSpec specs,
            final ConnectorCommand command) {

        long msgId = command.getMsgId();

        final Intent resendIntent = new Intent(Connector.ACTION_RESEND);
        command.setToIntent(resendIntent);
        specs.setToIntent(resendIntent);

        AlarmManager alarmMgr = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + RESEND_DELAY_MS, PendingIntent
                        .getBroadcast(context, (int) msgId, resendIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT)
        );
    }
}
