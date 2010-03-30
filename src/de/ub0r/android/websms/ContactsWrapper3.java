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

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;

/**
 * Implement {@link ContactsWrapper} for API 3 and 4.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class ContactsWrapper3 extends ContactsWrapper {

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
	public String getNameForNumber(final WebSMS act, final String number) {
		String ret = null;
		Cursor c = act.managedQuery(
				android.provider.Contacts.People.CONTENT_URI, new String[] {
						PeopleColumns.DISPLAY_NAME, PhonesColumns.NUMBER },
				PhonesColumns.NUMBER + " = '" + number + "'", null, null);
		if (c.moveToFirst()) {
			ret = c.getString(0);
		}
		return ret;
	}
}
