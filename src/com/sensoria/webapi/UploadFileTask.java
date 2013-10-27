package com.sensoria.webapi;

import android.os.AsyncTask;

public class UploadFileTask extends AsyncTask<UploadFileRequest, Boolean, String> {
    private WebServiceBase web = new WebServiceBase();

    @Override
    protected String doInBackground(UploadFileRequest... params) {
        web.uploadFile(params[0].url, params[0].input);
        return "";
    }
}
