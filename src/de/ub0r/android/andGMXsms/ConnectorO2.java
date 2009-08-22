package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.sonalb.net.http.cookie.Client;
import com.sonalb.net.http.cookie.CookieJar;

/**
 * AsyncTask to manage IO to O2 API.
 * 
 * @author flx
 */
public class ConnectorO2 extends AsyncTask<String, Boolean, Boolean> {
	/** Target host. */
	private static final String TARGET_HOST = "login.o2online.de";
	// private static final String TARGET_HOST = "app5.wr-gmbh.de";
	/** Target path on host. */
	private static final String TARGET_PATH = "https://login.o2online.de"
			+ "/loginRegistration/loginAction" + ".do?_flowId="
			+ "login&o2_type=asp&o2_label=login/co"
			+ "mcenter-login&scheme=http&" + "port=80&server=email."
			+ "o2online.de&url=%2Fssomanager.osp%3FAPIID" + "%3DAUT"
			+ "H-WEBSSO%26TargetApp%3D%2Fsms_new.osp%3F%26o2_type%3" + "Durl"
			+ "%26o2_label%3Dweb2sms-o2online";
	/** Target mime type. */
	private static final String TARGET_CONTENT = "text/plain";
	/** Target mime encoding. */
	private static final String TARGET_ACCEPT_ENCODING = "deflate";
	private static final String TARGET_ACCEPT = "text/html,application"
			+ "/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	private static final String TARGET_ACCEPT_LANGUAGE = "de-de";
	private static final String TARGET_ACCEPT_CHARSET = "ISO-8859-15"
			+ ",utf-8;q=0.7,*;q=0.7";
	private static final String TARGET_KEEP_ALIVE = "300";
	/** HTTP Useragent. */
	// private static final String TARGET_AGENT = "Mozilla/3.0 (compatible)";
	private static final String TARGET_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** recipient. */
	private String[] to;
	/** recipients list. */
	private String tos = "";
	/** text. */
	private String text;

