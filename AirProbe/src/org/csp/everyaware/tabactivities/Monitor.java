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

package org.csp.everyaware.tabactivities;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.bluetooth.BluetoothBroadcastReceiver;
import org.csp.everyaware.bluetooth.BluetoothManager;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Monitor extends Activity
{	
	private BluetoothBroadcastReceiver mReceiver;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter = null;
	
	private ProgressDialog mProgressDialog;
	private int mConnAttempts = 1;
	
	private TextView mBoxInfosTextView;
	private TextView mSourceSessionDetailsTv;
	private TextView mSemanticSessionDetailsTv;
	private TextView mCityNameTv;
	private TextView mGpsOverrideTv;
	private TextView mPosTv;
	private TextView mMd5UidTv;
	
    private MediaPlayer mMediaPlayer;
    
    private DbManager mDbManager;
    private HistoryGraphManager mHistoryGraphManager;
    private DbGraphManager mDbGraphManager;

    private int mTotalRecordsOnDb = 0;
    private int mUploadedRecordsOnDb = 0;
    
    private Toast mExitToast; //toast showed when user press back button
    
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
	
    /* il numero di record uploadati relativo alla traccia viene incrementato dal metodo updateUploadedRecord() di
     * DbManager assieme all'aggiornamento dei record con il timestamp di invio degli stessi, tramite una modalità 
     * veloce basata su SqLite statement. Il tutto viene avviato all'interno della classe di Store'n'forward
     * 
     * il numero di record di una data traccia viene incrementato nei metodi saveRecord e saveHistoryRecordsSeries di DbManager
     * 
     * eliminazione di una entry track: all'interno del metodo loadAllTracks() di DbManager, verifico che nella tabella dei record, per l'attuale
     * sid caricato dalla tabella dei tracciati, ci sia almeno un record. Se non c'è, allora la corrispondente entry nella tabella dei tracciati
     * viene cancellata
     */
    
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.d("Monitor", "******************************onCreate()******************************");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.monitor);
		
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
        
        mDbManager = DbManager.getInstance(getApplicationContext());
		Utils.setStep(Constants.SBOX, getApplicationContext());

		getButtonRefs();

		String boxInfoStr = "MAC: "+Utils.getDeviceAddress(getApplicationContext()) +"\n";
		boxInfoStr += Utils.getBoxInfoMsg(getApplicationContext());
		mBoxInfosTextView.setText(boxInfoStr);
		mBoxInfosTextView.setMovementMethod(new ScrollingMovementMethod());
		
		if(Utils.getUsePhoneGpsIndex(getApplicationContext()) == 0)
			mGpsOverrideTv.setText("Not Active (use Gps sensor box data if available)");
		else
			mGpsOverrideTv.setText("Activated (override Gps sensor box data if phone Gps works)");

		String uidMd5 = Record.md5(Utils.readUniquePhoneID(getApplicationContext()));
		if((uidMd5 != null)&&(uidMd5.length() > 16))
			mMd5UidTv.setText(uidMd5.substring(0, 16)+ "\n" +uidMd5.substring(16));
		else
			mMd5UidTv.setTag(uidMd5);
		
		mBluetoothManager = BluetoothManager.getInstance(null, null);
		mBluetoothManager.setSensorBoxHandler(mSensorBoxHandler);	
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mReceiver = new BluetoothBroadcastReceiver(getApplicationContext(), mSensorBoxHandler);

		mHistoryGraphManager = new HistoryGraphManager();
		mDbGraphManager = new DbGraphManager();
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("Monitor", "onStop()");
	}	    
    
    @Override
    public void onDestroy()
    {
    	Utils.paused = false;
    	super.onDestroy(); 	
    	Log.d("Monitor", "onDestroy()");
    		
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release(); 
    }      
    
    @Override
    public void onPause()
    {
    	super.onPause();  	
    	Log.d("Monitor", "onPause()");
    	
    	Utils.paused = true;
    	
    	mSensorBoxHandler.removeCallbacks(mDownloadedHistRecords);
    	mSensorBoxHandler.removeCallbacks(mCountRecordsOnDb);
    	mSensorBoxHandler.removeCallbacks(mGetLastRecIdRunnable);
    }
    
	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("Monitor", "onResume()");
		
		Utils.paused = false; 
		
		mWakeLock.acquire(); //acquire partial wake lock 
		
		if(mCityNameTv != null)
			mCityNameTv.setText(Utils.nearestCityName);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mSensorBoxHandler.postDelayed(mDownloadedHistRecords, 1000);
		mSensorBoxHandler.postDelayed(mCountRecordsOnDb, 1000);		
		mSensorBoxHandler.postDelayed(mGetLastRecIdRunnable, 1000);
	}    
	
	@Override
	public void onBackPressed()
	{
		closeAppDialog();
		/*
		if(mExitToast != null)
		{		
			if(mExitToast.getView().isShown())
			{				
				mExitToast.cancel();
				mExitToast = null;
				closeAppDialog();
			}
			else
			{
				//mExitToast = Toast.makeText(getApplicationContext(), R.string.press_again_to_exit_msg, Toast.LENGTH_SHORT);
				mExitToast.show();	
			}
		}
		else
		{
			mExitToast = Toast.makeText(getApplicationContext(), R.string.press_again_to_exit_msg, Toast.LENGTH_SHORT);
			mExitToast.show();		
		}*/
	}
	
	private void getButtonRefs()
	{
		mSourceSessionDetailsTv = (TextView)findViewById(R.id.sourceSessionDetails_content_textView);
		mSemanticSessionDetailsTv = (TextView)findViewById(R.id.semanticSessionDetails_content_textView);
		
		mBoxInfosTextView = (TextView)findViewById(R.id.info_content_textView);
				
		mCityNameTv = (TextView)findViewById(R.id.cityname_content_textView);    
		mGpsOverrideTv = (TextView)findViewById(R.id.phone_gps_override_content_tv);
                
		mMd5UidTv = (TextView)findViewById(R.id.uid_md5_content_textView);
		
        mPosTv = (TextView)findViewById(R.id.actualCoords_content_textView);
	}

	private Runnable mCountRecordsOnDb = new Runnable()
	{
		@Override
		public void run() 
		{		
			mTotalRecordsOnDb = Utils.getTotalStoredRecCount(getApplicationContext());
			mUploadedRecordsOnDb = Utils.getUploadedRecCount(getApplicationContext());
			
			Log.d("CountRecordsOnDb", "run()--> total records on Db: " +mTotalRecordsOnDb+ " uploaded records: " +mUploadedRecordsOnDb);
			
			//passed parameters: number of not uploaded records stored on DB, number of uploaded records stored on DB
			mDbGraphManager.refreshData(mTotalRecordsOnDb-mUploadedRecordsOnDb, mUploadedRecordsOnDb);	
			
			mSensorBoxHandler.postDelayed(mCountRecordsOnDb, 5000);
		}	
	};
		
	private Runnable mDownloadedHistRecords = new Runnable()
	{
		@Override
		public void run() 
		{
			Log.d("DownloadedHistRecords", "run()--> # history records: "+Utils.numberHR+ " # downloaded history records: " +Utils.counterHR);
			
			if(Utils.numberHR == 0)
				mHistoryGraphManager.refreshData(100, 0, 0);
			else
				mHistoryGraphManager.refreshData(0, Utils.numberHR-Utils.counterHR, Utils.counterHR);
			mSensorBoxHandler.postDelayed(mDownloadedHistRecords, 5000);
		}	
	};

    /***************************  HANDLER ******************************************************/
    
    private Handler mSensorBoxHandler = new Handler()
    {
    	String deviceAddress;
    	BluetoothDevice remoteDevice;
    	
    	@Override
    	public void handleMessage(Message msg)
    	{    		
			switch(msg.what)
    		{
				//sezione invocata nel caso in cui, a seguito dello spegnimento del dispositivo bluetooth
				//sullo smartphone e della caduta della connessione aperta verso la sensor box, viene
				//riattivato il dispositivo bluetooth e ci si riconnette alla sensor box
				case Constants.BT_ACTIVATED:
					//chiudo progress dialogo di attivazione bluetooth
					if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
						mProgressDialog.dismiss();
					
					mConnAttempts = 1;
					
		    		try
		    		{
		    			deviceAddress = Utils.getDeviceAddress(getApplicationContext());
		    			remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		    		}
		    		catch(IllegalArgumentException e)
		    		{
		    			e.printStackTrace();
		    		}
		    		
					//invoco il metodo connect() del BluetoothManager che inizia la connessione
					mBluetoothManager.connect(remoteDevice);
				break;
				
				case Constants.CONNECTION_FAILED:
					Log.d("SensorBoxHandler", "Failed to connect to selected device");
					//mConnStatusTextView.setText(Constants.DISCONNECTED);
					
					//chiudo la progress dialog di apertura connessione verso dispositivo remoto
					if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
						mProgressDialog.dismiss();
					
		    		try
		    		{
		    			deviceAddress = Utils.getDeviceAddress(getApplicationContext());
		    			remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		    		}
		    		catch(IllegalArgumentException e)
		    		{
		    			e.printStackTrace();
		    		}
		    		
					//numero massimo tentativi di riconnessione: 3
					if(mConnAttempts < 3)
					{
						mConnAttempts++;

						createProgressDialog("Connection attempt #" +mConnAttempts+ " to " +deviceAddress, false);
						//invoco il metodo connect() del BluetoothManager che inizia la connessione
						mBluetoothManager.connect(remoteDevice);
					}
					else
					{
						//avviso l'utente dei 3 tentativi di connessione falliti
						
						//chiudo la progress dialog di apertura connessione verso dispositivo remoto
						if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
							mProgressDialog.dismiss();
						
						Log.d("MonitorHandler", "Failed 3 reconnection attempts");
						
						//play sound alert when 3 connection attempts fail
						playConnAttempFailed();
						connAttemptFailedDialog();
					}
										
				break;	
				
				case Constants.CONNECTION_LOST:
					Log.d("MonitorHandler", "Connection lost");
					//mConnStatusTextView.setText(Constants.DISCONNECTED);
					
		    		try
		    		{
		    			deviceAddress = Utils.getDeviceAddress(getApplicationContext());
		    			remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		    		}
		    		catch(IllegalArgumentException e)
		    		{
		    			e.printStackTrace();
		    		}
		    		
					//verifico che il dispositivo Bluetooth dello smartphone è attivato, se lo è 
					//provo a ristabilire la connessione verso la sensor box
					if(mBluetoothAdapter.isEnabled())
					{
						mConnAttempts = 1;						
						createProgressDialog("Connection attempt " +mConnAttempts+ " to " +deviceAddress, false);
						
						mBluetoothManager.connect(remoteDevice);
					}
					else
					{
						//broadcastreceiver registered with ACTION_STATE_CHANGED intent, helpful to catch
						//the event of enabling bluetooth adapter on smartphone
	        	        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	        	        
	        	        mBluetoothAdapter.enable(); //enable bluetooth adapter on smartphone  
	        			
	        			createProgressDialog("Activating Bluetooth", true); 
					}
				break;
				
				case Constants.STATE_NONE:
					Log.d("MonitorHandler", "STATE NONE ");
					//mConnStatusTextView.setText(Constants.DISCONNECTED);
					
				break;
				case Constants.STATE_CONNECTING:
					Log.d("MonitorHandler", "STATE_CONNECTING");
				break;
				case Constants.STATE_CONNECTED:
					Log.d("MonitorHandler", "STATE_CONNECTED");
					//mConnStatusTextView.setText(Constants.CONNECTED);
					
					mConnAttempts = 1;
					//progress dialog about opening connection is closed
					if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
						mProgressDialog.dismiss();										
				break;	
				
				case Constants.DEVICE_GPS_ON:
			
				break;
				
				case Constants.DEVICE_GPS_OFF:

				break;
				
				case Constants.SENSOR_BOX_MAC_NOT_READ:
					Log.d("MonitorHandler", "SENSOR BOX MAC ADDRESS NOT READ!!!");
					
					 sensorBoxMacNotReadDialog();
				break;
    		}
    	}
    };
    
    /************************* DIALOGS ***************************************************************/
    
    public void connAttemptFailedDialog() 
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_conn_attempts_failed)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable( false )
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{
        			stopSound();
        			
					mConnAttempts = 1;
					
					String deviceAddress = Utils.getDeviceAddress(getApplicationContext());

					BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(deviceAddress);
					createProgressDialog("Connection attempt #" +mConnAttempts+ " to " +deviceAddress, false);

					//connection to remote device
					mBluetoothManager.connect(remoteDevice);	
        		}
        	})
    		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() 
    		{
    			public void onClick(DialogInterface dialog, int id) 
    			{
    				stopSound();
        			
    				closeApp();	
    			}
    		});
        
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void closeAppDialog() 
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_close_app)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable( false )
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{
        			closeApp();
        		}
        	})
    		.setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() 
    		{
    			public void onClick(DialogInterface dialog, int id) 
    			{
    				dialog.dismiss();
    			}
    		});
        
        AlertDialog alert = builder.create();
        alert.show(); 
    }
    
    public void sensorBoxMacNotReadDialog() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Monitor.this);
			        builder.setMessage(R.string.alert_dialog_sensorbox_mac_not_read)
			        	.setIcon(android.R.drawable.ic_dialog_info)
			        	.setTitle(R.string.app_name)
			        	.setCancelable( false )
			        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
			        	{
			        		public void onClick(DialogInterface dialog, int id) 
			        		{
			        			dialog.dismiss();
			        			closeApp();
			        		}
			        	})
			    		.setNegativeButton(R.string.continue_not_recommended, new DialogInterface.OnClickListener() 
			    		{
			    			public void onClick(DialogInterface dialog, int id) 
			    			{     			
			    				dialog.dismiss();	
			    			}
			    		});
			        
			        AlertDialog alert = builder.create();
			        alert.show(); 
    			}
    		}
    	});
    }
    
    /********************** CLOSE APP *************************************************************************/
    
    public void closeApp()
    { 	
    	Utils.backToHome = true;
    	
    	//stop store'n'forward service
    	if(Utils.storeForwServIntent != null)
    		stopService(Utils.storeForwServIntent);
    		
		mConnAttempts = 1;
		
    	//stop all threads
    	if(mBluetoothManager != null)
    		mBluetoothManager.stop();
    	mBluetoothManager = null;
    	
    	Graph.stopCallerThread();
    	Map.stopCallerThread();
    	
    	//clear shared prefs
    	Utils.deleteSharedPrefs(getApplicationContext());
    	  	
    	//useful to complete kill the app
    	Monitor.this.finish();
    	android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        getParent().finish();
    }   
    
	/*************** TO PLAY SOUND WHEN BLUETOOTH CONNECTION ATTEMPT FAILED DIALOG SHOWS **********************/
	
    public void playConnAttempFailed()
    {
        try
        {
        	Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); 
        	mMediaPlayer = new MediaPlayer();
        	mMediaPlayer.setDataSource(this, alert);
        	final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        	
        	if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) 
        	{
        		 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        		 mMediaPlayer.setLooping(true);
        		 mMediaPlayer.prepare();
        		 mMediaPlayer.start();     	
        	}
        } 
        catch (Exception e)
        {
        	e.printStackTrace();
        }
    }
    
    public void stopSound()
    {
		if(mMediaPlayer != null)
		{
			mMediaPlayer.release();
			mMediaPlayer = null;
		}
    }
    
	/************************ HISTORY RECORDS TOTAL/DOWNLOADED FROM SENSOR BOX GRAPH MANAGER ************************/
	
	private class HistoryGraphManager
	{
		public final int COLOR_GREEN = Color.parseColor("#62c51a");
		public final int COLOR_ORANGE = Color.parseColor("#ff6c0a");
		public final int COLOR_GRAY = Color.GRAY;
		
		//contiene elemento di tipo layout a cui sarà agganciato il grafico
		private LinearLayout mChartLayout;
		
	    //elemento di tipo View di Android
	    private GraphicalView mChartView; 

	    private CategorySeries mSeries = new CategorySeries("");
	    
	    public HistoryGraphManager()
	    {
	    	mChartLayout = (LinearLayout)findViewById(R.id.hi_chart);
	    	
	        mChartView = ChartFactory.getPieChartView(getApplicationContext(), getDataSet(100, 0, 0), getRenderer());	        
	        
			mChartView.setOnClickListener(new View.OnClickListener() 
			{
		        public void onClick(View v) 
		        {
		        	String message = "Num of history records on box: " +Utils.numberHR+ "\nNum of history records downloaded: " +Utils.counterHR;
		        	Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
		        }
			});
			
	        mChartLayout.addView(mChartView);
	    }
	    
	    private DefaultRenderer getRenderer()
		{
			int[] colors = new int[] { COLOR_GRAY, COLOR_GREEN, COLOR_ORANGE};
	 
			DefaultRenderer defaultRenderer = new DefaultRenderer();
			for (int color : colors)
			{
				SimpleSeriesRenderer simpleRenderer = new SimpleSeriesRenderer();
				simpleRenderer.setColor(color);
				defaultRenderer.addSeriesRenderer(simpleRenderer);
			}
			
			defaultRenderer.setShowLabels(false);
			defaultRenderer.setShowLegend(false);
			defaultRenderer.setZoomEnabled(false);
			defaultRenderer.setPanEnabled(false);
			return defaultRenderer;
		}
	    
	    private CategorySeries getDataSet(int empty, int income, int costs)
		{
	    	mSeries.add("", empty);
			mSeries.add("History records", costs);
			mSeries.add("Downloaded history records", income);
			return mSeries;
		}
	    
	    public void refreshData(int empty, int income, int costs)
	    {
			mSeries.clear();
			
			mSeries.add("", empty);
			mSeries.add("History records", costs);
			mSeries.add("Downloaded history records", income);
	    	
	    	mChartView.repaint();
	    }
	}
	
	/******************* TOTAL RECORDS ON DB/UPLOADED RECORDS GRAPH MANAGER *************************************/
	
	private class DbGraphManager
	{
		public final int COLOR_GREEN = Color.parseColor("#62c51a");
		public final int COLOR_ORANGE = Color.parseColor("#ff6c0a");
		
		//contiene elemento di tipo layout a cui sarà agganciato il grafico
		private LinearLayout mChartLayout;
		
	    //elemento di tipo View di Android
	    private GraphicalView mChartView; 
		
	    private CategorySeries mSeries = new CategorySeries("");
	    
	    public DbGraphManager()
	    {
	    	mChartLayout = (LinearLayout)findViewById(R.id.db_chart);
	    	
	        mChartView = ChartFactory.getPieChartView(getApplicationContext(), getDataSet(50, 50), getRenderer());	        
	        
			mChartView.setOnClickListener(new View.OnClickListener() 
			{
		        public void onClick(View v) 
		        {
		        	String message = "Total num of records on DB: " +mTotalRecordsOnDb+ "\nNum of not uploaded records: " +(mTotalRecordsOnDb-mUploadedRecordsOnDb)+ "\nNum of records uploaded: " +mUploadedRecordsOnDb;
		        	Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
		        }
			});
			
	        mChartLayout.addView(mChartView);
	    }
	    
	    private DefaultRenderer getRenderer()
		{
			int[] colors = new int[] {COLOR_ORANGE, COLOR_GREEN };
	 
			DefaultRenderer defaultRenderer = new DefaultRenderer();
			for (int color : colors)
			{
				SimpleSeriesRenderer simpleRenderer = new SimpleSeriesRenderer();
				simpleRenderer.setColor(color);
				defaultRenderer.addSeriesRenderer(simpleRenderer);
			}
			
			defaultRenderer.setShowLabels(false);
			defaultRenderer.setShowLegend(false);
			defaultRenderer.setZoomEnabled(false);
			defaultRenderer.setPanEnabled(false);
			return defaultRenderer;
		}
	    
	    private CategorySeries getDataSet(int notuploaded, int uploaded)
		{
	    	mSeries.add("Not uploaded records", notuploaded);
	    	mSeries.add("Uploaded records", uploaded);
			return mSeries;
		}
	    
	    public void refreshData(int notuploaded, int uploaded)
	    {
			mSeries.clear();
			
	    	mSeries.add("Not uploaded records", notuploaded);
	    	mSeries.add("Uploaded records", uploaded);
	    	
	    	mChartView.repaint();
	    }
	}
	
	//load last recorded (on db) record and show position in text view
	private Runnable mGetLastRecIdRunnable = new Runnable()
	{				
		@Override
		public void run() 
		{			
			Record tmp = Utils.lastSavedRecord;
			
			if((tmp != null)&&(tmp.mValues[0] != 0))
			{
				/*** FOR SEMANTIC WINDOW : WHEN SOURCE SESSION NUMBER CHANGES AND A SEMANTIC SESSION WINDOW IS OPEN, CLOSE THE SESSION AND BACK TO NORMAL STATE ***/
				if(Utils.semanticWindowStatus)
				{
					if(Utils.getSourceSessionNumber(getApplicationContext()) < tmp.mSourceSessionNumber)
					{
						Log.d("GetLastRecIdRunnable", "run()--> actual source session number: "+Utils.sourceSessionNumber+" NEW RECORD source session number: "+tmp.mSourceSessionNumber);
						Log.d("GetLastRecIdRunnable", "run()--> actual source point number: "+Utils.sourcePointNumber+" NEW RECORD source point number: "+tmp.mSourcePointNumber);
						Log.d("GetLastRecIdRunnable", "run()--> CLOSING SEMANTIC SESSION WINDOW");
						
						mDbManager.closeSemanticSessionEntry(Utils.sourcePointNumber);
						
						Utils.show_bc_cumulative = false;
							
						//set bc cumulative window status to false (= closed)
						Utils.semanticWindowStatus = false;
						
						//reset source session number
						Utils.setSourceSessionNumber(getApplicationContext(), -1);
						
						Toast.makeText(getApplicationContext(), "Semantic session successfully closed", Toast.LENGTH_LONG).show();
			
						//debug print
						mDbManager.printSemanticSessionEntries();
					}
				}
				
				Utils.sourceSessionNumber = tmp.mSourceSessionNumber;
				Utils.sourcePointNumber = tmp.mSourcePointNumber;
									
				/*** END FOR SEMANTIC WINDOW ***/
				
				double lat = tmp.mValues[0];
				double lon = tmp.mValues[1];
				
				int lat_int = (int)(lat * 1000);
				int lon_int = (int)(lon * 1000);
				
				lat = (double)lat_int / 1000;
				lon = (double)lon_int / 1000;
				
				mPosTv.setText(lat+"°, "+lon+"°");
				
				if(mSourceSessionDetailsTv != null)
					mSourceSessionDetailsTv.setText(tmp.mSourceSessionSeed+"\n"+tmp.mSourceSessionNumber+" - "+tmp.mSourcePointNumber);
				
				if(mSemanticSessionDetailsTv != null)
				{
					if((tmp.mSemanticSessionSeed != null)&&(!tmp.mSemanticSessionSeed.equals("")))
						mSemanticSessionDetailsTv.setText(tmp.mSemanticSessionSeed+"\n"+tmp.mSemanticSessionNumber+" - "+tmp.mSemanticPointNumber);
					else
						mSemanticSessionDetailsTv.setText("Semantic session for BC cumulative closed");
				}
			}
			
			mSensorBoxHandler.postDelayed(mGetLastRecIdRunnable, 1000);
		}
	};
	
    public void createProgressDialog(String msg, boolean cancelable)
    {
    	if((mProgressDialog != null)&&(!mProgressDialog.isShowing()))
    		mProgressDialog = ProgressDialog.show(Monitor.this, getResources().getString(R.string.app_name), msg, true, true);
    }
}
