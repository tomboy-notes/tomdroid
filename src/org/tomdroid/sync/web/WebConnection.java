/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2013 Stefan Hammer <j.4@gmx.at>
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.TLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

public abstract class WebConnection {
	
	private static final String TAG = "WebConnection";
	
	public abstract String get(String uri) throws UnknownHostException;
	public abstract String put(String uri, String data) throws UnknownHostException;
	
	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return sb.toString();
	}
	
	protected String parseResponse(HttpResponse response) {
		
		if (response == null)
			return "";
		
		String result = null;
		
		// Examine the response status
		TLog.i(TAG, "Response status : {0}", response.getStatusLine().toString());

		// Get hold of the response entity
		HttpEntity entity = response.getEntity();
		// If the response does not enclose an entity, there is no need
		// to worry about connection release

		if (entity != null) {
			
			try {
				InputStream instream;
				
				instream = entity.getContent();
				
				result = convertStreamToString(instream);
				
				TLog.i(TAG, "Received : {0}", result);
				
				// Closing the input stream will trigger connection release
				instream.close();
				
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	protected HttpResponse execute(HttpUriRequest request) throws UnknownHostException {
		
		DefaultHttpClient httpclient = MySSLSocketFactory.getNewHttpClient();
		
		try {
			// Execute the request
			TLog.i(TAG, "Sending http-header: {0}: {1}", "X-Tomboy-Client", Tomdroid.HTTP_HEADER);
			request.addHeader("X-Tomboy-Client", Tomdroid.HTTP_HEADER);
			HttpResponse response = httpclient.execute(request);
			return response;
			
		}catch (UnknownHostException e){
			throw e;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			TLog.i(TAG, "Somethings wrong with your HTTP request. Maybe errors in SSL, certificate?");
			e.printStackTrace();
		}
		
		return null;
	}
}