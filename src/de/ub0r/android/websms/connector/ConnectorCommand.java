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
 * A Command send to a Connector.
 * 
 * @author flx
 */
public final class ConnectorCommand {

	/** Key to find command in a Bundle. */
	private static final String EXTRAS_COMMAND = "command";

	/** Command: type. */
	public static final String TYPE = "command_type";
	/** Command: type - bootstrap. */
	public static final short TYPE_BOOTSTRAP = 1;
	/** Command: type - update. */
	public static final short TYPE_UPDATE = 2;
	/** Command: type - send. */
	public static final short TYPE_SEND = 4;
	/** Command: default sender. */
	public static final String DEFSENDER = "command_defsender";
	/** Command: default prefix. */
	public static final String DEFPREFIX = "command_defprefix";
	/** Command: recipients. */
	public static final String RECIPIENTS = "command_reciepients";
	/** Command: text. */
	public static final String TEXT = "command_text";
	/** Command: flashsms. */
	public static final String FLASHSMS = "command_flashsms";
	/** Command: timestamp. */
	public static final String TIMESTAMP = "command_timestamp";
	/** Command: custom sender. */
	public static final String CUSTOMSENDER = "command_customsender";

	/** {@link Bundle} represents the ConnectorSpec. */
	private final Bundle bundle;

	/**
	 * Create command with type update.
	 * 
	 * @return created command
	 */
	public static ConnectorCommand update() {
		return new ConnectorCommand(TYPE_UPDATE);
	}

	/**
	 * Create command with type bootstrap.
	 * 
	 * @return created command
	 */
	public static ConnectorCommand bootstrap() {
		return new ConnectorCommand(TYPE_BOOTSTRAP);
	}

	/**
	 * Create Command with type "send".
	 * 
	 * @param defPrefix
	 *            default prefix
	 * @param defSender
	 *            default sender
	 * @param recipients
	 *            reciepients
	 * @param text
	 *            text
	 * @param flashSMS
	 *            flashsms
	 * @param timestamp
	 *            timestamp for sending
	 * @param customSender
	 *            custom sender
	 * @return created command
	 */
	public static ConnectorCommand send(final String defPrefix,
			final String defSender, final String[] recipients,
			final String text, final boolean flashSMS, final long timestamp,
			final String customSender) {
		ConnectorCommand ret = send(defPrefix, defSender, recipients, text,
				flashSMS);
		ret.setSendLater(timestamp);
		ret.setCustomSender(customSender);
		return ret;
	}

	/**
	 * Create Command with type "send".
	 * 
	 * @param defPrefix
	 *            default prefix
	 * @param defSender
	 *            default sender
	 * @param recipients
	 *            reciepients
	 * @param text
	 *            text
	 * @param flashSMS
	 *            flashsms
	 * @return created command
	 */
	public static ConnectorCommand send(final String defPrefix,
			final String defSender, final String[] recipients,
			final String text, final boolean flashSMS) {
		final Bundle b = new Bundle();
		b.putShort(TYPE, TYPE_SEND);
		b.putString(DEFPREFIX, defPrefix);
		b.putString(DEFSENDER, defSender);
		b.putStringArray(RECIPIENTS, recipients);
		b.putString(TEXT, text);
		b.putBoolean(FLASHSMS, flashSMS);
		b.putLong(TIMESTAMP, -1);
		b.putString(CUSTOMSENDER, null);
		return new ConnectorCommand(b);
	}

	/**
	 * Create Command with type.
	 * 
	 * @param type
	 *            type
	 */
	private ConnectorCommand(final Short type) {
		this.bundle = new Bundle();
		this.bundle.putShort(TYPE, type);
	}

	/**
	 * Create Command from {@link Bundle}.
	 * 
	 * @param b
	 *            Bundle
	 */
	private ConnectorCommand(final Bundle b) {
		this.bundle = b;
	}

	/**
	 * Create Command from {@link Intent}.
	 * 
	 * @param i
	 *            Intent
	 */
	public ConnectorCommand(final Intent i) {
		Bundle e = i.getExtras();
		if (e != null) {
			this.bundle = e.getBundle(EXTRAS_COMMAND);
		} else {
			this.bundle = new Bundle();
		}
	}

	/**
	 * Set this {@link ConnectorCommand} to an {@link Intent}. Creates new
	 * Intent if needed.
	 * 
	 * @param intent
	 *            {@link Intent}.
	 * @return the same {@link Intent}
	 */
	public Intent setToIntent(final Intent intent) {
		Intent i = intent;
		if (i == null) {
			i = new Intent(Constants.ACTION_CONNECTOR_RUN);
		}
		i.putExtra(EXTRAS_COMMAND, this.getBundle());
		return i;
	}

	/**
	 * @return internal bundle
	 */
	public Bundle getBundle() {
		return this.bundle;
	}

	/**
	 * @return type
	 */
	public short getType() {
		return this.bundle.getShort(TYPE);
	}

	/**
	 * @return default sender
	 */
	public String getDefSender() {
		return this.bundle.getString(DEFSENDER);
	}

	/**
	 * @return default prefix
	 */
	public String getDefPrefix() {
		return this.bundle.getString(DEFPREFIX);
	}

	/**
	 * @return recipients
	 */
	public String[] getRecipients() {
		return this.bundle.getStringArray(RECIPIENTS);
	}

	/**
	 * @return text
	 */
	public String getText() {
		return this.bundle.getString(TEXT);
	}

	/**
	 * @return flashsms
	 */
	public boolean getFlashSMS() {
		return this.bundle.getBoolean(FLASHSMS, false);
	}

	/**
	 * @return timestamp for sending
	 */
	public long getSendLater() {
		return this.bundle.getLong(TIMESTAMP, -1);
	}

	/**
	 * Set timestamp for sending later.
	 * 
	 * @param timestamp
	 *            timestamp
	 */
	public void setSendLater(final long timestamp) {
		this.bundle.putLong(TIMESTAMP, timestamp);
	}

	/**
	 * @return custom sender
	 */
	public String getCustomSender() {
		return this.bundle.getString(CUSTOMSENDER);
	}

	/**
	 * Set custom sender.
	 * 
	 * @param customSender
	 *            custom sender
	 */
	public void setCustomSender(final String customSender) {
		this.bundle.putString(CUSTOMSENDER, customSender);
	}
}
