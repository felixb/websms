package de.ub0r.android.andGMXsms;

import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

public class MessageHandler extends Handler {

	public static final int WHAT_LOG = 0;
	public static final int WHAT_FREECOUNT = 1;

	@Override
	public void handleMessage(final Message msg) {
		switch (msg.what) {
		case WHAT_LOG:
			String l = (String) msg.obj;
			AndGMXsms.me.lognl(l);
			return;
		case WHAT_FREECOUNT:
			String c = (String) msg.obj;
			TextView tw = (TextView) AndGMXsms.me.findViewById(R.id.freecount);
			tw.setText(AndGMXsms.me.getResources().getString(R.string.free_)
					+ " " + c);
			return;
		}
	}
}
