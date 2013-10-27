package com.sensoria.webapi;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

public class AddSessionTask extends AsyncTask<JSONObject, Boolean, Session>{
    private WebServiceBase web = new WebServiceBase();

    @Override
    protected Session doInBackground(JSONObject... params) {
        Session result = null;

        try {
            String url = web.baseUrl + "session";
            String response = web.sendPostRequest(url, params[0]);
            JSONObject responseJson = new JSONObject(response);
            result = Session.parseFromJson(responseJson);
        }
        catch (JSONException e) {
            return null;
        }

        int x = result.SessionId;
        return result;
    }
}
