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

/**
 * 
 * Connector for arcor.de free sms / payed sms
 * 
 * 
 * @author lado
 * 
 */
public class ConnectorArcor extends Connector {

	/**
	 * Extract free and payed sms count from response//TODO try match only
	 * substring(performance) or even try if arcor web server support partial
	 * content
	 */
	private final static Pattern BALANCE_MATCH_PATTERN = Pattern
			.compile(
					"<td.+class=\"txtRed\".*?<b>(\\d{1,2})</b>.+<td.+class=\"txtRed\".*?<b>(\\d{1,})</b>",
					Pattern.DOTALL);

	private final static String MATCH_LOGIN_SUCCESS = "logout.jsp";

	private HttpClient client = null;

	// TODO share this. Make it Configurable clobal and local
	private static final String FAKE_USER_AGENT = "Mozilla/5.0 (Windows; U;"
			+ " Windows NT 5.1; de; rv:1.9.0.9) Gecko/2009040821"
			+ " Firefox/3.0.9 (.NET CLR 3.5.30729)";

	private static final int APPROXIMATE_SMS_COUNT_POSITION = 19000;// txtRed is
	// pos 20919

	private static final int APPROXIMATE_SMS_COUNT_LENGTH = 4000;

	private static final int APPROXIMATE_LOGOUT_LINK_COUNT_POSITION = 4000;

	private static final int APPROXIMATE_LOGOUT_LENGTH = 2000;

	/**
	 * needed urls
	 * 
	 * @author lado
	 * 
	 */
	private interface URL {
		public static final String Login = "https://www.arcor.de/login/login.jsp";
		public static final String Sms = "https://www.arcor.de/ums/ums_neu_sms.jsp";
	}

	protected ConnectorArcor(String u, String p, short con) {
		super(u, p, con);
	}

