package de.ub0r.android.websms;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SmsMessage;

import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.SMSLengthCalculator;

/**
 * A SMSLengthCalculator that just delegates to SmsMessage.calculateLength().
 *
 * @author Fintan Fairmichael / Felix Bechstein
 */
public class DefaultSMSLengthCalculator implements SMSLengthCalculator {
	/** Serial Version UID. */
	private static final long serialVersionUID = -1021281060248896432L;

    private static final String TAG = "DefaultSMSLengthCalculator";

    @Override
	public void writeToParcel(final Parcel dest, final int flags) {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public int[] calculateLength(final String messageBody,
			final boolean use7bitOnly) {
        try {
            return SmsMessage.calculateLength(messageBody, use7bitOnly);
        } catch (RuntimeException e) {
            Log.e(TAG, "unable to calculate message length");
            int l = messageBody.length();
            return new int[]{l % 160, l, 160 - l/160, 0};
        }
	}

	/** Parcel stuff. */
	public static final Parcelable.Creator<DefaultSMSLengthCalculator> CREATOR = new Parcelable.Creator<DefaultSMSLengthCalculator>() {
		public DefaultSMSLengthCalculator createFromParcel(final Parcel in) {
			return new DefaultSMSLengthCalculator();
		}

		public DefaultSMSLengthCalculator[] newArray(final int size) {
			return new DefaultSMSLengthCalculator[size];
		}
	};
}
