package de.ub0r.android.websms.connector.arcor;

import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.ConnectorCommand;

/**
 * represenst a set of instances needed over the calls. These are:
 * {@link Context}, {@link Intent}, {@link ConnectorCommand}, and
 * {@link DefaultHttpClient}
 * 
 * @author lado
 */
public class ConnectorContext {
	/**
	 * websms passthrough
	 */
	final Context context;
	/**
	 * websms passthrough
	 */
	final Intent intent;

	/**
	 * Wrapped intent
	 */
	final ConnectorCommand command;

	/**
	 * HttpClient, shared over the calls
	 */
	final DefaultHttpClient client;

	/**
	 * getter
	 * 
	 * @return Context
	 */
	public Context getContext() {
		return this.context;
	}

	/**
	 * getter
	 * 
	 * @return Intent
	 */
	public Intent getIntent() {
		return this.intent;
	}

	/**
	 * Will be created from intent
	 * 
	 * @return {@link ConnectorCommand}
	 */
	public ConnectorCommand getCommand() {
		return this.command;
	}

	/**
	 * Private constructor;
	 * 
	 * @param context
	 * @param intent
	 */
	private ConnectorContext(final Context context, final Intent intent) {
		this.context = context;
		this.intent = intent;
		this.command = new ConnectorCommand(this.intent);
		this.client = new DefaultHttpClient();

	}

	/**
	 * getter
	 * 
	 * @return DefaultHttpClient
	 */
	public DefaultHttpClient getClient() {
		return this.client;
	}

	/**
	 * Retrieve {@link SharedPreferences} over the {@link Context}
	 * 
	 * @return SharedPreferences
	 */
	public SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this.context);
	}

	/**
	 * Factory Method
	 * 
	 * @param context
	 * @param intent
	 * @return ConnectorContext
	 */
	public static ConnectorContext create(final Context context,
			final Intent intent) {
		return new ConnectorContext(context, intent);
	}
}