	/**
	 * Login to arcor
	 * 
	 * @return
	 * @throws WebSMSException
	 */
	protected boolean login() throws WebSMSException {
		try {
			HttpClient client = getHttpClient();
			HttpPost request = createPOST(URL.Login, getLoginPost());
			HttpResponse response = client.execute(request);
			String cutContent = cutLoginInfoFromContent(response.getEntity()
					.getContent());
			return cutContent.indexOf(MATCH_LOGIN_SUCCESS) > 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Simple execute a Http Request
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private HttpResponse execute(HttpRequestBase request) throws Exception {
		return getHttpClient().execute(request);
	}

	/**
	 * Updates balance und pushes it to WebSMS
	 * 
	 * @return
	 * @throws WebSMSException
	 */
	private boolean updateBalance() throws WebSMSException {
		try {
			HttpResponse response = execute(createGET(URL.Sms, null));
			// String c = slurp(new
			// InputStreamReader(response.getEntity().getContent()));
			return pushFreeCount(cutFreeCountFromContent(response.getEntity()
					.getContent()));
		} catch (Exception ex) {
			throw new WebSMSException(ex.getMessage());// TODO better ex
		}
	}

	private boolean pushFreeCount(String content) {
		Matcher m = BALANCE_MATCH_PATTERN.matcher(content);
		if (m.find() == true) {
			String freeCountTerm = m.group(1) + "f, " + m.group(2) + "g";
			WebSMS.SMS_BALANCE[ARCOR] = freeCountTerm;
			pushMessage(WebSMS.MESSAGE_FREECOUNT, null);
			return true;
		}
		return false;
	}

	/**
	 * @return
	 * @throws WebSMSException
	 */
	private boolean sendSms() throws WebSMSException {
		try {
			HttpResponse response = execute(createPOST(URL.Sms, getSmsPost()));
			// String res = stream2str(response.getEntity().getContent());

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

			pushFreeCount(cutFreeCountFromContent(response.getEntity()
					.getContent()));
			return true;
		} catch (Exception ex) {
			return false;
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
	 * This post data is needed for log in
	 * 
	 * @return
	 */
	private ArrayList<BasicNameValuePair> getLoginPost() {
		ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		post.add(new BasicNameValuePair("user_name", this.user));
		post.add(new BasicNameValuePair("password", this.password));
		post.add(new BasicNameValuePair("login", "Login"));
		post.add(new BasicNameValuePair("protocol", "https"));
		post.add(new BasicNameValuePair("info", "Online-Passwort"));
		post.add(new BasicNameValuePair("goto", ""));
		return post;
	}

	/**
	 * These post data is needed for sending a sms
	 * 
	 * @return
	 */
	protected ArrayList<BasicNameValuePair> getSmsPost() {
		ArrayList<BasicNameValuePair> post = new ArrayList<BasicNameValuePair>();
		StringBuilder sb = new StringBuilder();
		for (String r : this.to) {
			sb.append(r).append(",");
		}
		post.add(new BasicNameValuePair("empfaengerAn", sb.toString()));

		post.add(new BasicNameValuePair("emailAdressen", WebSMS.prefsSender)); // TODO
		// customize
		// http://code.google.com/p/websmsdroid/issues/detail?id=42&colspec=ID%20Type%20Status%20Priority%20Product%20Component%20Owner%20Summary#c6
		post.add(new BasicNameValuePair("nachricht", this.text));
		// http://code.google.com/p/websmsdroid/issues/detail?id=42&colspec=ID%20Type%20Status%20Priority%20Product%20Component%20Owner%20Summary#c8
		if (WebSMS.prefsCopySendSmsArcor == true) {
			post.add(new BasicNameValuePair("gesendetkopiesms", "on"));
		}
		// post.add(new BasicNameValuePair("firstVisitOfPage", "0")); do we need
		// this?
		post.add(new BasicNameValuePair("part", "0"));
		post.add(new BasicNameValuePair("senden", "Senden"));
		return post;
	}

	/**
	 * Create and Prepare a Get Request. Set also an User-Agent
	 * 
	 * @param url
	 * @param params
	 * @return
	 */
	private HttpGet createGET(String url, List<BasicNameValuePair> params) {
		if (params == null || params.isEmpty()) {
			return new HttpGet(url);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(url).append("?");
		for (BasicNameValuePair p : params) {
			sb.append(p.getName()).append("=").append(p.getValue());
		}
		return new HttpGet(sb.toString());
	}

	/**
	 * Create and Prepare a Post Request. Set also an User-Agent
	 * 
	 * @param url
	 * @param postData
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private HttpPost createPOST(String url, List<BasicNameValuePair> postData)
			throws UnsupportedEncodingException {
		HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", FAKE_USER_AGENT);
		post.setEntity(new UrlEncodedFormEntity(postData));
		return post;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean sendMessage() throws WebSMSException {
		return login() && sendSms();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean updateMessages() throws WebSMSException {
		return (login() && updateBalance());
	}

	/**
	 * Cuts only for us interessting content part. Here, if user ist logged in
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private String cutLoginInfoFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_LOGOUT_LINK_COUNT_POSITION, 128);
		byte data[] = readBytes(APPROXIMATE_LOGOUT_LENGTH, is);
		if (data.length == 0) {
			return "";
		}
		return new String(data, "ISO-8859-1");
	}

	/**
	 * skip 'bytes' size bytes from stream, consuming it in 'step' steps.
	 * 
	 * @param is
	 * @param bytes
	 * @param step
	 * @throws IOException
	 */
	private void skip(final InputStream is, long bytes, int step)
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
	 * Reads a size portion of bytes from strem
	 * 
	 * @param size
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private byte[] readBytes(int size, InputStream is) throws IOException {
		byte data[] = new byte[size];
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
	 * Cuts only for us interessting content part. Here, to match free sms count
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private String cutFreeCountFromContent(final InputStream is)
			throws IOException {
		skip(is, APPROXIMATE_SMS_COUNT_POSITION, 256);
		byte data[] = readBytes(APPROXIMATE_SMS_COUNT_LENGTH, is);
		if (data.length == 0) {
			return "";
		}
		return new String(data, "ISO-8859-1");
	}

}
