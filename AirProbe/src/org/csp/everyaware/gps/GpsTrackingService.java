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

package org.csp.everyaware.gps;

import org.csp.everyaware.Constants;
import org.csp.everyaware.Utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**************************** SMARTPHONE GPS TRACKING SERVICE ****************************************/

public class GpsTrackingService extends Service
{
	private Handler mHandler;
	private boolean bugged;
	
	//access to geolocalization services
	private LocationManager mLocManager;

	//first invoked method
	@Override
	public void onCreate() 
	{
		super.onCreate();
		mHandler = new Handler();
	}
	
	//second invoked method
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{	
		//LISTENER
		mLocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
	
    	mLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 
	    		0, locationListener); 
    	
    	//if network provider is enabled IN ADDITION to sensor box/phone gps provider, register
    	//listener for it
    	try
    	{
    		if(Utils.getUseNetworkProviderIndex(getApplicationContext()) == 0)
    			mLocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    	}
    	catch(IllegalArgumentException e)
    	{
    		bugged = true;
    		e.printStackTrace();
    	}
    	finally
    	{
    		if(bugged)
    			Log.d("GpsTrackingService", "onStartCommand()--> CAN'T ENABLE NETWORK_PROVIDER: bugged Android!!!");
    	}

    	Utils.setGpsTrackingOn(true, getApplicationContext());
    	
    	mHandler.postDelayed(checkTrackingOnRunnable, 2000);
    	
		return super.onStartCommand(intent, flags, startId);
	}
	
	//third invoked method
	public void onStart(Intent intent, int startId) 
	{
		 super.onStart(intent, startId);
	}
	
	//stop the service
	@Override
	public void onDestroy() 
	{
		if(Utils.getGpsTrackingOn(getApplicationContext()))
		{
			mLocManager.removeUpdates(locationListener);
			Log.d("GpsTrackingService", "onDestroy()--> UNREGISTER LISTENER");			
		}
		super.onDestroy();		
	}
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}
	
	//verifica se la variabile Constants.GpsData.gpsTrackingOn è stata messa a false, in tal caso
	//deregistra il location listener
	private Runnable checkTrackingOnRunnable = new Runnable()
	{
		@Override
		public void run() 
		{			
			if(!Utils.getGpsTrackingOn(getApplicationContext()))
			{
				mLocManager.removeUpdates(locationListener);
				Log.d("GpsTrackingService", "onDestroy()--> UNREGISTER LISTENER");
			}
			else
				mHandler.postDelayed(checkTrackingOnRunnable, 2000);
		}		
	};
    
    /************************** LOCATION LISTENERS ******************************************/
    
    //consente la ricezione di notifiche dal LocationManager.
    public final LocationListener locationListener = new LocationListener()
    {
		//invocato quando la location varia
		@Override
		public void onLocationChanged(Location location) 
		{		
			
	        //String lat = String.valueOf(location.getLatitude());
	        //String lon = String.valueOf(location.getLongitude());
	        /*
	        Log.d("LocationListener", "::onLocationChanged()--> lat: " +lat+ ", lon: " +lon);
	        Log.d("LocationListener", "::onLocationChanged()--> altitude: " +location.getAltitude());
	        Log.d("LocationListener", "::onLocationChanged()--> provider: " +location.getProvider());
	        Log.d("LocationListener", "::onLocationChanged()--> time: " +location.getTime());
	        Log.d("LocationListener", "::onLocationChanged()--> accuracy (meters): " +location.getAccuracy());
	        Log.d("LocationListener", "**********************************************************************************");   
        		*/
        	//save location from phone gps provider
        	if(location.getProvider().equals(LocationManager.GPS_PROVIDER))
        	{
        		Utils.lastPhoneLocation = location;
        		Utils.lastPhoneLocation.setTime(System.currentTimeMillis());
            	//send msg to UI thread for gps fix received
            	sendBroadcast(new Intent(Constants.PHONE_GPS_ON));
        	}
        	//save location from network gps provider
        	else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER))
        	{
        		Utils.lastNetworkLocation = location;
        		Utils.lastNetworkLocation.setTime(System.currentTimeMillis());
               	//send msg to UI thread for network gps fix received
            	sendBroadcast(new Intent(Constants.NETWORK_GPS_ON));
        	}	
  	
        	mHandler.postDelayed(mSendPhoneGpsOffMsgRunnable, 750);
		}

		@Override
		public void onProviderDisabled(String provider) 
		{
			Log.d("gpsLocationListener", "GPS Provider disabled");	
		}

		@Override
		public void onProviderEnabled(String provider) 
		{
			Log.d("gpsLocationListener", "GPS Provider enabled");
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) 
		{
			switch (status) 
			{
				case LocationProvider.OUT_OF_SERVICE:
					Log.d("gpsLocationListener", "Status Changed: Out of Service");
				break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					Log.d("gpsLocationListener", "Status Changed: Temporarily Unavailable");
				break;
				case LocationProvider.AVAILABLE:
					Log.d("gpsLocationListener", "Status Changed: Available");
				break;
			}			
		}	
    };
    
    private Runnable mSendPhoneGpsOffMsgRunnable = new Runnable()
    {
		@Override
		public void run() 
		{
			sendBroadcast(new Intent(Constants.PHONE_GPS_OFF));
		}
    	
    };
}
