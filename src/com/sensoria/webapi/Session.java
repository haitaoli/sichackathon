package com.sensoria.webapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Session {
    public int SessionId;
    public int UserId;
    public int ShoesId;
    public String Notes;
    public ArrayList<Segment> Segments;

    public Session() {}

    public static Session parseFromJson(JSONObject json) {
        Session c = new Session();
        try {
            c.Segments = null;
            c.SessionId = json.getInt("SessionId");
            c.UserId = json.getInt("UserId");
            c.ShoesId = json.getInt("ShoesId");
            c.Notes = json.getString("Notes");

            if (json.has("Segments") && !json.isNull("Segments")) {
                JSONArray fragmentsJson = json.getJSONArray("Segments");
                c.Segments = new ArrayList<Segment>();
                for (int i = 0; i < fragmentsJson.length(); i++) {
                    c.Segments.add(Segment.parseFromJson(fragmentsJson.getJSONObject(i)));
                }
            }
        }
        catch (JSONException e) {
            return null;
        }

        return c;
    }

    public JSONObject getJSON() {
        JSONObject object = new JSONObject();

        try {
            object.put("SessionId", SessionId);
            object.put("UserId", UserId);
            object.put("ShoesId", ShoesId);
            object.put("Notes", Notes);
            if (Segments != null) {
                JSONArray fragmentsJson = new JSONArray();
                for (Segment cf : Segments) {
                    fragmentsJson.put(cf.getJSON());
                }

                object.put("Segments", fragmentsJson);
            }

        }
        catch (JSONException e) {
            return null;
        }

        return object;
    }
}
