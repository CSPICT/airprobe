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

package org.csp.everyaware.offline;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.csp.everyaware.ColorHelper;
import org.csp.everyaware.Constants;
import org.csp.everyaware.ExtendedLatLng;
import org.csp.everyaware.KmlParser;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.bluetooth.BluetoothBroadcastReceiver;
import org.csp.everyaware.bluetooth.BluetoothManager;
import org.csp.everyaware.db.AnnotatedRecord;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.MapCluster;
import org.csp.everyaware.db.MarkerRecord;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.Track;
import org.csp.everyaware.internet.FacebookManager;
import org.csp.everyaware.internet.StoreAndForwardService;
import org.csp.everyaware.internet.TwitterManager;
import org.csp.everyaware.tabactivities.Graph;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;

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
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Map extends Activity
{
	private MapView mMapView;
	private GoogleMap mGoogleMap;
	private MyLocationSource mLocationSource;
	
	private Handler mHandler;
	private DbManager mDbManager;
	
	private List<Record>mRecords = new ArrayList<Record>();

	//opened track
	private Track mTrack;
	
	//track length button
	private Button mTrackLengthBtn;
	
	//zoom control custom buttons
	private Button mZoomInBtn, mZoomOutBtn;
	private float mZoom = 15f;
	
	//insert ann button
	private Button mInsertAnnBtn;

	//share button (log on facebook/twitter)
	private Button mShareBtn;
	
	//zoom controls (they can show/hide on map)
	private LinearLayout mZoomControls;
	private long mOnMapClickTs;
	
	//status icons
	private ImageView mGpsStatus;
	private ImageView mInterUplStatus;
	
	//camera tracking button and boolean flag
	private Button mFollowCamBtn;
	private boolean mCameraTrackOn = true;

	//get bc levels around user from server
	private Button mGetBcLevelsBtn;
	//black carbon legend
	private LinearLayout mSpectrum;
	
	//array of points of path showed on map
	private List<ExtendedLatLng>mLatLngPoints;
	
	private ProgressDialog mCancelDialog;
	private ProgressDialog mProgressDialog;
	private ProgressDialog mProgressDialog2;
	
	private boolean mInitializedView = false;
	private boolean mConnectivityOn;

	//private GetDataThread mGetDataThread;
	
	private double avg_poll_min = 0;
	private int poll_mult = 35;
	private int poll_base = 25;
	
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
	
	private KmlParser mKmlParser; //parse server response and returns a list of MapCluster(s)
	private List<MapCluster>mMapClusters; //black carbons level contained in the screen
	private List<Polygon>mMapPolygons;
	
	private Button mToBeHideBtn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		Log.d("Map", "******************************onCreate()******************************");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.map_container);		
		
        mToBeHideBtn = (Button)findViewById(R.id.startStopBtn);
        mToBeHideBtn.setVisibility(View.GONE);
        
        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
		
		Utils.setStep(Constants.TRACK_MAP, getApplicationContext());
		
		mDbManager = DbManager.getInstance(getApplicationContext());
		mDbManager.openDb();
	
		//it will contain  displayed latlng points
		mLatLngPoints = new ArrayList<ExtendedLatLng>();

		//istantiate custom implementation of LocationSource interface
		mLocationSource = new MyLocationSource();
		
        //google map initialization
        setUpMapIfNeeded(savedInstanceState);
		
		//obtaining references to buttons and defining listeners
		getButtonRefs();
		
		mHandler = new Handler();
		
		mKmlParser = new KmlParser();
		
		//starting store'n'forward service and saving reference to it
		Intent serviceIntent = new Intent(getApplicationContext(), StoreAndForwardService.class);
		Utils.storeForwServIntent = serviceIntent;
		startService(serviceIntent);
		
		//set appropriate colors in all seven black carbon levels under black carbon text box
		setBcLevelColors();
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
		Log.d("Map", "******************************onStop()******************************");
	}	    
    
    @Override
    public void onDestroy()
    {
    	Utils.paused = false;
    	
    	mMapView.onDestroy();
    	super.onDestroy(); 
    	Log.d("Map", "******************************onDestroy()***************************");
    	
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release();
    }      
    
    @Override
    public void onPause()
    {
    	Utils.paused = true;
    	
    	mLocationSource.removeLocUpdates();
    	mMapView.onPause();
    	
    	Log.d("Map", "******************************onPause()*****************************");
    	
    	if(mServiceReceiver != null)
    		//unregister receiver for messages from store'n'forward service
    		unregisterReceiver(mServiceReceiver);
    	
    	if(mGpsServiceReceiver != null)
    		//unregister receiver for messages from gps tracking service
    		unregisterReceiver(mGpsServiceReceiver);
 
		//remove polygon heat map from map and clear array of polygons
		if(mMapPolygons != null)
		{
			for(int i = 0; i < mMapPolygons.size(); i++)
				mMapPolygons.get(i).remove();
			mMapPolygons.clear();
		}
		
    	super.onPause();
    }
    
	@Override
	public void onResume()
	{
		Utils.paused = false;
		super.onResume();
    	mMapView.onResume();
    	
		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
    	mLocationSource.registerLocUpdates();   
    	
		if(Utils.uploadOn == Constants.INTERNET_ON_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.internet_on);
		else if(Utils.uploadOn == Constants.INTERNET_OFF_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.internet_off);
		else if(Utils.uploadOn == Constants.UPLOAD_ON_INT)
			mInterUplStatus.setBackgroundResource(R.drawable.upload);
		
    	if(mCameraTrackOn)
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_pressed);
    	else
    		mFollowCamBtn.setBackgroundResource(R.drawable.follow_camera_not_pressed);
    	
		//register receiver for messages from store'n'forward service
		IntentFilter internetOnFilter = new IntentFilter(Constants.INTERNET_ON);
		registerReceiver(mServiceReceiver, internetOnFilter);
		IntentFilter internetOffFilter = new IntentFilter(Constants.INTERNET_OFF);
		registerReceiver(mServiceReceiver, internetOffFilter);
		IntentFilter uploadOnFilter = new IntentFilter(Constants.UPLOAD_ON);
		registerReceiver(mServiceReceiver, uploadOnFilter);
		IntentFilter uploadOffFilter = new IntentFilter(Constants.UPLOAD_OFF);
		registerReceiver(mServiceReceiver, uploadOffFilter);
		
		//register receiver for messages from gps tracking service
		IntentFilter phoneGpsOnFilter = new IntentFilter(Constants.PHONE_GPS_ON);
		registerReceiver(mGpsServiceReceiver, phoneGpsOnFilter);
		IntentFilter networkGpsOnFilter = new IntentFilter(Constants.NETWORK_GPS_ON);
		registerReceiver(mGpsServiceReceiver, networkGpsOnFilter);
		IntentFilter phoneGpsOffFilter = new IntentFilter(Constants.PHONE_GPS_OFF);
		registerReceiver(mGpsServiceReceiver, phoneGpsOffFilter);
		
		//get selected track and draw it on map
		mTrack = Utils.track;
		
		if(mTrack != null)
		{
			Log.d("Map", "onCreate()--> shown session id: " +mTrack.mSessionId);
			
			if(mGoogleMap != null)
				mGoogleMap.clear();	
			
			int divider = 1;
			
			long trackLength = mTrack.mNumOfRecords;
			
			Log.d("Map", "onResume()--> track length: "+trackLength);
			
			if(trackLength > 1800)
				divider = 2;
			if(trackLength > 3600)
				divider = 4;
			if(trackLength > 7200)
				divider = 8;
			if(trackLength > 14400)
				divider = 16;
			
			mLatLngPoints = mDbManager.loadLatLngPointsBySessionId(mTrack.mSessionId, divider);
			if(mLatLngPoints != null)
			{
				int size = mLatLngPoints.size();
				
				if(size > 0)
				{
		       		calcMinAvgPollValue();
					drawPath();			
					mCameraTrackOn = false;
					if(mGoogleMap != null)
					try
					{
						mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mLatLngPoints.get(size-1).mLatLng));
					}
					catch(NullPointerException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		if(mMapPolygons == null)
			mMapPolygons = new ArrayList<Polygon>();
	
		int color;
		
		//draw map cluster on the map
		if((mMapClusters != null)&&(mMapClusters.size() > 0))
		{
			for(int i = 0; i < mMapClusters.size(); i++)
			{
				MapCluster mapCluster = mMapClusters.get(i);
				
				if(mapCluster.mBcLevel != 0)
				{
					if((mapCluster.mBcLevel > 0)&&(mapCluster.mBcLevel <= 10))
						color = ColorHelper.numberToColor(mapCluster.mBcLevel * BC_MULTIPLIER);
					else
						color = ColorHelper.numberToColor(100);
					
					mMapPolygons.add(mGoogleMap.addPolygon(new PolygonOptions()
    	        		.add(new LatLng(mapCluster.mMinLat, mapCluster.mMinLon), new LatLng(mapCluster.mMinLat, mapCluster.mMaxLon),
    	        				new LatLng(mapCluster.mMaxLat, mapCluster.mMaxLon), new LatLng(mapCluster.mMaxLat, mapCluster.mMinLon))
    	        		.strokeColor(Color.TRANSPARENT)
    	        		.fillColor(Color.parseColor("#66" + String.format("%06X", 0xFFFFFF & color)))));
				}
			}
		}	
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
	
    /***************************** INIT GOOGLE MAP **********************************************/
	
    private void setUpMap() 
    {
    	mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    	mGoogleMap.getUiSettings().setCompassEnabled(false);
    	mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
    	  
    	//assign istance of MyLocationSource to google maps and activate it
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
    	
    	mGoogleMap.setOnMapLongClickListener(new OnMapLongClickListener()
    	{
			@Override
			public void onMapLongClick(LatLng latLng) 
			{
				double minDistance = 0;
				int index = -1;
				
				Log.d("Map", "onMapLongClick()--> lat lng: " +latLng.latitude+", "+latLng.longitude);
				
				//calculate bounding box around tapped point (tapped point is the center of BB)
				LatLng topLeft = new LatLng(latLng.latitude + 0.01, latLng.longitude - 0.01);				 
				LatLng bottomRight = new LatLng(latLng.latitude - 0.01, latLng.longitude + 0.01);
				
				//calculate intersection between BB and path and the point of path inside the intersection that
				//is nearer to tapped point
				//output of the cycle is the index of path point nearest to tapped point
				for(int i = 0; i < mLatLngPoints.size(); i++)
				{
					ExtendedLatLng extLatLng = mLatLngPoints.get(i);
					
					//if actual path point is inside BB, calculate distance between tapped point and path point and save it 
					if((extLatLng.mLatLng.latitude <= topLeft.latitude)&&(extLatLng.mLatLng.latitude >= bottomRight.latitude))
					{
						if((extLatLng.mLatLng.longitude >= topLeft.longitude)&&(extLatLng.mLatLng.longitude <= bottomRight.longitude))
						{
							double latDiff = Math.abs(extLatLng.mLatLng.latitude - latLng.latitude);							
							double lonDiff = Math.abs(extLatLng.mLatLng.longitude - latLng.longitude);
							double distance = Math.sqrt(latDiff*latDiff+lonDiff*lonDiff);
							
							if(i == 0)
							{
								minDistance = distance;
								index = 0;
							}
							else if(distance < minDistance)
							{
								minDistance = distance;
								index = i;
							}
						}
					}
				}
				
		        //if an index is present, it is the index of the path point inside BB nearest to tapped point
		        if(index > -1)
		        {
		        	ExtendedLatLng nearestPathLatLng = mLatLngPoints.get(index);
		            insertAnnDialog(nearestPathLatLng);
		        }
		        else
		        	Toast.makeText(getApplicationContext(), "Path not found", Toast.LENGTH_LONG).show();
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
	
    /********************************* PERSONALIZED LOCATION SOURCE ******************************************************/
    
    //to draw user current position on map
    private class MyLocationSource implements LocationListener
    {
    	//private OnLocationChangedListener locChangeListener; //interface of LocationSource
    	private LocationManager locManager;
    	
    	public MyLocationSource()
    	{
    		locManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    	}
    	
    	public void registerLocUpdates()
    	{
			//register for location updates by gps and network providers
	    	locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	    	
	    	try
	    	{
	    		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
	    	}
	    	catch(IllegalArgumentException e)
	    	{
	    		e.printStackTrace();
	    	}
    	}
    	
    	public void removeLocUpdates()
    	{
    		//remove location updates (all)
			locManager.removeUpdates(this);
    	}
    	
		@Override
		public void onProviderDisabled(String provider) 
		{
			
		}

		@Override
		public void onProviderEnabled(String provider) 
		{
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) 
		{
			
		}
		
		//method of LocationSource invoked on location update
		@Override
		public void onLocationChanged(Location location) 
		{
			try
			{
				//center camera on last location update
				if(mCameraTrackOn)
					mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
			}
			catch(NullPointerException e)
			{
				e.printStackTrace();
			}
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

		mTrackLengthBtn.setVisibility(View.GONE);
		mInsertAnnBtn.setVisibility(View.GONE);
		
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
		
		mFollowCamBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0) 
			{
				//mCameraTrackOn = !mCameraTrackOn;
				setCameraTracking();
				
				if(mCameraTrackOn)
				{
					try
					{
						if(Utils.lastPhoneLocation != null)
							mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(Utils.lastPhoneLocation.getLatitude(), Utils.lastPhoneLocation.getLongitude())));
						else if(Utils.lastNetworkLocation != null)
							mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(Utils.lastNetworkLocation.getLatitude(), Utils.lastNetworkLocation.getLongitude())));
					}
					catch(NullPointerException e)
					{
						e.printStackTrace();
					}
				}
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
		
		//status icons references
		mGpsStatus = (ImageView)findViewById(R.id.gpsStatusIv);
		mInterUplStatus = (ImageView)findViewById(R.id.interUplStatusIv);

		//gps status icon initialization
		mGpsStatus.setBackgroundResource(R.drawable.gps_off);
		
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
		
		//network status icon initialization
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
		
		//button to get from server black carbon levels around user
		mGetBcLevelsBtn = (Button)findViewById(R.id.getBcLevelsBtn);
		mGetBcLevelsBtn.setVisibility(View.VISIBLE);
		mGetBcLevelsBtn.setOnClickListener(mGetBcLevelsOnClickListener);
		
		//bcLayout.addView(mGetBcLevelsBtn);
		mSpectrum = (LinearLayout)findViewById(R.id.spectrumLinearLayout);
		mSpectrum.setVisibility(View.VISIBLE);
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
	
	/****************** DISEGNA PATH ****************************************************/
	
	//draw path from a list of points
	public void drawPath()
	{
		ExtendedLatLng newLatLng = null; 
		ExtendedLatLng precLatLng = null;
		int color = Color.DKGRAY;
		
		try
		{
			Log.d("Map", "polyline count: " +mLatLngPoints.size());
			
			for (int i = 0; i < mLatLngPoints.size(); i++) 
	        {
				newLatLng = mLatLngPoints.get(i);
				color = Color.DKGRAY; //color for a segment without bc value
	            
				//10 is upper limit in scale, so a black carbon of 10 (multiplied by 10) must be drawn in dark red
	    	    if((newLatLng.mBc > 0)&&(newLatLng.mBc <= 10))
	    	    	color = ColorHelper.numberToColor(newLatLng.mBc * BC_MULTIPLIER);
	    	    else
	    	    	color = ColorHelper.numberToColor(100);
	    	    
	    	    //draw segment with appropriate color (dark grey if no bc value is present) 
	    	    if(precLatLng != null)
	    	    	mGoogleMap.addPolyline(new PolylineOptions().add(newLatLng.mLatLng).add(precLatLng.mLatLng).width(5).color(color));
	    	    
	            String annotation = newLatLng.mUserAnn;
	            
	            if(!annotation.equals(""))
	            {
	            	BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.annotation_marker);
		            mGoogleMap.addMarker(new MarkerOptions()
	        			.position(newLatLng.mLatLng)
	        			.title("AQI: "+newLatLng.mBc)
	        			.snippet("Annotation: " +annotation)
	        			.icon(icon)
	        			.anchor(0.25f, 1f)); //Map Markers are 'anchored' by default to the middle of the bottom of layout (i.e., anchor(0.5,1)).
	            }

	            precLatLng = newLatLng;
	        }			
			
			//center camera on last trackn position
			//if((mTrackMode)&&(mCameraTrackOn))
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
	
    /****************** CALCULATE MINS VALUE FOR POLLUTANTS IN LOADED HISTORY *******************/
    
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
	
    
    /******************************* DIALOGS ****************************************************/
    
    public void createProgressDialog(String msg, boolean cancelable)
    {
    	mProgressDialog = ProgressDialog.show(Map.this, getResources().getString(R.string.app_name), msg, true, true);
    }
    
    public void createProgressDialog2(String msg, boolean cancelable)
    {
    	mProgressDialog2 = new ProgressDialog(Map.this);
    	mProgressDialog2.setTitle(getResources().getString(R.string.app_name));
    	mProgressDialog2.setMessage(msg);
    	mProgressDialog2.setIndeterminate(true);
    	mProgressDialog2.setCancelable(true);
    	//mProgressDialog2 = ProgressDialog.show(Map.this, getResources().getString(R.string.app_name), msg, true, true);
    	mProgressDialog2.setOnShowListener(new OnShowListener()
    	{
			@Override
			public void onShow(DialogInterface arg0) 
			{
				Log.d("OnShowListener", "onShow()");
				
				mHandler.postDelayed(new Runnable()
				{
					@Override
					public void run() 
					{
						//clear precedent heat map
						if(mMapPolygons != null)
						{
							for(int i = 0; i < mMapPolygons.size(); i++)
								mMapPolygons.get(i).remove();
							
							mMapPolygons.clear();
						}
						
						if(mMapClusters != null)
							mMapClusters.clear();
						
						//update information about map zoom level
						mZoom = mGoogleMap.getCameraPosition().zoom;

			    		//get viewport min/max coordinates
			    		VisibleRegion vr = mGoogleMap.getProjection().getVisibleRegion();
			    		double minLong = vr.latLngBounds.southwest.longitude;
			    		double maxLat = vr.latLngBounds.northeast.latitude;
			    		double maxLong = vr.latLngBounds.northeast.longitude;
			    		double minLat = vr.latLngBounds.southwest.latitude;
			    		
			    		String requestUrl = Constants.GET_BC_LEVELS_ADDR+(Math.round(mZoom)+1)+"&bbox="+minLong+","+minLat+","+maxLong+","+maxLat; //complete url string		
			    		Log.d("Map", "getData()--> request Url: " +requestUrl);
			    		
						new GetDataTask().execute(requestUrl);
						/*
						mGetDataThread = new GetDataThread();
						mGetDataThread.run();*/
					}					
				}, 500);

			} 		
    	});
    	mProgressDialog2.show();
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
   
    
    public HashMap<String,?> createItem(String title, String caption) 
    {  
        HashMap<String,String> item = new HashMap<String,String>();  
        item.put(Constants.ITEM_TITLE, title);  
        item.put(Constants.ITEM_CAPTION, caption);  
        return item;  
    }  
	
    /*************************** OPTION MENU ***************************************************/
    
	public boolean onCreateOptionsMenu(Menu menu)
	{		
		String[] menuItems = getResources().getStringArray(R.array.map_optionmenu);
		
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
    	
    	//stop store'n'forward service
    	if(Utils.storeForwServIntent != null)
    		stopService(Utils.storeForwServIntent);
    	
    	//clear shared prefs
    	Utils.deleteSharedPrefs(getApplicationContext());
    	
	    Utils.track = null;
	    Utils.selectedTrackIndex = -1;
	    
    	Intent intent = new Intent(Map.this, Start.class);
    	startActivity(intent);
    	finish(); 
    }
    
    /******************* RECEIVE MESSAGES FROM GPS TRACKING SERVICE ***********************************/
    
    private BroadcastReceiver mGpsServiceReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
        	String action = intent.getAction();

            if (action.equals(Constants.PHONE_GPS_ON)) 
            {
            	Log.d("GpsServiceReceiver", "onReceive()--> Phone Gps ON");
            	mGpsStatus.setBackgroundResource(R.drawable.gps_on_phone);     	
            }
            if (action.equals(Constants.NETWORK_GPS_ON)) 
            {
            	Log.d("GpsServiceReceiver", "onReceive()--> Network Gps ON");
            	mGpsStatus.setBackgroundResource(R.drawable.gps_on_network);

            }       
            if (action.equals(Constants.PHONE_GPS_OFF)) 
            {
            	Log.d("GpsServiceReceiver", "onReceive()--> Phone Gps OFF");
            	mGpsStatus.setBackgroundResource(R.drawable.gps_off);

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
        
    
	/********************** CALLS getData() FUNCTION *************************************************/
	
    private class GetDataTask extends AsyncTask<String, Void, List<MapCluster>>
    {		
    	@Override
    	protected void onPreExecute()
    	{

    	}
    	
    	@Override
    	protected void onPostExecute(List<MapCluster> param)
    	{
    		int color;
    		
    		//draw map cluster on the map
    		if((mMapClusters != null)&&(mMapClusters.size() > 0))
    		{
    			for(int i = 0; i < mMapClusters.size(); i++)
    			{
    				MapCluster mapCluster = mMapClusters.get(i);
    				
    				if(mapCluster.mBcLevel != 0)
    				{
    					Log.d("GetDataTask", "onPostExecute()--> pos: "+i+ " bc level: "+mapCluster.mBcLevel+" coords: " +mapCluster.mMinLat+", "+mapCluster.mMinLon+", "+mapCluster.mMaxLat+", "+mapCluster.mMaxLon);
    				
    					if((mapCluster.mBcLevel > 0)&&(mapCluster.mBcLevel <= 10))
    						color = ColorHelper.numberToColor(mapCluster.mBcLevel * BC_MULTIPLIER);
    					else
    						color = ColorHelper.numberToColor(100);
    					
    					mMapPolygons.add(mGoogleMap.addPolygon(new PolygonOptions()
	    	        		.add(new LatLng(mapCluster.mMinLat, mapCluster.mMinLon), new LatLng(mapCluster.mMinLat, mapCluster.mMaxLon),
	    	        				new LatLng(mapCluster.mMaxLat, mapCluster.mMaxLon), new LatLng(mapCluster.mMaxLat, mapCluster.mMinLon))
	    	        		.strokeColor(Color.TRANSPARENT)
	    	        		.fillColor(Color.parseColor("#66" + String.format("%06X", 0xFFFFFF & color)))));
    				}
    			}
    		}	
    		
    		if((mProgressDialog2 != null)&&(mProgressDialog2.isShowing()))
    			mProgressDialog2.dismiss();
    		
    		runOnUiThread(new Runnable()
    		{
				@Override
				public void run() 
				{
					int clusterNum = 0;
					if(mMapClusters != null)
						clusterNum = mMapClusters.size();
					
					Toast.makeText(Map.this, "Found "+clusterNum+ " black carbon measures", Toast.LENGTH_LONG).show();
					
				}
    			
    		});
    	}
    	
		@Override
		protected List<MapCluster> doInBackground(String... params) 
		{
			int statusCode = -1;
			InputStream is = null;
			
			try 
			{
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(params[0]);		
				HttpResponse response = httpClient.execute(httpGet);
			
				Log.d("GetDataTask", "doInBackground()--> status line: " + response.getStatusLine());
				
				//if server response is 'HTTP 200 OK'
				if(response.getStatusLine().getStatusCode() == Constants.STATUS_OK)
				{
				    if (response.getEntity() != null) 
				    {  
				    	if(mKmlParser == null)
				    		mKmlParser = new KmlParser();
			
				    	//get InputStream from response entity
				    	is = response.getEntity().getContent();

				    	mMapClusters = mKmlParser.parseKml(is);  
				    }
				}
				else
				{
					if((mProgressDialog2 != null)&&(mProgressDialog2.isShowing()))
		    			mProgressDialog2.dismiss();
					
					Toast.makeText(Map.this, response.getStatusLine().toString(), Toast.LENGTH_LONG).show();
				}
				    	
			}
			//this exception is invoked when internet connection is up but traffic is not allowed
			//(for example, for networks that needs login but user is not logged)
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
			}
	    	finally
	    	{
		    	try 
		        {
		    		if(is != null)
		    			is.close();
		        } 
		        catch (IOException e) 
		        {
		        	e.printStackTrace();
		        }
	    	}
			
			Log.d("GetDataTask", "run()--> status code: " +statusCode);
			
			return mMapClusters;
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

    private void insertAnnDialog(final ExtendedLatLng annLatLng)
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
				double recordId = annLatLng.mRecordId;
								
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
				    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
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
    
    /********************* GET DATA FROM SERVER (BC LEVELS AROUND USER CURRENT POSITION) ******************************/
    
    //on click listener of get black carbon levels around user from server
    private OnClickListener mGetBcLevelsOnClickListener = new OnClickListener()
    {
		@Override
		public void onClick(View arg0) 
		{		
			//VERIFY INTERNET CONNECTION 
			
			//read network type index on which upload data is allowed: 0 - only wifi; 1 - both wifi and mobile
			//int networkTypeIndex = Utils.getUploadNetworkTypeIndex(getApplicationContext());
			
			//1 - is internet connection available? 
			boolean[] connectivity = Utils.haveNetworkConnection(getApplicationContext());
			
			/*
			//if user wants to download only on wifi networks, connectivity[0] (network connectivity) must be true
			if(networkTypeIndex == 0)
			{
				if(connectivity[0])
					mConnectivityOn = true;
				else
					mConnectivityOn = false;
			}
			else //if user wants to upload both on wifi/mobile networks
				mConnectivityOn = connectivity[0] || connectivity[1];
			*/
			
			//almost one type of connection (network, mobile) must be available
			mConnectivityOn = connectivity[0] || connectivity[1];
			
			//if internet connection is not available, inform user and return
			if(!mConnectivityOn)
			{
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.alert_dialog_no_internet), Toast.LENGTH_LONG).show();
				return;
			}
			
			//DOWNLOAD DATA
			new Handler().post(new Runnable(){

				@Override
				public void run() 
				{
					createProgressDialog2(getResources().getString(R.string.progress_dialog_please_wait), true);	
				}
				
			});
		}   	
    };
}
