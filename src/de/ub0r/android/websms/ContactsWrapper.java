/*
 * Copyright (C) 2010 Felix Bechstein, The Android Open Source Project
 * 
 * This file is part of SMSdroid.
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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import de.ub0r.android.websms.connector.common.Log;

/**
 * Wrap around contacts API.
 * 
 * @author flx
 */
abstract class ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw";

	/**
	 * Static singleton instance of {@link ContactsWrapper} holding the
	 * SDK-specific implementation of the class.
	 */
	private static ContactsWrapper sInstance;

	/** INDEX: name. */
	public static final int INDEX_NAME = 1;
	/** INDEX: number. */
	public static final int INDEX_NUMBER = 2;
	/** INDEX: type. */
	public static final int INDEX_NUMBER_TYPE = 3;

	/**
	 * Get instance.
	 * 
	 * @return {@link ContactsWrapper}
	 */
	public static final ContactsWrapper getInstance() {
		if (sInstance == null) {

			String className;

			/**
			 * Check the version of the SDK we are running on. Choose an
			 * implementation class designed for that version of the SDK.
			 * Unfortunately we have to use strings to represent the class
			 * names. If we used the conventional
			 * ContactAccessorSdk5.class.getName() syntax, we would get a
			 * ClassNotFoundException at runtime on pre-Eclair SDKs. Using the
			 * above syntax would force Dalvik to load the class and try to
			 * resolve references to all other classes it uses. Since the
			 * pre-Eclair does not have those classes, the loading of
			 * ContactAccessorSdk5 would fail.
			 */
			int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
			// Cupcake style
			if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
				className = "de.ub0r.android.websms.ContactsWrapper3";
			} else {
				className = "de.ub0r.android.websms.ContactsWrapper5";
			}

			// Find the required class by name and instantiate it.
			try {
				Class<? extends ContactsWrapper> clazz = Class.forName(
						className).asSubclass(ContactsWrapper.class);
				sInstance = clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			Log.d(TAG, "getInstance(): " + sInstance.getClass().getName());
		}
		return sInstance;
	}

	/**
	 * Get {@link Uri} for filter contacts by address.
	 * 
	 * @return {@link Uri}
	 */
	public abstract Uri getFilterUri();

	/**
	 * Get projection for filter contacts.
	 * 
	 * @return projection
	 */
	public abstract String[] getFilterProjection();

	/**
	 * Get sort order for filter contacts.
	 * 
	 * @return sort
	 */
	public abstract String getFilterSort();

	/**
	 * Get WHERE for filter.
	 * 
	 * @param filter
	 *            filter
	 * @return WHERE
	 */
	public abstract String getFilterWhere(final String filter);

	/**
	 * Get {@link Uri} to a Contact.
	 * 
	 * @return {@link Uri}
	 */
	public abstract Uri getContactUri();

	/**
	 * Get {@link String} selecting mobiles only.
	 * 
	 * @return mobiles only {@link String}
	 */
	public abstract String getMobilesOnlyString();

	/**
	 * Get a Name for a given number.
	 * 
	 * @param act
	 *            Activity to get the cursor from
	 * @param number
	 *            number to look for
	 * @return name matching the number
	 */
	public abstract String getNameForNumber(final WebSMS act,
			final String number);

	/**
	 * Get "Name <Number>" from {@link Uri}. * @param act {@link Activity} to
	 * get the cursor from.
	 * 
	 * @param uri
	 *            {@link Uri}
	 * @return "Name <Number>"
	 */
	// TODO: merge both APIs here?
	public abstract String getNameAndNumber(final WebSMS act, final Uri uri);

	/**
	 * Pck a Contact's phone.
	 * 
	 * @return {@link Intent}
	 */
	public abstract Intent getPickPhoneIntent();
}
