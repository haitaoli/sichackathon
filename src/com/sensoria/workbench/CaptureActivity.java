package com.sensoria.workbench;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;




import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;
import java.util.Random;

import com.sensoria.workbench.FootPressureMap;

import com.sensoria.signal.SharedObject;
import com.sensoria.signal.SMAQueue;
import com.sensoria.signal.COBSignalProcess;


public class CaptureActivity extends Activity implements SensorEventListener {
	// Debugging
    private static final String TAG = "CaptureActivity";
    private static final boolean D = false;
    
    // Extras
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_FILENAME_PREFIX = "filename_prefix";
    public static String EXTRA_SUBJECT_NAME = "subject_name";
    public static String EXTRA_SHOES = "shoes";
    public static String EXTRA_TERRAIN = "terrain";

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
    // Bluetooth communication objects
    private String mDeviceAddress;
    private String mConnectedDeviceName;
    private BluetoothAdapter mBluetoothAdapter = null;	
    private BluetoothAndromedaService mBluetoothService = null;
    
    // Storage
    private String mFilename;
    private File mFile;
    private File mLocationFile;
    private Writer mFileWriter;
    private Writer mLocationFileWriter;
    private int mCurrentTag = 0;

    // Accelerometer related variables
    private SensorManager mSensorManger;
    private Sensor mAccelerometer;

    // GPS tracking related variables
    private LocationManager mLocationManager;
    private Location mLocation;

    private Queue<SensorEvent> mAccelerationEvents = new ArrayDeque<SensorEvent>();

    // Franco's step counter
    private SMAQueue mySMAQueue = new SMAQueue();
	
    // Text Views & Edit Text
	TextView BobStepTV;
	TextView FrancoStepTV;
	TextView COBTV;
	TextView TagTV; // tag text view

	// Accelerometer
	TextView AccelX;
	TextView AccelY;
	TextView AccelZ;
		
	// Bob's step counter
	private COBSignalProcess signalProcess = new COBSignalProcess(HEEL_INDEX,MTB1_INDEX,MTB5_INDEX);
	private int watchDog = 0;	
	private int localCount = 0;  // read 5 data pts as it did in C#
    private SharedObject.Direction lastDirection = SharedObject.Direction.CENTER;
    
    // Receive remote Tag from another Android phone via WIFI
	private ServerSocket serverSocket;
	Handler updateConversationHandler;
	Thread serverThread = null;
	public static final int SERVERPORT = 4000;  // Port Number should match that in TagMe app which is a remote tag app by Bob
	
	private boolean sitting;
	
	//rest client talk to M2X
	private static RestClient restClient = new RestClient();
	
	////
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    private final Handler mBluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothAndromedaService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothAndromedaService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothAndromedaService.STATE_LISTEN:
                case BluetoothAndromedaService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                // Drop on the floor, no such thing for now in Andromeda scenarios
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mBuffer.append(readMessage);
                processData();
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    // Buffer for accumulated values from Bluetooth
    StringBuilder mBuffer = new StringBuilder();
    
    // Graphics
	private GraphView mGraphView;
	
	private GraphViewSeries mHeelSeries;
	private GraphViewSeries mMtb1Series;
	private GraphViewSeries mMtb5Series;
	
	private static final int HEEL_INDEX = 2;
	private static final int MTB1_INDEX = 1;
	private static final int MTB5_INDEX = 0;
	
	private Double mLastX = 10d;
	private Double mBaseX = 0d;
	private int mId = 0;
	private Double mCurrentX = 0d;
	private int mSampleCount = 0;
	private Boolean mPaused = false;
	private Boolean mNew = false;
	private Boolean mIsGraphVisible = true;
	private int mGraphLayout = 0;
	
	private Boolean mIsFootVisible = false;
	private FootPressureMap mFootPressureMap;
	
	private Boolean mIsCalibrationInProgress = false;
	double[] mMinCalibrationValues = new double[3];
	double[] mMaxCalibrationValues = new double[3];
	private int mCalibrationSequence;
	
