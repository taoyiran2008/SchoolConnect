package com.tyr.util;

import android.util.Log;

public class Debugger {
	private static final String TAG = "taoyr";
	private static final boolean DEBUG_FLAG = true;
	private static final boolean ERROR_FLAG = true;

	public static void logDebug(String tag, String msg) {
		if (DEBUG_FLAG) {
			Log.v(tag, msg);
		}
	}

	public static void logError(String tag, String msg) {
		if (ERROR_FLAG) {
			Log.e(tag, msg);
		}
	}

	public static void logDebug(String msg) {
		if (DEBUG_FLAG) {
			Log.v(TAG, msg);
		}
	}

	public static void logError(String msg) {
		if (ERROR_FLAG) {
			Log.e(TAG, msg);
		}
	}
}
