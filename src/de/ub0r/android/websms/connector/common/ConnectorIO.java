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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import de.ub0r.android.andGMXsms.Connector.WebSMSException;

/**
 * Base for all Connectors.
 * 
 * @author flx
 */
public abstract class ConnectorIO {

	/** Context to use. */
	protected final Context context;
	/** Bundle representing the connector. */
	protected final ConnectorSpec connector;
	/** Bundle representing the command to the connector. */
	protected final ConnectorCommand command;

	/**
	 * Create a connector task.
	 * 
	 * @param c
	 *            context
	 * @param i
	 *            intent
	 */
	public ConnectorIO(final Context c, final Intent i) {
		this.context = c;
		final Bundle b = i.getExtras();
		if (b == null) {
			this.command = null;
			this.connector = null;
		} else {
			// TODO: parse recipients.
			this.command = new ConnectorCommand(i);
			this.connector = new ConnectorSpec(i);
		}
	}

	/**
	 * @return command
	 */
	public final ConnectorCommand getCommand() {
		return this.command;
	}

	/**
	 * @return connector
	 */
	public final ConnectorSpec getConnector() {
		return this.connector;
	}

	/**
	 * Do bootstrap.
	 */
	protected void doBootstrap() {
		// do nothing by default
		return;
	}

	/**
	 * Do update.
	 */
	protected void doUpdate() {
		// do nothing by default
		return;
	}

	/**
	 * Do send.
	 * 
	 * @throws WebSMSException
	 *             WebSMSException
	 */
	protected void doSend() throws WebSMSException {
		// do nothing by default
		return;
	}
}
