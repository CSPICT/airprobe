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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYValueSeries;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class Graph extends Activity
{
	private Handler mHandler;
	private DbManager mDbManager;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter = null;
	private GraphManager mGraphManager;
	private BluetoothBroadcastReceiver mReceiver;
	
	private static boolean mIsRunning;
	
	private Record mPrecRecord = null;
	private Record mNewRecord = null;
	private List<Record>mRecords = new ArrayList<Record>();
	
	private double mPrecRecordId = -1;
	private long mPrecRecordSysTs = 0;
	private long mEndWindowTs = 0;
	private long mStartWindowTs = 0;
	
	private ProgressDialog mProgressDialog;
	private int mConnAttempts = 1;
	
	//change length in minutes of sliding window
	private Button mChangeBtn;
	
	//new data geo/not geo-referenced record image view indicator
	private ImageView mNewDataIv;
	
	//status icons
	private ImageView mGpsStatus;
	private ImageView mBtStatus;
	private ImageView mInterUplStatus;
	
	//timestamps showed under graph
	private TextView mTsStartTv, mTsMiddleTv, mTsEndTv;
	private TextView mAvgCoTv, mAvgNo2Tv, mVocTv, mO3Tv, mBcTv;
	
	private final long HALF_HOUR = 30*60*1000;
	private final long ONE_MINUTE = 60*1000;
	private final long FIVE_MINUTES = 5*60*1000;
	private final long FIFTEEN_MINUTES = 15*60*1000;
	
	private long mMillisAgo;
	
	private CallerThread mCallerThread;
	
	//options variables
	private CharSequence[] options1; //ages for uploaded records
	private CharSequence[] options2; //store'n'forward frequencies
	private CharSequence[] options3; //history download on/off
	private CharSequence[] options4; //upload records only on wifi network or both wifi/mobile network	
	private AlertDialog alert = null;
	
	private final int lineWidth = 3;
	
	private Context mContext;
    private MediaPlayer mMediaPlayer;
	
    private Toast mExitToast; //toast showed when user press back button

    private CharSequence[] sliding_window_lengths; 
    private final int[] minutes = {30, 1, 5, 15}; //lengths of slinding window in minutes
    private int lengthsIndex; //index on two above arrays
    
    private Resources mRes;
    
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
    
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.graph);

        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
        
		Utils.setStep(Constants.GRAPH, getApplicationContext());
		
		mDbManager = DbManager.getInstance(getApplicationContext());
		mDbManager.openDb();
		mRes = getResources();
		
		mBluetoothManager = BluetoothManager.getInstance(null, null);
		mBluetoothManager.setGraphHandler(mGraphHandler);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//istanzio il broadcast receiver (utile per ricevere evento di riattivazione dispositivo bluetooth
		//sullo smartphone)
		mReceiver = new BluetoothBroadcastReceiver(getApplicationContext(), mGraphHandler);
		
		//status icons references
		mGpsStatus = (ImageView)findViewById(R.id.gpsStatusIv);
		mBtStatus = (ImageView)findViewById(R.id.btStatusIv);
		mInterUplStatus = (ImageView)findViewById(R.id.interUplStatusIv);
		
		//status icon initializations
		mGpsStatus.setBackgroundResource(R.drawable.gps_off);
		mBtStatus.setBackgroundResource(R.drawable.bt_on);	
		
		//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
		int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
		
		//1 - is internet connection available? 
		boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
		
		boolean connectivityOn = false;
		
		//if user wants to upload only on wifi networks, connectivity[0] (network connectivity) must be true
		if(networkTypeIndex == 0)
		{
			if(connectivity[0])
				connectivityOn = true;
			else
				connectivityOn = false;
		}
		else //if user wants to upload both on wifi/mobile networks
			connectivityOn = connectivity[0] || connectivity[1];
		
		if(connectivityOn)
			mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
		else
			mInterUplStatus.setBackgroundResource(R.drawable.internet_off);
   		
		lengthsIndex = 0;
		sliding_window_lengths = getResources().getStringArray(R.array.sliding_window_lengths);		
		mChangeBtn = (Button)findViewById(R.id.changeBtn);
		mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
		mChangeBtn.setOnClickListener(mChangeBtnOnClickListener);
		mMillisAgo = HALF_HOUR;
		
		//get reference to text views containing last values for sensors and bc
		mAvgCoTv = (TextView)findViewById(R.id.avgCoTv);
		mAvgNo2Tv = (TextView)findViewById(R.id.avgNo2Tv);
		mVocTv = (TextView)findViewById(R.id.vocTv);
		mO3Tv = (TextView)findViewById(R.id.o3Tv);
		mBcTv = (TextView)findViewById(R.id.bcTv);
		
		//get reference to new data indicator
		mNewDataIv = (ImageView)findViewById(R.id.newDataIv);		
		mTsStartTv = (TextView)findViewById(R.id.tsStartTv);
		mTsMiddleTv = (TextView)findViewById(R.id.tsMiddleTv);
		mTsEndTv = (TextView)findViewById(R.id.tsEndTv);
		
		mGraphManager = new GraphManager();	
		
		mHandler = new Handler();
		mIsRunning = true;
		
		//starting runnable that loads last inserted record (runs last inserted runnable) and draws segments and points on graph
		mCallerThread = new CallerThread();
		mCallerThread.start();
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("Graph", "onStop()");
		//mHandler.removeCallbacks(mGetLastRecIdRunnable);
	}	    
    
    @Override
    public void onDestroy()
    {
    	Utils.paused = false;
    	super.onDestroy(); 	
    	Log.d("Graph", "onDestroy()");

    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release(); 
    }      
    
    @Override
    public void onPause()
    {
    	super.onPause(); 
    	Log.d("Graph", "onPause()");

    	Utils.paused = true;
    	
    	try
    	{
    		if(mServiceReceiver != null)
    			//unregister receiver for messages from store'n'forward service
    			unregisterReceiver(mServiceReceiver);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	
		mIsRunning = false;		
		mConnAttempts = 1;
		//rimuovo eventuali runnable schedulati nel futuro
		mHandler.removeCallbacks(mGetLastRecIdRunnable);
		mCallerThread = null;
		
    }
    
	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("Graph", "******************************onResume()****************************");
		
		Utils.paused = false;
		
		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
		if(Utils.uploadOn == Constants.INTERNET_ON_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
		else if(Utils.uploadOn == Constants.INTERNET_OFF_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.internet_off);
		else if(Utils.uploadOn == Constants.UPLOAD_ON_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.upload);
		
		if(Utils.btConnectionOn == Constants.STATE_CONNECTED)
			mBtStatus.setBackgroundResource(R.drawable.bt_on);
		else if(Utils.btConnectionOn == Constants.CONNECTION_LOST)
			mBtStatus.setBackgroundResource(R.drawable.bt_off);
		
		//open progress dialog data loading
		showProgressDialog("Loading data...", false);
		
		mHandler.postDelayed(new Runnable()
		{
			@Override
			public void run() 
			{
				mContext = getApplicationContext();
				mGraphManager.removeAllData();
				
				Utils.setStep(Constants.GRAPH, mContext);
				
				mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
				if(lengthsIndex == 0)
				{
					mGraphManager.setXAxisMax(900);
					mMillisAgo = HALF_HOUR;
				}
				else if(lengthsIndex == 1)
				{
					mGraphManager.setXAxisMax(30);
					mMillisAgo = ONE_MINUTE;
				}
				else if(lengthsIndex == 2)
				{
					mGraphManager.setXAxisMax(150);	
					mMillisAgo = FIVE_MINUTES;
				}
				else if(lengthsIndex == 3)
				{
					mGraphManager.setXAxisMax(450);
					mMillisAgo = FIFTEEN_MINUTES;
				}
				
				mHandler.post(mLoadGraphData);

				mIsRunning = true;
				
				//starting runnable that loads last inserted record (runs last inserted runnable) and draws segments and points on graph
				mCallerThread = new CallerThread();
				mCallerThread.start();
				
				//register receiver to receiver messages from store'n'forward service
				IntentFilter internetOnFilter = new IntentFilter(Constants.INTERNET_ON);
				registerReceiver(mServiceReceiver, internetOnFilter);
				IntentFilter internetOffFilter = new IntentFilter(Constants.INTERNET_OFF);
				registerReceiver(mServiceReceiver, internetOffFilter);
				IntentFilter uploadOnFilter = new IntentFilter(Constants.UPLOAD_ON);
				registerReceiver(mServiceReceiver, uploadOnFilter);
				IntentFilter uploadOffFilter = new IntentFilter(Constants.UPLOAD_OFF);
				registerReceiver(mServiceReceiver, uploadOffFilter);
				
				//register receiver for messages from gps tracking service
				/*
				IntentFilter phoneGpsOnFilter = new IntentFilter(Constants.PHONE_GPS_ON);
				registerReceiver(mGpsServiceReceiver, phoneGpsOnFilter);
				IntentFilter phoneGpsOffFilter = new IntentFilter(Constants.PHONE_GPS_OFF);
				registerReceiver(mGpsServiceReceiver, phoneGpsOffFilter);	
				*/
			}			
		}, 500);
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
	
	private Runnable hideData = new Runnable()
	{
		@Override
		public void run() 
		{
			mNewDataIv.setVisibility(View.INVISIBLE);
		}		
	};
		
	private Runnable mLoadGraphData = new Runnable()
	{
		@Override
		public void run() 
		{
			//ottengo mActualTs (inizializzato ad ora), mPrecRecordSysTs (inizializzato a mezz'ora fa) e il valore intermedio
			//e li visualizzo nelle apposite text view poste sotto il grafico
			updateTsTextViews();

			//inizialmente inizializzo mPrecRecordSysTs al valore di mezz'ora fa. Poi conterrà, invece, il timestamp del record
			//immediatamente precedente a quello attualmente disegnato
			mPrecRecordSysTs = mStartWindowTs;
			
			mRecords = mDbManager.loadRecordsFromTimestamp(mStartWindowTs, 2);
			
			Log.d("LoadGraphData", "run()--> number of loaded records: " +mRecords.size());

			mGraphManager.addAllData(mRecords);
			mGraphManager.repaint();
			
			if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
				mProgressDialog.dismiss();
		}		
	};
	
	private void updateTsTextViews()
	{
		mEndWindowTs = new Date().getTime(); //actual time 
		mStartWindowTs = mEndWindowTs - mMillisAgo; //half hour ago, one minute ago, 5 minutes ago, 15 minutes ago
		
		String ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format(mStartWindowTs);		
		mTsStartTv.setText(ts);
		
		//middle between actual time and half hour ago
		ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format( mStartWindowTs + (mEndWindowTs - mStartWindowTs)/2 );
		mTsMiddleTv.setText(ts);
		
		ts = new SimpleDateFormat("HH:mm:ss\nMM/dd/yyyy").format(mEndWindowTs);
		mTsEndTv.setText(ts);
	}
	
	/****************** CARICA DA DB ULTIMO RECORD RICEVUTO *********************************/
	
	int iterations = 0;
	
	//ottiene dalle shared prefs l'id dell'ultimo record salvato sul db e lo carica
	//quando questa activity va in pausa (in seguito ad esempio a passaggio ad un'altra tab)
	//il runnable continua a funzionare, quindi i record e le posizioni vengono aggiunte
	//ai rispettivi array e nulla viene perso
	private Runnable mGetLastRecIdRunnable = new Runnable()
	{				
		@Override
		public void run() 
		{
			double lastRecordId = Utils.getNewRecordId(getApplicationContext());
			
			//verifico che l'id del presunto nuovo record non sia già stato ottenuto all'esecuzione precedente del runnable
			//se così fosse, vorrebbe dire che non si stanno ottenendo dati nuovi dalla sensor box...
			if(mPrecRecordId != lastRecordId)
			{
				Record tmp = Utils.lastSavedRecord;
				
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
				
				if(tmp != null)
				{
					if(tmp.mGpsProvider != null)
					{
						if(tmp.mGpsProvider.equals(Constants.GPS_PROVIDERS[0])) //gps data come from sensorbox
							mGpsStatus.setBackgroundResource(R.drawable.gps_on_sbox);
						else if(tmp.mGpsProvider.equals(Constants.GPS_PROVIDERS[1])) //gps data come from phone device
							mGpsStatus.setBackgroundResource(R.drawable.gps_on_phone);
						else if(tmp.mGpsProvider.equals(Constants.GPS_PROVIDERS[2])) //gps data come from network
							mGpsStatus.setBackgroundResource(R.drawable.gps_on_network);
						else if(tmp.mGpsProvider.equals(Constants.GPS_PROVIDERS[3])) //no gps data
							mGpsStatus.setBackgroundResource(R.drawable.gps_off);
					}
				}
				
				//prima iterazione
				if(mPrecRecord == null)
				{
					if(tmp != null)
					{
						//show new record pin and hide it after 250 millisec
						mNewDataIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hideData, 250);
						
						mPrecRecord = tmp;
						mRecords.add(mPrecRecord);

						mGraphManager.refreshData(mPrecRecord.mValues);
						
						//show new record pin and hide it after 250 millisec
						mNewDataIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hideData, 250);
						
						iterations++;
					}					
				}
				//seconda iterazione
				else if((mPrecRecord != null)&&(mNewRecord == null))	
				{
					if(tmp != null)
					{				
						//show new record pin and hide it after 250 millisec
						mNewDataIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hideData, 250);
						
						mNewRecord = tmp;
						mRecords.add(mNewRecord);
					
						mGraphManager.refreshData(mNewRecord.mValues);
						
						//show new record pin and hide it after 250 millisec
						mNewDataIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hideData, 250);
						
						iterations++;
					}
				}
				//successive iterazioni
				else if((mPrecRecord != null)&&(mNewRecord != null))
				{					
					if(tmp != null)
					{		
						mPrecRecord = mNewRecord;
						mNewRecord = tmp;
						
						if(iterations % 2 == 0)						
						{
							//show new record pin and hide it after 250 millisec
							mNewDataIv.setVisibility(View.VISIBLE);			
							mHandler.postDelayed(hideData, 250);
							
							mRecords.add(mNewRecord);
							
							//Log.d("Graph", "Runnable::run()--> prec record: " +mPrecRecord.toString());
							Log.d("Graph", "Runnable::run()--> new record: " +mNewRecord.toString());
							
							//if first record is older than one hour, remove it from array of records
							if(mRecords.get(0).mSysTimestamp <= (System.currentTimeMillis() - mMillisAgo))
							{
								mRecords.remove(0);
								Log.d("Graph", "********************************* First record removed ********************************");
								
								//mGraphManager.removeOldestData();
							}
							
							mGraphManager.refreshData(mNewRecord.mValues);
							
							//show new record pin and hide it after 250 millisec
							mNewDataIv.setVisibility(View.VISIBLE);			
							mHandler.postDelayed(hideData, 250);
						}
						
						iterations++;
					}
				}
				
				mPrecRecordId = lastRecordId; //backup del record id
			}
			
			else
			{
				mGpsStatus.setBackgroundResource(R.drawable.gps_off);
				Log.d("Graph", "****************** Record già ricevuto!!! *********************************");
			}
		}		
	};
	
	private class CallerThread extends Thread
	{
		public void run()
		{
			while(mIsRunning)
			{
				mHandler.post(mGetLastRecIdRunnable);
			
				try 
				{
					Thread.currentThread().sleep(1000);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
		}
	};
	
	/*************** CLASSE INTERNA DI GESTIONE GRAFICO ****************************************************/
	
	private class GraphManager
	{
		//contiene elemento di tipo layout a cui sarà agganciato il grafico
		private LinearLayout mChartLayout;
		
	    //elemento di tipo View di Android
	    private GraphicalView mChartView; 
	    //si occupa del rendering degli elementi grafici del grafico
	    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	    //insieme delle sequenze di coppie (per rappresentare più sequenze sullo stesso grafico)
	    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	    
	    //co_1, co_2, co_3, co_4, no2_1, no2_2, voc, o3, bc, empty
	    private XYSeries[] mSeries = new XYValueSeries[10];
	    
	    //oggetti che definiscono come vanno renderizzate le varie sequenze di coppie mSeries
	    private XYSeriesRenderer[] mSeriesRenderer = new XYSeriesRenderer[10];
	    
	    //nomi sensori
	    private String[] mSensorNames = {"co_1", "co_2", "co_3", "co_4", "no2_1", "no2_2", "voc", "o3", "bc", ""};
	    
	    //scala verticale (variabile)
	    private int yMax = 5;
	    //numero di elementi lungo l'asse x
	    private int xElems = 5;
	    //private int yLabels = 10;
	    
	    //array che contiene gli 8 valori ricevuti dalla sensor box
	    private double[]mValues = new double[8];
	    
	    //contiene i massimi aggiornati di ognuna delle 9 serie (incluso bc)s
	    private double[]mMaxValues = new double[9];
	    
	    public GraphManager()
	    {
	    	mChartLayout = (LinearLayout)findViewById(R.id.chart);
	    	
	    	xElems = 900;
	    	
	    	//per non mostrare etichette su assi
	    	mRenderer.setShowLabels(false); 
	    	
	    	mRenderer.setZoomEnabled(false);
	    	
			//valori verticali minimo e massimo mostrati in una schermata
	    	//mRenderer.setYAxisMin(1);
	    	//mRenderer.setYAxisMax(yMax);
			
			//valori orizzontali minimo e massimo mostrati in una schermata
	    	//mRenderer.setXAxisMin(0);
	    	//mRenderer.setXAxisMax(xElems-1); //es. per 20 elementi, il range è [0..19]
	    	
			//numero di quadranti lungo gli assi
		    mRenderer.setXLabels(8);
		    //mRenderer.setYLabels(yLabels);
		    
		    mRenderer.setLabelsColor(Color.LTGRAY); //colore etichette
		    mRenderer.setPanEnabled(false, false);
		    mRenderer.setGridColor(Color.GRAY); //colore griglia

		    //graph margins from viewport
		    //left, top, bottom, right
			mRenderer.setMargins(new int[] {0, 0, 0, 0}); 
			mRenderer.setApplyBackgroundColor(true);
			mRenderer.setBackgroundColor(Color.parseColor("#ffffcb"));
			
			//show/hide legend
			mRenderer.setShowLegend(false); 
			mRenderer.setFitLegend(true);
			//mRenderer.setLegendHeight(30);
			mRenderer.setLegendTextSize(24);
			
			//per evitare shrinking del grafico quando è dentro una scrollview
			mRenderer.setInScroll(true); // for inscoroll
			mRenderer.setClickEnabled(true);
			mRenderer.setSelectableBuffer(100);  // for fixed char
			
			//show grid
			mRenderer.setShowGrid(true); 
			
			//radius of the circle around user touch point (useful to catch on touch event)
		    mRenderer.setSelectableBuffer(10);

			//mRenderer.setAntialiasing(true);
			
			//istanzio le serie numeriche e i loro renderizzatori
			for (int i = 0; i < 10; i++)
			{
				mSeries[i] = new XYValueSeries(mSensorNames[i]);
				mSeriesRenderer[i] = new XYSeriesRenderer();				
			}
			
			//graphical aspects of series
		
			//setDottedStroke();
			setLineStroke();
				
			
			/********************* MONOSSIDO CARBONIO (4 sensori --> 4 serie) ****************/
			mSeriesRenderer[0].setColor(Color.parseColor(mRes.getString(R.color.blue))); //colore blue
			mSeriesRenderer[0].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[0].setLineWidth(lineWidth);
			mSeriesRenderer[0].setDisplayChartValues(false);
			mSeriesRenderer[0].setChartValuesTextSize(12);
			mSeriesRenderer[0].setFillBelowLine(true);
			mSeriesRenderer[0].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.blue_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[0]);
		
			mSeriesRenderer[1].setColor(Color.parseColor(mRes.getString(R.color.dark_green))); //colore white
			mSeriesRenderer[1].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[1].setLineWidth(lineWidth);
			mSeriesRenderer[1].setDisplayChartValues(false);
			mSeriesRenderer[1].setChartValuesTextSize(12);
			mSeriesRenderer[1].setFillBelowLine(true);
			mSeriesRenderer[1].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.dark_green_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[1]);
			
			mSeriesRenderer[2].setColor(Color.parseColor(mRes.getString(R.color.red))); //colore red
			mSeriesRenderer[2].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[2].setLineWidth(lineWidth);
			mSeriesRenderer[2].setDisplayChartValues(false);
			mSeriesRenderer[2].setChartValuesTextSize(12);
			mSeriesRenderer[2].setFillBelowLine(true);
			mSeriesRenderer[2].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.red_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[2]);
			
			mSeriesRenderer[3].setColor(Color.parseColor(mRes.getString(R.color.orange))); //colore orange
			mSeriesRenderer[3].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[3].setLineWidth(lineWidth);
			mSeriesRenderer[3].setDisplayChartValues(false);
			mSeriesRenderer[3].setChartValuesTextSize(12);
			mSeriesRenderer[3].setFillBelowLine(true);
			mSeriesRenderer[3].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.orange_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[3]);
			
			/*********************** BIOSSIDO DI AZOTO (2 sensori --> 2 serie) ***************/ 
			
			mSeriesRenderer[4].setColor(Color.parseColor(mRes.getString(R.color.magenta))); //colore magenta
			mSeriesRenderer[4].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[4].setLineWidth(lineWidth);
			mSeriesRenderer[4].setDisplayChartValues(false);
			mSeriesRenderer[4].setChartValuesTextSize(12);
			mSeriesRenderer[4].setFillBelowLine(true);
			mSeriesRenderer[4].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.magenta_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[4]);
			
			mSeriesRenderer[5].setColor(Color.parseColor(mRes.getString(R.color.cyan))); //colore cyan
			mSeriesRenderer[5].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[5].setLineWidth(lineWidth);
			mSeriesRenderer[5].setDisplayChartValues(false);
			mSeriesRenderer[5].setChartValuesTextSize(12);
			mSeriesRenderer[5].setFillBelowLine(true);
			mSeriesRenderer[5].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.cyan_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[5]);
			
			/******************** COMPOSTI VOLATILI ORGANICI (1 sensore --> 1 serie **********/
			
			mSeriesRenderer[6].setColor(Color.parseColor(mRes.getString(R.color.yellow))); //colore yellow
			mSeriesRenderer[6].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[6].setLineWidth(lineWidth);
			mSeriesRenderer[6].setDisplayChartValues(false);
			mSeriesRenderer[6].setChartValuesTextSize(12);
			mSeriesRenderer[6].setFillBelowLine(true);
			mSeriesRenderer[6].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.yellow_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[6]);
			
			/****************************** OZONO ********************************************/
			
			mSeriesRenderer[7].setColor(Color.parseColor(mRes.getString(R.color.green))); //colore green
			mSeriesRenderer[7].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[7].setLineWidth(lineWidth);
			mSeriesRenderer[7].setDisplayChartValues(false);
			mSeriesRenderer[7].setChartValuesTextSize(12);
			mSeriesRenderer[7].setFillBelowLine(true);
			mSeriesRenderer[7].setFillBelowLineColor(Color.parseColor(mRes.getString(R.color.green_trasp)));
			mRenderer.addSeriesRenderer(mSeriesRenderer[7]);  
			
			/************************** BLACK CARBON SERIE *******************************************/
			
			mSeriesRenderer[8].setColor(Color.parseColor(mRes.getString(R.color.dark_gray)));
			mSeriesRenderer[8].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[8].setLineWidth(lineWidth);			
			mSeriesRenderer[8].setStroke(new BasicStroke(Cap.ROUND, Join.ROUND, 0, null, 0));
			/*
			mSeriesRenderer[8].setGradientEnabled(true);
			mSeriesRenderer[8].setGradientStart(1, Color.RED);
			mSeriesRenderer[8].setGradientStop(5, Color.BLUE);
			*/
			mRenderer.addSeriesRenderer(mSeriesRenderer[8]);
			
			/************************* BACKGROUND FAKE SERIE *****************************************/
			
			//mSeriesRenderer[9].setColor(Color.parseColor("#ffffcb")); //colore di sfondo sulla serie finta
			mSeriesRenderer[9].setColor(Color.TRANSPARENT);
			mSeriesRenderer[9].setPointStyle(PointStyle.POINT);
			mSeriesRenderer[9].setLineWidth(lineWidth+2);
			mSeriesRenderer[9].setDisplayChartValues(false);
			mSeriesRenderer[9].setChartValuesTextSize(12);
			mSeriesRenderer[9].setStroke(new BasicStroke(Cap.ROUND, Join.ROUND, 0, null, 0));
			mSeriesRenderer[9].setFillBelowLine(true);
			//mSeriesRenderer[9].setFillBelowLineColor(Color.parseColor("#ffffcb"));
			mSeriesRenderer[9].setFillBelowLineColor(Color.TRANSPARENT);
			
			mRenderer.addSeriesRenderer(mSeriesRenderer[9]);  			
			
			//add all series to dataset
			for (int i = 0; i < 10; i++)
				mDataset.addSeries(mSeries[i]);
			
			//check if serie length is the same both renderer and dataset
			if((mDataset == null)|| (mRenderer == null) || (mDataset.getSeriesCount() != mRenderer.getSeriesRendererCount()))
				throw new IllegalArgumentException("Dataset and renderer should not be null and " +
						"should have the same number of series");
			
			//encapsulate the graph into viewl
			if (mChartView == null) 
			{		    
				Log.d("GraphManager", "encapsulate the graph into view");
				
				mChartView = ChartFactory.getLineChartView(getApplicationContext(), mDataset, mRenderer);
				
				if(mChartView == null)
				{ 			
					Log.d("GraphManager","mChartView == null");
				}
				else
				{
					mChartView.setOnClickListener(new View.OnClickListener() 
					{
				        public void onClick(View v) 
				        {
				        	// handle the click event on the chart
				        	SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
				        	if (seriesSelection == null) 
				        	{
				        		Toast.makeText(Graph.this, "No chart element", Toast.LENGTH_SHORT).show();
				        	} 
				        	else 
				        	{
				        		// display information of the clicked point
				        		/*
				        		Toast.makeText(Graph.this,
					                "Chart element in series index " + seriesSelection.getSeriesIndex()
					                    + " data point index " + seriesSelection.getPointIndex() + " was clicked"
					                    + " closest point value X=" + seriesSelection.getXValue() + ", Y="
					                    + seriesSelection.getValue(), Toast.LENGTH_LONG).show();
				        		*/
				        		int serieIndex = seriesSelection.getSeriesIndex();
				        		int pointIndex = seriesSelection.getPointIndex();
				        		double serieValue = mSeries[serieIndex].getY(pointIndex);
				        		
				        		if(serieIndex == 8) //8 for black carbon serie
				        			Toast.makeText(Graph.this, mSeries[serieIndex].getTitle()+" clicked value: "+serieValue, Toast.LENGTH_LONG).show();
				        		else
				        			Toast.makeText(Graph.this, mSeries[serieIndex].getTitle()+" clicked value: "+serieValue, Toast.LENGTH_LONG).show();
				        	}
				         }
				    });
					
					//add graph view to layout
					mChartLayout.addView(mChartView, new LinearLayout.LayoutParams
					    		(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
				}    
			} 
			else 
			{
				Log.d("GraphManager", "graph redraw");
				mChartView.repaint();
			}
	    }
	    
	    //given an array of records, draw each records on the graph, at the right position
	    public void addAllData(List<Record>records)
	    {
	    	mRenderer.setRange(new double[]{1, xElems, 0, yMax});
	    	
			//draw all loaded records on graph
			for(int i = 0; i < records.size(); i++)
			{
				Record rec = mRecords.get(i);		
				
				long recTs = rec.mSysTimestamp;
				
				//calculate system timestamp delta between actual record and precedent one
				long tsDelta = recTs - mPrecRecordSysTs;
					
				//if timestamp delta is around 2 sec (under 2250 milliseconds)
				//actual record is draw in position following the precedent record
				if(tsDelta < 2750)
				{		
					addDataToGraph(rec.mValues, rec.mBcMobile);
				}
				else
				{
					//calculate how long, in terms of number of couple of seconds, is the delta
					int twoSeconds = Math.round((float)tsDelta /2000);

					//Log.d("GraphManager", "addAllData()--> empty position - sysTs: " +rec.mSysTimestamp+ " prec sysTs: " +mPrecRecordSysTs);
					//Log.d("GraphManager", "addAllData()--> empty position - tsDelta: " +tsDelta+ " # of couple of seconds: " +twoSeconds);
						
					//draw # seconds empty point on graph
					for(int j = 0; j < twoSeconds; j++)
					{
						//first 2 values of double[] array are not used by refreshData()
						addDataToGraph(new double[]{-1, -1, 0, 0, 0, 0, 0, 0, 0, 0}, 0);
						
						//Log.d("GraphManager", "addAllData()--> ADDED EMPTY POSITION");
					}	
				}		
				//}
				//mPrecRecordSysTs, ad esclusione della fase iniziale, in cui contiene il timestamp di mezz'ora fa,
				//contiene sempre il system timestamp dell'ultimo record disegnato sul grafico
				mPrecRecordSysTs = rec.mSysTimestamp;
			}	
	    }
	    
	    public void repaint()
	    {
	    	if(mChartView != null)
	    		//ridisegna la view con i dati aggiunti
				mChartView.repaint();
	    }
	    
	    public double calcAvgCo()
	    {
	    	double avgCo = (mValues[0] + mValues[1] + mValues[2] + mValues[3]) /4;
	    	int avgCo_int = (int)Math.round(avgCo * 100);
	    	return (double)avgCo_int / 100;
	    }
	    
	    public double calcAvgNo2()
	    {
	    	double avgNo2 = (mValues[4] + mValues[5]) /2;
	    	int avgNo2_int = (int)Math.round(avgNo2 * 100);
	    	return (double)avgNo2_int / 100;
	    }
	    
	    public double calcVoc()
	    {
	    	int voc_int = (int)Math.round(mValues[6] * 100);
	    	return (double)voc_int / 100;
	    }
	    
	    public double calcO3()
	    {
	    	int o3_int = (int)Math.round(mValues[7] * 100);
	    	return (double)o3_int / 100;
	    }
	    
	    public double calcBc()
	    {
	    	if(Utils.bc == 0)
	    		return 0;
	    	
	    	int bc_int = (int)Math.round(Utils.bc * 100);
	    	return (double)bc_int / 100;
	    }
	    
	    //aggiunge un nuovo vettore di dati senza gestire lo scrolling e senza fare repaint
	    public void addDataToGraph(double[] pollutants, double blackCarbon)
	    {
	    	double x;
	    	
	    	//se non ho coppie nella serie, sono al tempo 0
	    	if(mSeries[0].getItemCount() == 0)
	    		x = 0;
	    	else
	    		//ottengo i valori massimo x dalla xyseries, che corrisponde al tempo
	    		x = mSeries[0].getMaxX();
	    	
	    	//Log.d("GraphManager", "refreshData()--> x= " +x);
	    
			//copio nell'array i valori
			mValues[0] = pollutants[2];
			mValues[1] = pollutants[3];
			mValues[2] = pollutants[4];
			mValues[3] = pollutants[5];
			mValues[4] = pollutants[6];
			mValues[5] = pollutants[7];
			mValues[6] = pollutants[8];
			mValues[7] = pollutants[9];				
					
			try
			{
				mAvgCoTv.setText(String.valueOf(calcAvgCo()));
				mAvgNo2Tv.setText(String.valueOf(calcAvgNo2()));				
				mVocTv.setText(String.valueOf(calcVoc()));
				mO3Tv.setText(String.valueOf(calcO3()));
				
				int bc_int = 0;				
				if(blackCarbon > 0)
					bc_int = (int)Math.round(blackCarbon * 100);

				mBcTv.setText(String.valueOf((double)bc_int / 100));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			//calcolo il valore più alto fra i 9 valori (sull'intera serie)
			double max = calcMax();
			
			mRenderer.setYAxisMax(Math.ceil(max));
			
			Log.d("GraphManager", "addDataToGraph()--> getYAxisMax: "+mRenderer.getYAxisMax()+ " black carbon: " +blackCarbon);
			
			//getYAxisMax include anche il calcolo della serie del black carbon, mentre max solo
			//è riferito solo agli 8 valori attuali
			
			//se il massimo delle serie è inferiore al black carbon del record attuale, modifica la scala verticale
			//adattandola al ceiling del black carbon
			/*
			if(mRenderer.getYAxisMax()  < blackCarbon + 2)
			{
				int bcMax = (int)Math.ceil(blackCarbon);
				mRenderer.setYAxisMax(bcMax); 
				mRenderer.setYLabels(bcMax);
				
				//Log.d("GraphManager", "addDataToGraph()--> setYAxisMax: " +mRenderer.getYAxisMax()+" bc max: " +bcMax);
			}*/
			
			
			/*
			double max = 4;
					
			//aggiorno la scala del grafico di conseguenza
			if(mRenderer.getYAxisMax() < max)
				mRenderer.setYAxisMax(max+1);
			else if(mRenderer.getYAxisMax() > max+2)
				mRenderer.setYAxisMax(max + 1);
				*/	
			
			//
			
			
			
			/*
			if(max < blackCarbon)
			{
				if(max < blackCarbon)
				{
					mRenderer.setYAxisMax(bcMax);
					mRenderer.setYLabels(bcMax*2);
				}
				else if(max > bcMax)
				{
					mRenderer.setYAxisMax(max);
					mRenderer.setYLabels(max*2);
				}
			}*/
			
			
			//aggiungo alla mSeries i-esima il valore x+1 sulle ascisse e il valore i-esimo di mValues alle ordinate
			addData(mSeries[0], x+1, mValues[0]);   
			addData(mSeries[1], x+1, mValues[1]);
			addData(mSeries[2], x+1, mValues[2]);
			addData(mSeries[3], x+1, mValues[3]);
					
			addData(mSeries[4], x+1, mValues[4]);
			addData(mSeries[5], x+1, mValues[5]);
					
			addData(mSeries[6], x+1, mValues[6]);
			addData(mSeries[7], x+1, mValues[7]);
								
			//addData(mSeries[8], x+1, calcActualMax(mValues)+0.2); //la serie finta colorata di nero sta 0.2 più su del max
		    if(mValues[0] == 0)
		    	addData(mSeries[9], x+1, mRenderer.getYAxisMax()+0.2);
		    else
		    	addData(mSeries[9], x+1, 0);
		
		    addData(mSeries[8], x+1, blackCarbon); //black carbon serie
		    
	    	//carico in x il nuovo massimo x dopo aver aggiunto le n coppie (x, y)
	    	x = mSeries[0].getMaxX();    			    		    	
	    }
	    
	    //aggiunge un nuovo vettore di dati, gestisce lo scrolling e fa repaint del grafico
	    public void refreshData(double[] pollutants)
	    {	    	
	    	double x;
	    	
	    	//se non ho coppie nella serie, sono al tempo 0
	    	if(mSeries[0].getItemCount() == 0)
	    		x = 0;
	    	else
	    		//ottengo i valori massimo x dalla xyseries, che corrisponde al tempo
	    		x = mSeries[0].getMaxX();
	    	
	    	//Log.d("GraphManager", "refreshData()--> x= " +x);
	    
			//copio nell'array i valori
			mValues[0] = pollutants[2];
			mValues[1] = pollutants[3];
			mValues[2] = pollutants[4];
			mValues[3] = pollutants[5];
			mValues[4] = pollutants[6];
			mValues[5] = pollutants[7];
			mValues[6] = pollutants[8];
			mValues[7] = pollutants[9];				
						
			try
			{
				mAvgCoTv.setText(String.valueOf(calcAvgCo()));
				mAvgNo2Tv.setText(String.valueOf(calcAvgNo2()));
				mVocTv.setText(String.valueOf(calcVoc()));
				mO3Tv.setText(String.valueOf(calcO3()));								
				mBcTv.setText(String.valueOf(calcBc()));
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			//SCROLLING
			if((x > 0) && (((x % (xElems)) == 0) || (x > (xElems))))
			{
				updateTsTextViews();
				
				if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
					mProgressDialog.dismiss();
				
				int itemCount = mSeries[0].getItemCount();
				
				Log.d("GraphManager", "refreshData()--> item count: " +itemCount);
				
				int limit = 0;
				//half hour, no more then 450 items 
				if(lengthsIndex == 0)
					limit = 899;
				else if(lengthsIndex == 1)
					limit = 29;
				else if(lengthsIndex == 2)
					limit = 149;
				else if(lengthsIndex == 3)
					limit = 449;
				
				//capita che il grafico contenga qualche valore più del dovuto. Con questo ciclo mi assicuro che ciò
				//non avvenga
				for(int i = 0; i < itemCount - limit; i++)
					removeOldestData();

				//calcolo il valore più alto fra gli 8 valori				
				double max = calcMax();
				mRenderer.setYAxisMax(Math.ceil(max));
				
				//getYAxisMax include anche il calcolo della serie del black carbon, mentre max solo
				//è riferito solo agli 8 valori attuali
				/*
				if(mRenderer.getYAxisMax() < Utils.bc + 2)
				{
					int bcMax = (int)Math.ceil(Utils.bc);
					
					Log.d("GraphManager", "refreshData()--> Y axis max: " +mRenderer.getYAxisMax()+ " bc ceil: " +bcMax);
					mRenderer.setYAxisMax(bcMax); 
					mRenderer.setYLabels(bcMax);
				}
				else if((Utils.bc > 0)&&(max - 2 > Utils.bc))
				{
					mRenderer.setYAxisMax(max); 
					mRenderer.setYLabels((int)Math.ceil(max));
				}*/
				
				//addData prende in input: serie a cui aggiungere la coppia, 
				//tempo (orizzontale) e valore inquinante (verticale)
		    	addData(mSeries[0], x+1, mValues[0]);  
		    	addData(mSeries[1], x+1, mValues[1]); 
		    	addData(mSeries[2], x+1, mValues[2]); 
		    	addData(mSeries[3], x+1, mValues[3]); 
		    			
		    	addData(mSeries[4], x+1, mValues[4]);
		    	addData(mSeries[5], x+1, mValues[5]);
		    			
		    	addData(mSeries[6], x+1, mValues[6]);
		    	addData(mSeries[7], x+1, mValues[7]);    	
		    	
		    	//addData(mSeries[8], x+1,calcActualMax(mValues)+0.2); //la serie finta colorata di nero sta 0.2 più su del max
		    	
		    	if(mValues[0] == 0)
		    		addData(mSeries[9], x+1, mRenderer.getYAxisMax()+0.2);
		    	else
		    		addData(mSeries[9], x+1, 0);
		    	
		    	addData(mSeries[8], x+1, Utils.bc);
		    	
		    	Log.d("GraphManager", "refreshData()--> added row with black carbon value: " +Utils.bc);
		    	
			    //imposto il valore minimo mostrato sull'asse X su quello più vecchio, che è anche 
			    //il più basso, essendo i valori sull'asse X strettamente crescenti (secondi)
			    mRenderer.setXAxisMin(mSeries[0].getMinX());
			    //imposto il valore massimo mostrato sull'asse X sul più recente, che è anche il più alto
			    mRenderer.setXAxisMax(mSeries[0].getMaxX());
			}    
			else
			{
				updateTsTextViews();
				
				if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
					mProgressDialog.dismiss();
				
				//calcolo il valore più alto fra gli 8 valori				
				double max = calcMax();
				
				//getYAxisMax include anche il calcolo della serie del black carbon, mentre max solo
				//è riferito solo agli 8 valori attuali
				if(mRenderer.getYAxisMax() < Utils.bc + 2)
				{
					int bcMax = (int)Math.ceil(Utils.bc);
					
					Log.d("GraphManager", "refreshData()--> Y axis max: " +mRenderer.getYAxisMax()+ " bc ceil: " +bcMax);
					mRenderer.setYAxisMax(bcMax); 
					mRenderer.setYLabels(bcMax);
				}
				else if((Utils.bc > 0)&&(max - 2 > Utils.bc))
				{
					mRenderer.setYAxisMax(max); 
					mRenderer.setYLabels((int)Math.ceil(max));
				}
					
				//aggiungo alla mSeries i-esima il valore x+1 sulle ascisse e il valore i-esimo di mValues alle ordinate
				addData(mSeries[0], x+1, mValues[0]);   
				addData(mSeries[1], x+1, mValues[1]);
				addData(mSeries[2], x+1, mValues[2]);
				addData(mSeries[3], x+1, mValues[3]);
					
				addData(mSeries[4], x+1, mValues[4]);
				addData(mSeries[5], x+1, mValues[5]);
					
				addData(mSeries[6], x+1, mValues[6]);
				addData(mSeries[7], x+1, mValues[7]);
								
				//addData(mSeries[8], x+1, calcActualMax(mValues)+0.2); //la serie finta colorata di nero sta 0.2 più su del max
		    	if(mValues[0] == 0)
		    		addData(mSeries[9], x+1, mRenderer.getYAxisMax()+0.2);
		    	else
		    		addData(mSeries[9], x+1, 0);
		    	
		    	addData(mSeries[8], x+1, Utils.bc);	
		    	
		    	Log.d("GraphManager", "refreshData()--> added row with black carbon value: " +Utils.bc);
			}
				
	    	//ridisegna la view con i dati aggiunti
			mChartView.repaint();
	    	//carico in x il nuovo massimo x dopo aver aggiunto le n coppie (x, y)
	    	x = mSeries[0].getMaxX();    		    	
		}		
	    
	    public void removeOldestData()
	    {
			//rimuovo la prima coppia in tutte le serie, ossia la più vecchia
			mSeries[0].remove(0);
			mSeries[1].remove(0);
			mSeries[2].remove(0);
			mSeries[3].remove(0);
				
			mSeries[4].remove(0);
			mSeries[5].remove(0);
				
			mSeries[6].remove(0);
			mSeries[7].remove(0);
			
			mSeries[8].remove(0); //black carbon serie
			
			mSeries[9].remove(0); //fake serie 
	    }
	    
	    public void removeAllData()
	    {
			mSeries[0].clear();
			mSeries[1].clear();
			mSeries[2].clear();
			mSeries[3].clear();
					
			mSeries[4].clear();
			mSeries[5].clear();
					
			mSeries[6].clear();
			mSeries[7].clear();
			
			mSeries[8].clear(); //black carbon serie 
			
			mSeries[9].clear(); //fake serie
	    }
	    
	    //aggiunge alla serie (mSeries) xyseries di coppie (x, y) una nuova coppia
	    public void addData(XYSeries xyseries, double d1, double d2)
	    {
	    	xyseries.add(d1, d2);
	    }
	    
	    //calcola il max fra le 9 serie, considerando ogni serie nella sua interezza
	    public double calcMax()
	    {
	    	double max = 0;
	    	
	    	for(int i = 0; i < 9; i++)
	    	{
	    		mMaxValues[i] = mSeries[i].getMaxY(); 
	    		if(mMaxValues[i] >= max)
	    			max = mMaxValues[i];
	    	}
	    	return max;
	    }
	    
	    //calcola il max dell'arrya di valori passati in ingresso
	    public double calcActualMax(double[] values)
	    {
	    	double max = 0;
	    	
	    	for(int i = 0; i < values.length; i++)
	    	{
	    		if(i == 0)
	    			max = values[i];
	    		else
	    		{
	    			if(values[i] >= max)
	    				max = values[i];
	    		}
	    	}
	    	return max;
	    }
	    
	    public void setXAxisMax(int xNum)
	    {
	    	xElems = xNum;
	    	mRenderer.setXAxisMax(xNum-1);
	    }
	    
	    public void setDottedStroke()
	    {
	    	//BasicStroke.DASHED: trattini
	    	//BasicStroke.DOTTED: puntini
	    	
	    	for(int i = 0; i < 8; i++)
	    		mSeriesRenderer[i].setStroke(BasicStroke.DOTTED);
	    }
	    
	    public void setLineStroke()
	    {
	    	for(int i = 0; i < 8; i++)
	    		mSeriesRenderer[i].setStroke(new BasicStroke(Cap.ROUND, Join.ROUND, 0, null, 0));
	    }
	}
	
	
    /*************************** GRAPH HANDLER ******************************************************/
    
    private Handler mGraphHandler = new Handler()
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
					
					Log.d("GraphHandler", "conn attempts: " +mConnAttempts);
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
				
				case Constants.DISCOVERY_STARTED:
					Log.d("GraphHandler", "discovery started");
					
				break;
				
				case Constants.DISCOVERY_FINISHED:
					
				break;
				
				case Constants.DEVICE_DISCOVERED:										

				break;	
				
				case Constants.CONNECTION_FAILED:
					Log.d("GraphHandler", "Failed to connect to selected device");
					
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

						showProgressDialog("Connection attempt #" +mConnAttempts+ " to " +deviceAddress, false);
						//invoco il metodo connect() del BluetoothManager che inizia la connessione
						mBluetoothManager.connect(remoteDevice);
					}
					else
					{
						//avviso l'utente dei 3 tentativi di connessione falliti
						
						//chiudo la progress dialog di apertura connessione verso dispositivo remoto
						if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
							mProgressDialog.dismiss();
						
						Log.d("MapHandler", "Failed 3 reconnection attempts");
						
						//play sound alert when 3 connection attempts fail
						playConnAttempFailed();
						connAttemptFailedDialog();
					}
										
				break;	
				
				case Constants.CONNECTION_LOST:
					Log.d("GraphHandler", "Connection lost");
					mBtStatus.setBackgroundResource(R.drawable.bt_off);
					
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
						showProgressDialog("Connection attempt " +mConnAttempts+ " to " +deviceAddress, false);
						
						//invoco il metodo connect() del BluetoothManager che inizia la connessione
						mBluetoothManager.connect(remoteDevice);
					}
					else
					{
	        			//registro il broadcast receiver con intent ACTION_STATE_CHANGED, utile per
						//rilevare quando il dispositivo bluetooth dello smartphone viene acceso
	        	        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	        	        
	        	        mBluetoothAdapter.enable(); //attivazione dispositivo bluetooth smartphone  
	        			
	        	        showProgressDialog("Activating Bluetooth", true); //l'attivazione del dispositivo bluetooth ha progress dialog
					}
				break;
				
				case Constants.STATE_NONE:
					Log.d("GraphHandler", "STATE NONE ");
					mBtStatus.setBackgroundResource(R.drawable.bt_off);
					
				break;
				case Constants.STATE_CONNECTING:
					Log.d("GraphHandler", "STATE_CONNECTING");
				break;
				case Constants.STATE_CONNECTED:
					Log.d("GraphHandler", "STATE_CONNECTED");
					mBtStatus.setBackgroundResource(R.drawable.bt_on);
					mConnAttempts = 1;
					//chiudo la progress dialog di apertura connessione verso dispositivo remoto
					if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
						mProgressDialog.dismiss();
				break;	
				/*
				case Constants.DEVICE_GPS_ON:
					mGpsStatus.setBackgroundResource(R.drawable.gps_on_sbox);
				break;
				
				case Constants.DEVICE_GPS_OFF:
					mGpsStatus.setBackgroundResource(R.drawable.gps_off);
				break;*/
				
				case Constants.SENSOR_BOX_MAC_NOT_READ:
					Log.d("GraphHandler", "SENSOR BOX MAC ADDRESS NOT READ!!!");
					
					 sensorBoxMacNotReadDialog();
				break;
    		}
    	}
    };
    
    /******************************* DIALOGS ****************************************************/
    
    public void showProgressDialog(String msg, boolean cancelable)
    {
    	mProgressDialog = ProgressDialog.show(Graph.this, getResources().getString(R.string.app_name), msg, true, true);
    }
    
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
					showProgressDialog("Connection attempt #" +mConnAttempts+ " to " +deviceAddress, false);
					
					//invoco il metodo connect() del BluetoothManager che inizia la connessione
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
    
    public void sensorBoxMacNotReadDialog() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Graph.this);
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
    
    /******************** CLOSES APP *********************************************************/
    
    public void closeApp()
    {   	
    	Utils.backToHome = true;
    	Utils.setGpsTrackingOn(false, getApplicationContext());
    	
    	//stop gps tracking service
    	if(Utils.gpsTrackServIntent != null)
    		stopService(Utils.gpsTrackServIntent);
    	
    	//fermo il servizio di store'n'forward
    	if(Utils.storeForwServIntent != null)
    		stopService(Utils.storeForwServIntent);
    	
		mIsRunning = false;		
		mConnAttempts = 1;
		//rimuovo eventuali runnable schedulati nel futuro
		mHandler.removeCallbacks(mGetLastRecIdRunnable);
		
    	//fermo tutti i thread
    	if(mBluetoothManager != null)
    		mBluetoothManager.stop();
    	mBluetoothManager = null;
    	
    	Map.stopCallerThread();
    	   	
    	//ripulisco le shared prefs
    	Utils.deleteSharedPrefs(getApplicationContext());
    	  	
    	//useful to complete kill the app
    	Graph.this.finish();
    	android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        getParent().finish();
    }
    
    public static void stopCallerThread()
    {
    	mIsRunning = false;
    }
    
    /******************* RECEIVE MESSAGES FROM GPS TRACKING SERVICE ***********************************/
    /*
    private BroadcastReceiver mGpsServiceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	String action = intent.getAction();

            if (action.equals(Constants.PHONE_GPS_ON)) 
            {
            	Log.d("GpsServiceReceiver", "Internet is ON");

            }
            if (action.equals(Constants.PHONE_GPS_OFF)) 
            {
            	Log.d("GpsServiceReceiver", "Internet is OFF");

            }    
        }
    };*/
    
    /********************* RECEIVES MESSAGES FROM STORE'N'FORWARD SERVICE *****************************/
    
    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	String action = intent.getAction();

            if (action.equals(Constants.INTERNET_ON)) 
            {
            	Log.d("ServiceReceiver", "Internet is ON");
            	mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
            }
            if (action.equals(Constants.INTERNET_OFF)) 
            {
            	Log.d("ServiceReceiver", "Internet is OFF");
            	mInterUplStatus.setBackgroundResource(R.drawable.internet_off);
            }    
            if (action.equals(Constants.UPLOAD_ON)) 
            {
            	Log.d("ServiceReceiver", "Upload is ON");
            	mInterUplStatus.setBackgroundResource(R.drawable.upload);
            }   
            if (action.equals(Constants.UPLOAD_OFF)) 
            {
            	Log.d("ServiceReceiver", "Upload is OFF");
            	mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
            }         
        }
    };    
    
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
    
    /*************** CHANGE LENGTH OF SLIDING WINDOW BTN ON CLICK LISTENER ********************************/
    private OnClickListener mChangeBtnOnClickListener = new OnClickListener()
    {
		@Override
		public void onClick(View v) 
		{
			lengthsIndex++;
			if(lengthsIndex > 3)
				lengthsIndex = 0;
			mChangeBtn.setText(sliding_window_lengths[lengthsIndex]);
			
			//open progress dialog data loading
			showProgressDialog("Loading data...", false);
			
			mHandler.postDelayed(new Runnable()
			{
				@Override
				public void run() 
				{
					mGraphManager.removeAllData();
					
					if(lengthsIndex == 0)
					{
						mGraphManager.setXAxisMax(900);
						mMillisAgo = HALF_HOUR;
					}
					else if(lengthsIndex == 1)
					{
						mGraphManager.setXAxisMax(30);
						mMillisAgo = ONE_MINUTE;
					}
					else if(lengthsIndex == 2)
					{
						mGraphManager.setXAxisMax(150);	
						mMillisAgo = FIVE_MINUTES;
					}
					else if(lengthsIndex == 3)
					{
						mGraphManager.setXAxisMax(450);
						mMillisAgo = FIFTEEN_MINUTES;
					}
					
					mHandler.post(mLoadGraphData);

					mIsRunning = true;
				}			
			}, 500);
		} 	
    };
}
