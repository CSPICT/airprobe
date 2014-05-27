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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record.RecordMetaData;
import org.csp.everyaware.db.Track;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MyTracks extends Activity
{
	private DbManager mDbManager;
	private List<java.util.Map<String, String>> mTrackList;
	private ListView mListView;
	private MyTracksAdapter mMyTracksAdapter;
	private String[] from = {"formattedSessionId", "trackLength"};
	private int[] to = {R.id.sessionIdTv, R.id.trackLengthTv};
	private Toast mExitToast; //toast showed when user press back button
	private ProgressDialog mProgressDialog;
	
	private Handler mProgBarHandler;
	private ProgressBar mUplProgBar;
	private TextView mUplProgTv;
	
	private PowerManager mPowerMan; 
	private PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.my_tracks);		

        mPowerMan = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerMan.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUalwaysOn");
        
		mDbManager = DbManager.getInstance(getApplicationContext());
		mDbManager.openDb();
		
    	mUplProgBar = (ProgressBar)findViewById(R.id.uploadProgBar);   	
    	mUplProgBar.setMax(Utils.getTotalStoredRecCount(getApplicationContext()));
    	mUplProgBar.setProgress(Utils.getUploadedRecCount(getApplicationContext())); 	
    	mUplProgTv = (TextView)findViewById(R.id.uplProgTv);
    	mUplProgTv.setText("Uploaded: "+Utils.getUploadedRecCount(getApplicationContext())+"/"+Utils.getTotalStoredRecCount(getApplicationContext())+" on disk");
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();	
	}	    
    
    @Override
    public void onDestroy()
    {
    	Utils.paused = false;
    	super.onDestroy(); 
    	
    	//release partial wake lock
    	if(mWakeLock != null)
    		mWakeLock.release();
    }      
    
    @Override
    public void onPause()
    {
    	Utils.paused = true;
    	super.onPause();
    	
    	if(mListView != null)
    		mListView.setAdapter(null);
    	mMyTracksAdapter = null;
    	
    	if(mProgBarHandler != null)
    	{
    		mProgBarHandler.removeCallbacks(mUpdateBars);
    		mProgBarHandler.removeCallbacks(mUpdateAdapter);
    	}
    	
    }
    
	@Override
	public void onResume()
	{
		Utils.paused = false;
		super.onResume();	
		
		//acquire partial wake lock
		if(!mWakeLock.isHeld())
			mWakeLock.acquire();  
		
		if(mTrackList == null)
			mTrackList = mDbManager.loadAllTracks();
		
		if(mTrackList != null)
		{
			for(int i = 0; i < mTrackList.size(); i++)
				Log.d("MyTracks", "onResume()--> sessionId: "+mTrackList.get(i).get("sessionId")+" # recs: "+mTrackList.get(i).get("numOfRecs")+" # upl recs: "+mTrackList.get(i).get("numOfUploadedRecs"));
		}
		
		if((mTrackList != null)&&(mTrackList.size() > 0))
		{
			if(mMyTracksAdapter == null)
			{
				mMyTracksAdapter = new MyTracksAdapter(MyTracks.this, mTrackList, R.layout.my_tracks_list_item, from, to);
				mListView = (ListView)findViewById(R.id.listView1);
				mListView.setAdapter(mMyTracksAdapter);			
				mListView.setOnItemClickListener(new OnItemClickListener()
				{
					@Override
					public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) 
					{						
						HashMap<String, String> trackMap = (HashMap<String, String>)mTrackList.get(pos);
		
						//Utils.track = new Track(trackMap.get("sessionId"), Long.valueOf(trackMap.get("firstSysTs")), Long.valueOf(trackMap.get("firstBoxTs")),
						//		Long.valueOf(trackMap.get("lastSysTs")), Long.valueOf(trackMap.get("lastBoxTs")), Integer.valueOf(trackMap.get("numOfRecs")), Integer.valueOf(trackMap.get("numOfUploadedRecs")),
						//		null, null);
						
						Utils.track = mDbManager.loadTrackBySessionId(trackMap.get("sessionId"));
						
						if(view != null)
						{
							for(int i = 0; i < adapter.getChildCount(); i++)
								adapter.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
							
							//color actual selection
							view.setBackgroundColor(Color.parseColor("#88ffffff"));
							
							//save index of actual selection
							Utils.selectedTrackIndex = pos;
						}
					}				
				});
			}
			
			mMyTracksAdapter.notifyDataSetChanged();
			
			if((mProgressDialog != null)&&(mProgressDialog.isShowing()))
				mProgressDialog.dismiss();
		}
		
		if(mProgBarHandler == null)
			mProgBarHandler = new Handler();
		mProgBarHandler.postDelayed(mUpdateBars, 2000);
		mProgBarHandler.postDelayed(mUpdateAdapter, 30000);
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
	    
    	Intent intent = new Intent(MyTracks.this, Start.class);
    	startActivity(intent);
	    finish(); 
	}	   
	
    /****************** RUNNABLE THAT UPDATES UPLOAD PROGRESS BAR *****************************/
    
    private Runnable mUpdateBars = new Runnable()
    {
		@Override
		public void run() 
		{
			if(mUplProgBar != null)
				mUplProgBar.setProgress(Utils.getUploadedRecCount(getApplicationContext()));
			
			if(mUplProgTv != null)
			{
				if(Utils.uploadOn == Constants.INTERNET_OFF_INT)
					mUplProgTv.setText("Internet connection not available");
				else
					mUplProgTv.setText("Uploaded: "+Utils.getUploadedRecCount(getApplicationContext())+"/"+(Utils.getTotalStoredRecCount(getApplicationContext())+" on disk"));
			}
	
			mProgBarHandler.postDelayed(mUpdateBars, 2000);
		} 	
    };
    
    private Runnable mUpdateAdapter = new Runnable()
    {
		@Override
		public void run() 
		{
			mTrackList = mDbManager.loadAllTracks();
	    	if(mListView != null)
	    		mListView.setAdapter(null);
	    	mMyTracksAdapter = null;
	    	
	    	if((mTrackList != null)&&(mTrackList.size() > 0))
			{
				mMyTracksAdapter = new MyTracksAdapter(MyTracks.this, mTrackList, R.layout.my_tracks_list_item, from, to);
				mListView = (ListView)findViewById(R.id.listView1);
				mListView.setAdapter(mMyTracksAdapter);			
				mListView.setOnItemClickListener(new OnItemClickListener()
				{
					@Override
					public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) 
					{						
						HashMap<String, String> trackMap = (HashMap<String, String>)mTrackList.get(pos);
						Utils.track = mDbManager.loadTrackBySessionId(trackMap.get("sessionId"));
							
						if(view != null)
						{
							for(int i = 0; i < adapter.getChildCount(); i++)
								adapter.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
								
							//color actual selection
							view.setBackgroundColor(Color.parseColor("#88ffffff"));
								
							//save index of actual selection
							Utils.selectedTrackIndex = pos;
						}
					}				
				});					
				mMyTracksAdapter.notifyDataSetChanged();
				mProgBarHandler.postDelayed(mUpdateAdapter, 30000);
			}
		} 	
    };
    
	private class MyTracksAdapter extends SimpleAdapter
	{
		public MyTracksAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) 
		{
			super(context, data, resource, from, to);
		}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent)
		{
			if(convertView == null)
			{
				convertView = getLayoutInflater().inflate(R.layout.my_tracks_list_item, parent,false);

				HashMap<String, String> trackMap = (HashMap<String, String>)mTrackList.get(position);
				int numOfRecs = Integer.valueOf(trackMap.get("numOfRecs"));
				int numOfUploadedRecs = Integer.valueOf(trackMap.get("numOfUploadedRecs"));
				
				Log.d("MyTracksAdapter", "getView()--> # records: "+numOfRecs+" # uploaded records: "+numOfUploadedRecs);
				
				ImageView statusIv = (ImageView)convertView.findViewById(R.id.statusIv);
				if(numOfUploadedRecs == 0)
					statusIv.setImageResource(R.drawable.uploaded_none);
				else if(numOfUploadedRecs < numOfRecs)
					statusIv.setImageResource(R.drawable.uploaded_half);
				else if(numOfUploadedRecs == numOfRecs)
					statusIv.setImageResource(R.drawable.uploaded_all);
			}
			
			if(Utils.selectedTrackIndex == position)
				convertView.setBackgroundColor(Color.parseColor("#88ffffff"));
			else
				convertView.setBackgroundColor(Color.TRANSPARENT);
			
			return super.getView(position, convertView, parent);
		}
	};
	
    public void showProgressDialog(String msg, boolean cancelable)
    {
    	mProgressDialog = ProgressDialog.show(MyTracks.this, getResources().getString(R.string.app_name), msg, true, true);
    }
}
