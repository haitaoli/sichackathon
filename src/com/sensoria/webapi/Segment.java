package com.sensoria.webapi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Segment {
    public int SegmentId;
    public int SessionId;
    public ArrayList<DataStream> DataStreams;

    public static Segment parseFromJson(JSONObject json) {
        Segment cf = new Segment();

        try {
            cf.DataStreams = null;
            cf.SessionId = json.getInt("SessionId");
            cf.SegmentId = json.getInt("SegmentId");
            if (json.has("DataStreams") && !json.isNull("DataStreams")) {
                JSONArray blobsJson = json.getJSONArray("DataStreams");
                cf.DataStreams = new ArrayList<DataStream>();
                for (int i = 0; i < blobsJson.length(); i++) {
                    cf.DataStreams.add(DataStream.parseFromJson(blobsJson.getJSONObject(i)));
                }
            }
        }
        catch (JSONException e) {
            return null;
        }

        return cf;
    }

    public JSONObject getJSON() {
        JSONObject object = new JSONObject();

        try {
            object.put("SessionId", SessionId);
            object.put("SegmentId", SegmentId);
            if (DataStreams != null) {
                JSONArray blobs = new JSONArray();
                for (DataStream bd : DataStreams) {
                    blobs.put(bd.getJSON());
                }

                object.put("DataStreams", blobs);
            }
        }
        catch (JSONException e) {
            return null;
        }

        return object;
    }
}
