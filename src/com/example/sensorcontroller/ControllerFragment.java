package com.example.sensorcontroller;

import android.R.string;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;


	public class ControllerFragment extends Fragment {
		
		ListView devList;
		Button startBtn;
		Button scanBtn;
		Button applyBtn;
		CheckBox enableAccel;
		CheckBox enableGyro;
		CheckBox enableMag;
		ProgressBar scanning;
		TextView recordID;
		EditText messageText;
		
		Spinner accScale;
		Spinner gyroScale;
		Spinner samplingRate;

		//Constructor
		public ControllerFragment(){
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main,
					container, false);		
			ListView devList = (ListView) rootView.findViewById(R.id.devList);
			startBtn = (Button) rootView.findViewById(R.id.btn_start);
			scanBtn = (Button) rootView.findViewById(R.id.btn_scan);
			applyBtn = (Button) rootView.findViewById(R.id.btn_apply);
			
			enableAccel = (CheckBox) rootView.findViewById(R.id.accEnable);
			enableGyro = (CheckBox) rootView.findViewById(R.id.gyroEnable);
			enableMag = (CheckBox) rootView.findViewById(R.id.magEnable);
			
			devList.setAdapter(MainActivity.mDeviceAdapter);
			devList.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			devList.setItemsCanFocus(true);
			
			scanning = (ProgressBar) rootView.findViewById(R.id.progressBar1);
			recordID = (TextView) rootView.findViewById(R.id.record_id);
			messageText = (EditText) rootView.findViewById(R.id.txt_msg);
			accScale = (Spinner) rootView.findViewById(R.id.acc_scale);
			gyroScale = (Spinner) rootView.findViewById(R.id.gyro_scale);
			samplingRate = (Spinner) rootView.findViewById(R.id.smpl_rate);
			
			scanning.setVisibility(android.view.View.INVISIBLE);
			initialize();
			
			MainActivity.scanButton = scanBtn;
			MainActivity.startButton = startBtn;
			MainActivity.applyButton = applyBtn;

			MainActivity.scanProgressBar = scanning;
			
			return rootView;		
		}
		
		private void initialize(){
			
			//Button click listiners
			startBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					
					byte timeArray[] = new byte[4];		
					String ID;
											
					
					String message = messageText.getText().toString();
					if(message.length() > 0 && message.length() < 21){
						
						MainActivity.mBleDeviceController.message = message;
					}
					else
					{
						MainActivity.mBleDeviceController.message = "Default";
					}
					Log.i("Set message", "Message: " + message);
					
					//Start recording procedure
					if(MainActivity.mBleDeviceController.startRecording()){
						startBtn.setText("Stop");
						
						Time time = new Time();
						time.setToNow();
						if(time.month < 9)
							ID = "ID: 0" + (time.month + 1) + time.monthDay + time.hour + time.minute;
						else
							ID = "ID: " + (time.month + 1) + time.monthDay + time.hour + time.minute;
						
						recordID.setText(ID);
					}
					else{
						startBtn.setText("Start");
					}
					//Stop recording procedure
					
				}
				
			});
			
			scanBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					MainActivity.mBleDeviceController.scanBleDevice(true);
					scanBtn.setEnabled(false);
					startBtn.setEnabled(false);
					applyBtn.setEnabled(false);
					scanning.setVisibility(android.view.View.VISIBLE);

				}
				
			});
			
			applyBtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					
					if(enableAccel.isChecked()){
						
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.ENABLE_ACC);						
					}
					else{
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.DISABLE_ACC);
					}

					if(enableGyro.isChecked()){
						
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.ENABLE_GYRO);						
					}
					else{
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.DISABLE_GYRO);
					}

					if(enableMag.isChecked()){
						
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.ENABLE_MAG);
					}
					else{
						MainActivity.mBleDeviceController.mSensorConfig.enableMeasurement(SensorConfig.DISABLE_MAG);
					}

					
					
					MainActivity.mBleDeviceController.applyConfig();
					
				}
				
				
			});
			
			accScale.setAdapter(MainActivity.accScaleAdapter);
			gyroScale.setAdapter(MainActivity.gyroScaleAdapter);
			samplingRate.setAdapter(MainActivity.samplingRateAdapter);
			
			accScale.setOnItemSelectedListener(new OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					switch(position){
					case 0:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_ACC_SCALE, SensorConfig.ACC_SCALE_NORMAL);

						break;
						
					case 1:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_ACC_SCALE, SensorConfig.ACC_SCALE_MAX);						
						break;
						
					case 2:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_ACC_SCALE, SensorConfig.ACC_SCALE_MIN);
						break;
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
				
			});

			gyroScale.setOnItemSelectedListener(new OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					switch(position){
					case 0:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_GYRO_SCALE, SensorConfig.GYRO_SCALE_NORMAL);
						break;
						
					case 1:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_GYRO_SCALE, SensorConfig.GYRO_SCALE_MAX);
						break;
						
					case 2:
						MainActivity.mBleDeviceController.mSensorConfig.setScale(SensorConfig.SET_GYRO_SCALE, SensorConfig.GYRO_SCALE_MIN);
						break;
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
				
			});
			
			samplingRate.setOnItemSelectedListener(new OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					switch(position){
					case 0:
						MainActivity.mBleDeviceController.mSensorConfig.setSamplingRate(SensorConfig.SMPL_RATE_NORMAL);
						break;
						
					case 1:
						MainActivity.mBleDeviceController.mSensorConfig.setSamplingRate(SensorConfig.SMPL_RATE_MAX);
						break;
						
					case 2:
						MainActivity.mBleDeviceController.mSensorConfig.setSamplingRate(SensorConfig.SMPL_RATE_MIN);
						break;
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					// TODO Auto-generated method stub
					
				}
				
			});
		}
		
		
	}