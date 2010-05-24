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

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * Wrap around contacts API.
 * 
 * @author flx
 */
abstract class ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw";

	/** Index of id. */
	public static final int FILTER_INDEX_ID = 0;
	/** Index of name. */
	public static final int FILTER_INDEX_NAME = 1;
	/** Index of number. */
	public static final int FILTER_INDEX_NUMBER = 2;

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
	 * Get a {@link Cursor} with <id,name,number> for a given number.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param number
	 *            number to look for
	 * @return a {@link Cursor} matching the number
	 */
	public abstract Cursor getContact(final ContentResolver cr,
			final String number);

	/**
	 * Get a {@link Cursor} with <id,name,number> for a given number.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param uri
	 *            {@link Uri} to get the contact from
	 * @return a {@link Cursor} matching the number
	 */
	public abstract Cursor getContact(final ContentResolver cr, final Uri uri);

	/**
	 * Get a Name for a given number.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param number
	 *            number to look for
	 * @return name matching the number
	 */
	public final String getNameForNumber(final ContentResolver cr,
			final String number) {
		final Cursor c = this.getContact(cr, number);
		if (c != null) {
			return c.getString(FILTER_INDEX_NAME);
		}
		return null;
	}

	/**
	 * Get "Name <Number>" from {@link Uri}.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param uri
	 *            {@link Uri}
	 * @return "Name <Number>"
	 */
	public final String getNameAndNumber(final ContentResolver cr, // .
			final Uri uri) {
		final Cursor c = this.getContact(cr, uri);
		if (c != null) {
			return c.getString(FILTER_INDEX_NAME) + " <"
					+ Utils.cleanRecipient(c.getString(FILTER_INDEX_NAME))
					+ ">";
		}
		return null;
	}

	/**
	 * Pck a Contact's phone.
	 * 
	 * @return {@link Intent}
	 */
	public abstract Intent getPickPhoneIntent();
}
