package com.sonalb.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.sonalb.net.http.cookie.Client;
import com.sonalb.net.http.cookie.CookieJar;
import com.sonalb.net.http.cookie.MalformedCookieException;

/**
 * Convenience class that combines cookie-handling and redirect-handling logic.
 * When the connect() method is invoked, the handler cyclically processes HTTP
 * Redirects (if any), and also takes care of cookie-handling while doing so.
 * The maximum number of redirects defaults to 10. The handler determines a
 * successful run, when the HTTP response code is equal to a specified success
 * code. This code defaults to 200 (OK).
 * 
 * @author Sonal Bansal
 */

public class HTTPRedirectHandler {
	HttpURLConnection huc;
	CookieJar cj = new CookieJar();
	Client client = new Client();
	boolean bCookies = true;
	int successCode = 200;
	int maxRedirects = 10;

	boolean bConnected = false;
	boolean bConnectMethodAlreadyCalled = false;
	InputStream is;

	/**
	 * Creates a handler for the input HttpURLConnection. The HttpURLConnection
	 * must NOT be connected yet.
	 */
	public HTTPRedirectHandler(final HttpURLConnection huc) {
		if (huc == null) {
			throw new NullPointerException();
		}

		this.huc = huc;
	}

	/**
	 * Sets the Client to be used for this handler.
	 */
	public void setClient(final Client cl) {
		if (cl == null) {
			throw new IllegalArgumentException("Null argument.");
		}

		synchronized (this.client) {
			this.client = cl;
		}
	}

	/**
	 * Enables/Disables automated cookie-handling.
	 */
	public void handleCookies(final boolean b) {
		this.bCookies = b;
	}

	/**
	 * Sets the HTTP response code designating a successful run.
	 * 
	 * @param i
	 *            the code; non-positive values ignored
	 */
	public void setSuccessCode(final int i) {
		if (i > 0) {
			this.successCode = i;
		}
	}

	/**
	 * Sets the CookieJar containing Cookies to be used during cookie-handling.
	 */
	public void setCookieJar(final CookieJar cj) {
		if (cj == null || cj.isEmpty()) {
			return;
		}

		this.cj = cj;
	}

	/**
	 * Adds some Cookies to the existing Cookies in an HTTPRedirectHandler.
	 * 
	 * @param cj
	 *            the Cookies to be added
	 */
	public void addCookies(final CookieJar cj) {
		if (cj == null || cj.isEmpty()) {
			return;
		}

		this.cj.addAll(cj);
	}

	/**
	 * Gets the CookieJar containing any pre-existing Cookies, as well as new
	 * ones extracted during processing.
	 * 
	 * @return the CookieJar; always non-null
	 */
	public CookieJar getCookieJar() {
		return (this.cj);
	}

	/**
	 * Gets the InputStream for the final successful response.
	 * 
	 * @throws IllegalStateException
	 *             when called before successful connection.
	 */
	public InputStream getInputStream() {
		if (!this.bConnected) {
			throw new IllegalStateException("Not Connected");
		}

		return (this.is);
	}

	/**
	 * Gets the HttpURLConnection for the final successful response.
	 */
	public HttpURLConnection getConnection() {
		if (!this.bConnected) {
			throw new IllegalStateException("Not Connected");
		}

		return (this.huc);
	}

	/**
	 * Sets the maximum number of redirects that will be followed.
	 * 
	 * @param i
	 *            the max number; non-positive values are ignored
	 */
	public void setMaxRedirects(final int i) {
		if (i > 0) {
			this.maxRedirects = i;
		}
	}

	/**
	 * Connects to initial HttpURLConnection (specified during construction),
	 * and initiates cookie-handling and redirect-handling. It can only be
	 * called once per instance.
	 * 
	 * @throws IOException
	 *             if there is an I/O problem
	 * @throws MalformedCookieException
	 *             if there was a problem with cookie-handling
	 * @throws IllegalStateException
	 *             if this method has already been called
	 */
	public void connect() throws IOException, MalformedCookieException {
		if (this.bConnectMethodAlreadyCalled) {
			throw new IllegalStateException("No can do.");
		}

		this.bConnectMethodAlreadyCalled = true;

		int code;
		URL url;

		HttpURLConnection.setFollowRedirects(false);
		if (!this.cj.isEmpty()) {
			this.client.setCookies(this.huc, this.cj);
		}
		this.is = this.huc.getInputStream();
		this.cj.addAll(this.client.getCookies(this.huc));

		while ((code = this.huc.getResponseCode()) != this.successCode
				&& this.maxRedirects > 0) {
			if (code < 300 || code > 399) {
				throw new IOException("Can't deal with this response code ("
						+ code + ").");
			}

			this.is.close();
			this.is = null;

			url = new URL(this.huc.getHeaderField("location"));

			this.huc.disconnect();
			this.huc = null;

			this.huc = (HttpURLConnection) url.openConnection();
			this.client.setCookies(this.huc, this.cj);
			HttpURLConnection.setFollowRedirects(false);
			this.huc.connect();

			this.is = this.huc.getInputStream();
			this.cj.addAll(this.client.getCookies(this.huc));
			this.maxRedirects--;
		}

		if (this.maxRedirects <= 0 && code != this.successCode) {
			throw new IOException("Max redirects exhausted.");
		}

		this.bConnected = true;
	}

	/**
	 * Checks whether this handler has successfully connected.
	 */
	public boolean isConnected() {
		return (this.bConnected);
	}
}