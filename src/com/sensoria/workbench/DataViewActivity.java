package com.sensoria.workbench;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.sensoria.webapi.AddSessionTask;
import com.sensoria.webapi.DataStream;
import com.sensoria.webapi.DataStreamHeader;
import com.sensoria.webapi.Segment;
import com.sensoria.webapi.Session;
import com.sensoria.webapi.UploadFileRequest;
import com.sensoria.webapi.UploadFileTask;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static android.app.AlertDialog.Builder;

public class DataViewActivity extends Activity {

    private String legend[] = {"Heel", "MTB 1", "MTB 2"};
    private int colors[] = {Color.BLUE, Color.RED, Color.GREEN};
    private GraphViewSeries[] series;
    private int seriesSize = 0;
    private String fileName;

    private String getBasePath() {
	if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
	    return Environment.getExternalStorageDirectory() + "/com.sensoria.workbench/";
	} else {
	    return getFilesDir().getAbsolutePath() + "/";
	}
    }

    private void clickDeleteFile() {
        Builder builder = new Builder(this);
        builder.setTitle(R.string.warning_alert_title);
        String messageText = getString(R.string.delete_file_confirmation) + fileName + "?";
        builder.setMessage(messageText);
        builder.setPositiveButton(R.string.yes_answer, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File fileToDelete = new File(getBasePath() + fileName);
                fileToDelete.delete();
                dialog.cancel();
                onBackPressed();
            }
        });

        builder.setNegativeButton(R.string.no_answer, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void writeHeader(DataStreamHeader header, ByteArrayOutputStream stream) throws IOException {
        DataOutputStream dataStream = new DataOutputStream(stream);
        header.writeHeader(dataStream);
    }

    private void uploadFile(String fileName) throws IOException {
        final int bitsInUse = 8;
        final int sensoriaSamplingRate = 40;
        final int gpsSamplingRate = 1000;
        final int accelSamplingRate = 40;

        SensoriaReaderCsv readerCsv = new SensoriaReaderCsv(fileName);
        ByteArrayOutputStream sensoriaOutputStream = new ByteArrayOutputStream();
        BitStreamWriter writer = new BitStreamWriter(sensoriaOutputStream);
        ByteArrayOutputStream gpsOutputStream = new ByteArrayOutputStream();
        DataOutputStream gpsWriter = new DataOutputStream(gpsOutputStream);
        ByteArrayOutputStream accelOutputStream = new ByteArrayOutputStream();
        DataOutputStream accelWriter = new DataOutputStream(accelOutputStream);

        // Create the stream headers
        DataStreamHeader sensoriaHeader = new DataStreamHeader();
        sensoriaHeader.SamplingPeriod = sensoriaSamplingRate;
        DataStreamHeader gpsHeader = new DataStreamHeader();
        gpsHeader.SamplingPeriod = gpsSamplingRate;
        DataStreamHeader accelHeader = new DataStreamHeader();
        accelHeader.SamplingPeriod = accelSamplingRate;

        sensoriaHeader.writeHeader(writer);
        gpsHeader.writeHeader(gpsWriter);
        accelHeader.writeHeader(accelWriter);

        int samplesCounter = 0;
        float lastLat = 0.0f;
        float lastLon = 0.0f;
        while (readerCsv.hasNext()) {
            SensoriaDataEntry entry = readerCsv.nextEntry();
            if (entry.lon != 0.0f || entry.lat != 0.0f) {
                lastLat = entry.lat;
                lastLon = entry.lon;
            }

            writer.write(entry.s0, bitsInUse);
            writer.write(entry.s1, bitsInUse);
            writer.write(entry.s2, bitsInUse);

            if (samplesCounter % (gpsSamplingRate / sensoriaSamplingRate) == 0) {
                gpsWriter.writeFloat(lastLat);
                gpsWriter.writeFloat(lastLon);
            }

            accelWriter.writeFloat(entry.ax);
            accelWriter.writeFloat(entry.ay);
            accelWriter.writeFloat(entry.az);
            samplesCounter++;
        }

        // Update the headers with the samples count
        sensoriaHeader.SamplesCount = samplesCounter;
        gpsHeader.SamplesCount = samplesCounter / 25;
        accelHeader.SamplesCount = samplesCounter;

        // Rewrite the headers
        writeHeader(gpsHeader, gpsOutputStream);
        writeHeader(accelHeader, accelOutputStream);
        writeHeader(sensoriaHeader, sensoriaOutputStream);

        Session newCapture = new Session();
        newCapture.Notes = "Uploaded form Android";
        newCapture.ShoesId = 1;
        newCapture.UserId = 1;
        newCapture.Segments = new ArrayList<Segment>();

        Segment cf = new Segment();
        cf.DataStreams = new ArrayList<DataStream>();

        DataStream blobSensoria = new DataStream();
        blobSensoria.SourceId = 1;
        blobSensoria.SamplingPeriod = sensoriaSamplingRate;
        blobSensoria.DataStreamType = 1;

        DataStream blobAcceleration = new DataStream();
        blobAcceleration.SourceId = 1;
        blobAcceleration.SamplingPeriod = accelSamplingRate;
        blobAcceleration.DataStreamType = 1;

        DataStream blobGPS = new DataStream();
        blobGPS.SourceId = 1;
        blobGPS.SamplingPeriod = gpsSamplingRate;
        blobGPS.DataStreamType = 1;

        cf.DataStreams.add(blobSensoria);
        cf.DataStreams.add(blobAcceleration);
        cf.DataStreams.add(blobGPS);

        try {
            newCapture.Segments.add(cf);
            AddSessionTask addCapture = new AddSessionTask();
            addCapture.execute(newCapture.getJSON());
            newCapture = addCapture.get();

            UploadFileTask uploadSensoria = new UploadFileTask();
            UploadFileRequest uploadSensoriaRequest = new UploadFileRequest();
            uploadSensoriaRequest.url = newCapture.Segments.get(0).DataStreams.get(0).Location;
            uploadSensoriaRequest.input = new ByteArrayInputStream(writer.toByteArray());
            uploadSensoria.execute(uploadSensoriaRequest);

            UploadFileTask uploadGPS = new UploadFileTask();
            UploadFileRequest uploadGPSRequest = new UploadFileRequest();
            uploadGPSRequest.url = newCapture.Segments.get(0).DataStreams.get(1).Location;
            uploadGPSRequest.input = new ByteArrayInputStream(gpsOutputStream.toByteArray());
            uploadGPS.execute(uploadGPSRequest);

            UploadFileTask uploadAccel = new UploadFileTask();
            UploadFileRequest uploadAccelRequest = new UploadFileRequest();
            uploadAccelRequest.url = newCapture.Segments.get(0).DataStreams.get(2).Location;
            uploadAccelRequest.input = new ByteArrayInputStream(accelOutputStream.toByteArray());
            uploadAccel.execute(uploadAccelRequest);

            Builder builder = new Builder(this);
            builder.setTitle(R.string.finished_alert_title);
            builder.setMessage(R.string.finished_upload_text);
            builder.setNeutralButton(R.string.ok_answer, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void clickUploadFile() {
        try {
            uploadFile(getBasePath() + fileName);
        }
        catch (IOException e) {
            Builder builder = new Builder(this);
            builder.setTitle(R.string.warning_alert_title);
            builder.setMessage(R.string.not_implemented_yet);
            builder.setNeutralButton(R.string.ok_answer, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    private void readDataFromFile(String fileName) {
        series = new GraphViewSeries[3];

        try {
            ArrayList[] dataArrays = new ArrayList[3];
            dataArrays[0] = new ArrayList();
            dataArrays[1] = new ArrayList();
            dataArrays[2] = new ArrayList();

            BufferedReader br = new BufferedReader(new FileReader(getBasePath() + fileName));
            String currentLine;

            // Current format as 5 leading lines of extra information
            // We could parse them and do some extra verification here
            currentLine = br.readLine();
            currentLine = br.readLine();
            currentLine = br.readLine();
            currentLine = br.readLine();
            currentLine = br.readLine();

            while ((currentLine = br.readLine()) != null) {
                String[] parts = currentLine.split(",");
                if (parts.length > 3) {
                    dataArrays[0].add(Float.parseFloat(parts[3]));
                    dataArrays[1].add(Float.parseFloat(parts[4]));
                    dataArrays[2].add(Float.parseFloat(parts[5]));
                }
            }

            br.close();
            for (int i = 0; i < 3; i++) {
                GraphViewData[] data = new GraphViewData[dataArrays[i].size()];
                for (int j = 0; j < data.length; j++) {
                    data[j] = new GraphViewData(j, (Float)dataArrays[i].get(j));
                }
                series[i] = new GraphViewSeries(legend[i], new GraphViewSeriesStyle(colors[i], 3), data);
            }

            seriesSize = dataArrays[0].size();

        } catch (Exception e)
        {
        }
    }

    // Demo code from early prototype - let's keep it around for now, just in case.

    private void generateSampleData() {
        // TEST: draw random curves
        int num = 300;
        GraphViewData[] data;
        double v;

        GraphViewSeries[] generatedData = new GraphViewSeries[3];

        data = new GraphViewData[num];
        v=0;
        for (int i=0; i<num; i++) {
            v += 0.2;
            data[i] = new GraphViewData(i, Math.sin(v));
        }

        generatedData[0] = new GraphViewSeries("Heel", new GraphViewSeriesStyle(Color.BLUE, 3), data);

        data = new GraphViewData[num];
        v = 0;
        for (int i=0; i<num; i++) {
            v += 0.2;
            data[i] = new GraphViewData(i, Math.cos(v));
        }

        generatedData[1] = new GraphViewSeries("MTB 1", new GraphViewSeriesStyle(Color.GREEN, 3), data);

        data = new GraphViewData[num];
        v = 0;
        for (int i=0; i<num; i++) {
            v += 0.2;
            data[i] = new GraphViewData(i, Math.sin(Math.random()*v));
        }

        generatedData[2] = new GraphViewSeries("MTB 2", new GraphViewSeriesStyle(Color.RED, 3), data);

        series = generatedData;
        seriesSize = num;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_data_view);
		
		Intent intent = getIntent();
		String captureId = intent.getStringExtra(MainActivity.MESSAGE_CAPTURE_ID);
        fileName = captureId;
		
		// Temp: when receiving an actual capture id, we will have to get the title from the stored blob instead
		setTitle(captureId);

		// graph with dynamically generated horizontal and vertical labels
		GraphView graphView = new LineGraphView(this, "test");		
		GraphViewStyle style = new GraphViewStyle();
		style.setVerticalLabelsColor(Color.BLUE);
		style.setHorizontalLabelsColor(Color.BLUE);
		
		graphView.setGraphViewStyle(style);

        readDataFromFile(captureId); //generateSampleData();
        if (series.length != 3) {
            throw new Error("Unexpected data series.");
        }

		// add data
		graphView.addSeries(series[0]);
		graphView.addSeries(series[1]);
		graphView.addSeries(series[2]);
		
		graphView.setViewPort(0, seriesSize);
		graphView.setScalable(true);
		graphView.setScrollable(true);
		
//		graphView.setShowLegend(true);
//		graphView.setLegendAlign(LegendAlign.BOTTOM);
//		graphView.setLegendWidth(200);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.data_view_graph);
		layout.addView(graphView);

        View.OnClickListener tagClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId()) {
                    case R.id.delete_file:
                        clickDeleteFile();
                        break;
                    case R.id.upload_file:
                        clickUploadFile();
                        break;
                }
            }
        };

        View button;
        button = findViewById(R.id.delete_file);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.upload_file);
        button.setOnClickListener(tagClickListener);
    }
		
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// Called when Home (Up) is pressed in the action bar
				Intent parentActivityIntent = new Intent(this, MainActivity.class);
				NavUtils.navigateUpTo(this, parentActivityIntent);
				parentActivityIntent.addFlags(
						Intent.FLAG_ACTIVITY_CLEAR_TOP |
						Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(parentActivityIntent);
				finish();
				return true;
		}		
		return super.onOptionsItemSelected(item);
	}
}

