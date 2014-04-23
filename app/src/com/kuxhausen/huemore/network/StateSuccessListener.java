package com.kuxhausen.huemore.network;

import com.android.volley.Response.Listener;
import com.kuxhausen.huemore.net.hue.HubConnection;
import com.kuxhausen.huemore.net.hue.PendingStateChange;
import com.kuxhausen.huemore.state.api.LightsPutResponse;

public class StateSuccessListener extends BasicSuccessListener<LightsPutResponse[]>{
	
	HubConnection mHubConnection;
	PendingStateChange mRequest;
	
	public StateSuccessListener(HubConnection hubConnection, PendingStateChange request){
		super(hubConnection);
		mHubConnection=hubConnection;
		mRequest=request;
	}

	@Override
	public void onResponse(LightsPutResponse[] response) {
		if(response.length>0 && response[0].success!=null){
			mHubConnection.reportStateChangeSucess(mRequest);
		} else {
			mHubConnection.reportStateChangeFailure(mRequest);
		}
	}	

}