package de.ub0r.android.andGMXsms;

import android.os.Handler;
import android.os.Message;

public class MessageHandler extends Handler {

	public static final int WHAT_LOG = 0;

	@Override
	public void handleMessage(final Message msg) {
		switch (msg.what) {
		case WHAT_LOG:
			String l = (String) msg.obj;
			AndGMXsms.me.lognl(l);
			return;
		}
	}
}
