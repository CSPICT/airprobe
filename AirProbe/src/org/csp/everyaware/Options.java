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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.csp.everyaware.db.DbManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class Options extends Activity 
{           
	//options variables
	private CharSequence[] options1; //ages for uploaded records
	private CharSequence[] options2; //store'n'forward frequencies
	private CharSequence[] options3; //history download on/off
	private CharSequence[] options4; //upload records only on wifi network or both wifi/mobile network
	private CharSequence[] options5; //switch on and use network provider for location data in addition to sensorbox/phone gps provider
	private CharSequence[] options6; //reset preferred user sensor box (useful to connect directly to it)
	private CharSequence[] options7; //force use of phone gps
	private CharSequence[] options8; //recover records
	
	private AlertDialog alert = null;
	private ProgressDialog mProgressDialog;
	
	private DbManager mDbManager;
	
    @Override  
    public void onCreate(Bundle icicle) 
    {  
        super.onCreate(icicle);  
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.options);
        
        mDbManager = DbManager.getInstance(getApplicationContext());
        mDbManager.openDb();
        
    	options1 = getResources().getStringArray(R.array.record_ages);
    	options2 = getResources().getStringArray(R.array.storeforw_frequencies);
    	options3 = getResources().getStringArray(R.array.download_history);
    	options4 = getResources().getStringArray(R.array.network_type);
        options5 = getResources().getStringArray(R.array.network_provider);
        
        if(!Utils.getPrefDeviceAddress(getApplicationContext()).equals(""))
        	options6 = new CharSequence[]{Utils.getPrefDeviceAddress(getApplicationContext()), "Clear preference"};
        else
        	options6 = new CharSequence[]{"No sensor box saved"};
        
        options7 = getResources().getStringArray(R.array.use_phone_gps);       
        options8 = getResources().getStringArray(R.array.recover_records);
        
    	ListView listView = (ListView)findViewById(R.id.option_list);

		//carico le voci e le subvoci (caption) - queste ultime in base ai dati appena caricati
        List<HashMap<String,?>> options = new LinkedList<HashMap<String,?>>();  
        options.add(createItem("Uploaded records max age", (String)options1[Utils.getRecordAgesIndex(getApplicationContext())]));  
        options.add(createItem("Store'n'Forward frequency", (String)options2[Utils.getStoreForwFreqIndex(getApplicationContext())]));   
        options.add(createItem("History data download (in Live Track)", (String)options3[Utils.getDownloadHistIndex(getApplicationContext())]));   
		options.add(createItem("Upload data on", (String)options4[Utils.getUploadNetworkTypeIndex(getApplicationContext())]));
		options.add(createItem("Enable network gps", (String)options5[Utils.getUseNetworkProviderIndex(getApplicationContext())]));
		options.add(createItem("Direct connect to sensor box", (String)options6[0]));
		options.add(createItem("Use phone GPS", (String)options7[Utils.getUsePhoneGpsIndex(getApplicationContext())]));
		options.add(createItem("Recovery mode for upload not working", (String)options8[0]));
		
		SimpleAdapter optionsAdapter = new SimpleAdapter(this, options, R.layout.list_complex,   
	            new String[] {Constants.ITEM_TITLE, Constants.ITEM_CAPTION }, 
	            new int[] { R.id.list_complex_title, R.id.list_complex_caption });  
		listView.setAdapter(optionsAdapter);

		listView.setOnItemClickListener(mOnClickListener);    
    }  
    
    public HashMap<String,?> createItem(String title, String caption) 
    {  
        HashMap<String,String> item = new HashMap<String,String>();  
        item.put(Constants.ITEM_TITLE, title);  
        item.put(Constants.ITEM_CAPTION, caption);  
        return item;  
    }  
    
    OnItemClickListener mOnClickListener = new OnItemClickListener()
    {

		@Override
		public void onItemClick(AdapterView<?> arg0, View view, int position,
				long id) 
		{
			TextView caption = (TextView)view.findViewById(R.id.list_complex_caption);
			
			switch (position)
			{
				//uploaded records age 
				case 0:
					showDialogUploadedRecordAges(caption);
				break;
				//store and forward activation frequency
				case 1:
					showDialogStoreForwFrequency(caption);
				break;
				//download history from sensor box sd card
				case 2:
					showDialogDownloadHist(caption);
				break;			
				//upload on network type on: only wifi or both wifi/mobile network
				case 3:
					showDialogUploadNetType(caption);
				break;
				//use network provider for gps data (in addition to sensor box/phone gps provider)
				case 4:
					showUseNetworkProvider(caption);
				break;
				//preferred sensor box saved clear?
				case 5:
					if(options6.length>1)
						showPreferredSensorBox(caption);
				break;
				//use phone gps
				case 6:
					showUsePhoneGps(caption);
				break;
				//recover record mode
				case 7:
					showRecoveryModeAlert(caption);
				break;
			}			
		}
    };
    
    private void showDialogUploadedRecordAges(final TextView tv) 
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Uploaded record age");

    	alt_bld.setSingleChoiceItems(options1, Utils.getRecordAgesIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setRecordAgesIndex(index, getApplicationContext());
    			tv.setText(options1[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showDialogStoreForwFrequency(final TextView tv) 
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Store'n'forward activation frequency");

    	alt_bld.setSingleChoiceItems(options2, Utils.getStoreForwFreqIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setStoreForwFreqIndex(index, getApplicationContext());
    			Utils.setStoreForwInterval(Constants.storeForwFreqs[index], getApplicationContext());
    			tv.setText(options2[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showDialogDownloadHist(final TextView tv) 
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("History download from sensor box");

    	alt_bld.setSingleChoiceItems(options3, Utils.getDownloadHistIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setDownloadHistIndex(index, getApplicationContext());
    			tv.setText(options3[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showDialogUploadNetType(final TextView tv)
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Upload on network type");

    	alt_bld.setSingleChoiceItems(options4, Utils.getUploadNetworkTypeIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setUploadNetworkTypeIndex(index, getApplicationContext());
    			tv.setText(options4[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();    	
    }
    
    private void showUseNetworkProvider(final TextView tv)
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Enable network gps provider");

    	alt_bld.setSingleChoiceItems(options5, Utils.getUseNetworkProviderIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setUseNetworkProviderIndex(index, getApplicationContext());
    			tv.setText(options5[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showPreferredSensorBox(final TextView tv)
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Connection to saved sensor box");

    	alt_bld.setSingleChoiceItems(options6, 0, new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			if(index == 1)
    			{
    				Utils.savePrefDeviceAddress("", getApplicationContext());
    				options6 = new CharSequence[]{"No sensor box saved"};
    				tv.setText(options6[0]);
    			}
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showUsePhoneGps(final TextView tv)
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Use phone Gps");

    	alt_bld.setSingleChoiceItems(options7, Utils.getUsePhoneGpsIndex(getApplicationContext()), new DialogInterface.OnClickListener() 
    	{
    		public void onClick(DialogInterface dialog, int index) 
    		{
    			Utils.setUsePhoneGpsIndex(index, getApplicationContext());
    			tv.setText(options7[index]);
    			alert.cancel();
    		}
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void showRecoveryModeAlert(final TextView tv)
    {
    	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
    	alt_bld.setTitle("Recovery mode alert");
    	alt_bld.setMessage(getResources().getString(R.string.recovery_mode_message));
    	
    	alt_bld.setPositiveButton("Ok", new OnClickListener()
    	{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.dismiss();
				
				new UpdateDbTask().execute();
			}    		
    	});
    	
    	alt_bld.setNegativeButton("Cancel", new OnClickListener()
    	{
			@Override
			public void onClick(DialogInterface dialog, int which) 
			{
				dialog.dismiss();
				
			}    		
    	});

    	alert = alt_bld.create();
    	alert.show();
    }
    
    private void createIndetProgressDialog()
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
    				//if((mProgressDialog != null)&&(!mProgressDialog.isShowing()))
    					mProgressDialog = ProgressDialog.show(Options.this, getResources().getString(R.string.app_name), "Updating records on db. Please wait.", true, false);
    			}
    		}
    	});
    }
    
    private class UpdateDbTask extends AsyncTask<Void, Void, Integer>
    {
    	@Override
    	protected void onPreExecute()
    	{
    		createIndetProgressDialog();
    	}
    	
		protected Integer doInBackground(Void... arg0) 
		{			
			return mDbManager.resetUploadedRecords();
		}
    	
		@Override
		protected void onPostExecute(Integer count)
		{
			if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
				mProgressDialog.dismiss();
		
			Utils.resetUploadedRecCount(getApplicationContext());
			Utils.setTotalStoredRecCount(getApplicationContext(), count);
			recordCountDialog(count);
		}
    }
    
    private void recordCountDialog(final int result)
    {
    	runOnUiThread(new Runnable() 
    	{
    		@Override
    		public void run() 
    		{
    			if(!isFinishing())
    			{
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Options.this);
					alertDialogBuilder.setTitle(getResources().getString(R.string.app_name));
			 
					alertDialogBuilder.setMessage("Number of affected records: "+result)
			
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
