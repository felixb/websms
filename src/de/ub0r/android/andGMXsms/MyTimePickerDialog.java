package de.ub0r.android.andGMXsms;

import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TimePicker;

/**
 * TimePickerDialog checking time set by user. o2 allows only 00/15/30/45
 * minutes.
 * 
 * @author flx
 */
public class MyTimePickerDialog extends TimePickerDialog {
	/** Last set minutes. */
	private int lastMinutes;

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            Context
	 * @param callBack
	 *            call back method
	 * @param hourOfDay
	 *            hour of day
	 * @param minute
	 *            minute
	 * @param is24HourView
	 *            24h / day or am/pm?
	 */
	public MyTimePickerDialog(final Context context,
			final TimePickerDialog.OnTimeSetListener callBack,
			final int hourOfDay, final int minute, final boolean is24HourView) {
		super(context, callBack, hourOfDay, minute, is24HourView);
		this.lastMinutes = minute;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onTimeChanged(final TimePicker view, final int hourOfDay,
			final int minute) {
		if (WebSMS.prefsConnectorSpecs.getName().equals("WebSMS.o2") // FIXME
				&& minute % 15 != 0) {
			// check input for o2 connector.
			// only 00/15/30/45 allowed
			int newMinute = minute;
			if (this.lastMinutes == 0 && minute == 59) {
				newMinute = 45;
			} else if (this.lastMinutes % 15 != 0) {
				newMinute = ((minute / 15)) * 15;
			} else if (minute >= this.lastMinutes) {
				newMinute = ((minute / 15) + 1) * 15;
			} else {
				newMinute = ((minute / 15)) * 15;
			}
			newMinute = newMinute % 60;
			view.setCurrentMinute(newMinute);
			this.lastMinutes = newMinute;
		} else {
			this.lastMinutes = minute;
		}
	}
}
