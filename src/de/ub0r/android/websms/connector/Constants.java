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

	/** Default constructor. */
	private Constants() {
		// do nothing
		return;
	}

	/** Action to start a connector's {@link Service}. */
	public static final String ACTION_RUN_CONNECTOR = "de.ub0r."
			+ "android.websms.connector.RUN";

	/** Broadcast Action to update Connector's status. */
	public static final String ACTION_UPDATE_CONNECTOR = "de.ub0r."
			+ "android.websms.UPDATE";

	/** Broadcast Action to send updated Connector infos back to WebSMS. */
	public static final String ACTION_CONNECTOR_INFO = "de.ub0r."
			+ "android.websms.INFO";

	/** Key to find a connector in a Bundle. */
	public static final String EXTRAS_CONNECTOR = "connector";

	/** Key to find command in a Bundle. */
	public static final String EXTRAS_COMMAND = "command";

}
