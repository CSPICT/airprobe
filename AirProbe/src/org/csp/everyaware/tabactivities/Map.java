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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;

import org.csp.everyaware.ColorHelper;
import org.csp.everyaware.Constants;
import org.csp.everyaware.ExtendedLatLng;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.bluetooth.BluetoothBroadcastReceiver;
import org.csp.everyaware.bluetooth.BluetoothManager;
import org.csp.everyaware.db.AnnotatedRecord;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.MarkerRecord;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.SemanticSessionDetails;
import org.csp.everyaware.internet.FacebookManager;
import org.csp.everyaware.internet.StoreAndForwardService;
import org.csp.everyaware.internet.TwitterManager;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Map extends Activity
{
	private MapView mMapView;
	private GoogleMap mGoogleMap;
	
	private Handler mHandler;
	private static boolean mIsRunning;
	private DbManager mDbManager;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter = null;
	
	private BluetoothBroadcastReceiver mReceiver;
	
	private Record mPrecRecord = null;
	private Record mNewRecord = null;
	private List<Record>mRecords = new ArrayList<Record>();

	//track length button
	private Button mTrackLengthBtn;
	
	//camera tracking button and boolean flag
	private Button mFollowCamBtn;
	private boolean mCameraTrackOn = true;
	
	//zoom control custom buttons
	private Button mZoomInBtn, mZoomOutBtn;
	private float mZoom = 15;
	
	//insert ann button
	private Button mInsertAnnBtn;

	//share button (log on facebook/twitter)
	private Button mShareBtn;
	
	//zoom controls (they can show/hide on map)
	private LinearLayout mZoomControls;
	private long mOnMapClickTs;
	
	//new geo referenced record image view indicator
	private ImageView mNewPinIv;
	//status icons
	private ImageView mGpsStatus;
	private ImageView mBtStatus;
	private ImageView mInterUplStatus;
	
	private TextView mAqiTv;
	private LinearLayout mSpectrum;
	
	//array of points of path showed on map
	private List<ExtendedLatLng>mLatLngPoints;
	private double mPrecRecordId = -1;
	
	private List<AnnotatedRecord>mAnnotatedRecords = new ArrayList<AnnotatedRecord>();
	
	private Drawable mMarkerTrackIcon;
	private Drawable mTextAnnIcon;
	
	private boolean mTrackMode;
	
	private ProgressDialog mCancelDialog;
	private ProgressDialog mProgressDialog;
	private int mConnAttempts = 1;
	
	private boolean mInitializedView = false;
	private boolean mConnectivityOn;
	
	private CallerThread mCallerThread;
	private GetDataThread mGetDataThread;
	
	//options variables
	/*
	private CharSequence[] options1; //ages for uploaded records
	private CharSequence[] options2; //store'n'forward frequencies
	private CharSequence[] options3; //history download on/off
	private CharSequence[] options4; //upload records only on wifi network or both wifi/mobile network
	private AlertDialog alert = null;
*/
    private MediaPlayer mMediaPlayer;
	
    /*
	private double avg_poll_min = 0;
	private int poll_mult = 35;
	private int poll_base = 25;
	*/
    
	//keep reference min/max black carbon values of track
    /*
	private double mMinBcValue = 0;
	private double mMaxBcValue = 21;
	private boolean mMinMaxInitialized = false;
	*/
    
	//share section
	private boolean mValidFbSession;
	private boolean mValidTwSession;	
	private FacebookManager mFacebookManager;
	private TwitterManager mTwitterManager;
	private Button[] mButtons  = new Button[2]; //login buttons (facebook and twitter)
	
	private Toast mExitToast; //toast showed when user press back button
	
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
	
	private final int BC_MULTIPLIER = 10;
	
	private Button mStartStopBtn;
	private TextView mBcCumulativeTv;
	private TextView mTimeLeftTv;
	private long mStartPlayTs;
	private final Calendar cal = Calendar.getInstance();
	private String mTimeDiff = "";
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.d("Map", "******************************onCreate()******************************");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.map_container);		
		
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
		
		Utils.setStep(Constants.TRACK_MAP, getApplicationContext());
		
		mDbManager = DbManager.getInstance(getApplicationContext());
		mDbManager.openDb();
		
		mBluetoothManager = BluetoothManager.getInstance(Map.this, null);
		mBluetoothManager.setMapHandler(mMapHandler);		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		//broadcast receiver to receive events about bluetooth connection
		mReceiver = new BluetoothBroadcastReceiver(getApplicationContext(), mMapHandler);
	
		//it will contain  displayed latlng points
		mLatLngPoints = new ArrayList<ExtendedLatLng>();

        //google map initialization
        setUpMapIfNeeded(savedInstanceState);
 
		mTrackMode = true;
		
		//obtaining references to buttons and defining listeners
		getButtonRefs();
		
		mHandler = new Handler();
		mIsRunning = true;
		
		//starting runnable that loads last inserted record and draws segments and points on map
		mCallerThread = new CallerThread();
		mCallerThread.start();
		
		//starting store'n'forward service and saving reference to it
		Intent serviceIntent = new Intent(getApplicationContext(), StoreAndForwardService.class);
		Utils.storeForwServIntent = serviceIntent;
		startService(serviceIntent);
		
		//set appropriate colors in all seven black carbon levels under black carbon text box
		setBcLevelColors();
		
		//initialize the start/stop button according to the state (open or closed) of the 
		//semantic window. Semantic window can be left open when the app is closed, so, when
		//AP is relaunched, in this case the button is 'stop'
		initStartStopSemanticWindow();
		
	}
	
	@Override
	protected void onStart() 
	{
	    super.onStart();

	    // This verification should be done during onStart() because the system calls
	    // this method when the user returns to the activity, which ensures the desired
	    // location provider is enabled each time the activity resumes from the stopped state.
	    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

	    if (!gpsEnabled) 
	    {
	        // Build an alert dialog here that requests that the user enable
	        // the location services, then when the user clicks the "OK" button,
	        // call enableLocationSettings()
	    }
	}

	private void enableLocationSettings() 
	{
	    Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	    startActivity(settingsIntent);
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("Map", "onStop()");
		mHandler.removeCallbacks(mGetLastRecIdRunnable);
	}	    
    
    @Override
    public void onDestroy()
    { 	
    	Utils.paused = false;
    	mMapView.onDestroy();
    	super.onDestroy(); 
    	
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release(); 
    	
    	Log.d("Map", "onDestroy()");
    }      
    
    @Override
    public void onPause()
    {
    	Utils.paused = true;
    	mMapView.onPause();

    	Log.d("Map", "onPause()");
    	
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
    	
    	super.onPause();
    }
    
	@Override
	public void onResume()
	{
    	mMapView.onResume();
    	Utils.paused = false;
		super.onResume();
		Log.d("Map", "******************************onResume()****************************");
		
		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
		Utils.setStep(Constants.TRACK_MAP, getApplicationContext());
		mTrackMode = true;

		if(!Utils.semanticWindowStatus)
		{
			mStartStopBtn.setBackgroundResource(R.drawable.play); //show play button
		}
		
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
		
    	if(mCameraTrackOn)
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_pressed);
    	else
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_not_pressed);
	
		if(Utils.getStep(getApplicationContext()) == Constants.TRACK_MAP)
		{
			if(mGoogleMap != null)
				mGoogleMap.clear();		
			mTrackMode = true;		
			
			long trackLength = Utils.getTrackLength(getApplicationContext());
			
			if(trackLength == Constants.FIVE_MINS)
			{
				mTrackLengthBtn.setBackgroundResource(R.drawable.five_mins_button);
				//show right number of point of map (if Graph activity has changed track length)
				mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - trackLength, 1);
	       		//if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))
	       		//	calcMinMaxBCvalue();
			}
			else if(trackLength == Constants.FIFTEEN_MINS)
			{
				mTrackLengthBtn.setBackgroundResource(R.drawable.fifteen_mins_button);
				//show right number of point of map (if Graph activity has changed track length)
				mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - trackLength, 1);
	       		//if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))
	       		//	calcMinMaxBCvalue();
			}
			else if(trackLength == Constants.SIXTY_MINS)
			{
				mTrackLengthBtn.setBackgroundResource(R.drawable.sixty_mins_button);
				//show right number of point of map (if Graph activity has changed track length)
				mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - trackLength, 4);
	       		//if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))
	       		//	calcMinMaxBCvalue();
			}

			drawPath();
			//drawTextAnnotations();
		}	

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		mPrecRecordId = Utils.getNewRecordId(getApplicationContext());
		
		//register receiver for messages from store'n'forward service
		IntentFilter internetOnFilter = new IntentFilter(Constants.INTERNET_ON);
		registerReceiver(mServiceReceiver, internetOnFilter);
		IntentFilter internetOffFilter = new IntentFilter(Constants.INTERNET_OFF);
		registerReceiver(mServiceReceiver, internetOffFilter);
		IntentFilter uploadOnFilter = new IntentFilter(Constants.UPLOAD_ON);
		registerReceiver(mServiceReceiver, uploadOnFilter);
		IntentFilter uploadOffFilter = new IntentFilter(Constants.UPLOAD_OFF);
		registerReceiver(mServiceReceiver, uploadOffFilter);
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
	
	private Runnable hidePin = new Runnable()
	{
		@Override
		public void run() 
		{
			mNewPinIv.setVisibility(View.INVISIBLE);
		}		
	};
	
    /***************************** INIT GOOGLE MAP **********************************************/
	
    private void setUpMap() 
    {
    	mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    	mGoogleMap.getUiSettings().setCompassEnabled(false);
    	mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
    	  
    	//assegno la mia implementazione della classe LocationSource
    	//mGoogleMap.setLocationSource(mLocationSource); 
    	//mGoogleMap.setMyLocationEnabled(true);
    	
    	//mGoogleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
    	
    	//animate camera to an initial zoom level 
    	try
    	{
    		mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(mZoom), 1500, null);
    	}
    	catch(NullPointerException e)
    	{
    		e.printStackTrace();
    	}
    	
    	mGoogleMap.setOnMapClickListener(new OnMapClickListener()
    	{
			@Override
			public void onMapClick(LatLng arg0) 
			{
				if(mZoomControls != null)
				{
					mOnMapClickTs = new Date().getTime();
					
					mZoomControls.setVisibility(View.VISIBLE);
					
					mHandler.postDelayed(new Runnable()
					{
						@Override
						public void run() 
						{
							if((new Date().getTime() - mOnMapClickTs) >= 2500)
								mZoomControls.setVisibility(View.GONE);
						}
						
					}, 3000);
				}
			} 		
    	});
    }

    private void setUpMapIfNeeded(Bundle savedInstanceState) 
    {
        // Do a null check to confirm that we have not already instantiated the map.
        if(mMapView == null)	
        {
        	mMapView = (MapView) findViewById(R.id.mapView);
        	//MapView object has the same life cycle of activity
        	mMapView.onCreate(savedInstanceState);
        	
        	//get reference to GoogleMap object from MapView
        	mGoogleMap = mMapView.getMap();

            if(isGoogleMapsInstalled())
            {
                if (mGoogleMap != null) 
                {
            		//CameraUpdateFactory e BitmapDescriptorFactory need initialization now
            		try 
            		{
            		     MapsInitializer.initialize(this);
            		} 
            		catch (GooglePlayServicesNotAvailableException e) 
            		{
            		     e.printStackTrace();
            		}

                    setUpMap();
                }
            }
            else
            {
                Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.install_google_maps_string));
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.alert_dialog_ok, getGoogleMapsListener());
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }	
	
    public boolean isGoogleMapsInstalled()
    {
        try
        {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );
            return true;
        } 
        catch(PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }
    
    public DialogInterface.OnClickListener getGoogleMapsListener()
    {
        return new DialogInterface.OnClickListener() 
        {
            @Override
            public void onClick(DialogInterface dialog, int which) 
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                startActivity(intent);

                //Finish the activity so they can't circumvent the check
                finish();
            }
        };
    }
    
	public final void createLegalNoticesDialog(Activity activity)
	{
		AlertDialog ad = new AlertDialog.Builder(activity).create();  
		ad.setCancelable(false); // This blocks the 'BACK' button  
		ad.setMessage(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(activity.getApplicationContext()));  
		ad.setButton("OK", new DialogInterface.OnClickListener() 
		{  
		    @Override  
		    public void onClick(DialogInterface dialog, int which) 
		    {  
		        dialog.dismiss();                      
		    }  
		});  
		ad.show(); 
	}
	
	private void initStartStopSemanticWindow()
	{
		//check if a semantic session entry OPEN in semantic table is found. 
		SemanticSessionDetails semantic = mDbManager.getOpenSemanticSessionEntry();
		if(semantic != null) //semantic session open found
		{
			Utils.semanticWindowStatus = true;
			
			Utils.bc_cumulative = 0; //reset previous sum				
			mBcCumulativeTv.setText("Cumulative BC: 0.0 "+getResources().getString(R.string.micrograms)+" (wait for 10secs)");
			mStartStopBtn.setBackgroundResource(R.drawable.stop); //show stop button
				
			Utils.show_bc_cumulative = true;			
			
			Utils.semanticSessionNumber = Utils.getSemanticSessionNumber(getApplicationContext());
			Utils.semanticStartPointNumber = Utils.getSemanticStartPointNumber(getApplicationContext());
		}
		else
		{
			Utils.semanticWindowStatus = false;
			
			mStartStopBtn.setBackgroundResource(R.drawable.play); //show play button
			Utils.show_bc_cumulative = false;
		}
	}
	
	/****************** OTTIENE RIFERIMENTO AI BOTTONI *********************************/
	
	public void getButtonRefs()
	{		
		mZoomControls = (LinearLayout)findViewById(R.id.zoomLinearLayout);
		mZoomControls.setVisibility(View.GONE);
		
		mTrackLengthBtn = (Button)findViewById(R.id.trackLengthBtn);
		mFollowCamBtn = (Button)findViewById(R.id.followCameraBtn);
		mZoomOutBtn = (Button)findViewById(R.id.zoomOutBtn);
		mZoomInBtn = (Button)findViewById(R.id.zoomInBtn);
		mInsertAnnBtn = (Button)findViewById(R.id.insertAnnBtn);
		mShareBtn = (Button)findViewById(R.id.shareBtn);
		
		mStartStopBtn = (Button)findViewById(R.id.startStopBtn);
		mBcCumulativeTv = (TextView)findViewById(R.id.bcCumulativeTv);
		mTimeLeftTv = (TextView)findViewById(R.id.timeLeftTv);
		
		mStartStopBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				//start calculate bc cumulative
				if(!Utils.show_bc_cumulative)
				{
					if(Utils.show_bc)
					{
						if(mNewRecord == null)
						{
							Toast.makeText(getApplicationContext(), "Records not coming from sensor box: can't open the semantic session", Toast.LENGTH_LONG).show();
							return;
						}
						Utils.bc_cumulative = 0; //reset previous sum				
						mBcCumulativeTv.setText("Cumulative BC: 0.0 "+getResources().getString(R.string.micrograms));
						mStartStopBtn.setBackgroundResource(R.drawable.stop); //show stop button
						
						mStartPlayTs = System.currentTimeMillis();
						mTimeLeftTv.setText("Time: 00:00:00");	
						Utils.show_bc_cumulative = true;
						
						//************* management of semantic session details ************//
						
						//creating of 'semanticSessionString' as cuncatenation of semanticSessionSeed and semanticSessionNumber
						String semanticSessionSeed = Utils.installID;
						int semanticSessionNumber = Utils.increaseSemanticSessionNumber(getApplicationContext());
						
						//save onSince field of actual record (also called sb_time_on and sourcePointNumber) to be use to calculated the difference between successive
						//onSince values of this semantic window and this saved value
						Utils.setSemanticStartPointNumber(getApplicationContext(), mNewRecord.mSourcePointNumber);
						
						Log.d("OnClickListener", "onClick()--> semantic session seed: "+semanticSessionSeed+ " semantic session number: "+semanticSessionNumber+
								" start semantic point number: "+mNewRecord.mSourcePointNumber);
						
						//when user presses PLAY (or 'start'), a new semantic session entry in semantic table is created. An entry, initially contains
						//the source track id, generated from box, the Android generated session seed and the onSince value (as source point number, generated from box)
						//relative to actual record
						if(!mDbManager.createSemanticSessionEntry(mNewRecord.mSourceSessionSeed, mNewRecord.mSourceSessionNumber, semanticSessionSeed, semanticSessionNumber, 
								mNewRecord.mSourcePointNumber))
						{
							Toast.makeText(getApplicationContext(), "ERROR ON creating semantic session", Toast.LENGTH_LONG).show();
							return;
						}
						
						//set bc cumulative window status to true (= open)
						//Utils.setSemanticWindowStatus(getApplicationContext(), true);
						Utils.semanticWindowStatus = true;
						
						//save source session seed and number (to verify changes in records coming from sensor box)
						//Utils.setSourceSessionSeed(getApplicationContext(), mNewRecord.mSourceSessionSeed);
						Utils.setSourceSessionNumber(getApplicationContext(), mNewRecord.mSourceSessionNumber);
						
						Toast.makeText(getApplicationContext(), "Semantic session entry created", Toast.LENGTH_LONG).show();
						
						//debug print
						mDbManager.printSemanticSessionEntries();
						
						//***********end of managemend of semantic session details *************//
					}
					else
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.wait_bc_show_value), Toast.LENGTH_LONG).show();					
				}
				//stop calculate bc cumulative
				else
				{										
					//String semanticSessionSeed = Utils.installID;
					//int semanticSessionNumber = Utils.getSemanticSessionNumber(getApplicationContext());
					
					if(mNewRecord != null)
					{
						if(!mDbManager.closeSemanticSessionEntry(mNewRecord.mSourcePointNumber))
						{
							Toast.makeText(getApplicationContext(), "ERROR ON closing semantic session", Toast.LENGTH_LONG).show();
							return;
						}
					}
					else
					{
						Toast.makeText(getApplicationContext(), "Records not coming from sensor box: can't close the semantic session", Toast.LENGTH_LONG).show();
						return;
					}
					
					mStartStopBtn.setBackgroundResource(R.drawable.play); //show play button
					Utils.show_bc_cumulative = false;
						
					//set bc cumulative window status to false (= closed)
					//Utils.setSemanticWindowStatus(getApplicationContext(), false);
					Utils.semanticWindowStatus = false;
					
					//reset source session seed and number
					//Utils.setSourceSessionSeed(getApplicationContext(), "");
					Utils.setSourceSessionNumber(getApplicationContext(), -1);
					
					Toast.makeText(getApplicationContext(), "Semantic session successfully closed", Toast.LENGTH_LONG).show();
		
					//debug print
					mDbManager.printSemanticSessionEntries();
				}
			}			
		});
		mStartStopBtn.setBackgroundResource(R.drawable.play);
		mStartStopBtn.setSoundEffectsEnabled(true);
		
		mTrackLengthBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				if(Utils.getTrackLength(getApplicationContext()) == Constants.SIXTY_MINS)
				{
					mTrackLengthBtn.setBackgroundResource(R.drawable.five_mins_button);      		
					Utils.setTrackLength(Constants.FIVE_MINS, getApplicationContext());
	       		
					mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - Constants.FIVE_MINS,1);
					mGoogleMap.clear();
					if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))	       		
						drawPath();
				}
				else if(Utils.getTrackLength(getApplicationContext()) == Constants.FIVE_MINS)
				{			
					mTrackLengthBtn.setBackgroundResource(R.drawable.fifteen_mins_button); 	
					Utils.setTrackLength(Constants.FIFTEEN_MINS, getApplicationContext());
	       		
					mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - Constants.FIFTEEN_MINS, 1);	
					mGoogleMap.clear();
					if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))
						drawPath();
				}
				else if(Utils.getTrackLength(getApplicationContext()) == Constants.FIFTEEN_MINS)
				{
					mTrackLengthBtn.setBackgroundResource(R.drawable.sixty_mins_button); 
	       			Utils.setTrackLength(Constants.SIXTY_MINS, getApplicationContext());
	       		
	       			mLatLngPoints = mDbManager.loadLatLngPointsFromTimestamp(new Date().getTime() - Constants.SIXTY_MINS,4);
	       			mGoogleMap.clear();
	       			if((mLatLngPoints!=null)&&(mLatLngPoints.size()>0))     			
	       				drawPath();
				}
			}			
		});
		
		mFollowCamBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				Log.d("Map", "track mode: " +mTrackMode);
				if(mTrackMode)
					setCameraTracking();
			}			
		});
		
		
		mZoomOutBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				// Zoom out
				try
				{
					mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(-1f)); 
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
				//read and save actual zoom level
				mZoom = mGoogleMap.getCameraPosition().zoom;
			}			
		});
		
		mZoomInBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				// Zoom in
				try
				{
					mGoogleMap.moveCamera(CameraUpdateFactory.zoomBy(1f)); 
				}
				catch(NullPointerException e)
				{
					e.printStackTrace();
				}
				//read and save actual zoom level
				mZoom = mGoogleMap.getCameraPosition().zoom;
			}			
		});
		
		mShareBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				mFacebookManager = FacebookManager.getInstance(Map.this, mFacebookHandler);
				mTwitterManager = TwitterManager.getInstance(Map.this);
				
				final Dialog dialog = new Dialog(Map.this);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.share_dialog);
				//dialog.setTitle("Activate login on...");

				getShareButtonsRef(dialog);
				
				dialog.show();
			}			
		});
		
		mInsertAnnBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				final Dialog insertDialog = new Dialog(Map.this);
				insertDialog.setContentView(R.layout.insert_dialog);
				insertDialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, 
						LayoutParams.WRAP_CONTENT);
				insertDialog.setTitle(R.string.annotation_insertion);
				insertDialog.setCancelable(false);		

				//get reference to send button
				final Button sendButton = (Button)insertDialog.findViewById(R.id.send_button);
				sendButton.setEnabled(false); //active only if there's text

				//get reference to cancel/close window button
				final Button cancelButton = (Button)insertDialog.findViewById(R.id.cancel_button);
				cancelButton.setEnabled(true); //active all time
				cancelButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v) 
					{
						insertDialog.dismiss();
					}					
				});

				//get reference to edittext in which user writes annotation
				final EditText editText = (EditText)insertDialog.findViewById(R.id.annotation_editText);
				editText.addTextChangedListener(new TextWatcher() 
		        {
		            @Override
		            public void onTextChanged(CharSequence s, int start, int before, int count) 
		            {		            	
		            	//if modified text length is more than 0, activate send button
		            	if(s.length() > 0)
		            		sendButton.setEnabled(true);
		            	else
		            		sendButton.setEnabled(false);
		            }

		            @Override
		            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		            @Override
		            public void afterTextChanged(Editable s) {}
		         });

				//get checkbox references
				CheckBox facebookChBox = (CheckBox)insertDialog.findViewById(R.id.facebook_checkBox);
				CheckBox twitterChBox = (CheckBox)insertDialog.findViewById(R.id.twitter_checkBox);
				
				//activate check boxes depends from log in facebook/twitter
				boolean[] logs = new boolean[2];
				logs[0] = Utils.getValidFbSession(getApplicationContext());
				logs[1] = Utils.getValidTwSession(getApplicationContext());
				
				facebookChBox.setEnabled(logs[0]);
				twitterChBox.setEnabled(logs[1]);

				//checked on check boxes
				final boolean[] checkeds = Utils.getShareCheckedOn(getApplicationContext());
				if(checkeds[0] == true)
					facebookChBox.setChecked(true);
				else
					facebookChBox.setChecked(false);
				if(checkeds[1] == true)
					twitterChBox.setChecked(true);
				else
					twitterChBox.setChecked(false);				
				
				facebookChBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean checked) 
					{
						Utils.setShareCheckedOn(checked, checkeds[1], getApplicationContext());
						checkeds[0] = checked;
					}					
				});
				
				twitterChBox.setOnCheckedChangeListener(new OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean checked) 
					{
						Utils.setShareCheckedOn(checkeds[0], checked, getApplicationContext());
						checkeds[1] = checked;
					}					
				});
				
				//send annotation to server and on facebook/twitter if user is logged on 
				sendButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v) 
					{
						//1 - read inserted annotation
						String annotation = editText.getText().toString();
						
						//2 - update record on db with annotation and save recordId
						double recordId = Utils.getNewRecordId(getApplicationContext());
						Record recordToUpdate = mDbManager.loadRecordById(recordId);
						//if mSysTimestamp != 0, record has been uploaded to server: if I save annotation on it, annotation will
						//be not uploaded
						if(recordToUpdate.mSysTimestamp != 0)
						{
							try 
							{
								//wait for half second
								Thread.sleep(500);
								//take a new recordId (I hope)
								recordId = Utils.getNewRecordId(getApplicationContext());								
							} 
							catch (InterruptedException e) 
							{
								e.printStackTrace();
							}
						}						
						int result = mDbManager.updateRecordAnnotation(recordId, annotation);
						
						if(result == 1)
							Toast.makeText(getApplicationContext(), "Updated record", Toast.LENGTH_LONG).show();
						else
							Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_LONG).show();
						
						boolean[] checks = Utils.getShareCheckedOn(getApplicationContext());
						
						//3 - share on facebook is user wants and internet is active now
						if(checks[0] == true)
						{
							Record annotatedRecord = mDbManager.loadRecordById(recordId);
							try 
							{
								FacebookManager fb = FacebookManager.getInstance(null,null);
								if(fb != null)
									fb.postMessageOnWall(annotatedRecord);
			                } 
							catch (Exception e) 
							{
			                    e.printStackTrace();
			                }								
						}
						
						//4 - share on twitter is user wants and internet is active now
						if(checks[1] == true)
						{
							Record annotatedRecord = mDbManager.loadRecordById(recordId);
							
							try 
							{
								TwitterManager twManager = TwitterManager.getInstance(null);
								twManager.postMessage(annotatedRecord);
			                } 
							catch (Exception e) 
							{
			                    e.printStackTrace();
			                }
						}

						//5 - show marker for annotated record
						Record annotatedRecord = mDbManager.loadRecordById(recordId);
							
						String userAnn = annotatedRecord.mUserData1;
					           
					    if(!userAnn.equals("")&&(annotatedRecord.mValues[0] != 0))
					    {
					    	BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.annotation_marker);
						    mGoogleMap.addMarker(new MarkerOptions()
					        	.position(new LatLng(annotatedRecord.mValues[0], annotatedRecord.mValues[1]))
					        	.title("BC: "+String.valueOf(annotatedRecord.mBcMobile)+ " "+getResources().getString(R.string.micrograms))
					        	.snippet("Annotation: " +userAnn)
					        	.icon(icon)
					        	.anchor(0f, 1f));
					    }
						
						insertDialog.dismiss();				
					}					
				});
				
				insertDialog.show();
			}			
		});
		
		mTrackLengthBtn.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View arg0) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.track_length_btn_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 250); //250 from top on a 480x800 screen
				toast.show();
				return true;
			}			
		});
		
		mFollowCamBtn.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View arg0) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.camera_track_btn_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 170); //170 from top on a 480x800 screen
				toast.show();
				return true;
			}			
		});
				
		mInsertAnnBtn.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View arg0) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.insert_ann_btn_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 170); //170 from top on a 480x800 screen
				toast.show();
				return true;
			}			
		});
		
		mShareBtn.setOnLongClickListener(new OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View arg0) 
			{				
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.share_btn_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.TOP|Gravity.RIGHT, 0, 250); //250 from top on a 480x800 screen
				toast.show();
				return false;
			}			
		});
		
		//get reference to new record led indicator
		mNewPinIv = (ImageView)findViewById(R.id.newPinIv);
		mNewPinIv.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{	
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.new_data_icon_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM|Gravity.LEFT, 0, 170); //170 from top on a 480x800 screen
				toast.show();
			}			
		});
		
        //reference to TextView containing air quality index textual description
        mAqiTv = (TextView)findViewById(R.id.aqiTv);
        mSpectrum = (LinearLayout)findViewById(R.id.spectrumLinearLayout);
        
		//status icons references
		mGpsStatus = (ImageView)findViewById(R.id.gpsStatusIv);
		mBtStatus = (ImageView)findViewById(R.id.btStatusIv);
		mInterUplStatus = (ImageView)findViewById(R.id.interUplStatusIv);
		
		mGpsStatus.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.gps_status_icon_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 170); //170 from top on a 480x800 screen
				toast.show();
			}			
		});
		
		mBtStatus.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.bt_status_icon_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 170); //170 from top on a 480x800 screen
				toast.show();				
			}			
		});
		
		mInterUplStatus.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.inter_upl_status_icon_text), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM|Gravity.CENTER, 0, 170); //170 from top on a 480x800 screen
				toast.show();				
			}			
		});
		
		//status icon initializations
		mGpsStatus.setBackgroundResource(R.drawable.gps_off);
		mBtStatus.setBackgroundResource(R.drawable.bt_on);	
		
		//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
		int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
		
		//1 - is internet connection available? 
		boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
		
		//if user wants to upload only on wifi networks, connectivity[0] (network connectivity) must be true
		if(networkTypeIndex == 0)
		{
			if(connectivity[0])
				mConnectivityOn = true;
			else
				mConnectivityOn = false;
		}
		else //if user wants to upload both on wifi/mobile networks
			mConnectivityOn = connectivity[0] || connectivity[1];
		
		if(mConnectivityOn)
		{
			mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
			Utils.uploadOn = Constants.INTERNET_ON_INT;	
		}
		else
		{
			mInterUplStatus.setBackgroundResource(R.drawable.internet_off);           
			Utils.uploadOn = Constants.INTERNET_OFF_INT;
		}
	}
	
	private void setBcLevelColors()
	{
		TextView[] levelsTv = new TextView[7];
		levelsTv[0] = (TextView)findViewById(R.id.level1Tv);
		levelsTv[1] = (TextView)findViewById(R.id.level2Tv);
		levelsTv[2] = (TextView)findViewById(R.id.level3Tv);
		levelsTv[3] = (TextView)findViewById(R.id.level4Tv);
		levelsTv[4] = (TextView)findViewById(R.id.level5Tv);
		levelsTv[5] = (TextView)findViewById(R.id.level6Tv);
		levelsTv[6] = (TextView)findViewById(R.id.level7Tv);
		
		levelsTv[0].setBackgroundColor(ColorHelper.numberToColor(5));
		levelsTv[1].setBackgroundColor(ColorHelper.numberToColor(20));
		levelsTv[2].setBackgroundColor(ColorHelper.numberToColor(40));
		levelsTv[3].setBackgroundColor(ColorHelper.numberToColor(70));
		levelsTv[4].setBackgroundColor(ColorHelper.numberToColor(80));
		levelsTv[5].setBackgroundColor(ColorHelper.numberToColor(90));
		levelsTv[6].setBackgroundColor(ColorHelper.numberToColor(100));
	}
	
	private void updateAqiTv()
	{
		boolean continuous = true;
		
		int color = Color.DKGRAY;
		
		if(Utils.show_bc)
		{
			int bc_int = (int)Math.round(Utils.bc * 100);
			
			if(Utils.show_bc_cumulative)
			{
				int bc_cumulative_int = (int)Math.round(Utils.bc_cumulative * 100);
				
				mBcCumulativeTv.setText("Cumulative BC: "+((double)bc_cumulative_int/100)+" "+getResources().getString(R.string.micrograms));
			
				if(mStartPlayTs == 0)
					mStartPlayTs = System.currentTimeMillis();
				cal.setTimeInMillis(System.currentTimeMillis() - mStartPlayTs);
		
				if(cal.get(Calendar.HOUR) - 1 < 10)
					mTimeDiff = "0"+String.valueOf((cal.get(Calendar.HOUR) - 1));
				else
					mTimeDiff = String.valueOf((cal.get(Calendar.HOUR) - 1));
				
				if(cal.get(Calendar.MINUTE) < 10)
					mTimeDiff += ":0"+String.valueOf(cal.get(Calendar.MINUTE));
				else
					mTimeDiff += ":"+String.valueOf(cal.get(Calendar.MINUTE));
				
				if(cal.get(Calendar.SECOND) < 10)
					mTimeDiff += ":0"+String.valueOf(cal.get(Calendar.SECOND));
				else
					mTimeDiff += ":"+String.valueOf(cal.get(Calendar.SECOND));
				
				mTimeLeftTv.setText("Time: " +mTimeDiff);
			}
			
			if(continuous)
			{
				//10 is upper limit in scale, so a black carbon of 10 (multiplied by 10) must be drawn in dark red
				if((Utils.bc > 0)&&(Utils.bc <= 10))
					color = ColorHelper.numberToColor(Utils.bc * BC_MULTIPLIER);
				else
					color = ColorHelper.numberToColor(100);
				
				mAqiTv.setBackgroundColor(color); //color of black carbon text view
				
				//appropirate text about black carbon level
				if(Utils.bc < 0.5)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[0]);
				else if(Utils.bc < 2)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[1]);
				else if(Utils.bc < 4)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[1]);
				else if(Utils.bc < 6)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[2]);
				else if(Utils.bc < 8)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[3]);
				else if(Utils.bc < 10)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[3]);
				else if(Utils.bc > 10)
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[4]);										
			}
			else
			{
				//appropriate text about black carbon level
				if(Utils.bc < 0.5)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[0]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[0]);
				}
				else if(Utils.bc < 2)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[1]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[1]);
				}
				else if(Utils.bc < 4)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[2]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[1]);
				}
				else if(Utils.bc < 6)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[3]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[2]);
				}
				else if(Utils.bc < 8)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[4]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[3]);
				}
				else if(Utils.bc < 10)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[5]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[3]);
				}
				else if(Utils.bc > 10)
				{
					mAqiTv.setBackgroundColor(Color.parseColor(Constants.BC_COLORS[6]));
					mAqiTv.setText(((double)bc_int/100) + " "+getResources().getString(R.string.micrograms)+ " - " +Constants.BC_LEVELS[4]);
				}				
			}
			mAqiTv.setVisibility(View.VISIBLE);
			mSpectrum.setVisibility(View.VISIBLE);			
		}
	}
	
	/************************** DISEGNA COMMUNITY RECORDS SU COMMUNITY MAP *******************************/

	//draw community annotated markers on map
	public void drawCommunityRecords()
	{
		//0 - clean overlays on map, if any
		mGoogleMap.clear();
				
		//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
		int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
		
		//1 - is internet connection available? 
		boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
		
		//if user wants to upload only on wifi networks, connectivity[0] (network connectivity) must be true
		if(networkTypeIndex == 0)
		{
			if(connectivity[0])
				mConnectivityOn = true;
			else
				mConnectivityOn = false;
		}
		else //if user wants to upload both on wifi/mobile networks
			mConnectivityOn = connectivity[0] || connectivity[1];
		
		//2 - add my current annotations to mAnnotatedRecords array
		//mAnnotatedRecords = mDbManager.loadAnnotatedRecords();
		
		//3 - if internet connectivity is on, send http request and add received records to
		//    mAnnotatedRecord array
		if(mConnectivityOn)
		{
			//show wait dialog until annotated records are drawn on map
			createProgressDialog("Receiving data...", true);
			
			mGetDataThread = new GetDataThread();
			mGetDataThread.start();
		}
		else
			noConnectivityDialog();
	}
	
	/************************** DISEGNA TESTO ANNOTAZIONI MIE SU TRACK MAP **************************************/
	/*
	//draws markers with my annotations (inserted by user during track recording)
	public void drawTextAnnotations()
	{	
		//1 - load all annotated records
		mAnnotatedRecords = mDbManager.loadAnnotatedRecords();
				
		for(int i = 0; i < mAnnotatedRecords.size(); i++)
		{			
			//2 - draw overlay containing annotations as underlined text
			TextAnnotationOverlay textAnnotationOverlay = new TextAnnotationOverlay(mTextAnnIcon);
			AnnotatedRecord annRec = mAnnotatedRecords.get(i);
			
			textAnnotationOverlay.addItem(new GeoPoint((int)(annRec.mLat * 1000000),
					(int)(annRec.mLon * 1000000)), ""  , annRec.mUserData);
			
			Log.d("Map", "drawTextAnnotations()-->" +annRec.toString());
			mMapView.getOverlays().add(textAnnotationOverlay);
		}		
	}*/
	
	/****************** DISEGNA PATH ****************************************************/
	
	//draw path from a list of points loaded as history from db
	public void drawPath()
	{
		boolean continuous = true;
		
		ExtendedLatLng newLatLng = null; 
		ExtendedLatLng precLatLng = null;
				
		int color = Color.DKGRAY;
		
		try
		{
			//Log.d("Map", "drawPath()--> polyline count: " +mLatLngPoints.size());
			
			for (int i = 0; i < mLatLngPoints.size(); i++) 
	        {
				newLatLng = mLatLngPoints.get(i);
				
				color = Color.DKGRAY; //color for a segment without bc value
	        
	    	        //double value = newLatLng.mAvgValue;
	    	       
	    	        //if(value < avg_poll_min)
	    	        //	avg_poll_min = value;

	    	        //value must be in [0,1]
	    	        //int color  = ColorHelper.numberToColor((value - avg_poll_min) * poll_mult+poll_base);
	    	        
			    if(!continuous)
			    {
			    	if(newLatLng.mBc > 0)
		    	    {
			    		if(newLatLng.mBc < 0.5)
			    			color = Color.parseColor(Constants.BC_COLORS[0]);
		    	        else if(newLatLng.mBc < 2)
				    		color = Color.parseColor(Constants.BC_COLORS[1]);
		    	        else if(newLatLng.mBc < 4)
				    		color = Color.parseColor(Constants.BC_COLORS[2]);
		    	        else if(newLatLng.mBc < 6)
				    		color = Color.parseColor(Constants.BC_COLORS[3]);
		    	        else if(newLatLng.mBc < 8)
				    		color = Color.parseColor(Constants.BC_COLORS[4]);
		    	        else if(newLatLng.mBc < 10)
				    		color = Color.parseColor(Constants.BC_COLORS[5]);
		    	        else if(newLatLng.mBc > 12)
				    		color = Color.parseColor(Constants.BC_COLORS[6]);
		    	    }
			    }
			    else
			    {
					//10 is upper limit in scale, so a black carbon of 10 (multiplied by 10) must be drawn in dark red
					if((newLatLng.mBc > 0)&&(newLatLng.mBc <= 10))
						color = ColorHelper.numberToColor(newLatLng.mBc * BC_MULTIPLIER);
					else
						color = ColorHelper.numberToColor(100);
			    }

	    	    //draw segment with appropriate color (dark grey if no bc value is present) 
	    	    if(precLatLng != null)
	    	    	mGoogleMap.addPolyline(new PolylineOptions().add(newLatLng.mLatLng).add(precLatLng.mLatLng).width(5).color(color));
	            
	    	    //draw annotation as a clickable marker
	            String annotation = newLatLng.mUserAnn;	            
	            if(!annotation.equals(""))
	            {
	            	BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.annotation_marker);
		            mGoogleMap.addMarker(new MarkerOptions()
	        			.position(newLatLng.mLatLng)
	        			.title("BC: "+newLatLng.mBc)
	        			.snippet("Annotation: " +annotation)
	        			.icon(icon)
	        			.anchor(0.25f, 1f)); //Map Markers are 'anchored' by default to the middle of the bottom of layout (i.e., anchor(0.5,1)).
	            }
	            
	            //save reference to object
	            precLatLng = newLatLng;
	        }			
			
			//center camera on last trackn position
			if((mTrackMode)&&(mCameraTrackOn))
			try
			{
				mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLngPoints.get(mLatLngPoints.size()-1).mLatLng, mZoom));
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	//draw path between two points
	public void drawSegment(ExtendedLatLng newExtLatLng, LatLng precLatLng)
	{
		boolean continuous = true;
		
		/*
		double value = newExtLatLng.mAvgValue;
	       
        if(value < avg_poll_min)
        	avg_poll_min = value;

        //value must be in [0,1]
        //int color  = ColorHelper.numberToColor((value - avg_poll_min) * poll_mult+poll_base);
*/
        
		int color = Color.DKGRAY;
        
		if(!continuous)
		{
			if(newExtLatLng.mBc > 0)
	        {
	        	if(newExtLatLng.mBc < 0.5)
	    			color = Color.parseColor(Constants.BC_COLORS[0]);
	        	else if(newExtLatLng.mBc < 2)
	        		color = Color.parseColor(Constants.BC_COLORS[1]);
	        	else if(newExtLatLng.mBc < 4)
	        		color = Color.parseColor(Constants.BC_COLORS[2]);
	        	else if(newExtLatLng.mBc < 6)
	        		color = Color.parseColor(Constants.BC_COLORS[3]);
	        	else if(newExtLatLng.mBc < 8)
	        		color = Color.parseColor(Constants.BC_COLORS[4]);
	        	else if(newExtLatLng.mBc < 10)
	        		color = Color.parseColor(Constants.BC_COLORS[5]);
	        	else if(newExtLatLng.mBc > 12)
	        		color = Color.parseColor(Constants.BC_COLORS[6]);
	        }
		}
		else
		{
			//10 is upper limit in scale, so a black carbon of 10 (multiplied by 10) must be drawn in dark red
			if((newExtLatLng.mBc > 0)&&(newExtLatLng.mBc <= 10))
				color = ColorHelper.numberToColor(newExtLatLng.mBc * BC_MULTIPLIER);
			else
				color = ColorHelper.numberToColor(100);
		}
		
        mGoogleMap.addPolyline(new PolylineOptions().add(newExtLatLng.mLatLng).add(precLatLng).width(5).color(color));
	}
	
	/****************** CARICA DA DB ULTIMO RECORD RICEVUTO *********************************/
	
	int iterations = 0;
	
	//load last recorded (on db) record 
	private Runnable mGetLastRecIdRunnable = new Runnable()
	{				
		@Override
		public void run() 
		{			
			//obtain last received record id
			double lastRecordId = Utils.getNewRecordId(getApplicationContext());
			
			//check if last record id is different from previous received (if they are the same, this means
			//that we aren't obtaining any new data from sensor box)
			if(mPrecRecordId != lastRecordId)
			{
				Record tmp = Utils.lastSavedRecord;
				
				if(tmp != null)
				{
					updateAqiTv();
					
					/*** FOR SEMANTIC WINDOW : WHEN SOURCE SESSION NUMBER CHANGES AND A SEMANTIC SESSION WINDOW IS OPEN, CLOSE THE SESSION AND BACK TO NORMAL STATE ***/
					if(Utils.semanticWindowStatus)
					{
						if(Utils.getSourceSessionNumber(getApplicationContext()) < tmp.mSourceSessionNumber)
						{
							Log.d("GetLastRecIdRunnable", "run()--> actual source session number: "+Utils.sourceSessionNumber+" NEW RECORD source session number: "+tmp.mSourceSessionNumber);
							Log.d("GetLastRecIdRunnable", "run()--> actual source point number: "+Utils.sourcePointNumber+" NEW RECORD source point number: "+tmp.mSourcePointNumber);
							Log.d("GetLastRecIdRunnable", "run()--> CLOSING SEMANTIC SESSION WINDOW");
							
							mDbManager.closeSemanticSessionEntry(Utils.sourcePointNumber);
							
							mStartStopBtn.setBackgroundResource(R.drawable.play); //show play button
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
				
				//first iteration
				if(mPrecRecord == null)
				{
					Utils.setFirstRecordId(lastRecordId, getApplicationContext());
					
					//tmp.mValues[0] != 0 is a filter on latitude
					if((tmp != null)&&(tmp.mValues[0] != 0))
					{
						mPrecRecord = tmp;
						mRecords.add(mPrecRecord);
						mLatLngPoints.add(new ExtendedLatLng(tmp.mId, mPrecRecord.mValues[0], mPrecRecord.mValues[1], tmp.mSysTimestamp, tmp.mValues, tmp.mBcMobile, tmp.mUserData1));
						
						//calcMinAvgPollValue();
						
						//center camera on new position
						if((mTrackMode)&&(mCameraTrackOn))
						try
						{
							mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mPrecRecord.mValues[0], mPrecRecord.mValues[1]), mZoom));
						}
						catch(NullPointerException e)
						{
							e.printStackTrace();
						}
						//show new record pin and hide it after 250 millisec
						mNewPinIv.setVisibility(View.VISIBLE);	
						mHandler.postDelayed(hidePin, 250);
						
						iterations++;
					}					
				}
				//second iteration
				else if((mPrecRecord != null)&&(mNewRecord == null))	
				{
					if((tmp != null)&&(tmp.mValues[0] != 0))
					{				
						mNewRecord = tmp;
						mRecords.add(mNewRecord);
					
						ExtendedLatLng newExtLatLng = new ExtendedLatLng(tmp.mId, mNewRecord.mValues[0], mNewRecord.mValues[1], tmp.mSysTimestamp, tmp.mValues, tmp.mBcMobile, tmp.mUserData1);						
						mLatLngPoints.add(newExtLatLng);	
						
						//calcMinAvgPollValue();
						
						//center camera on new position
						if((mTrackMode)&&(mCameraTrackOn))
						try
						{
							mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mPrecRecord.mValues[0], mPrecRecord.mValues[1]), mZoom));
						}
						catch(NullPointerException e)
						{
							e.printStackTrace();
						}
						drawSegment(newExtLatLng, new LatLng(mPrecRecord.mValues[0], mPrecRecord.mValues[1]));
						
						//show new record pin and hide it after 250 millisec
						mNewPinIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hidePin, 250);
						
						iterations++;
					}
				}
				//next iterations (from third)
				else if((mPrecRecord != null)&&(mNewRecord != null))
				{
					//the second term is a filter on latitudine, that must be explicit
					if((tmp != null)&&(tmp.mValues[0] != 0))
					{		
						mPrecRecord = mNewRecord;
						mNewRecord = tmp;
						mRecords.add(mNewRecord);
						
						if(Utils.getTrackLength(getApplicationContext()) == Constants.SIXTY_MINS)
						{							
							//in sixty_mins track length, draw 1 point every 4
							if(iterations % 4 == 0)					
							{
								ExtendedLatLng newExtLatLng = new ExtendedLatLng(tmp.mId, mNewRecord.mValues[0], mNewRecord.mValues[1], tmp.mSysTimestamp, tmp.mValues, tmp.mBcMobile, tmp.mUserData1);								
								mLatLngPoints.add(newExtLatLng);		
								
								//get precedent LatLng in list and draw segment between this and last LatLng
								ExtendedLatLng extLatLng = mLatLngPoints.get(mLatLngPoints.size()-2); 
								if(extLatLng != null)
									drawSegment(newExtLatLng, new LatLng(extLatLng.mLatLng.latitude, extLatLng.mLatLng.longitude));
								
								//center map on last point
								if((mTrackMode)&&(mCameraTrackOn))
								try
								{
									mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mNewRecord.mValues[0], mNewRecord.mValues[1]), mZoom));
								}
								catch(NullPointerException e)
								{
									e.printStackTrace();
								}
							}
							//else do nothing
						}
						else //for 5-15 mins track length
						{
							ExtendedLatLng newExtLatLng = new ExtendedLatLng(tmp.mId, mNewRecord.mValues[0], mNewRecord.mValues[1], tmp.mSysTimestamp, tmp.mValues, tmp.mBcMobile, tmp.mUserData1);								
							mLatLngPoints.add(newExtLatLng);															
							drawSegment(newExtLatLng, new LatLng(mPrecRecord.mValues[0], mPrecRecord.mValues[1]));
							
							//center map on last point
							if((mTrackMode)&&(mCameraTrackOn))
							try
							{
								mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mNewRecord.mValues[0], mNewRecord.mValues[1]), mZoom));
							}
							catch(NullPointerException e)
							{
								e.printStackTrace();
							}
						}
						
						//Log.d("Map", "Runnable::run()--> prec record: " +mPrecRecord.toString());
						//Log.d("Map", "Runnable::run()--> new record: " +mNewRecord.toString());

						//show new record pin and hide it after 250 millisec
						mNewPinIv.setVisibility(View.VISIBLE);			
						mHandler.postDelayed(hidePin, 250);
						
						iterations++;
						
						//if first record is older than one hour, remove it from arrays 
						//arrays are: mRecords, mGeoPointsBackup
						if(mRecords.get(0).mSysTimestamp <= (new Date().getTime() - Constants.SIXTY_MINS))
						{
							mRecords.remove(0);
							//Log.d("Map", "********************************* First record removed ********************************");
						}
						
						//if first displayed geoPoint exceeds track length (older than actual track duration), remove it
						if(mLatLngPoints.get(0).mSysTimestamp <= (new Date().getTime() - Utils.getTrackLength(getApplicationContext())))
						{							
							//Log.d("Map", "******************************** First geopoint removed *******************************");
							//Log.d("Map", "actual track length: " +Utils.getTrackLength(getApplicationContext()));
							//Log.d("Map", mLatLngPoints.get(0).mSysTimestamp+ " <= " +(new Date().getTime() - Utils.getTrackLength(getApplicationContext())));
							
							mLatLngPoints.remove(0);
						}
						
						//Log.d("Map", "******** latlng points list length: " +mLatLngPoints.size());
						//Log.d("Map", "******** records list length: " +mRecords.size());
					}
				}
				
				mPrecRecordId = lastRecordId; //backup del record id
			}
			else
			{
				mGpsStatus.setBackgroundResource(R.drawable.gps_off);
				Log.d("Map", "****************** Record già ricevuto!!! *********************************");
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
	
    /************************ ALERT DIALOG NO INTERNET AVAILABLE *************************************/
    
    public void noConnectivityDialog() 
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_no_internet)
        	.setIcon(android.R.drawable.ic_dialog_info)
        	.setTitle(R.string.app_name)
        	.setCancelable( false )
        	.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() 
        	{
        		public void onClick(DialogInterface dialog, int id) 
        		{
	
        		}
        	});
        
        AlertDialog alert = builder.create();
        alert.show(); 
    }
	
    /****************** GESTIONE CAMERA TRACKING SU ULTIMO PUNTO INSERITO ***********************/
    
    public void setCameraTracking()
    {
    	mCameraTrackOn = !mCameraTrackOn;
    	
    	if(mCameraTrackOn)
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_pressed);
    	else
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_not_pressed);
    }
    
    /****************** CALCULATE MIN AND MAX BC VALUES IN LOADED HISTORY ***********************/
 /*   
    public void calcMinMaxBCvalue()
    {
    	int j = 0;
    	boolean found = false;
    	
    	//init min/max variable with first valid black carbon value of the serie
    	while((j < mLatLngPoints.size())&&(!found))
    	{
    		if(mLatLngPoints.get(j).mBc > 0)
    		{
    			found = true;
    			mMaxBcValue = mMinBcValue = mLatLngPoints.get(j).mBc;
    		}
    		else
    			j++;
    	}
    	
    	if(!found)
    		return;
    	
    	mMinMaxInitialized = found;
    	
    	//if min/max values have been initialized (--> the serie contains at least one valid
    	//bc value not equals to zero), update them cycling over the entire serie from the 
    	//last point of the previous cycle 	
    	while(j <  mLatLngPoints.size())
    	{
    		if(mLatLngPoints.get(j).mBc > 0) //only valid bc values are considered
    		{
	    		if(mLatLngPoints.get(j).mBc > mMaxBcValue)
	    			mMaxBcValue = mLatLngPoints.get(j).mBc;   			
	    		else if(mLatLngPoints.get(j).mBc < mMinBcValue)
	    			mMinBcValue = mLatLngPoints.get(j).mBc;
    		}	
    		j++;
    	}   	
    }*/
    
    /****************** CALCULATE MINS VALUE FOR POLLUTANTS IN LOADED HISTORY *******************/
