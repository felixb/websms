package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

/**
 * Connector for arcor.de free sms / payed sms.
 * 
 * @author lado
 */
public class ConnectorArcor extends Connector {

	/**
	 * Pattern to extract free sms count from sms page. Looks like
	 */
	private static final Pattern BALANCE_MATCH_PATTERN = Pattern
			.compile(
					"<td.+class=\"txtRed\".*?<b>(\\d{1,2})</b>.+<td.+class=\"txtRed\".*?<b>(\\d{1,})</b>",
					Pattern.DOTALL);

	/**
	 * This String will be matched if the user is logged in.
	 */
	private static final String MATCH_LOGIN_SUCCESS = "logout.jsp";

	/**
	 * Cache this client over several http calls.
	 */
	private HttpClient client = null;

	/**
	 * HTTP Header User-Agent.
	 */
	// TODO share this. Make it Configurable clobaland local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** */
	private static final int APPROXIMATE_SMS_COUNT_POSITION = 19000;

	/** */
	private static final int APPROXIMATE_SMS_COUNT_LENGTH = 4000;

	/** */
	private static final int APPROXIMATE_LOGOUT_LINK_COUNT_POSITION = 4000;

	/** */
	private static final int APPROXIMATE_LOGOUT_LENGTH = 2000;

	private static final String LOG_TAG = "WebSMS."
			+ ConnectorArcor.class.getSimpleName();

	/**
	 * needed urls.
	 * 
	 * @author lado
	 */
	private interface URL {
		/**
		 * Login URL, to send Login (POST).
		 */
		String LOGIN = "https://www.arcor.de/login/login.jsp";
		/**
		 * Send SMS URL(POST) / Free SMS Count URL(GET).
		 */
		String SMS = "https://www.arcor.de/ums/ums_neu_sms.jsp";
	}

	/**
	 * The Only Constructor.
	 * 
	 * @param u
	 *            Username
	 * @param p
	 *            Password
	 * @param con
	 *            Connection type
	 */
	protected ConnectorArcor(final String u, final String p, final short con) {
		super(u, p, con);
	}

	/**
	 * Login to arcor.
	 * 
	 * @return true if successfullu logged in, false otherwise.
	 * @throws WebSMSException
	 *             if any Exception occures.
	 */
	private boolean login() throws WebSMSException {
		try {
			final HttpClient httpClient = this.getHttpClient();
			final HttpPost request = this.createPOST(URL.LOGIN, this
					.getLoginPost());
			final HttpResponse response = httpClient.execute(request);
			final String cutContent = this.cutLoginInfoFromContent(response
					.getEntity().getContent());
			return cutContent.indexOf(MATCH_LOGIN_SUCCESS) > 0;
		} catch (final IOException e) {
			Log.w(LOG_TAG, e.getMessage());
			return false;
		}
	}

	/**
	 * Simple execute a Http Request.
	 * 
	 * @param request
	 *            a Http Request
	 * @return HttpResponse
	 * @throws Exception
	 *             if an error occures
	 */
	private HttpResponse execute(final HttpRequestBase request)
			throws Exception {
		return this.getHttpClient().execute(request);
	}

