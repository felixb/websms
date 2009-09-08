package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.message.BasicNameValuePair;

import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends AsyncTask<String, Boolean, Boolean> {
	/** HTTP Useragent. */
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** recipient. */
	private String[] to;
	/** recipients list. */
	private String tos = "";
	/** text. */
	private String text;

	/**
	 * Extract _flowExecutionKey from HTML output.
	 * 
	 * @param html
	 *            input
	 * @return _flowExecutionKey
	 */
	private String getFlowExecutionkey(final String html) {
		String ret = "";
		int i = html.indexOf("name=\"_flowExecutionKey\" value=\"");
		if (i > 0) {
			int j = html.indexOf("\"", i + 35);
			if (j >= 0) {
				ret = html.substring(i + 32, j);
			}
		}
		return ret;
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 */
	private boolean sendData() {
		try { // get Connection
			String url = "https://login.o2online.de/loginRegistration"
					+ "/loginAction.do"
					+ "?_flowId=login&o2_type=asp&o2_label=login/"
					+ "comcenter-login&scheme=http&port=80&server=email."
					+ "o2online.de&url=%2Fssomanager.osp%3FAPIID%3DAUTH"
					+ "-WEBSSO%26TargetApp%3D%2Fsmscenter_new.osp%253f%"
					+ "26o2_type" + "%3Durl%26o2_label%3Dweb2sms-o2online";
			ArrayList<org.apache.http.cookie.Cookie> cookies = new ArrayList<org.apache.http.cookie.Cookie>();
			HttpResponse response = AndGMXsms.getHttpClient(url, cookies, null,
					TARGET_AGENT);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ " " + resp);
				return false;
			}
			AndGMXsms.updateCookies(cookies, response.getAllHeaders(), url);
			String htmlText = AndGMXsms.stream2String(response.getEntity()
					.getContent());
			String flowExecutionKey = this.getFlowExecutionkey(htmlText);
			htmlText = null;

			url = "https://login.o2online.de/loginRegistration/loginAction.do";
			// post data
			ArrayList<BasicNameValuePair> postData = new ArrayList<BasicNameValuePair>(
					4);
			postData.add(new BasicNameValuePair("_flowExecutionKey",
					flowExecutionKey));
			postData.add(new BasicNameValuePair("loginName", "0"
					+ AndGMXsms.prefsSender.substring(3)));
			postData.add(new BasicNameValuePair("password",
					AndGMXsms.prefsPasswordO2));
			postData.add(new BasicNameValuePair("_eventId", "login"));
			response = AndGMXsms.getHttpClient(url, cookies, postData,
					TARGET_AGENT);
			postData = null;
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ " " + resp);
				return false;
			}
			AndGMXsms.updateCookies(cookies, response.getAllHeaders(), url);

			url = "http://email.o2online.de:80/ssomanager.osp"
					+ "?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp"
					+ "?&o2_type=url&o2_label=web2sms-o2online";
			response = AndGMXsms
					.getHttpClient(url, cookies, null, TARGET_AGENT);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ " " + resp);
				return false;
			}
			AndGMXsms.updateCookies(cookies, response.getAllHeaders(), url);

			url = "https://email.o2online.de/smscenter_new.osp"
					+ "?Autocompletion=1&MsgContentID=-1";
			response = AndGMXsms
					.getHttpClient(url, cookies, null, TARGET_AGENT);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(R.string.log_error_http)
						+ " " + resp);
				return false;
			}
			AndGMXsms.updateCookies(cookies, response.getAllHeaders(), url);
			htmlText = AndGMXsms.stream2String(response.getEntity()
					.getContent());
			int i = htmlText.indexOf("Frei-SMS: ");
			if (i > 0) {
				int j = htmlText.indexOf("Web2SMS", i);
				if (j > 0) {
					AndGMXsms.smsO2free = Integer.parseInt(htmlText.substring(
							i + 9, j).trim());
					AndGMXsms.sendMessage(AndGMXsms.MESSAGE_FREECOUNT, null);
				}
			}

			if (this.text != null && this.tos != null) {
				url = "https://email.o2online.de/smscenter_send.osp";
				postData = new ArrayList<BasicNameValuePair>(4);
				postData.add(new BasicNameValuePair("SMSTo", this.tos));
				postData.add(new BasicNameValuePair("SMSText", this.text));
				postData.add(new BasicNameValuePair("SMSFrom", ""));
				postData.add(new BasicNameValuePair("Frequency", "5"));
				response = AndGMXsms.getHttpClient(url, cookies, postData,
						TARGET_AGENT);
				postData = null;
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
							.getResources().getString(R.string.log_error_http)
							+ " " + resp);
					return false;
				}
			}
		} catch (IOException e) {
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (URISyntaxException e) {
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		} catch (MalformedCookieException e) {
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}
		return true;
	}

	/**
	 * Get free sms count.
	 * 
	 * @return ok?
	 */
	private boolean getFree() {
		return this.sendData();
	}

	/**
	 * Send sms.
	 * 
	 * @return ok?
	 */
	private boolean send() {
		AndGMXsms.sendMessage(AndGMXsms.MESSAGE_DISPLAY_ADS, null);
		int j = 0;
		for (int i = 1; i < this.to.length; i++) {
			if (this.to[i] != null && this.to[i].length() > 1) {
				if (j > 1) {
					this.tos += ", ";
				}
				this.tos += this.to[i];
			}
		}
		this.publishProgress((Boolean) null);
		if (!this.sendData()) {
			// failed!
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
					.getResources().getString(R.string.log_error));
			return false;
		} else {
			// result: ok
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_RESET, null);
			AndGMXsms.saveMessage(this.to, this.text);
			return true;
		}
	}

	/**
	 * Run IO in background.
	 * 
	 * @param textTo
	 *            (text,recipient)
	 * @return ok?
	 */
	@Override
	protected final Boolean doInBackground(final String... textTo) {
		boolean ret = false;
		if (textTo == null || textTo[0] == null) {
			this.publishProgress((Boolean) null);
			ret = this.getFree();
		} else {
			this.text = textTo[ConnectorGMX.ID_TEXT];
			this.to = textTo;
			this.publishProgress((Boolean) null);
			ret = this.send();
		}
		return new Boolean(ret);
	}

	/**
	 * Update progress. Only ran once on startup to display progress dialog.
	 * 
	 * @param progress
	 *            finished?
	 */
	@Override
	protected final void onProgressUpdate(final Boolean... progress) {
		if (AndGMXsms.dialog != null) {
			try {
				AndGMXsms.dialog.dismiss();
			} catch (Exception e) {
				// do nothing
			}
		}
		if (this.to == null) {
			AndGMXsms.dialogString = AndGMXsms.me.getResources().getString(
					R.string.log_update);
			AndGMXsms.dialog = ProgressDialog.show(AndGMXsms.me, null,
					AndGMXsms.dialogString, true);
		} else {
			AndGMXsms.dialogString = AndGMXsms.me.getResources().getString(
					R.string.log_sending);
			if (this.tos != null && this.tos.length() > 0) {
				AndGMXsms.dialogString += " (" + this.tos + ")";
			}
			AndGMXsms.dialog = ProgressDialog.show(AndGMXsms.me, null,
					AndGMXsms.dialogString, true);
		}
	}

	/**
	 * Push data back to GUI. Close progress dialog.
	 * 
	 * @param result
	 *            result
	 */
	@Override
	protected final void onPostExecute(final Boolean result) {
		AndGMXsms.dialogString = null;
		if (AndGMXsms.dialog != null) {
			try {
				AndGMXsms.dialog.dismiss();
				AndGMXsms.dialog = null;
			} catch (Exception e) {
				System.gc();
			}
		}
	}
}
