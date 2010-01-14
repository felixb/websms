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

package de.ub0r.android.websms.connector;

import android.os.Bundle;

/**
 * ConnectorSpec presents all necessary informations to use a connector.
 * 
 * @author flx
 */
public final class ConnectorSpec {

	/** Connector: ID. */
	public static final String ID = "connector_id";
	/** Connector: Status. */
	public static final String STATUS = "connector_status";
	/** Connector: Status: inactive. */
	public static final short STATUS_INACTIVE = 0;
	/** Connector: Status: enabled. */
	public static final short STATUS_ENABLED = 1;
	/** Connector: Status: ready. */
	public static final short STATUS_READY = 2;
	/** Connector: Status: bootstrapping. */
	public static final short STATUS_BOOTSTRAPPING = 4;
	/** Connector: Status: updating. */
	public static final short STATUS_UPDATING = 8;
	/** Connector: Status: sending. */
	public static final short STATUS_SENDING = 16;
	/** Connector: Author. */
	public static final String AUTHOR = "connector_author";
	/** Connector: Preferences' intent URI. */
	public static final String PREFSINTENT = "connector_prefsintent";
	/** Connector: Preferences' title. */
	public static final String PREFSTITLE = "connector_prefstitle";
	/** Connector: Capabilities. */
	public static final String CAPABILITIES = "connector_capabilities";
	/** Feature: none. */
	public static final short CAPABILITIES_NONE = 0;
	/** Feature: bootstrap. */
	public static final short CAPABILITIES_BOOSTRAP = 1;
	/** Feature: update. */
	public static final short CAPABILITIES_UPDATE = 2;
	/** Feature: send. */
	public static final short CAPABILITIES_SEND = 4;

	// Subconnectors
	/** Connector: SubConnector prefix. */
	private static final String SUB_PREFIX = "sub_";
	/** Connector: number of subconnectors. */
	private static final String SUB_COUNT = SUB_PREFIX + "n";
	/** Connector: name. */
	public static final String NAME = "subconnector_name";
	/** Connector: features. */
	public static final String FEATURES = "subconnector_features";
	/** Feature: none. */
	public static final short FEATURE_NONE = 0;
	/** Feature: multiple recipients. */
	public static final short FEATURE_MULTIRECIPIENTS = 1;
	/** Feature: flash sms. */
	public static final short FEATURE_FLASHSMS = 2;
	/** Feature: send later. */
	public static final short FEATURE_SENDLATER = 4;
	/** Feature: custom sender. */
	public static final short FEATURE_CUSTOMSENDER = 8;

	/**
	 * SubConnectorSpec presents all necessary informations to use a
	 * subconnector.
	 * 
	 * @author flx
	 */
	public final class SubConnectorSpec {
		/** {@link Bundle} represents the SubConnectorSpec. */
		private final Bundle bundle;

		/**
		 * Create SubConnectorSpec from bundle.
		 * 
		 * @param b
		 *            bundle
		 */
		SubConnectorSpec(final Bundle b) {
			this.bundle = b;
		}

		/**
		 * Create SubConnectorSpec.
		 * 
		 * @param id
		 *            id
		 * @param name
		 *            name
		 * @param features
		 *            features
		 */
		SubConnectorSpec(final String id, final String name,
				final short features) {
			this.bundle = new Bundle();
			this.bundle.putString(ID, id);
			this.bundle.putString(NAME, name);
			this.bundle.putShort(FEATURES, features);
		}

		/**
		 * @return internal bundle
		 */
		Bundle getBundle() {
			return this.bundle;
		}

		/**
		 * @return ID
		 */
		public String getID() {
			return this.bundle.getString(ID);
		}

		/**
		 * @return name
		 */
		public String getName() {
			return this.bundle.getString(NAME);
		}

		/**
		 * @return features
		 */
		public short getFeatures() {
			return this.bundle.getShort(FEATURES, FEATURE_NONE);
		}

		/**
		 * @param features
		 *            features
		 * @return true if connector has given features
		 */
		public boolean hasFeatures(final short features) {
			final short f = this.bundle.getShort(FEATURES, FEATURE_NONE);
			return (f & features) == features;
		}
	}

	/** {@link Bundle} represents the ConnectorSpec. */
	private final Bundle bundle;

	/**
	 * Create ConnectorSpec from bundle.
	 * 
	 * @param b
	 *            bundle
	 */
	public ConnectorSpec(final Bundle b) {
		this.bundle = b;
	}

	/**
	 * @return internal bundle
	 */
	public Bundle getBundle() {
		return this.bundle;
	}

	/**
	 * @return ID
	 */
	public String getID() {
		return this.bundle.getString(ID);
	}

	/**
	 * @return status
	 */
	public short getStatus() {
		return this.bundle.getShort(STATUS, STATUS_INACTIVE);
	}

	/**
	 * @param status
	 *            status
	 * @return true if connector has given status
	 */
	public boolean hasStatus(final short status) {
		final short s = this.bundle.getShort(STATUS, STATUS_INACTIVE);
		return (s & status) == status;
	}

	/**
	 * @return author
	 */
	public String getAuthor() {
		return this.bundle.getString(AUTHOR);
	}

	/**
	 * @return prefs intent uri
	 */
	public String getPrefsIntent() {
		return this.bundle.getString(PREFSINTENT);
	}

	/**
	 * @return prefs title
	 */
	public String getPrefsTitle() {
		return this.bundle.getString(PREFSTITLE);
	}

	/**
	 * @return capabilities
	 */
	public short getCapabilities() {
		return this.bundle.getShort(CAPABILITIES, CAPABILITIES_NONE);
	}

	/**
	 * @param capabilities
	 *            capabilities
	 * @return true if connector has given capabilities
	 */
	public boolean hasCapabilities(final short capabilities) {
		final short c = this.bundle.getShort(CAPABILITIES, CAPABILITIES_NONE);
		return (c & capabilities) == capabilities;
	}

	/**
	 * @return all SubConnectors
	 */
	public SubConnectorSpec[] getSubConnectors() {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		final SubConnectorSpec[] ret = new SubConnectorSpec[c];
		for (int i = 0; i < c; i++) {
			ret[i] = new SubConnectorSpec(this.bundle.getBundle(SUB_PREFIX + i));
		}
		return ret;
	}

	/**
	 * Get SubConnector by ID.
	 * 
	 * @param id
	 *            ID
	 * @return SubConnector
	 */
	public SubConnectorSpec getSubConnector(final String id) {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		for (int i = 0; i < c; i++) {
			final SubConnectorSpec sc = new SubConnectorSpec(this.bundle
					.getBundle(SUB_PREFIX + i));
			if (id.equals(sc.getID())) {
				return sc;
			}
		}
		return null;
	}

	/**
	 * Add a SubConnector from bundle.
	 * 
	 * @param b
	 *            bundle
	 */
	private void addSubConnector(final Bundle b) {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		this.bundle.putBundle(SUB_PREFIX + c, b);
		this.bundle.putInt(SUB_COUNT, c + 1);
	}

	/**
	 * Add a SubConnector.
	 * 
	 * @param id
	 *            id
	 * @param name
	 *            name
	 * @param features
	 *            features
	 */
	void addSubConnector(final String id, final String name,
			final short features) {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		this.bundle.putBundle(SUB_PREFIX + c, new SubConnectorSpec(id, name,
				features).getBundle());
		this.bundle.putInt(SUB_COUNT, c + 1);
	}
}