	/**
	 * Updates balance und pushes it to WebSMS.
	 * 
	 * @return successful?
	 * @throws WebSMSException
	 *             on an error
	 */
	private boolean updateBalance() throws WebSMSException {
		try {
			final HttpResponse response = this.execute(this.createGET(URL.SMS,
					null));
			return this.pushFreeCount(this.cutFreeCountFromContent(response
					.getEntity().getContent()));
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage()); // TODO better ex
		}
	}

	/**
	 * Push SMS Free Count to WebSMS.
	 * 
	 * @param content
	 *            conten to investigate.
	 * @return push ok?
	 */
	private boolean pushFreeCount(final String content) {
		final Matcher m = BALANCE_MATCH_PATTERN.matcher(content);
		if (m.find()) {
			WebSMS.SMS_BALANCE[ARCOR] = m.group(1) + "+" + m.group(2);
			this.pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			return true;
		}
		return false;
	}

	/**
	 * Get Sender. If arcor specific sender defined, use it. Otherwise user
	 * global sender.
	 * 
	 * @return sender
	 */
	private String getSender() {
		if (WebSMS.prefsEnableSenderNumberArcor) {
			return WebSMS.prefsSenderNumberArcor;
		}
		return WebSMS.prefsSender;
	}

	/**
	 * Sends an sms via HTTP POST.
	 * 
	 * @return successfull?
	 * @throws WebSMSException
	 *             on an error
	 */
	private boolean sendSms() throws WebSMSException {
		try {
			final HttpResponse response = this.execute(this.createPOST(URL.SMS,
					this.getSmsPost()));

			// TODO hier folgendes auswerten
			// warning
			// <div class="contentArea">
			// <div class="warning">Die angegebene SMS-Nummernliste enthielt 1
			// doppelten Eintrag. Dieser wurde entfernt. Bitte klicken Sie
			// nochmals auf Senden.</div>

			// error
			// <div class="contentArea">
			// <div class="error">Kein Empfänger angegeben!</div>

			// Die Nachticht dem user präsentieren

			this.pushFreeCount(this.cutFreeCountFromContent(response
					.getEntity().getContent()));
			return true;
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * If not already created. create an instance of {@link HttpClient}. This
	 * will be cached for a sending cycle as a member variable. So login && send
	 * || login && updateBalance needs only one instance
	 * 
	 * @return {@link HttpClient} instance
	 */
	private HttpClient getHttpClient() {
		if (this.client == null) {
			this.client = new DefaultHttpClient();
		}
		return this.client;
	}

	/**
	 * This post data is needed for log in.
	 * 
	 * @return List of BasicNameValuePairs
	 */
	private ArrayList<BasicNameValuePair> getLoginPost() {
		final ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		post.add(new BasicNameValuePair("user_name", this.user));
		post.add(new BasicNameValuePair("password", this.password));
		post.add(new BasicNameValuePair("login", "Login"));
		post.add(new BasicNameValuePair("protocol", "https"));
		post.add(new BasicNameValuePair("info", "Online-Passwort"));
		post.add(new BasicNameValuePair("goto", ""));
		return post;
	}

	/**
	 * These post data is needed for sending a sms.
	 * 
	 * @return List of BasicNameValuePairs
	 */
	private ArrayList<BasicNameValuePair> getSmsPost() {
		final ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		final StringBuilder sb = new StringBuilder();
		for (final String r : this.to) {
			sb.append(r).append(",");
		}
		post.add(new BasicNameValuePair("empfaengerAn", sb.toString()));

		post.add(new BasicNameValuePair("emailAdressen", getSender()));

		post.add(new BasicNameValuePair("nachricht", this.text));
		// http://code.google.com/p/websmsdroid/issues/detail?id=42&colspec=ID%20Type%20Status%20Priority%20Product%20Component%20Owner%20Summary#c8
		if (WebSMS.prefsCopySentSmsArcor) {
			post.add(new BasicNameValuePair("gesendetkopiesms", "on"));
		}
		// post.add(new BasicNameValuePair("firstVisitOfPage", "0")); do we need
		// this?
		post.add(new BasicNameValuePair("part", "0"));
		post.add(new BasicNameValuePair("senden", "Senden"));
		return post;
	}

	/**
	 * Create and Prepare a Get Request. Sets also an User-Agent.
	 * 
	 * @param url
	 *            GET url
	 * @param params
	 *            Http Get params.
	 * @return HttpGet
	 */
	private HttpGet createGET(final String url,
			final List<BasicNameValuePair> params) {
		if (params == null || params.isEmpty()) {
			return new HttpGet(url);
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(url).append("?");
		for (final BasicNameValuePair p : params) {
			sb.append(p.getName()).append("=").append(p.getValue());
		}
		return new HttpGet(sb.toString());
	}

	/**
	 * Create and Prepare a Post Request. Set also an User-Agent
	 * 
	 * @param url
	 *            http post url
	 * @param postData
	 *            post data
	 * @return HttpPost
	 * @throws UnsupportedEncodingException
	 *             UnsupportedEncodingException
	 */
	private HttpPost createPOST(final String url,
			final List<BasicNameValuePair> postData)
			throws UnsupportedEncodingException {
		final HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", FAKE_USER_AGENT);
		post.setEntity(new UrlEncodedFormEntity(postData));
		return post;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		return this.login() && this.sendSms();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return (this.login() && this.updateBalance());
	}

	/**
	 * Cuts only for us interessting content part. Here, if user ist logged in.
	 * 
	 * @param is
	 *            response body stream
	 * @return cut content
	 * @throws IOException
	 *             on an I/O error
	 */
	private String cutLoginInfoFromContent(final InputStream is)
			throws IOException {
		this.skip(is, APPROXIMATE_LOGOUT_LINK_COUNT_POSITION, 128);
		final byte[] data = this.readBytes(APPROXIMATE_LOGOUT_LENGTH, is);
		return new String(data, "ISO-8859-1");
	}

	/**
	 * skip 'bytes' size bytes from stream, consuming it in 'step' steps.
	 * 
	 * @param is
	 *            Content stream
	 * @param bytes
	 *            bytes to skip
	 * @param step
	 *            try to skip 'step' skips at once
	 * @throws IOException
	 *             on an I/O error
	 */
	private void skip(final InputStream is, final long bytes, final int step)
			throws IOException {
		long alreadySkipped = 0;
		long skip = 0;
		while (true) {
			skip = is.skip(step);
			if (skip == 0) {
				break;
			}
			alreadySkipped += skip;
			if (alreadySkipped >= bytes) {
				break;
			}
		}
	}

	/**
	 * Reads a size portion of bytes from strem.
	 * 
	 * @param size
	 *            to read
	 * @param is
	 *            content stream
	 * @return read bytes
	 * @throws IOException
	 *             on an I/O error
	 */
	private byte[] readBytes(final int size, final InputStream is)
			throws IOException {
		final byte[] data = new byte[size];
		int offset = 0;
		int read = 0;
		int length = size;
		while (true) {
			read = is.read(data, offset, length);
			if (read == -1) {
				break;
			}
			offset = offset + read;
			if (offset >= size) {
				break;
			}
			length = length - read;
		}
		return data;
	}

	/**
	 * Cuts only for us interessting content part. Here, to match free sms
	 * count.
	 * 
	 * @param is
	 *            HttpResponse strem.
	 * @return cut part of the response.
	 * @throws IOException
	 *             on I/O error.
	 */
	private String cutFreeCountFromContent(final InputStream is)
			throws IOException {
		this.skip(is, APPROXIMATE_SMS_COUNT_POSITION, 256);
		final byte[] data = this.readBytes(APPROXIMATE_SMS_COUNT_LENGTH, is);
		return new String(data, "ISO-8859-1");
	}

}
