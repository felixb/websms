package de.ub0r.android.andGMXsms;

import java.util.Hashtable;
import java.util.Vector;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;


import android.util.Log;

public class ConnectorSipgate extends Connector {

	/** Tag for output. */
	private static final String TAG = "WebSMS.Sipgate";
	
	@SuppressWarnings("unchecked")
	@Override
	protected boolean sendMessage() {
		AndGMXsms.sendMessage(AndGMXsms.MESSAGE_DISPLAY_ADS, null);
		String  VERSION = "0.1";
		String	VENDOR = "mirko.weber@gmail.com";
		this.publishProgress((Boolean) null);

	    XMLRPCClient client = new XMLRPCClient("https://samurai.sipgate.net/RPC2");
	    client.setBasicAuthentication(AndGMXsms.prefsUserSipgate, AndGMXsms.prefsPasswordSipgate);
		Object back;
		try {
		    Hashtable<String, String> ident = new Hashtable<String, String>();
		    ident.put("ClientName", TAG);
		    ident.put("ClientVersion", VERSION);
		    ident.put("ClientVendor", VENDOR);
			back = client.call( "samurai.ClientIdentify", ident );
			Log.d(TAG, back.toString());
            Vector<String> remoteUris = new Vector<String>();
            for (int i = 0; i < this.to.length; i++) {
				if (this.to[i] != null && this.to[i].length() > 1) { 	
                    remoteUris.add("sip:" + this.to[i].replaceAll("\\+","") + "@sipgate.net");
                    Log.d(TAG, "Telefonnummer:"+remoteUris.get(i));
				}
            }
            Hashtable params = new Hashtable();
            params.put("RemoteUri", remoteUris);
            params.put("TOS", "text");
            params.put("Content", this.text);
            back = client.call("samurai.SessionInitiateMulti",params);
            Log.d(TAG, back.toString());
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_RESET, null);
			saveMessage(this.to, this.text);
			

		} catch (XMLRPCException e) {
			Log.e(TAG, null, e);
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}
		return true;
	}

	@Override
	protected boolean updateMessages() {
		// TODO Auto-generated method stub
		return false;
	}

}
