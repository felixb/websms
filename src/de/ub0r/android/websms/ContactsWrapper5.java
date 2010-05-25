/*
 * Copyright (C) 2010 Felix Bechstein
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
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import de.ub0r.android.websms.connector.common.Log;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw5";

	/** SQL to select mobile numbers only. */
	private static final String MOBILES_ONLY = ") AND ("
			+ ContactsContract.CommonDataKinds.Phone.TYPE + " = "
			+ ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE + ")";

	/** Sort Order. */
	private static final String SORT_ORDER = // .
	ContactsContract.CommonDataKinds.Phone.STARRED + " DESC, "
			+ ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED
			+ " DESC, " + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
			+ " ASC, " + ContactsContract.CommonDataKinds.Phone.TYPE;

	/** Cursor's projection. */
	private static final String[] PROJECTION = { //  
	BaseColumns._ID, // 0
			ContactsContract.Data.DISPLAY_NAME, // 1
			ContactsContract.CommonDataKinds.Phone.NUMBER, // 2
			ContactsContract.CommonDataKinds.Phone.TYPE // 3
	};

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
			ContactsContract.PhoneLookup.DISPLAY_NAME,
			ContactsContract.CommonDataKinds.Phone.NUMBER };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContactUri() {
		return Contacts.CONTENT_URI;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getFilterProjection() {
		return PROJECTION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getMobilesOnlyString() {
		return MOBILES_ONLY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getFilterSort() {
		return SORT_ORDER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getFilterUri() {
		return ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	}

	@Override
	public String getFilterWhere(final String filter) {
		String f = DatabaseUtils.sqlEscapeString('%' + filter.toString() + '%');
		StringBuilder s = new StringBuilder();
		s.append("(" + ContactsContract.Data.DISPLAY_NAME + " LIKE ");
		s.append(f);
		s.append(") OR (" + ContactsContract.CommonDataKinds.Phone.DATA1
				+ " LIKE ");
		s.append(f);
		s.append(")");
		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final Uri uri) {
		// FIXME: this is broken in android os; issue #8255
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c != null && c.moveToFirst()) {
			return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final String number) {
		if (number == null || number.length() == 0) {
			return null;
		}
		Uri uri = Uri.withAppendedPath(
				ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
				number);
		// FIXME: this is broken in android os; issue #8255
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c != null && c.moveToFirst()) {
			return c;
		}
		// Fallback to API3
		c = new ContactsWrapper3().getContact(cr, number);
		if (c != null && c.moveToFirst()) {
			// get orig API5 cursor for the real number
			final String where = PROJECTION_FILTER[FILTER_INDEX_NUMBER]
					+ " = '" + c.getString(FILTER_INDEX_NUMBER) + "'";
			Log.d(TAG, "query: " + Phone.CONTENT_URI + " # " + where);
			Cursor c0 = cr.query(Phone.CONTENT_URI, PROJECTION_FILTER, where,
					null, null);
			if (c0 != null && c0.moveToFirst()) {
				Log.d(TAG, "id: " + c0.getString(FILTER_INDEX_ID));
				Log.d(TAG, "name: " + c0.getString(FILTER_INDEX_NAME));
				Log.d(TAG, "number: " + c0.getString(FILTER_INDEX_NUMBER));
				return c0;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getPickPhoneIntent() {
		final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		return i;
	}
}
