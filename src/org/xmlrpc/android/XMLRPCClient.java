package org.xmlrpc.android;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

/**
 * original source code from http://code.google.com/p/android-xmlrpc/ released
 * under apache licence 2.0 http://www.apache.org/licenses/LICENSE-2.0
 * XMLRPCClient allows to call remote XMLRPC method.
 * <p>
 * The following table shows how XML-RPC types are mapped to java call
 * parameters/response values.
 * </p>
 * <p>
 * <table border="2" align="center" cellpadding="5">
 * <thead>
 * <tr>
 * <th>XML-RPC Type</th>
 * <th>Call Parameters</th>
 * <th>Call Response</th>
 * </tr>
 * </thead> <tbody>
 * <td>int, i4</td>
 * <td>byte<br />Byte<br />short<br />Short<br />int<br />Integer</td>
 * <td>int<br />Integer</td> </tr>
 * <tr>
 * <td>i8</td>
 * <td>long<br />Long</td>
 * <td>long<br />Long</td>
 * </tr>
 * <tr>
 * <td>double</td>
 * <td>float<br />Float<br />double<br />Double</td>
 * <td>double<br />Double</td>
 * </tr>
 * <tr>
 * <td>string</td>
 * <td>String</td>
 * <td>String</td>
 * </tr>
 * <tr>
 * <td>boolean</td>
 * <td>boolean<br />Boolean</td>
 * <td>boolean<br />Boolean</td>
 * </tr>
 * <tr>
 * <td>dateTime.iso8601</td>
 * <td>java.util.Date<br />java.util.Calendar</td>
 * <td>java.util.Date</td>
 * </tr>
 * <tr>
 * <td>base64</td>
 * <td>byte[]</td>
 * <td>byte[]</td>
 * </tr>
 * <tr>
 * <td>array</td>
 * <td>java.util.List&lt;Object&gt;<br />Object[]</td>
 * <td>Object[]</td>
 * </tr>
 * <tr>
 * <td>struct</td>
 * <td>java.util.Map&lt;String, Object&gt;</td>
 * <td>java.util.Map&lt;String, Object&gt;</td>
 * </tr>
 * </tbody>
 * </table>
 * </p>
 * <p>
 * You can also pass as a parameter any object implementing XMLRPCSerializable
 * interface. In this case your object overrides getSerializable() telling how
 * to serialize to XMLRPC protocol
 * </p>
 */

public class XMLRPCClient {
	private static final String TAG_METHOD_CALL = "methodCall";
	private static final String TAG_METHOD_NAME = "methodName";
	private static final String TAG_METHOD_RESPONSE = "methodResponse";
	private static final String TAG_PARAMS = "params";
	private static final String TAG_PARAM = "param";
	private static final String TAG_FAULT = "fault";
	private static final String TAG_FAULT_CODE = "faultCode";
	private static final String TAG_FAULT_STRING = "faultString";

	private HttpClient client;
	private HttpPost postMethod;
	private XmlSerializer serializer;
	private HttpParams httpParams;
	private IXMLRPCSerializer iXMLRPCSerializer;

	/**
	 * XMLRPCClient constructor. Creates new instance based on server URI
	 * 
	 * @param XMLRPC
	 *            server URI
	 */
	public XMLRPCClient(final URI uri) {
		this.postMethod = new HttpPost(uri);
		this.postMethod.addHeader("Content-Type", "text/xml");

		// WARNING
		// I had to disable "Expect: 100-Continue" header since I had
		// two second delay between sending http POST request and POST body
		this.httpParams = this.postMethod.getParams();
		HttpProtocolParams.setUseExpectContinue(this.httpParams, false);

		this.client = new DefaultHttpClient();
		this.serializer = Xml.newSerializer();
		this.iXMLRPCSerializer = new XMLRPCSerializer();
	}

	/**
	 * Convenience constructor. Creates new instance based on server String
	 * address
	 * 
	 * @param XMLRPC
	 *            server address
	 */
	public XMLRPCClient(final String url) {
		this(URI.create(url));
	}

