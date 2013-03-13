/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.tomdroid.sync.web;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.tomdroid.ui.Tomdroid;

public class AnonymousConnection extends WebConnection {

	@Override
	public String get(String uri) throws UnknownHostException
	{
		// Prepare a request object
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader("X-Tomboy-Client", Tomdroid.HTTP_HEADER);
		HttpResponse response = execute(httpGet);
		return parseResponse(response);
	}
	
	@Override
	public String put(String uri, String data) throws UnknownHostException {
		
		// Prepare a request object
		HttpPut httpPut = new HttpPut(uri);
		
		try {
			// The default http content charset is ISO-8859-1, JSON requires UTF-8
			httpPut.setEntity(new StringEntity(data, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		
		httpPut.setHeader("Content-Type", "application/json");
		httpPut.addHeader("X-Tomboy-Client", Tomdroid.HTTP_HEADER);
		HttpResponse response = execute(httpPut);
		return parseResponse(response);
	}
}
