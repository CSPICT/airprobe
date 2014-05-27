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

package org.csp.everyaware;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.csp.everyaware.bluetooth.BluetoothBroadcastReceiver;
import org.csp.everyaware.bluetooth.BluetoothHistoryManager;
import org.csp.everyaware.bluetooth.BluetoothManager;
import org.csp.everyaware.bluetooth.BluetoothObject;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.SemanticSessionDetails;
import org.csp.everyaware.gps.GpsTrackingService;
import org.csp.everyaware.internet.StoreAndForwardService;
import org.csp.everyaware.tabactivities.Tabs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Start extends Activity 
{	
	private Button mConnectBtn, mDownloadHiBtn, mOfflineBtn, mOptionsBtn, mActivateBtn, mCreditsBtn;
	private DbManager mDbManager;
	private BluetoothAdapter mBluetoothAdapter = null;
	private static BluetoothManager mBluetoothManager = null;
	private static BluetoothHistoryManager mBluetoothHistManager = null;
	
	private BluetoothBroadcastReceiver mReceiver;
	private ProgressDialog mBTProgressDialog;
	
	private Dialog mProgDialog;
	private ProgressBar mDownProgBar;
	private ProgressBar mUplProgBar;
	private TextView mDownProgTv, mUplProgTv;
	
	private List<BluetoothObject>mDevices = new ArrayList<BluetoothObject>();
	private DevicesAdapter mDevicesAdapter;
	
	private DeleteUploadedRecordsThread mDelThread;
	
	//******** variabili relative alla devices discovering dialog ****/
	private Dialog mDevDiscDialog;
	
	private Button mAgainButton;
	//****************************************************************/
	
	private LocationManager mLocManager;
	
	private int mAction; //choosen action: connect, download history, offline
	private final int CONNECT = 300;
	private final int DOWNLOAD_HIST = 301;
	private final int OFFLINE = 302;
	
	private Handler mProgBarHandler;
	private boolean mFinishedDownload;
	private boolean mFinishedUpload;
	
	private boolean mSaveChoosenSensorBox;
	
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.start);
        
        Utils.appVer = Utils.getAppVer(Start.this);
                
        //get the semantic session seed (of generates a new one if it doesn't exist)
        //Utils.getSemanticSessionSeed(getApplicationContext());
        
        //get reference to a PARTIAL WAKE LOCK; it assures that CPU will always run, while 
        //screen and keyboard backlight can turn off. In addition, when user presses power
        //button, screen and keyboard will turn off but CPU will kept alive
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
        
        setBackground();
        
        CheckBox showHintsChBox = (CheckBox)findViewById(R.id.showHintsChBox);
        showHintsChBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) 
			{
				LinearLayout buttonsContainer = (LinearLayout)findViewById(R.id.buttonsContainer);
				TextView[] explTvs = new TextView[5];
				explTvs[0] = (TextView)findViewById(R.id.live_expl_tv);
				explTvs[1] = (TextView)findViewById(R.id.synchro_expl_tv);
				explTvs[2] = (TextView)findViewById(R.id.browse_expl_tv);
				explTvs[3] = (TextView)findViewById(R.id.settings_expl_tv);
				explTvs[4] = (TextView)findViewById(R.id.activate_expl_tv);
				
				if(isChecked)
				{
					buttonsContainer.setBackgroundColor(Color.WHITE);
					for(int i = 0; i < explTvs.length; i++)
						explTvs[i].setVisibility(View.VISIBLE);
				}
				else
				{
					buttonsContainer.setBackgroundColor(Color.TRANSPARENT);
					for(int i = 0; i < explTvs.length; i++)
						explTvs[i].setVisibility(View.GONE);
				}
			}   	
        });
        
		Utils.installID = Installation.id(getApplicationContext());
		Utils.deviceID = Utils.readUniquePhoneID(getApplicationContext());
		
        //check if smartphone gps device is ON/OFF and ask user to activate it
        mLocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);      
		if (!mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			showGPSDisabledAlertToUser();
		
		//start always gps tracking service (and turn off if not used)
        Utils.gpsTrackServIntent = new Intent(getApplicationContext(),GpsTrackingService.class);
	    startService(Utils.gpsTrackServIntent);
	    	
        Utils.setStep(Constants.START, getApplicationContext());
        Utils.createAppDir();
        Utils.initCounters();
        
        mDbManager = DbManager.getInstance(getApplicationContext());
        mDbManager.openDb();
        
        //check if session id still exists: this means that app crashed before
        if(!Utils.getSessionId(getApplicationContext()).equals(""))
        {
        	Log.d("Start", "Session id still exists, app has previously crashed");
        	sessionIdDialog();
        }
        else
        {
			//creating a new session_id from the timestamp of this moment and storing it on shared prefs
			String sessionId = String.valueOf(new Date().getTime());
			Log.d("Start", "sessionId " +sessionId);
			Utils.setSessionId(sessionId, getApplicationContext());
        }
		
		//delete oldest uploaded records
		mDelThread = new DeleteUploadedRecordsThread();
        mDelThread.start();
        
        //live track button
        mConnectBtn = (Button)findViewById(R.id.connect_btn);
        mConnectBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View arg0) 
			{
				/*
				mDbManager.resetUploadedRecords();
				
				int[] count = mDbManager.countRecords();
				
				recordCountDialog(count);
				*/
				
				mAction = CONNECT;
				
				Utils.historyDownloadMode = false;
				
				if(mDevices != null)
					mDevices.clear();
				
				//1 - ottengo il riferimento al dispositivo Bluetooth dello smartphone
				//    e verifico se è presente
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

				if (mBluetoothAdapter == null) 
				{
		            finishDialogNoBluetooth();
					return;
				}				
				
				mBluetoothManager = BluetoothManager.getInstance(getApplicationContext(), mStartHandler);
				
				//istanzio il broadcast receiver
				mReceiver = new BluetoothBroadcastReceiver(getApplicationContext(), mStartHandler);
				
				//registro il broadcast receiver con intent ACTION_FOUND (caso invocato quando
				//un device viene trovato durante la ricerca)
		        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

				//registro il broadcast receiver affinchè rilevi l'inizio della ricerca e la fine
				registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
				registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
				
				//2 - verifico se il dispositivo Bluetooth dello smartphone è attivato
				//    se non lo è, chiedo all'utente il permesso di attivarlo
				if(mBluetoothAdapter.isEnabled())
				{			        
					//if user preferred sensor box is saved, connect to it
					if(!Utils.getPrefDeviceAddress(getApplicationContext()).equals(""))
					{					
						BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(Utils.getPrefDeviceAddress(getApplicationContext()));
						Utils.setDeviceAddress(remoteDevice.getAddress(), getApplicationContext());
						createConnProgressDialog("Connecting to " +remoteDevice, false);
						//start full bluetooth connection (download real time + history(optional))
						mBluetoothManager.connect(remoteDevice);			
					}
					//else start discovery
					else
					{		
		            	//3 - avvio la ricerca di dispositivi Bluetooth nelle vicinanze
		                BluetoothAdapter.getDefaultAdapter().startDiscovery();	 
		                
		                deviceDiscDialog(); //apro finestra con lista devices bluetooth (inizialmente è vuota)
					}
				}
				else
				{					
        	        enableDialogBluetooth(); 
				}
				
			}        	
        });
        
        //synchro button
        mDownloadHiBtn = (Button)findViewById(R.id.download_history_btn);
        mDownloadHiBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				/*
				Intent intent = new Intent(Start.this, Synchro.class);
				startActivity(intent);
				finish();
				*/
				
				mAction = DOWNLOAD_HIST;
				
				Utils.historyDownloadMode = true;
						
				if(mDevices != null)
					mDevices.clear();
				
				//register receiver for messages from store'n'forward service
				IntentFilter internetOffFilter = new IntentFilter(Constants.INTERNET_OFF);
				registerReceiver(mServiceReceiver, internetOffFilter);
				IntentFilter uploadOffFilter = new IntentFilter(Constants.UPLOAD_OFF);
				registerReceiver(mServiceReceiver, uploadOffFilter);
				IntentFilter finishedUploadFilter = new IntentFilter(Constants.FINISHED_UPLOAD);
				registerReceiver(mServiceReceiver, finishedUploadFilter);
				
				//1 - ottengo il riferimento al dispositivo Bluetooth dello smartphone
				//    e verifico se è presente
				mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

				if (mBluetoothAdapter == null) 
				{
		            finishDialogNoBluetooth();
					return;
				}				
				
				mBluetoothHistManager = BluetoothHistoryManager.getInstance(getApplicationContext(), mStartHistoryHandler);
				
				//istanzio il broadcast receiver
				mReceiver = new BluetoothBroadcastReceiver(getApplicationContext(), mStartHistoryHandler);
				
				//registro il broadcast receiver con intent ACTION_FOUND (caso invocato quando
				//un device viene trovato durante la ricerca)
		        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

				//registro il broadcast receiver affinchè rilevi l'inizio della ricerca e la fine
				registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
				registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
				
				//2 - verifico se il dispositivo Bluetooth dello smartphone è attivato
				//    se non lo è, chiedo all'utente il permesso di attivarlo
				if(mBluetoothAdapter.isEnabled())
				{			      
					//if user preferred sensor box is saved, connect to it
					if(!Utils.getPrefDeviceAddress(getApplicationContext()).equals(""))
					{
						BluetoothDevice remoteDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(Utils.getPrefDeviceAddress(getApplicationContext()));
						createConnProgressDialog("Connecting to " +remoteDevice, false);
						//start bluetooth connection to download only history records
						mBluetoothHistManager.connect(remoteDevice);
					}
					else
					{
		            	//3 - avvio la ricerca di dispositivi Bluetooth nelle vicinanze
		                BluetoothAdapter.getDefaultAdapter().startDiscovery();	 
		                
		                deviceDiscHistDialog(); //apro finestra con lista devices bluetooth (inizialmente è vuota)
					}
				}
				else
				{					
        	        enableDialogBluetooth(); 
				}
			} 	
        });
        
        //browse button
        mOfflineBtn = (Button)findViewById(R.id.offline_btn);
        mOfflineBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				mAction = OFFLINE;
				
				Intent intent = new Intent(Start.this, org.csp.everyaware.offline.Tabs.class);
				startActivity(intent);
				finish();
			}     	
        });
        
        //settings button
        mOptionsBtn = (Button)findViewById(R.id.options_btn);
        mOptionsBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(Start.this, Options.class);
				startActivity(intent);
			}
        	
        });
        
        //activate
        mActivateBtn = (Button)findViewById(R.id.activate_btn);
        
        mCreditsBtn = (Button)findViewById(R.id.credits_btn);
        mCreditsBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				Intent intent = new Intent(Start.this, Credits.class);
				startActivity(intent);				
			}        	
        });
    }
    
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("Start", "onStop()");
		
		//close progress dialog about remote device opening connection
		if(Utils.connProgressDialog != null)
		{
			Log.d("Start", "onStop()--> connection progress dialog dismiss");
			Utils.connProgressDialog.dismiss();
			Utils.connProgressDialog = null;
		}	
		
		//de-registro il broadcast receiver
    	try
    	{
    		if(mReceiver != null)
    			unregisterReceiver(mReceiver);
    		
    		if(mAction == DOWNLOAD_HIST)    	
    			if(mServiceReceiver != null)
    				unregisterReceiver(mServiceReceiver);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	}	    
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy(); 	
    	Log.d("Start", "onDestroy()");
    	
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release(); 
    	
    	if(mProgBarHandler != null)
			mProgBarHandler.removeCallbacks(mUpdateBars);
    	
		if((mBTProgressDialog != null)&&(mBTProgressDialog.isShowing()))
			mBTProgressDialog.dismiss();
		
		if((mProgDialog != null)&&(mProgDialog.isShowing()))
			mProgDialog.dismiss();
		
		if((mDevDiscDialog != null)&&(mDevDiscDialog.isShowing()))
			mDevDiscDialog.dismiss();
    }      
    
    //questo è il primo metodo invocato non appena si stabilisce una connessione verso il device remoto e viene
    //avviato l'intent che attiva la Tabs activity. Qui devono essere chiuse le dialog boxes attive in modo da 
    //non generare un'eccezione di leaked window legata a questa activity. Subito dopo questo metodo viene 
    //invocato il metodo onStop()
    @Override
    public void onPause()
    {
    	super.onPause();  
    	Log.d("Start", "onPause()");

		//de-registro il broadcast receiver
		/*
    	try
    	{
    		if(mReceiver != null)
    			unregisterReceiver(mReceiver);
    		
    		if(mAction == DOWNLOAD_HIST)    	
    			if(mServiceReceiver != null)
    				unregisterReceiver(mServiceReceiver);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}*/
    	/*
    	//distruggo l'attività (ma non se vado alle opzioni o modalità history download)
    	if((mAction == CONNECT)||(mAction == OFFLINE))
    	{
    		finish();
    	}*/
    }
    
	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("Start", "onResume()");
		
		if((Utils.paused)&&(!Utils.backToHome))
		{
			Log.d("Start", "onResume()--> PAUSED");
			Utils.paused = false;
			finish();
		}
		
		Utils.paused = false;
		Utils.backToHome = false;
		
		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
        //if app is activated, disable activation button, put green icon and change text
		//live track record and synchro buttons are enabled
        if(Utils.getAccountActivationState(getApplicationContext()))
        {
        	mConnectBtn.setEnabled(true);
        	mDownloadHiBtn.setEnabled(true);
        	
        	mActivateBtn.setEnabled(true);
        	mActivateBtn.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.activated_on), null, null, null);
        	mActivateBtn.setText(getResources().getString(R.string.activated_msg));
        	
        	mActivateBtn.setOnClickListener(new OnClickListener()
        	{
				@Override
				public void onClick(View v) 
				{
					deactivateAccountDialog();
				}       		
        	});
        }
        //else enable it to let the wizard go on if choosen
        //live track record  and synchro buttons are disabled
        else
        {
        	mConnectBtn.setEnabled(true);
        	mDownloadHiBtn.setEnabled(true);
        	
        	mActivateBtn.setEnabled(true); 
        	mActivateBtn.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.activated_off), null, null, null);
        	mActivateBtn.setText(getResources().getString(R.string.activate_msg));
        	
	        mActivateBtn.setOnClickListener(new OnClickListener()
	        {
				@Override
				public void onClick(View v) 
				{
					Intent intent = new Intent(Start.this, ManageAccount.class);
					startActivity(intent);				
				}        	
	        });
        }
	}        
    
    @Override
    public void onBackPressed()
    {
    	if((mDevDiscDialog == null)||((mDevDiscDialog != null)&&(!mDevDiscDialog.isShowing())))
    	{
        	//stop gps tracking service
        	if(Utils.gpsTrackServIntent != null)
        		stopService(Utils.gpsTrackServIntent);
        	Utils.setGpsTrackingOn(false, getApplicationContext());
        	Utils.deleteSharedPrefs(getApplicationContext());
    		finish();
    	}
    }
    
    /*************************** HANDLER ******************************************************/
    
    private Handler mStartHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
			switch(msg.what)
    		{
				//evento discovery dispositivi bluetooth terminata
				case Constants.BT_ACTIVATED:
					if((mBTProgressDialog != null)&&(mBTProgressDialog.isShowing()))
						mBTProgressDialog.dismiss();
					
	            	//3 - avvio la ricerca di dispositivi Bluetooth nelle vicinanze
	                BluetoothAdapter.getDefaultAdapter().startDiscovery();
	                
	                deviceDiscDialog(); //apro finestra con lista devices bluetooth (inizialmente è vuota)
				break;
				
				case Constants.DISCOVERY_STARTED:
					Log.d("StartHandler", "handleMessage()--> discovery started");
					
				break;
				
				case Constants.DISCOVERY_FINISHED:
					Log.d("StartHandler", "handleMessage()--> discovery finished - trovato/i " +mDevices.size()+ " dispositivo/i");	
					mDevDiscDialog.setTitle(getResources().getString(R.string.complete_discovery_msg));	
					mAgainButton.setEnabled(true);
				break;
				
				case Constants.DEVICE_DISCOVERED:										
					BluetoothObject device = 
							(BluetoothObject)msg.getData().getSerializable(Constants.DEVICE);
					Log.d("StartHandler", "handleMessage()--> device discovered - nome: "+device.getName());
					
					//check if device address appears in device list, if not add it (to avoid duplicate names on some smartphones)
					boolean found = false;
					int i = 0;
					while((i < mDevices.size())&&(!found))
					{
						if(mDevices.get(i).getAddress().equals(device.getAddress()))
						{
							found = true;
							Log.d("StartHandler", "handleMessage()--> this device is just in list, ignore it");
						}
						i++;
					}
					//if device address is not in the list, add it
					if(!found)
					{
						mDevices.add(device); 
						mDevicesAdapter.notifyDataSetChanged();
					}
				break;	
				
				case Constants.CONNECTION_FAILED:
					Log.d("StartHandler", "handleMessage()--> CONNECTION_FAILED");
					
					//in questo punto, la mProgressDialog risulta null
				
					//close progress dialog about remote device opening connection
					if(Utils.connProgressDialog != null)
					{
						Log.d("StartHandler", "handleMessage()--> connection progress dialog dismiss");
						Utils.connProgressDialog.dismiss();
						Utils.connProgressDialog = null;
					}		
					Toast.makeText(Start.this, getResources().getString(R.string.error_connection_msg), Toast.LENGTH_LONG).show();
					
					//connFailedDialog();					
				break;	
				
				case Constants.STATE_NONE:
					Log.d("StartHandler", "handleMessage()--> STATE NONE ");
				break;
				case Constants.STATE_CONNECTING:
					Log.d("StartHandler", "handleMessage()--> STATE_CONNECTING");
				break;
				case Constants.STATE_CONNECTED:
					
					if(mReceiver != null)
					{
						try
						{
							unregisterReceiver(mReceiver);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						mReceiver = null;
					}
					if(mDevices != null)
						mDevices.clear();
			
					Log.d("StartHandler", "handleMessage()--> STATE_CONNECTED - Action: CONNECT");

					Intent intent = new Intent(Start.this, Tabs.class);
					startActivity(intent);
					finish();
					
				break;	

    		}
    	}
    };
    
    private Handler mStartHistoryHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
			switch(msg.what)
    		{
				//evento discovery dispositivi bluetooth terminata
				case Constants.BT_ACTIVATED:
					if((mBTProgressDialog != null)&&(mBTProgressDialog.isShowing()))
						mBTProgressDialog.dismiss();
					
	            	//3 - avvio la ricerca di dispositivi Bluetooth nelle vicinanze
	                BluetoothAdapter.getDefaultAdapter().startDiscovery();
	                
	                deviceDiscHistDialog(); //apro finestra con lista devices bluetooth (inizialmente è vuota)
				break;
				
				case Constants.DISCOVERY_STARTED:
					Log.d("StartHistoryHandler", "handleMessage()--> discovery started");
					
				break;
				
				case Constants.DISCOVERY_FINISHED:
					Log.d("StartHistoryHandler", "handleMessage()--> discovery finished - trovato/i " +mDevices.size()+ " dispositivo/i");	
					mDevDiscDialog.setTitle(getResources().getString(R.string.complete_discovery_msg));	
					mAgainButton.setEnabled(true);
				break;
				
				case Constants.DEVICE_DISCOVERED:										
					BluetoothObject device = 
							(BluetoothObject)msg.getData().getSerializable(Constants.DEVICE);
					Log.d("StartHistoryHandler", "handleMessage()--> device discovered - nome: "+device.getName());
					
					//check if device address appears in device list, if not add it (to avoid duplicate names on some smartphones)
					boolean found = false;
					int i = 0;
					while((i < mDevices.size())&&(!found))
					{
						if(mDevices.get(i).getAddress().equals(device.getAddress()))
						{
							found = true;
							Log.d("StartHistoryHandler", "handleMessage()--> this device is just in list, ignore it");
						}
						i++;
					}
					//if device address is not in the list, add it
					if(!found)
					{
						mDevices.add(device); 
						mDevicesAdapter.notifyDataSetChanged();
					}
				break;	
				
				case Constants.CONNECTION_FAILED:
					Log.d("StartHistoryHandler", "handleMessage()--> CONNECTION_FAILED");
					
					//in questo punto, la mProgressDialog risulta null
				
					//close progress dialog about remote device opening connection
					if(Utils.connProgressDialog != null)
					{
						Log.d("StartHistoryHandler", "handleMessage()--> connection progress dialog dismiss");
						Utils.connProgressDialog.dismiss();
						Utils.connProgressDialog = null;
					}		
					Toast.makeText(Start.this, getResources().getString(R.string.error_connection_msg), Toast.LENGTH_LONG).show();
					
					//connFailedDialog();					
				break;	
				
				case Constants.STATE_NONE:
					Log.d("StartHistoryHandler", "handleMessage()--> STATE NONE ");
				break;
				case Constants.STATE_CONNECTING:
					Log.d("StartHistoryHandler", "handleMessage()--> STATE_CONNECTING");
				break;
				
				case Constants.CONNECTION_LOST:
					
					Log.d("StartHistoryHandler", "handleMessage()--> CONNECTION_LOST");
					
				break;
				
				case Constants.STATE_CONNECTED:
					
					Log.d("StartHistoryHandler", "handleMessage()--> STATE_CONNECTED");
					
					if(mReceiver != null)
					{
						try
						{
							unregisterReceiver(mReceiver);
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
						mReceiver = null;
					}
					if(mDevices != null)
						mDevices.clear();
					
					downloadHistory();
					
					
				break;	

    		}
    	}
    };
    
    //history download mode handler
    private Handler mDownloadHandler = new Handler()
    {
    	@Override
    	public void handleMessage(Message msg)
    	{
			switch(msg.what)
    		{
			
				case Constants.SENSOR_BOX_MAC_NOT_READ:
					Log.d("DownloadHandler", "handleMessage()--> SENSOR BOX MAC ADDRESS NOT READ!!!");
					
					 sensorBoxMacNotReadDialog();
				break;
			
				case Constants.TOTAL_HISTORY_NUM:
					
					Log.d("DownloadHandler", "handleMessage()--> TOTAL_HISTORY_NUM received");
					
					mFinishedDownload = false;
					
					if((mDevDiscDialog != null)&&(mDevDiscDialog.isShowing()))
						mDevDiscDialog.dismiss();
					
			    	runOnUiThread(new Runnable() 
			    	{
			    		@Override
			    		public void run() 
			    		{
			    			if(!isFinishing())
			    			{
			    				Log.d("DownloadHandler", "handleMessage()--> createDownUplProgdialog");
			    				
								//when I receive the number of history records on sensor box, I can start to download history record
								//but I also have the total number of records to upload to server by store'n'forward service
								createDownUplProgdialog(Utils.numberHR);
								
								Log.d("DownloadHandler", "handleMessage()--> starting store and forward service");
								
								//starting store'n'forward service and saving reference to it
								Intent serviceIntent = new Intent(getApplicationContext(), StoreAndForwardService.class);
								Utils.storeForwServIntent = serviceIntent;
								startService(serviceIntent);
								
								mProgBarHandler = new Handler();
								mProgBarHandler.postDelayed(mUpdateBars, 2000);
			    			}
			    			else
			    			{
			    				Log.d("DownloadHandler", "handleMessage()--> CAN'T createDownUplProgdialog");
			    				Toast.makeText(getApplicationContext(), "Error opening progress dialog!!", Toast.LENGTH_LONG).show();
			    				
			    				//close bluetooth thread (this works here)
			    		    	if(mBluetoothHistManager != null)
			    		    		mBluetoothHistManager.stop();			    
			    			}
			    		}
			    	});
			    	
					
					
				break;
				
				case Constants.UPDATE_PROGRESS:
					
				break;
				
				case Constants.NO_HIST_RECS:
					
					Log.d("DownloadHandler", "handleMessage()--> NO_HIST_RECS");
					
					runOnUiThread(new Runnable() 
        	    	{
        	    		@Override
        	    		public void run() 
        	    		{
        	    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_hist_recs), Toast.LENGTH_LONG).show();       	    			
        	    		}
        	    	});
					
				break;
				
				case Constants.FINISHED_HIST_DOWN:
					
					Log.d("DownloadHandler", "handleMessage()--> FINISHED_HIST_DOWN");
					
					mFinishedDownload = true;
					
					if(mFinishedUpload)
					{
						Log.d("DownloadHandler", "handleMessage()--> FINISHED_HIST_DOWN + FINISHED UPLOAD");
						//close bluetooth thread
				    	if(mBluetoothHistManager != null)
				    		mBluetoothHistManager.stop();
				    	
				    	//stop store'n'forward service
				    	if(Utils.storeForwServIntent != null)
				    		stopService(Utils.storeForwServIntent);
				    	
				    	if((mProgDialog != null)&&(mProgDialog.isShowing()))
				    		mProgDialog.dismiss();
				    	
						new Handler().postDelayed(new Runnable()
						{
							@Override
							public void run() 
							{
								createFinishedDownloadDialog();
							}					
						}, 500);
					}
					else
					{
						Log.d("DownloadHandler", "handleMessage()--> FINISHED_HIST_DOWN + NOT! FINISHED UPLOAD");
					}
					
				break;
    		}
    	}
    };
		
    /********************* RECEIVES MESSAGES FROM STORE'N'FORWARD SERVICE *****************************/
    
    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	String action = intent.getAction();

            if (action.equals(Constants.INTERNET_OFF)) 
            {
            	Log.d("ServiceReceiver", "Internet is OFF");
            	if(mUplProgTv != null)
            		mUplProgTv.setText("Internet connection not available");
            }    
            
            if(action.equals(Constants.UPLOAD_ON))
            {
            	Log.d("ServiceReceiver", "onReceive()--> Upload is ON");
            	mFinishedUpload = false;
            }
            
            if (action.equals(Constants.UPLOAD_OFF)) 
            {
            	Log.d("ServiceReceiver", "onReceive()--> Upload is OFF");
            }   
            
            if(action.equals(Constants.FINISHED_UPLOAD))
            {
            	Log.d("ServiceReceiver", "onReceive()--> Finished upload");

    			mFinishedUpload = true;
    			
    			if(mFinishedDownload)
    			{
					//close bluetooth thread
			    	if(mBluetoothHistManager != null)
			    		mBluetoothHistManager.stop();
			    	
			    	//stop store'n'forward service
			    	if(Utils.storeForwServIntent != null)
			    		stopService(Utils.storeForwServIntent);
			    	
			    	if((mProgDialog != null)&&(mProgDialog.isShowing()))
			    		mProgDialog.dismiss();
			    	
					new Handler().postDelayed(new Runnable()
					{
						@Override
						public void run() 
						{
							createFinishedDownloadDialog();
						}					
					}, 500);
    			}
            }
        }
    };
    
    /****************** RUNNABLE THAT UPDATES DOWNLOAD AND UPLOAD PROGRESS BAR *****************************/
    
    private Runnable mUpdateBars = new Runnable()
    {
		@Override
		public void run() 
		{
			if(mDownProgBar != null)
				mDownProgBar.setProgress(Utils.counterHR);
			
			if(mDownProgTv != null)
				mDownProgTv.setText("Downloaded "+Utils.counterHR+"/"+Utils.numberHR);
			
			if(mUplProgBar != null)
				mUplProgBar.setProgress(Utils.getUploadedRecCount(getApplicationContext()));
			
			if(mUplProgTv != null)
			{
				if(Utils.uploadOn == Constants.INTERNET_OFF_INT)
					mUplProgTv.setText("Internet connection not available");
				else
					mUplProgTv.setText("Uploaded: "+Utils.getUploadedRecCount(getApplicationContext())+"/"+(Utils.getTotalStoredRecCount(getApplicationContext())+Utils.numberHR-Utils.counterHR)+
						" ("+(Utils.getTotalStoredRecCount(getApplicationContext())+" on disk)"));
			}
			
			mProgBarHandler.postDelayed(mUpdateBars, 2000);
		} 	
    };
    
    /*************************** ARRAY ADAPTER ************************************************/
    
    public class DevicesAdapter extends ArrayAdapter<BluetoothObject>
    {
    	List<BluetoothObject>deviceItems;
    	Activity context;
    	
    	public DevicesAdapter(Activity context, int textViewResourceId, List<BluetoothObject> devices)
    	{
    		super(context, textViewResourceId, devices);

    		this.deviceItems = devices;
    		this.context = context;
    	}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent)
    	{
    		View view = convertView;
    	
			if(view == null)
			{
				LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.devices_list_single_row, null);
			}
			
			BluetoothObject device = deviceItems.get(position);
			
			if(device != null)
			{
				TextView deviceNameTV = (TextView)view.findViewById(R.id.device_name_textView);
				deviceNameTV.setText(device.getName());
				
				TextView deviceMacTV = (TextView)view.findViewById(R.id.device_mac_textView);
				deviceMacTV.setText(device.getAddress());
			}
			
			return view;
    	}
    }
    
    /*************************** DIALOGS ******************************************************/
    
    public void sensorBoxMacNotReadDialog() 
    {
		if(mProgDialog != null)
			mProgDialog.dismiss();
    	
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
			        builder.setMessage(R.string.alert_dialog_sensorbox_mac_not_read)
			        	.setIcon(android.R.drawable.ic_dialog_info)
			        	.setTitle(R.string.app_name)
			        	.setCancelable( false )
			        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			dialog.dismiss();
			        			
			    				//close bluetooth thread
			    		    	if(mBluetoothHistManager != null)
			    		    		mBluetoothHistManager.stop();
			    		    	
			    		    	//stop store'n'forward service
			    		    	if(Utils.storeForwServIntent != null)
			    		    		stopService(Utils.storeForwServIntent);
			    		    	
			    				//de-registro il broadcast receiver
			    		    	try
			    		    	{
			    		    		if(mReceiver != null)
			    		    			unregisterReceiver(mReceiver);
			    		    		
			    		    		if(mAction == DOWNLOAD_HIST)    	
			    		    			if(mServiceReceiver != null)
			    		    				unregisterReceiver(mServiceReceiver);
			    		    	}
			    		    	catch(Exception e)
			    		    	{
			    		    		e.printStackTrace();
			    		    	}
			        		}
			        	});
			        
			        AlertDialog alert = builder.create();
			        alert.show(); 
    			}
    		}
    	});
    }
    
    //error!
    public void connFailedDialog() 
    {
    	/*
    	//per le activity contenute in tabs activity, è meglio usare getParent() invece di this
        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
        builder.setMessage(R.string.alert_dialog_conn_failed)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable(false)
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{
        			dialog.dismiss();
        		}
        	});
        
        AlertDialog alert = builder.create();
        alert.show(); */ 
    	
    	Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_connection_msg), Toast.LENGTH_LONG).show();
    }
    
    //visualizza dialog contenente lista di devices bluetooth compatibili nelle vicinanze
    public void deviceDiscDialog()
    {   	   	
		mDevDiscDialog = new Dialog(Start.this);
		mDevDiscDialog.setTitle(getResources().getString(R.string.run_discovery_msg)+" (Live Track mode)");
		mDevDiscDialog.setContentView(R.layout.devices_list_dialog);		
		mDevDiscDialog.setCancelable(false); //to avoid back button closes dialog
        
		mDevDiscDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, 
				LayoutParams.WRAP_CONTENT);
		
		ListView devicesListView = (ListView)mDevDiscDialog.findViewById(R.id.devices_listView);
		mDevicesAdapter = new DevicesAdapter(this, R.layout.devices_list_single_row, mDevices);
		devicesListView.setAdapter(mDevicesAdapter);
		
		CheckBox prefCheckBox = (CheckBox)mDevDiscDialog.findViewById(R.id.save_pref_chbox);
		prefCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				mSaveChoosenSensorBox = checked;
			}		
		});
		
		//gestione click su elementi della lista (ovvero sui ogni singolo device bluetooth trovato)
		devicesListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) 
			{					    			
				String deviceAddress = mDevices.get(position).getAddress();
				Utils.setDeviceAddress(deviceAddress, getApplicationContext());
				if(mSaveChoosenSensorBox)
					Utils.savePrefDeviceAddress(deviceAddress, getApplicationContext());
				
				//4 - istanzio l'oggetto di tipo BluetoothDevice che rappresenta il device
				//    remoto (rintracciato tramite il suo mac)				
				BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mDevices.get(position).getAddress());

				//fermo la discovery se è in corso
				if(mBluetoothAdapter.isDiscovering())
					mBluetoothAdapter.cancelDiscovery();

				//utile
				mDevicesAdapter.notifyDataSetChanged();
				
				createConnProgressDialog("Connecting to " +remoteDevice, false);
				
				Log.d("OnItemClickListener", "onItemClick()--> mAction: "+mAction);
				
				Log.d("OnItemClickListener", "onItemClick()--> CONNECT");
				
				//Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_LONG).show();
				
				//start full bluetooth connection (download real time + history(optional))
				mBluetoothManager.connect(remoteDevice);
			}			
		});
		
		mAgainButton = (Button)mDevDiscDialog.findViewById(R.id.again_button);
		mAgainButton.setEnabled(false);
		mAgainButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				mDevices.clear();
                BluetoothAdapter.getDefaultAdapter().startDiscovery();	
                mAgainButton.setEnabled(false);
                mDevDiscDialog.setTitle(getResources().getString(R.string.run_discovery_msg));
			}			
		});
		
		Button cancelBtn = (Button)mDevDiscDialog.findViewById(R.id.cancel_button);
		cancelBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
					cancelDiscoverDialog();
				else //chiusura dialog lista devices quando non è in corso la ricerca
				{	    			
					unregisterReceiver(mReceiver);
					mReceiver = null;
					
					mDevices.clear();
					
	    			new Handler().postDelayed(closeDialogRunnable, 500);
				}
			}			
		});
		
		mDevDiscDialog.show();
    }
    
    //visualizza dialog contenente lista di devices bluetooth compatibili nelle vicinanze
    public void deviceDiscHistDialog()
    {   	   	
		mDevDiscDialog = new Dialog(Start.this);
		mDevDiscDialog.setTitle(getResources().getString(R.string.run_discovery_msg)+" (Synchro mode)");
		mDevDiscDialog.setContentView(R.layout.devices_list_dialog);		
		mDevDiscDialog.setCancelable(false); //to avoid back button closes dialog
        
		mDevDiscDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, 
				LayoutParams.WRAP_CONTENT);
		
		ListView devicesListView = (ListView)mDevDiscDialog.findViewById(R.id.devices_listView);
		mDevicesAdapter = new DevicesAdapter(this, R.layout.devices_list_single_row, mDevices);
		devicesListView.setAdapter(mDevicesAdapter);
		
		CheckBox prefCheckBox = (CheckBox)mDevDiscDialog.findViewById(R.id.save_pref_chbox);
		prefCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean checked) 
			{
				mSaveChoosenSensorBox = checked;
			}		
		});
		
		//gestione click su elementi della lista (ovvero sui ogni singolo device bluetooth trovato)
		devicesListView.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) 
			{					    			
				String deviceAddress = mDevices.get(position).getAddress();
				Utils.setDeviceAddress(deviceAddress, getApplicationContext());
				if(mSaveChoosenSensorBox)
					Utils.savePrefDeviceAddress(deviceAddress, getApplicationContext());
				
				//4 - istanzio l'oggetto di tipo BluetoothDevice che rappresenta il device
				//    remoto (rintracciato tramite il suo mac)				
				BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mDevices.get(position).getAddress());

				//fermo la discovery se è in corso
				if(mBluetoothAdapter.isDiscovering())
					mBluetoothAdapter.cancelDiscovery();

				//utile
				mDevicesAdapter.notifyDataSetChanged();
				
				createConnProgressDialog("Connecting to " +remoteDevice, false);
				
				Log.d("OnItemClickListener2", "onItemClick()--> mAction: "+mAction);
				
				Log.d("OnItemClickListener2", "onItemClick()--> DOWNLOAD_HIST");
				
				//Toast.makeText(getApplicationContext(), "DOWNLOAD_HIST", Toast.LENGTH_LONG).show();
				
				//start bluetooth connection to download only history records
				mBluetoothHistManager.connect(remoteDevice);			
			}			
		});
		
		mAgainButton = (Button)mDevDiscDialog.findViewById(R.id.again_button);
		mAgainButton.setEnabled(false);
		mAgainButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				mDevices.clear();
                BluetoothAdapter.getDefaultAdapter().startDiscovery();	
                mAgainButton.setEnabled(false);
                mDevDiscDialog.setTitle(getResources().getString(R.string.run_discovery_msg));
			}			
		});
		
		Button cancelBtn = (Button)mDevDiscDialog.findViewById(R.id.cancel_button);
		cancelBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
					cancelDiscoverDialog();
				else //chiusura dialog lista devices quando non è in corso la ricerca
				{	    			
					unregisterReceiver(mReceiver);
					mReceiver = null;
					
					mDevices.clear();
					
	    			new Handler().postDelayed(closeDialogRunnable, 500);
				}
			}			
		});
		
		mDevDiscDialog.show();
    }
    
    //dialog conferma annullamento fase di ricerca
    public void cancelDiscoverDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
        builder.setMessage(R.string.cancel_discovery)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable( false )
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{	
    		    	try
    		    	{
    		    		if(mReceiver != null)
    		    		{
    		    			if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
    		    				BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    		    			unregisterReceiver(mReceiver);
    		    			mReceiver = null;
    		    			
    		    			mDevices.clear();
    		    		}
    		    	}
    		    	catch(Exception e)
    		    	{
    		    		e.printStackTrace();
    		    	}
    				
    		    	new Handler().postDelayed(closeDialogRunnable, 500);
        		}
        	})
            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener()
            {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {}        	
            });
        
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    //progress dialog usata durante l'attivazione del dispositivo bluetooth sullo smartphone
    public void createBTProgressDialog(final String msg, boolean cancelable)
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
    				mBTProgressDialog = ProgressDialog.show(Start.this, getResources().getString(R.string.app_name), msg, true, false);
    			}
    		}
    	});
    }
    
    //progress dialog usata durante l'apertura di una connessione bluetooth verso il device remoto scelto
    //lasciarla creata così, come oggetto statico in Utils e istanziata tramite new. Se la creo da show, non la chiude quando
    //si verifica un errore di connessione
    public void createConnProgressDialog(final String msg, final boolean cancelable)
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			    	Utils.connProgressDialog = new ProgressDialog(Start.this);
			    	Utils.connProgressDialog.setTitle(getResources().getString(R.string.app_name));
			    	Utils.connProgressDialog.setMessage(msg);
			    	Utils.connProgressDialog.setCancelable(cancelable);
			    	Utils.connProgressDialog.show();   	
    			}
    		}
    	});
    }
    
    //apre finestra avviso di attivazione dispositivo Bluetooth dello smartphone se spento
    public void enableDialogBluetooth() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{   				
			        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
			        builder.setMessage(R.string.alert_dialog_enable_bt)
			        	.setIcon(android.R.drawable.ic_dialog_info)
			        	.setTitle(R.string.app_name)
			        	.setCancelable( false )
			        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			//registro il broadcast receiver con intent ACTION_STATE_CHANGED, utile per
								//rilevare quando il dispositivo bluetooth dello smartphone viene acceso
			        	        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
			        	        
			        			mBluetoothAdapter.enable(); //attivazione dispositivo bluetooth smartphone  
			        			
			        	    	runOnUiThread(new Runnable() 
			        	    	{
			        	    		@Override
			        	    		public void run() 
			        	    		{
			        	    			if(!isFinishing())
			        	    			{
			        	    				createBTProgressDialog("Activating Bluetooth", true); //l'attivazione del dispositivo bluetooth ha progress dialog
			        	    			}
			        	    		}
			        	    	});
			        		}
			        	})
			            .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener()
			            {
							@Override
							public void onClick(DialogInterface arg0, int arg1) 
							{
								finish(); //chiudo l'activity 				
							}        	
			            });
			        
			        final AlertDialog alert = builder.create();
    				alert.show(); 
    			}
    		}
    	});
    }
    
    //apre finestra avviso indisponibilità bluetooth sullo smartphone e chiude l'app
    public void finishDialogNoBluetooth() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
			        builder.setMessage(R.string.alert_dialog_no_bt)
			        	.setIcon(android.R.drawable.ic_dialog_info)
			        	.setTitle(R.string.app_name)
			        	.setCancelable( false )
			        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			finish(); //chiudo l'activity 	
			        		}
			        	});
			        
			        AlertDialog alert = builder.create();
			        alert.show(); 
    			}
    		}
    	});
    }
    
    public void sessionIdDialog() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Start.this);
			        builder.setMessage(R.string.alert_dialog_old_session_id_found)
			        	.setIcon(android.R.drawable.ic_dialog_info)
			        	.setTitle(R.string.app_name)
			        	.setCancelable( false )
			        	.setPositiveButton(R.string.alert_dialog_use_it, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			dialog.dismiss();
			        		}
			        	})
			        	.setNegativeButton(R.string.alert_dialog_create_new, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			//creating a new session_id from the timestamp of this moment and storing it on shared prefs
			        			String sessionId = String.valueOf(System.currentTimeMillis());
			        			Log.d("Start", "sessionId: " +sessionId);
			        			Utils.setSessionId(sessionId, getApplicationContext());
			        			
			        			dialog.dismiss();
			        		}
			        	});
			        
			        AlertDialog alert = builder.create();
			        alert.show(); 
    			}
    		}
    	});
    }
    
    /************************* RUNNABLE ********************************************************/
    
    //permette chiusura del dialog lista dispositivi bluetooth con ritardo
    private Runnable closeDialogRunnable = new Runnable()
    {

		@Override
		public void run() 
		{
			if(mDevDiscDialog != null)
				mDevDiscDialog.dismiss();
		}    	
    };
    
    /************************** THREAD ********************************************************/
    
    private class DeleteUploadedRecordsThread extends Thread
    {
        //List<Record>records = null;
        
        public void run()
        {
	        //delete uploaded and old records
	        int index = Utils.getRecordAgesIndex(getApplicationContext());
	        Log.d("DeleteUploadedRecordsThread", "run() --> calling deleteUploadedRecords() with time: "+Constants.recordAges[index]);
	        int count = mDbManager.deleteUploadedRecords(Constants.recordAges[index]);
	        Log.d("DeleteUploadedRecordsThread", "run() # records cancellati: " +count);
        }
    }
    
    /************************ SET BACKGROUND IMG FOR RIGHT DISPLAY RESOLUTION *****************/
    
    private void setBackground()
    {      
    	LinearLayout ll = (LinearLayout)findViewById(R.id.startLinearLayout);
    	
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
             
        //Log.d("Start", "setBackground()--> " +screenWidth+"X"+screenHeight);
        //Toast.makeText(getApplicationContext(), "setBackground()--> " +screenWidth+"X"+screenHeight, Toast.LENGTH_LONG).show();
        
        if(screenWidth == 320)
        {
        	if(screenHeight == 480)
        		ll.setBackgroundResource(R.drawable.background320x480);
        }
        else if(screenWidth == 480)
        {
        	if(screenHeight == 800)
        		ll.setBackgroundResource(R.drawable.background480x800);
        }
        else if(screenWidth == 720)
        {
        	if(screenHeight == 1280)     		
        		ll.setBackgroundResource(R.drawable.background720x1280);
        }
        else 
        	ll.setBackgroundResource(R.drawable.background720x1280);
    }   
    
    private void showGPSDisabledAlertToUser()
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Start.this);
			    	alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
			    	.setCancelable(false)
			    	.setPositiveButton("Goto Settings Page To Enable GPS",
			    			new DialogInterface.OnClickListener()
			    			{
			    				public void onClick(DialogInterface dialog, int id)
			    				{
			    					Intent callGPSSettingIntent = new Intent(
			    							android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			    					startActivity(callGPSSettingIntent);
			    				}
			    			});
			    	
			    	alertDialogBuilder.setNegativeButton("Cancel",
			    			new DialogInterface.OnClickListener()
			    		{
			    			public void onClick(DialogInterface dialog, int id)
			    			{
			    				dialog.cancel();
			    			}
			    		});
			    	AlertDialog alert = alertDialogBuilder.create();
			    	alert.show();
    			}
    		}
    	});
    }
    
    private void downloadHistory()
    {
    	Log.d("Start", "downloadHistory()");
    	
		if((mBTProgressDialog != null)&&(mBTProgressDialog.isShowing()))
			mBTProgressDialog.dismiss();
		if((Utils.connProgressDialog != null)&&Utils.connProgressDialog.isShowing())
			Utils.connProgressDialog.dismiss();
    	
		if((mDevDiscDialog != null)&&(mDevDiscDialog.isShowing()))
			mDevDiscDialog.dismiss();
		
		mBluetoothHistManager.setDownloadHandler(mDownloadHandler);
    }
    
    private void createDownUplProgdialog(int recsToDown)
    {
    	Log.d("Start", "createDownUplProgdialog()");
    	
    	if((mProgDialog != null)&&(mProgDialog.isShowing()))
    		mProgDialog.dismiss();
    	
    	mProgDialog = null;
    	
    	mProgDialog = new Dialog(Start.this);
    	mProgDialog.setContentView(R.layout.down_upl_prog_dialog);
    	
    	mProgDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, 
				LayoutParams.WRAP_CONTENT);

    	mProgDialog.setCancelable(false);
    	mProgDialog.setTitle(getResources().getString(R.string.app_name));
    	
    	mDownProgBar = (ProgressBar)mProgDialog.findViewById(R.id.downloadProgBar);
    	mDownProgBar.setMax(recsToDown);
    	mDownProgBar.setProgress(0);
    	mDownProgTv = (TextView)mProgDialog.findViewById(R.id.downProgTv);
    	mDownProgTv.setText("Downloaded "+0+"/"+recsToDown);
  
    	Utils.uploadOn = Constants.INTERNET_ON_INT;
    	
    	mUplProgBar = (ProgressBar)mProgDialog.findViewById(R.id.uploadProgBar);   	
    	mUplProgBar.setMax(Utils.getTotalStoredRecCount(getApplicationContext())+recsToDown);
    	mUplProgBar.setProgress(Utils.getUploadedRecCount(getApplicationContext())); 	
    	mUplProgTv = (TextView)mProgDialog.findViewById(R.id.uplProgTv);
    	mUplProgTv.setText("Uploaded: "+Utils.getUploadedRecCount(getApplicationContext())+"/"+(Utils.getTotalStoredRecCount(getApplicationContext())+Utils.numberHR)+
				" ("+(Utils.getTotalStoredRecCount(getApplicationContext())+" on disk)"));
    	
    	Button stopBtn = (Button)mProgDialog.findViewById(R.id.stopBtn);
    	stopBtn.setOnClickListener(new OnClickListener()
    	{
			@Override
			public void onClick(View v) 
			{
				if(mProgDialog != null)
					mProgDialog.dismiss();
		    	
				//close bluetooth thread
		    	if(mBluetoothHistManager != null)
		    		mBluetoothHistManager.stop();
		    	
		    	//stop store'n'forward service
		    	if(Utils.storeForwServIntent != null)
		    		stopService(Utils.storeForwServIntent);
		    	
		    	createFinishedDownloadDialog();
			}  		
    	});
    	
    	Log.d("Start", "createDownUplProgdialog()--> going to show mProgDialog");
    	mProgDialog.show();
    }
  
    private void deactivateAccountDialog()
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Start.this);
					alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
			 
					alertDialogBuilder.setMessage(getResources().getString(R.string.deactivation_confirmation))
			
						.setCancelable(false)
						.setPositiveButton("OK",new DialogInterface.OnClickListener() 
						{
							public void onClick(DialogInterface dialog,int id) 
							{
								dialog.dismiss();
			    				Utils.setAccountActivationState(getApplicationContext(), false); //deactivate account
			    				Utils.setCredentialsData(getApplicationContext(), "", "", "", -1, -1);
			    				
			    				mActivateBtn.setEnabled(true); 
			    	        	mActivateBtn.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.activated_off), null, null, null);
			    	        	mActivateBtn.setText(getResources().getString(R.string.activate_msg));
			    	        	
			    		        mActivateBtn.setOnClickListener(new OnClickListener()
			    		        {
			    					@Override
			    					public void onClick(View v) 
			    					{
			    						Intent intent = new Intent(Start.this, ManageAccount.class);
			    						startActivity(intent);				
			    					}        	
			    		        });
							}
						})
						.setNegativeButton("Cancel",new DialogInterface.OnClickListener() 
						{
							public void onClick(DialogInterface dialog,int id) 
							{
								dialog.dismiss();				
							}
						});
						
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();		
    			}
    		}
    	});
    }
    
    private void createFinishedDownloadDialog()
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Start.this);
					alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
			 
					alertDialogBuilder.setMessage("Records downloaded: "+Utils.counterHR+"/"+Utils.numberHR+"\nRecords uploaded: "
						+Utils.getUploadedRecCount(getApplicationContext())+"/"+Utils.getUploadedRecCount(getApplicationContext()))
			
						.setCancelable(false)
						.setPositiveButton("OK",new DialogInterface.OnClickListener() 
						{
							public void onClick(DialogInterface dialog,int id) 
							{
								dialog.dismiss();
								
								if(mProgBarHandler != null)
									mProgBarHandler.removeCallbacks(mUpdateBars);
							}
						});
						
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();		
    			}
    		}
    	});
    }
    
    private void noHistRecDialog()
    {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Start.this);
		alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
 
		alertDialogBuilder.setMessage("No history records to download")
			.setCancelable(false)
			.setPositiveButton("OK",new DialogInterface.OnClickListener() 
			{
				public void onClick(DialogInterface dialog,int id) 
				{
					dialog.dismiss();
					
					//close bluetooth thread
			    	if(mBluetoothHistManager != null)
			    	{
			    		mBluetoothHistManager.stop();
			    		mBluetoothHistManager = null;
			    	}
				}
			});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
    }
    
    private void recordCountDialog(final int[] result)
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Start.this);
					alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
			 
					alertDialogBuilder.setMessage("number of records saved on Db: " +result[0]+" number of uploaded records: "+result[1])
			
						.setCancelable(false)
						.setPositiveButton("ok",new DialogInterface.OnClickListener() 
						{
							public void onClick(DialogInterface dialog,int id) 
							{
								dialog.dismiss();
							}
						});
						
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();		
    			}
    		}
    	});
    }
}