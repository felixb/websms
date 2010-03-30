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
package de.ub0r.android.websms.connector.o2;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import android.util.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * The Class ApacheNetworkHandler handles all network connection using the
 * internal Apache HTTP Client.
 * 
 * @author mastix, flx
 */
class ApacheNetworkHandler {

	/**
	 * Used to decompress the gzipped markup that is returned by the web server.
	 */
	private static final class GzipDecompressingEntity extends
			HttpEntityWrapper {

		/**
		 * Instantiates a new gzip decompressing entity.
		 * 
		 * @param pEntity
		 *            the p_entity
		 */
		private GzipDecompressingEntity(final HttpEntity pEntity) {
			super(pEntity);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public InputStream getContent() throws IOException {
			Log.d(TAG, "gziped.getContent()");
			// the wrapped entity's getContent() decides about repeatability
			final InputStream wrappedin = this.wrappedEntity.getContent();
			return new GZIPInputStream(wrappedin);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getContentLength() {
			// length of ungzipped content is not known
			return -1;
		}
	}

	/** The Logger tag. */
	private static final String TAG = "WebSMS.o2.anh";

	/** The http client. */
	private final transient DefaultHttpClient httpClient;

	/** The request type. */
	private transient HttpRequestBase requestType = null;

	/** The http response. */
	private transient HttpResponse httpResponse = null;

	/** The content. */
	private transient String content = "";

	/** The cookie as string. */
	private transient String cookie = "";

	/** Set cookes on request. */
	private transient boolean setCookie = true;

	/**
	 * Default Constructor.
	 * 
	 * @param cookies
	 *            cookies
	 */
	public ApacheNetworkHandler(final String cookies) {
		if (cookies == null || cookies.length() == 0) {
			this.cookie = "";
			this.setCookie = false;
		} else {
			this.cookie = cookies;
			this.setCookie = true;
		}

		this.httpClient = new DefaultHttpClient();
		this.httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
				CookiePolicy.RFC_2109);
	}

	/**
	 * Gets the content.
	 * 
	 * @return the content
	 */
	public final String getContent() {
		return this.content;
	}

	/**
	 * Get Content as {@link InputStream}.
	 * 
	 * @return {@link InputStream}
	 * @throws IOException
	 *             IOException
	 */
	public final InputStream getContentStream() throws IOException {
		return this.httpResponse.getEntity().getContent();
	}

	/**
	 * Gets the content source.
	 * 
	 * @param pInputStream
	 *            the p_input stream
	 * @param pLineNumberFrom
	 *            the line number from where to parse
	 * @param pLineNumberTo
	 *            the line number to where to parse
	 * @return the content source
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String getContentSource(final InputStream pInputStream,
			final int pLineNumberFrom, final int pLineNumberTo)
			throws IOException {
		return Utils.stream2str(pInputStream, pLineNumberFrom, pLineNumberTo);
	}

	/**
	 * Gets the cookie.
	 * 
	 * @return the cookie
	 */
	public final String getCookie() {
		if (this.setCookie) {
			return this.cookie;
		}
		try {
			StringBuilder cookies = new StringBuilder();
			for (Cookie c : this.httpClient.getCookieStore().getCookies()) {
				cookies.append(c.getName());
				cookies.append('=');
				cookies.append(c.getValue());
				cookies.append(';');
			}
			this.cookie = cookies.toString();
			Log.v(TAG, "Set cookie: " + this.cookie);
		} catch (Exception e) {
			Log.d(TAG, "error in parsing cookies", e);
			this.cookie = "";
		}
		return this.cookie;
	}

	/**
	 * Gets the flow execution key from the markup using regular expressions.
	 * 
	 * @return the flow execution key
	 */
	public final String getFlowExecutionKey() {
		String returnValue;
		final Pattern p = Pattern.compile(Constants.REGEX_FLOWEXECUTIONKEY);
		final Matcher m = p.matcher(this.content);
		final boolean result = m.find();
		if (result) {
			returnValue = m.group(m.groupCount());
		} else {
			returnValue = "";
		}
		return returnValue;
	}

	/**
	 * Gets the free sms.
	 * 
	 * @return the free sms
	 * @throws WebSMSException
	 *             the web sms exception is returned when the FREE SMS String
	 *             has not been found, which would indicate that the session has
	 *             timed out
	 */
	public String getFreeSMS() throws WebSMSException {
		String returnValue;
		final Pattern p = Pattern.compile(Constants.REGEX_REMAINING_SMS,
				Pattern.MULTILINE | Pattern.DOTALL);
		final Matcher m = p.matcher(this.content);
		if (m.find()) {
			returnValue = m.group(m.groupCount());
		} else {
			// it seems that this string could not have been found - probably
			// session timed out. Try it again.
			throw new WebSMSException("failed to locate freesms on site.\n"
					+ "please turn off tweak.");
		}
		return returnValue;
	}

	/**
	 * Gets the http client (used to shutdown the client after usage).
	 * 
	 * @return the http client
	 */
	public final HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * The HTTP handler takes care of the connection.
	 * 
	 * @param pRequestMode
	 *            the p_request mode
	 * @param pUrl
	 *            the p_url
	 * @param pPostReq
	 *            the p_post req
	 * @param pLoadContent
	 *            the p_load content
	 * @param pReferrer
	 *            the referrer
	 * @param pLineNumberFrom
	 *            the line number from where to parse
	 * @param pLineNumberTo
	 *            the line number to where to parse
	 * @throws WebSMSException
	 *             the web sms exception
	 */
	protected final void httpHandler(final String pRequestMode,
			final String pUrl, final ArrayList<BasicNameValuePair> pPostReq,
			final boolean pLoadContent, final String pReferrer,
			final int pLineNumberFrom, final int pLineNumberTo)
			throws WebSMSException {

		this.content = "";

		Log.d(TAG, "Calling URL: " + pUrl);
		Log.d(TAG, "Using Referrer: " + pReferrer);

		try {
			// check if either POST or GET was triggered
			if (pRequestMode.equals(Constants.HTTP_GET)) {
				this.requestType = new HttpGet(pUrl);
			} else {
				this.requestType = new HttpPost(pUrl);
				((HttpPost) this.requestType)
						.setEntity(new UrlEncodedFormEntity(pPostReq,
								Constants.MAIL_ENCODING));
				this.requestType.setHeader("Content-Type",
						"application/x-www-form-urlencoded");
			}

			// set header information
			this.prepareConnection(pReferrer);

			// get response
			this.httpResponse = this.httpClient.execute(this.requestType);
			int resp = this.httpResponse.getStatusLine().getStatusCode();
			if (resp != HttpURLConnection.HTTP_OK
					&& resp != HttpURLConnection.HTTP_MOVED_TEMP) {
				throw new WebSMSException("Handler returned Statuscode: "
						+ resp);
			}

			if (pLoadContent) {
				if (resp == HttpURLConnection.HTTP_OK) {
					this.content = this.getContentSource(this.httpResponse
							.getEntity().getContent(), pLineNumberFrom,
							pLineNumberTo);
				} else if (resp == HttpURLConnection.HTTP_MOVED_TEMP) {
					this.redirectToNewLocation(this.httpResponse.getEntity()
							.getContent(), pLineNumberFrom, pLineNumberTo);
				}
			} else if (pRequestMode.equals(Constants.HTTP_POST)) {
				if (resp != HttpURLConnection.HTTP_OK) {
					Log
							.e(TAG, "Response code: "
									+ this.httpResponse.getStatusLine()
											.getStatusCode());
					Log.e(TAG, "Response code: "
							+ this.httpResponse.getStatusLine()
									.getReasonPhrase());
					Log.e(TAG, "Response code: "
							+ this.httpResponse.getStatusLine()
									.getProtocolVersion());
					throw new WebSMSException("Handler returned Statuscode: "
							+ this.httpResponse.getStatusLine().// .
									getStatusCode());
				}
			}
		} catch (final UnsupportedEncodingException e) {
			throw new WebSMSException(String.format(
					Constants.TEXT_CONNECTION_FAILED, new Object[] { e
							.getMessage() }));
		} catch (final ClientProtocolException e) {
			throw new WebSMSException(String.format(
					Constants.TEXT_CONNECTION_FAILED, new Object[] { e
							.getMessage() }));
		} catch (final IOException e) {
			throw new WebSMSException(String.format(
					Constants.TEXT_CONNECTION_FAILED, new Object[] { e
							.getMessage() }));
		}
	}

	/**
	 * Prepare connection by setting new user agent settings.
	 * 
	 * @param pReferrer
	 *            the referrer
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private void prepareConnection(final String pReferrer) throws IOException {
		this.requestType.setHeader("User-Agent",
				"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US;"
						+ " rv:1.9.1.5) Gecko/20091102 Firefox/3.5.5");
		this.requestType.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;"
						+ "q=0.9,*/*;q=0.8");
		this.requestType.setHeader("Accept-Language", "en-us,en;q=0.5");
		this.requestType.setHeader("Accept-Charset",
				"ISO-8859-15,utf-8;q=0.7,*;q=0.7");
		this.requestType.setHeader("Keep-Alive", "300");
		this.requestType.setHeader("Connection", "keep-alive");
		if (this.setCookie) { // set cookies on first request
			Log.d(TAG, "set fresh cookies: " + this.cookie);
			this.requestType.setHeader("Cookie", this.cookie);
			this.setCookie = false;
		}
		if (pReferrer != null && !pReferrer.trim().equals("")) {
			Log.d(TAG, "Setting referrer to header: " + pReferrer);
			this.requestType.setHeader("Referer", pReferrer);
			if (pReferrer.indexOf(Constants.URL_HOST_EMAIL) != -1) {
				this.requestType.setHeader("Host", Constants.URL_HOST_EMAIL);
			} else {
				this.requestType.setHeader("Host", Constants.URL_HOST_LOGIN);
			}
		}
		// setting interceptors to use gzip compression to get better GPRS/EDGE
		// speed
		this.httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(final HttpRequest pRequest,
					final HttpContext pContext) throws HttpException,
					IOException {
				if (!pRequest.containsHeader("Accept-Encoding")) {
					pRequest.addHeader("Accept-Encoding", "gzip");
				}
			}
		});

		this.httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(final HttpResponse pResponse,
					final HttpContext pContext) throws HttpException,
					IOException {
				final HttpEntity entity = pResponse.getEntity();
				final Header ceheader = entity.getContentEncoding();
				if (ceheader != null) {
					final HeaderElement[] codecs = ceheader.getElements();
					for (final HeaderElement codec : codecs) {
						if (codec.getName().equalsIgnoreCase("gzip")) {
							pResponse.setEntity(new GzipDecompressingEntity(
									pResponse.getEntity()));
							return;
						}
					}
				}
			}
		});
	}

	/**
	 * Redirect to a new location by calling the handler with new location
	 * parameters.
	 * 
	 * @param pIS
	 *            the InputStream that needs to be closed if there's a
	 *            redirection
	 * @param pLineNumberFrom
	 *            the line number from where to parse
	 * @param pLineNumberTo
	 *            the line number to where to parse
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws WebSMSException
	 *             the web sms exception
	 */
	private void redirectToNewLocation(final InputStream pIS,
			final int pLineNumberFrom, final int pLineNumberTo)
			throws IOException, WebSMSException {
		final String newUrl = this.requestType.getFirstHeader("Location")
				.getValue();
		if (pIS != null) {
			pIS.close();
		}
		this.httpHandler(Constants.HTTP_GET, newUrl, null, true, "",
				pLineNumberFrom, pLineNumberTo);
	}
}