	/** Cookies Client. */
	private Client client = new Client();

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
	 * Get a fresh HTTP-Connection.
	 * 
	 * @param url
	 *            url to open
	 * @param cookies
	 *            cookies to transmit
	 * @param post
	 *            post data?
	 * @return the connection
	 * @throws IOException
	 *             IOException
	 */
	private HttpURLConnection getConnection(final String url,
			final CookieJar cookies, final boolean post) throws IOException {
		HttpURLConnection c = (HttpURLConnection) (new URL(url))
				.openConnection();
		c.setRequestProperty("User-Agent", TARGET_AGENT);
		c.setRequestProperty("Accept", TARGET_ACCEPT);
		c.setRequestProperty("Accept-Language", TARGET_ACCEPT_LANGUAGE);
		c.setRequestProperty("Accept-Encoding", TARGET_ACCEPT_ENCODING);
		c.setRequestProperty("Accept-Charset", TARGET_ACCEPT_CHARSET);
		c.setRequestProperty("Keep-Alive", TARGET_KEEP_ALIVE);
		c.setAllowUserInteraction(false);
		c.setUseCaches(false);
		if (cookies != null) {
			this.client.setCookies(c, cookies);
		}
		if (post) {
			c.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			c.setRequestMethod("POST");
			c.setDoOutput(true);
		}
		return c;
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 */
	private boolean sendData() {
		try { // get Connection
			String url = "https://login.o2online.de"
					+ "/loginRegistration/loginAction.do?_flowId="
					+ "login&o2_type=asp&o2_label=login/co"
					+ "mcenter-login&scheme=http&" + "port=80&server=email."
					+ "o2online.de&url=%2Fssomanager.osp%3FAPIID" + "%3DAUT"
					+ "H-WEBSSO%26TargetApp%3D%2Fsms_new.osp%3F%26o2_type%3"
					+ "Durl" + "%26o2_label%3Dweb2sms-o2online";
			url = "https://login.o2online.de/loginRegistration/loginAction.do"
					+ "?_flowId=login&o2_type=asp&o2_label=login/"
					+ "comcenter-login&scheme=http&port=80&server=email."
					+ "o2online.de&url=%2Fssomanager.osp%3FAPIID%3DAUTH"
					+ "-WEBSSO%26TargetApp%3D%2Fsmscenter_new.osp%253f%"
					+ "26o2_type" + "%3Durl%26o2_label%3Dweb2sms-o2online";
			HttpURLConnection c = this.getConnection(url, null, false);
			int resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			CookieJar cj = this.client.getCookies(c);
			String htmlText = AndGMXsms.stream2String(c.getInputStream());
			String flowExecutionKey = this.getFlowExecutionkey(htmlText);
			htmlText = null;

			url = "https://login.o2online.de/loginRegistration/loginAction.do";
			c = this.getConnection(url, cj, true);

			// push post data
			OutputStreamWriter wr = new OutputStreamWriter(c.getOutputStream());
			wr.write("_flowExecutionKey="
					+ URLEncoder.encode(flowExecutionKey)
					+ "&loginName="
					+ URLEncoder.encode("0"
							+ AndGMXsms.prefsSender.substring(3))
					+ "&password="
					+ URLEncoder.encode(AndGMXsms.prefsPasswordO2)
					+ "&_eventId=login");
			wr.flush();
			wr.close();
			wr = null;
			System.gc();
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			cj.addAll(this.client.getCookies(c));
			htmlText = AndGMXsms.stream2String(c.getInputStream());
			System.out.println(htmlText);
			System.out.println(cj);
			// url =
			// "https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&"
			// +
			// "TargetApp=/smscenter_new.osp%3FAutocompletion%3D1%26SID%3D76254760_akfokdox%26MsgContentID%2520%3D%2520-1%26REF%3D1250431146"
			// ;
			url = "https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&"
					+ "TargetApp=/sms_new.osp%3f&o2_type=url&o2_label=web2sms-o2online";
			// url =
			// "https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&"
			// +
			// "TargetApp=/smscenter_new.osp%3f&o2_type=url&o2_label=web2sms-o2online"
			// ;
			url = "https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&"
					+ "TargetApp=/sms_new.osp?&o2_type=url&o2_label=web2sms-o2online";
			url = "http://email.o2online.de:80/ssomanager.osp?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp?&o2_type=url&o2_label=web2sms-o2online";
			c = this.getConnection(url, cj, false);
			System.gc();
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			cj.addAll(this.client.getCookies(c));
			htmlText = AndGMXsms.stream2String(c.getInputStream());
			System.out.println(htmlText);
			System.out.println(cj);
			/*
			 * if (cookies.equalsIgnoreCase(oldCookies)) { AndGMXsms
			 * .sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
			 * .getResources().getString( R.string.log_error_pw + resp)); return
			 * false; }
			 */
			url = "https://email.o2online.de/smscenter_new.osp?Autocompletion=1&MsgContentID=-1";
			c = this.getConnection(url, cj, false);
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			cj.addAll(this.client.getCookies(c));
			/*
			 * url = "https://email.o2online.de/smscenter_send.osp"; c =
			 * (HttpURLConnection) (new URL(url)).openConnection();
			 * c.setRequestProperty("User-Agent", TARGET_AGENT);
			 * client.setCookies(c, cj); resp = c.getResponseCode(); if (resp !=
			 * HttpURLConnection.HTTP_OK) { //
			 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me //
			 * .getResources().getString( // R.string.log_error_http + resp));
			 * return false; } cj.addAll(client.getCookies(c));
			 */htmlText = AndGMXsms.stream2String(c.getInputStream());
			System.out.println(htmlText);
			System.out.println(cj);

			if (1 == 1) {
				return false;
			}

			// send data
			resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(
								R.string.log_error_http + resp));
			}
			// read received data
			int bufsize = c.getHeaderFieldInt("Content-Length", -1);
			if (bufsize > 0) {
				String resultString = AndGMXsms.stream2String(c
						.getInputStream());
				if (resultString.startsWith("The truth")) { // wrong data sent!

					AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
							.getResources()
							.getString(R.string.log_error_server)
							+ resultString);
					return false;
				}

				/*
				 * // strip packet int resultIndex =
				 * resultString.indexOf("rslt="); String outp =
				 * resultString.substring(resultIndex).replace( "\\p", "\n");
				 * outp = outp.replace("</WR>", "");
				 * 
				 * // get result code String resultValue = this.getParam(outp,
				 * // "rslt"); int rslt; try { rslt =
				 * Integer.parseInt(resultValue); } catch (Exception e) {
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
				 * return false; } switch (rslt) { case RSLT_OK: // ok // fetch
				 * additional info String p = this.getParam(outp,
				 * "free_rem_month"); if (p != null) { AndGMXsms.smsGMXfree =
				 * Integer.parseInt(p); p = this.getParam(outp,
				 * "free_max_month"); if (p != null) { AndGMXsms.smsGMXlimit =
				 * Integer.parseInt(p); } AndGMXsms
				 * .sendMessage(AndGMXsms.MESSAGE_FREECOUNT, null); } p =
				 * this.getParam(outp, "customer_id"); if (p != null) {
				 * AndGMXsms.prefsUser = p; if (AndGMXsms.prefsGMXsender) {
				 * AndGMXsms.prefsSender = this.getParam(outp, "cell_phone"); }
				 * if (this.pw != null) { AndGMXsms.prefsPasswordGMX = this.pw;
				 * } if (this.mail != null) { AndGMXsms.prefsMail = this.mail; }
				 * AndGMXsms.me.savePreferences(); inBootstrap = false;
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_PREFSREADY, null); }
				 * return true; case RSLT_WRONG_CUSTOMER: // wrong user/pw
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				 * .getResources().getString(R.string.log_error_pw)); return
				 * false; case RSLT_WRONG_MAIL: // wrong mail/pw inBootstrap =
				 * false; AndGMXsms.prefsPasswordGMX = "";
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				 * .getResources().getString(R.string.log_error_mail));
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_PREFSREADY, null);
				 * return false; default:
				 * AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, outp + "#" +
				 * rslt); return false; }
				 */
			} else {
				AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
						.getResources().getString(
								R.string.log_http_header_missing));
				return false;
			}

		} catch (IOException e) {
			AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, e.toString());
			return false;
		}
		return false;
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
		// StringBuilder packetData = openBuffer("SEND_SMS", "1.01", true);
		// fill buffer
		// writePair(packetData, "sms_text", this.text);
		StringBuilder recipients = new StringBuilder();
		// table: <id>, <name>, <number>
		int j = 0;
		for (int i = 1; i < this.to.length; i++) {
			if (this.to[i] != null && this.to[i].length() > 1) {
				recipients.append(++j);
				recipients.append("\\;null\\;");
				recipients.append(this.to[i]);
				recipients.append("\\;");
				if (j > 1) {
					this.tos += ", ";
				}
				this.tos += this.to[i];
			}
		}
		this.publishProgress((Boolean) null);
		recipients.append("</TBL>");
		String recipientsString = "<TBL ROWS=\"" + j + "\" COLS=\"3\">"
				+ "receiver_id\\;receiver_name\\;receiver_number\\;"
				+ recipients.toString();
		recipients = null;

		// if date!='': data['send_date'] = date
		// push data
		// if (!this.sendData(closeBuffer(packetData))) {
		if (!this.sendData()) {
			// // failed!
			// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
			// .getResources().getString(R.string.log_error));
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
