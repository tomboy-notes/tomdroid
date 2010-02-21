package org.tomdroid.sync.web;

import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;

public class AnonymousConnection extends WebConnection {

	@Override
	public String get(String uri)
	{
		// Prepare a request object
		HttpGet httpGet = new HttpGet(uri);
		HttpResponse response = execute(httpGet);
		return parseResponse(response);
	}
	
	@Override
	public String put(String uri, String data) {
		
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
		HttpResponse response = execute(httpPut);
		return parseResponse(response);
	}
}
