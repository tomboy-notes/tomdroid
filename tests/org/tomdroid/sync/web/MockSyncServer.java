package org.tomdroid.sync.web;

import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

public class MockSyncServer extends SyncServer {

	public MockSyncServer() throws UnknownHostException, JSONException {
		JSONObject mockedResponse = new JSONObject("{'user-name':'<in reality here comes a http address>',"
				+ "'notes-ref':{'api-ref':'https://one.ubuntu.com/notes/api/1.0/op/',"
				+ "'href':'https://one.ubuntu.com/notes/'}," + "'current-sync-guid':'0',"
				+ "'last-name':'Mustermann','first-name':'Max','latest-sync-revision':1}");
		
		readMetaData(mockedResponse);
	}

}