	/**
	 * Convenience XMLRPCClient constructor. Creates new instance based on
	 * server URL
	 * 
	 * @param XMLRPC
	 *            server URL
	 */
	public XMLRPCClient(final URL url) {
		this(URI.create(url.toExternalForm()));
	}

	/**
	 * Sets custom IXMLRPCSerializer serializer (in case when server doesn't
	 * support standard XMLRPC protocol)
	 * 
	 * @param serializer
	 *            custom serializer
	 */
	public void setSerializer(final IXMLRPCSerializer serializer) {
		this.iXMLRPCSerializer = serializer;
	}

	/**
	 * Sets basic authentication on web request using plain credentials
	 * 
	 * @param username
	 *            The plain text username
	 * @param password
	 *            The plain text password
	 */
	public void setBasicAuthentication(final String username,
			final String password) {
		((DefaultHttpClient) this.client).getCredentialsProvider()
				.setCredentials(
						new AuthScope(this.postMethod.getURI().getHost(),
								this.postMethod.getURI().getPort(),
								AuthScope.ANY_REALM),
						new UsernamePasswordCredentials(username, password));
	}

	/**
	 * Call method with optional parameters. This is general method. If you want
	 * to call your method with 0-8 parameters, you can use more convenience
	 * call() methods
	 * 
	 * @param method
	 *            name of method to call
	 * @param params
	 *            parameters to pass to method (may be null if method has no
	 *            parameters)
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object callEx(final String method, final Object[] params)
			throws XMLRPCException {
		try {
			// prepare POST body
			StringWriter bodyWriter = new StringWriter();
			this.serializer.setOutput(bodyWriter);
			this.serializer.startDocument(null, null);
			this.serializer.startTag(null, TAG_METHOD_CALL);
			// set method name
			this.serializer.startTag(null, TAG_METHOD_NAME).text(method)
					.endTag(null, TAG_METHOD_NAME);
			if (params != null && params.length != 0) {
				// set method params
				this.serializer.startTag(null, TAG_PARAMS);
				for (int i = 0; i < params.length; i++) {
					this.serializer.startTag(null, TAG_PARAM).startTag(null,
							IXMLRPCSerializer.TAG_VALUE);
					this.iXMLRPCSerializer
							.serialize(this.serializer, params[i]);
					this.serializer.endTag(null, IXMLRPCSerializer.TAG_VALUE)
							.endTag(null, TAG_PARAM);
				}
				this.serializer.endTag(null, TAG_PARAMS);
			}
			this.serializer.endTag(null, TAG_METHOD_CALL);
			this.serializer.endDocument();

			// set POST body
			HttpEntity entity = new StringEntity(bodyWriter.toString());
			this.postMethod.setEntity(entity);

			// execute HTTP POST request
			HttpResponse response = this.client.execute(this.postMethod);

			// check status code
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new XMLRPCFault("HTTP status code: " + statusCode
						+ " != " + HttpStatus.SC_OK, statusCode);
			}

			// parse response stuff
			//
			// setup pull parser
			XmlPullParser pullParser = XmlPullParserFactory.newInstance()
					.newPullParser();
			entity = response.getEntity();
			Reader reader = new InputStreamReader(new BufferedInputStream(
					entity.getContent()));
			// for testing purposes only
			// reader = new StringReader(
			// "<?xml version='1.0'?><methodResponse><params><param><value>\n\n\n</value></param></params></methodResponse>"
			// );
			pullParser.setInput(reader);

			// lets start pulling...
			pullParser.nextTag();
			pullParser.require(XmlPullParser.START_TAG, null,
					TAG_METHOD_RESPONSE);

			pullParser.nextTag(); // either TAG_PARAMS (<params>) or TAG_FAULT
			// (<fault>)
			String tag = pullParser.getName();
			if (tag.equals(TAG_PARAMS)) {
				// normal response
				pullParser.nextTag(); // TAG_PARAM (<param>)
				pullParser.require(XmlPullParser.START_TAG, null, TAG_PARAM);
				pullParser.nextTag(); // TAG_VALUE (<value>)
				// no parser.require() here since its called in
				// XMLRPCSerializer.deserialize() below

				// deserialize result
				Object obj = this.iXMLRPCSerializer.deserialize(pullParser);
				entity.consumeContent();
				return obj;
			} else if (tag.equals(TAG_FAULT)) {
				// fault response
				pullParser.nextTag(); // TAG_VALUE (<value>)
				// no parser.require() here since its called in
				// XMLRPCSerializer.deserialize() below

				// deserialize fault result
				Map<String, Object> map = (Map<String, Object>) this.iXMLRPCSerializer
						.deserialize(pullParser);
				String faultString = (String) map.get(TAG_FAULT_STRING);
				int faultCode = (Integer) map.get(TAG_FAULT_CODE);
				entity.consumeContent();
				throw new XMLRPCFault(faultString, faultCode);
			} else {
				entity.consumeContent();
				throw new XMLRPCException("Bad tag <" + tag
						+ "> in XMLRPC response - neither <params> nor <fault>");
			}
		} catch (XMLRPCException e) {
			// catch & propagate XMLRPCException/XMLRPCFault
			throw e;
		} catch (Exception e) {
			// wrap any other Exception(s) around XMLRPCException
			throw new XMLRPCException(e);
		}
	}

	/**
	 * Convenience method call with no parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method) throws XMLRPCException {
		return this.callEx(method, null);
	}

	/**
	 * Convenience method call with one parameter
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0)
			throws XMLRPCException {
		Object[] params = { p0, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with two parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1)
			throws XMLRPCException {
		Object[] params = { p0, p1, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with three parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2) throws XMLRPCException {
		Object[] params = { p0, p1, p2, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with four parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @param p3
	 *            method's 4th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2, final Object p3) throws XMLRPCException {
		Object[] params = { p0, p1, p2, p3, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with five parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @param p3
	 *            method's 4th parameter
	 * @param p4
	 *            method's 5th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2, final Object p3, final Object p4)
			throws XMLRPCException {
		Object[] params = { p0, p1, p2, p3, p4, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with six parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @param p3
	 *            method's 4th parameter
	 * @param p4
	 *            method's 5th parameter
	 * @param p5
	 *            method's 6th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2, final Object p3, final Object p4, final Object p5)
			throws XMLRPCException {
		Object[] params = { p0, p1, p2, p3, p4, p5, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with seven parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @param p3
	 *            method's 4th parameter
	 * @param p4
	 *            method's 5th parameter
	 * @param p5
	 *            method's 6th parameter
	 * @param p6
	 *            method's 7th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2, final Object p3, final Object p4, final Object p5,
			final Object p6) throws XMLRPCException {
		Object[] params = { p0, p1, p2, p3, p4, p5, p6, };
		return this.callEx(method, params);
	}

	/**
	 * Convenience method call with eight parameters
	 * 
	 * @param method
	 *            name of method to call
	 * @param p0
	 *            method's 1st parameter
	 * @param p1
	 *            method's 2nd parameter
	 * @param p2
	 *            method's 3rd parameter
	 * @param p3
	 *            method's 4th parameter
	 * @param p4
	 *            method's 5th parameter
	 * @param p5
	 *            method's 6th parameter
	 * @param p6
	 *            method's 7th parameter
	 * @param p7
	 *            method's 8th parameter
	 * @return deserialized method return value
	 * @throws XMLRPCException
	 */
	public Object call(final String method, final Object p0, final Object p1,
			final Object p2, final Object p3, final Object p4, final Object p5,
			final Object p6, final Object p7) throws XMLRPCException {
		Object[] params = { p0, p1, p2, p3, p4, p5, p6, p7, };
		return this.callEx(method, params);
	}
}
