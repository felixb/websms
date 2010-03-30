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

/**
 * The Class Constants holds all constants needed for the application.
 * 
 * @author mastix, flx
 * @since v0.1
 */
public final class Constants {
	/** HTTP method: GET. */
	public static final String HTTP_GET = "GET";
	/** HTTP method: POST. */
	public static final String HTTP_POST = "POST";

	/** The Constant URL_HOST_LOGIN. */
	public static final String URL_HOST_LOGIN = "login.o2online.de";

	/** The Constant URL_HOST_EMAIL. */
	public static final String URL_HOST_EMAIL = "email.o2online.de";

	/** The Constant TEXT_CONNECTION_FAILED. */
	public static final String TEXT_CONNECTION_FAILED = // .
	"Connection failed, reason: %s";

	/** The Constant URL_LOGIN_REGISTRATION. */
	public static final String URL_LOGIN_REGISTRATION = // .
	"https://login.o2online.de/loginRegistration/loginAction.do?"
			+ "_flowId=login&o2_type=asp&o2_label=login/comcenter-login"
			+ "&scheme=http&port=80&server=email.o2online.de&url="
			+ "%2Fssomanager.osp%3FAPIID%3DAUTH-WEBSSO%26TargetApp%3D%2F"
			+ "sms_new.osp%253f%26o2_type%3Durl%26o2_label%3Dweb2sms-o2online";

	/** The Constant URL_LOGIN_ACTION. */
	public static final String URL_LOGIN_ACTION = "https://login.o2online.de/"
			+ "loginRegistration/loginAction.do";

	/** The Constant URL_LOGOUT_ACTION. */
	public static final String URL_LOGOUT_ACTION = "https://login.o2online.de/"
			+ "loginRegistration/loginAction.do?_flowId=logout";

	/** The Constant URL_TARGET_APP_SMS_NEW. */
	public static final String URL_TARGET_APP_SMS_NEW = // .
	"https://email.o2online.de/ssomanager.osp?APIID=AUTH-WEBSSO&"
			+ "TargetApp=/sms_new.osp%3f&o2_type=url&o2_label=web2sms-o2online";

	/** The Constant URL_SMS_CENTER. */
	public static final String URL_SMS_CENTER = "https://email.o2online.de/"
			+ "smscenter_new.osp?Autocompletion=1&MsgContentID=-1";

	/** The Constant URL_SEND_SMS. */
	public static final String URL_SEND_SMS = "https://email.o2online.de/"
			+ "smscenter_send.osp";

	/** The Constant URL_SEND_SMS. */
	public static final String URL_SCHEDULE_SMS = "https://email.o2online.de/"
			+ "smscenter_schedule.osp";

	/** The Constant REGEX_FLOWEXECUTIONKEY. */
	public static final String REGEX_FLOWEXECUTIONKEY = // .
	".*name=\"_flowExecutionKey\" value=\"(.*)\" />";

	/**
	 * The line of the HTML markup where the flowexecution key string appears.
	 */
	public static final int FLOWEXECUTIONKEY_LINENUMBER = 213;

	/** The Constant REGEX_SMS_FORM. */
	public static final String REGEX_SMS_FORM = // .
	"<form name=\"frmSMS\"(.*)</form>";

	/** The Constant REGEX_SMS_FORM_INPUT. */
	public static final String REGEX_SMS_FORM_INPUT = // .
	"<input type=\"(.*)\" name=\"(.*)\" value=\"(.*)\">";

	/** The Constant REGEX_REMAINING_SMS. */
	public static final String REGEX_REMAINING_SMS = // .
	"strong>Frei-SMS: ([0-9]*) Web2SMS noch in diesem Monat "
			+ "mit Ihrem Internet-Pack inklusive!</strong";

	/** The line of the HTML markup where the "free sms" String appears. */
	public static final int SMS_LINENUMBER_FROM = 100;

	/** The line of the HTML markup where the "free sms" String appears. */
	public static final int SMS_LINENUMBER_TO = 20000;

	/** The Constant REGEX_REMAINING_SMS. */
	public static final String REGEX_OWN_SENDERNAME = "^[a-zA-Z]+$";

	/**
	 * Tells whether how many lines before and after a given line need to be
	 * fetched.
	 */
	public static final int LINENUMBER_BUFFER = 20;

	/** Sets the time patter for the Free SMS String. * */
	public static final String DATE_TIME_PATTERN = "dd.MM.yyyy - HH:mm";

	/** The Constant MIN_SIZE_MANUALSENDERNAME. */
	public static final int MIN_SIZE_MANUALSENDERNAME = 4;

	/** The Constant TIME_QUARTER. */
	public static final int TIME_QUARTER = 15;

	/** The Constant TIME_THREEQUARTER. */
	public static final int TIME_THREEQUARTER = 45;

	/** The Constant TIME_HOURINMINUTES. */
	public static final int TIME_HOURINMINUTES = 60;

	/** The Constant TIME_DAYINHOURS. */
	public static final int TIME_DAYINHOURS = 24;

	/** The Constant TIME_MINIMUMTWODIGIT. */
	public static final int TIME_MINIMUMTWODIGIT = 10;

	/** The Constant SLEEP_TIME_LONG. */
	public static final int SLEEP_TIME_LONG = 3000;

	/**
	 * The Constant TIME_MAX_SESSIONTIMEOUT sets the time to check if a session
	 * timeout occured.
	 */
	public static final long TIME_MAX_SESSIONTIMEOUT = 3600000;

	/** The Constant MAIL_ENCODING. */
	public static final String MAIL_ENCODING = "ISO-8859-15";

	/** The Constant FREE_SMS_COUNTRIES_US. */
	public static final int FREE_SMS_COUNTRIES_US = 19;

	/** The Constant MAX_COUNTRYCODE_CHAR_LENGTH. */
	public static final int MAX_COUNTRYCODE_CHAR_LENGTH = 4;

	/** The FRE e_ sm s_ countries. */
	public static final String[] FREE_SMS_COUNTRIES = { "+49", "+45", "+33",
			"+358", "+30", "+44", "+39", "+385", "+31", "+47", "+43", "+46",
			"+41", "+421", "+386", "+34", "+420", "+90", "+36", "+1", "+20",
			"+376", "+374", "+994", "+61", "+973", "+880", "+375", "+387",
			"+55", "+673", "+359", "+56", "+372", "+298", "+995" };

	/**
	 * Private constructor to prevent instantiation.
	 */
	private Constants() {
	}
}
