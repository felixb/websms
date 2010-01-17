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

import android.content.Intent;
import android.os.AsyncTask;

/**
 * {@link AsyncTask} run by the Connector's {@link ConnectorService}.
 * 
 * @author flx
 */
public class ConnectorTask extends AsyncTask<Void, Void, Void> {

	/** Connector class which will do the actual IO. */
	private final CommandReceiver receiver;
	/** Used connector. */
	private final ConnectorSpec connector;
	/** Command running. */
	private final ConnectorCommand command;
	/** Connectorservice. */
	private final ConnectorService service;

	/**
	 * Create a connector task.
	 * 
	 * @param c
	 *            {@link ConnectorSpec}
	 * @param com
	 *            {@link ConnectorCommand}
	 * @param r
	 *            {@link CommandReceiver}
	 * @param s
	 *            {@link ConnectorService}
	 */
	public ConnectorTask(final ConnectorSpec c, final ConnectorCommand com,
			final CommandReceiver r, final ConnectorService s) {
		this.connector = c;
		this.command = com;
		this.receiver = r;
		this.service = s;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Void doInBackground(final Void... arg0) {
		try {
			switch (this.command.getType()) {
			case ConnectorCommand.TYPE_BOOTSTRAP:
				this.receiver.doBootstrap();
				break;
			case ConnectorCommand.TYPE_UPDATE:
				this.receiver.doSend();
				break;
			case ConnectorCommand.TYPE_SEND:
				this.receiver.doSend();
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
		final Intent intent = this.connector.setToIntent(null);
		this.service.sendBroadcast(intent);
		this.service.unregister();
	}
}
