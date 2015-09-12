package com.example.sensorcontroller;

import android.util.Log;


public class SensorConfig {
	
	final public byte ACC_SCALE[] = {0x00, 0x08, 0x10};
	final public byte GYRO_SCALE[] = {0x08, 0x10, 0x10};
	final public byte SAMPLING_DELAY[] = {0x01, 0x02, 0x04};
	
	
	//Commands
	final public static byte ENABLE_MEASUREMENT = 0x11;
	final public static byte SET_ACC_SCALE = 0x12;
	final public static byte SET_GYRO_SCALE = 0x13;
	final public static byte SET_SMPL_RATE = 0x14;
	//Command parameters
	final public static byte ENABLE_ACC = 0x00;
	final public static byte ENABLE_GYRO = 0x02;
	final public static byte ENABLE_MAG = 0x04;
	final public static byte DISABLE_ACC = 0x01;
	final public static byte DISABLE_GYRO = 0x03;
	final public static byte DISABLE_MAG = 0x05;
	
	final public static byte ACC_SCALE_MIN = 0x00;
	final public static byte ACC_SCALE_NORMAL = 0x08;
	final public static byte ACC_SCALE_MAX = 0x10;
	final public static byte GYRO_SCALE_MIN = 0x08;
	final public static byte GYRO_SCALE_NORMAL = 0x10;
	final public static byte GYRO_SCALE_MAX = 0x18;
	
	final public static byte SMPL_RATE_MIN = 0x01;
	final public static byte SMPL_RATE_NORMAL = 0x02;
	final public static byte SMPL_RATE_MAX = 0x04;	
	
	final public int COMMAND_SIZE = 12;  //The formatted command contains 12 bytes

	public boolean accEnable;
	public boolean gyroEnable;
	public boolean magEnable;
	public byte samplingRate;
	public byte accScale;
	public byte gyroScale;
	
	public byte [] command;
	
	public String Message;
	
	public SensorConfig (){
		
		command = new byte[COMMAND_SIZE];		
		
		accEnable = true;
		gyroEnable = true;
		magEnable = false;
		samplingRate = SMPL_RATE_NORMAL;
		accScale = ACC_SCALE[0];
		gyroScale = GYRO_SCALE[0];
		Message = "None";
	}
	
	public void enableMeasurement(byte param){
		switch(param/2)
		{
		case 0:
			if(param%2 == 0){
				accEnable = true;
			}
			else
			{
				accEnable = false;
			}
			break;
			
		case 1:
			if(param%2 == 0){
				gyroEnable = true;
			}
			else
			{
				gyroEnable = false;
			}
			break;
			
		case 2:
			if(param%2 == 0){
				magEnable = true;
			}
			else
			{
				magEnable = false;
			}
			break;
			
		default:
			break;
		}
	}
	
	public void setScale(byte meas, byte param){
		
		if(meas == SET_ACC_SCALE && (param == ACC_SCALE_MIN || param == ACC_SCALE_NORMAL || param == ACC_SCALE_MAX))
		{
			accScale = param;
			Log.i("SensorConfig", "set acc scale " + param);
		}
		
		if(meas == SET_GYRO_SCALE && (param == GYRO_SCALE_MIN || param == GYRO_SCALE_NORMAL || param == GYRO_SCALE_MAX))
		{
			gyroScale = param;
			Log.i("SensorConfig", "set gyro scale " + param);
		}		
		
		
	}
	
	public void setSamplingRate(byte param){
		if(param == SMPL_RATE_MIN || param == SMPL_RATE_NORMAL || param == SMPL_RATE_MAX)
		{
			samplingRate = param;
		}		
	}
	
	public void formatCommand(){
		
		command = new byte[COMMAND_SIZE];
		
		
		command[0] = ENABLE_MEASUREMENT;
		command[2] = ENABLE_MEASUREMENT;
		command[4] = ENABLE_MEASUREMENT;
		command[6] = SET_ACC_SCALE;
		command[8] = SET_GYRO_SCALE;
		command[10] = SET_SMPL_RATE;
		
		
		if(accEnable){
			command[1] = ENABLE_ACC;
		}else{
			command[1] = DISABLE_ACC;
		}
		
		if(gyroEnable){
			command[3] = ENABLE_GYRO;
		}else{
			command[3] = DISABLE_GYRO;
		}
		
		if(magEnable){
			command[5] = ENABLE_MAG;
		}else{
			command[5] = DISABLE_MAG;
		}


		command[7] = accScale;
		command[9] = gyroScale;
		command[11] = samplingRate;
		
	}
	
	
	
}
