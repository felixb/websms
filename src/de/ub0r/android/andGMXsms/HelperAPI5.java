/*
 * Copyright (C) 2009 Felix Bechstein
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
package de.ub0r.android.andGMXsms;

import java.lang.reflect.Method;

import android.app.Notification;
import android.app.Service;
import android.content.ContentResolver;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public class HelperAPI5 {
	/** Tag for output. */
	private static final String TAG = "WebSMS.api5";

	/** Error message if API5 is not available. */
	private static final String ERRORMESG = "no API5 available";

	/** Sort Order. */
	private static final String SORT_ORDER = ContactsContract.CommonDataKinds.Phone.STARRED
			+ " DESC, "
			+ ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED
			+ " DESC, "
			+ ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
			+ " ASC, " + ContactsContract.CommonDataKinds.Phone.TYPE;

	/** Cursor's projection. */
	private static final String[] PROJECTION = { //  
	BaseColumns._ID, // 0
			ContactsContract.Data.DISPLAY_NAME, // 1
			ContactsContract.CommonDataKinds.Phone.NUMBER, // 2
			ContactsContract.CommonDataKinds.Phone.TYPE // 3
	};

	/**
	 * Check whether API5 is available.
	 * 
	 * @return true if API5 is available
	 */
	final boolean isAvailable() {
		try {
			Method mDebugMethod = Service.class.getMethod("startForeground",
					new Class[] { Integer.TYPE, Notification.class });
			/* success, this is a newer device */
			if (mDebugMethod != null) {
				return true;
			}
		} catch (Throwable e) {
			Log.d(TAG, ERRORMESG, e);
			throw new VerifyError(ERRORMESG);
		}
		Log.d(TAG, ERRORMESG);
		throw new VerifyError(ERRORMESG);
	}

	/**
	 * Run Query to update data.
	 * 
	 * @param mContentResolver
	 *            a Content Resolver
	 * @param constraint
	 *            filter string
	 * @return cursor
	 */
	final Cursor runQueryOnBackgroundThread(
			final ContentResolver mContentResolver,
			final CharSequence constraint) {
		String where = null;

		if (constraint != null) {
			String filter = DatabaseUtils.sqlEscapeString('%' + constraint
					.toString() + '%');

			StringBuilder s = new StringBuilder();
			s.append("(" + ContactsContract.Data.DISPLAY_NAME + " LIKE ");
			s.append(filter);
			s.append(") OR (" + ContactsContract.CommonDataKinds.Phone.DATA1
					+ " LIKE ");
			s.append(filter);
			s.append(")");

			where = s.toString();
		}
		return mContentResolver.query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION,
				where, null, SORT_ORDER);
	}

	/**
	 * Run Service in foreground.
	 * 
	 * @see Service.startForeground()
	 * @param service
	 *            the Service
	 * @param id
	 *            notification id
	 * @param notification
	 *            notification
	 */
	final void startForeground(final Service service, final int id,
			final Notification notification) {
		service.startForeground(id, notification);
	}

	/**
	 * Run Service in background.
	 * 
	 * @see Service.stopForeground()
	 * @param service
	 *            Service
	 * @param removeNotification
	 *            remove notification?
	 */
	final void stopForeground(final Service service,
			final boolean removeNotification) {
		service.stopForeground(removeNotification);
	}

	/**
	 * Get a Name for a given number.
	 * 
	 * @param act
	 *            activity to get the cursor from
	 * @param number
	 *            number to look for
	 * @return name matching the number
	 */
	final String getNameForNumber(final WebSMS act, final String number) {
		String ret = null;

		Cursor c = act.managedQuery(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[] { ContactsContract.Data.DISPLAY_NAME,
						ContactsContract.CommonDataKinds.Phone.NUMBER },
				ContactsContract.CommonDataKinds.Phone.DATA1 + " = '" + number
						+ "'", null, null);
		if (c.moveToFirst()) {
			ret = c.getString(c
					.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
		}

		return ret;
	}
}
