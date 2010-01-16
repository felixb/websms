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

import android.content.Intent;
import android.os.Bundle;

/**
 * ConnectorSpec presents all necessary informations to use a connector.
 * 
 * @author flx
 */
public final class ConnectorSpec {

	/** Key to find a connector in a Bundle. */
	private static final String EXTRAS_CONNECTOR = "connector";

	/** Connector: ID. */
	private static final String ID = "connector_id";
	/** Connector: ID. */
	private static final String NAME = "connector_name";
	/** Connector: Status. */
	private static final String STATUS = "connector_status";
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
	/** Connector: Status: error. */
	public static final short STATUS_ERROR = 32;
	/** Connector: Author. */
	private static final String AUTHOR = "connector_author";
	/** Connector: Preferences' intent URI. */
	private static final String PREFSINTENT = "connector_prefsintent";
	/** Connector: Preferences' title. */
	private static final String PREFSTITLE = "connector_prefstitle";
	/** Connector: Capabilities. */
	private static final String CAPABILITIES = "connector_capabilities";
	/** Feature: none. */
	public static final short CAPABILITIES_NONE = 0;
	/** Feature: bootstrap. */
	public static final short CAPABILITIES_BOOSTRAP = 1;
	/** Feature: update. */
	public static final short CAPABILITIES_UPDATE = 2;
	/** Feature: send. */
	public static final short CAPABILITIES_SEND = 4;
	/** Connector: Balance. */
	private static final String BALANCE = "connector_balance";
	/** Connector: Error message */
	private static final String ERRORMESSAGE = "connector_errormessage";

	// Subconnectors
	/** Connector: SubConnector prefix. */
	private static final String SUB_PREFIX = "sub_";
	/** Connector: number of subconnectors. */
	private static final String SUB_COUNT = SUB_PREFIX + "n";

	/**
	 * SubConnectorSpec presents all necessary informations to use a
	 * subconnector.
	 * 
	 * @author flx
	 */
	public final class SubConnectorSpec {
		/** Connector: ID. */
		private static final String ID = "subconnector_id";
		/** Connector: name. */
		private static final String NAME = "subconnector_name";
		/** Connector: features. */
		private static final String FEATURES = "subconnector_features";
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
	 * Create ConnectorSpec from intent.
	 * 
	 * @param i
	 *            intent
	 */
	public ConnectorSpec(final Intent i) {
		Bundle e = i.getExtras();
		if (e != null) {
			this.bundle = e.getBundle(EXTRAS_CONNECTOR);
		} else {
			this.bundle = new Bundle();
		}
	}

	/**
	 * Create ConnectorSpec.
	 * 
	 * @param id
	 *            ID
	 * @param name
	 *            name
	 */
	public ConnectorSpec(final String id, final String name) {
		this.bundle = new Bundle();
		this.bundle.putString(ID, id);
		this.bundle.putString(NAME, name);
	}

	/**
	 * Update ConnectorSpecs.
	 * 
	 * @param connector
	 *            {@link ConnectorSpec}
	 */
	public void update(final ConnectorSpec connector) {
		this.bundle.putAll(connector.getBundle());
	}

	/**
	 * Set this {@link ConnectorSpec} to an {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}.
	 * @return the same {@link Intent}
	 */
	public Intent setToIntent(final Intent intent) {
		intent.putExtra(EXTRAS_CONNECTOR, this.getBundle());
		return intent;
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
	 * @return Name
	 */
	public String getName() {
		return this.bundle.getString(NAME);
	}

	/**
	 * Set name.
	 * 
	 * @param name
	 *            name
	 */
	public void setName(final String name) {
		this.bundle.putString(NAME, name);
	}

	/**
	 * @return status
	 */
	public short getStatus() {
		return this.bundle.getShort(STATUS, STATUS_INACTIVE);
	}

	/**
	 * Set status.
	 * 
	 * @param status
	 *            status
	 */
	public void setStatus(final short status) {
		this.bundle.putShort(STATUS, status);
	}

	/**
	 * Add status.
	 * 
	 * @param status
	 *            status
	 */
	public void addStatus(final short status) {
		this.setStatus(status | this.getStatus());
	}

	/**
	 * Set status.
	 * 
	 * @param status
	 *            status
	 */
	public void setStatus(final int status) {
		this.bundle.putShort(STATUS, (short) status);
	}

	/**
	 * Set status: ready.
	 */
	public void setReady() {
		this.setStatus(STATUS_ENABLED | STATUS_READY);
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
	 * Set author.
	 * 
	 * @param author
	 *            author
	 */
	public void setAuthor(final String author) {
		this.bundle.putString(AUTHOR, author);
	}

	/**
	 * @return prefs intent uri
	 */
	public String getPrefsIntent() {
		return this.bundle.getString(PREFSINTENT);
	}

	/**
	 * Set prefs intent.
	 * 
	 * @param prefsIntent
	 *            prefs intent
	 */
	public void setPrefsIntent(final String prefsIntent) {
		this.bundle.putString(PREFSINTENT, prefsIntent);
	}

	/**
	 * @return prefs title
	 */
	public String getPrefsTitle() {
		return this.bundle.getString(PREFSTITLE);
	}

	/**
	 * Set prefs title.
	 * 
	 * @param prefsTitle
	 *            prefs title
	 */
	public void setPrefsTitle(final String prefsTitle) {
		this.bundle.putString(PREFSTITLE, prefsTitle);
	}

	/**
	 * @return balance
	 */
	public String getBalance() {
		return this.bundle.getString(BALANCE);
	}

	/**
	 * Set balance.
	 * 
	 * @param balance
	 *            balance
	 */
	public void setBalance(final String balance) {
		this.bundle.putString(BALANCE, balance);
	}

	/**
	 * @return capabilities
	 */
	public short getCapabilities() {
		return this.bundle.getShort(CAPABILITIES, CAPABILITIES_NONE);
	}

	/**
	 * Set capabilities.
	 * 
	 * @param capabilities
	 *            capabilities
	 */
	public void setCapabilities(final short capabilities) {
		this.bundle.putShort(CAPABILITIES, capabilities);
	}

	/**
	 * Set capabilities.
	 * 
	 * @param capabilities
	 *            capabilities
	 */
	public void setCapabilities(final int capabilities) {
		this.setCapabilities((short) capabilities);
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
	 * Get error message.
	 * 
	 * @return error message
	 */
	public String getErrorMessage() {
		return this.bundle.getString(ERRORMESSAGE);
	}

	/**
	 * Set error message.
	 * 
	 * @param error
	 *            error message
	 */
	public void setErrorMessage(final String error) {
		if (error != null) {
			this.addStatus(STATUS_ERROR);
		}
		this.bundle.putString(ERRORMESSAGE, error);
	}

	/**
	 * @return all SubConnectors
	 */
	public SubConnectorSpec[] getSubConnectors() {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		final SubConnectorSpec[] ret = new SubConnectorSpec[c];
		for (int i = 0; i < c; i++) {
			ret[i] = new SubConnectorSpec(// .
					this.bundle.getBundle(SUB_PREFIX + i));
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
	 * Add a SubConnector.
	 * 
	 * @param id
	 *            id
	 * @param name
	 *            name
	 * @param features
	 *            features
	 */
	public void addSubConnector(final String id, final String name,
			final short features) {
		final int c = this.bundle.getInt(SUB_COUNT, 0);
		this.bundle.putBundle(SUB_PREFIX + c, new SubConnectorSpec(id, name,
				features).getBundle());
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
	public void addSubConnector(final String id, final String name,
			final int features) {
		this.addSubConnector(id, name, (short) features);
	}
}