/*    
    public void calcMinAvgPollValue()
    {
    	double actual_avg_poll = 0;
    	
    	//save first avg min
    	avg_poll_min = mLatLngPoints.get(0).mAvgValue;
    	
    	for(int i = 1; i < mLatLngPoints.size(); i++)
    	{
    		actual_avg_poll = mLatLngPoints.get(i).mAvgValue;
    		
    		//if actual avg is less then avg min, save it
    		if(actual_avg_poll < avg_poll_min)
    			avg_poll_min = actual_avg_poll;	
    	}
    	//reduction
    	avg_poll_min = avg_poll_min /2;
    	
    	//Log.d("Map", "calcMinAvgPollValue()--> " +avg_poll_min);
    }
	*/
	/***************** CALCOLA MEDIE VALORI RECORDS COMPRESI IN INTERVALLO **********************/
	
	public MarkerRecord calcAvgRecords(int begin, int end) throws IndexOutOfBoundsException
	{
		int totalRecords = 0;
		long startTs = 0, endTs = 0;
		
		double avg_poll = 0;
		
		totalRecords = end-begin;
		
		Log.d("calcAvgRecords()", "begin: "+begin+" end:" +end+ " totalRecords: " +totalRecords);
		
		for(int i = begin; i < end; i++)
		{
			if(i == begin)
				startTs = mRecords.get(i).mSysTimestamp;
			if(i == end-1)
				endTs = mRecords.get(i).mSysTimestamp;
			
			avg_poll += mRecords.get(i).calcAvgPoll();
			
		}		
		avg_poll = avg_poll / totalRecords;
		
		return new MarkerRecord(avg_poll, totalRecords, startTs, endTs);
	}
	
	public MarkerRecord calcAvgRecords(List<ExtendedLatLng>points) throws IndexOutOfBoundsException
	{
		int totalRecords = 0;
		long startTs = 0, endTs = 0;
		
		double avg_poll = 0;
		
		totalRecords = points.size();
		
		Log.d("calcAvgRecords()", " totalRecords: " +totalRecords);
		
		for(int i = 0; i < points.size(); i++)
		{
			if(i == 0)
				startTs = points.get(i).mSysTimestamp;
			if(i == totalRecords-1)
				endTs = points.get(i).mSysTimestamp;
			
			avg_poll += points.get(i).calcAvgPoll();
			
		}		
		avg_poll = avg_poll / totalRecords;
		
		return new MarkerRecord(avg_poll, totalRecords, startTs, endTs);
	}
	
    /*************************** MAP HANDLER ******************************************************/
    
    private Handler mMapHandler = new Handler()
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
				
				case Constants.DISCOVERY_STARTED:
					Log.d("MapHandler", "discovery started");
					
				break;
				
				case Constants.DISCOVERY_FINISHED:
					
				break;
				
				case Constants.DEVICE_DISCOVERED:										

				break;	
				
				case Constants.CONNECTION_FAILED:
					Log.d("MapHandler", "Failed to connect to selected device");
					
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
						
						Log.d("MapHandler", "Failed 3 reconnection attempts");
						
						//play sound alert when 3 connection attempts fail
						playConnAttempFailed();
						connAttemptFailedDialog();
					}
										
				break;	
				
				case Constants.CONNECTION_LOST:
					Log.d("MapHandler", "Connection lost");
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
					Log.d("MapHandler", "STATE NONE ");
					mBtStatus.setBackgroundResource(R.drawable.bt_off);
					
				break;
				case Constants.STATE_CONNECTING:
					Log.d("MapHandler", "STATE_CONNECTING");
				break;
				case Constants.STATE_CONNECTED:
					Log.d("MapHandler", "STATE_CONNECTED");
					mBtStatus.setBackgroundResource(R.drawable.bt_on);
					mConnAttempts = 1;
					//progress dialog about opening connection is closed
					if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
						mProgressDialog.dismiss();										
				break;	
				/*
				case Constants.DEVICE_GPS_ON:
					mGpsStatus.setBackgroundResource(R.drawable.gps_on_sbox);
				break;
				
				case Constants.DEVICE_GPS_OFF:
					mGpsStatus.setBackgroundResource(R.drawable.gps_off);
				break;
				*/
				case Constants.DOWNLOADING_HISTORY_STARTED:
					Log.d("MapHandler", "DOWNLOAD HISTORY STARTED");
					
				break;
				
				case Constants.DOWNLOADING_HISTORY_FINISHED:
					Log.d("MapHandler", "DOWNLOAD HISTORY FINISHED");

				break;
				
				case Constants.SENSOR_BOX_MAC_NOT_READ:
					Log.d("MapHandler", "SENSOR BOX MAC ADDRESS NOT READ!!!");
					
					 sensorBoxMacNotReadDialog();
				break;
				
    		}
    	}
    };
    
    /******************************* DIALOGS ****************************************************/
    
    public void createProgressDialog(final String msg, boolean cancelable)
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
    				mProgressDialog = ProgressDialog.show(Map.this, getResources().getString(R.string.app_name), msg, true, true);
    			}
    		}
    	});
    }
    
    public void connAttemptFailedDialog() 
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Map.this);
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
    		}
    	});
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
			        AlertDialog.Builder builder = new AlertDialog.Builder(Map.this);
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
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
			        AlertDialog.Builder builder = new AlertDialog.Builder(Map.this);
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
    		}
    	});
    }
	
    /*************************** OPTION MENU ***************************************************/
    
	public boolean onCreateOptionsMenu(Menu menu)
	{		
		SubMenu mapSubMenu = menu.addSubMenu("Map Modes").setIcon(android.R.drawable.ic_menu_mapmode);
		
		mapSubMenu.add(1, Menu.FIRST, Menu.FIRST, "Hybrid").setCheckable(false);		
		mapSubMenu.add(1, Menu.FIRST+1, Menu.FIRST, "Normal").setCheckable(false);
		mapSubMenu.add(1, Menu.FIRST+2, Menu.FIRST+2, "Satellite").setCheckable(false);
		mapSubMenu.add(1, Menu.FIRST+3, Menu.FIRST+3, "Terrain").setCheckable(false);
		
		return super.onCreateOptionsMenu(menu);		
	}
	
    @Override  
    public boolean onOptionsItemSelected(MenuItem item) 
    {  
       int itemId = item.getItemId();
       
       switch(itemId)
       {
			case Menu.FIRST:
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				break;
			case Menu.FIRST + 1:
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				break;
			case Menu.FIRST + 2:
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
				break;
			case Menu.FIRST + 3:
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
				break;		
       }
       return true;
    }
    
    /********************** CLOSE APP *************************************************************************/
    
    public void closeApp()
    { 	 	    	
    	Utils.backToHome = true;
    	Utils.setGpsTrackingOn(false, getApplicationContext());
    	
    	//stop gps tracking service
    	if(Utils.gpsTrackServIntent != null)
    		stopService(Utils.gpsTrackServIntent);
    	
    	//stop store'n'forward service
    	if(Utils.storeForwServIntent != null)
    		stopService(Utils.storeForwServIntent);
    	
		mIsRunning = false;		
		mConnAttempts = 1;
		//remove scheduled runnable
		mHandler.removeCallbacks(mGetLastRecIdRunnable);
		
    	//stop all threads
    	if(mBluetoothManager != null)
    		mBluetoothManager.stop();
    	mBluetoothManager = null;
    	
    	Graph.stopCallerThread();
    	
    	//clear shared prefs
    	Utils.deleteSharedPrefs(getApplicationContext());
    	  	
    	//useful to complete kill the app
    	Map.this.finish();
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
            	Log.d("GpsServiceReceiver", "Phone Gps ON");
            	mGpsStatus.setBackgroundResource(R.drawable.gps_on_phone);

            }
            if (action.equals(Constants.PHONE_GPS_OFF)) 
            {
            	Log.d("GpsServiceReceiver", "Phone Gps OFF");
            	mGpsStatus.setBackgroundResource(R.drawable.gps_off);

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

    /************************ SET BACKGROUND IMG FOR RIGHT DISPLAY RESOLUTION *****************/
   /* 
    private void setBackground()
    {     
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
       
    	LinearLayout top_ll = (LinearLayout)findViewById(R.id.topLinearLayout);
    	LinearLayout bottom_ll = (LinearLayout)findViewById(R.id.bottomLinearLayout);
    	if((screenWidth > 540)&&(screenHeight > 960))
    	{
    		top_ll.setPadding(20, 20, 20, 20);   
    		bottom_ll.setPadding(20, 20, 20, 20);
    	}
    	else 
    	{
    		top_ll.setPadding(7,7,7,7);   
    		bottom_ll.setPadding(7,7,7,7);
    	}
    }    */
        
    
	/********************** CALLS getData() FUNCTION *************************************************/
	
	private class GetDataThread extends Thread
	{
		@Override
		public void run()
		{
			int statusCode = -1;
			try 
			{
				Thread.currentThread().sleep(1000);
				//statusCode = getData();
			} 
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			//this exception is invoked when internet connection is up but traffic is not allowed
			//(for example, for networks that needs login but user is not logged)
			catch(Exception e)
			{
				
			}/*
			catch (HttpHostConnectException e) 
			{
				e.printStackTrace();
			} 
			catch (IllegalArgumentException e) 
			{
				e.printStackTrace();
			} 
			catch (ClientProtocolException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}*/
			
			Log.d("GetDataThread", "status code: " +statusCode);
			
								/**********************/		
				
			synchronized(Map.class)
        	{
        		mGetDataThread = null;
        	}
		}
	}
/*	
	public int getData() throws IllegalArgumentException, ClientProtocolException, 
	HttpHostConnectException, IOException
	{
		LatLng center = mGoogleMap.getCameraPosition().target;
		double latitude = center.latitude;
		double longitude = center.longitude;
		
		VisibleRegion vr = mGoogleMap.getProjection().getVisibleRegion().
		double minLong = vr.latLngBounds.southwest.longitude;
		double maxLat = vr.latLngBounds.northeast.latitude;
		double maxLong = vr.latLngBounds.northeast.longitude;
		double minLat = vr.latLngBounds.southwest.latitude;
		
		float latitudeDelta = mGoogleMap. //from top edge to bottom edge
		float longitudeDelta = mMapView.getLongitudeSpan() / 1.0E6f; //from left edge to right edge
		
		Formatter formatter = new Formatter(Locale.US)
				.format("%s?lat=%f&lon=%f&lat_delta=%f&lon_delta=%f", 
				Constants.GET_COMM_RECORDS_ADDR, latitude, longitude, latitudeDelta, longitudeDelta);
		
		String requestUrl = formatter.toString(); //complete url string
		
		Log.d("Map", "getData()--> request Url: " +requestUrl);
		
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(requestUrl);
		
		HttpResponse response =httpClient.execute(httpGet);		
		HttpEntity resEntityGet = response.getEntity();  
		
		//reset
		mAnnotatedRecords.clear();
		
	    if (resEntityGet != null) 
	    {  
	    	String result = EntityUtils.toString(resEntityGet);
	    	
	    	//do something with the response
            Log.d("Map", "getData()--> " +result);
            
            //Convert String to JSON Object
            try 
            {
				JSONObject jsonObj = new JSONObject(result);
				
				JSONArray tokenList = jsonObj.getJSONArray("data");

				//if data contains annotated records, parse them in objects
				for(int i = 0; i < tokenList.length(); i++)
				{
					JSONObject oj = tokenList.getJSONObject(i);
					Log.d("Map", "getData()--> " +oj.toString());
					
					if(!oj.getString("user_data").equals(""))
					{
						JSONArray coords = oj.getJSONArray("geo_coord");
						
						//double normalizedPollValue = Record.normalizeValue(oj.getDouble("avg_pollution"));
						
						AnnotatedRecord annRecord = new AnnotatedRecord(oj.getDouble("id"),
								oj.getDouble("avg_pollution"), oj.getLong("timestamp")*1000,
								oj.getString("user_data"),coords.getDouble(1), coords.getDouble(0));
						
						mAnnotatedRecords.add(annRecord);
					}
				}
			} 
            catch (JSONException e) 
            {
				e.printStackTrace();
			} 
        }
	    
	    //4 - draw annotated records as overlay on map
	    //AnnotationOverlay annotationOverlay = new AnnotationOverlay();
	    //mMapView.getOverlays().add(annotationOverlay);
	    
	  
		//4 - draw annotated records as overlay on map
		for(int i = 0; i < mAnnotatedRecords.size(); i++)
		{			
			AnnotatedRecord annRec = mAnnotatedRecords.get(i);
			
			//confronto id del telefono con il corrispettivo campo sul record
			//if(annRec.mUniquePhoneId.equals(Constants.mUniquePhoneId))
			//{
				//se il record ha latitudine esplicitata
				if(annRec.mLat != 0)
				{
					
					
					AnnotationOverlay annotationOverlay = new AnnotationOverlay(mMarkerTrackIcon);
					annotationOverlay.addItem(new GeoPoint((int)(annRec.mLat * 1000000),
						(int)(annRec.mLon * 1000000)), ""  ,
						annRec.mUserData+"\n" +
						"A.Q.I.: " +Utils.reduceNumLength(String.valueOf(annRec.mAvgPoll))+"\n" +				
						"Time: " +Utils.fromTimestampToTime(annRec.mTimestamp)+ "\n" +
						"Date: " +Utils.fromTimestampToDayDate(annRec.mTimestamp));
					mMapView.getOverlays().add(annotationOverlay);
				}
			//}
		}
		
		//hide wait dialog
		if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
			mProgressDialog.dismiss();
			
		//5 - center map on last received annotation
		if(mAnnotatedRecords.size() > 0)
		{
			AnnotatedRecord annRec = mAnnotatedRecords.get(mAnnotatedRecords.size()-1);
			
			mMapController.setCenter(new GeoPoint((int)(annRec.mLat * 1000000),
				(int)(annRec.mLon * 1000000)));
		}
		else
		{
			//if no annotated record received, show message on dialog
			//Toast.makeText(getApplicationContext(), "There aren't community records in this area", Toast.LENGTH_LONG).show();		
			Log.d("getData()", "There aren't community records in this area");
		}
		
		//server response, status line
		StatusLine statusLine = response.getStatusLine();
		return statusLine.getStatusCode(); 
	}*/
	
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
    
    /********************** SHARE SECTION ***************************************************************/
    
	//get button references and set text on them
	private void getShareButtonsRef(Dialog dialog)
	{
		mValidFbSession = mFacebookManager.isSessionValid();
		mValidTwSession = Utils.getValidTwSession(getApplicationContext());// mTwitterManager.isSessionValid();	
		
		//if(mButtons[0] == null)
		//{
			mButtons[0] = (Button)dialog.findViewById(R.id.facebook_log_button);
			mButtons[0].setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0) 
				{
					//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
					int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
					
					//1 - is internet connection available? 
					boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
					
					//if user wants to upload only on wifi networks, connectivity[0] (network connectivity) must be true
					if(networkTypeIndex == 0)
					{
						if(connectivity[0])
							mConnectivityOn = true;
						else
							mConnectivityOn = false;
					}
					else //if user wants to upload both on wifi/mobile networks
						mConnectivityOn = connectivity[0] || connectivity[1];
					
					if(mValidFbSession)
					{
						mFacebookManager.clearCredentials();
					}
					else
					{
						if(mConnectivityOn)
						{
							mFacebookManager.authorizeFbUser();
						}
						else
						{
							noConnectivityDialog();
						}
					}
				}				
			});
		//}
		//if(mButtons[1] == null)
		//{
			mButtons[1] = (Button)dialog.findViewById(R.id.twitter_log_button);
			mButtons[1].setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View arg0) 
				{
					//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
					int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
					
					//1 - is internet connection available? 
					boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
					
					//if user wants to upload only on wifi networks, connectivity[0] (network connectivity) must be true
					if(networkTypeIndex == 0)
					{
						if(connectivity[0])
							mConnectivityOn = true;
						else
							mConnectivityOn = false;
					}
					else //if user wants to upload both on wifi/mobile networks
						mConnectivityOn = connectivity[0] || connectivity[1];
					
					if(mValidTwSession)
					{
						mTwitterManager.shutdown();
		                
		                mValidTwSession = false;
		                mButtons[1].setText(getResources().getString(R.string.login_twitter_text));
		                Utils.setValidTwSession(false, getApplicationContext());
		                Toast.makeText(Map.this, "unauthorized", Toast.LENGTH_SHORT).show();
					}
					else
					{
						if(mConnectivityOn)
						{
							mTwitterManager.initTwitter();
						}
						else
						{
							noConnectivityDialog();
						}
					}
				}				
			});
		//}
		
		Log.d("Share", "getShareButtonsRef()--> login facebook: " +mValidFbSession);
		Log.d("Share", "getShareButtonsRef()--> login twitter: " +mValidTwSession);
		
		//display right text (login/logout) on facebook button
		if(mValidFbSession)
			mButtons[0].setText(getResources().getString(R.string.logout_facebook_text));
		else
			mButtons[0].setText(getResources().getString(R.string.login_facebook_text));
		
		//display text (login/logout) on twitter button
		if(mValidTwSession)
			mButtons[1].setText(getResources().getString(R.string.logout_twitter_text));
		else
			mButtons[1].setText(getResources().getString(R.string.login_twitter_text));
	}
	
	private Handler mFacebookHandler = new Handler()
	{
    	@Override
    	public void handleMessage(Message msg)
    	{    		
			switch(msg.what)
    		{
				case Constants.LOGIN_COMPLETED:
					Log.d("Share", "FacebookHandler --> Login completed");
					mButtons[0].setText(getResources().getString(R.string.logout_facebook_text));
					mValidFbSession = true;				
					Utils.setValidFbSession(true, getApplicationContext());
				break;
				
				case Constants.LOGIN_CLOSED:
					Log.d("Share", "FacebookHandler --> Login closed");
					mButtons[0].setText(getResources().getString(R.string.login_facebook_text));
					mValidFbSession = false;	
					Utils.setValidFbSession(false, getApplicationContext());
				break;
				
				case Constants.LOGIN_CANCEL:
					Log.d("Share", "FacebookHandler --> Login cancel");
					mValidFbSession = false;
					Utils.setValidFbSession(false, getApplicationContext());
				break;
				
				case Constants.LOGIN_ERROR:
					Log.d("Share", "FacebookHandler --> Login error");
					mValidFbSession = false;
					Utils.setValidFbSession(false, getApplicationContext());
				break;
				
				case Constants.LOGIN_FACEBOOK_ERROR:
					Log.d("Share", "FacebookHandler --> Login facebook error");
					mValidFbSession = false;
					Utils.setValidFbSession(false, getApplicationContext());
				break;
    		}
    	}
	};

    /********************** INVOKED WHEN TwitterLogin RETURNS **********************************************************/
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (requestCode == 0) 
        {
            if (resultCode == RESULT_OK) 
            {
            	Log.d("Map", "Twitter auth RESULT OK");
            	
            	String oauthVerifier = intent.getExtras().getString(Constants.IEXTRA_OAUTH_VERIFIER);
            	final boolean result = mTwitterManager.authoriseNewUser(oauthVerifier);
            	
                new Handler().postDelayed(new Runnable()
                {
					@Override
					public void run() 
					{
						if(result)
						{
							mValidTwSession = true;
							mButtons[1].setText(getResources().getString(R.string.logout_twitter_text));	
							Utils.setValidTwSession(true, getApplicationContext());
						}
						else
						{
				            mValidTwSession = false;
				            mButtons[1].setText(getResources().getString(R.string.login_twitter_text));
						}
					}
                	
                }, 500);
            } 
            else if (resultCode == RESULT_CANCELED) 
            {
            	mValidTwSession = false;
            	mButtons[1].setText(getResources().getString(R.string.login_twitter_text));
            	Utils.setValidTwSession(false, getApplicationContext());
                Log.d("Map", "Twitter auth canceled.");
            }
    			
        }
    }
}
