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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.csp.everyaware.Constants;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

//registrato per due eventi: ACTION_STATE_CHANGED e ACTION_FOUND
public class BluetoothBroadcastReceiver extends BroadcastReceiver
{
	private Context mContext;
	private Handler mHandler;
	
	public BluetoothBroadcastReceiver(Context ctx, Handler handler)
	{
		mContext = ctx;
		mHandler = handler;
	}
	
	//sulla scoperta di un nuovo device ad opera di startDiscovery()
	public void onReceive(Context context, Intent intent)
	{
		//recuper il codice dell'intent
		String action = intent.getAction();
		
		Log.d("BBR", "action: " +action);
		
		if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
		{
			//il secondo campo specificato per getIntExtra è solo il valore da restituire in caso di errore
			int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            
			if (newState == BluetoothAdapter.STATE_TURNING_ON) 
            	Log.d("BBR", "onReceive()--> BluetoothAdapter STATE TURNING ON");
			
            if (newState == BluetoothAdapter.STATE_ON) 
            {           	
            	Log.d("BBR", "onReceive()--> BluetoothAdapter STATE ON");
            	
            	//chiudo la progress dialog 
    			Message msg = mHandler.obtainMessage(Constants.BT_ACTIVATED);
    			mHandler.sendMessage(msg);    			
            }
		}	
		
		//se è tale codice è ACTION_FOUND, recupero informazioni dall'intent
		if(BluetoothDevice.ACTION_FOUND.equals(action))
		{
			Log.d("BBR", "onReceive()--> BluetoothAdapter ACTION FOUND");
			
			//il campo EXTRA_DEVICE contiene un'istanza di BluetoothDevice; essa 
			//è l'istanza del device appena trovato
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			
			if(device == null)
				return;
			
			if(device.getName() != null)
			{				
				Log.d("BBR", "onReceive()--> device name: " +device.getName());
				
				if(device.getName().length() >= 9)
				{
					//se il nome del device inizia con "SensorBox" lo aggiungo all'array di
					//device compatibili con l'app
					if(Constants.ROOT_DEVICE_NAME.equals(device.getName().substring(0, 9)))
					{
						//invio il device alla Start tramite handler
						Message msg = mHandler.obtainMessage(Constants.DEVICE_DISCOVERED);
						Bundle bundle = new Bundle();
						
						bundle.putSerializable(Constants.DEVICE, 
								(Serializable) new BluetoothObject(device));
						
						msg.setData(bundle);
						mHandler.sendMessage(msg);
						
						Log.d("BBR", "onReceive()--> aggiunto device con nome: " +device.getName());
					}
				}
			}
			else
				Log.d("BBR", "device name null");
		}
		if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
		{
			Log.d("BBR", "action acl connected");
		}
		if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
		{
			Log.d("BBR", "action acl disconnected");
		}
		if(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action))
		{
			Log.d("BBR", "action acl disconnected requested");
		}
		if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
		{
			Log.d("BBR", "Action discovery started");
			
			Message msg = mHandler.obtainMessage(Constants.DISCOVERY_STARTED);
			mHandler.sendMessage(msg);
		}
		if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
		{
			Log.d("BBR", "Action discovery finished");
			
			Message msg = mHandler.obtainMessage(Constants.DISCOVERY_FINISHED);			
			mHandler.sendMessage(msg);
		}
	}
};
