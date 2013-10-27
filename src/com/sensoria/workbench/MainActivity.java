package com.sensoria.workbench;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	public final static String MESSAGE_CAPTURE_ID = "com.sensoria.workbench.MESSAGE_CAPTURE_ID";
	
	public final static String SETTINGS_FILE_KEY = "com.sensoria.workbench.SETTINGS";
	public final static String SETTINGS_CURRENT_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	public final static String SETTINGS_CURRENT_DEVICE_NAME = "DEVICE_NAME";
	public final static String SETTINGS_CURRENT_FILENAME_PREFIX = "FILENAME_PREFIX";
    public final static String SETTINGS_SUBJECT_NAME = "SUBJECT_NAME";
    public final static String SETTINGS_SHOES = "SHOES";
    public final static String SETTINGS_TERRAIN = "TERRAIN";
	
	public static String currentDeviceAddress;
	public static String currentDeviceName;
	public static String currentFilenamePrefix;
    public static String currentSubjectName;
    public static String currentShoes;
    public static String currentTerrain;
	
	/**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
     * derivative, which will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    // The {@link ViewPager} that will display the two sections of the app, one at a time.
    ViewPager mViewPager;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {        
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
                        
        // Create the adapter that will return a fragment for each of the three primary sections of the app
        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(), getResources());

        // Set up the action bar.		
		final ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.activity_main);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }	
	
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {        	
    	switch (requestCode) {
    	case DeviceActivity.DEVICE_SELECTION:
    		if (resultCode == Activity.RESULT_OK) {
    			currentDeviceAddress = data.getExtras().getString(DeviceActivity.EXTRA_DEVICE_ADDRESS);
    			currentDeviceName = data.getExtras().getString(DeviceActivity.EXTRA_DEVICE_NAME);
    			
    			SharedPreferences sharedPref = getSharedPreferences(SETTINGS_FILE_KEY, MODE_PRIVATE);    			
    			SharedPreferences.Editor editor = sharedPref.edit();
    			editor.putString(SETTINGS_CURRENT_DEVICE_ADDRESS, currentDeviceAddress);
    			editor.putString(SETTINGS_CURRENT_DEVICE_NAME, currentDeviceName);
    			editor.commit();
    			
    			View button = findViewById(R.id.button_start_capture);
    			TextView textStatus = (TextView) findViewById(R.id.text_status);
    			
                if (currentDeviceAddress.isEmpty()) {
                	textStatus.setText(R.string.status_no_device);
                	button.setEnabled(false);
                }
                else {
                	textStatus.setText(String.format(getString(R.string.status_yes_device), currentDeviceName));
                	button.setEnabled(true);
                }
            }
            else {
            }
			break;		
    	}		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.app_action_menu_device:
				// Called when Home (Up) is pressed in the action bar
				Intent activityIntent = new Intent(this, DeviceActivity.class);
				activityIntent.putExtra(DeviceActivity.EXTRA_DEVICE_NAME, (currentDeviceName.isEmpty() ? "<NONE>" : currentDeviceName));
				activityIntent.putExtra(DeviceActivity.EXTRA_DEVICE_ADDRESS, currentDeviceAddress);
				startActivityForResult(activityIntent, DeviceActivity.DEVICE_SELECTION);
				return true;
		}		
		return super.onOptionsItemSelected(item);
	}

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }
    
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {
    	Resources mResources;
    	
        public AppSectionsPagerAdapter(FragmentManager fm, Resources resources) {
            super(fm);
            mResources = resources;
        }
    	
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    // Session Data
                	return new CaptureDataSectionFragment(); 
                case 1:
                default:
                    // View Data
                	return new ViewDataSectionFragment();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	
        	return mResources.getStringArray(R.array.tabs_main_activity)[position];        	
        }
    }

    // Session Data Fragment Launcher
    public static class CaptureDataSectionFragment extends Fragment {

        private class CustomTextWatcher implements TextWatcher {
            private EditText mEditText;

            public CustomTextWatcher(EditText e) {
                mEditText = e;
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Noop
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
                // Noop
            }

            @Override
            public void afterTextChanged(Editable s) {
                switch (mEditText.getId()) {
                    case R.id.edit_file_prefix:
                        currentFilenamePrefix = saveControlChange(s, SETTINGS_CURRENT_FILENAME_PREFIX);
                        break;

                    case R.id.edit_subject:
                        currentSubjectName = saveControlChange(s, SETTINGS_SUBJECT_NAME);
                        break;

                    case R.id.edit_shoes:
                        currentShoes = saveControlChange(s, SETTINGS_SHOES);
                        break;

                    case R.id.edit_terrain:
                        currentTerrain = saveControlChange(s, SETTINGS_TERRAIN);
                        break;
                }
            }

            private String saveControlChange(Editable s, String settingsId) {
                String controlValue = s.toString();
                SharedPreferences sharedPref = getActivity().getSharedPreferences(SETTINGS_FILE_KEY, MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(settingsId, controlValue);
                editor.commit();
                return controlValue;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {        	
            View rootView = inflater.inflate(R.layout.fragment_section_capture_data, container, false);

            readSharedPreferences();

            View button = rootView.findViewById(R.id.button_start_capture);
            
            button.setOnClickListener(new View.OnClickListener() {		
					@Override
					public void onClick(View v) {						
						Toast.makeText(getActivity().getApplicationContext(), "Launching Session", Toast.LENGTH_SHORT).show();
						
						Intent activityIntent = new Intent(getActivity(), CaptureActivity.class);
						activityIntent.putExtra(CaptureActivity.EXTRA_DEVICE_ADDRESS, currentDeviceAddress);
						activityIntent.putExtra(CaptureActivity.EXTRA_FILENAME_PREFIX, currentFilenamePrefix);
                        activityIntent.putExtra(CaptureActivity.EXTRA_SUBJECT_NAME, currentSubjectName);
                        activityIntent.putExtra(CaptureActivity.EXTRA_SHOES, currentShoes);
                        activityIntent.putExtra(CaptureActivity.EXTRA_TERRAIN, currentTerrain);
						startActivity(activityIntent);
					}
				});


            EditText editPrefix = setEditTextDefault(rootView, R.id.edit_file_prefix, currentFilenamePrefix);
            EditText editSubject = setEditTextDefault(rootView, R.id.edit_subject, currentSubjectName);
            EditText editShoes = setEditTextDefault(rootView, R.id.edit_shoes, currentShoes);
            EditText editTerrain = setEditTextDefault(rootView, R.id.edit_terrain, currentTerrain);

            editPrefix.addTextChangedListener(new CustomTextWatcher(editPrefix));
            editSubject.addTextChangedListener(new CustomTextWatcher(editSubject));
            editShoes.addTextChangedListener(new CustomTextWatcher(editShoes));
            editTerrain.addTextChangedListener(new CustomTextWatcher(editTerrain));
            
            // This needs to happen after the the fragments have been loaded
            TextView textStatus = (TextView) rootView.findViewById(R.id.text_status);
            if (currentDeviceAddress.isEmpty()) {
            	textStatus.setText(R.string.status_no_device);
            	button.setEnabled(false);
            }
            else {
            	textStatus.setText(String.format(getString(R.string.status_yes_device), currentDeviceName));
            	button.setEnabled(true);
            }
            
            return rootView;
        }

        private void readSharedPreferences() {
            // Retrieve current device address from configuration
            SharedPreferences sharedPref = getActivity().getSharedPreferences(SETTINGS_FILE_KEY, MODE_PRIVATE);
            currentDeviceAddress = sharedPref.getString(SETTINGS_CURRENT_DEVICE_ADDRESS, "");
            currentDeviceName = sharedPref.getString(SETTINGS_CURRENT_DEVICE_NAME, "");
            currentFilenamePrefix = sharedPref.getString(SETTINGS_CURRENT_FILENAME_PREFIX, "");
            currentSubjectName = sharedPref.getString(SETTINGS_SUBJECT_NAME, "");
            currentShoes = sharedPref.getString(SETTINGS_SHOES, "");
            currentTerrain = sharedPref.getString(SETTINGS_TERRAIN, "");
        }
    }

    private static EditText setEditTextDefault(View rootView, int widget_id, String defaultValue) {
        View v = rootView.findViewById(widget_id);
        EditText editWidget = (EditText) rootView.findViewById(widget_id);
        if ((defaultValue != null) && !defaultValue.isEmpty()) {
            editWidget.setText(defaultValue);
        }
        return editWidget;
    }

    // View Data Fragment Launcher
    public static class ViewDataSectionFragment extends Fragment {

        @Override
        public void onResume() {
            updateFileList(getView().getRootView());

            super.onResume();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_section_view_data, container, false);

            return rootView;
        }

        private void updateFileList(View rootView) {
        	File dataDirectory;
        	
        	if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {        		
        		// External storage available
        		dataDirectory = new File(Environment.getExternalStorageDirectory() + "/com.sensoria.workbench");
        	} else {
        		// Use internal storage
        		dataDirectory = new File(getActivity().getFilesDir().getAbsolutePath());
        	}

            List<String> fileNamesList = new ArrayList<String>();

            // Directory does not exists the first time the app is run (or before first capture happens)
            if (dataDirectory.isDirectory()) {
	            for (File f: dataDirectory.listFiles()) {
	                fileNamesList.add(f.getName());
	            }
            }

            ArrayAdapter<String> fileListAdapter;
            fileListAdapter = new ArrayAdapter<String>(getActivity(), R.layout.item_template_data_capture_item, fileNamesList);

            final ListView listView = (ListView) rootView.findViewById(R.id.list_view_capture);
            listView.setAdapter(fileListAdapter);

            listView.setOnItemClickListener(new OnItemClickListener() {
            	public void onItemClick(AdapterView parent, View v, int position, long id) {
                    TextView tv = (TextView)v;
            		Intent activityIntent = new Intent(getActivity(), DataViewActivity.class);
            		activityIntent.putExtra(MainActivity.MESSAGE_CAPTURE_ID, tv.getText());
            		startActivity(activityIntent);
                }
			});
        }
    }
}