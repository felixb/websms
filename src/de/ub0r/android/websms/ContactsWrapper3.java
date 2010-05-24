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
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.People.Extensions;
import de.ub0r.android.websms.connector.common.Log;

/**
 * Implement {@link ContactsWrapper} for API 3 and 4.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class ContactsWrapper3 extends ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "cw3";

	/** SQL to select mobile numbers only. */
	private static final String MOBILES_ONLY = ") AND (" + PhonesColumns.TYPE
			+ " = " + PhonesColumns.TYPE_MOBILE + ")";

	/** Sort Order. */
	private static final String SORT_ORDER = PeopleColumns.STARRED + " DESC, "
			+ PeopleColumns.TIMES_CONTACTED + " DESC, " + PeopleColumns.NAME
			+ " ASC, " + PhonesColumns.TYPE;

	/** {@link Uri} for persons, content filter. */
	private static final Uri URI_CONTENT_FILTER = // .
	Contacts.Phones.CONTENT_URI;

	/** Cursor's projection. */
	private static final String[] PROJECTION = { //  
	BaseColumns._ID, // 0
			PeopleColumns.NAME, // 1
			PhonesColumns.NUMBER, // 2
			PhonesColumns.TYPE // 3
	};

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { Extensions.PERSON_ID, PeopleColumns.DISPLAY_NAME,
			PhonesColumns.NUMBER };

	/** Projection for persons query, plain. */

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
	public Uri getFilterUri() {
		return URI_CONTENT_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getContactUri() {
		return Phones.CONTENT_URI;
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
	public String getFilterWhere(final String filter) {
		String f = DatabaseUtils.sqlEscapeString('%' + filter.toString() + '%');
		StringBuilder s = new StringBuilder();
		s.append("(" + PeopleColumns.NAME + " LIKE ");
		s.append(f);
		s.append(") OR (" + PhonesColumns.NUMBER + " LIKE ");
		s.append(f);
		s.append(")");
		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final String number) {
		final Uri uri = Uri.withAppendedPath(
				Contacts.Phones.CONTENT_FILTER_URL, number);
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c.moveToFirst()) {
			Log.d(TAG, "id: " + c.getString(FILTER_INDEX_ID));
			Log.d(TAG, "name: " + c.getString(FILTER_INDEX_NAME));
			Log.d(TAG, "number: " + c.getString(FILTER_INDEX_NUMBER));
			return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor getContact(final ContentResolver cr, // .
			final Uri uri) {
		Log.d(TAG, "query: " + uri);
		Cursor c = cr.query(uri, PROJECTION_FILTER, null, null, null);
		if (c.moveToFirst()) {
			Log.d(TAG, "id: " + c.getString(FILTER_INDEX_ID));
			Log.d(TAG, "name: " + c.getString(FILTER_INDEX_NAME));
			Log.d(TAG, "number: " + c.getString(FILTER_INDEX_NUMBER));
			return c;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getPickPhoneIntent() {
		final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType(Phones.CONTENT_ITEM_TYPE);
		return i;
	}
}