    private double ax = 0;
    private double ay = 0;
    private double az = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_capture);
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_bluetooth_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Check address
        Bundle extras = getIntent().getExtras();
        mDeviceAddress = extras.getString(EXTRA_DEVICE_ADDRESS);
        if ((mDeviceAddress == null) || mDeviceAddress.isEmpty()) {
        	Toast.makeText(this, R.string.bt_device_not_specified, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
                   
		mHeelSeries = new GraphViewSeries(
				"Heel", 
				new GraphViewSeriesStyle(Color.BLUE, 3), 
				new GraphViewData[] {
					new GraphViewData(0, 0)
				});

		mMtb1Series = new GraphViewSeries(
				"MTB 1", 
				new GraphViewSeriesStyle(Color.RED, 3), 
				new GraphViewData[] {
					new GraphViewData(0, 0)
				});
		
		mMtb5Series = new GraphViewSeries(
				"MTB 5", 
				new GraphViewSeriesStyle(Color.GREEN, 3), 
				new GraphViewData[] {
					new GraphViewData(0, 0)
				});
		
		// graph with dynamically generated horizontal and vertical labels
		mGraphView = new LineGraphView(this, "Andromeda Session");
		GraphViewStyle style = new GraphViewStyle();
		style.setVerticalLabelsColor(Color.BLUE);
		style.setHorizontalLabelsColor(Color.BLUE);
		
		mGraphView.setGraphViewStyle(style);
		
		// add data
		mGraphView.addSeries(mHeelSeries);
		mGraphView.addSeries(mMtb1Series);
		mGraphView.addSeries(mMtb5Series);
		
		mGraphView.setViewPort(0, 10);
		mGraphView.setScalable(true);
		mGraphView.setScrollable(true);
		
		// Graph Layout
		LinearLayout layout = (LinearLayout) findViewById(R.id.capture_graph);
		layout.addView(mGraphView);

		// Foot Map
		mFootPressureMap = (FootPressureMap) findViewById(R.id.foot_canvas);
		turnOffFootPanel();
				
        // Setup new capture
        startNewCapture();  

        // Prepare graph toggle button
        View toggleGraphButton = findViewById(R.id.button_toggle_graph);        
        toggleGraphButton.setOnClickListener(new View.OnClickListener() {		
				@Override
				public void onClick(View v) {									
					if (mIsGraphVisible) {
						turnOffGraphPanel();
					} else {
						turnOnGraphPanel();						
						if (mIsFootVisible) {
							turnOffFootPanel();
						}
					}
				}
			});
        
        // Prepare foot toggle button
        View toggleFootButton = findViewById(R.id.button_toggle_foot);        
        toggleFootButton.setOnClickListener(new View.OnClickListener() {		
				@Override
				public void onClick(View v) {
					if (mIsFootVisible) {
						turnOffFootPanel();						
					} else {						
						turnOnFootPanel();						
						if (mIsGraphVisible) {
							turnOffGraphPanel();
						}
					}
				}
			});
        
        // Prepare foot swap button
        View footSwapButton = findViewById(R.id.button_foot_swap);
        footSwapButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
	            mFootPressureMap.swapFoot();				
			}
		});
        
        // Prepare foot calibrate button
        View calibrateButton = findViewById(R.id.button_foot_calibrate);
        calibrateButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				setupCalibrationSequence();
			}
		});
                
		// Prepare pause/resume button
        View pauseButton = findViewById(R.id.button_pause_resume_capture);        
        pauseButton.setOnClickListener(new View.OnClickListener() {		
				@Override
				public void onClick(View v) {
					mPaused = ! mPaused;
					((Button)v).setCompoundDrawablesWithIntrinsicBounds(mPaused ? R.drawable.ic_capture : R.drawable.ic_pause, 0, 0, 0);
				}
			});

        // Prepare new button
        View newButton = findViewById(R.id.button_new_capture);
        newButton.setOnClickListener(new View.OnClickListener() {		
				@Override
				public void onClick(View v) {
					mNew = true;
				}
			});
                                
        // Prepare tag buttons
        View button;
        
        OnClickListener tagClickListener = new View.OnClickListener() {			
			@Override
			
			public void onClick(View v) {
				TextView currentTagView = (TextView)findViewById(R.id.text_current_tag);
				currentTagView.setText("Current Tag: " + ((Button)v).getText());
				
				switch(v.getId()) {
					case R.id.button_sit_tag:
						mCurrentTag  = 1;
						break;
					case R.id.button_stand_tag:
						mCurrentTag  = 2;
						break;
					case R.id.button_walk_tag:
						mCurrentTag  = 4;
						break;
					case R.id.button_run_tag:
						mCurrentTag  = 8;
						break;
					case R.id.button_jump_tag:
						mCurrentTag  = 16;
						break;
					case R.id.button_bicyle_tag:
						mCurrentTag  = 32;
						break;
					case R.id.button_squat_tag:
						mCurrentTag  = 64;
						break;
                    case R.id.button_step_down_tag:
                        mCurrentTag = 128;
                        break;
                    case R.id.button_step_up_tag:
                        mCurrentTag = 256;
                        break;
                    case R.id.button_elliptical_tag:
                    	mCurrentTag = 512;
                    	break;
					case R.id.button_none_tag:
						mCurrentTag  = 0;
						break;
				}				
				
				//test
				Random x = new Random();
				mFootPressureMap.setSensors(x.nextInt(1024), x.nextInt(1024), x.nextInt(1024));
			}
		};
		
        button = findViewById(R.id.button_sit_tag);
        button.setOnClickListener(tagClickListener);
        
        button = findViewById(R.id.button_stand_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_walk_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_run_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_jump_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_bicyle_tag);
        button.setOnClickListener(tagClickListener);
        
        button = findViewById(R.id.button_squat_tag);
        button.setOnClickListener(tagClickListener);
        
        button = findViewById(R.id.button_none_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_step_down_tag);
        button.setOnClickListener(tagClickListener);

        button = findViewById(R.id.button_step_up_tag);
        button.setOnClickListener(tagClickListener);
        
        button = findViewById(R.id.button_elliptical_tag);
        button.setOnClickListener(tagClickListener);


        // Set up the accelerometer listener
        mSensorManger = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManger.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 0, locationListener);
	
        // set up step count edit texts
    	BobStepTV = (TextView) findViewById(R.id.BobView);
    	FrancoStepTV = (TextView) findViewById(R.id.FrancoView);
    	COBTV = (TextView) findViewById(R.id.COBView);
    	TagTV = (TextView) findViewById(R.id.TagNum);
    	
    	AccelX = (TextView) findViewById(R.id.accel_x);
    	AccelY = (TextView) findViewById(R.id.accel_y);
    	AccelZ = (TextView) findViewById(R.id.accel_z);
    	
    	// set up Wifi handler
		updateConversationHandler = new Handler();
		this.serverThread = new Thread(new ServerThread());
		this.serverThread.start();
	}
	
	////////     kill the socket when done //////////////
	@Override
	protected void onStop() {
		super.onStop();
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// ///////private classes for remote tagging /////////////////
	class ServerThread implements Runnable {
		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(SERVERPORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!Thread.currentThread().isInterrupted()) {
				try {
					socket = serverSocket.accept();
					CommunicationThread commThread = new CommunicationThread(socket);
					new Thread(commThread).start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class CommunicationThread implements Runnable {
		private Socket clientSocket;
		private BufferedReader input;
		public CommunicationThread(Socket clientSocket) {
			this.clientSocket = clientSocket;
			try {
				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {	
			while (!Thread.currentThread().isInterrupted()) {
				try {
					String read = input.readLine();
					updateConversationHandler.post(new updateUIThread(read));					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class updateUIThread implements Runnable {
		private String msg;
		public updateUIThread(String str) {
			this.msg = str;
		}
		@Override
		public void run() {
			mCurrentTag = Integer.parseInt(msg);	
			TagTV.setText("" + mCurrentTag);  // update the number
        	updateCurrentTag();
		}
	}
	////////////////////////// end of remote tagging private classes///////////////
	
	private void turnOnFootPanel() {
		GridLayout layout1 = (GridLayout) findViewById(R.id.capture_foot);
		GridLayout layout2 = (GridLayout) findViewById(R.id.capture_foot2);
		
		mIsFootVisible = true;
		
		// Get screen dimensions
		Display display = getWindowManager().getDefaultDisplay();
		
		Point size = new Point();
		display.getSize(size);
		
		ViewGroup.LayoutParams params = layout1.getLayoutParams();						
		
		//params.height = 325;
		// Setting the height as tall as the width of the screen, to get full foot display  
		params.height = size.x + 10;
		
		layout1.setLayoutParams(params);
		layout1.setVisibility(View.VISIBLE);
		
		params = layout2.getLayoutParams();

		params.height = (80 * size.x) / 300;
		layout2.setLayoutParams(params);
		layout2.setVisibility(View.VISIBLE);
	}
	
	private void turnOffFootPanel() {
		GridLayout layout1 = (GridLayout) findViewById(R.id.capture_foot);
		GridLayout layout2 = (GridLayout) findViewById(R.id.capture_foot2);
		
		mIsFootVisible = false;
		
		ViewGroup.LayoutParams params = layout1.getLayoutParams();
		params.height=0;
		layout1.setLayoutParams(params);
		layout1.setVisibility(View.INVISIBLE);
		
		params = layout2.getLayoutParams();
		params.height=0;
		layout2.setLayoutParams(params);
		layout2.setVisibility(View.INVISIBLE);		
	}
	
	private void turnOnGraphPanel() {		
		LinearLayout layout = (LinearLayout) findViewById(R.id.capture_graph);
		
		mIsGraphVisible = true;
		ViewGroup.LayoutParams params = layout.getLayoutParams();
		params.height=mGraphLayout;
		mGraphLayout = 0;
		layout.setLayoutParams(params);
		layout.setVisibility(View.VISIBLE);
	}
	
	private void turnOffGraphPanel() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.capture_graph);
		
		mIsGraphVisible = false;
		ViewGroup.LayoutParams params = layout.getLayoutParams();
		mGraphLayout = params.height;
		params.height=0;
		layout.setLayoutParams(params);
		layout.setVisibility(View.INVISIBLE);		
	}
		
	private void setupCalibrationSequence() {
		 
		 Toast.makeText(getApplicationContext(), "Calibration in progress\r\nRock your foot back and forth", Toast.LENGTH_LONG).show();  
		 
		 View calibrateButton = findViewById(R.id.button_foot_calibrate);
		 calibrateButton.setEnabled(false);
		 
		 mIsCalibrationInProgress = true;
         mCalibrationSequence = 0;

         for (int i = 0; i < 3; i++)
         {
             mMinCalibrationValues[i] = 1024;
             mMaxCalibrationValues[i] = 0;
         }				
	}
	
    private void finalizeCalibrationSequence() {        
        mFootPressureMap.setSensorCaps(
        		(int)mMinCalibrationValues[HEEL_INDEX],
        		(int)mMaxCalibrationValues[HEEL_INDEX],
        		(int)mMinCalibrationValues[MTB1_INDEX],
        		(int)mMaxCalibrationValues[MTB1_INDEX],
        		(int)mMinCalibrationValues[MTB5_INDEX],
        		(int)mMaxCalibrationValues[MTB5_INDEX]);

        mIsCalibrationInProgress = false;
        View calibrateButton = findViewById(R.id.button_foot_calibrate);
		calibrateButton.setEnabled(true);            
    }	
	
	private void startNewCapture() {
		
		if (mFilename != null)	{
			if (mFileWriter != null) {
				try {
					mFileWriter.close();
				} catch (IOException e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
				}
				mFileWriter = null;
			}

            if (mLocationFileWriter != null) {
                try {
                    mLocationFileWriter.close();
                } catch (IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
                mLocationFileWriter = null;
            }
			
			mFilename = null;
			mFile = null;
            mLocationFile = null;
		}

		Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-hhmmss");
        
    	Bundle extras = getIntent().getExtras();
        String prefix = extras.getString(EXTRA_FILENAME_PREFIX);
        
        if (prefix == null) {
        	mFilename = formatter.format(now).concat(".csv"); 
        } else {
        	mFilename = prefix.concat("-").concat(formatter.format(now)).concat(".csv");
        }
        
        try {
        	String basePath;
        	
        	if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {        		
        		// External storage available
        		basePath = Environment.getExternalStorageDirectory() + "/" + getPackageName() + "/";
        	} else {
        		// Use internal storage
        		basePath = getFilesDir().getAbsolutePath() + "/";
        	}
        		
	        mFile = new File(basePath + mFilename);
            mLocationFile = new File(basePath + mFilename + ".location");

            File dirs = new File(mFile.getParent());
		        
	        if (!dirs.exists()) {
	        	dirs.mkdirs();
	        }

	        mFile.createNewFile();
		    mFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mFile), "utf-8"));
		        
		    mLocationFile.createNewFile();
	        mLocationFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mLocationFile), "utf-8"));

        	//} else {
        		// External storage not available >> use internal storage
        				        
		    //    mFileWriter = new BufferedWriter(new OutputStreamWriter(openFileOutput(mFilename, Context.MODE_PRIVATE), "utf-8"));
	        //    mLocationFileWriter = new BufferedWriter(new OutputStreamWriter(openFileOutput(mFilename + ".location", Context.MODE_PRIVATE), "utf-8"));
        //	}
        	
	        mFileWriter.write("Andromeda Session from Android");
	        mFileWriter.write("Session for: " + prefix + "\r\n");
	        mFileWriter.write("Tags: 0:None - 1:Sit - 2:Stand - 4:Walk - 8:Run - 16:Jump - 32:Bicyle - 64:Squat - 128:StepDown - 256:StepUp - 512:Elliptical\r\n");
	        mFileWriter.write("Subject:" + extras.getString(EXTRA_SUBJECT_NAME, "") + ", Shoes:" + extras.getString(EXTRA_SHOES, "") + ", Terrain:" + extras.getString(EXTRA_TERRAIN, "") + "\r\n");
            mFileWriter.write("\r\n");
            
            // Create header for the CSV file	        
	        mFileWriter.write("Tag,Id,T,S0,S1,S2,AX,AY,AZ\r\n");

        		
        }
        catch (IOException e)
        {
        	Toast.makeText(this, R.string.storage_unable_to_write, Toast.LENGTH_LONG).show();
        	mFileWriter = null;
        	mLocationFileWriter = null;
        }
               
        
        mHeelSeries.resetData(new GraphViewData[] { new GraphViewData(0.0, 0.0) });
        mMtb1Series.resetData(new GraphViewData[] { new GraphViewData(0.0, 0.0) });
        mMtb5Series.resetData(new GraphViewData[] { new GraphViewData(0.0, 0.0) });
        
        mBaseX = 0.0;
        mId = 0;
    	mLastX = 10d;
    	mCurrentX = 0d;
    	mSampleCount = 0;
	}

	@Override
	public void onStart() {
		super.onStart();
		
		// If BT is not on, request that it be enabled.
        // setupConnection() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
        	setupConnection();
        }
	}
	
    @Override
    public synchronized void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothAndromedaService.STATE_NONE) {
              // Start the Bluetooth chat services
              mBluetoothService.start();
            }
        }

        mSensorManger.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// Called when Home (Up) is pressed in the action bar			
				closeDataCollection();				
				Intent parentActivityIntent = new Intent(this, MainActivity.class);
				parentActivityIntent.addFlags(
						Intent.FLAG_ACTIVITY_CLEAR_TOP |
						Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(parentActivityIntent);
				finish();
				return true;
		}		
		return super.onOptionsItemSelected(item);
	}	

	private void closeDataCollection() {
        if (mBluetoothService != null) {
        	mBluetoothService.stop();
        	mBluetoothService = null;
        }
        
        // Close the file
        if (mFileWriter != null) {
			try {
				mFileWriter.close();
				mFileWriter = null;
                mLocationFileWriter.close();
                mLocationFileWriter = null;
			} catch (IOException e) {
				// Eat the exception
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

        mSensorManger.unregisterListener(this);
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeDataCollection();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {        	
    	switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                setupConnection();
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, R.string.bt_bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
                finish();
            }			
    	}
    }
    
    private void setupConnection() {
    	if (mBluetoothService == null) {
	    	BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
	    	
	    	mBluetoothService = new BluetoothAndromedaService(this, mBluetoothHandler);       
	    	mBluetoothService.connect(device, true);
    	}
    }
    
    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mAccelerationEvents.add(event);
    }
    
    private void processData() {
    	int position = mBuffer.indexOf("\n");
    	if (position >= 0) {
    		// A full row is available    		
    		try {
    			if (mNew)
    			{
    				startNewCapture();
    				mNew = false;    				
    			}
    			
    			if (! mPaused) {
	    			String row = mBuffer.substring(0, position);
	    			if(D) Log.i(TAG, "row data: " + row);
	    			String[] rows = row.split(","); 
	    			Long millis = Long.parseLong(rows[0].trim());
	    			
	    			Integer[] sensors = new Integer[3];
	    			Boolean s = false;
	    			int pressure = 0;
	    			
	    			for (int i = 0; i < 3; i++) {
	    				if ((i < 2) || (rows.length > 3)) {
	    					sensors[i] = Integer.parseInt(rows[i + 1].trim());
	    					if (sensors[i] > 500) {
	    						s = true;
	    						pressure = 1;
	    		    			restClient.httpPush(pressure);
	    					}
	    				}
	    			}
	    			
	    			
	    			if (s != sitting) {
		    			ImageView statusImageView = (ImageView) findViewById(R.id.statusImageView);
	    				statusImageView.setImageResource(s ? R.drawable.sit: R.drawable.stand);
	    				sitting = s;
	    			}
	    			
                    mSampleCount++;
                    //only send 1 out 50
                    if(mSampleCount % 50 == 0)
                    	restClient.httpPush(pressure);
                    
	    			// Only plot 1 out of 5 samples
	    			if (mSampleCount % 5 == 0) {
	    				//Reset graph every minute
                        if (mSampleCount % (1000 / 40 * 60) == 0) {
                            mHeelSeries.resetData(new GraphViewData[] { new GraphViewData(0, (double)sensors[HEEL_INDEX]) });
                            mMtb1Series.resetData(new GraphViewData[] { new GraphViewData(0, (double)sensors[MTB1_INDEX]) });
                            mMtb5Series.resetData(new GraphViewData[] { new GraphViewData(0, (double)sensors[MTB5_INDEX]) });
                            mCurrentX = 0.0;
                        } else {
                            mHeelSeries.appendData(new GraphViewData(mCurrentX, (double)sensors[HEEL_INDEX]), false);
                            mMtb1Series.appendData(new GraphViewData(mCurrentX, (double)sensors[MTB1_INDEX]), false);
                            mMtb5Series.appendData(new GraphViewData(mCurrentX, (double)sensors[MTB5_INDEX]), true);
                        }
                        
                        if(D) Log.i(TAG, "S0:" + sensors[0].toString() + ", S1:" + sensors[1].toString() + ", S2:" + sensors[2].toString() + ", x:" + mCurrentX);
	    			}

                    if (mAccelerationEvents.size() > 0) {
                    	ax = 0;
                    	ay = 0;
                    	az = 0;
                        int count = 0;
                        SensorEvent se;
                        while ((se = mAccelerationEvents.poll()) != null) {
                            ax += se.values[0];
                            ay += se.values[1];
                            az += se.values[2];
                            count++;
                        }

                        ax /= count;
                        ay /= count;
                        az /= count;
                    }
                    
                    // Sample the GPS once a second, or every 25th cycle
                    if (mSampleCount % 25 == 0) {
                        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    }
                    
                    // Refresh the Accelerometer display 5 times a second
                    if (mSampleCount % 5 == 0) {
                        AccelX.setText(String.format("%.3f", ax));
                        AccelY.setText(String.format("%.3f", ay));
                        AccelZ.setText(String.format("%.3f", az));
                    }
                    
                    // Manage Pressure Map                    
                    if (mIsCalibrationInProgress) {
                        // The calibration lasts 5 seconds
                        ++ mCalibrationSequence;

                        // Update Min/Max for the sensors
                        for (int i = 0; i < 3; i++)
                        {
                            if (mMaxCalibrationValues[i] < sensors[i])
                            {
                                mMaxCalibrationValues[i] = sensors[i];
                            }

                            if (mMinCalibrationValues[i] > sensors[i])
                            {
                                mMinCalibrationValues[i] = sensors[i];
                            }
                        }

                        // 25 Hz = Sampling Rate >> 5 seconds for calibration
                        if (mCalibrationSequence > 25 * 5) {	
                            finalizeCalibrationSequence();
                        }
                    }
                    else {
                    	// Update foot map only 5 times a second, and only if the foot is visible
                    	if (mIsFootVisible && (mSampleCount % 3 == 0)) {
	                    	mFootPressureMap.setSensors(sensors[HEEL_INDEX], sensors[MTB1_INDEX], sensors[MTB5_INDEX]);
                    	}
                    	
                    	 //// ===============bob's step count ==================
                        if (localCount < SharedObject.BATCH_SIZE) {
                        	signalProcess.readData(sensors);
                        	localCount++;
                        } else {                        
	                        watchDog++;
	                        SharedObject.Direction currDirection = signalProcess.getMove();
	                        String dirText = "" + currDirection;
	                        COBTV.setText("" + dirText.charAt(0));  // initial letter

	                        if (watchDog < 10)
	                        {
	                            if (currDirection == SharedObject.Direction.NORTH &&
	                            (lastDirection == SharedObject.Direction.FOOT_UP))
	                            {
	                            	int newSteps = Integer.parseInt(BobStepTV.getText().toString()) + 1;
	                            	BobStepTV.setText("" + newSteps);  
	                                lastDirection = SharedObject.Direction.CENTER;
	                                watchDog = 0;
	                            }
	                        }
	                        else if (watchDog > 10)
	                        {  // watch dog reset last direction 
	                        	lastDirection = SharedObject.Direction.CENTER;
	                            watchDog = 0;   
	                        }
	                        if (currDirection == SharedObject.Direction.FOOT_UP && lastDirection != SharedObject.Direction.FOOT_UP)
	                        {
	                            lastDirection = SharedObject.Direction.FOOT_UP;
	                        }
	                        localCount = 0;
                        }
                    	// ======Franco's Step Count=========
                      	int step = mySMAQueue.processData(sensors);
                      	
                    	if (step > 0){
                    		int newSteps = Integer.parseInt(FrancoStepTV.getText().toString()) + 1;
                    		
                    		FrancoStepTV.setText("" + newSteps);                    		
                    	}
                    	TagTV.setText("" + mCurrentTag);  // update the number
                    	updateCurrentTag();
                    }
                    
                   
	    			// Write to storage
	    			// Rebuild a string with fixed millis and adding tags
                    if (mFileWriter != null) {
		    			String newRow;
	                    newRow = String.format("%d,%d,%s,%d,%d,%d,%f,%f,%f\r\n", mCurrentTag, mId, mBaseX, sensors[0], sensors[1], sensors[2], ax, ay, az);
	                    mFileWriter.write(newRow);
	
	                    if (mSampleCount %25 == 0 && mLocation != null) {
	                        newRow = String.format("%d,%f,%f\r\n", mBaseX, mLocation.getLongitude(), mLocation.getLatitude());
	                        mLocationFileWriter.write(newRow);
	                    }
                    }

	    			// Move to next sample
	    			mBaseX += 40;
                    mCurrentX += 40.0/1000.0;
	    			++ mId;
    			}
    			
    		}
    		catch (Exception e)
    		{
    			// Bad or partial data, ignore 
    			if(D) Log.i(TAG, "Ignoring bad data: " + e.getMessage());
    		}
    		finally {
    			// 
    			mBuffer.delete(0, position+1);
    		}
    	}
    }

	private void updateCurrentTag() {
		// TODO Auto-generated method stub
     	TextView currentTagView = (TextView)findViewById(R.id.text_current_tag);
     	switch(mCurrentTag){
     		case 0: currentTagView.setText("Current Tag: [None]"); break;
     		case 1: currentTagView.setText("Current Tag: [Sit]"); break;
     		case 2: currentTagView.setText("Current Tag: [Stand]"); break;
     		case 4: currentTagView.setText("Current Tag: [Walk]"); break;
     		case 8: currentTagView.setText("Current Tag: [Run]"); break;
     		case 16: currentTagView.setText("Current Tag: [Jump]"); break;
     		case 32: currentTagView.setText("Current Tag: [Bicycle]"); break;
     		case 64: currentTagView.setText("Current Tag: [Squat]"); break;
     		case 128: currentTagView.setText("Current Tag: [Step Dn]"); break;
     		case 256: currentTagView.setText("Current Tag: [Step Up]"); break;
     		case 512: currentTagView.setText("Current Tag: [Elliptical]"); break;
     	}
	}
}
