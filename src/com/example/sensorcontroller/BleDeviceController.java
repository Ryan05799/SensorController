package com.example.sensorcontroller;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;

public class BleDeviceController {
	//UUIDs of BLE services, characteristics and Descriptors
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	//MpuService
	public static final UUID MPU_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_RECORDS_CHAR = UUID.fromString("6e400101-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_MODE_CHAR = UUID.fromString("6e400201-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_COMMAND_CHAR = UUID.fromString("6e400202-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_TIME_CHAR = UUID.fromString("6e400301-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_MESSAGE_CHAR = UUID.fromString("6e400302-b5a3-f393-e0a9-e50e24dcca9e");
	
	//BAS Service
	public static final UUID BAS_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_LEVEL_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	public static enum connStatus{CONNECTED, DISCONNECTED, OFFLINE};
	
	public enum ExeThreadMode {READY, CONNECT, CONFIG, START, RECORDING, TERMINATE};
	public enum GattOp {READ_CHARACTERISTIC, WRITE_CHARACTERISTIC, CONNECT};
	
	public static final int MAX_DEVICE_NUMBER = 3; 
	private static final long SCAN_PERIOD = 2000; //2 seconds
	public static final String [] BLE_DEVICE_NAME = {"MPU_Sensor", "MPU_Sensor2" ,"MPU_Sensor3"};
	
	
	private Context MainActivityContext;
	private static BluetoothAdapter mBluetoothAdapter;
	private static Handler mHandler;
	private static BleService mBleService;
	protected static BleDevice [] mBleDeviceSet;
	
	private static ConcurrentLinkedQueue <GattCommand> mCommandQueue;
	private CommandExecThread cmdExeThrd;
	public static SensorConfig mSensorConfig;
	private GattOp waitOp;
	
	public String message;
	

	//Constructor of BleDeviceController
	public BleDeviceController(Context context){
		MainActivityContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = new Handler();
		mSensorConfig = new SensorConfig();
		message = "DEFAULT";
		
		//Initialize BleDevices
		mBleDeviceSet = new BleDevice[MAX_DEVICE_NUMBER];
		for(int i = 0; i < MAX_DEVICE_NUMBER ; i++){
			//initialize BleDevice according to the device name list
			mBleDeviceSet[i] = new BleDevice(BLE_DEVICE_NAME[i]);
		}
		
		 //initialize BleService
		 if(BleService_init())
			 Log.e("BDC", "BleService initialized");
		 
		//Initialize queue
		mCommandQueue = new ConcurrentLinkedQueue<GattCommand>();
		 

	}

	//Show if the Bluetooth is enable
	public boolean isBluetoothEnabled(){
		return mBluetoothAdapter.isEnabled();
	}
		
	//Start scanning procedure
	public void scanBleDevice(boolean enable){
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					
					mBluetoothAdapter.stopLeScan(mBleScanCallback);
					MainActivity.refreshFragment(0);
					connectAll();
				}
			}, SCAN_PERIOD);
				mBluetoothAdapter.startLeScan(mBleScanCallback);
		}
		else {
			mBluetoothAdapter.stopLeScan(mBleScanCallback);
		}
	}
		
	//The callback defines the actions taken when a BLE device is found
	private static BluetoothAdapter.LeScanCallback mBleScanCallback = new BluetoothAdapter.LeScanCallback(){
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
				int index = checkDeviceName(device.getName());
				UUID uuid = findUUID(scanRecord);
				
				//Device found, update device info
				if(index != -1 && uuid.equals(MPU_SERVICE)){
					mBleDeviceSet[index].device = device;
					mBleDeviceSet[index].status = connStatus.DISCONNECTED;
					//Add connect command to the queue
					mCommandQueue.add(new GattCommand(GattOp.CONNECT, null, index, null, 0));
				}
		}		
	};
	
	//Check if the device name is in the list, and returns the index if found
	private static int checkDeviceName(String name){
		for(int i = 0 ; i < MAX_DEVICE_NUMBER ; i++)
			if(BLE_DEVICE_NAME[i].equals(name))
				return i;
		return -1;
	}
	//Find the index of the UUID in the scanRecord
	private static UUID findUUID(byte [] advdata){
		byte type = advdata[1];
		int length = (int) advdata[0];
		int pointer = 0;
		
		//find the index of the UUID in scanRecord
		while(type != 0x07){
			pointer = pointer + length+1;
			length = (int) advdata[pointer];
			type = advdata[pointer+1];
		}
		
		//format byte array to UUID
		String UUIDString = "";
		for(int i = length ; i > 1 ; i--){
			UUIDString = UUIDString + String.format("%02x", advdata[pointer+i] & 0xff);
			if(i == 8 || i==10 || i == 12 || i == 14)
				UUIDString = UUIDString +"-";
		}
		return UUID.fromString(UUIDString);
	}
	
	//Connect all device found (when end scanning)
	private void connectAll(){
		cmdExeThrd = new CommandExecThread();
		cmdExeThrd.mode = ExeThreadMode.CONNECT;
		cmdExeThrd.start();
		MainActivity.refreshFragment(0);
	}
	
	public boolean startRecording(){
				
		if( cmdExeThrd == null)
			cmdExeThrd = new CommandExecThread();

		if(	cmdExeThrd.mode == ExeThreadMode.READY){
			
			//Check if devices are ready

			
			//Start recording		
			cmdExeThrd = new CommandExecThread();
			cmdExeThrd.mode = ExeThreadMode.START;
			cmdExeThrd.start();
			return true;
		}
		else if(cmdExeThrd.mode == ExeThreadMode.RECORDING){
			//Stop recording
			cmdExeThrd.mode = ExeThreadMode.TERMINATE;
		}
		return false;

	}
	
	public void applyConfig(){
			if(cmdExeThrd != null)
				cmdExeThrd = null;
			mSensorConfig.formatCommand();
			cmdExeThrd = new CommandExecThread();
			cmdExeThrd.mode = ExeThreadMode.CONFIG;
			cmdExeThrd.start();
	}
	
	/**
	 * The following are the functions and instance relative to BleService.
	 * service_init(), unbindBleService(), makeGattUpdateIntentFilter()
	 * mServiceConnection, BleServiceStatusChangeRevceiver
	 */
	//BleService related functions and instances
	public boolean BleService_init(){
		Intent bindIntent = new Intent(MainActivityContext, BleService.class);
		if(MainActivityContext.bindService(bindIntent, mServiceConnection,  Context.BIND_AUTO_CREATE))
		{
			LocalBroadcastManager.getInstance(MainActivityContext).registerReceiver(BleServiceStatusChangeRevceiver, makeGattUpdateIntentFilter());
		}else
		{
			Log.e("BDC", "bind fail");
			return false;
		}
		return true;
	}
	
	public void unbindBleService(){
		try{
			LocalBroadcastManager.getInstance(MainActivityContext).unregisterReceiver(BleServiceStatusChangeRevceiver);
		}catch(Exception ignore){
			Log.e("BDC", ignore.toString());
		}
		MainActivityContext.unbindService(mServiceConnection);
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			
			mBleService = ((BleService.LocalBinder) rawBinder).getService();
			if(!mBleService.initialize(MAX_DEVICE_NUMBER))
			{				
				Log.e("BDC", "Fail to initialize BleService");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Log.e("BDC", "BleService disconnected");
			mBleService = null;
		}
		
	};
	
	private static IntentFilter makeGattUpdateIntentFilter(){
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_NOTIFY);
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_READ);
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_WRITE);
		intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BleService.ACTION_GATT_ERROR);
		return intentFilter;
	}
	
	private final BroadcastReceiver BleServiceStatusChangeRevceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			
			int index = checkDeviceName(intent.getStringExtra(BleService.DEVICE_NAME));
			if(index == -1){
				
				//Invalid device index
				return;
			}
			
			//On connected
			if(action.equals(BleService.ACTION_GATT_CONNECTED)){
				
					mBleDeviceSet[index].status = connStatus.CONNECTED;
					MainActivity.refreshFragment(0);										
			}
			
			//On disconnected
			if(action.equals(BleService.ACTION_GATT_DISCONNECTED)){
				
					mBleDeviceSet[index].status = connStatus.DISCONNECTED;
					mBleDeviceSet[index].ready = false;
					MainActivity.refreshFragment(0);
					
					if(cmdExeThrd.mode == ExeThreadMode.RECORDING){
						//Disconnected when recording

						//Terminate the CommandExecthread
						cmdExeThrd.mode = ExeThreadMode.TERMINATE;														
						
						//Reset Widget
						MainActivity.startButton.setText("Start");
						MainActivity.scanButton.setEnabled(true);
						MainActivity.startButton.setEnabled(true);
						MainActivity.applyButton.setEnabled(true);
					}


			}
			
			//On service discovered
			if(action.equals(BleService.ACTION_GATT_SERVICES_DISCOVERED)){
				
				mBleDeviceSet[index].ready = true;
				Log.i("Service discovered","Device"+ index + "Ready");
				
				
				if(waitOp == GattOp.CONNECT){
					
					//Connection procedure is complete
						waitOp = null;
				}
			}
			
			//On characteristic write
			if(action.equals(BleService.ACTION_CHARACTERISTIC_WRITE)){
				String charUUID = intent.getStringExtra(BleService.CHAR_UUID);
				Log.i("Write ", "Su ");

				if(waitOp == GattOp.WRITE_CHARACTERISTIC){
					//Write char procedure is complete
					Log.i("Write ", "UUID "+ charUUID);
					waitOp = null;		
				}
				
			}
			
			//On characteristic read
			if(action.equals(BleService.ACTION_CHARACTERISTIC_READ)){
				String charUUID = intent.getStringExtra(BleService.CHAR_UUID);
				final byte[] value = intent.getByteArrayExtra(BleService.CHAR_VALUE);
				if(charUUID != null && value != null){
					Log.i("Read ", "UUID "+ charUUID);

					if(charUUID.equals(BATTERY_LEVEL_CHAR_UUID.toString())){
						mBleDeviceSet[index].batteryLevel = (int)((float)(value[0] & 0x000000ff)/255 *100);
						MainActivity.refreshFragment(0);
					}
					
					if(charUUID.equals(MPU_RECORDS_CHAR.toString())){
						String valueStr = new String(value);
						mBleDeviceSet[index].recordNum = Integer.parseInt(valueStr);
						MainActivity.refreshFragment(0);
					}
					
					if(waitOp == GattOp.READ_CHARACTERISTIC){
						//Connection procedure is complete
						waitOp = null;
					}
				}
			}
		}
	};
	
	//Get device according to its index in the device set
	public BleDevice getDevice(int index){
		return mBleDeviceSet[index];
	}

	
	public void close(){
		
		//Terminate CommandExecThread
		if(	cmdExeThrd != null)
		{
			cmdExeThrd.mode = ExeThreadMode.READY;
			cmdExeThrd.interrupt();
		}
		
		//Procedure to terminate BleService
		unbindBleService();
		mBleService.stopSelf();
		mBleService = null;
		Log.i("Close", "BLE service closed");
	}
	
	//Inner Classes
	public static class GattCommand{
		
		UUID Target;
		int devIndex;
		int retry;
		GattOp op;
		byte [] value;
		
		public GattCommand(GattOp op, UUID characteristic, int index, byte [] value, int retry){
			
			this.Target = characteristic;
			this.devIndex = index;
			this.op  = op;
			this.value = value;
			this.retry = retry;
		}
	}
	
	public class BleDevice {
		String devName;
		public BluetoothDevice device;
		public int batteryLevel;
		public int recordNum;
		public connStatus status;
		public boolean ready;
		public String ID;
		
		
		public BleDevice(String name){
			devName = name;
			status = connStatus.OFFLINE;
			batteryLevel = 0;
			recordNum = 0;
			ID = "None";
			ready = false;
		}
	}

	
	public class CommandExecThread extends Thread{
		
		public ExeThreadMode mode;
		
		public CommandExecThread(){
			
			mode = ExeThreadMode.READY;
		}
		
		@Override
		public void run(){
			
			//Set widgets: Disable all buttons, show the progress bar
			sendWidgetMessage(0);

			
			//Procedure taken when connecting
			if(this.mode == ExeThreadMode.CONNECT){
				
				
				
				while((this.mode == ExeThreadMode.CONNECT) && !mCommandQueue.isEmpty()){
					//poll commands
					GattCommand command = mCommandQueue.poll();
					
					//Connect and discover services
					if(command.op == GattOp.CONNECT){
						mBleService.connect(command.devIndex, mBleDeviceSet[command.devIndex].device);
						waitGattCallback(500, GattOp.CONNECT);
						
						//Check if the device has been connected, disconnect and retry if hasn't 
						if(mBleDeviceSet[command.devIndex].status == connStatus.DISCONNECTED || !mBleDeviceSet[command.devIndex].ready)
						{
							mBleService.disconnect(command.devIndex);
							command.retry++;
							if(command.retry < 5)
							{//Retry the terminating procedure at most 5 times	
								mCommandQueue.add(command);
							}
						}
						//Device add read Record and Battery level characteristic commands
						else{
							mCommandQueue.add(new GattCommand(GattOp.READ_CHARACTERISTIC, BATTERY_LEVEL_CHAR_UUID, command.devIndex, null, 0));
							mCommandQueue.add(new GattCommand(GattOp.READ_CHARACTERISTIC, MPU_RECORDS_CHAR, command.devIndex, null, 0));
							mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_TIME_CHAR, command.devIndex, null, 0));

						}
					}
					
					if(command.op == GattOp.READ_CHARACTERISTIC){
						if(command.Target.equals(BATTERY_LEVEL_CHAR_UUID)){
							//Read BAS
							mBleService.readCharateristic(command.devIndex, BAS_UUID, BATTERY_LEVEL_CHAR_UUID);
						}
						
						if(command.Target.equals(MPU_RECORDS_CHAR)){
							//Read Records(# of files)
							mBleService.readCharateristic(command.devIndex, MPU_SERVICE, MPU_RECORDS_CHAR);
						}
						waitGattCallback(500, GattOp.READ_CHARACTERISTIC);
					}
					
					if(command.op == GattOp.WRITE_CHARACTERISTIC && command.Target.equals(MPU_TIME_CHAR)){
						byte timeArray[] = new byte[6];		
						Time time = new Time();
						time.setToNow();
						//Format time to byte array
						timeArray[0] = 0;
						timeArray[1] = (byte) (time.second & 0xff);
						timeArray[2] = (byte) (time.minute & 0xff);
						timeArray[3] = (byte) (time.hour & 0xff);
						timeArray[4] = (byte) (time.monthDay & 0xff);
						timeArray[5] = (byte) (time.month+1 & 0xff);
						//Write to characteristic
						mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, MPU_TIME_CHAR, timeArray);
						if(!waitGattCallback(200, GattOp.WRITE_CHARACTERISTIC))
						{
							command.retry++;
							if(command.retry < 5)
							{//Retry the terminating procedure at most 5 times	
								mCommandQueue.add(command);
							}
							else{
								mBleService.disconnect(command.devIndex);
							}
						}
					}
				}
				
				//Set widgets: Enable all buttons, hide the progress bar
				sendWidgetMessage(1);
				
				this.mode = ExeThreadMode.READY;
				this.interrupt();
			}
			
			
			//Procedure taken for applying configuration
			if(this.mode == ExeThreadMode.CONFIG){
				
				while(!mCommandQueue.isEmpty()){
					//Clear command queue
					mCommandQueue.poll();
				}
				
				for(int index = 0 ; index <  MAX_DEVICE_NUMBER ; index++)
				{
					//Add write characteristic command to the queue
					if(mBleDeviceSet[index].ready)
						mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_COMMAND_CHAR, index, null,0));
				}
				
				while((this.mode == ExeThreadMode.CONFIG) && !mCommandQueue.isEmpty()){
					
					GattCommand command = mCommandQueue.poll();
															
					Log.i("Configuring", "Device " + command.devIndex);
					mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, MPU_COMMAND_CHAR, mSensorConfig.command);
					if(!waitGattCallback(40, GattOp.WRITE_CHARACTERISTIC))
					{ //Timeout
						command.retry++;
						if(command.retry < 5)
						{//Retry the terminating procedure at most 5 times	
							mCommandQueue.add(command);
						}
					}										
				}
				
				//Set widgets: Enable all buttons, hide the progress bar
				sendWidgetMessage(1);
			}
			
			
			//Procedure taken when starting recording
			if(this.mode == ExeThreadMode.START){
				
				while(!mCommandQueue.isEmpty()){
					//Clear command queue
					mCommandQueue.poll();
				}
				
				for(int index = 0 ; index <  MAX_DEVICE_NUMBER ; index++)
				{
					//Add write message characteristic command to the queue
					if(mBleDeviceSet[index].ready)
						mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_MESSAGE_CHAR, index, null,0));
				}
				
				while((this.mode == ExeThreadMode.START) && !mCommandQueue.isEmpty()){
					
					GattCommand command = mCommandQueue.poll();
					
					if(command.op != GattOp.WRITE_CHARACTERISTIC)
						continue;
						
					
					if(command.Target.equals(MPU_MESSAGE_CHAR)){
						
						byte[] msg = message.getBytes();
						if(msg.length < 20){
							mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, MPU_MESSAGE_CHAR, msg);
						}
						
						mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_MODE_CHAR, command.devIndex, null, 0));
						waitGattCallback(40, GattOp.WRITE_CHARACTERISTIC);
					}
					
					if(command.Target.equals(MPU_MODE_CHAR))
					{
						Log.i("CmdExe", "Start recording. Device "+ command.devIndex);				
						byte [] mode = {0x02};
						mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, MPU_MODE_CHAR, mode);
						if(!waitGattCallback(40, GattOp.WRITE_CHARACTERISTIC))
						{ //Timeout
							command.retry++;
							if(command.retry < 5)
							{//Retry the terminating procedure at most 5 times	
								mCommandQueue.add(command);
							}
							else{
								//Force the recording to stop by disconnecting all device
								mBleService.disconnect(command.devIndex);
							}
						}
					}
					
				}
				
				this.mode = ExeThreadMode.RECORDING;
				
				//Set widgets: Enable only Stop buttons, hide the progress bar
				sendWidgetMessage(2);

			}
						
			while(this.mode == ExeThreadMode.RECORDING){
				//Wait for terminating
			}			
			//Procedure for terminating recording
			if(this.mode == ExeThreadMode.TERMINATE){
								
				sendWidgetMessage(0);
				
				while(!mCommandQueue.isEmpty()){
					//Clear command queue
					mCommandQueue.poll();
				}
				
				for(int index = 0 ; index <  MAX_DEVICE_NUMBER ; index++)
				{
					//Add write mode characteristic command to the queue
					byte [] mode = {0x01};
					if(mBleDeviceSet[index].ready)
						mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_MODE_CHAR, index, mode, 0));
				}
				
				while((this.mode == ExeThreadMode.TERMINATE) && !mCommandQueue.isEmpty()){
					
					GattCommand command = mCommandQueue.poll();
					
					if(command.op != GattOp.WRITE_CHARACTERISTIC)
						continue;
					
					if(command.Target.equals(MPU_MODE_CHAR))
					{
						Log.i("CmdExe", "Stop recording. Device "+ command.devIndex);				
						mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, MPU_MODE_CHAR, command.value);
						
						if(!waitGattCallback(40, GattOp.WRITE_CHARACTERISTIC))
						{ //Timeout
							command.retry++;
							if(command.retry < 5)
							{//Retry the terminating procedure at most 5 times	
								mCommandQueue.add(command);
							}
							else{
								//disconnect the device
								mBleService.disconnect(command.devIndex);
							}
						}
					}
					
				}
				//Set widgets: Enable all buttons
				sendWidgetMessage(1);
			}
			
				this.mode = ExeThreadMode.READY;		

			}
		
			
		
		}
		
		private void sendWidgetMessage(int option){
			Message thrdEndMsg = new Message();
			thrdEndMsg.what = option;
			MainActivity.mWidgetHandler.sendMessage(thrdEndMsg);

		}
	
		//Function for pausing CommandExecThread with a lock(isWaiting), return true if the lock is set false before timeout
		private boolean waitGattCallback(int timeout, GattOp op){
			
			waitOp = op;
			
			while(waitOp != null && timeout > 0){
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				timeout--;
			}
			
			waitOp = null;
			
			if(timeout == 0){
				Log.i("Thread waiting", "Timeout");
				return false;
			}
			else{
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			}
		}		
	
}
