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

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Display About {@link Activity}.
 * 
 * @author flx
 */
public class About extends Activity {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about);
		this.setTitle(this.getString(R.string.about_) + " v"
				+ this.getString(R.string.app_version));
		StringBuffer authors = new StringBuffer();
		final ConnectorSpec[] css = WebSMS.getConnectors(
				ConnectorSpec.CAPABILITIES_NONE, ConnectorSpec.STATUS_INACTIVE);
		String a;
		for (ConnectorSpec cs : css) {
			a = cs.getAuthor();
			if (a != null && a.length() > 0) {
				authors.append(cs.getName());
				authors.append(":\t");
				authors.append(a);
				authors.append("\n");
			}
		}
		a = null;
		((TextView) this.findViewById(R.id.author_connectors)).setText(authors
				.toString().trim());
	}
}
