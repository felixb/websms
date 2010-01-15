package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

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
	 * After Sending arcor sends, hint/error/warning as status. hint meand sent,
	 * warning and eror bad things happen :)
	 */
	private static final Pattern SEND_CHECK_STATUS_PATTERN = Pattern
			.compile(
					"<div class=\"contentArea\">.+?<div class=\"(hint|warning|error)\">(.+?)</div>",
					Pattern.DOTALL);
	/**
	 * This String will be matched if the user is logged in.
	 */
	private static final String MATCH_LOGIN_SUCCESS = "logout.jsp";

	/**
	 * Keine SMS mehr?
	 */
	private static final String MATCH_NO_SMS = "Sie haben derzeit keine SMS zur Verf";
	/**
	 * Cache this client over several calls
	 */
	private HttpClient client = new DefaultHttpClient();
	/**
	 * HTTP Header User-Agent.
	 */
	// TODO share this. Make it Configurable global and local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	/** where to find sms count */
	private static final int APPROXIMATE_SMS_COUNT_POSITION = 18000;

	/** where to find sms count */
	private static final int APPROXIMATE_SMS_COUNT_LENGTH = 3000;

	/** where to find logout link */
	private static final int APPROXIMATE_LOGOUT_LINK_COUNT_POSITION = 4000;

	/** where to find logout link */
	private static final int APPROXIMATE_LOGOUT_LENGTH = 2000;
	/**
	 * Login URL, to send Login (POST).
	 */
	private static final String LOGIN_URL = "https://www.arcor.de/login/login.jsp";
	/**
	 * Send SMS URL(POST) / Free SMS Count URL(GET).
	 */
	private static final String SMS_URL = "https://www.arcor.de/ums/ums_neu_sms.jsp";

	private static final String AROCR_ENCODING = "ISO-8859-1";

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
			final HttpPost request = createPOST(LOGIN_URL, getLoginPost(user,
					password));
			final HttpResponse response = client.execute(request);
			final String cutContent = cutLoginInfoFromContent(response
					.getEntity().getContent());
			if (cutContent.indexOf(MATCH_LOGIN_SUCCESS) == -1) {
				pushMessage(WebSMS.MESSAGE_LOG, this.context
						.getString(R.string.log_login_unsuccessfull_arcor));
				return false;
			}
			return true;
		} catch (final Exception e) {
			throw new WebSMSException(e.getMessage());
		}
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
			final HttpResponse response = client.execute(new HttpGet(SMS_URL));
			return pushFreeCount(cutFreeCountFromContent(response.getEntity()
					.getContent()));
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
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
		String term = null;
		if (m.find()) {
			term = m.group(1) + "+" + m.group(2);
		} else if (content.contains(MATCH_NO_SMS)) {
			term = "0+0";
		} else {
			return false;
		}
		WebSMS.SMS_BALANCE[ARCOR] = term;
		pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
		return true;
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
			final HttpResponse response = client.execute(createPOST(SMS_URL,
					getSmsPost(to, text)));
			return afterSmsSent(response);
		} catch (final Exception ex) {
			throw new WebSMSException(ex.getMessage());
		}
	}

	/**
	 * Handles content after sms sending.
	 * 
	 * @param response
	 *            HTTP Response
	 * @return true if arcor returns success
	 * @throws Exception
	 *             if an Error occures
	 */
	private boolean afterSmsSent(final HttpResponse response) throws Exception {

		String body = cutFreeCountFromContent(response.getEntity().getContent());

		Matcher m = SEND_CHECK_STATUS_PATTERN.matcher(body);
		boolean found = m.find();
		if (!found || m.groupCount() != 2) {
			// should not happen
			Log.w("WebSMS.ConnectorArcor", body);
			throw new Exception(context
					.getString(R.string.log_unknow_status_after_send_arcor));
		}

		String status = m.group(1);
		// ok, message sent!
		if (status.equals("hint")) {
			return pushFreeCount(body);
		}
		// warning or error
		throw new Exception(m.group(2));
	}

	/**
	 * This post data is needed for log in.
	 * 
	 * @param username
	 *            username
	 * @param password
	 *            password
	 * @return array of params
	 * @throws UnsupportedEncodingException
	 *             if the url can not be encoded
	 */
	private static String getLoginPost(final String username,
			final String password) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder();
		sb
				.append("user_name=")
				.append(encode(username))
				.append("&password=")
				.append(encode(password))
				.append(
						"&login=Login&protocol=https&info=Online-Passwort&goto=");
		return sb.toString();
	}

	/**
	 * These post data is needed for sending a sms.
	 * 
	 * @param to
	 *            receiver array
	 * @param text
	 *            text to send
	 * @return array of params
	 * @throws Exception
	 *             if an error occures.
	 */
	private static String getSmsPost(final String[] to, final String text)
			throws Exception {
		final StringBuilder sb = new StringBuilder();
		for (final String r : to) {
			sb.append(r).append(",");
		}
		StringBuilder sb1 = new StringBuilder();
		sb1.append("empfaengerAn=").append(encode(sb.toString())).//
				append("&emailAdressen=").append(encode(WebSMS.prefsSender)).//
				append("&nachricht=").append(encode(text)).//
				append("&firstVisitOfPage=foo&part=0&senden=Senden");

		if (WebSMS.prefsCopySentSmsArcor) {
			sb1.append("&gesendetkopiesms=on");
		}
		if (WebSMS.prefsEnableValidatedNumberArcor) {
			sb1.append("&useOwnMobile=on");
		}

		return sb1.toString();
	}

	private static String encode(String string)
			throws UnsupportedEncodingException {
		return URLEncoder.encode(string, AROCR_ENCODING);
	}

	/**
	 * Create and Prepare a Post Request. Set also an User-Agent
	 * 
	 * @param url
	 *            http post url
	 * @param params
	 *            key=value pairs as string[] post data
	 * @return HttpPost
	 * @throws Exception
	 *             if an error occures
	 */
	private static HttpPost createPOST(final String url,
			final String urlencodedparams) throws Exception {
		final HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", FAKE_USER_AGENT);
		post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE,
				URLEncodedUtils.CONTENT_TYPE));
		post.setEntity(new StringEntity(urlencodedparams));
		return post;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean sendMessage() throws WebSMSException {
		return login() && sendSms();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final boolean updateMessages() throws WebSMSException {
		return login() && updateBalance();
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
	private static String cutLoginInfoFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_LOGOUT_LINK_COUNT_POSITION);
		final byte[] data = readBytes(APPROXIMATE_LOGOUT_LENGTH, is);
		return new String(data, AROCR_ENCODING);
	}

	/**
	 * skip 'bytes' size bytes from stream, consuming it in 'step' steps.
	 * 
	 * @param is
	 *            Content stream
	 * @param bytes
	 *            bytes to skip
	 * @throws IOException
	 *             on an I/O error
	 */
	private static void skip(final InputStream is, final long bytes)
			throws IOException {
		long alreadySkipped = 0;
		long skip = 0;
		while (true) {
			skip = is.skip(256);
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
	private static byte[] readBytes(final int size, final InputStream is)
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
	private static String cutFreeCountFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_SMS_COUNT_POSITION);
		final byte[] data = readBytes(APPROXIMATE_SMS_COUNT_LENGTH, is);
		return new String(data, AROCR_ENCODING);
	}

}
