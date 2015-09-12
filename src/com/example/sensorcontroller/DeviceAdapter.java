package com.example.sensorcontroller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DeviceAdapter extends BaseAdapter{
	
	LayoutInflater mLayoutInflater;
	
	public DeviceAdapter(Context context){
		mLayoutInflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return BleDeviceController.MAX_DEVICE_NUMBER;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		ViewGroup vg;
		if(convertView != null){
			vg = (ViewGroup) convertView;
		}else{
			vg = (ViewGroup) mLayoutInflater.inflate(R.layout.device_listitem, null);
		}
		
		TextView devNameText  = (TextView) vg.findViewById(R.id.txt_device_name); 
		TextView connStatusText  = (TextView) vg.findViewById(R.id.txt_conn_status);
		ProgressBar batteryLvl  = (ProgressBar) vg.findViewById(R.id.battery_level);
		TextView recordNumText  = (TextView) vg.findViewById(R.id.txt_file_num); 
		BleDeviceController.BleDevice device = MainActivity.mBleDeviceController.getDevice(position);
		
		devNameText.setText(device.devName);
		switch(device.status){
		case CONNECTED:
			connStatusText.setText("Connected");
			break;
			
		case DISCONNECTED:
			connStatusText.setText("Disonnected");
			break;
			
		case OFFLINE:
			connStatusText.setText("Offline");
			break;
		}
		
		batteryLvl.setProgress(device.batteryLevel);
		recordNumText.setText(device.recordNum+" ");
		
		
		
		return vg;
	}

}
