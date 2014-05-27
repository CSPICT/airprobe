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

package org.csp.everyaware.fragments;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.csp.everyaware.Constants;
import org.csp.everyaware.R;
import org.csp.everyaware.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentWizardStep2  extends Fragment 
{
	private EditText mUserEt;
	private EditText mPswdEt;
	private TextView mOpResTv;
	private Button mActivateBtn;
	
	private boolean mUserMod;
	private boolean mPswdMod;
	
	//metodo statico di tipo factory per istanziare il fragment e passare i parametri  come bundle argument
    public static FragmentWizardStep2 newInstance()
    {
    	Log.d("FragmentWizardStep2", "newInstance()");
    	
    	FragmentWizardStep2 newFragment = new FragmentWizardStep2();

    	return newFragment;
    }	
    
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
    {   	
    	Log.d("FragmentWizardStep2", "onCreateView()"); 	
    	
    	//inflating del layout del fragment
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_wizard_step2, container, false);
        
        mUserEt = (EditText)rootView.findViewById(R.id.usernameEt);
        mPswdEt = (EditText)rootView.findViewById(R.id.passwordEt);
        mOpResTv = (TextView)rootView.findViewById(R.id.operationResultTv);
        mActivateBtn = (Button)rootView.findViewById(R.id.activateAccountBtn);
        
        mActivateBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				if(!mUserMod || !mPswdMod)
				{
					Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.fill_both_field_str), Toast.LENGTH_LONG).show();
			    	mOpResTv.setText("");
					return;
				}
				mActivateBtn.setEnabled(false);
				
				String[] credentials = new String[2];
				
				credentials[0] = mUserEt.getText().toString();
				credentials[1] = mPswdEt.getText().toString();
				
				Log.d("OnClickListener", "onClick()--> credentials: "+credentials[0]+" "+credentials[1]);
				
				//is internet connection available? 
				boolean[] connectivity = Utils.haveNetworkConnection(getActivity().getApplicationContext());				
				boolean connectivityOn = connectivity[0] || connectivity[1];
				
				if(connectivityOn)
					new ActivateAccountTask().execute(credentials);
				else
				{
					mActivateBtn.setEnabled(true);
					Toast.makeText(getActivity().getApplicationContext(), R.string.alert_dialog_no_internet, Toast.LENGTH_LONG).show();
				}
			}      	
        });
        
        mUserMod = false;
        
        mUserEt.addTextChangedListener(new TextWatcher()
        {
			@Override
			public void afterTextChanged(Editable s) 
			{
				if(mUserEt.getText().toString().length() > 0)
					mUserMod = true;
				else
					mUserMod = false;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}        	
        });
        
        mPswdMod = false;
        
        mPswdEt.addTextChangedListener(new TextWatcher()
        {
			@Override
			public void afterTextChanged(Editable s) 
			{
				if(mPswdEt.getText().toString().length() > 0)
					mPswdMod = true;
				else
					mPswdMod = false;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}      	
        });
        
        return rootView; 
    }
    
    @Override
    public void onDestroyView()
    {
    	super.onDestroyView();
    	Log.d("FragmentWizardStep2", "onDestroyView()");
    }
    
	private class ActivateAccountTask extends AsyncTask<String, Void, Boolean>
	{
		boolean success = false;
		
		@Override
		protected Boolean doInBackground(String... params) 
		{
			try 
			{		
				//doHttpPost(params[0], params[1]);
				
				doSecureHttpPost(params[0], params[1]);
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
		
		@Override
		protected void onPostExecute(Boolean success)
		{
			Log.d("ActivateAccountTask", "onPostExecute()");
			
		    //if activation process ends successfully, let action button disabled, else
			//re-enable it
		    mActivateBtn.setEnabled(!success);
		    
		    if(success)
		    {
		    	mOpResTv.setTextColor(getResources().getColor(R.color.green));
		    	mOpResTv.setText(R.string.activation_successfull);
		    	mUserEt.setEnabled(false);
		    	mPswdEt.setEnabled(false);
		    	
		    	new Handler().postDelayed(new Runnable()
		    	{
					@Override
					public void run() 
					{
						if(getActivity() != null)
						{
							getActivity().getSupportFragmentManager().beginTransaction().remove(FragmentWizardStep2.this).commit();
							getActivity().finish();
						}
					}
		    		
		    	}, 1000);
		    }
		    else
		    {
		    	mOpResTv.setTextColor(getResources().getColor(R.color.red));
		    	mOpResTv.setText(R.string.error_during_activation);
		    	mPswdEt.setText("");
		    	mPswdEt.setHint(getResources().getString(R.string.your_password_str));
		    }
		}
		
		//makes a not secure (http://) post with hidden parameters
		//not used but do not delete it
		private void doHttpPost(String username, String password) throws ClientProtocolException, IOException, JSONException, OutOfMemoryError, IllegalArgumentException
		{
			int statusCode = -1;
			
			//http post to hide sent params	
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(Constants.LOGIN_URL);
			
		    // Add  data
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		    nameValuePairs.add(new BasicNameValuePair("grant_type", "password"));
		    nameValuePairs.add(new BasicNameValuePair("client_id", "airprobe_android_client"));
		    nameValuePairs.add(new BasicNameValuePair("client_secret", Constants.SECRET_KEY));
		    nameValuePairs.add(new BasicNameValuePair("username", username));		
		    nameValuePairs.add(new BasicNameValuePair("password", password));	
		    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		    // Execute HTTP Post Request
		    HttpResponse response = httpClient.execute(httpPost);
		        
			//copio la entity response su output stream, ottenendo una stringa
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    response.getEntity().writeTo(out);
		    String responseString = out.toString();
		    Log.d("ActivateAccountTask", "doHttpPost()--> "+responseString);
		    	
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
				    long expiresIn = json.getLong("expires_in");
						    
				    Log.d("ActivateAccountTask", "doHttpPost()--> "+accessToken+" "+refreshToken+" "+tokenType+" "+expiresIn);	
						    
				    //save account credentials in shared preferences
				    Utils.setCredentialsData(getActivity(), accessToken, refreshToken, tokenType, expiresIn, System.currentTimeMillis() / 1000);
				    //air probe is now activated!
				    Utils.setAccountActivationState(getActivity(), true);
						    
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
						    
				    Log.d("ActivateAccountTask", "doHttpPost()--> "+error+" "+errorDescr);
						    
				    success = false;
			    } 		
			}			        
		}
		
		//makes a secure (https://) http post
		private void doSecureHttpPost(String username, String password) throws ClientProtocolException, IOException, JSONException, OutOfMemoryError, IllegalArgumentException
		{			
			String urlString = Constants.LOGIN_URL;
			String urlParameters = "?grant_type=password&client_id=airprobe_android_client&client_secret="+Constants.SECRET_KEY+"&username="+username+"&password="+password;
			
			URL url = new URL(urlString+urlParameters);
			//URL url = new URL(Constants.REFRESH_TOKEN_URL);
		    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
		    
		    con.setRequestMethod("POST");
		    con.setUseCaches(false);
		    con.setDoInput(true);
			con.setDoOutput(true);

		    // Send post request (in this way it doesn't work)
			/*
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(urlParameters); //parameters here
			wr.flush();
			wr.close();
			*/
			
			int responseCode = con.getResponseCode();
			//Log.d("RefreshTokenTask", "doSecureHttpPost()--> Sending 'POST' request to URL : " + url);
			//Log.d("RefreshTokenTask", "doSecureHttpPost()--> Post parameters : " + urlParameters);
			Log.d("ActivateAccountTask", "doSecureHttpPost()--> Response Code : " + responseCode);

		    String responseString = getStringFromInputStream(con.getInputStream());
		    Log.d("ActivateAccountTask", "doSecureHttpPost()--> "+responseString);
		    
			//se la risposta dal server è 'HTTP 200 OK'
			if(responseCode == Constants.STATUS_OK)
			{
				 JSONObject json = new JSONObject(responseString);
				 String accessToken = json.getString("access_token");
				 String refreshToken = json.getString("refresh_token");
				 String tokenType = json.getString("token_type");				    	
				 long expiresIn = json.getLong("expires_in");
						    
				 Log.d("ActivateAccountTask", "doSecureHttpPost()--> "+accessToken+" "+refreshToken+" "+tokenType+" "+expiresIn);	
						    
				 //save account credentials in shared preferences
				 Utils.setCredentialsData(getActivity(), accessToken, refreshToken, tokenType, expiresIn, System.currentTimeMillis() / 1000);
				 //air probe is now activated!
				 Utils.setAccountActivationState(getActivity(), true);
						    
				 success = true;
			}
			else
			{
				JSONObject json = new JSONObject(responseString);
				String error = json.getString("error");
				String errorDescr = json.getString("error_description");
						    
				Log.d("ActivateAccountTask", "doSecureHttpPost()--> "+error+" "+errorDescr);
						    
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
	
	public OutputStream copyInputStreamToOutputStream(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[1024];
		int len = in.read(buffer);
		while (len != -1) {
		    out.write(buffer, 0, len);
		    len = in.read(buffer);
		}
		
		return out;
	}
}
