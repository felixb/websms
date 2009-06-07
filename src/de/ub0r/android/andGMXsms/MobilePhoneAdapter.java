package de.ub0r.android.andGMXsms;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.Contacts.Phones;
import android.view.View;
import android.widget.ResourceCursorAdapter;

public class MobilePhoneAdapter extends ResourceCursorAdapter {

	/** INDEX: name. */
	public static final int NAME_INDEX = 1;
	/** INDEX: number. */
	public static final int NUMBER_INDEX = 2;

	/** Global ContentResolver. */
	private ContentResolver mContentResolver;

	/** Cursor's projection. */
	private static final String[] PROJECTION = { //  
	Phones._ID, // 0
			Phones.NAME, // 1
			Phones.NUMBER // 2
	};

	public MobilePhoneAdapter(final Context context) {
		super(context, R.id.to1, null);
		mContentResolver = context.getContentResolver();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TextView text1 = (TextView) view.findViewById(R.id.text1);
		// TextView text2 = (TextView) view.findViewById(R.id.text2);
		// text1.setText(cursor.getString(NAME_INDEX));
		// text2.setText(cursor.getString(NUMBER_INDEX));
	}

	@Override
	public final String convertToString(Cursor cursor) {
		String name = cursor.getString(NAME_INDEX);
		String number = cursor.getString(NUMBER_INDEX);
		if (name == null || name.length() == 0) {
			return number;
		}
		return name + '(' + number + ')';
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		String where = null;

		if (constraint != null) {
			String filter = DatabaseUtils
					.sqlEscapeString(constraint.toString() + '%');

			StringBuilder s = new StringBuilder();
			s.append("(people.name LIKE ");
			s.append(filter);
			s.append(") OR (people.number LIKE ");
			s.append(filter);
			s.append(")");

			where = s.toString();
		}

		return mContentResolver.query(Phones.CONTENT_URI, PROJECTION, where,
				null, null);
	}

}
