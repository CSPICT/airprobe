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
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.SemanticSessionDetails;
import org.csp.everyaware.gps.GpsTrackingService;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BluetoothHistoryManager 
{    
    private int mState; //current connection status
    
	private final BluetoothAdapter mBluetoothAdapter;
	private Context mContext;
	private Handler mStartHistoryHandler;
	private Handler mDownloadHandler;
	
	//it opens RFCOMM socket and estabilishes a remote device connection on socket
	private ConnectThread mConnectThread;

	//it reads received data on socket from remote device
	private ConnectedThread mConnectedThread;
	
	//to close while cycle in run() method of ConnectedThread
	private boolean mTransferON;
	
	//Patrick
	private static volatile BluetoothHistoryManager mBluetoothManager;
	
	private DbManager mDbManager;
	
	private String mHistoryBuffer; //containes raw history records
	
	private List<Record>mHistoryRecordsSerie = null; //containes a list of history records to be saved on db
	
	private boolean mHistoryDownloadFinished;
	
	private static long mPrecBoxTs;
	private static String mCurrentHistSessionId;
	
	public static BluetoothHistoryManager getInstance(Context ctx, Handler handler)
	{
		if(mBluetoothManager == null)
			mBluetoothManager = new BluetoothHistoryManager(ctx, handler);
		
		return mBluetoothManager;
	}
	
	private BluetoothHistoryManager(Context ctx, Handler handler)
	{
		Log.d("BluetoothHistoryManager", "BluetoothHistoryManager()");
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mContext = ctx;		
		mStartHistoryHandler = handler;
		
		mPrecBoxTs = 0;
		mCurrentHistSessionId = Utils.getSessionId(mContext);
		
		mDbManager = DbManager.getInstance(mContext);
		mDbManager.openDb();

		setState(Constants.STATE_NONE);
	}
	
	public void setDownloadHandler(Handler handler)
	{
		Log.d("BluetoothHistoryManager", "setDownloadHandler()");
		
		mDownloadHandler = handler;
	}

	//starts ConnectThread
	public synchronized void connect(BluetoothDevice device)
	{
		Log.d("BluetoothHistoryManager", "connect()--> Connecting to: " +device);
		
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
		Log.d("BluetoothHistoryManager", "connected()");
		
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
		
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		
		setState(Constants.STATE_CONNECTED);
  		
	}

	//to track failure of attempt to connect to remote device
	//UI main thread is advised
    private void connectionFailed() 
    {
    	Log.d("BluetoothHistoryManager", "connectionFailed()");
    	
        setState(Constants.STATE_NONE);

        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHistoryHandler == null)
        		return;
        	
	        Message msg = mStartHistoryHandler.obtainMessage(Constants.CONNECTION_FAILED);
	        mStartHistoryHandler.sendMessage(msg);
	        return;
        }
    }
    
    //to track losed bluetooth connection
    public void connectionLost()
    {
    	Log.d("BluetoothHistoryManager", "connectionLost()");
    	
    	setState(Constants.STATE_NONE);
    	
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mStartHistoryHandler == null)
        		return;
        	
        	Log.d("BluetoothHistoryManager", "connectionLost()--> sending CONNECTION_LOST message to start history handler");
        	
        	Message msg = mStartHistoryHandler.obtainMessage(Constants.CONNECTION_LOST);
        	mStartHistoryHandler.sendMessage(msg);
        	return;
        }
    }
    
    private synchronized void setState(int state)
    {
    	Log.d("BluetoothHistoryManager", "setState()");
    	mState = state;
    	
        if(Utils.getStep(mContext) == Constants.START)
        {
        	Log.d("BluetoothHistoryManager", "setState()--> going to send message to start history handler");
        	
        	if(mStartHistoryHandler == null)
        		return;
        	
        	Message msg = mStartHistoryHandler.obtainMessage(mState);
        	mStartHistoryHandler.sendMessage(msg);  	
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
    	Log.d("BluetoothHistoryManager", "sensorBoxMacNotRead()");
    	
        if(Utils.getStep(mContext) == Constants.START)
        {
        	if(mDownloadHandler == null)
        		return;
        	
	        Message msg = mDownloadHandler.obtainMessage(Constants.SENSOR_BOX_MAC_NOT_READ);	        
	        mDownloadHandler.sendMessage(msg);
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
			Log.d("ConnectThread", "ConnectThread()");
			
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
				
				Log.d("ConnectThread", "run()--> socket connected ");
			}
			//failure on opening connection
			catch(IOException e) 
			{
				e.printStackTrace();
				
				Log.d("ConnectThread", "run()--> IOException ");
				
				connectionFailed();
				
				try
				{
					if(mmSocket != null)
						mmSocket.close();
				}
				catch(IOException e2)
				{
					e.printStackTrace();
					Log.d("ConnectThread", "run()--> IOException2 ");
				}
				
				return; //exit from run()
			}
			
			//ConnectThread reset
			synchronized(BluetoothHistoryManager.this)
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
		private int mNumOfHistoryRecords;
		
		boolean transmittingHNR = false; //transimitting HistoryNumberRecords
		//Patrick
		private byte[] buffer;
		//----
		public ConnectedThread(BluetoothSocket socket)
		{
			//Patrick
			buffer = new byte[Constants.BLUETOOTH_BUFFER_SIZE];
			//--------
			Log.d("ConnectedThread", "ConnectedThread()");
			
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			
			mTransferON = true;

			mNumOfHistoryRecords = 0;
			
			//important initializations
			Utils.counterHR = 0; //received history records			
			Utils.lostHR = 0; //lost history records
			//mHistoryBuffer = ""; //raw buffer history records		
			mHistoryRecordsSerie = new ArrayList<Record>();
			mHistoryDownloadFinished = false;			
			
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
		}
		
		public void run()
		{
			Looper.prepare();
			
    		//receiving buffer
    		//byte[] buffer = new byte[Constants.BLUETOOTH_BUFFER_SIZE]; //for a set of 50 records, buffer size must be about 6500 bytes
    		int bytesRead = 0;
    		
    		boolean connectionFailed = false;
    		//hardware informations from sensor box
    		String infoMsg = "";
    		//contantins number of history records on sd card of sensor box
    		String numberHR = "";;

    		/* 1 - ASK for infos to sensor box */

    		transmittingInfos = true;
    		
    		try
    		{
    			//sending of "%I" string to tell the sensor box that smartphone is ready to receive data
    			mmOutStream.write(Constants.askForInfo);
    			
    			//Log.d("ConnectedThreadHI", "run()--> SENT " +new String(Constants.askForInfo));
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
    				//Log.d("ConnectedThreadHI", "run()--> infoMsg: " +infoMsg);
    				
    				if((infoMsg.length() >= 4)&&(infoMsg.substring(infoMsg.length()-4, infoMsg.length()-2).equalsIgnoreCase("%K")))
    				{
    					transmittingInfos = false;
    					infoMsg = infoMsg.substring(0, infoMsg.length()-4); //toggle %K
    					Log.d("ConnectedThreadHI", "run()--> read box infos: " +infoMsg);
    					
    					String[] infos = infoMsg.split("\\*\\*");
    					if(infos != null)
    					{
    						for(int i = 0; i < infos.length; i++)
    						Log.d("ConnectedThreadHI", "run()--> infos: "+infos[i]);
    						
	    					//Save firmware version and mac address on static variable for continuous usage on saving record
	    					Constants.mFirmwareVersion = infos[0];
	    					Constants.mMacAddress = Utils.getDeviceAddress(mContext);

	    					Log.d("ConnectedThreadHI", "run()--> mac address: "+Constants.mMacAddress);
	    					
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
	    								
	    								Log.d("ConnectedThreadHI", "run()--> mac address recovered from info!--> mac: "+Constants.mMacAddress);
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
	    							Log.d("ConnectedThreadHI", "run()--> address["+j+"] = " +Utils.addresses[j]);
	    							
	    							if(Utils.addresses[j].equals(Constants.mMacAddress))
	    							{
	    								found = true;
	    								Utils.actual_sb_index = j;
	    								
	    								Log.d("ConnectedThreadHI", "run()--> found sensor box with address: " +Constants.mMacAddress+ " and index: " +Utils.actual_sb_index);
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
	    								
	    								//Log.d("ConnectedThreadHI", "run()--> max["+i+"] = " +Utils.max[i]+" - min["+i+"] = " +Utils.min[i]);
	    							}
	    						}
	    			
	    						Utils.mac_recognized = found;
	    					}

	    					//save received informations on shared prefs
	    					if(infos.length < 3)
	    					{
	    						Utils.setBoxInfoMsg("Firmware version: not read\nSensor models: not read", mContext);
	    					}
	    					else
	    						Utils.setBoxInfoMsg("Firmware version: V" +infos[0]+"\nSensor models: " +infos[2], mContext);
    					}
    					//if infos from sensor box has not read, comunicate it
    					else
    					{
    						
    					}
    				}
    			}
    			//this exception happens when bluetooth connection fails
    			catch(IOException ex)
    			{
    				ex.printStackTrace();
    						
    				connectionLost();
    				connectionFailed = true;
    				
    				try
    				{
    					if(mmSocket != null)
    						mmSocket.close();
    				}
    				catch(IOException e2)
    				{
    					e2.printStackTrace();
    				}
    				
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
		    		
		    	//Log.d("ConnectedThreadHI", "run()--> INVIATO " +new String(Constants.askForNumberHist)); 
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
	    				
	    			Log.d("ConnectedThreadHI", "numberHR parziale: " +numberHR);
	     			if((numberHR.length() >= 4)&&(numberHR.substring(numberHR.length()-4, numberHR.length()-2).equalsIgnoreCase("%K")))
	    			{
	     				transmittingHNR = false;
	     				numberHR = numberHR.substring(0, numberHR.length()-4); //toggle %K and slash r slash n
	     				
	     				Utils.numberHR = Integer.valueOf(numberHR);

	     				if(mDownloadHandler != null)
	     				{
	     					Log.d("ConnectedThreadHI", "run()--> sending TOTAL_HISTORY_NUM to download handler ");
	     					Message msg = mDownloadHandler.obtainMessage(Constants.TOTAL_HISTORY_NUM);
	     					mDownloadHandler.sendMessage(msg);
	     				}
	     				
	    				Log.d("ConnectedThreadHI", "run()--> number of history records on sensor box sdcard: " +Utils.numberHR);
	    			}
	    		}
	    		//this exception happens when bluetooth connection fails
	    		catch(IOException ex)
	    		{
	    			ex.printStackTrace();
	    						
	    			connectionLost();
	    			connectionFailed = true;
	    			
					try
					{
						if(mmSocket != null)
							mmSocket.close();
					}
					catch(IOException e2)
					{
						e2.printStackTrace();
					}
					
	    			break; //if connection fails, exit from while
	    		}
 				catch(NumberFormatException e)
 				{
 					e.printStackTrace();
 				}
		    }
    		
    		//if infos lack of some information, comunicate it
			if(Constants.mMacAddress.equals(""))
			{
				sensorBoxMacNotRead();
				return;
			}
			
	    	if(connectionFailed)
	    		return;
    		
    		/* 3 - ASK for history data to sensor box */
    		if(Utils.numberHR > 0)
    		{
		    	try
		    	{		
		    		//sending of string to tell the sensor box to stop to collect and save real time data
		    		mmOutStream.write(Constants.askForTurnOffRealTime);
		    		//Log.d("ConnectedThreadHI", "run()--> SENT " +new String(Constants.askForTurnOffRealTime));
		    		
		    		Log.d("ConnectedThreadHI", "run()--> WAIT 200 millisec ");
					Thread.currentThread().sleep(200);

		    		//sending of  string to tell the sensor box that smartphone is ready to receive history data	    		
		    		mmOutStream.write(Constants.askForHistory);				
			    	Log.d("ConnectedThreadHI", "run()--> SENT " +new String(Constants.askForHistory));
		    		
		    		bytesRead = 0;
		    	}
		    	catch(IOException e)
		    	{
		    		e.printStackTrace();
		    		
		    		Log.d("ConnectedThreadHI", "run()--> IOException1");
		    	}
	    		catch (InterruptedException e1) 
	    		{
					e1.printStackTrace();
					
					Log.d("ConnectedThreadHI", "run()--> InterruptedException");
				}
	
			    //listening on socket (for all the time the connection is up)
	    		while(mTransferON)
	    		{     			
	    			try
	    			{        				
		    			Log.d("ConnectedThreadHI", "run()--> bytesRead = "+bytesRead+ " buffer length = " +buffer.length+ " buffer length - bytesRead = " +(buffer.length-bytesRead));
		    			bytesRead += mmInStream.read(buffer, bytesRead, buffer.length-bytesRead);	//3rd parameter: the maximum number of bytes to store in buffer	    			

	        			//save on string buffer contents
	        			if(bytesRead != 0)
	        			{    					
	        				if((bytesRead >= 2) && (buffer[bytesRead-2] == 13) && (buffer[bytesRead-1] == 10)) // \r\n
	        				{       
	        					if((bytesRead >= 4) && (buffer[bytesRead-4] == Constants.endHistorySet[0]) && ((buffer[bytesRead-3]) == Constants.endHistorySet[1])) // %K
	        					{
	        						//create string array from buffer, last four bytes ('%' 'K' '\r' '\n') excluded
	        						String[] historyRecordsStr = new String(buffer, 0, bytesRead-4).split("\r\n");
	        							
	        						//reset byte count on buffer
	        						bytesRead = 0;
	        							
	        						mNumOfHistoryRecords = historyRecordsStr.length;
	        						if(mNumOfHistoryRecords == 50)
	        						{
	        							new SaveHistoryTask().execute(historyRecordsStr);
	        							
	        							if(mDownloadHandler != null)
	        							{
		        							Log.d("ConnectedThreadHI", "run()--> update progress bar - sending UPDATE_PROGRESS to download handler");
		        							//update progress bar 
		    								Message msg = mDownloadHandler.obtainMessage(Constants.UPDATE_PROGRESS);
		    								mDownloadHandler.sendMessage(msg);
	        							}
	        						}

	        						String command = "";
	        							
	    							if(mNumOfHistoryRecords < 10)
	    								command = new String(Constants.receivedRecordsNum)+"0"+String.valueOf(mNumOfHistoryRecords);
	    							else
	    								command = new String(Constants.receivedRecordsNum)+String.valueOf(mNumOfHistoryRecords);
	    								
	    							//inform sensor box of the number of records received (so sensor box can keep track of these record as sent)
	    							mmOutStream.write( command.getBytes() );
	    								
	    							//Log.d("ConnectedThreadHI", "run()--> SENT " +new String(command.getBytes())); 
	        					}
	        					else if((bytesRead >= 5) && (buffer[bytesRead-5] == Constants.endHistory[0]) && (buffer[bytesRead-4] == Constants.endHistory[1]) && (buffer[bytesRead-3] == Constants.endHistory[2])) // %KK
	            				{
        							//create string array from buffer, last five bytes ('%' 'K' 'K' '\r' '\n') excluded
	        						String[] historyRecordsStr = new String(buffer, 0, bytesRead-5).split("\r\n");
	        							
	        						mNumOfHistoryRecords = historyRecordsStr.length;
	        							
	        						if(mNumOfHistoryRecords > 0)
	        						{
	    								new SaveHistoryTask().execute(historyRecordsStr);
	    									
	    								//update progress bar 
	    								Message msg = mDownloadHandler.obtainMessage(Constants.FINISHED_HIST_DOWN);
	    							    mDownloadHandler.sendMessage(msg);
	    							        
		        						String command = "";
		        							
		    							if(mNumOfHistoryRecords < 10)
		    								command = new String(Constants.receivedRecordsNum)+"0"+String.valueOf(mNumOfHistoryRecords);
		    							else
		    								command = new String(Constants.receivedRecordsNum)+String.valueOf(mNumOfHistoryRecords);
		    								
		    							//inform sensor box of the number of records received (so sensor box can keep track of these record as sent)
		    							mmOutStream.write( command.getBytes() );
		    								
		    							//Log.d("ConnectedThreadHI", "run()--> SENT " +new String(command.getBytes())); 
	        						}
	        					}
	        					else
	        					{
	        						Log.d("ConnectedThreadHI", "run()--> update progress bar - Non so parsificare---------*****");
	        					}
	        				}       			
	        			}		
	    			}
	    			//this exception happens when bluetooth connection falls
	    			catch(IOException ex)
	    			{
	    				ex.printStackTrace();
	    					
	    				Log.d("ConnectedThreadHI", "run()--> IOException2");
	    				
	    				connectionLost();
	    				connectionFailed = true;
	    				
	    				try
	    				{
	    					if(mmSocket != null)
	    						mmSocket.close();
	    				}
	    				catch(IOException e2)
	    				{
	    					e2.printStackTrace();
	    				}
	    				
		    			break; //if connection fails, exit from while
	    			}
	    			catch(NumberFormatException e)
	    			{
	    				e.printStackTrace();
	    			}
	    		}
	    		Looper.loop();
    		}
    		else
    		{
    			Log.d("ConnectedThreadHI", "run()--> NO HISTORY records to download!");
    			
    			this.cancel(); //stop the thread
    			
    			//update progress bar 
				Message msg = mDownloadHandler.obtainMessage(Constants.NO_HIST_RECS);
			    mDownloadHandler.sendMessage(msg);
    		}
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
					{
						boxTs = Utils.getTsFromBoxTimestampFields(values[0], values[1]);
						gpsProvider = Constants.GPS_PROVIDERS[0]; //sensorbox gps provider
					}
					else
						gpsProvider = Constants.GPS_PROVIDERS[3]; //no gps data
					
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
					newHistRec.mUserData3 = Constants.mMacAddress;
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
}
