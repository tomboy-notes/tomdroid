/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 *
 * Copyright 2011 Piotr Adamski <mcveat@gmail.com>
 *
 * This file is part of Tomdroid.
 *
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.util;

import android.util.Log;

import static java.text.MessageFormat.format;

/**
 * @author Piotr Adamski <mcveat@gmail.com>
 */
public class TLog {
    // Logging should be disabled for release builds
    private static final boolean LOGGING_ENABLED = false;

    public static void v(String tag, Throwable t, String msg, Object... args) {
        if (LOGGING_ENABLED) Log.v(tag, format(msg, args), t);
    }

    public static void v(String tag, String msg, Object... args) {
    	if (LOGGING_ENABLED) Log.v(tag, format(msg, args));
    }

    public static void d(String tag, Throwable t, String msg, Object... args) {
        if (LOGGING_ENABLED) Log.d(tag, format(msg, args), t);
    }

    public static void d(String tag, String msg, Object... args) {
    	if (LOGGING_ENABLED) Log.d(tag, format(msg, args));
    }

    public static void i(String tag, Throwable t, String msg, Object... args) {
        Log.i(tag, format(msg, args), t);
    }

    public static void i(String tag, String msg, Object... args) {
        Log.i(tag, format(msg, args));
    }

    public static void w(String tag, Throwable t, String msg, Object... args) {
        Log.w(tag, format(msg, args), t);
    }

    public static void w(String tag, String msg, Object... args) {
        Log.w(tag, format(msg, args));
    }

    public static void e(String tag, Throwable t, String msg, Object... args) {
        Log.e(tag, format(msg, args), t);
    }

    public static void e(String tag, String msg, Object... args) {
        Log.e(tag, format(msg, args));
    }
/**
 * FIXME disabled since they were introduced in API level 8 and we target lower
    public static void wtf(String tag, Throwable t, String msg, Object... args) {
        Log.wtf(tag, format(msg, args), t);
    }

    public static void wtf(String tag, String msg, Object... args) {
        Log.wtf(tag, format(msg, args));
    }
 */
}
