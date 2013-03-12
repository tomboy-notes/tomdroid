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

import android.net.Uri;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.Preferences;
import org.tomdroid.util.TLog;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

public class OAuthConnection extends WebConnection {
	
	private static final String TAG = "OAuthConnection";
	private static final String CONSUMER_KEY = "anyone";
	private static final String CONSUMER_SECRET = "anyone";
	
	private OAuthConsumer consumer = null;
	
	public String accessToken = "";
	public String accessTokenSecret = "";
	public String requestToken = "";
	public String requestTokenSecret = "";
	public boolean oauth10a = false;
	public String authorizeUrl = "";
	public String requestTokenUrl = "";
	public String accessTokenUrl = "";
	public String rootApi = "";
	public String userApi = "";
	
	public OAuthConnection() {
		
		consumer = new CommonsHttpOAuthConsumer(
				CONSUMER_KEY,
				CONSUMER_SECRET);
	}

	public boolean isAuthenticated() {
		
		if (accessToken.equals("") || accessTokenSecret.equals(""))
			return false;
		else
			return true;
	}
	
	private OAuthProvider getProvider() {
		
		// use our http client that accepts self-signed certificates
		DefaultHttpClient httpclient = MySSLSocketFactory.getNewHttpClient();
		
		// Use the provider bundled with signpost, the android libs are buggy
		// See: http://code.google.com/p/oauth-signpost/issues/detail?id=20
		OAuthProvider provider = new CommonsHttpOAuthProvider(
				requestTokenUrl,
				accessTokenUrl,
				authorizeUrl,
				httpclient);
		provider.setOAuth10a(oauth10a);
		
		return provider;
	}
	
	private void sign(HttpRequest request) {
		
		if (isAuthenticated())
			consumer.setTokenWithSecret(accessToken, accessTokenSecret);
		else
			return;
		
		// TODO: figure out if we should throw exceptions
		try {
			consumer.sign(request);
		} catch (OAuthMessageSignerException e1) {
			e1.printStackTrace();
		} catch (OAuthExpectationFailedException e1) {
			e1.printStackTrace();
		} catch (OAuthCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Uri getAuthorizationUrl(String server) throws UnknownHostException {
		
		String url = "";
		
		// this method shouldn't have been called
		if (isAuthenticated())
			return null;
		
		rootApi = server+"/api/1.0/";
		
		AnonymousConnection connection = new AnonymousConnection();
		String response = connection.get(rootApi);
		
		JSONObject jsonResponse;
		
		try {
			jsonResponse = new JSONObject(response);
			
			accessTokenUrl = jsonResponse.getString("oauth_access_token_url");
			requestTokenUrl = jsonResponse.getString("oauth_request_token_url");
			authorizeUrl = jsonResponse.getString("oauth_authorize_url");
			
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
		OAuthProvider provider = getProvider();
		
		try {
			// the argument is the callback used when the remote authorization is complete
			url = provider.retrieveRequestToken(consumer, "tomdroid://sync");
			
			requestToken = consumer.getToken();
			requestTokenSecret = consumer.getTokenSecret();
			oauth10a = provider.isOAuth10a();
			accessToken = "";
			accessTokenSecret = "";
			saveConfiguration();
			
		} catch (OAuthMessageSignerException e1) {
			e1.printStackTrace();
			return null;
		} catch (OAuthNotAuthorizedException e1) {
			e1.printStackTrace();
			return null;
		} catch (OAuthExpectationFailedException e1) {
			e1.printStackTrace();
			return null;
		} catch (OAuthCommunicationException e1) {
			e1.printStackTrace();
			return null;
		}
		
		TLog.i(TAG, "Authorization URL : {0}", url);
		
		return Uri.parse(url);
	}
	
	public boolean getAccess(String verifier) throws UnknownHostException {
		
		TLog.i(TAG, "Verifier: {0}", verifier);
		
		// this method shouldn't have been called
		if (isAuthenticated())
			return false;
		
		if (!requestToken.equals("") && !requestTokenSecret.equals("")) {
			consumer.setTokenWithSecret(requestToken, requestTokenSecret);
			TLog.d(TAG, "Added request token {0} and request token secret {1}", requestToken, requestTokenSecret);
		}
		else
			return false;
		
		OAuthProvider provider = getProvider();
		
		try {
			provider.retrieveAccessToken(consumer, verifier);
		} catch (OAuthMessageSignerException e1) {
			e1.printStackTrace();
			return false;
		} catch (OAuthNotAuthorizedException e1) {
			e1.printStackTrace();
			return false;
		} catch (OAuthExpectationFailedException e1) {
			e1.printStackTrace();
			return false;
		} catch (OAuthCommunicationException e1) {
			e1.printStackTrace();
			return false;
		}
		
		// access has been granted, store the access token
		accessToken = consumer.getToken();
		accessTokenSecret = consumer.getTokenSecret();
		requestToken = "";
		requestTokenSecret = "";
		
		try {
			JSONObject response = new JSONObject(get(rootApi));
			TLog.d(TAG, "Request: {0}", rootApi);
		
			// append a slash to the url, else the signature will fail
			userApi = response.getJSONObject("user-ref").getString("api-ref");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		saveConfiguration();
		
		TLog.i(TAG, "Got access token {0}.", consumer.getToken());
		
		return true;
	}
	
	@Override
	public String get(String uri) throws java.net.UnknownHostException {
		
		// Prepare a request object
		HttpGet httpGet = new HttpGet(uri);
		httpGet.addHeader("X-Tomboy-Client", Tomdroid.HTTP_HEADER);
		sign(httpGet);
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
		sign(httpPut);
		
		// Do not handle redirects, we need to sign the request again as the old signature will be invalid
		HttpResponse response = execute(httpPut);
		return parseResponse(response);
	}
	
	public void saveConfiguration() {
		
		Preferences.putString(Preferences.Key.ACCESS_TOKEN, accessToken);
		Preferences.putString(Preferences.Key.ACCESS_TOKEN_SECRET, accessTokenSecret);
		Preferences.putString(Preferences.Key.ACCESS_TOKEN_URL, accessTokenUrl);
		Preferences.putString(Preferences.Key.REQUEST_TOKEN, requestToken);
		Preferences.putString(Preferences.Key.REQUEST_TOKEN_SECRET, requestTokenSecret);
		Preferences.putString(Preferences.Key.REQUEST_TOKEN_URL, requestTokenUrl);
		Preferences.putBoolean(Preferences.Key.OAUTH_10A, oauth10a);
		Preferences.putString(Preferences.Key.AUTHORIZE_URL, authorizeUrl);
		Preferences.putString(Preferences.Key.SYNC_SERVER_ROOT_API, rootApi);
		Preferences.putString(Preferences.Key.SYNC_SERVER_USER_API, userApi);
	}
}
