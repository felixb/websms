/*
 * Copyright (C) 2010 Felix Bechstein
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
package de.ub0r.android.websms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * CursorAdapter getting Name, Phone from DB.
 * 
 * @author flx
 */
public class MobilePhoneAdapter extends ResourceCursorAdapter {
	/** Preferences: show mobile numbers only. */
	private static boolean prefsMobilesOnly;

	/** Global ContentResolver. */
	private ContentResolver mContentResolver;

	/** {@link ContactsWrapper} to use. */
	private static final ContactsWrapper WRAPPER = ContactsWrapper
			.getInstance();

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            context
	 */
	public MobilePhoneAdapter(final Context context) {
		super(context, R.layout.recipient_dropdown_item, null);
		this.mContentResolver = context.getContentResolver();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		((TextView) view.findViewById(R.id.text1)).setText(cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NAME));
		((TextView) view.findViewById(R.id.text2)).setText(cursor
				.getString(ContactsWrapper.CONTENT_INDEX_NUMBER));
		final int i = cursor.getInt(ContactsWrapper.CONTENT_INDEX_TYPE) - 1;
		String[] types = context.getResources().getStringArray(
				android.R.array.phoneTypes);
		if (i >= 0 && i < types.length) {
			((TextView) view.findViewById(R.id.text3)).setText(types[i]);
		} else {
			((TextView) view.findViewById(R.id.text3)).setText("");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final String convertToString(final Cursor cursor) {
		String name = cursor.getString(ContactsWrapper.CONTENT_INDEX_NAME);
		String number = cursor.getString(ContactsWrapper.CONTENT_INDEX_NUMBER);
		if (name == null || name.length() == 0) {
			return Utils.cleanRecipient(number);
		}
		return name + " <" + Utils.cleanRecipient(number) + '>';
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final Cursor runQueryOnBackgroundThread(// .
			final CharSequence constraint) {
		String where = null;
		if (constraint != null) {
			where = WRAPPER.getContentWhere(constraint.toString());
			if (prefsMobilesOnly) {
				where = DbUtils.sqlAnd(where, WRAPPER.getMobilesOnlyString());
			}
		}

		return this.mContentResolver.query(WRAPPER.getContentUri(), WRAPPER
				.getContentProjection(), where, null, WRAPPER.getContentSort());
	}

	/**
	 * @param b
	 *            set to true, if only mobile numbers should be displayed.
	 */
	static final void setMoileNubersObly(final boolean b) {
		prefsMobilesOnly = b;
	}
}
