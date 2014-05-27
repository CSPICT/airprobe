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

package org.csp.everyaware.internet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.csp.everyaware.Constants;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.facebooksdk.AsyncFacebookRunner;
import org.csp.everyaware.facebooksdk.AsyncFacebookRunner.RequestListener;
import org.csp.everyaware.facebooksdk.DialogError;
import org.csp.everyaware.facebooksdk.Facebook;
import org.csp.everyaware.facebooksdk.Facebook.DialogListener;
import org.csp.everyaware.facebooksdk.FacebookError;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FacebookManager 
{
	private static FacebookManager mFbMan;
	private Activity mActivity;
    private Facebook mFacebook;
    private Handler mFacebookHandler; //to send msg to Share activity
    
    //msg posted on facebook wall
    private String mMessage;
    
	 // Your Facebook Application ID must be set before running this example
    // See http://www.facebook.com/developers/createapp.php
    public static final String APP_ID = "140238296120716"; //mia signature
    
	public static FacebookManager getInstance(Activity activity, Handler fbHandler)
	{
		if(mFbMan == null)
			mFbMan = new FacebookManager(activity, fbHandler);
		return mFbMan;
	}
	
	private FacebookManager(Activity activity, Handler fbHandler)
	{
		mActivity = activity;
		initFacebook();
		mFacebookHandler = fbHandler;
	}
	
	//call this only if internet connection is available
	private void initFacebook()
	{
		mFacebook = new Facebook(APP_ID);
	}
	
	//send everyaware message on user's facebook wall
	public void postMessageOnWall(Record record)
	{			
	    composeMessage(record);
		
		if(mFacebook.isSessionValid())
		{
			Bundle parameters = new Bundle();
			
			String link = "http://www.everyaware.eu/";
			
			parameters.putString(Facebook.TOKEN, mFacebook.getAccessToken());	
			
			parameters.putString("link", link); //link box
			parameters.putString("message", mMessage); //message above link
			parameters.putString("description", mMessage); //message IN link
			
			//works
			//parameters.putString("picture", "http://lib.store.yahoo.net/lib/yhst-17155638221985/img-twitter.gif"); //used for testing
			
			//doesn't work
			//parameters.putString("icon", "http://photos-c.ak.fbcdn.net/photos-ak-snc1/v27562/74/174829003346/app_2_174829003346_2760.gif"); //used for testing

			Log.d("FacebookManager", "postMessageOnWall()--> starting message upload");
			
			try
			{
				AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(mFacebook);
				mAsyncRunner.request("me/feed", parameters, "POST", new MyRequestListener(), null);				
			}
			catch(Exception e)
			{
				e.printStackTrace();
				Log.d("FacebookManager", "postMessageOnWall()--> Error during upload");
			}
		}
		else
			authorizeFbUser();
	}
	
	//do logout of user from facebook
	public void clearCredentials()
	{
		try
		{
			String res = mFacebook.logout(mActivity);
			
			Log.d("FacebookManager", "result: "+res);
			
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_CLOSED);
	        mFacebookHandler.sendMessage(msg);
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}	
	
	//compose string message that will be posted on wall
	private void composeMessage(Record record)
	{		
		//add user annotation to msg
		mMessage = "\"" +record.mUserData1+ "\"\n\n";
		
		//average pollution
		mMessage += "Average pollution: " +record.calcAvgPoll()+ "\n";
		
		//add gps coordinates to msg
		if(record.mValues[0] != 0)
			mMessage += "GPS Coordinates: lat " +record.mValues[0]+"°, lon "+record.mValues[1]+"°\n";
				
		//format timestamp into date and add to msg
		String dateString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(record.mSysTimestamp));	
		mMessage += "Time: " +dateString+ "\n";
		
		mMessage += "\n\n";
		
		mMessage += "Sent with AirProbe for Android - http://www.everyaware.eu/\n";
	}
	
	//user login on facebook
	public void authorizeFbUser()
	{
		if(!mFacebook.isSessionValid())
		{
			Log.d("FacebookManager", "authorizeFbUser()--> session not valid yet");
			mFacebook.authorize(mActivity, new String[] {"email", "read_stream", "publish_stream"}, 
					new MyDialogListener());
		}
		else
		{
			Log.d("FacebookManager", "authorizeFbUser()--> session still valid");
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_COMPLETED);
	        mFacebookHandler.sendMessage(msg);
		}
	}
	
	//is user login session valid?
	public boolean isSessionValid()
	{
		if((mFacebook!=null)&&(mFacebook.isSessionValid()))
		{
			return true;
		}
		return false;
	}
	
	/**************************** FACEBOOK LISTENERS **********************************************/
	
	//events from sending message to facebook server
	private class MyRequestListener implements RequestListener
	{
		@Override
		public void onComplete(String response, Object state) 
		{
			Log.d("RequestListener", "onComplete()--> response: " +response);		
		}

		@Override
		public void onIOException(IOException e, Object state) 
		{
			Log.d("RequestListener", "onIOException()");			
		}

		@Override
		public void onFileNotFoundException(FileNotFoundException e, Object state) 
		{
			Log.d("RequestListener", "onFileNotFoundException()");		
		}

		@Override
		public void onMalformedURLException(MalformedURLException e, Object state) 
		{
			Log.d("RequestListener", "onMalformedURLException()");				
		}

		@Override
		public void onFacebookError(FacebookError e, Object state) 
		{
			Log.d("RequestListener", "onFacebookError() " +e.getLocalizedMessage());	
		}		
	}
	
	//events from login phase
	private class MyDialogListener implements DialogListener
	{
		@Override
		public void onComplete(Bundle values)
		{
			Log.d("DialogListener()", "onComplete()");
			
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_COMPLETED);
	        mFacebookHandler.sendMessage(msg);
		}

		@Override
		public void onFacebookError(FacebookError e) 
		{
			Log.d("DialogListener()", "onFacebookError() " +e.getLocalizedMessage());		
			
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_FACEBOOK_ERROR);
	        mFacebookHandler.sendMessage(msg);
		}

		@Override
		public void onError(DialogError e) 
		{
			Log.d("DialogListener()", "onError()");			
			
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_ERROR);
	        mFacebookHandler.sendMessage(msg);
		}

		@Override
		public void onCancel() 
		{
			Log.d("DialogListener()", "onCancel()");	
			
	        Message msg = mFacebookHandler.obtainMessage(Constants.LOGIN_CANCEL);
	        mFacebookHandler.sendMessage(msg);
		}		
	}
}
