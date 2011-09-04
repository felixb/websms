package de.ub0r.android.websms;

import android.os.Parcel;
import android.os.Parcelable;
import de.ub0r.android.lib.apis.TelephonyWrapper;
import de.ub0r.android.websms.connector.common.SMSLengthCalculator;

/**
 * 
 * A SMSLengthCalculator that just delegates to
 * TelephonyWrapper.getInstance().calculateLength
 * 
 * @author Fintan Fairmichael
 * 
 */
public class DefaultSMSLengthCalculator implements SMSLengthCalculator {
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
		return TelephonyWrapper.getInstance().calculateLength(messageBody,
				use7bitOnly);
	}

	public static final Parcelable.Creator<DefaultSMSLengthCalculator> CREATOR = new Parcelable.Creator<DefaultSMSLengthCalculator>() {
		public DefaultSMSLengthCalculator createFromParcel(final Parcel in) {
			return new DefaultSMSLengthCalculator();
		}

		public DefaultSMSLengthCalculator[] newArray(final int size) {
			return new DefaultSMSLengthCalculator[size];
		}
	};
}
