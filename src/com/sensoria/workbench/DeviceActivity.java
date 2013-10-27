package com.sensoria.workbench;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import java.util.Set;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class DeviceActivity extends Activity {
    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_DEVICE_NAME = "device_name";
    
    public static final int DEVICE_SELECTION = 991;

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
		}		
		return super.onOptionsItemSelected(item);
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);        
		setContentView(R.layout.activity_device);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();        
        Boolean currentFound = false;
        String currentAddress = extras.getString(EXTRA_DEVICE_ADDRESS);        
        
        // Initialize array adapter
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.item_template_device_name_item);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                if (currentAddress.equalsIgnoreCase(device.getAddress())) {
                	currentFound = true;
                }
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        
        // Find and set up the TextView for current device
        TextView textCurrent = (TextView) findViewById(R.id.text_current_device);
        if (currentFound) {
        	textCurrent.setText(extras.getString(EXTRA_DEVICE_NAME));
        } else {
        	textCurrent.setText(extras.getString(EXTRA_DEVICE_NAME) + "\nINVALID");
        }
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtra(EXTRA_DEVICE_NAME, info);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
}
