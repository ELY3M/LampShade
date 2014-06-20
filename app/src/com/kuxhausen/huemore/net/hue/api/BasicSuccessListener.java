package com.kuxhausen.huemore.net.hue.api;

import com.android.volley.Response.Listener;

public class BasicSuccessListener<T> implements Listener<T>{
	
	ConnectionMonitor parrent;

	public BasicSuccessListener(ConnectionMonitor parrentA){
		parrent = parrentA;
	}

	@Override
	public void onResponse(T response) {
		if(parrent!=null)
			parrent.setHubConnectionState(true);
	}	

}