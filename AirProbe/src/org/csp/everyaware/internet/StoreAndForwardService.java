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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.Start;
import org.csp.everyaware.Utils;
import org.csp.everyaware.db.DbManager;
import org.csp.everyaware.db.Record;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class StoreAndForwardService extends Service
{
	private Handler mHandler;
	private boolean mConnectivityOn;
	private DbManager mDbManager;
	private List<Record>mToSendRecords;
	private List<Record>mAllLoadedRecords;
	private PostDataThread mPostDataThread;
	
	@Override
	public void onCreate() 
	{
		Log.d("StoreAndForwardService", "***************************** onCreate() ***************************");
	
		mHandler = new Handler();
		mDbManager = DbManager.getInstance(getApplicationContext());
		mPostDataThread = new PostDataThread();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		Log.d("StoreAndForwardService", "*************************** onStartCommand() ***********************");
		
		mHandler.postDelayed(mSendRecordsRunnable, Utils.getStoreForwInterval(getApplicationContext()));
		
		return super.onStartCommand(intent, flags, startId);		
	}
	
	public void onStart(Intent intent, int startId) 
	{
		Log.d("StoreAndForwardService", "****************************** onStart() ***************************");
		super.onStart(intent, startId);		 
	}
	
	@Override
	public void onDestroy() 
	{
		Log.d("StoreAndForwardService", "***************************** onDestroy() *************************");	
		
		mHandler.removeCallbacks(mSendRecordsRunnable);
	}
	
	@Override
	public IBinder onBind(Intent arg0) 
	{
		return null;
	}

	/************************* RUNNABLE CHE INVIA RECORDS AL SERVER ****************************************/
	
	private Runnable mSendRecordsRunnable = new Runnable()
	{		
		@Override
		public void run() 
		{
			Log.d("SendRecordsRunnable", "run() *******************************");

			if((mToSendRecords != null)&&(mToSendRecords.size() > 0))
			{
				Log.d("SendRecordsRunnable", "run()--> mToSendsRecords still contains data. This runnable will be recalled after N secs");
				mHandler.postDelayed(this, Constants.storeForwHistFreqs);
			}
			else
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
				
				if(mConnectivityOn)
				{
					sendBroadcast(new Intent(Constants.INTERNET_ON));
					Utils.uploadOn = Constants.INTERNET_ON_INT;
				}
				else
				{
					sendBroadcast(new Intent(Constants.INTERNET_OFF));
					Utils.uploadOn = Constants.INTERNET_OFF_INT;
				}
				
				Log.d("SendRecordsRunnable", "run()--> Internet connection on: " +mConnectivityOn);
				
				if(mConnectivityOn)
				{				
					if(mDbManager == null)
						mDbManager = DbManager.getInstance(getApplicationContext());
					
					//if store'n'forward interval is 1 sec, load only records marked with actual session Id
					if(Utils.getStoreForwInterval(getApplicationContext()) == 1000)
						//2 - load from DB the N oldest record not yet sent to server
						mAllLoadedRecords = mDbManager.loadOlderNotUploadedRecords(Constants.REC_TO_UPLOAD_MAX_NUM, true);
					else
					{
						//2 - load from DB the N oldest record not yet sent to server
						if(Utils.historyDownloadMode)
							mAllLoadedRecords = mDbManager.loadOlderNotUploadedRecords(Constants.REC_TO_UPLOAD_MAX_NUM*4, false);
						else
							mAllLoadedRecords = mDbManager.loadOlderNotUploadedRecords(Constants.REC_TO_UPLOAD_MAX_NUM, false);
					}
										
					//if there are records to send, send them, but, from ap 1.4, check if they are all of the 
					//same semantic session seed (that can be an empty string if the are recorded out of a semantic
					//window)
					//only record in the mToSendRecords array will be sent to the server
					if((mAllLoadedRecords != null)&&(mAllLoadedRecords.size() > 0))
					{
						 mToSendRecords = new ArrayList<Record>();
						 String semanticSessionSeed = "";
						 int k = 0;
						 Record temp = null;
						 boolean stop = false;
						 
						 while((k < mAllLoadedRecords.size())&&(!stop))
						 {
							 temp = mAllLoadedRecords.get(k);
							
							 //initialize value with semantic session seed from the first record
							 if(k == 0)
								 semanticSessionSeed = temp.mSemanticSessionSeed;
							 
							 //if actual record contains the same session seed of the first record,
							 //add to array of record to be sent
							 if(semanticSessionSeed.equals(temp.mSemanticSessionSeed))
								 mToSendRecords.add(temp);
							 else //else stop! 
								 stop = true;
							 
							 k++;
						 }
						
						 Log.d("SendRecordsRunnable", "run()--> # all loaded records: "+mAllLoadedRecords.size()+" - # to send records: "+mToSendRecords.size());
						 /*** LOGGED USER ACCESS TOKEN ***/
						 //check if airprobe has been activated
						 if(Utils.getAccountActivationState(getApplicationContext()))
						 {
							//check if access token is available and valid. If it is expired (creation time + expires in is > actual time), ask for a new one
							if(checkIfAccessTokenIsValid())
							{
								Log.d("SendRecordsRunnable", "run()--> access token is still valid");
		
								try
								{
									//3 - send data to server in a gzip http compression format	
									sendBroadcast(new Intent(Constants.UPLOAD_ON)); //send msg to UI thread
									Utils.uploadOn = Constants.UPLOAD_ON_INT;
									
									mPostDataThread = new PostDataThread();
									mPostDataThread.start();		
								} 
						        catch (Exception e) 
						        {
									e.printStackTrace();
								}
								finally
								{
									//5 - next runnable call is scheduled
									if(!Utils.historyDownloadMode)
										mHandler.postDelayed(this, Utils.getStoreForwInterval(getApplicationContext()));
									else
										mHandler.postDelayed(this, Constants.storeForwHistFreqs);
								}
							}
							//if AirProbe has been activated but access token is exipired, require new access token and send data
							else
							{
								Log.d("SendRecordsRunnable", "run()--> access token expired - ask for new one");
								new RefreshTokenTask().execute();
							}
						 }
						 /*** CLIENT ACCESS TOKEN ***/
						 //if AirProbe has not been activated, check if anonymous client access token is valid and use it to send data
						 else
						 {
							//check if access token for client is available and valid. If it is expired (creation time + expires in is > actual time), ask for a new one
							 if(checkIfAccessTokenIsValidForClient())
							 {
								 Log.d("SendRecordsRunnable", "run()--> access token FOR CLIENT is still valid");
								 
								 try
								 {
									//3 - send data to server in a gzip http compression format	
									sendBroadcast(new Intent(Constants.UPLOAD_ON)); //send msg to UI thread
									Utils.uploadOn = Constants.UPLOAD_ON_INT;
										
									mPostDataThread = new PostDataThread();
									mPostDataThread.start();		
								 } 
							     catch (Exception e) 
							     {
									e.printStackTrace();
								 }
								 finally
								 {
									//5 - next runnable call is scheduled
									if(!Utils.historyDownloadMode)
										mHandler.postDelayed(this, Utils.getStoreForwInterval(getApplicationContext()));
									else
										mHandler.postDelayed(this, Constants.storeForwHistFreqs);
								 }
							 }
							 //if AirProbe has not been activated and access token for client is exipired, require new access token for client and send data
							 else
							 {
								Log.d("SendRecordsRunnable", "run()--> access token for client expired - ask for new one");
								new RefreshTokenTaskForClient().execute();
							 }
						 }
					}
					else
					{
						sendBroadcast(new Intent(Constants.FINISHED_UPLOAD));
						Utils.uploadOn = Constants.INTERNET_ON_INT;
						
						Log.d("SendRecordsRunnable", "run()--> No records to send");
						
						//next runnable call is scheduled
						if(!Utils.historyDownloadMode)
							mHandler.postDelayed(this, Utils.getStoreForwInterval(getApplicationContext()));
						else
							mHandler.postDelayed(this, Constants.storeForwHistFreqs);
					}
				}
				else
				{
					Log.d("SendRecordsRunnable", "run()--> No internet connectivity");
					//next runnable call is scheduled
					if(!Utils.historyDownloadMode)
						mHandler.postDelayed(this, Utils.getStoreForwInterval(getApplicationContext()));
					else
						mHandler.postDelayed(this, Constants.storeForwHistFreqs);
				}
			}
		}	
	};
	
	/********************** CALLS postData() FUNCTION *************************************************/
	
	private class PostDataThread extends Thread
	{
		@Override
		public void run()
		{
			int statusCode = -1;
			//int statusCodeTags = -1;
			
			try 
			{
				Thread.currentThread().sleep(0); //helpful for icon on/off sync
				
				if(Utils.report_url == null)
				{
					getServer();
				
					if(Utils.report_url != null)
					{
						//statusCode = postData();
						statusCode = postSecureData();
						postSecureTags();
						//postTags();				
					}
				}
				else
				{
					//statusCode = postData();			
					statusCode = postSecureData();
					//postTags();
					postSecureTags();
				}
			} 
			catch(InterruptedException e)
			{
				e.printStackTrace();
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
			
			Log.d("PostDataThread", "run()--> status code: " +statusCode);
			
								/**********************/		
				
			//4 - if server response is HTTP 200 OK, mark sent records on db with timestamp
			//    at the moment of data post
			if(statusCode == Constants.STATUS_OK)
			{
				long uploadSysTs = System.currentTimeMillis(); //actual system timestamp		
				
				Log.d("PostDataThread", "run()--> # records to update with upload sys ts: "+mToSendRecords.size());
				int size = mDbManager.updateUploadedRecord(mToSendRecords, uploadSysTs);
				
				//increment number of uploaded records stored on DB by 'size' quantity
				Utils.incrUploadedRecCount(getApplicationContext(), size);
			}	
			//4b - if server response is HTTP 401 OK, access_token is expired and before sending again data
			//     I need to require a new access token to server
			if(statusCode == Constants.STATUS_UNAUTHORIZED)
			{
				Log.d("PostDataThread", "run()--> STATUS_UNAUTHORIZED"); 
				//do nothing in this case. This call to send record runnable isn't useful
			}
			
			//5 - cleaning of array to free memory
			if(mAllLoadedRecords != null)
				mAllLoadedRecords.clear();
			if(mToSendRecords != null)
				mToSendRecords.clear();		
			
			//send msg to UI thread
			sendBroadcast(new Intent(Constants.UPLOAD_OFF));
			Utils.uploadOn = Constants.INTERNET_ON_INT;
			
			synchronized(Start.class)
        	{
        		mPostDataThread = null;
        	}
		}
	}
	
	/*************** SCRIVE FILE DI TESTO CONTENENTE OGGETTI JSON *****************************************/
	
	public void writeTextFile(File txtFile) throws IOException
	{
		BufferedWriter bW = null;
		
		try 
		{
			bW = new BufferedWriter(new FileWriter(txtFile));
			bW.write("[");				
			for(int i = 0; i < mToSendRecords.size(); i++)
			{
				bW.write(mToSendRecords.get(i).toJson().toString());				
				if(i != mToSendRecords.size()-1)	
					bW.write(",");						
				bW.write("\n");
			}				
			bW.write("]");
		} 
		finally
		{
			try
			{
				if(bW != null)
					bW.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/***************** METODO CHE COMPRIME UN FILE DI TESTO IN UNO ZIP **********************************/
	/*
	private void compressFile(String inputFilename, String outputFilename) throws IOException
	{
		// Create a buffer for reading the files
		byte[] buf = new byte[1024];

		// Create the ZIP file
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputFilename));

		// Compress the files
		FileInputStream in = new FileInputStream(inputFilename);

		int index = inputFilename.lastIndexOf(File.separator);
		inputFilename = inputFilename.substring(index+1);
		    
		// Add ZIP entry to output stream.
		out.putNextEntry(new ZipEntry(inputFilename));

		// Transfer bytes from the file to the ZIP file
		int len;
		while ((len = in.read(buf)) > 0) 
		{
			out.write(buf, 0, len);
		}

		// Complete the entry
		out.closeEntry();
		in.close();

		// Complete the ZIP file
		out.close();
	}*/
	
	/************************ NEW TOKEN REQUEST *******************************************/
	
	private class RefreshTokenTask extends AsyncTask<Void, Void, Boolean>
	{
		private boolean success = false;
		
		@Override
		protected Boolean doInBackground(Void... params) 
		{	
			try 
			{	
				//doHttpPost();
				
				doSecureHttpPost();
				
			} 
			catch (ClientProtocolException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
			    e.printStackTrace();
		    }
		    catch (JSONException e1) 
			{
				e1.printStackTrace();
			}
			catch(OutOfMemoryError e)
			{
			    e.printStackTrace();
			} 		
			catch (IllegalArgumentException e) 
			{
				e.printStackTrace();
			} 
			return success;
		}	
		
		//if access token needed to be updated, the send of actual set of records is performed here
		@Override
		protected void onPostExecute(Boolean success)
		{
			Log.d("RefreshTokenTask", "onPostExecute()");
		
			try
			{
				if(success)
				{
					//3 - send data to server in a gzip http compression format	
					sendBroadcast(new Intent(Constants.UPLOAD_ON)); //send msg to UI thread
					Utils.uploadOn = Constants.UPLOAD_ON_INT;
					
					mPostDataThread = new PostDataThread();
					mPostDataThread.start();		
				}
				else
				{
					if(mAllLoadedRecords != null)
						mAllLoadedRecords.clear();
					//cleaning of array to free memory
					if(mToSendRecords != null)
						mToSendRecords.clear();		
					
					//send msg to UI thread
					sendBroadcast(new Intent(Constants.UPLOAD_OFF));
					Utils.uploadOn = Constants.INTERNET_ON_INT;
				}
			} 
	        catch (Exception e) 
	        {
				e.printStackTrace();
			}
			finally
			{
				//5 - next runnable call is scheduled
				if(!Utils.historyDownloadMode)
					mHandler.postDelayed(mSendRecordsRunnable, Utils.getStoreForwInterval(getApplicationContext()));
				else
					mHandler.postDelayed(mSendRecordsRunnable, Constants.storeForwHistFreqs);
			}
		}
		
		//makes a not secure (http://) post with hidden parameters to request the couple <access_token, refresh_token>
		//not used but do not delete it
		private void doHttpPost() throws ClientProtocolException, IOException, JSONException, OutOfMemoryError, IllegalArgumentException
		{
			int statusCode = -1;
			
			//http post to hide sent params	
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(Constants.REFRESH_TOKEN_URL);
			
		    // Add  data
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		    nameValuePairs.add(new BasicNameValuePair("grant_type", "refresh_token"));
		    nameValuePairs.add(new BasicNameValuePair("client_id", "airprobe_android_client"));
		    nameValuePairs.add(new BasicNameValuePair("client_secret", Constants.SECRET_KEY));
		    nameValuePairs.add(new BasicNameValuePair("refresh_token", Utils.getRefreshToken(getApplicationContext())));			
		    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		    // Execute HTTP Post Request
		    HttpResponse response = httpClient.execute(httpPost);
		        
			//copio la entity response su output stream, ottenendo una stringa
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    response.getEntity().writeTo(out);
		    String responseString = out.toString();
		    Log.d("RefreshTokenTask", "doHttpPost()--> "+responseString);
		    	
			statusCode = response.getStatusLine().getStatusCode();				
				
			//se la risposta dal server è 'HTTP 200 OK'
			if(statusCode == Constants.STATUS_OK)
			{
			    if (response.getEntity() != null) 
			    {  
				    JSONObject json = new JSONObject(responseString);
				    String accessToken = json.getString("access_token");
				    String refreshToken = json.getString("refresh_token");
				    String tokenType = json.getString("token_type");				    	
				    int expiresIn = json.getInt("expires_in");
						    
				    Log.d("RefreshTokenTask", "doHttpPost()--> "+accessToken+" "+refreshToken+" "+tokenType+" "+expiresIn);	
						    
				    //save account credentials in shared preferences
				    Utils.setCredentialsData(getApplicationContext(), accessToken, refreshToken, tokenType, expiresIn, System.currentTimeMillis() / 1000);
						    
				    success = true;
			    } 						    
			}
			else
			{
			    if (response.getEntity() != null) 
			    {  
				    JSONObject json = new JSONObject(responseString);
				    String error = json.getString("error");
				    String errorDescr = json.getString("error_description");
						    
				    Log.d("RefreshTokenTask", "doHttpPost()--> "+error+" "+errorDescr);
						    
				    success = false;
			    } 		
			}			
		}
		
		//makes a secure (https://) http post to request the couple <access_token, refresh_token>
		private void doSecureHttpPost() throws ClientProtocolException, IOException, JSONException, OutOfMemoryError, IllegalArgumentException
		{			
			String urlString = Constants.REFRESH_TOKEN_URL;
			String urlParameters = "?grant_type=refresh_token&client_id=airprobe_android_client&client_secret="+Constants.SECRET_KEY+"&refresh_token="+Utils.getRefreshToken(getApplicationContext());
			
			URL url = new URL(urlString+urlParameters);
			//URL url = new URL(Constants.REFRESH_TOKEN_URL);
		    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
		    
		    con.setRequestMethod("POST");
		    con.setUseCaches(false);
		    con.setDoInput(true);
			con.setDoOutput(true);
			
			int responseCode = con.getResponseCode();
			//Log.d("RefreshTokenTask", "doSecureHttpPost()--> Sending 'POST' request to URL : " + url);
			//Log.d("RefreshTokenTask", "doSecureHttpPost()--> Post parameters : " + urlParameters);
			Log.d("RefreshTokenTask", "doSecureHttpPost()--> Response Code : " + responseCode);

		    String responseString = getStringFromInputStream(con.getInputStream());
		    Log.d("RefreshTokenTask", "doSecureHttpPost()--> "+responseString);
		    
			//se la risposta dal server è 'HTTP 200 OK'
			if(responseCode == Constants.STATUS_OK)
			{
				JSONObject json = new JSONObject(responseString);
				String accessToken = json.getString("access_token");
				String refreshToken = json.getString("refresh_token");
				String tokenType = json.getString("token_type");				    	
				int expiresIn = json.getInt("expires_in");
						    
				Log.d("RefreshTokenTask", "doSecureHttpPost()--> "+accessToken+" "+refreshToken+" "+tokenType+" "+expiresIn);	
						    
				//save account credentials in shared preferences
				Utils.setCredentialsData(getApplicationContext(), accessToken, refreshToken, tokenType, expiresIn, System.currentTimeMillis() / 1000);
						    
				success = true;			     						    
			}
			else
			{ 
				JSONObject json = new JSONObject(responseString);
				String error = json.getString("error");
				String errorDescr = json.getString("error_description");
						    
				Log.d("RefreshTokenTask", "doSecureHttpPost()--> "+error+" "+errorDescr);
						    
				success = false; 		
			}		
		}
		
		private String getStringFromInputStream(InputStream is)
		{ 
			BufferedReader br = null;
			StringBuilder sb = new StringBuilder();
	 
			String line;
			try 
			{
				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) 
				{
					sb.append(line);
				}
	 
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			finally 
			{
				if (br != null) 
				{
					try 
					{
						br.close();
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
	 
			return sb.toString();	 
		}
	}
	
	private class RefreshTokenTaskForClient extends AsyncTask<Void, Void, Boolean>
	{
		private boolean success = false;
		
		@Override
		protected Boolean doInBackground(Void... params) 
		{	
			try 
			{	
				doSecureHttpPostForClient();					
			} 
			catch (ClientProtocolException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
			    e.printStackTrace();
		    }
		    catch (JSONException e1) 
			{
				e1.printStackTrace();
			}
			catch(OutOfMemoryError e)
			{
			    e.printStackTrace();
			} 		
			catch (IllegalArgumentException e) 
			{
				e.printStackTrace();
			} 
			return success;
		}	
		
		//if access token needed to be updated, the send of actual set of records is performed here
		@Override
		protected void onPostExecute(Boolean success)
		{
			Log.d("RefreshTokenTaskForClient", "onPostExecute()");
		
			try
			{
				if(success)
				{
					//3 - send data to server in a gzip http compression format	
					sendBroadcast(new Intent(Constants.UPLOAD_ON)); //send msg to UI thread
					Utils.uploadOn = Constants.UPLOAD_ON_INT;
					
					mPostDataThread = new PostDataThread();
					mPostDataThread.start();
				}
				else
				{
					if(mAllLoadedRecords != null)
						mAllLoadedRecords.clear();
					//cleaning of array to free memory
					if(mToSendRecords != null)
						mToSendRecords.clear();		
					
					//send msg to UI thread
					sendBroadcast(new Intent(Constants.UPLOAD_OFF));
					Utils.uploadOn = Constants.INTERNET_ON_INT;
				}
			} 
	        catch (Exception e) 
	        {
				e.printStackTrace();
			}
			finally
			{
				//5 - next runnable call is scheduled
				if(!Utils.historyDownloadMode)
					mHandler.postDelayed(mSendRecordsRunnable, Utils.getStoreForwInterval(getApplicationContext()));
				else
					mHandler.postDelayed(mSendRecordsRunnable, Constants.storeForwHistFreqs);
			}
		}		
	
		//makes a secure (https://) http post to request the couple <access_token, refresh_token>
		private void doSecureHttpPostForClient() throws ClientProtocolException, IOException, JSONException, OutOfMemoryError, IllegalArgumentException
		{			
			String urlString = Constants.REFRESH_TOKEN_URL;
			String urlParameters = "?grant_type=client_credentials&client_id=airprobe_android_client&client_secret="+Constants.SECRET_KEY;
			
			URL url = new URL(urlString+urlParameters);
		    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
		    
		    con.setRequestMethod("POST");
		    con.setUseCaches(false);
		    con.setDoInput(true);
			con.setDoOutput(true);
			
			int responseCode = con.getResponseCode();
			//Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> Sending 'POST' request to URL : " + url);
			//Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> Post parameters : " + urlParameters);
			Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> Response Code : " + responseCode);
	
		    String responseString = getStringFromInputStream(con.getInputStream());
		    Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> "+responseString);
		    
			//se la risposta dal server è 'HTTP 200 OK'
			if(responseCode == Constants.STATUS_OK)
			{
				JSONObject json = new JSONObject(responseString);
				String accessToken = json.getString("access_token");
				//String refreshToken = json.getString("refresh_token");
				String tokenType = json.getString("token_type");				    	
				int expiresIn = json.getInt("expires_in");
						    
				Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> "+accessToken+" "+tokenType+" "+expiresIn);	
						    
				//save account credentials in shared preferences
				Utils.setCredentialsDataForClient(getApplicationContext(), accessToken, tokenType, expiresIn, System.currentTimeMillis() / 1000);
						    
				success = true;			     						    
			}
			else
			{ 
				JSONObject json = new JSONObject(responseString);
				String error = json.getString("error");
				String errorDescr = json.getString("error_description");
						    
				Log.d("RefreshTokenTaskForClient", "doSecureHttpPostForClient()--> "+error+" "+errorDescr);
						    
				success = false; 		
			}		
		}
		
		private String getStringFromInputStream(InputStream is)
		{ 
			BufferedReader br = null;
			StringBuilder sb = new StringBuilder();
	 
			String line;
			try 
			{
				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) 
				{
					sb.append(line);
				}
	 
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			} 
			finally 
			{
				if (br != null) 
				{
					try 
					{
						br.close();
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
			}
	 
			return sb.toString();	 
		}
	}
	
	/***************** GET ENDPOINT ADDRESS FROM REDIRECT SERVER **************************/
	
	public void getServer() throws IllegalArgumentException, ClientProtocolException, 
	HttpHostConnectException, IOException
	{
		Log.d("StoreAndForwardService", "getServer()");
		
		DefaultHttpClient httpClient = new DefaultHttpClient();    
	    HttpPost httpPost = new HttpPost(Constants.REDIRECT_ADDR);
	    httpClient.setRedirectHandler(new RedirectHandler()
	    {
			@Override
			public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException 
			{
				Log.d("StoreAndForwardService", "getServer() - getLocationURI()");
				return null;
			}
	
			@Override
			public boolean isRedirectRequested(HttpResponse response, HttpContext context) throws ParseException 
			{
				String responseBody = null;
				
				try 
				{
					responseBody = EntityUtils.toString(response.getEntity());
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				catch(OutOfMemoryError e)
				{
					e.printStackTrace();
				}
				
				if(responseBody != null)
				{
					Log.d("StoreAndForwardService", "getServer() - isRedirectRequested()--> status line: " +response.getStatusLine());
					
					if(response.getStatusLine().getStatusCode() == 302)
					{	
						Header[] locHeader = response.getHeaders("Location");
						if((locHeader != null)&&(locHeader.length > 0))
						{
							Utils.report_url = locHeader[0].getValue();
							Log.d("StoreAndForwardService", "getServer() - isRedirectRequested()--> report url: " +Utils.report_url);							
						}
						
						Header[] countryHeader = response.getHeaders("Country");
						if((countryHeader != null)&&(countryHeader.length > 0))
						{
							Utils.report_country = countryHeader[0].getValue();
							Log.d("StoreAndForwardService", "getServer() - isRedirectRequested()--> report country: " +Utils.report_country);
						}
					}
					else
						Log.d("StoreAndForwardService", "getServer() - isRedirectRequested()--> redirect server response is WRONG!");
				}
				else
					Log.d("StoreAndForwardService", "getServer() - isRedirectRequested()--> response body from redirect server is NULL!");
				
				return false;
			}
		});
	    
	    httpClient.execute(httpPost);
	}
	
	/********************** SEND DATA TO SERVER *******************************************************/
	
	//create an array of json objects containing records, compress it in gzip http compression format and send it to server
	public int postData() throws IllegalArgumentException, ClientProtocolException, 
	HttpHostConnectException, IOException
	{			
		Log.d("StoreAndForwardService", "postData()");

		//get size of array of records to send and reference to the last record
		int size = mToSendRecords.size();
		if(size == 0)
			return -1;
		
		int sepIndex = 1; //default is '.' separator (see Constants.separators array)
		if((Utils.report_country != null)&&(Utils.report_country.equals("IT")))
			sepIndex = 0; //0 is for '-' separator (for italian CSP server)
		
		Record lastToSendRecord = mToSendRecords.get(size-1);
		Log.d("StoreAndForwardService", "postData()--> # of records: "+size);
		
		//save timestamp
		long lastTimestamp = 0;		
		if(lastToSendRecord.mSysTimestamp > 0)
			lastTimestamp = lastToSendRecord.mSysTimestamp;
		else
			lastTimestamp = lastToSendRecord.mBoxTimestamp;
		
		String lastTsFormatted = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSz", Locale.US).format(new Date(lastTimestamp));

		//********* MAKING OF HTTP HEADER **************
		
	    DefaultHttpClient httpClient = new DefaultHttpClient();    
	    HttpPost httpPost = new HttpPost(Utils.report_url);	     
	    
	    httpPost.setHeader("Content-Encoding", "gzip");
	    httpPost.setHeader("Content-Type", "application/json");
	    httpPost.setHeader("Accept", "application/json");
	    httpPost.setHeader("User-Agent", "AirProbe"+Utils.appVer);

	    //******** authorization bearer header ********
	    
	    //air probe can be used also anymously. If account activation state is true (--> AirProbe activated) add this header
	    if(Utils.getAccountActivationState(getApplicationContext()))
	    	httpPost.setHeader("Authorization", "Bearer "+Utils.getAccessToken(getApplicationContext()));
	    
	    //******** meta header (for new version API V1)
	    
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"timestampRecorded", lastTsFormatted);   
	    //httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sessionId", lastToSendRecord.mSessionId); //deprecated from AP 1.4
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"deviceId", Utils.deviceID);
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"installId", Utils.installID);
	    //httpPost.setHeader("meta"+Constants.separators[sepIndex]+"userFeedId", "");
	    //httpPost.setHeader("meta"+Constants.separators[sepIndex]+"eventFeedId", "");
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"visibilityEvent", Constants.DATA_VISIBILITY[0]);
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"visibilityGlobal", Constants.DATA_VISIBILITY[0]); //set meta.visibilityGlobal=DETAILS for testing (to retrieve data after insertion)
	    
	    /* from AP 1.4 */
	    if((lastToSendRecord.mBoxMac != null)&&(!lastToSendRecord.mBoxMac.equals("")))
	    {	    	
	    	httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sourceId", lastToSendRecord.mBoxMac);
	    }
	    
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sourceSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSourceSessionSeed);
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sourceSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSourceSessionNumber));
	    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sourceSessionPointNumber", String.valueOf(lastToSendRecord.mSourcePointNumber));
	    
	    if((lastToSendRecord.mSemanticSessionSeed != null)&&(!lastToSendRecord.mSemanticSessionSeed.equals("")))
	    {
	    	httpPost.setHeader("meta"+Constants.separators[sepIndex]+"semanticSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSemanticSessionSeed);
		    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"semanticSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSemanticSessionNumber));
		    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"semanticSessionPointNumber", String.valueOf(lastToSendRecord.mSemanticPointNumber));
	    }
	    
	    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"typeVersion", "30"); //update by increment this field on header changes
	    
	    /* end of from AP 1.4 */
	    
	    //******** data header (for new version API V1)
	    
	    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"type", "airprobe_report");
	    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"format", "json");
	    //httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"specification", "a-3"); //deprecated from ap v1.4  
	    if(size > 1)
	    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "true");
	    else
	    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "false");	    
	    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"listSize", String.valueOf(size));
	    
	    //******** geo header (for new version API V1)

	    //add the right provider to header
	    if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[0])) //sensor box
	    {
	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mBoxLon));
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mBoxLat));
	 	    if(lastToSendRecord.mBoxAcc != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"hdop", String.valueOf(lastToSendRecord.mBoxAcc)); //from AP 1.4
	 	    if(lastToSendRecord.mBoxAltitude != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mBoxAltitude)); //from AP 1.4
	 	   	if(lastToSendRecord.mBoxSpeed != 0)
	 	   		httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mBoxSpeed)); //from AP 1.4
	 	  	if(lastToSendRecord.mBoxBear != 0)
	 	  		httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mBoxBear)); //from AP 1.4	 	    
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }
	    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[1])) //phone
	    {
	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mPhoneLon));
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mPhoneLat));
	 	    if(lastToSendRecord.mPhoneAcc != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mPhoneAcc));
	 	    if(lastToSendRecord.mPhoneAltitude != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mPhoneAltitude)); //from AP 1.4
	 	    if(lastToSendRecord.mPhoneSpeed != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mPhoneSpeed)); //from AP 1.4
	 	    if(lastToSendRecord.mPhoneBear != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mPhoneBear)); //from AP 1.4
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }
	    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[2])) //network
	    {
	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mNetworkLon));
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mNetworkLat));
	 	    if(lastToSendRecord.mNetworkAcc != 0)
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mNetworkAcc));
	 	    if(lastToSendRecord.mNetworkAltitude != 0)	 	    
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mNetworkAltitude)); //from AP 1.4
	 	    if(lastToSendRecord.mNetworkSpeed != 0)	 	    
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mNetworkSpeed)); //from AP 1.4
	 	    if(lastToSendRecord.mNetworkBear != 0)	 	    
	 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mNetworkBear)); //from AP 1.4	 	     
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }   
	    
	    //******** MAKING OF HTTP CONTENT (JSON) *************
	    
	    //writing string content as an array of json object
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		for(int i = 0; i < mToSendRecords.size(); i++)
		{
			sb.append(mToSendRecords.get(i).toJson().toString());				
			if((size != 0)&&(i != size-1))
				sb.append(",");						
			sb.append("\n");
		}			
		sb.append("]");
		
		Log.d("StoreAndForwardService", "postData()--> json: " +sb.toString());
		Log.d("StoreAndForwardService", "postData()--> access token: "+Utils.getAccessToken(getApplicationContext()));

		//compress json content into byte array entity
		byte[] contentGzippedBytes = zipStringToBytes(sb.toString());
		ByteArrayEntity byteArrayEntity = new ByteArrayEntity(contentGzippedBytes);		
		byteArrayEntity.setChunked(false); //IMPORTANT: put false for smartcity.csp.it server

	    httpPost.setEntity(byteArrayEntity);
	    
	    Log.d("StoreAndForwardService", "postData()--> Content length: " +httpPost.getEntity().getContentLength());
		Log.d("StoreAndForwardService", "postData()--> Method: " +httpPost.getMethod());
	
		//do http post (it performs asynchronously)
		HttpResponse response = httpClient.execute(httpPost);
	
		sb = null;

		//server response
		Log.d("StoreAndForwardService", "postData()--> status line: " +response.getStatusLine()); 
		
		httpClient.getConnectionManager().shutdown();
		        
		//server response, status line
		StatusLine statusLine = response.getStatusLine();
		Log.d("StoreAndForwardService", "postData()--> status code: " +statusLine.getStatusCode());
		return statusLine.getStatusCode(); 
	}	
	
	//create an array of json objects containing records, compress it in gzip http compression format and send it to server
	//makes a secure (https://) http post
	public int postSecureData() throws IllegalArgumentException, ClientProtocolException, 
	HttpHostConnectException, IOException
	{			
		Log.d("StoreAndForwardService", "postSecureData()");

		//get size of array of records to send and reference to the last record
		int size = mToSendRecords.size();
		if(size == 0)
			return -1;
		
		int sepIndex = 1; //default is '.' separator (see Constants.separators array)
		if((Utils.report_country != null)&&(Utils.report_country.equals("IT")))
			sepIndex = 0; //0 is for '-' separator (for italian CSP server)
		
		Record lastToSendRecord = mToSendRecords.get(size-1);
		Log.d("StoreAndForwardService", "postSecureData()--> # of records: "+size);
		
		//save timestamp
		long lastTimestamp = 0;		
		if(lastToSendRecord.mSysTimestamp > 0)
			lastTimestamp = lastToSendRecord.mSysTimestamp;
		else
			lastTimestamp = lastToSendRecord.mBoxTimestamp;
		
		String lastTsFormatted = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSz", Locale.US).format(new Date(lastTimestamp));

		//********* MAKING OF HTTP HEADER **************

		URL url = new URL(Utils.report_url);
		HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
	    
	    con.setRequestMethod("POST");
	    con.setUseCaches(false);
	    con.setDoInput(true);
		con.setDoOutput(true);
		
		con.setRequestProperty("Content-Encoding", "gzip");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("User-Agent", "AirProbe"+Utils.appVer);

	    //******** authorization bearer header ********
	    
		//air probe can be used also anymously. If account activation state is true (--> AirProbe activated) add this header
		if(Utils.getAccountActivationState(getApplicationContext()))
			con.setRequestProperty("Authorization", "Bearer "+Utils.getAccessToken(getApplicationContext()));
		else if(Utils.getAccountActivationStateForClient(getApplicationContext()))
			con.setRequestProperty("Authorization", "Bearer "+Utils.getAccessTokenForClient(getApplicationContext()));
		
	    //******** meta header (for new version API V1)
	    
		con.setRequestProperty("meta"+Constants.separators[sepIndex]+"timestampRecorded", lastTsFormatted);
		//con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sessionId", lastToSendRecord.mSessionId); //deprecated from AP 1.4
		con.setRequestProperty("meta"+Constants.separators[sepIndex]+"deviceId", Utils.deviceID);
		con.setRequestProperty("meta"+Constants.separators[sepIndex]+"installId", Utils.installID);
		//con.setRequestProperty("meta"+Constants.separators[sepIndex]+"userFeedId", "");
		//con.setRequestProperty("meta"+Constants.separators[sepIndex]+"eventFeedId", "");
		con.setRequestProperty("meta"+Constants.separators[sepIndex]+"visibilityEvent", Constants.DATA_VISIBILITY[0]);
		con.setRequestProperty("meta"+Constants.separators[sepIndex]+"visibilityGlobal", Constants.DATA_VISIBILITY[0]);	//set meta.visibilityGlobal=DETAILS for testing (to retrieve data after insertion)	
	    
		/* from AP 1.4 */
	    if((lastToSendRecord.mBoxMac != null)&&(!lastToSendRecord.mBoxMac.equals("")))
	    {
	    	Log.d("StoreAndForwardService", "postSecureData()--> box mac address: "+lastToSendRecord.mBoxMac);
	    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceId", lastToSendRecord.mBoxMac);
	    }
	    else
	    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceId", Utils.getDeviceAddress(getApplicationContext()));
	    
	    con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSourceSessionSeed);
	    con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSourceSessionNumber));
	    con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceSessionPointNumber", String.valueOf(lastToSendRecord.mSourcePointNumber));
	    
	    if((lastToSendRecord.mSemanticSessionSeed != null)&&(!lastToSendRecord.mSemanticSessionSeed.equals("")))
	    {
	    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"semanticSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSemanticSessionSeed);
	    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"semanticSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSemanticSessionNumber));
	    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"semanticSessionPointNumber", String.valueOf(lastToSendRecord.mSemanticPointNumber));
	    }
	    
	    con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"typeVersion", "30"); //update by increment this field on header changes	    
	    /* end of from AP 1.4 */
		
	    //******** data header (for new version API V1)
	    
		con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"type", "airprobe_report");
		con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"format", "json");
		//con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"specification", "a-3"); //deprecated from AP 1.4
	    if(size > 1)
	    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "true");
	    else
	    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "false");
	    con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"listSize", String.valueOf(size));

	    //******** geo header (for new version API V1)

	    //add the right provider to header
	    if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[0])) //sensor box
	    {
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mBoxLon));
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mBoxLat));
	 	    if(lastToSendRecord.mBoxAcc != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"hdop", String.valueOf(lastToSendRecord.mBoxAcc)); //from AP 1.4
	 	    if(lastToSendRecord.mBoxAltitude != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mBoxAltitude)); //from AP 1.4
	 	   	if(lastToSendRecord.mBoxSpeed != 0)
	 	   	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mBoxSpeed)); //from AP 1.4
	 	  	if(lastToSendRecord.mBoxBear != 0)
	 	  		con.setRequestProperty("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mBoxBear)); //from AP 1.4	 	    
	 	  	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	  	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }
	    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[1])) //phone
	    {
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mPhoneLon));
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mPhoneLat));
	 	    if(lastToSendRecord.mPhoneAcc != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mPhoneAcc));
	 	    if(lastToSendRecord.mPhoneAltitude != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mPhoneAltitude)); //from AP 1.4
	 	    if(lastToSendRecord.mPhoneSpeed != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mPhoneSpeed)); //from AP 1.4
	 	    if(lastToSendRecord.mPhoneBear != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mPhoneBear)); //from AP 1.4
	 	    con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	    con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }
	    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[2])) //network
	    {
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mNetworkLon));
	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mNetworkLat));
	 	    if(lastToSendRecord.mNetworkAcc != 0)
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mNetworkAcc));
	 	    if(lastToSendRecord.mNetworkAltitude != 0)	 	    
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mNetworkAltitude)); //from AP 1.4
	 	    if(lastToSendRecord.mNetworkSpeed != 0)	 	    
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mNetworkSpeed)); //from AP 1.4
	 	    if(lastToSendRecord.mNetworkBear != 0)	 	    
	 	    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mNetworkBear)); //from AP 1.4	 	     
	 	    con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
	 	    con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
	    }  
	    
	    //******** MAKING OF HTTP CONTENT (JSON) *************
	    
	    //writing string content as an array of json object
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		for(int i = 0; i < mToSendRecords.size(); i++)
		{
			sb.append(mToSendRecords.get(i).toJson().toString());				
			if((size != 0)&&(i != size-1))
				sb.append(",");						
			sb.append("\n");
		}			
		sb.append("]");
		
		//Log.d("StoreAndForwardService", "postSecureData()--> json: " +sb.toString());
		//Log.d("StoreAndForwardService", "postSecureData()--> access token: "+Utils.getAccessToken(getApplicationContext()));

		//compress json content into byte array entity
		byte[] contentGzippedBytes = zipStringToBytes(sb.toString());
		
	    //write json compressed content into output stream
		OutputStream outputStream = con.getOutputStream();
		outputStream.write(contentGzippedBytes);
		outputStream.flush();
		outputStream.close();
		
		int responseCode = con.getResponseCode();
		
		Log.d("StoreAndForwardService", "postSecureData()--> response code: " +responseCode);

		sb = null;

		return responseCode; 
	}		
	
	//create an array of json objects containing only tags, compress it in gzip http compression format and send it to server
	public void postTags() throws IllegalArgumentException, ClientProtocolException, HttpHostConnectException, IOException
	{			
		Log.d("StoreAndForwardService", "postTags()");
		
		int sepIndex = 1; //default is '.' separator (see Constants.separators array)
		if((Utils.report_country != null)&&(Utils.report_country.equals("IT")))
			sepIndex = 0; //0 is for '-' separator (for italian CSP server)
		
		List<String> sids = mDbManager.getSidsOfRecordsWithTags();

		if((sids != null)&&(sids.size() > 0))
		{
			for(int i = 0; i < sids.size(); i++)
			{
				String sessionId = (String)sids.get(i);
						
				//load records containing user tags with actual session id
				List<Record>recordsWithTags = mDbManager.loadRecordsWithTagBySessionId(sessionId);
				
				if((recordsWithTags != null)&&(recordsWithTags.size() > 0))
				{
					//get size of array of records containing tags and reference to the last record
					int size = recordsWithTags.size();
					
					//obtain reference to the last record of serie
					Record lastToSendRecord = recordsWithTags.get(size-1);
					Log.d("StoreAndForwardService", "postTags()--> # of records containing tags: "+size);
					
					//save timestamp of last record containing tags
					long lastTimestamp = 0;		
					if(lastToSendRecord.mSysTimestamp > 0)
						lastTimestamp = lastToSendRecord.mSysTimestamp;
					else
						lastTimestamp = lastToSendRecord.mBoxTimestamp;
					
					String lastTsFormatted = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSz", Locale.US).format(new Date(lastTimestamp));
				
					//********* MAKING OF HTTP HEADER **************
					
				    DefaultHttpClient httpClient = new DefaultHttpClient();    
				    HttpPost httpPost = new HttpPost(Utils.report_url);	     
				    
				    httpPost.setHeader("Content-Encoding", "gzip");
				    httpPost.setHeader("Content-Type", "application/json");
				    httpPost.setHeader("Accept", "application/json");
				    httpPost.setHeader("User-Agent", "AirProbe"+Utils.appVer);

				    // ******* authorization bearer header ********
				   
				    httpPost.setHeader("Authorization", "Bearer "+Utils.getAccessToken(getApplicationContext()));
				    
				    //******** meta header (for new version API V1)
				    
				    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"timestampRecorded", lastTsFormatted);   
				    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"deviceId", Utils.deviceID);
				    httpPost.setHeader("meta"+Constants.separators[sepIndex]+"installId", Utils.installID);
				    
				    //******** data header (for new version API V1)
				    
				    //httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedPacketId", "");
				    //httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedPacketPointId", "");
				    //httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedSessionId", ""); //deprecated from AP 1.4
				    
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"type", "airprobe_tags");
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"format", "json");
				    //httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"specification", "at-3");	//deprecated from AP 1.4
				    if(size > 1)
				    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "true");
				    else
				    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "false");	    
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"listSize", String.valueOf(size));
				    
				    /* from AP 1.4 */
				    if((lastToSendRecord.mBoxMac != null)&&(!lastToSendRecord.mBoxMac.equals("")))
				    	httpPost.setHeader("meta"+Constants.separators[sepIndex]+"sourceId", lastToSendRecord.mBoxMac);
				    
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedSourceSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSourceSessionSeed);
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedSourceSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSourceSessionNumber));
				    if((lastToSendRecord.mSemanticSessionSeed != null)&&(!lastToSendRecord.mSemanticSessionSeed.equals("")))
				    {
				    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedSemanticSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSemanticSessionSeed);
				    	httpPost.setHeader("data"+Constants.separators[sepIndex]+"extendedSemanticSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSemanticSessionNumber));
				    }
				    
				    httpPost.setHeader("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"typeVersion", "30"); //update by increment this field on header changes				    
				    /* end of from AP 1.4 */
				    
				    //******** geo header (for new version API V1)
				    
				    //add the right provider to header
				    if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[0])) //sensor box
				    {
				    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mBoxLon));
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mBoxLat));
				 	    if(lastToSendRecord.mBoxAcc != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"hdop", String.valueOf(lastToSendRecord.mBoxAcc)); //from AP 1.4
				 	    if(lastToSendRecord.mBoxAltitude != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mBoxAltitude)); //from AP 1.4
				 	   	if(lastToSendRecord.mBoxSpeed != 0)
				 	   		httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mBoxSpeed)); //from AP 1.4
				 	  	if(lastToSendRecord.mBoxBear != 0)
				 	  		httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mBoxBear)); //from AP 1.4	 	    
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }
				    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[1])) //phone
				    {
				    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mPhoneLon));
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mPhoneLat));
				 	    if(lastToSendRecord.mPhoneAcc != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mPhoneAcc));
				 	    if(lastToSendRecord.mPhoneAltitude != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mPhoneAltitude)); //from AP 1.4
				 	    if(lastToSendRecord.mPhoneSpeed != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mPhoneSpeed)); //from AP 1.4
				 	    if(lastToSendRecord.mPhoneBear != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mPhoneBear)); //from AP 1.4
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }
				    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[2])) //network
				    {
				    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mNetworkLon));
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mNetworkLat));
				 	    if(lastToSendRecord.mNetworkAcc != 0)
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mNetworkAcc));
				 	    if(lastToSendRecord.mNetworkAltitude != 0)	 	    
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mNetworkAltitude)); //from AP 1.4
				 	    if(lastToSendRecord.mNetworkSpeed != 0)	 	    
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mNetworkSpeed)); //from AP 1.4
				 	    if(lastToSendRecord.mNetworkBear != 0)	 	    
				 	    	httpPost.setHeader("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mNetworkBear)); //from AP 1.4	 	     
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				 	    httpPost.setHeader("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }   
				    
				    //******** MAKING OF HTTP CONTENT (JSON) *************
				    
				    //writing string content as an array of json object
				    StringBuilder sb = new StringBuilder();
					sb.append("[");
					
					JSONObject object = new JSONObject();
					
					try 
					{
						object.put("timestamp", lastTimestamp);
						
						JSONArray locations = new JSONArray();
						
						//sensor box gps data
						if(lastToSendRecord.mBoxLat != 0)
						{
							JSONObject boxLocation = new JSONObject();
							boxLocation.put("latitude", lastToSendRecord.mBoxLat);
							boxLocation.put("longitude", lastToSendRecord.mBoxLon);
							if(lastToSendRecord.mBoxAcc != 0)
								boxLocation.put("hdpop", lastToSendRecord.mBoxAcc);
							if(lastToSendRecord.mBoxAltitude != 0)
								boxLocation.put("altitude", lastToSendRecord.mBoxAltitude);
							if(lastToSendRecord.mBoxSpeed != 0)
								boxLocation.put("speed", lastToSendRecord.mBoxSpeed);
							if(lastToSendRecord.mBoxBear != 0)
								boxLocation.put("bearing", lastToSendRecord.mBoxBear);
							boxLocation.put("provider", Constants.GPS_PROVIDERS[0]);
							boxLocation.put("timestamp", lastToSendRecord.mBoxTimestamp);
							
							locations.put(0, boxLocation);
						}
						
						//phone gps data
						if(lastToSendRecord.mPhoneLat != 0)
						{
							JSONObject phoneLocation = new JSONObject();
							phoneLocation.put("latitude", lastToSendRecord.mPhoneLat);
							phoneLocation.put("longitude", lastToSendRecord.mPhoneLon);
							if(lastToSendRecord.mPhoneAcc != 0)
								phoneLocation.put("accuracy", lastToSendRecord.mPhoneAcc);
							if(lastToSendRecord.mPhoneAltitude != 0)
								phoneLocation.put("altitude", lastToSendRecord.mPhoneAltitude);
							if(lastToSendRecord.mPhoneSpeed != 0)
								phoneLocation.put("speed", lastToSendRecord.mPhoneSpeed);
							if(lastToSendRecord.mPhoneBear != 0)
								phoneLocation.put("bearing", lastToSendRecord.mPhoneBear);
							phoneLocation.put("provider", Constants.GPS_PROVIDERS[1]);
							phoneLocation.put("timestamp", lastToSendRecord.mPhoneTimestamp);
							
							locations.put(1, phoneLocation);
						}
						
						//network gps data
						if(lastToSendRecord.mNetworkLat != 0)
						{
							JSONObject netLocation = new JSONObject();
							netLocation.put("latitude", lastToSendRecord.mNetworkLat);
							netLocation.put("longitude", lastToSendRecord.mNetworkLon);
							if(lastToSendRecord.mNetworkAcc != 0)
								netLocation.put("accuracy", lastToSendRecord.mNetworkAcc);
							if(lastToSendRecord.mNetworkAltitude != 0)
								netLocation.put("altitude", lastToSendRecord.mNetworkAltitude);
							if(lastToSendRecord.mNetworkSpeed != 0)
								netLocation.put("speed", lastToSendRecord.mNetworkSpeed);
							if(lastToSendRecord.mNetworkBear != 0)
								netLocation.put("bearing", lastToSendRecord.mNetworkBear);				
							netLocation.put("provider", Constants.GPS_PROVIDERS[2]);
							netLocation.put("timestamp", lastToSendRecord.mNetworkTimestamp);

							locations.put(2, netLocation);
						}
						
						object.put("locations", locations);
					}
					catch (JSONException e) 
					{
						e.printStackTrace();
					}
					
					String concatOfTags = "";
					
					//put the tags of all records in concatOfTags string
					for(int j = 0; j < recordsWithTags.size(); j++)
					{
						if((recordsWithTags.get(j).mUserData1 != null)&&(!recordsWithTags.get(j).mUserData1.equals("")))
							concatOfTags += recordsWithTags.get(j).mUserData1+" ";
					}
					Log.d("StoreAndForwardService", "postTags()--> concat of tags: " +concatOfTags);
					
					try 
					{
						String[] tags = concatOfTags.split(" ");
						JSONArray tagsArray = new JSONArray();
						if((tags != null)&&(tags.length > 0))
						{
							for(int k = 0; k < tags.length; k++)
							{
								if(!tags[k].equals(""))
									tagsArray.put(k, tags[k]);
							}
						}
						object.put("tags", tagsArray);
						//object.put("tags_cause", null);
						//object.put("tags_location", null);
						//object.put("tags_perception", null);
					} 
					catch (JSONException e) 
					{
						e.printStackTrace();
					}
					sb.append(object.toString());	
					sb.append("]");				
								
					//Log.d("StoreAndForwardService", "]");	
					Log.d("StoreAndForwardService", "postTags()--> json to string: " +sb.toString());
				
					byte[] contentGzippedBytes = zipStringToBytes(sb.toString());
					ByteArrayEntity byteArrayEntity = new ByteArrayEntity(contentGzippedBytes);
					
					byteArrayEntity.setChunked(false); //IMPORTANT: must put false for smartcity.csp.it server

					//IMPORTANT: do not set the content-Length, because is embedded in Entity
					//httpPost.setHeader("Content-Length", byteArrayEntity.getContentLength()+"");
				    
					httpPost.setEntity(byteArrayEntity);
				    
				    Log.d("StoreAndForwardService", "postTags()--> Content length: " +httpPost.getEntity().getContentLength());
					Log.d("StoreAndForwardService", "postTags()--> Method: " +httpPost.getMethod());
				
					//do http post (it performs asynchronously)
					HttpResponse response = httpClient.execute(httpPost);
					
					sb = null;
					
					//server response
					//String responseBody = EntityUtils.toString(response.getEntity());
					//Log.d("StoreAndForwardService", "postTags()--> response: " +responseBody);
					Log.d("StoreAndForwardService", "postTags()--> status line: " +response.getStatusLine());   
					
					httpClient.getConnectionManager().shutdown();
					        
					//server response, status line
					StatusLine statusLine = response.getStatusLine();
					int statusCode = statusLine.getStatusCode(); 	
					
					if(statusCode == Constants.STATUS_OK)
					{
						Log.d("StoreAndForwardService", "postTags()--> STATUS OK");
						mDbManager.deleteRecordsWithTagsBySessionId(sessionId);					
					}
					else
						Log.d("StoreAndForwardService", "postTags()--> status error code: "+statusCode);
				}
				else
					Log.d("StoreAndForwardService", "postTags()--> no tags to send");
			}
		}
	}		
	
	//create an array of json objects containing only tags, compress it in gzip http compression format and send it to server
	public void postSecureTags() throws IllegalArgumentException, ClientProtocolException, HttpHostConnectException, IOException
	{			
		Log.d("StoreAndForwardService", "postSecureTags()");
		
		int sepIndex = 1; //default is '.' separator (see Constants.separators array)
		if((Utils.report_country != null)&&(Utils.report_country.equals("IT")))
			sepIndex = 0; //0 is for '-' separator (for italian CSP server)
		
		List<String> sids = mDbManager.getSidsOfRecordsWithTags();

		if((sids != null)&&(sids.size() > 0))
		{
			for(int i = 0; i < sids.size(); i++)
			{
				String sessionId = (String)sids.get(i);
						
				//load records containing user tags with actual session id
				List<Record>recordsWithTags = mDbManager.loadRecordsWithTagBySessionId(sessionId);
				
				if((recordsWithTags != null)&&(recordsWithTags.size() > 0))
				{
					//get size of array of records containing tags and reference to the last record
					int size = recordsWithTags.size();
					
					//obtain reference to the last record of serie
					Record lastToSendRecord = recordsWithTags.get(size-1);
					Log.d("StoreAndForwardService", "postSecureTags()--> # of records containing tags: "+size);
					
					//save timestamp of last record containing tags
					long lastTimestamp = 0;		
					if(lastToSendRecord.mSysTimestamp > 0)
						lastTimestamp = lastToSendRecord.mSysTimestamp;
					else
						lastTimestamp = lastToSendRecord.mBoxTimestamp;
					
					String lastTsFormatted = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSSz", Locale.US).format(new Date(lastTimestamp));

					//********* MAKING OF HTTP HEADER **************

					URL url = new URL(Utils.report_url);
					HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
				    
				    con.setRequestMethod("POST");
				    con.setUseCaches(false);
				    con.setDoInput(true);
					con.setDoOutput(true);
					
					con.setRequestProperty("Content-Encoding", "gzip");
					con.setRequestProperty("Content-Type", "application/json");
					con.setRequestProperty("Accept", "application/json");
					con.setRequestProperty("User-Agent", "AirProbe"+Utils.appVer);

				    //******** authorization bearer header ********
				    
					if(Utils.getAccountActivationState(getApplicationContext()))
						con.setRequestProperty("Authorization", "Bearer "+Utils.getAccessToken(getApplicationContext()));
					else if(Utils.getAccountActivationStateForClient(getApplicationContext()))
						con.setRequestProperty("Authorization", "Bearer "+Utils.getAccessTokenForClient(getApplicationContext()));
					
				    //******** meta header (for new version API V1)
				    
					con.setRequestProperty("meta"+Constants.separators[sepIndex]+"timestampRecorded", lastTsFormatted);
					con.setRequestProperty("meta"+Constants.separators[sepIndex]+"deviceId", Utils.deviceID);
					con.setRequestProperty("meta"+Constants.separators[sepIndex]+"installId", Utils.installID);
					
				    //******** data header (for new version API V1)
				    
					//con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedPacketId", "");
					//con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedPacketPointId", "");
					//con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedSessionId", ""); //deprecated from AP 1.4
				    
					con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"type", "airprobe_tags");
					con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"format", "json");
					//con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"specification", "at-3");	//update by increment this field on header changes    
				    if(size > 1)
				    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "true");
				    else
				    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"list", "false");	    
				    con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"listSize", String.valueOf(size));
				    
				    /* from AP 1.4 */
				    if((lastToSendRecord.mBoxMac != null)&&(!lastToSendRecord.mBoxMac.equals("")))
				    {
				    	Log.d("StoreAndForwardService", "postSecureData()--> box mac address: "+lastToSendRecord.mBoxMac);
				    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceId", lastToSendRecord.mBoxMac);
				    }
				    else
				    	con.setRequestProperty("meta"+Constants.separators[sepIndex]+"sourceId", Utils.getDeviceAddress(getApplicationContext()));
				    
				    con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedSourceSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSourceSessionSeed);
				    con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedSourceSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSourceSessionNumber));
				    if((lastToSendRecord.mSemanticSessionSeed != null)&&(!lastToSendRecord.mSemanticSessionSeed.equals("")))
				    {
				    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedSemanticSessionId"+Constants.separators[sepIndex]+"seed", lastToSendRecord.mSemanticSessionSeed);
				    	con.setRequestProperty("data"+Constants.separators[sepIndex]+"extendedSemanticSessionId"+Constants.separators[sepIndex]+"number", String.valueOf(lastToSendRecord.mSemanticSessionNumber));
				    }
				    
				    con.setRequestProperty("data"+Constants.separators[sepIndex]+"contentDetails"+Constants.separators[sepIndex]+"typeVersion", "30"); //update by increment this field on header changes				    
				    /* end of from AP 1.4 */
				    
				    //******** geo header (for new version API V1)
				    
				    //add the right provider to header
				    if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[0])) //sensor box
				    {
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mBoxLon));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mBoxLat));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }
				    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[1])) //phone
				    {
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mPhoneLon));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mPhoneLat));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mPhoneAcc));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mPhoneAltitude)); //from AP 1.4
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mPhoneSpeed)); //from AP 1.4
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mPhoneBear)); //from AP 1.4
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }
				    else if(lastToSendRecord.mGpsProvider.equals(Constants.GPS_PROVIDERS[2])) //network
				    {
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"longitude", String.valueOf(lastToSendRecord.mNetworkLon));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"latitude", String.valueOf(lastToSendRecord.mNetworkLat));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"accuracy", String.valueOf(lastToSendRecord.mNetworkAcc));
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"altitude", String.valueOf(lastToSendRecord.mNetworkAltitude)); //from AP 1.4
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"speed", String.valueOf(lastToSendRecord.mNetworkSpeed)); //from AP 1.4
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"bearing", String.valueOf(lastToSendRecord.mNetworkBear)); //from AP 1.4	 	     
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"provider", lastToSendRecord.mGpsProvider);
				    	con.setRequestProperty("geo"+Constants.separators[sepIndex]+"timestamp", lastTsFormatted);
				    }  
				    
				    //******** MAKING OF HTTP CONTENT (JSON) *************
				    
				    //writing string content as an array of json object
				    StringBuilder sb = new StringBuilder();
					sb.append("[");
					
					JSONObject object = new JSONObject();
					
					try 
					{
						object.put("timestamp", lastTimestamp);
						
						JSONArray locations = new JSONArray();
						
						//sensor box gps data
						if(lastToSendRecord.mBoxLat != 0)
						{
							JSONObject boxLocation = new JSONObject();
							boxLocation.put("latitude", lastToSendRecord.mBoxLat);
							boxLocation.put("longitude", lastToSendRecord.mBoxLon);
							//boxLocation.put("accuracy", "null");
							//boxLocation.put("altitude", "null");
							//boxLocation.put("speed", "null");
							//boxLocation.put("bearing", "null");
							boxLocation.put("provider", Constants.GPS_PROVIDERS[0]);
							boxLocation.put("timestamp", lastToSendRecord.mBoxTimestamp);
							
							locations.put(0, boxLocation);
						}
						
						//phone gps data
						if(lastToSendRecord.mPhoneLat != 0)
						{
							JSONObject phoneLocation = new JSONObject();
							phoneLocation.put("latitude", lastToSendRecord.mPhoneLat);
							phoneLocation.put("longitude", lastToSendRecord.mPhoneLon);
							phoneLocation.put("accuracy", lastToSendRecord.mAccuracy);
							phoneLocation.put("altitude", lastToSendRecord.mPhoneAltitude);
							phoneLocation.put("speed", lastToSendRecord.mPhoneSpeed);
							phoneLocation.put("bearing", lastToSendRecord.mPhoneBear);
							phoneLocation.put("provider", Constants.GPS_PROVIDERS[1]);
							phoneLocation.put("timestamp", lastToSendRecord.mPhoneTimestamp);
							
							locations.put(1, phoneLocation);
						}
						
						//network gps data
						if(lastToSendRecord.mNetworkLat != 0)
						{
							JSONObject netLocation = new JSONObject();
							netLocation.put("latitude", lastToSendRecord.mNetworkLat);
							netLocation.put("longitude", lastToSendRecord.mNetworkLon);
							netLocation.put("accuracy", lastToSendRecord.mNetworkAcc);
							netLocation.put("altitude", lastToSendRecord.mNetworkAltitude);
							netLocation.put("speed", lastToSendRecord.mNetworkSpeed);
							netLocation.put("bearing", lastToSendRecord.mNetworkBear);				
							netLocation.put("provider", Constants.GPS_PROVIDERS[2]);
							netLocation.put("timestamp", lastToSendRecord.mNetworkTimestamp);

							locations.put(2, netLocation);
						}
						
						object.put("locations", locations);
					}
					catch (JSONException e) 
					{
						e.printStackTrace();
					}
					
					String concatOfTags = "";
					
					//put the tags of all records in concatOfTags string
					for(int j = 0; j < recordsWithTags.size(); j++)
					{
						if((recordsWithTags.get(j).mUserData1 != null)&&(!recordsWithTags.get(j).mUserData1.equals("")))
							concatOfTags += recordsWithTags.get(j).mUserData1+" ";
					}
					Log.d("StoreAndForwardService", "postSecureTags()--> concat of tags: " +concatOfTags);
					
					try 
					{
						String[] tags = concatOfTags.split(" ");
						JSONArray tagsArray = new JSONArray();
						if((tags != null)&&(tags.length > 0))
						{
							for(int k = 0; k < tags.length; k++)
							{
								if(!tags[k].equals(""))
									tagsArray.put(k, tags[k]);
							}
						}
						object.put("tags", tagsArray);
						//object.put("tags_cause", null);
						//object.put("tags_location", null);
						//object.put("tags_perception", null);
					} 
					catch (JSONException e) 
					{
						e.printStackTrace();
					}
					sb.append(object.toString());	
					sb.append("]");				

					Log.d("StoreAndForwardService", "postSecureTags()--> json to string: " +sb.toString());
					
					//compress json content into byte array entity
					byte[] contentGzippedBytes = zipStringToBytes(sb.toString());
	
				    //write json compressed content into output stream
					OutputStream outputStream = con.getOutputStream();
					outputStream.write(contentGzippedBytes);
					outputStream.flush();
					outputStream.close();
					
					int responseCode = con.getResponseCode();
					
					Log.d("StoreAndForwardService", "postSecureTags()--> response code: " +responseCode);

					sb = null;

					if(responseCode == Constants.STATUS_OK)
					{
						Log.d("StoreAndForwardService", "postSecureTags()--> STATUS OK");
						mDbManager.deleteRecordsWithTagsBySessionId(sessionId);					
					}
					else
						Log.d("StoreAndForwardService", "postSecureTags()--> status error code: "+responseCode);					
				}
				else
					Log.d("StoreAndForwardService", "postTags()--> no tags to send");
			}
		}
	}		
	
	 public static byte [] zipStringToBytes (String input) throws IOException
	 {
	     ByteArrayOutputStream bos = new ByteArrayOutputStream ();
	     //BufferedOutputStream buffs = new BufferedOutputStream (new GZIPOutputStream (bos));
	     
	     //use PrintStream to avod converting a String into byte array, that can cause out of memory error
	     final PrintStream printStream = new PrintStream(new GZIPOutputStream (bos));
	     printStream.print(input);
	     printStream.close();
	     
	     //buffs.write (input. getBytes ());
	     //buffs.close ();
	     
	     byte [] retval = bos.toByteArray ();
	     bos.close ();
	     return retval;
	 } 
	 
	 //check if actual access token is valid: creation time + expiresIn field must be > actual time
	 public boolean checkIfAccessTokenIsValid()
	 {		 
		 long creationTime = Utils.getCreationTime(getApplicationContext());
		 if(creationTime < 0)
			 return false;
		 
		 long expiresIn = Utils.getExpiresIn(getApplicationContext());
		 if(expiresIn < 0)
			 return false;
		 expiresIn -= 1000;
		 
		 long actualTime = System.currentTimeMillis() / 1000; //not valid date?
		 if(actualTime <= 0)
		 	 return false;
		 
		 //Log.d("StoreAndForwardService", "checkIfAccessTokenIsValid()--> creationTime: "+creationTime+" expiresIn: "+expiresIn+" actualTime: "+actualTime);
		 
		 if(creationTime + expiresIn > actualTime)	 
		 {
			 Log.d("StoreAndForwardService", "checkIfAccessTokenIsValid()--> STILL VALID ACCESS TOKEN - creationTime + expiresIn "+(creationTime+expiresIn)+" actualTime: "+actualTime);
			 return true;
		 }
	     else
			 return false;
	 }
	 
	 //check if actual access token is valid: creation time + expiresIn field must be > actual time
	 public boolean checkIfAccessTokenIsValidForClient()
	 {		 
		 long creationTime = Utils.getCreationTimeForClient(getApplicationContext());
		 if(creationTime < 0)
			 return false;
		 
		 long expiresIn = Utils.getExpiresInForClient(getApplicationContext());
		 if(expiresIn < 0)
			 return false;
		 expiresIn -= 1000;
		 
		 long actualTime = System.currentTimeMillis() / 1000; //not valid date?
		 if(actualTime <= 0)
			 return false;
		 
		 //Log.d("StoreAndForwardService", "checkIfAccessTokenIsValidForClient()--> creationTime: "+creationTime+" expiresIn: "+expiresIn+" actualTime: "+actualTime);
		 
		 if(creationTime + expiresIn > actualTime)	 
		 {
			 Log.d("StoreAndForwardService", "checkIfAccessTokenIsValidForClient()--> STILL VALID ACCESS TOKEN - creationTime + expiresIn "+(creationTime+expiresIn)+" actualTime: "+actualTime);
			 return true;
		 }
	     else
			 return false;
	 }
}
