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
package de.ub0r.android.websms.connector.common;


/**
 * @author flx
 */
public final class Constants {

	/** No Constructor needed here. */
	private Constants() {
		// do nothing
		return;
	}

	/** Common Action prefix. */
	private static final String ACTION_PREFIX = "de.ub0r."
			+ "android.websms.connector.";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: boostrap.
	 */
	public static final String ACTION_CONNECTOR_RUN_BOOSTRAP = ACTION_PREFIX
			+ "RUN_BOOTSTRAP";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: update.
	 */
	public static final String ACTION_CONNECTOR_RUN_UPDATE = ACTION_PREFIX
			+ "RUN_UPDATE";

	/**
	 * Action to start a connector's {@link Service}. This should include a
	 * {@link ConnectorCommand}: send.
	 */
	public static final String ACTION_CONNECTOR_RUN_SEND = ACTION_PREFIX
			+ "RUN_SEND";

	/** Broadcast Action requesting update of {@link ConnectorSpec}'s status. */
	public static final String ACTION_CONNECTOR_UPDATE = ACTION_PREFIX
			+ "UPDATE";

	/**
	 * Broadcast Action sending updated {@link ConnectorSpec} informations back
	 * to WebSMS. This should include a {@link ConnectorSpec}.
	 */
	public static final String ACTION_CONNECTOR_INFO = ACTION_PREFIX + "INFO";
}
