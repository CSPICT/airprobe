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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.db.AnnotatedRecord;
import org.csp.everyaware.db.Record;
import org.csp.everyaware.facebooksdk.Facebook;
import org.csp.everyaware.tabactivities.Map;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;


/* DATA OBTAINED DURING APP REGISTRATION ON TWITTER
 * 
	Consumer key 	OOvAO63hbxDRnc9wiG5w
	Consumer secret 	dNFbQk6HaMAAON36Mg6tKn4RufsehZbj4OuhozyswY
	Request token URL 	https://api.twitter.com/oauth/request_token
	Authorize URL 	https://api.twitter.com/oauth/authorize
	Access token URL 	https://api.twitter.com/oauth/access_token
	Callback URL 	http://www.everyaware.eu/
	
	Access token 	552769274-ljMnrJFjpiD5Pygvh7pFJU06WQUPNEo2UkMoBgHQ
	Access token secret 	fiGzcndltiOFUEpm1CU4Ox24bz3jk1iW3ojaPDgP3Sw
	Access level 	Read and write	
*/

public class TwitterManager 
{
	private static TwitterManager mTwMan;
	private Activity mActivity;
    private Twitter mTwitter;
    
    //tokens
    private RequestToken mRequestToken;
    
	public static TwitterManager getInstance(Activity activity)
	{
		if(mTwMan == null)
			mTwMan = new TwitterManager(activity);
		return mTwMan;
	}
	
	private TwitterManager(Activity activity)
	{
		mActivity = activity;
	}
	
	//call this only if internet connection is available
	public void initTwitter()
	{
		ConfigurationBuilder confbuilder = new ConfigurationBuilder();
        Configuration conf = confbuilder
            .setOAuthConsumerKey(Constants.CONSUMER_KEY)
            .setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
            .build();
        mTwitter = new TwitterFactory(conf).getInstance();
        mTwitter.setOAuthAccessToken(null);
        
        //try 
        //{
        	new OuthRequestTokenTask().execute(Constants.CALLBACK_URL);
        	/*
            mRequestToken = mTwitter.getOAuthRequestToken(Constants.CALLBACK_URL);
            Intent intent = new Intent(mActivity, TwitterLogin.class);
            intent.putExtra(Constants.IEXTRA_AUTH_URL, mRequestToken.getAuthorizationURL());
            mActivity.startActivityForResult(intent, 0);*/
        /*
		} 
        catch (TwitterException e) 
        {
            e.printStackTrace();
        }*/
	}
	
	public boolean authoriseNewUser(String oauthVerifier) 
	{
        AccessToken accessToken = null;
        
        try 
        {            
            accessToken = mTwitter.getOAuthAccessToken(mRequestToken, oauthVerifier);
            SharedPreferences pref = mActivity.getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(Constants.PREF_KEY_ACCESS_TOKEN, accessToken.getToken());
            editor.putString(Constants.PREF_KEY_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
            editor.commit();
                              
            Toast.makeText(mActivity, "authorized", Toast.LENGTH_SHORT).show();
            return true;
        } 
        catch(TwitterException e) 
        {
            e.printStackTrace();
            return false;

        }
	}
	
	public void shutdown()
	{
        SharedPreferences pref = mActivity.getSharedPreferences(Constants.PREF_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(Constants.PREF_KEY_ACCESS_TOKEN);
        editor.remove(Constants.PREF_KEY_ACCESS_TOKEN_SECRET);
        editor.commit();
        
        if (mTwitter != null) 
        {
            mTwitter.shutdown();
        }
	}
	
	public void postMessage(Record annotatedRecord)
	{
		SharedPreferences pref = mActivity.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, 
				mActivity.getApplicationContext().MODE_PRIVATE);
        String accessToken = pref.getString(Constants.PREF_KEY_ACCESS_TOKEN, null);
        String accessTokenSecret = pref.getString(Constants.PREF_KEY_ACCESS_TOKEN_SECRET, null);
        if (accessToken == null || accessTokenSecret == null) 
        {
            Toast.makeText(mActivity.getApplicationContext(), "not authorize yet", Toast.LENGTH_SHORT).show();
            return;
        }
        mTwitter.setOAuthAccessToken(new AccessToken(accessToken, accessTokenSecret));

        String status = composeMessage(annotatedRecord);
        
        //do twitter update status in async task
        new PostMessageTask().execute(status);
	}
	
	//compose string message that will be posted on wall
	private String composeMessage(Record record)
	{		
		String message;
		String avgPoll = "";
		
		try
		{
			avgPoll = String.valueOf(record.calcAvgPoll());
		}
		catch(Exception e)
		{}
		
		if(avgPoll.length()-1 - avgPoll.lastIndexOf(".") > 2);
			avgPoll = avgPoll.substring(0, avgPoll.lastIndexOf(".")+4);
		
		//add user annotation to msg
		message = "\"" +record.mUserData1+ "\"\n";
		
		//average pollution
		message += "AQI: " +record.calcAvgPoll()+ "\n";
		
		//add gps coordinates to msg
		if(record.mValues[0] != 0)
			message += "Lat " +record.mValues[0]+"°, Lon "+record.mValues[1]+"°\n";
				
		//format timestamp into date and add to msg
		String dateString = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date(record.mSysTimestamp));	
		message += "Time: " +dateString+ "\n";
		message += "Sent with AirProbe - http://www.everyaware.eu/\n";
		
		if(message.length() > 140)
			message = message.substring(0, 140);
		return message;
	}
	
	private class PostMessageTask extends AsyncTask<String, Integer, Integer>
	{

		@Override
		protected void onPostExecute( Integer result )  
		{
		      if(result == 1)
		    	  Toast.makeText(mActivity.getApplicationContext(), "Twitter status updated", Toast.LENGTH_LONG).show();
		}

		@Override
		protected Integer doInBackground(String... params) 
		{
			try 
	        {
				mTwitter.updateStatus(params[0]);
			} 
	        catch (TwitterException e) 
	        {
				e.printStackTrace();
			}
			return 1;
		}
	}
	
	private class OuthRequestTokenTask extends AsyncTask<String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... url) 
		{
			try
			{
				mRequestToken = mTwitter.getOAuthRequestToken(Constants.CALLBACK_URL);
			}
			catch(TwitterException e)
			{
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result )  
		{
			Intent intent = new Intent(mActivity, TwitterLogin.class);
	        intent.putExtra(Constants.IEXTRA_AUTH_URL, mRequestToken.getAuthorizationURL());
	        mActivity.startActivityForResult(intent, 0);    			
		}
	}
}
