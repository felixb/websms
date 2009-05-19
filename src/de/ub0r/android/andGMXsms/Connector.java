package de.ub0r.android.andGMXsms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Message;

public class Connector extends Thread {
	// private static final String TARGET_HOST = "app5.wr-gmbh.de";
	private static final String TARGET_HOST = "app0.wr-gmbh.de";
	private static final String TARGET_PATH = "/WRServer/WRServer.dll/WR";
	private static final String TARGET_ENCODING = "wr-cs";
	private static final String TARGET_CONTENT = "text/plain";
	private static final String TARGET_AGENT = "Mozilla/3.0 (compatible)";
	private static final String TARGET_PROTOVERSION = "1.13.03";

	private static final int MAX_BUFSIZE = 4096;

	private final String to;
	private final String text;

	/**
	 * Create get_free Connector.
	 */
	public Connector() {
		this.to = null;
		this.text = null;
	}

	/**
	 * Create send_sms Connector.
	 * 
	 * @param to
	 *            receiver
	 * @param text
	 *            text
	 */
	public Connector(final String to, final String text) {
		this.to = to;
		this.text = text;
	}

	/**
	 * Create default data hashtable.
	 * 
	 * @return ht
	 */
	private static final Hashtable<String, Object> getBaseData() {
		Hashtable<String, Object> ret = new Hashtable<String, Object>();
		ret.put("customer_id", AndGMXsms.prefs_user);
		ret.put("password", AndGMXsms.prefs_password);
		return ret;
	}

	/**
	 * Send data.
	 * 
	 * @param packetName
	 *            packetName
	 * @param packetVersion
	 *            packetVersion
	 * @param packetData
	 *            packetData
	 * @return successful?
	 */
	private boolean sendData(final String packetName,
			final String packetVersion,
			final Hashtable<String, Object> packetData) {
		try {
			HttpURLConnection c = (HttpURLConnection) (new URL("http://"
					+ TARGET_HOST + TARGET_PATH)).openConnection();

			c.setRequestProperty("User-Agent", TARGET_AGENT);
			c.setRequestProperty("Content-Encoding", TARGET_ENCODING);
			c.setRequestProperty("Content-Type", TARGET_CONTENT);
			c.setRequestMethod("POST");

			c.setDoOutput(true);
			OutputStream os = c.getOutputStream();
			os.write("<WR TYPE=\"RQST\" NAME=\"".getBytes());
			os.write(packetName.getBytes());
			os.write("\" VER=\"".getBytes());
			os.write(packetVersion.getBytes());
			os.write("\" PROGVER=\"".getBytes());
			os.write(TARGET_PROTOVERSION.getBytes());
			os.write("\">".getBytes());

			for (Enumeration<String> keys = packetData.keys(); keys
					.hasMoreElements();) {
				String key = keys.nextElement();
				Object value = packetData.get(key);
				os.write(key.getBytes());
				os.write("=".getBytes());
				if (value instanceof String) {
					String v = ((String) value).replace("\\", "\\\\");
					v = v.replace(">", "\\>");
					v = v.replace("<", "\\<");
					os.write(v.getBytes());
				} else if (value instanceof byte[]) {
					os.write((byte[]) value);
				}
				os.write("\\p".getBytes());
			}
			// add packetData
			// for key in data:
			// payload +=
			// key+"="+str(data[key]).replace("\\","\\\\").replace(">"
			// ,"\\>").replace("<","\\<")+"\\p"
			os.write("</WR>".getBytes());
			os.close();
			os = null;

			int resp = c.getResponseCode();
			if (resp != HttpURLConnection.HTTP_OK) {
				Message.obtain(
						AndGMXsms.me.messageHandler,
						MessageHandler.WHAT_LOG,
						AndGMXsms.me.getResources().getString(
								R.string.log_error_http + resp)).sendToTarget();
			}
			int bufsize = c.getHeaderFieldInt("Content-Length", -1);
			StringBuffer data = null;
			if (bufsize > 0) {
				data = new StringBuffer();
				InputStream is = c.getInputStream();
				byte[] buf;
				if (bufsize > MAX_BUFSIZE) {
					buf = new byte[MAX_BUFSIZE];
				} else {
					buf = new byte[bufsize];
				}
				int read = is.read(buf, 0, buf.length);
				int count = read;
				while (read > 0) {
					data.append(new String(buf, 0, read, "ASCII"));
					read = is.read(buf, 0, buf.length);
					count += read;
				}
				buf = null;
				is.close();
				is = null;
				String resultString = data.toString();
				if (resultString.startsWith("The truth")) {
					Message.obtain(
							AndGMXsms.me.messageHandler,
							MessageHandler.WHAT_LOG,
							AndGMXsms.me.getResources().getString(
									R.string.log_error_server)
									+ resultString).sendToTarget();
					return false;
				}

				int resultIndex = resultString.indexOf("rslt=");
				if (resultIndex < 0) {
					return false;
				}
				String resultValue = resultString.substring(resultIndex + 5,
						resultIndex + 6);
				String outp = resultString.substring(resultIndex).replace(
						"\\p", "\n");
				outp = outp.replace("</WR>", "");
				if (!resultValue.equals("0")) {
					Message.obtain(AndGMXsms.me.messageHandler,
							MessageHandler.WHAT_LOG, outp).sendToTarget();
					return false;
				} else {
					resultIndex = outp.indexOf("free_rem_month=");
					if (resultIndex > 0) {
						int resIndex = outp.indexOf("\n", resultIndex);
						Message.obtain(AndGMXsms.me.messageHandler,
								MessageHandler.WHAT_LOG,
								outp.substring(resultIndex, resIndex))
								.sendToTarget();
					}
				}
			} else {
				Message.obtain(
						AndGMXsms.me.messageHandler,
						MessageHandler.WHAT_LOG,
						AndGMXsms.me.getResources().getString(
								R.string.log_http_header_missing))
						.sendToTarget();
				return false;
			}
		} catch (IOException e) {
			Message.obtain(AndGMXsms.me.messageHandler,
					MessageHandler.WHAT_LOG, e.toString()).sendToTarget();
			return false;
		}
		return true;
	}

