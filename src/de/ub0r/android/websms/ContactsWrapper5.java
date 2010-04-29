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

import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
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
	public String getNameForNumber(final WebSMS act, final String number) {
		String ret = null;
		Cursor c = act.managedQuery(Uri.withAppendedPath(
				ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
				number), new String[] { ContactsContract.Data.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER }, null, null,
				null);
		if (c.moveToFirst()) {
			ret = c.getString(0);
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getNameAndNumber(final WebSMS act, final Uri uri) {
		String ret = null;
		Cursor c = act.managedQuery(uri, new String[] {
				ContactsContract.Data.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER }, null, null,
				null);
		if (c.moveToFirst()) {
			ret = c.getString(0) + " <" + Utils.cleanRecipient(c.getString(1))
					+ ">";
		}
		return ret;
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
