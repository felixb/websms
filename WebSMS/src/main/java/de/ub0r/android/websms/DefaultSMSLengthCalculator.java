package de.ub0r.android.websms;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SmsMessage;
import de.ub0r.android.websms.connector.common.SMSLengthCalculator;

/**
 * A SMSLengthCalculator that just delegates to SmsMessage.calculateLength().
 * 
 * @author Fintan Fairmichael / Felix Bechstein
 */
public class DefaultSMSLengthCalculator implements SMSLengthCalculator {
	/** Serial Version UID. */
	private static final long serialVersionUID = -1021281060248896432L;

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
		return SmsMessage.calculateLength(messageBody, use7bitOnly);
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
