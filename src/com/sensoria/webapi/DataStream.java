package com.sensoria.webapi;

import org.json.JSONException;
import org.json.JSONObject;

public class DataStream {
    public int DataStreamType;
    public int DataStreamId;
    public String Location;
    public int SamplingPeriod;
    public int SourceId;

    public static DataStream parseFromJson(JSONObject json) {
        DataStream bd = new DataStream();
        try {
            bd.DataStreamType = json.getInt("DataStreamType");
            bd.DataStreamId = json.getInt("DataStreamId");
            bd.Location = json.getString("Location");
            bd.SamplingPeriod = json.getInt("SamplingPeriod");
            bd.SourceId = json.getInt("SourceId");
        }
        catch (JSONException e) {
            return null;
        }

        return  bd;
    }

    public JSONObject getJSON() {
        JSONObject object = new JSONObject();

        try {
            object.put("DataStreamType", DataStreamType);
            object.put("DataStreamId", DataStreamId);
            object.put("Location", Location);
            object.put("SamplingPeriod", SamplingPeriod);
            object.put("SourceId", SourceId);
        }
        catch (JSONException e) {
            return null;
        }

        return object;
    }
}
