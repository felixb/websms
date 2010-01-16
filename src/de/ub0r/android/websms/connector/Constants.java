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

/**
 * @author flx
 */
public final class Constants {

	/** No Constructor needed here. */
	private Constants() {
		// do nothing
		return;
	}

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}.
	 */
	// TODO: split update and bootstrap from send. only connectors needing the
	// bootstrap broadcast should be spawned on that particular broadcast.
	public static final String ACTION_CONNECTOR_RUN = "de.ub0r."
			+ "android.websms.connector.RUN";

	/** Broadcast Action requesting update of {@link ConnectorSpec}'s status. */
	public static final String ACTION_CONNECTOR_UPDATE = "de.ub0r."
			+ "android.websms.connector.UPDATE";

	/**
	 * Broadcast Action sending updated {@link ConnectorSpec} informations back
	 * to WebSMS. This should include a {@link ConnectorSpec}.
	 */
	public static final String ACTION_CONNECTOR_INFO = "de.ub0r."
			+ "android.websms.connector.INFO";
}
