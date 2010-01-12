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
import android.os.AsyncTask;
import de.ub0r.android.andGMXsms.Connector.WebSMSException;

/**
 * {Qlink AsyncTask} run by the Connectors {@link ConnectorService}.
 * 
 * @author flx
 */
public class ConnectorTask extends AsyncTask<Void, Void, Void> {

	/** Connector class which will do the actual IO. */
	private final ConnectorIO connector;

	/** Connectorservice. */
	private final ConnectorService service;

	/**
	 * Create a connector task.
	 * 
	 * @param c
	 *            {@link ConnectorIO}
	 * @param s
	 *            {@link ConnectorService}
	 */
	public ConnectorTask(final ConnectorIO c, final ConnectorService s) {
		this.connector = c;
		this.service = s;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Void doInBackground(final Void... arg0) {
		try {
			switch (this.connector.getCommand()
					.getShort(Constants.COMMAND_TYPE)) {
			case Constants.COMMAND_BOOTSTRAP:
				this.connector.doBootstrap();
				break;
			case Constants.COMMAND_UPDATE:
				this.connector.doSend();
				break;
			case Constants.COMMAND_SEND:
				this.connector.doSend();
				break;
			default:
				break;
			}
		} catch (WebSMSException e) {
			// TODO: handle exception
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPostExecute(final Void result) {
		final Intent intent = new Intent(Constants.ACTION_UPDATE_CONNECTOR);
		intent.putExtra(Constants.EXTRAS_CONNECTOR, this.connector
				.getConnector());
		this.service.sendBroadcast(intent);
		this.service.unregister();
	}
}
