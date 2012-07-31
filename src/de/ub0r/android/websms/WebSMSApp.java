/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of SMSdroid.
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
package de.ub0r.android.websms;

import android.app.Application;
import android.content.res.Resources;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;

import com.actionbarsherlock.app.ActionBar;

import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class WebSMSApp extends Application {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.init("WebSMS");
	}

	/**
	 * Fix ActionBar background. See http://b.android.com/15340.
	 * 
	 * @param ab
	 *            {@link ActionBar}
	 * @param r
	 *            {@link Resources}
	 * @param bg
	 *            res id of background {@link BitmapDrawable}
	 * @param bgSplit
	 *            res id of background {@link BitmapDrawable} in split mode
	 */
	public static void fixActionBarBackground(final ActionBar ab,
			final Resources r, final int bg, final int bgSplit) {
		// This is a workaround for http://b.android.com/15340 from
		// http://stackoverflow.com/a/5852198/132047
		BitmapDrawable d = (BitmapDrawable) r.getDrawable(bg);
		d.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		ab.setBackgroundDrawable(d);
		if (bgSplit >= 0) {
			d = (BitmapDrawable) r.getDrawable(bgSplit);
			d.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			ab.setSplitBackgroundDrawable(d);
		}
	}
}