	private void getFree() {
		this.sendData("GET_SMS_CREDITS", "1.00", getBaseData());
	}

	private void send() {
		Hashtable<String, Object> packetData = getBaseData();
		packetData.put("sms_text", this.text);
		// table: <id>, <name>, <number>
		String receivers = "<TBL ROWS=\"1\" COLS=\"3\">"
				+ "receiver_id\\;receiver_name\\;receiver_number\\;"
				+ "1\\;null\\;" + this.to + "\\;" + "</TBL>";
		packetData.put("receivers", receivers);
		packetData.put("send_option", "sms");
		packetData.put("sms_sender", AndGMXsms.prefs_sender);
		// if date!='': data['send_date'] = date
		Message.obtain(AndGMXsms.me.messageHandler, MessageHandler.WHAT_LOG,
				AndGMXsms.me.getResources().getString(R.string.log_sending))
				.sendToTarget();
		if (!this.sendData("SEND_SMS", "1.01", packetData)) {
			Message.obtain(AndGMXsms.me.messageHandler,
					MessageHandler.WHAT_LOG,
					AndGMXsms.me.getResources().getString(R.string.log_error))
					.sendToTarget();
		} else {
			Composer.lastMsg = null;
			Composer.lastTo = null;
			Message.obtain(AndGMXsms.me.messageHandler,
					MessageHandler.WHAT_LOG,
					AndGMXsms.me.getResources().getString(R.string.log_done))
					.sendToTarget();
		}
	}

	@Override
	public void run() {
		if (this.to == null) {
			this.getFree();
		} else {
			this.send();
		}
	}
}
