package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.impl.cookie.CookieSpecBase;
import org.apache.http.message.BasicNameValuePair;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.sonalb.net.http.cookie.Client;
import com.sonalb.net.http.cookie.Cookie;
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
	 * Get a fresh HTTP-Connection.
	 * 
	 * @param url
	 *            url to open
	 * @param cookies
	 *            cookies to transmit
	 * @param postData
	 *            post data
	 * @return the connection
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private HttpResponse getHttpClient(final String url,
			final ArrayList<org.apache.http.cookie.Cookie> cookies,
			final ArrayList<BasicNameValuePair> postData)
			throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpRequestBase request;
		if (postData == null) {
			request = new HttpGet(url);
		} else {
			request = new HttpPost(url);
			((HttpPost) request).setEntity(new UrlEncodedFormEntity(postData));
		}
		request.setHeader("User-Agent", TARGET_AGENT);

		if (cookies != null && cookies.size() > 0) {
			CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
			for (Header cookieHeader : cookieSpecBase.formatCookies(cookies)) {
				// Setting the cookie
				request.setHeader(cookieHeader);
			}
		}
		return client.execute(request);
	}

	/**
	 * Update cookies from response.
	 * 
	 * @param cookies
	 *            old cookie list
	 * @param headers
	 *            headers from response
	 * @param url
	 *            requested url
	 * @throws URISyntaxException
	 *             malformed uri
	 * @throws MalformedCookieException
	 *             malformed cookie
	 */
	private void updateCookies(
			final ArrayList<org.apache.http.cookie.Cookie> cookies,
			Header[] headers, final String url) throws URISyntaxException,
			MalformedCookieException {
		final URI uri = new URI(url);
		int port = uri.getPort();
		if (port < 0) {
			if (url.startsWith("https")) {
				port = 443;
			} else {
				port = 80;
			}
		}
		CookieOrigin origin = new CookieOrigin(uri.getHost(), port, uri
				.getPath(), false);
		CookieSpecBase cookieSpecBase = new BrowserCompatSpec();
		for (Header header : headers) {
			for (org.apache.http.cookie.Cookie cookie : cookieSpecBase.parse(
					header, origin)) {
				// THE cookie
				String name = cookie.getName();
				String value = cookie.getValue();
				if (value == null || value.equals("")) {
					continue;
				}
				for (org.apache.http.cookie.Cookie c : cookies) {
					if (name.equals(c.getName())) {
						cookies.remove(c);
						cookies.add(cookie);
						name = null;
						break;
					}
				}
				if (name != null) {
					cookies.add(cookie);
				}
			}
		}
	}

	/**
	 * Send data.
	 * 
	 * @return successful?
	 */
	private boolean sendData() {
		try { // get Connection
			String url = "https://login.o2online.de/loginRegistration/loginAction.do"
					+ "?_flowId=login&o2_type=asp&o2_label=login/"
					+ "comcenter-login&scheme=http&port=80&server=email."
					+ "o2online.de&url=%2Fssomanager.osp%3FAPIID%3DAUTH"
					+ "-WEBSSO%26TargetApp%3D%2Fsmscenter_new.osp%253f%"
					+ "26o2_type" + "%3Durl%26o2_label%3Dweb2sms-o2online";
			ArrayList<org.apache.http.cookie.Cookie> cookies = new ArrayList<org.apache.http.cookie.Cookie>();
			HttpResponse response = this.getHttpClient(url, cookies, null);
			int resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), url);
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
			response = this.getHttpClient(url, cookies, postData);
			postData = null;
			resp = response.getStatusLine().getStatusCode();
			System.gc();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), url);

			url = "http://email.o2online.de:80/ssomanager.osp?APIID=AUTH-WEBSSO&TargetApp=/smscenter_new.osp?&o2_type=url&o2_label=web2sms-o2online";
			response = this.getHttpClient(url, cookies, null);
			resp = response.getStatusLine().getStatusCode();
			System.gc();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), url);

			url = "https://email.o2online.de/smscenter_new.osp?Autocompletion=1&MsgContentID=-1";
			response = this.getHttpClient(url, cookies, null);
			resp = response.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
				// .getResources().getString(
				// R.string.log_error_http + resp));
				return false;
			}
			updateCookies(cookies, response.getAllHeaders(), url);
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
				response = this.getHttpClient(url, cookies, postData);
				postData = null;
				resp = response.getStatusLine().getStatusCode();
				if (resp != HttpURLConnection.HTTP_OK) {
					// AndGMXsms.sendMessage(AndGMXsms.MESSAGE_LOG, AndGMXsms.me
					// .getResources().getString(
					// R.string.log_error_http + resp));
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
