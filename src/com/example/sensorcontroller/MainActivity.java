package com.example.sensorcontroller;

import java.util.Locale;

import android.R.string;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import android.widget.ProgressBar;

public class MainActivity extends Activity implements ActionBar.TabListener {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	
	public static BleDeviceController mBleDeviceController;
	public static DeviceAdapter mDeviceAdapter;
	
	public static Button scanButton;
	public static Button startButton;
	public static Button applyButton;	
	public static ProgressBar scanProgressBar;

	public static Handler mWidgetHandler;

	public static ArrayAdapter<String> accScaleAdapter;
	public static ArrayAdapter<String> gyroScaleAdapter;
	public static ArrayAdapter<String> samplingRateAdapter;

    private static final int REQUEST_ENABLE_BT = 2;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Initialize instances
		initialize();
		
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
		
		if(!mBleDeviceController.isBluetoothEnabled())
		{
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}		
		
	}
	
	public void initialize(){
		mBleDeviceController = new BleDeviceController(this);
		mDeviceAdapter = new DeviceAdapter(this);		
		
		mWidgetHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				switch(msg.what){
				case 0:
						
					//Disable all button, show the progress bar
					scanButton.setEnabled(false);
					startButton.setEnabled(false);
					applyButton.setEnabled(false);
					scanProgressBar.setVisibility(android.view.View.VISIBLE);
					
					break;
					
				case 1:
					//Enable all button, hide the progress bar
					scanButton.setEnabled(true);
					startButton.setEnabled(true);
					applyButton.setEnabled(true);
					scanProgressBar.setVisibility(android.view.View.INVISIBLE);
					break;
					
				case 2:
					
					//Enable only stop button, hide the progress bar
					startButton.setEnabled(true);
					scanProgressBar.setVisibility(android.view.View.INVISIBLE);
					break;					
				}
				
			}
		};
		
		String[] level = {"Normal","Max","Min"};
		accScaleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, level);
		gyroScaleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, level);
		samplingRateAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, level);

	}
	
	
	
	public static void refreshFragment(int section){
		switch(section){
		case 0:
			//DeviceList update
			mDeviceAdapter.notifyDataSetChanged();
			break;
		case 1:
			break;
		}
	}
	
	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵
	            ConfirmExit(); //呼叫ConfirmExit()函數
	            return true;  
	     }  
	        return super.onKeyDown(keyCode, event);  
	  }

	   public void ConfirmExit(){

	        AlertDialog.Builder ad=new AlertDialog.Builder(MainActivity.this); //創建訊息方塊

	        ad.setTitle("離開");
	        ad.setMessage("確定要離開?");
	        ad.setPositiveButton("是", new DialogInterface.OnClickListener() { //按"是",則退出應用程式
	            public void onClick(DialogInterface dialog, int i) {
	              mBleDeviceController.close();
	              MainActivity.this.finish();//關閉activity
	       }
	     });

	        ad.setNegativeButton("否",new DialogInterface.OnClickListener() { //按"否",則不執行任何操作
	            public void onClick(DialogInterface dialog, int i) {
	       }
	     });

	        ad.show();//顯示訊息視窗
	  }


	   
	   
	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			
			Fragment fragment;
			
			if(position == 0)
			{
				fragment = new ControllerFragment();
				return fragment;
			}
			else if(position == 1)
			{
				fragment = new RecordListFragment();
				return fragment;
			}
			else
				return null;
		}

		@Override
		public int getCount() {
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			}
			return null;
		}
	}
	
}
