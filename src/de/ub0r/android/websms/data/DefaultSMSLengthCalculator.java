/*
 * Copyright (C) 2010-2012 Fintan Fairmichael, Felix Bechstein
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
package de.ub0r.android.websms.data;

import android.os.Parcel;
import android.os.Parcelable;
import de.ub0r.android.lib.apis.TelephonyWrapper;
import de.ub0r.android.websms.connector.common.SMSLengthCalculator;

/**
 * A SMSLengthCalculator that just delegates to
 * TelephonyWrapper.getInstance().calculateLength().
 * 
 * @author Fintan Fairmichael
 */
public final class DefaultSMSLengthCalculator implements SMSLengthCalculator {
	/** Generated serial UID. */
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

	/**
	 * Parcel creator.
	 */
	public static final Parcelable.Creator<DefaultSMSLengthCalculator> // .
	CREATOR = new Parcelable.Creator<DefaultSMSLengthCalculator>() {
		public DefaultSMSLengthCalculator createFromParcel(final Parcel in) {
			return new DefaultSMSLengthCalculator();
		}

		public DefaultSMSLengthCalculator[] newArray(final int size) {
			return new DefaultSMSLengthCalculator[size];
		}
	};
}
