/*
 * Copyright (C) 2010-2012 Felix Bechstein
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
package de.ub0r.android.websms.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import de.ub0r.android.lib.Base64Coder;
import de.ub0r.android.lib.Log;
import de.ub0r.android.websms.WebSMS;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.ConnectorSpec.SubConnectorSpec;

/**
 * Container for all available {@link ConnectorSpec}s.
 * 
 * @author Felix Bechstein <f@ub0r.de>
 */
public final class ConnectorContainer {
	/** Tag for output. */
	private static final String TAG = "container";
	/** Single instance. */
	private static ConnectorContainer instance = null;

	/** {@link ArrayList} of {@link ConnectorSpec}s. */
	private final ArrayList<ConnectorSpec> connectors;
	/** Map {@link ConnectorSpec}s by id. */
	private final HashMap<String, ConnectorSpec> ids;
	/** Selected {@link ConnectorSpec}. */
	private ConnectorSpec selected = null;

	/**
	 * @return {@link ConnectorContainer}
	 */
	public ConnectorContainer getInstance() {
		if (instance == null) {
			instance = new ConnectorContainer();
		}
		return instance;
	}

	/**
	 * Private constructor.
	 */
	private ConnectorContainer() {
		this.connectors = new ArrayList<ConnectorSpec>();
		this.ids = new HashMap<String, ConnectorSpec>();
	}

	/**
	 * Get {@link ConnectorSpec} by ID.
	 * 
	 * @param id
	 *            ID
	 * @return {@link ConnectorSpec}
	 */
	public ConnectorSpec getConnectorByID(final String id) {
		synchronized (this.connectors) {
			if (id == null) {
				return null;
			}
			return this.ids.get(id);
		}
	}

	/**
	 * Get {@link ConnectorSpec} by name.
	 * 
	 * @param name
	 *            name
	 * @return {@link ConnectorSpec}
	 */
	public ConnectorSpec getConnectorByName(final String name) {
		synchronized (this.connectors) {
			if (name == null) {
				return null;
			}
			final int l = this.connectors.size();
			ConnectorSpec c;
			String n;
			for (int i = 0; i < l; i++) {
				c = this.connectors.get(i);
				n = c.getName();
				if (name.startsWith(n)) {
					if (name.length() == n.length()) {
						return c;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get {@link ConnectorSpec}s by capabilities and/or status.
	 * 
	 * @param capabilities
	 *            capabilities needed
	 * @param status
	 *            status required {@link SubConnectorSpec}
	 * @return {@link ConnectorSpec}s
	 */
	public ConnectorSpec[] getConnectors(final int capabilities,
			final int status) {
		synchronized (this.connectors) {
			final int l = this.connectors.size();
			final ArrayList<ConnectorSpec> ret = new ArrayList<ConnectorSpec>(l);
			ConnectorSpec c;
			for (int i = 0; i < l; i++) {
				c = this.connectors.get(i);
				if (c.hasCapabilities((short) capabilities)
						&& c.hasStatus((short) status)) {
					ret.add(c);
				}
			}
			return ret.toArray(new ConnectorSpec[0]);
		}
	}

	/**
	 * @return selected {@link ConnectorSpec}
	 */
	public ConnectorSpec getSelected() {
		return this.selected;
	}

	/**
	 * Set selected {@link ConnectorSpec}.
	 * 
	 * @param id
	 *            id
	 */
	public void setSelected(final String id) {
		this.selected = this.ids.get(id);
	}

	/**
	 * Write cache to {@link SharedPreferences}.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public void writeToPrefs(final Context context) {
		synchronized (this.connectors) {
			Editor editor = PreferenceManager.getDefaultSharedPreferences(
					context).edit();
			try {
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream objOut = new ObjectOutputStream(
						new BufferedOutputStream(out, WebSMS.BUFSIZE));
				objOut.writeObject(this.connectors);
				objOut.close();
				final String s = String.valueOf(Base64Coder.encode(out
						.toByteArray()));
				Log.d(TAG, s);
				editor.putString(WebSMS.PREFS_CONNECTORS, s);
			} catch (Exception e) {
				editor.remove(WebSMS.PREFS_CONNECTORS);
				Log.e(TAG, "IO", e);
			}
			editor.commit();
		}
	}

	/**
	 * Read {@link ConnectorSpec}s from cache.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	@SuppressWarnings("unchecked")
	public void readFromCache(final Context context) {
		this.connectors.clear();
		this.ids.clear();
		synchronized (this.connectors) {
			SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			String s = p.getString(WebSMS.PREFS_CONNECTORS, null);
			if (!TextUtils.isEmpty(s)) {
				ArrayList<ConnectorSpec> cache;
				try {
					cache = (ArrayList<ConnectorSpec>) (new ObjectInputStream(
							new BufferedInputStream(new ByteArrayInputStream(
									Base64Coder.decode(s)), WebSMS.BUFSIZE)))
							.readObject();
					int l = cache.size();
					for (int i = 0; i < l; i++) {
						ConnectorSpec cs = cache.get(i);
						this.connectors.add(cs);
						this.ids.put(cs.getPackage(), cs);
					}
				} catch (Exception e) {
					Log.e(TAG, "error reading cache", e);
				}
			}
		}
	}
}
