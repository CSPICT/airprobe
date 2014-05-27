/**
 * AirProbe
 * Air quality application for Android, developed as part of 
 * EveryAware project (<http://www.everyaware.eu>).
 *
 * Copyright (C) 2014 CSP Innovazione nelle ICT. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For any inquiry please write to <devel@csp.it>
 * 
 * CONTRIBUTORS
 * 
 * This program was made with the contribution of:
 *   Fabio Saracino <fabio.saracino@csp.it>
 *   Patrick Facco <patrick.facco@csp.it>
 * 
 * 
 * SOURCE CODE
 * 
 *  The source code of this program is available at
 *  <https://github.com/CSPICT/airprobe>
 */

package org.csp.everyaware.bluetooth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.csp.everyaware.Constants;
import org.csp.everyaware.Utils;
import org.csp.everyaware.bluetooth.BluetoothHistoryManager.SaveHistoryTask;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.SemanticSessionDetails;
import org.csp.everyaware.gps.GpsTrackingService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BluetoothManager 
{    
    private int mState; //current connection status
    
	private final BluetoothAdapter mBluetoothAdapter;
	private Context mContext;
	private Handler mStartHandler;
	private Handler mMapHandler;
	private Handler mGraphHandler;
	private Handler mSensorBoxHandler;

	//it opens RFCOMM socket and estabilishes a remote device connection on socket
	private ConnectThread mConnectThread;

	//it reads received data on socket from remote device
	private ConnectedThread mConnectedThread;

	//to close while cycle in run() method of ConnectedThread
	private boolean mTransferON;
	
	//Patrick
	private static volatile BluetoothManager mBluetoothManager;
	
	private DbManager mDbManager;
	
	//read sessionId from shared prefs (sessionId is calculated when bluetooth connection is estabilished)
	private String mSessionId;

	private List<Record>mHistoryRecordsSerie = null; //containes a list of history records to be saved on db

	private int mNumOfHistoryRecords;	
	
	private static long mPrecBoxTs;
	private static String mCurrentHistSessionId;
	
	private boolean mPhoneGpsOverride; //if phone gps must override sensor box gps
	
	public static BluetoothManager getInstance(Context ctx, Handler handler)
	{
		if(mBluetoothManager == null)
			mBluetoothManager = new BluetoothManager(ctx, handler);
		
		return mBluetoothManager;
	}
	
	private BluetoothManager(Context ctx, Handler handler)
	{
		Log.d("BluetoothManager", "BluetoothManager()");
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mContext = ctx;		
		mStartHandler = handler;
		
		mPrecBoxTs = 0;
		mCurrentHistSessionId = Utils.getSessionId(mContext);
		
		mDbManager = DbManager.getInstance(mContext);
		mDbManager.openDb();
		
		//after bt connection is established and before communication starts, download sensor box
		//addresses and parse max.txt and min.txt files
		new GetMaxMinTask().execute(); 
		
		setState(Constants.STATE_NONE);
	}
	
	public void setMapHandler(Handler handler)
	{
		mMapHandler = handler;
	}
	
	public void setGraphHandler(Handler handler)
	{
		mGraphHandler = handler;
	}	
	
	public void setSensorBoxHandler(Handler handler)
	{
		mSensorBoxHandler = handler;
	}

	//starts ConnectThread
	public synchronized void connect(BluetoothDevice device)
	{
		Log.d("BluetoothManager", "Connecting to: " +device);
		
		if(mState == Constants.STATE_CONNECTING)
		{
			if(mConnectThread != null)
			{
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		
		if(mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		//starting of Connect Thread (opening socket and estabilishing remote device connection on it)
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		
		setState(Constants.STATE_CONNECTING);
	}
	
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
	{
		if(mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		if(mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		
		Looper.prepare();
		
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		
		setState(Constants.STATE_CONNECTED);
  		Utils.btConnectionOn = Constants.STATE_CONNECTED;
	}

	//to track failure of attempt to connect to remote device
	//UI main thread is advised
    private void connectionFailed() 
    {
        setState(Constants.STATE_NONE);
        Utils.btConnectionOn = Constants.CONNECTION_LOST;
        
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHandler == null)
        		return;
        	
	        Message msg = mStartHandler.obtainMessage(Constants.CONNECTION_FAILED);
	        mStartHandler.sendMessage(msg);
	        return;
        }
        if((Utils.getStep(mContext) == Constants.TRACK_MAP)||(Utils.getStep(mContext) == Constants.COMM_MAP))
        {
        	if(mMapHandler == null)
        		return;
        	
	        Message msg = mMapHandler.obtainMessage(Constants.CONNECTION_FAILED);
	        mMapHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.GRAPH)
        {
        	if(mGraphHandler == null)
        		return;
        	
	        Message msg = mGraphHandler.obtainMessage(Constants.CONNECTION_FAILED);
	        mGraphHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.SBOX)
        {
        	if(mSensorBoxHandler == null)
        		return;
        	
	        Message msg = mSensorBoxHandler.obtainMessage(Constants.CONNECTION_FAILED);
	        mSensorBoxHandler.sendMessage(msg);
        	return;
        }
    }
    
    //to track losed bluetooth connection
    public void connectionLost()
    {
    	setState(Constants.STATE_NONE);
    	Utils.btConnectionOn = Constants.CONNECTION_LOST;
    	
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHandler == null)
        		return;
        	
        	Message msg = mStartHandler.obtainMessage(Constants.CONNECTION_LOST);
        	mStartHandler.sendMessage(msg);
        	return;
        }
        if((Utils.getStep(mContext) == Constants.TRACK_MAP)||(Utils.getStep(mContext) == Constants.COMM_MAP))
        {
        	if(mMapHandler == null)
        		return;
        	
	        Message msg = mMapHandler.obtainMessage(Constants.CONNECTION_LOST);
	        mMapHandler.sendMessage(msg);	        
        	return;
        }
        if(Utils.getStep(mContext) == Constants.GRAPH)
        {
        	if(mGraphHandler == null)
        		return;
        	
	        Message msg = mGraphHandler.obtainMessage(Constants.CONNECTION_LOST);
	        mGraphHandler.sendMessage(msg);
	        return;
        }
        if(Utils.getStep(mContext) == Constants.SBOX)
        {
        	if(mSensorBoxHandler == null)
        		return;
        	
	        Message msg = mSensorBoxHandler.obtainMessage(Constants.CONNECTION_LOST);
	        mSensorBoxHandler.sendMessage(msg);
        	return;
        }
    }
    
    private synchronized void setState(int state)
    {
    	mState = state;
    	
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHandler == null)
        		return;
        	
        	Message msg = mStartHandler.obtainMessage(mState);
        	mStartHandler.sendMessage(msg);  	
        	return;
        }
        if((Utils.getStep(mContext) == Constants.TRACK_MAP)||(Utils.getStep(mContext) == Constants.COMM_MAP))
        {
        	if(mMapHandler == null)
        		return;
        	
	        Message msg = mMapHandler.obtainMessage(mState);
	        mMapHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.GRAPH)
        {
        	if(mGraphHandler == null)
        		return;
        	
	        Message msg = mGraphHandler.obtainMessage(mState);
	        mGraphHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.SBOX)
        {        	
        	if(mSensorBoxHandler == null)
        		return;
        	
	        Message msg = mSensorBoxHandler.obtainMessage(mState);
	        mSensorBoxHandler.sendMessage(msg);
        	return;
        }    
    }
    
    //stop all threads
    public synchronized void stop() 
    {
        if (mConnectThread != null) 
        {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        if (mConnectedThread != null) 
        {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(Constants.STATE_NONE);
    }
    
    private void sensorBoxMacNotRead() 
    { 
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHandler == null)
        		return;
        	
	        Message msg = mStartHandler.obtainMessage(Constants.SENSOR_BOX_MAC_NOT_READ);	        
	        mStartHandler.sendMessage(msg);
	        return;
        }
        if((Utils.getStep(mContext) == Constants.TRACK_MAP)||(Utils.getStep(mContext) == Constants.COMM_MAP))
        {
        	if(mMapHandler == null)
        		return;
        	
	        Message msg = mMapHandler.obtainMessage(Constants.SENSOR_BOX_MAC_NOT_READ);
	        mMapHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.GRAPH)
        {
        	if(mGraphHandler == null)
        		return;
        	
	        Message msg = mGraphHandler.obtainMessage(Constants.SENSOR_BOX_MAC_NOT_READ);
	        mGraphHandler.sendMessage(msg);
        	return;
        }
        if(Utils.getStep(mContext) == Constants.SBOX)
        {
        	if(mSensorBoxHandler == null)
        		return;
        	
	        Message msg = mSensorBoxHandler.obtainMessage(Constants.SENSOR_BOX_MAC_NOT_READ);
	        mSensorBoxHandler.sendMessage(msg);
        	return;
        }
    }
    
    /************ THREAD APERTURA SOCKET E CONNESSIONE AL DISPOSIIVO SCELTO *********************/
    
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
		
		public ConnectThread(BluetoothDevice device)
		{
			mmDevice = device; 
			BluetoothSocket tmp = null;
			
			try
			{
				//5 - opening RFCOMM socket
				tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
				//to resolve pairing problem on Samsung Galaxy Tab2 - this solution requires Android 2.3.4 and later (minSdkVersion = 10)
				//tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			mmSocket = tmp;
		}
		
		public void run()
		{
			try
			{
				//6 - opening remote device connection on RFCOMM socket
				//    this call is blocking and it returns success or exception
				if(mmSocket != null)
					mmSocket.connect();
			}
			//failure on opening connection
			catch(IOException e) 
			{
				connectionFailed();
				e.printStackTrace();
				
				try
				{
					if(mmSocket != null)
						mmSocket.close();
				}
				catch(IOException e2)
				{
					e.printStackTrace();
				}
				
				return; //exit from run()
			}
			
			//ConnectThread reset
			synchronized(BluetoothManager.this)
			{
				mConnectThread = null;
			}
			
			if(mmSocket != null)
				connected(mmSocket, mmDevice);
			else
				connectionFailed();
		}
		
		public void cancel()
		{
			try
			{
				if(mmSocket != null)
					mmSocket.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/***************** THREAD RICEZIONE DATI DA DEVICE REMOTO SUL SOCKET APERTO *****************/
	
	private class ConnectedThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		
		boolean transmittingInfos = false;
		boolean transmittingHNR = false; //transmitting HistoryNumberRecords
			
		private String mSourceSessionSeed = "";
		private int mSourceSessionNumber = 0;
		private int mSourcePointNumber = 0;
		//private SemanticSessionDetails mSemantic;
		private String mSemanticSessionSeed = "";
		private int mSemanticSessionNumber = 0;
		private int mSemanticPointNumber = 0;
		private String mBoxMacAddr;
		
		double boxAcc = 0.0, boxBear = 0.0, boxSpeed = 0.0, boxAltitude = 0.0;
		int boxNumSat = 0;
		
		//Patrick
		byte[] buffer;
		//------------
		public ConnectedThread(BluetoothSocket socket)
		{
			//Patrick
			buffer = new byte[Constants.BLUETOOTH_BUFFER_SIZE_LIVE];
			//--------
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			mTransferON = true;

			//important initializations
			Utils.counterHR = 0; //received history records			
			Utils.lostHR = 0; //lost history records	
			mHistoryRecordsSerie = new ArrayList<Record>();

			mNumOfHistoryRecords = 0;			
			
			//getting I/O streams on RFCOMM socket
            try 
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            
    		mSessionId = Utils.getSessionId(mContext);
    		Log.d("ConnectedThreadRT", "ConnectedThread() --> sessionId: " +mSessionId);    
    		
    		//check if phone gps must override sensor box gps
    		if(Utils.getUsePhoneGpsIndex(mContext) == 0)
    		{
    			mPhoneGpsOverride = false;
    			Log.d("ConnectedThreadRT", "ConnectedThread()--> PHONE GPS OVERRIDE FALSE");			
    		}
    		else
    		{
    			mPhoneGpsOverride = true;
    			Log.d("ConnectedThreadRT", "ConnectedThread()--> PHONE GPS OVERRIDE TRUE");			
    		}
		}
		
		public void run()
		{
			Looper.prepare();
			
    		//receiving buffer
    		//buffer = new byte[4096];
    		int bytesRead;
    		int lastReadIndex; //it's the previuous cycle bytesRead value
    		
    		boolean connectionFailed = false;
    		
    		//hardware informations from sensor box
    		String infoMsg = "";
    		//contains measurements record
    		String message = "";
    		//contantins number of history records on sd card of sensor box
    		String numberHR = "";
    		
    		//DO NOTHING UNTIL MAP ACTIVITY STARTS!!! (onCreate() method invoked)
    		while((Utils.getStep(mContext)!=Constants.TRACK_MAP)||(mMapHandler == null))
    		{	
	    		try 
	    		{
	    			Log.d("ConnectedThreadRT", "run()--> WAIT 1 sec ");
					Thread.currentThread().sleep(1000);
				} 
	    		catch (InterruptedException e1) 
	    		{
					e1.printStackTrace();
				}
    		}
    		
    		/* 1 - ASK for infos to sensor box */

    		transmittingInfos = true;
    		
    		try
    		{
    			//sending of "%I" string to tell the sensor box that smartphone is ready to receive data
    			mmOutStream.write(Constants.askForInfo);
    			
    			//Log.d("ConnectedThreadRT", "run()--> SENT " +new String(Constants.askForInfo));
    		}
    		catch(IOException e)
    		{
    			e.printStackTrace();
    		}
    		while(transmittingInfos)
    		{
    			try
    			{
    				bytesRead = mmInStream.read(buffer);
    				infoMsg += new String(buffer, 0, bytesRead);
    				//Log.d("ConnectedThreadRT", "run()--> infoMsg: " +infoMsg);
    				
    				if((infoMsg.length() >= 4)&&(infoMsg.substring(infoMsg.length()-4, infoMsg.length()-2).equalsIgnoreCase("%K")))
    				{
    					transmittingInfos = false;
    					infoMsg = infoMsg.substring(0, infoMsg.length()-4); //toggle %K
    					Log.d("ConnectedThreadRT", "run()--> read box infos: " +infoMsg);
    					
    					String[] infos = infoMsg.split("\\*\\*");
    					if(infos != null)
    					{
    						for(int i = 0; i < infos.length; i++)
    						Log.d("ConnectedThreadRT", "run()--> infos: "+infos[i]);
    						
	    					//Save firmware version and mac address on static variable for continuous usage on saving record
	    					Constants.mFirmwareVersion = infos[0];
	    					Constants.mMacAddress = Utils.getDeviceAddress(mContext);

	    					Log.d("ConnectedThreadRT", "run()--> mac address: "+Constants.mMacAddress);
	    					
	    					if((Constants.mMacAddress != null)&&(!Constants.mMacAddress.equals("")))
	    					{
	    						if(infos.length > 1)
	    						{
	    							if((infos[1] != null)&&(infos[1].length() == 12))
	    							{
	    								Constants.mMacAddress = infos[1].substring(0, 2)+":"+infos[1].substring(2, 4)+":"+
	    										infos[1].substring(4, 6)+":"+infos[1].substring(6, 8)+":"+infos[1].substring(8, 10)+
	    										":"+infos[1].substring(10);
	    								
	    								Utils.setDeviceAddress(Constants.mMacAddress, mContext);
	    								
	    								Log.d("ConnectedThreadRT", "run()--> mac address recovered from info!--> mac: "+Constants.mMacAddress);
	    							}
	    						}
	    					}
	    					
	    					//check mac address with mac address read from max/min.txt files
	    					if(Utils.addresses != null)
	    					{
	    						boolean found = false;
	    						int j = 0;
	    					
	    						while((j < Utils.addresses.length)&&(!found))
	    						{
	    							Log.d("ConnectedThreadRT", "run()--> address["+j+"] = " +Utils.addresses[j]);
	    							
	    							if(Utils.addresses[j].equals(Constants.mMacAddress))
	    							{
	    								found = true;
	    								Utils.actual_sb_index = j;
	    								
	    								Log.d("ConnectedThreadRT", "run()--> found sensor box with address: " +Constants.mMacAddress+ " and index: " +Utils.actual_sb_index);
	    							}
	    							else
	    								j++;
	    						}
	    						
	    						//if actual sensor box is recognized, use it mac address to select the right column in min_matrix and max_matrix
	    						if(found)
	    						{
	    							int rows = Utils.max_matrix.length;
	    							
	    							Utils.max = new double[rows];
	    							Utils.min = new double[rows];
	    							
	    							//copy j-mo column into max vector
	    							for(int i = 0; i < rows; i++)
	    							{
	    								Utils.max[i] = Utils.max_matrix[i][j];
	    								Utils.min[i] = Utils.min_matrix[i][j];
	    								
	    								//Log.d("ConnectedThreadRT", "run()--> max["+i+"] = " +Utils.max[i]+" - min["+i+"] = " +Utils.min[i]);
	    							}
	    						}
	    			
	    						Utils.mac_recognized = found;
	    					}

	    					//save received informations on shared prefs
	    					if(infos.length < 3)
	    					{
	    						Utils.setBoxInfoMsg("Firmware version: not read\nSensor models: not read", mContext);
	    						
	    						//if infos lack of some information, comunicate it
	    						if(Constants.mMacAddress.equals(""))
	    							sensorBoxMacNotRead();
	    					}
	    					else
	    						Utils.setBoxInfoMsg("Firmware version: V" +infos[0]+"\nSensor models: " +infos[2], mContext);
    					}
    					//if infos from sensor box has not read, comunicate it
    					else
    					{
    						if(Constants.mMacAddress.equals(""))
    							sensorBoxMacNotRead();
    					}
    				}
    			}
    			//this exception happens when bluetooth connection fails
    			catch(IOException ex)
    			{
    				ex.printStackTrace();
    						
    				connectionLost();
    				connectionFailed = true;
    				break; //if connection fails, exit from while
    			}
    		}
    		
    		if(connectionFailed)
    			return;

    		/* 2 - ASK for number of history records saved on sensor box sd card */
	    		
	    	transmittingHNR = true;
	    		
		    try
		    {
		    	//sending of "%N" string to tell the sensor box that smartphone wants to know the number of history records
		    	//saved on its sd card
		    	mmOutStream.write(Constants.askForNumberHist);	    			    		
		    		
		    	//Log.d("ConnectedThreadRT", "run()--> SENT " +new String(Constants.askForNumberHist)); 
		    }
		    catch(IOException e)
		    {
		    	e.printStackTrace();
		    }    		
		    	
		    while(transmittingHNR)
		    {
	    		try
	    		{
	    			bytesRead = mmInStream.read(buffer);
	    			numberHR += new String(buffer, 0, bytesRead);
	    				
	    			Log.d("ConnectedThreadRT", "numberHR parziale: " +numberHR);
	     			if((numberHR.length() >= 4)&&(numberHR.substring(numberHR.length()-4, numberHR.length()-2).equalsIgnoreCase("%K")))
	    			{
	     				transmittingHNR = false;
	     				numberHR = numberHR.substring(0, numberHR.length()-4); //toggle %K and slash r slash n
	     				
	     				Utils.numberHR = Integer.valueOf(numberHR);

	    				Log.d("ConnectedThreadRT", "run()--> number of history records on sensor box sdcard: " +Utils.numberHR);
	    			}
	    		}
	    		//this exception happens when bluetooth connection fails
	    		catch(IOException ex)
	    		{
	    			ex.printStackTrace();
	    						
	    			connectionLost();
	    			connectionFailed = true;
	    			break; //if connection fails, exit from while
	    		}
 				catch(NumberFormatException e)
 				{
 					e.printStackTrace();
 				}
		    }
		    	
	    	if(connectionFailed)
	    		return;
    		
    		/* 3 - ASK for real time data to sensor box */
    		
	    	bytesRead = 0;
	    	lastReadIndex = 0;
	    	try
	    	{			
	    		//sending of "%R" string to tell the sensor box that smartphone is ready to receive data	    		
	    		mmOutStream.write(Constants.askForRealTime);
	    		
	    		//Log.d("ConnectedThreadRT", "run()--> SENT " +new String(Constants.askForRealTime)); 
 		
	    		//sleep useful to send ask for history command
	    		Thread.currentThread().sleep(1000);
				
	    		//if history download option is ON, send "%S" string to tell the sensor box to send also history data
	    		int downHistIdx = Utils.getDownloadHistIndex(mContext);
	    		if(Constants.historyDown[downHistIdx])
	    		{	    			    			
					mmOutStream.write(Constants.askForHistory);				
			    	//Log.d("ConnectedThread", "run()--> SENT " +new String(Constants.askForHistory)); 				    		
	    		}
	    		
	    		//send msg to UI thread
	            if((Utils.getStep(mContext) == Constants.TRACK_MAP)||(Utils.getStep(mContext) == Constants.COMM_MAP))
	            {
	            	if(mMapHandler != null)
	            	{            	
	            		Message msg = mMapHandler.obtainMessage(Constants.DATA_TRANSFER_STARTED);
	            		mMapHandler.sendMessage(msg);
	            	}
	            }	    		
	    	}
	    	catch(IOException e)
	    	{
	    		e.printStackTrace();
	    	}
	    	catch (InterruptedException e) 
	    	{
				e.printStackTrace();
			}

		    //listening on socket (for all the time the connection is up)
    		while(mTransferON)
    		{     			
    			try
    			{        				    				
    				//reading from InputStream; buffer is a vector on which data are saved
    				bytesRead += mmInStream.read(buffer, bytesRead, buffer.length-bytesRead);
    				
        			//save on string buffer contents
        			if(bytesRead != 0)
        			{    					
        				// \r\n: end of row
        				if((bytesRead >= 2) && (buffer[bytesRead-2] == 13) && (buffer[bytesRead-1] == 10)) 
        				{       				
        					//a) row terminates with %K: end of history set
        					if((bytesRead >= 4) && (buffer[bytesRead-4] == Constants.endHistorySet[0]) && ((buffer[bytesRead-3]) == Constants.endHistorySet[1]))
        					{
        						Log.d("ConnectedThreadRT", "run()--> end of history set");
        														
        						//create string array from buffer, last four bytes ('%' 'K' '\r' '\n') excluded
    							String[] historyRecordsStr = new String(buffer, 0, bytesRead-4).split("\r\n");
    							
    							mNumOfHistoryRecords = historyRecordsStr.length;
    							
        						//reset byte count on buffer
        						bytesRead = 0;
        							
        						String command = "";
        							
	    						if(mNumOfHistoryRecords < 10)
	    							command = new String(Constants.receivedRecordsNum)+"0"+String.valueOf(mNumOfHistoryRecords);
	    						else
	    							command = new String(Constants.receivedRecordsNum)+String.valueOf(mNumOfHistoryRecords);
	    								
    							//inform sensor box of the number of records received (so sensor box can keep track of these record as sent)
    							mmOutStream.write( command.getBytes() );
    								
    							//Log.d("ConnectedThread", "run()--> SENT " +new String(command.getBytes()));   
    							
    							//save history records if their number is 15
								if(mNumOfHistoryRecords == 15)
									new SaveHistoryTask().execute(historyRecordsStr);
        					}
        					//b) row terminates with %KK : end of history
        					else if((bytesRead >= 5) && (buffer[bytesRead-5] == Constants.endHistory[0]) && (buffer[bytesRead-4] == Constants.endHistory[1]) && (buffer[bytesRead-3] == Constants.endHistory[2])) 
        					{
        						Log.d("ConnectedThreadRT", "run()--> end of history");
        						
        						//create string array from buffer, last five bytes ('%' 'K' 'K' '\r' '\n') excluded
    							String[] historyRecordsStr = new String(buffer, 0, bytesRead-5).split("\r\n");   							
    							mNumOfHistoryRecords = historyRecordsStr.length;
    							
        						//reset byte count on buffer
        						bytesRead = 0;
        							
        						String command = "";
        							
	    						if(mNumOfHistoryRecords < 10)
	    							command = new String(Constants.receivedRecordsNum)+"0"+String.valueOf(mNumOfHistoryRecords);
	    						else
	    							command = new String(Constants.receivedRecordsNum)+String.valueOf(mNumOfHistoryRecords);
	    								
    							//inform sensor box of the number of records received (so sensor box can keep track of these record as sent)
    							mmOutStream.write( command.getBytes() );
    								
    							//Log.d("ConnectedThread", "run()--> SENT " +new String(command.getBytes()));
    							
        						new SaveHistoryTask().execute(historyRecordsStr);
        					}
        					//c) row terminates with </rt>: real time record
        					else if(bytesRead >= 7) 
        					{
        						if((buffer[bytesRead-7] == Constants.realTimeTagClose[0]) && (buffer[bytesRead-6] == Constants.realTimeTagClose[1]) && (buffer[bytesRead-5] == Constants.realTimeTagClose[2])
        							 && (buffer[bytesRead-4] == Constants.realTimeTagClose[3]) && (buffer[bytesRead-3] == Constants.realTimeTagClose[4]))
        						{        	
        							//Log.d("ConnectedThread", "run()--> real time data");
        							
        							//get string from buffer
        							String rtMsg = new String(buffer, lastReadIndex+4, bytesRead-lastReadIndex-11); //remove '<rt>' and '</rt>\r\n' 
        							
        							//put bytesRead to the start of this real time message, so the next reading from buffer overwrite it
        							bytesRead = lastReadIndex;
        							
        							//split string
        							String[] values = rtMsg.split(",");
        								
        							//from sensorbox firmware >= 1.18 there are 26 fields
        							if(values.length == 26)
        							{
        								Log.d("ConnectedThreadRT", "run()--> real time data - fields number: " +values.length);
        								
        								mSourcePointNumber = Integer.valueOf(values[2]);
        								
        								//read system date and save it with record on db as timestamp
        								long sysTs = System.currentTimeMillis(); 
        								long boxTs = 0;
        								
        								double boxLat = 0.0;
        								double boxLon = 0.0;
        								
        								if(Utils.semanticWindowStatus)
        								{      									
        									mSemanticSessionSeed = Utils.installID;
        									mSemanticSessionNumber = Utils.semanticSessionNumber;   
        									mSemanticPointNumber = mSourcePointNumber - Utils.semanticStartPointNumber;
        									Log.d("ConnectedThreadRT", "run()--> semanticWindowStatus: "+Utils.semanticWindowStatus+" semantic session seed: "+mSemanticSessionSeed+
        											" semantic session number: "+mSemanticSessionNumber+" semantic point number: "+mSemanticPointNumber);
        								}
        								else
        								{
        									mSemanticSessionSeed = "";
        									mSemanticSessionNumber = 0;
        									mSemanticPointNumber = 0;
        								}
        								
        								
        								//********** received record is correctly georeferenced (gps from sensor box) **********
        								if(values[3].equalsIgnoreCase("A"))
        								{
        									Log.d("ConnectedThreadRT", "run()--> Valid georeferenced data received from sensor box");
        	
											//backup of box lat, lon values
											boxLat = Double.valueOf(values[4]);
											boxLon = Double.valueOf(values[6]);
											
        									if(mPhoneGpsOverride)
        										Log.d("ConnectedThreadRT", "run()--> Phone GPS override mode activated");
        									
        									//********** switch off gps tracking service (if active and phone gps must not override box gps) **********
        									if((Utils.gpsTrackServIntent != null)&&(Utils.getGpsTrackingOn(mContext))&&(!mPhoneGpsOverride))
        									{
        										mContext.stopService(Utils.gpsTrackServIntent);
        										Utils.setGpsTrackingOn(false, mContext);
        										
        										Log.d("ConnectedThreadRT", "run()--> phone gps tracking turned OFF");
        									}
        									
        									//*********** if gps on sensor box is not working now but gps phone override is requested, start service **********
        									if((mPhoneGpsOverride)&&(!Utils.getGpsTrackingOn(mContext)))
        									{
        										Utils.gpsTrackServIntent = new Intent(mContext,GpsTrackingService.class);
        									    mContext.startService(Utils.gpsTrackServIntent);
        									    
        									    Log.d("ConnectedThreadRT", "run()--> phone gps tracking turned ON");
        									}
        									
        									//********** if received data is georeferenced, boxTs is a valid data and save it **********
        									boxTs = Utils.getTsFromBoxTimestampFields(values[0], values[1]);
        									
        									//*********** try to get phone gps coords (only if phone gps must override box gps) ***********
        									boolean phoneGps = false;
        									double phoneAccuracy = 0; 
        									if(mPhoneGpsOverride)
        									{
	        									Location phoneLocation = Utils.lastPhoneLocation;	        									
	        									      									
	        									if(phoneLocation != null)
	        									{
	        										//if smartphone coords are not older than about 3 sec save them
	        										if(Math.abs(phoneLocation.getTime()-sysTs) <= 3000)
	        										{	        											
	        											//override sensor box gps values
	        											values[4] = String.valueOf(phoneLocation.getLatitude());
	        											values[6] = String.valueOf(phoneLocation.getLongitude());
	        									
	        											phoneAccuracy = phoneLocation.getAccuracy();      											
	        											phoneGps = true;
	        										}									
	        									}
        									}
        									
        									//********** check for network location. This is IN ADDITION to sensor box gps data **********
        							
        									Location networkLocation = Utils.lastNetworkLocation;
        									double[] networkLocArray = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //lat, lon, accuracy, bearing, speed, altitude
        									long networkTs = 0;
        									if(networkLocation != null)
        									{
        										long coordsTs = networkLocation.getTime();
        										
        										//if network data location are not older then 5 minutes, use them
        										if(Math.abs(coordsTs-sysTs) <= Constants.FIVE_MINS)
        										{
        											networkLocArray[0] = networkLocation.getLatitude();
        											networkLocArray[1] = networkLocation.getLongitude();
        											networkLocArray[2] = networkLocation.getAccuracy();
        											networkLocArray[3] = networkLocation.getBearing();
        											networkLocArray[4] = networkLocation.getSpeed();
        											networkLocArray[5] = networkLocation.getAltitude();
        																	
        											networkTs = networkLocation.getTime();       											
        										}
        									}
        									
        									double[] valuesArray = fillValues(values);
        									
        									//********** calculate Air Quality Index for actual array of values **********
        									Utils.calcBlackCarbon(valuesArray);
        									
        									//********** save record *************
        									if(phoneGps) //phone gps data found (phone gps override gps sensor box mode)
        									{
        										Log.d("ConnectedThreadRT", "run()--> PHONE GPS OVERRIDING MODE ACTIVATED - PHONE GPS DATA AVAILABLE AND USED");
        										
        										Log.d("ConnectedThreadRT", "run()--> phone gps: "+Utils.lastPhoneLocation.getLatitude()+", "+Utils.lastPhoneLocation.getLongitude());
    											Log.d("ConnectedThreadRT", "run()--> box gps: "+boxLat+", "+boxLon);
    											Log.d("ConnectedThreadRT", "run()--> net gps: "+networkLocArray[0]+", "+networkLocArray[1]);
    												        		
    											mSourceSessionSeed = values[19];
    											mSourceSessionNumber = Integer.valueOf(values[18]); //ex trackId in firmware v1.17

    											mBoxMacAddr = values[20];					
    											if((values[20] != null)&&(values[20].length() == 12))
    											{
    												mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
    														values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
    														":"+values[20].substring(10);
    											}
    											  											
    											try {
    												boxAcc = Double.valueOf(values[22]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxBear = Double.valueOf(values[25]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxSpeed = Double.valueOf(values[24]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxAltitude = Double.valueOf(values[22]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxNumSat = Integer.valueOf(values[21]);
    											}catch(Exception e){e.printStackTrace();};
    											
    											Log.d("ConnectedThreadRT", "run()--> "+values[22]+" "+values[25]+" "+values[24]+" "+values[23]+" "+values[21]);
    											mDbManager.saveRecord(sysTs, boxTs, valuesArray, mSessionId, Constants.LOCALIZATION[1], Constants.GPS_PROVIDERS[1], phoneAccuracy, networkLocArray, networkTs, 
    													mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumber, 
    													Utils.lastPhoneLocation.getLatitude(), Utils.lastPhoneLocation.getLongitude(), Utils.lastPhoneLocation.getAccuracy(), 
    													Utils.lastPhoneLocation.getBearing(), Utils.lastPhoneLocation.getSpeed(), Utils.lastPhoneLocation.getAltitude(), Utils.lastPhoneLocation.getTime(), 
    													boxLat, boxLon, boxAcc, boxBear, 0.51 * boxSpeed, boxAltitude, boxNumSat,
    													networkLocArray[3], networkLocArray[4], networkLocArray[5], 
    													mBoxMacAddr);
        										
        									}
        									else //phone gps not available, use only from sensor box
        									{
        										Log.d("ConnectedThreadRT", "run()--> phone gps: NOT AVAILABLE");
    											Log.d("ConnectedThreadRT", "run()--> box gps: "+boxLat+", "+boxLon);
    											Log.d("ConnectedThreadRT", "run()--> net gps: "+networkLocArray[0]+", "+networkLocArray[1]);
    											
        										//gps values in this case are from sensor box, so accuracy is not present and set to 0
    											mSourceSessionSeed = values[19];
    											mSourceSessionNumber = Integer.valueOf(values[18]); 

    											mBoxMacAddr = values[20];					
    											if((values[20] != null)&&(values[20].length() == 12))
    											{
    												mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
    														values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
    														":"+values[20].substring(10);
    											}
    											try {
    												boxAcc = Double.valueOf(values[22]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxBear = Double.valueOf(values[25]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxSpeed = Double.valueOf(values[24]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxAltitude = Double.valueOf(values[22]);
    											}catch(Exception e){e.printStackTrace();};
    											try {
    												boxNumSat = Integer.valueOf(values[21]);
    											}catch(Exception e){e.printStackTrace();};
    											
    											mDbManager.saveRecord(sysTs, boxTs, valuesArray, mSessionId, Constants.LOCALIZATION[1], Constants.GPS_PROVIDERS[0], 0, networkLocArray, networkTs, 
    													mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumber, 
    													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 
    													boxLat, boxLon, Double.valueOf(values[22]), Double.valueOf(values[25]), 0.51 * Double.valueOf(values[24]), Double.valueOf(values[23]), Integer.valueOf(values[21]),
    													networkLocArray[3], networkLocArray[4], networkLocArray[5],  
    													mBoxMacAddr);       										
        									}
            									
        									
            								if(Utils.firstAvailableLocation[0] == 0.0)
            								{
            									Utils.firstAvailableLocation[0] = valuesArray[0];
            									Utils.firstAvailableLocation[1] = valuesArray[1];
            										
            									if(phoneGps)
            										Log.d("ConnectedThreadRT", "run()--> first available location comes from SMARTPHONE");
            									else
            										Log.d("ConnectedThreadRT", "run()--> first available location comes from SENSOR BOX");
            									
            									//after the first location data is available, download sensor box
            									//calibration model data of the nearest city to this fix and parse it
            									new GetCalibrationDataTask().execute();
            								}
        								}
        								//received record is not georeferenced: use phone gps if available or network gps
        								else
        								{
        									Log.d("ConnectedThreadRT", "run()--> Received data IS NOT GEOREFERENCED");
        									
        									//*********** if gps on sensor box is not working now, activate smartphone gps **********
        									if(!Utils.getGpsTrackingOn(mContext))
        									{
        										Utils.gpsTrackServIntent = new Intent(mContext,GpsTrackingService.class);
        									    mContext.startService(Utils.gpsTrackServIntent);
        									}
        									
        									//*********** try to get phone gps coords ***********
        									Location phoneLocation = Utils.lastPhoneLocation;
        									boolean phoneGps = false;
        									double phoneAccuracy = 0;       									
        									if(phoneLocation != null)
        									{
        										//if smartphone coords are not older than about 3 sec save them
        										if(Math.abs(phoneLocation.getTime()-sysTs) <= 3000)
        										{
        											values[4] = String.valueOf(phoneLocation.getLatitude());
        											values[6] = String.valueOf(phoneLocation.getLongitude());
        									
        											phoneAccuracy = phoneLocation.getAccuracy();      											
        											phoneGps = true;
        										}									
        									}
        									//if I don't have valid phone coords, put lat/lon values to '0.0'
        									else
        									{
        										Log.d("ConnectedThreadRT", "run()--> phone location is null!");
        										values[4] = "0.0";
        										values[6] = "0.0";
        									}
        									
        									//*********** check for network location. This is in addition to phone/sensor box gps data ***********
        									Location networkLocation = Utils.lastNetworkLocation;
        									double[] networkLocArray = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //lat, lon, accuracy, bearing, speed, altitude
        									long networkTs = 0;
        									
        									if(networkLocation != null)
        									{
        										long coordsTs = networkLocation.getTime();
        										
        										//if network data location are not older then 5 minutes, save them on record
        										if(Math.abs(coordsTs-sysTs) <= Constants.FIVE_MINS)
        										{
        											networkLocArray[0] = networkLocation.getLatitude();
        											networkLocArray[1] = networkLocation.getLongitude();
        											networkLocArray[2] = networkLocation.getAccuracy();
        											networkLocArray[3] = networkLocation.getBearing();
        											networkLocArray[4] = networkLocation.getSpeed();
        											networkLocArray[5] = networkLocation.getAltitude();
        											
        											networkTs = networkLocation.getTime();
        										}
        									}
        									
        									double[] valuesArray = fillValues(values);	

        									//********** if received data is georeferenced, boxTs is a valid data and save it **********
        									try
        									{
        										boxTs = Utils.getTsFromBoxTimestampFields(values[0], values[1]);
        									}
        									catch(Exception e)
        									{
        										e.printStackTrace();
        									}
        									
        									//********** calculate Air Quality Index for actual array of values **********
        									Utils.calcBlackCarbon(valuesArray);
        									
        									//********** save record **********
        									if(phoneGps) //phone gps data (plus additional eventual network gps data)
        									{
        										Log.d("ConnectedThreadRT", "run()--> phone gps: "+Utils.lastPhoneLocation.getLatitude()+", "+Utils.lastPhoneLocation.getLongitude());
    											Log.d("ConnectedThreadRT", "run()--> box gps: NOT AVAILABLE");
    											Log.d("ConnectedThreadRT", "run()--> net gps: "+networkLocArray[0]+", "+networkLocArray[1]);
    											
    											mSourceSessionSeed = values[19];
    											mSourceSessionNumber = Integer.valueOf(values[18]); //ex trackId in firmware v1.17

    											mBoxMacAddr = values[20];					
    											if((values[20] != null)&&(values[20].length() == 12))
    											{
    												mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
    														values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
    														":"+values[20].substring(10);
    											}
    											
    											mDbManager.saveRecord(sysTs, boxTs, valuesArray, mSessionId, Constants.LOCALIZATION[1], Constants.GPS_PROVIDERS[1], phoneAccuracy, networkLocArray, networkTs, 
    													mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumber, 
    													Utils.lastPhoneLocation.getLatitude(), Utils.lastPhoneLocation.getLongitude(), Utils.lastPhoneLocation.getAccuracy(), 
    													Utils.lastPhoneLocation.getBearing(), Utils.lastPhoneLocation.getSpeed(), Utils.lastPhoneLocation.getAltitude(), Utils.lastPhoneLocation.getTime(), 
    													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0,
    													networkLocArray[3], networkLocArray[4], networkLocArray[5], 
    													mBoxMacAddr);    

    											if(Utils.firstAvailableLocation[0] == 0.0)
            									{
            										Utils.firstAvailableLocation[0] = valuesArray[0];
            										Utils.firstAvailableLocation[1] = valuesArray[1];
            										
            										Log.d("ConnectedThreadRT", "run()--> first available location comes from SMARTPHONE");
            										
            										//after the first location data is available, download sensor box
            										//calibration model data of the nearest city to this fix and parse it
            										new GetCalibrationDataTask().execute();
            									}
        									}
        									else
        									{
        										if(networkTs > 0) //only network gps data
        										{
        											//if record doesn't have gps data, use network gps data as official record gps value
        											valuesArray[0] = networkLocArray[0];
        											valuesArray[1] = networkLocArray[1];

        											Log.d("ConnectedThreadRT", "run()--> phone gps: NOT AVAILABLE");
        											Log.d("ConnectedThreadRT", "run()--> box gps: NOT AVAILABLE");
        											Log.d("ConnectedThreadRT", "run()--> net gps: "+networkLocArray[0]+", "+networkLocArray[1]);
  	
        											mSourceSessionSeed = values[19];
        											mSourceSessionNumber = Integer.valueOf(values[18]); //ex trackId in firmware v1.17

        											mBoxMacAddr = values[20];					
        											if((values[20] != null)&&(values[20].length() == 12))
        											{
        												mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
        														values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
        														":"+values[20].substring(10);
        											}
        											
        											mDbManager.saveRecord(sysTs, boxTs, valuesArray, mSessionId, Constants.LOCALIZATION[1], Constants.GPS_PROVIDERS[2], phoneAccuracy, networkLocArray, networkTs, 
        													mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumber, 
        													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 
        													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0,
        													networkLocArray[3], networkLocArray[4], networkLocArray[5],
        													mBoxMacAddr);
        												
                									if(Utils.firstAvailableLocation[0] == 0.0)
                									{
                										Utils.firstAvailableLocation[0] = valuesArray[0];
                										Utils.firstAvailableLocation[1] = valuesArray[1];
                										
                										Log.d("ConnectedThreadRT", "run()--> first available location comes from NETWORK PROVIDER");
                										
                										//after the first location data is available, download sensor box
                										//calibration model data of the nearest city to this fix and parse it
                										new GetCalibrationDataTask().execute();
                									}
        										}
        										else //if record doesn't have sensorbox, phone, network gps data, put gps provider filled to none
        										{
        											Log.d("ConnectedThreadRT", "run()--> phone gps: NOT AVAILABLE");
        											Log.d("ConnectedThreadRT", "run()--> box gps: NOT AVAILABLE");
        											Log.d("ConnectedThreadRT", "run()--> net gps: NOT AVAILABLE");

        											mSourceSessionSeed = values[19];
        											mSourceSessionNumber = Integer.valueOf(values[18]); //ex trackId in firmware v1.17

        											mBoxMacAddr = values[20];					
        											if((values[20] != null)&&(values[20].length() == 12))
        											{
        												mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
        														values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
        														":"+values[20].substring(10);
        											}
        											
        											mDbManager.saveRecord(sysTs, boxTs, valuesArray, mSessionId, Constants.LOCALIZATION[1], Constants.GPS_PROVIDERS[3], phoneAccuracy, networkLocArray, networkTs, 
        													mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumber, 
        													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 
        													0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0,
        													0.0, 0.0, 0.0, 
        													mBoxMacAddr);        											
        										}
        									}
        								}
        							}
        							else
        								Log.d("ConnectedThreadRT", "run()--> REAL TIME message incomplete! # of fields: " +values.length);
        						}
        						/*
        						else if(((buffer[bytesRead-7]) == Constants.historyTagClose[0]) && (buffer[bytesRead-6] == Constants.historyTagClose[1]) && (buffer[bytesRead-5] == Constants.historyTagClose[2])
       								 && (buffer[bytesRead-4] == Constants.historyTagClose[3]) && (buffer[bytesRead-3] == Constants.historyTagClose[4]))
        						{
        							Log.d("ConnectedThread", "run()--> history data ");

        						}
        						else
        						{
        							Log.d("ConnectedThread", "run()--> not recognized data");
        						}*/
        					}
        					
        					lastReadIndex = bytesRead;
        				} 				
        			}
    			}
    			//this exception happens when bluetooth connection falls
    			catch(IOException ex)
    			{
    				ex.printStackTrace();
    						
    				connectionLost();
    				break; //if connection fails, exit from while
    			}
    			catch(NumberFormatException e)
    			{
    				e.printStackTrace();
    				Log.d("BluetoothManager", "Error on following received message: " +message);
    			}
    		}
    		Looper.loop();
		}
		
		public void cancel()
		{
			mTransferON = false;
			
			try
			{
				//delayed socket closing; in this way while cycle can't read socket when this is closed
				new Handler().postDelayed(new CloseSocketDelayedRunnable(mmSocket), 1500);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}	
	}
	
	private class CloseSocketDelayedRunnable implements Runnable
	{
		private final BluetoothSocket mSocket;
		
		public CloseSocketDelayedRunnable(BluetoothSocket socket)
		{
			mSocket = socket;
		}
		
		@Override
		public void run()
		{
			try
			{
				mSocket.close();
				
				Log.d("CloseSocketDelayedRunnable", "close socket");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	};
		
	/******************** RIEMPIMENTO ARRAY VALORI ********************************************/
	
	public double[] fillValues(String[] values) throws NumberFormatException
	{
		
		double[] valuesArray = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
		
		//lat
		if(!values[4].equals(""))
			valuesArray[0] = Double.valueOf(values[4]);
		
		//lon
		if(!values[6].equals(""))
			valuesArray[1] = Double.valueOf(values[6]);

		//co
		if(!values[8].equals(""))
			valuesArray[2] = Double.valueOf(values[8]);						
		if(!values[9].equals(""))
			valuesArray[3] = Double.valueOf(values[9]);	
		if(!values[10].equals(""))
			valuesArray[4] = Double.valueOf(values[10]);
		if(!values[11].equals(""))
			valuesArray[5] = Double.valueOf(values[11]);

		//no2
		if(!values[12].equals(""))
			valuesArray[6] = Double.valueOf(values[12]);	
		if(!values[13].equals(""))
			valuesArray[7] = Double.valueOf(values[13]);
		
		//voc
		if(!values[14].equals(""))
			valuesArray[8] = Double.valueOf(values[14]);	
		
		//o3
		if(!values[15].equals(""))
			valuesArray[9] = Double.valueOf(values[15]);	
		
		//temp
		if(!values[16].equals(""))
			valuesArray[10] = Double.valueOf(values[16]);
		//hum
		if(!values[17].equals(""))
		{
			//Log.d("BluetootManager", "fillValues()--> " +values[16]);
			
			//to correct a possible bug of substring! do not remove
			int index = values[17].indexOf("<");
			if(index != -1)
				values[17] = values[17].substring(0, index);
			valuesArray[11] = Double.parseDouble(values[17]);
		}
		return valuesArray;
	}
	
	public class SaveHistoryTask extends AsyncTask<String[], Integer, Integer>
	{
		private DbManager mDbManager;
		private String mSourceSessionSeed = "";
		private int mSourceSessionNumber = 0;
		private int mSourcePointNumber = 0;
		private SemanticSessionDetails mSemantic;
		private String mSemanticSessionSeed = "";
		private int mSemanticSessionNumber = 0;
		private int mSemanticPointNumberStart = 0;		
		private String mBoxMacAddr;
		
		double boxAcc = 0.0, boxBear = 0.0, boxSpeed = 0.0, boxAltitude = 0.0;
		int boxNumSat = 0;
		
		@Override
		protected Integer doInBackground(String[]... params) 
		{			
			//split history buffer in <hi> strings
			String[] historyRecordsStr = params[0];
			
			Log.d("SaveHistoryTask", "doInBackground()--> # received records: " +historyRecordsStr.length);

			mDbManager = DbManager.getInstance(null);

			for(int i = 0; i < historyRecordsStr.length; i++)
			{
				String msg = historyRecordsStr[i];

				//remove <hi> and </hi> tags
				int i1 = msg.indexOf(">");
				int i2 = msg.lastIndexOf("<");
				if((i1 != -1)&&(i2 != -1))
					msg = msg.substring(i1+1, i2); //in some cases, substring doens't remove final tag </hi>
							
				//Log.d("SaveHistoryTask", "msg post: " +msg);
				
				//Log.d("BluetoothManager", "saveHistoryRecordsOnDb()--> " +historyRecordsStr[i]);
				String[] values = msg.split(",");
				
				//from sensorbox firmware >= 1.18
				if(values.length == 26)
				{	
					//Log.d("SaveHistoryTask", "doInBackground()--> history data - fields number: " +values.length);
					
					long boxTs = 0;
					String gpsProvider = "";
					
					//if history records contains valid gps coordinates, then it contains also valid sensorbox timestamp
					if(values[3].equalsIgnoreCase("A"))				
						gpsProvider = Constants.GPS_PROVIDERS[0]; //sensorbox gps provider
					else
						gpsProvider = Constants.GPS_PROVIDERS[3]; //no gps data
					
					//in box cases I convert sensor box date into utc timestamp (from AirProbe 1.1.3)
					try
					{
						boxTs = Utils.getTsFromBoxTimestampFields(values[0], values[1]);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
					//if timestamp of actual history records is almost five minutes after timestamp of precedent history 
					//record, this is a new track. So I change current session Id and save it in new records from now
					if(Math.abs(boxTs - mPrecBoxTs) >= Constants.mHistoryTracksDistance)
					{
						String sessionId = String.valueOf(boxTs);
						mCurrentHistSessionId = sessionId;
						
						//Log.d("SaveHistoryTask", "doInBackground()--> new session id: " +mCurrentSessionId);
					}
					//else if this is first history records (mPrecBoxTs is zero) then save its timestamp as current session id
					else if(mPrecBoxTs == 0)
					{
						String sessionId = String.valueOf(boxTs);
						mCurrentHistSessionId = sessionId;
					}
					
					//save ts of actual timestamp 
					mPrecBoxTs = boxTs;					

					Record newHistRec = null;
					
					mSourcePointNumber = Integer.valueOf(values[2]); //sinceOn field (also called sb_time_on)

					mSourceSessionSeed = values[19];
					mSourceSessionNumber = Integer.valueOf(values[18]);

					mSemantic = mDbManager.checkIfActualSourceIdBelongsToASemanticSessionEntry(mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber); 

					if(mSemantic != null)
					{
						mSemanticSessionSeed = mSemantic.mSemanticSessionSeed;
						mSemanticSessionNumber = mSemantic.mSourceSessionNumber;
						mSemanticPointNumberStart = mSourcePointNumber - mSemantic.mPointNumberStart;
					}
					else
					{
						mSemanticSessionSeed = "";
						mSemanticSessionNumber = 0;
						mSemanticPointNumberStart = 0;
					}

					mBoxMacAddr = values[20];					
					if((values[20] != null)&&(values[20].length() == 12))
					{
						mBoxMacAddr = values[20].substring(0, 2)+":"+values[20].substring(2, 4)+":"+
								values[20].substring(4, 6)+":"+values[20].substring(6, 8)+":"+values[20].substring(8, 10)+
								":"+values[20].substring(10);
					}
	
					try {
						boxAcc = Double.valueOf(values[22]);
					}catch(Exception e){e.printStackTrace();};
					try {
						boxBear = Double.valueOf(values[25]);
					}catch(Exception e){e.printStackTrace();};
					try {
						boxSpeed = Double.valueOf(values[24]);
					}catch(Exception e){e.printStackTrace();};
					try {
						boxAltitude = Double.valueOf(values[22]);
					}catch(Exception e){e.printStackTrace();};
					try {
						boxNumSat = Integer.valueOf(values[21]);
					}catch(Exception e){e.printStackTrace();};
					
					newHistRec = new Record(0, boxTs, fillValues(values), "offline_"+mCurrentHistSessionId, Constants.LOCALIZATION[1], gpsProvider, 0, new double[]{0.0, 0.0, 0.0}, 0, 
							mSourceSessionSeed, mSourceSessionNumber, mSourcePointNumber, mSemanticSessionSeed, mSemanticSessionNumber, mSemanticPointNumberStart, 
							0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 
							Double.valueOf(values[4]), Double.valueOf(values[6]), boxAcc, boxBear, 0.51 * boxSpeed, boxAltitude, boxNumSat, 
							0.0, 0.0, 0.0, 
							mBoxMacAddr);
					
					newHistRec.mUserData2 = Constants.mFirmwareVersion;
					newHistRec.mUserData3 = mBoxMacAddr;
					newHistRec.mUniquePhoneId = Constants.mUniquePhoneId;
					newHistRec.mPhoneModel = Constants.mPhoneModel;
					
					mHistoryRecordsSerie.add(newHistRec);	
				}
				else
				{
					Log.d("SaveHistoryTask", "doInBackground()--> History record NOT valid!");
					Log.d("SaveHistoryTask", "doInBackground()--> msg lost: " +msg);
					Utils.lostHR++;
				}
			}
			
			Record[] recordsToSaveArray = new Record[mHistoryRecordsSerie.size()];			
			mHistoryRecordsSerie.toArray(recordsToSaveArray);
			
			int size = recordsToSaveArray.length;
			
			if(mDbManager.saveHistoryRecordsSerie(recordsToSaveArray))
			{
				//save received history records
				Utils.counterHR += size;
				//clear array to avoid duplicated records
				mHistoryRecordsSerie.clear();

				Log.d("SaveHistoryTask", "doInBackground()--> history records saved count: " +Utils.counterHR);
			}
			else
				Log.d("SaveHistoryTask", "doInBackground()--> error saving history records");
			
			return 1;
		}
		
		public double[] fillValues(String[] values) throws NumberFormatException
		{
			double[] valuesArray = {0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
			
			//lat
			if(!values[4].equals(""))
				valuesArray[0] = Double.valueOf(values[4]);
			
			//lon
			if(!values[6].equals(""))
				valuesArray[1] = Double.valueOf(values[6]);

			//co
			if(!values[8].equals(""))
				valuesArray[2] = Double.valueOf(values[8]);						
			if(!values[9].equals(""))
				valuesArray[3] = Double.valueOf(values[9]);	
			if(!values[10].equals(""))
				valuesArray[4] = Double.valueOf(values[10]);
			if(!values[11].equals(""))
				valuesArray[5] = Double.valueOf(values[11]);

			//no2
			if(!values[12].equals(""))
				valuesArray[6] = Double.valueOf(values[12]);	
			if(!values[13].equals(""))
				valuesArray[7] = Double.valueOf(values[13]);
			
			//voc
			if(!values[14].equals(""))
				valuesArray[8] = Double.valueOf(values[14]);	
			
			//o3
			if(!values[15].equals(""))
				valuesArray[9] = Double.valueOf(values[15]);	
			
			//temp
			if(!values[16].equals(""))
				valuesArray[10] = Double.valueOf(values[16]);
			//hum
			if(!values[17].equals(""))
			{
				//Log.d("BluetootManager", "fillValues()--> " +values[16]);
				
				//to correct a possible bug of substring! do not remove
				int index = values[17].indexOf("<");
				if(index != -1)
					values[17] = values[17].substring(0, index);
				valuesArray[11] = Double.parseDouble(values[17]);
			}
			return valuesArray;
		}
	}
	
	//read max.txt and min.txt, parse all rows and get also sensor box mac addresses
	private class GetMaxMinTask extends AsyncTask<Void, Void, Void>
	{
		private final boolean D = false;
		
		@Override
		protected Void doInBackground(Void... params) 
		{	
			try 
			{
				List<String>maxLines = readFile("max.txt");
				List<String>minLines = readFile("min.txt");
				
				if((maxLines != null)&&(maxLines.size() > 0))
				{					
					Utils.max_matrix = new double[maxLines.size()-1][maxLines.get(0).split(",").length-1];
					
					//each row corresponds to a different sensor
					for(int i = 1; i < maxLines.size(); i++)
					{
						String row = maxLines.get(i);
						String[] rowValues = row.split(",");
						
						//each column (from 1 to end) corresponds to a different sensor box
						for(int j = 1; j < rowValues.length; j++)
							Utils.max_matrix[i-1][j-1] = Double.valueOf(rowValues[j]);						
					}
				}
				else
					Utils.calibrationDataLoaded = false;
				
				//PARSING OF min.txt FILE
				
				if((minLines != null)&&(minLines.size() > 0))
				{
					String[] firstLine = minLines.get(0).split(",");
					
					int cols = firstLine.length-1;
					
					//parsing fo first row, that contains mac address of sensor boxes
					Utils.addresses = new String[cols];
					
					for(int j = 0; j < cols; j++)
						Utils.addresses[j] = firstLine[j+1];
					
					Utils.min_matrix = new double[minLines.size()-1][cols];
					
					//each row corresponds to a different sensor
					for(int i = 1; i < minLines.size(); i++)
					{
						String row = minLines.get(i);
						String[] rowValues = row.split(",");
						
						//each column (from 1 to end) corresponds to a different sensor box
						for(int j = 1; j < rowValues.length; j++)
							Utils.min_matrix[i-1][j-1] = Double.valueOf(rowValues[j]);	
					}
					
					if(D)
					{
						//print addresses vector
						String str = "";
						for(int i = 0; i < Utils.addresses.length; i++)
							str += Utils.addresses[i]+ " ";
						Log.d("GetMaxMinTask", "doInBackground()--> addresses vector: " + str);
					}
				}
				else
					Utils.calibrationDataLoaded = false;

			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			return null;
		}
		
		//read a file and returns the lines of it
		private List<String> readFile(String filename) throws FileNotFoundException, IOException
	    {
			boolean fileNotFound = false;
			
			InputStream is = null;
	
			try
			{
				//is = mContext.getResources().getAssets().open(dirName+"/"+filename);			
				is = mContext.getResources().getAssets().open(filename);
			}
			catch(FileNotFoundException e)
			{
				//Log.d("BluetoothManager", "File Not Found Exception - complete filename: " +dirName+"/"+filename);
				Log.d("GetMaxMinTask", "File Not Found Exception - complete filename: " +filename);
				fileNotFound = true;
			}
			
			if((is == null)||(fileNotFound))
			{
				//Log.d("BluetoothManager", "loading default sensor box profile 00066645D635");
				//is = mContext.getResources().getAssets().open("00066645D635/"+filename);
				return null;
			}
			
	    	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    	
	    	List<String> fileLines = new ArrayList<String>();
	    	
	        try 
	        {	            
	            String line = br.readLine();

	            while (line != null) 
	            {
	            	fileLines.add(line);
	                line = br.readLine();
	            } 
	        } 
	        finally 
	        {
	            br.close();          
	        }
	        return fileLines;
	    }
	}
	
	//obtain from url calibration model data for connected sensor box, selected from its mac address
	private class GetCalibrationDataTask extends AsyncTask<Void, Void, Void>
	{
		private final boolean D = false;
		
		@Override
		protected Void doInBackground(Void... params) 
		{	
			//find nearest city
			int cityIndex = findNearestCity(Utils.firstAvailableLocation);
			
			//save in global var the name of the nearest city
			Utils.nearestCityName = Constants.CITY_NAMES[cityIndex];
			
			try 
			{
				List<String>modelParamsLines = readFile(Constants.MODEL_PARAMS_NAMES[cityIndex]);
				//List<String>maxLines = readFile("max.txt");
				//List<String>minLines = readFile("min.txt");
				
				Utils.calibrationDataLoaded = true;
				
				//PARSING OF modelParams.txt FILE
				
				if((modelParamsLines != null)&&(modelParamsLines.size() > 0))
				{
					//1 - read line 0, parse it and get vector b1
					String b1Str = modelParamsLines.get(0);
					String[] b1Values = b1Str.split(",");					
					for(int i = 0; i < b1Values.length; i++)
						Utils.b1[i] = Double.valueOf(b1Values[i]);
					
					//2 - read lines [1-10], parse each one and fill w1 matrix
					for(int i = 1; i < 11; i++)
					{
						String w1LineStr = modelParamsLines.get(i);
						String[] w1LineValues = w1LineStr.split(",");
						for(int j = 0; j < w1LineValues.length; j++)
							Utils.w1[i-1][j] = Double.valueOf(w1LineValues[j]);
					}
					
					//3 - read line [11], parse it and get vector w2
					String w2Str = modelParamsLines.get(11);
					String w2Values[] = w2Str.split(",");
					for(int i = 0; i < w2Values.length; i++)
						Utils.w2[i] = Double.valueOf(w2Values[i]);
					
					//4 - read line [12] and get value b2
					String b2Str = modelParamsLines.get(12);
					String b2Value = b2Str.split(",")[0];
					Utils.b2 = Double.valueOf(b2Value);
				}
				else
					Utils.calibrationDataLoaded = false;
				
				//PARSING OF max.txt FILE
	/*			
				if((maxLines != null)&&(maxLines.size() > 0))
				{					
					Utils.max_matrix = new double[maxLines.size()-1][maxLines.get(0).split(",").length-1];
					
					//each row corresponds to a different sensor
					for(int i = 1; i < maxLines.size(); i++)
					{
						String row = maxLines.get(i);
						String[] rowValues = row.split(",");
						
						//each column (from 1 to end) corresponds to a different sensor box
						for(int j = 1; j < rowValues.length; j++)
							Utils.max_matrix[i-1][j-1] = Double.valueOf(rowValues[j]);						
					}
				}
				else
					Utils.calibrationDataLoaded = false;
				
				//PARSING OF min.txt FILE
				
				if((minLines != null)&&(minLines.size() > 0))
				{
					String[] firstLine = minLines.get(0).split(",");
					
					int cols = firstLine.length-1;
					
					//parsing fo first row, that contains mac address of sensor boxes
					Utils.addresses = new String[cols];
					
					for(int j = 0; j < cols; j++)
						Utils.addresses[j] = firstLine[j+1];
					
					Utils.min_matrix = new double[minLines.size()-1][cols];
					
					//each row corresponds to a different sensor
					for(int i = 1; i < minLines.size(); i++)
					{
						String row = minLines.get(i);
						String[] rowValues = row.split(",");
						
						//each column (from 1 to end) corresponds to a different sensor box
						for(int j = 1; j < rowValues.length; j++)
							Utils.min_matrix[i-1][j-1] = Double.valueOf(rowValues[j]);	
					}
				}
				else
					Utils.calibrationDataLoaded = false;
*/
				//print debug values
				if(D)
				{
					//print b1 vector
					String str = "";				
					for(int i = 0; i < Utils.b1.length; i++)
						str += Utils.b1[i]+" ";
					Log.d("GetCalibrationDataTask", "doInBackground()--> b1 vector: " + str);
					
					//print w1 matrix
					for(int i = 0; i < Utils.w1.length; i++)
					{
						str = "";
						
						for(int j = 0; j < Utils.w1[i].length; j++)
							str += String.valueOf(Utils.w1[i][j])+ " ";
						
						Log.d("GetCalibrationDataTask", "doInBackground()--> w1 matrix row["+i+"] = "+str);
					}
					
					//print w2 vector
					str = "";
					for(int i = 0; i < Utils.w2.length; i++)
						str += Utils.w2[i]+ " ";
					Log.d("GetCalibrationDataTask", "doInBackground()--> w2 vector: " + str);
					
					//print b2 value
					Log.d("GetCalibrationDataTask", "doInBackground()--> b2 value: " + Utils.b2);
					
					//print addresses vector
					str = "";
					for(int i = 0; i < Utils.addresses.length; i++)
						str += Utils.addresses[i]+ " ";
					Log.d("GetCalibrationDataTask", "doInBackground()--> addresses vector: " + str);
					
					//print max matrix
					for(int i = 0; i < Utils.max_matrix.length; i++)
					{
						str = "";
						
						for(int j = 0; j < Utils.max_matrix[i].length; j++)
							str += String.valueOf(Utils.max_matrix[i][j])+ " ";
						
						Log.d("GetCalibrationDataTask", "doInBackground()--> max matrix row["+i+"] = "+str);
					}
					
					//print min matrix
					for(int i = 0; i < Utils.min_matrix.length; i++)
					{
						str = "";
						
						for(int j = 0; j < Utils.min_matrix[i].length; j++)
							str += String.valueOf(Utils.min_matrix[i][j])+ " ";
						
						Log.d("GetCalibrationDataTask", "doInBackground()--> min matrix row["+i+"] = "+str);
					}
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				Utils.calibrationDataLoaded = false;
			}
			return null;
		}
		
		//read a file and returns the lines of it
		private List<String> readFile(String filename) throws FileNotFoundException, IOException
	    {
			boolean fileNotFound = false;
			
			InputStream is = null;
	
			try
			{
				//is = mContext.getResources().getAssets().open(dirName+"/"+filename);			
				is = mContext.getResources().getAssets().open(filename);
			}
			catch(FileNotFoundException e)
			{
				//Log.d("BluetoothManager", "File Not Found Exception - complete filename: " +dirName+"/"+filename);
				Log.d("GetCalibrationDataTask", "File Not Found Exception - complete filename: " +filename);
				fileNotFound = true;
			}
			
			if((is == null)||(fileNotFound))
			{
				//Log.d("BluetoothManager", "loading default sensor box profile 00066645D635");
				//is = mContext.getResources().getAssets().open("00066645D635/"+filename);
				return null;
			}
			
	    	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    	
	    	List<String> fileLines = new ArrayList<String>();
	    	
	        try 
	        {	            
	            String line = br.readLine();

	            while (line != null) 
	            {
	            	fileLines.add(line);
	                line = br.readLine();
	            } 
	        } 
	        finally 
	        {
	            br.close();          
	        }
	        return fileLines;
	    }
		
		//restituisce indice sull'array Constants.CITY_NAMES della città più vicina alla coppia di coordinate passate
		private int findNearestCity(double[] loc)
		{
			float minDistance = 0;
			float[] results = new float[3];
			int minDistanceCityIndex = 0;
			
			for(int i = 0; i < Constants.CITY_NAMES.length; i++)
			{
				Location.distanceBetween(Constants.CITIES_COORDS[i][0], Constants.CITIES_COORDS[i][1], loc[0], loc[1], results);
				
				Log.d("GetCalibrationDataTask", "findNearestCity()--> city: "+Constants.CITY_NAMES[i]+" - distance between city coords ("+Constants.CITIES_COORDS[i][0]+","+Constants.CITIES_COORDS[i][1]+") and first location: "+loc[0]+", "+loc[1]+" is: "+results[0]);
				if(i == 0)
				{
					minDistance = results[0];
					minDistanceCityIndex = i;
				}
				else if(results[0] < minDistance)
				{
					minDistance = results[0];
					minDistanceCityIndex = i;
				}				
			}
			Log.d("GetCalibrationDataTask", "findNearestCity()--> minimum distance is: "+minDistance+ " from city: "+Constants.CITY_NAMES[minDistanceCityIndex]);
			return minDistanceCityIndex;
		}
	};    
}
