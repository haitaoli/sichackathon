package com.sensoria.webapi;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class GetSessionsTask extends AsyncTask<JSONObject, Boolean, ArrayList<Session> > {
    private WebServiceBase web = new WebServiceBase();

    @Override
    protected ArrayList<Session> doInBackground(JSONObject... params) {
        ArrayList<Session> result = new ArrayList<Session>();
        try {
            String url = web.baseUrl + "session";
            String response = web.sendGetRequest(url);
            JSONArray sessionsJson = new JSONArray(response);
            for (int i = 0; i < sessionsJson.length(); ++i) {
                Session c = Session.parseFromJson(sessionsJson.getJSONObject(i));
                result.add(c);
            }
        }
        catch(JSONException e) {
            return null;
        }

        return result;
    }
}
