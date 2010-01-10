/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of AndGMXsms.
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

package de.ub0r.android.andGMXsms;

import android.content.Context;
import android.content.Intent;

/**
 * ConnectorSpecs presents all necessary informations to use a connector.
 * 
 * @author flx
 */
public interface ConnectorSpecs {

	/** Prefs: enable connector. */
	String PREFS_ENABLED = "enable_";

	/**
	 * Init ConnectorSpecs' context.
	 * 
	 * @param c
	 *            context
	 */
	void init(final Context c);

	/**
	 * Get a fresh Connector.
	 * 
	 * @param c
	 *            context
	 * @return Connector
	 */
	Connector getConnector(final Context c);

	/**
	 * @return true if connector is enabled
	 */
	boolean isEnabled();

	/**
	 * Set Account's balance.
	 * 
	 * @param b
	 *            balance
	 */
	void setBalance(final String b);

	/**
	 * Get Conector's account balance. This does not run any update!
	 * 
	 * @return balance
	 */
	String getBalance();

	/**
	 * @return connector's author
	 */
	String getAuthor();

	/**
	 * @return prefix for preferences
	 */
	String getPrefsPrefix();

	/**
	 * @return connector's preference intent.
	 */
	Intent getPreferencesIntent();

	/**
	 * @param shortName
	 *            get short name?
	 * @return conector's name
	 */
	String getName(final boolean shortName);

	/**
	 * @return true if more than one recipient is allowed.
	 */
	boolean supportMultipleRecipients();

	/**
	 * Check whether this connector supports flashsms.
	 * 
	 * @return true if connector supports flashsms
	 */
	boolean supportFlashsms();

	/**
	 * Check whether this connector supports custom sender.
	 * 
	 * @return true if connector supports custom sender
	 */
	boolean supportCustomsender();

	/**
	 * Check whether this connector supports send later.
	 * 
	 * @return true if connector supports custom sender
	 */
	boolean supportSendLater();
}
