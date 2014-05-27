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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import org.csp.everyaware.db.Record;
import org.csp.everyaware.db.Track;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class Utils 
{	
	//semanticSessionSeed is installID
	public static int semanticSessionNumber;
	public static int semanticStartPointNumber;
	public static boolean semanticWindowStatus;

	public static int sourceSessionNumber;
	public static int sourcePointNumber;
	
	public static Intent storeForwServIntent;	
	public static Intent gpsTrackServIntent;
	
	public static ProgressDialog connProgressDialog;
	
	public static int counterHR; //count of received history records
	public static int numberHR; //number of history records on sd card of sensor box
	public static int lostHR; //number of lost history records
	
	//public static int uploadedRecords = 0; //total number of uploaded records to server (from the start of app session)
	public static boolean historyDownloadMode;	
	
	public static Record lastSavedRecord = null;
	public static Location lastPhoneLocation; //contains last gps location from phone gps provider
	public static Location lastNetworkLocation; //containes last gps location from network gps provider
	public static double[] firstAvailableLocation = {0.0, 0.0}; //containes the first gps available location (from sensorbox, phone or network)
	public static String nearestCityName = "";
	
	public static Track track = null;
	public static int selectedTrackIndex = -1;
	
	public static String report_url = null; //url to send data (official server, italian CSP server)
	public static String report_country = null; //DE, IT
	
	public static int uploadOn; 
	public static int btConnectionOn;
	
	public static String deviceID = "";
	public static String installID = "";

	public static String appVer = "";

	public static boolean paused = false;
	public static boolean backToHome = false;
	
	/******************* vars useful to calculate black carbon level *************************************************/
	
	public static boolean calibrationDataLoaded = false;
	public static boolean mac_recognized = false;
	
	//********* 1 - MODEL PARAMETERS - they are the same for each sensor box ***************
	
	//parameters of hidden layer
	public static double[] b1 = new double[10]; //vector of 10 elements (10 sensors)	
	public static double[][] w1 = new double[10][10]; //matrix of 10*10 elements (10 sensors)
	
	//output layer weights
	public static double[] w2 = new double[10];
	public static double b2;
	
	//********* 2 - SCALING DATA - they are different for each sensor box ******************
	
	//max_matrix and min_matrix contain row and columns from max/min.txt files
	public static double[][] max_matrix;
	public static double[][] min_matrix;
	public static int actual_sb_index = 0;
	
	//max and min are vectors that correspond to a single column from max/min.txt file. This columns correspond to the mac address
	//of the actual connected sensor box
	public static double[] max;
	public static double[] min;
	
	public static String[] addresses;
	
	//********* 3 - measurements and averages ********************
	
	public static double[][] measurements = new double[60][10]; //60 seconds of measurements
	public static double[] avg = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //averages over measurements
	
	public static double[] scaled_values = new double[10]; //scaled values for #1-8 pollution sensors and #9-10 temp/hum sensors
			
	//************** 4 - NUMBER OF ITERATIONS ***********************
	
	//number of records on which air quality index can be calculated
	public final static int NUM_OF_ITERATIONS = 60; //optimal number of iterations to calculate bc
	public static int iterations_counter = 0;
	public static final int shiftStep = 5;
	public static final int MIN_NUM_OF_ITERATIONS_INDEX = 9; //minimum number of iterations to calculate bc
	public static boolean is_optimal_calc = false;
	
	//************** 5 - BLACK CARBON VALUE *************************
	
	public static double bc;
	public static double bc_cumulative = 0;
	
	//************** 6 - IS BLACK CARBON READY TO BE SHOWN TO USER? ****************
	
	public static boolean show_bc = false;
	public static boolean show_bc_cumulative = false;
	
	/*****************************************************************************************************************/
	
	public static String generateRandomString(int length)
	{
		final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();
		
		StringBuilder sb = new StringBuilder( length );
		for( int i = 0; i < length; i++ ) 
			sb.append( alphabet.charAt( rnd.nextInt(alphabet.length()) ));
		
		return sb.toString();
	}
	
	public static void initCounters()
	{
		counterHR = 0;
		numberHR = 0;
		lostHR = 0;
		//uploadedRecords = 0;
		
		lastSavedRecord = null;
		lastPhoneLocation = null;
		lastNetworkLocation = null;
		
		iterations_counter = 0;
		bc = 0;
		show_bc = false;
		is_optimal_calc = false;
		mac_recognized = false;
		calibrationDataLoaded = true;
		report_url = null;
		
		uploadOn = Constants.INTERNET_OFF_INT;
		btConnectionOn = Constants.STATE_CONNECTED;
		
		firstAvailableLocation[0] = 0.0;
		firstAvailableLocation[1] = 0.0;
		nearestCityName = "";
	}
	
	//algorithm to calculate air quality index, by Alina Sirbu
	public static void calcBlackCarbon(double[] newValues)
	{
		boolean D = false;
		
		if((!calibrationDataLoaded)||(!mac_recognized))
		{
			if(D)
			{
				if(!calibrationDataLoaded)
					Log.d("Utils", "calcAirQualityIndex()--> calibration data not loaded!");
				if(!mac_recognized)
					Log.d("Utils", "calcAirQualityIndex()--> sensor box mac address not recognized!");
			}
			return;
		}
		
		bc_cumulative += bc;
		
		//save actual 10 values from sensor 
		if(iterations_counter < NUM_OF_ITERATIONS - 1)
		{
			int l = newValues.length;
			
			//fixed_values is a vector of 10 elements containing values from sensor (8 pollutation sensors + temp + hum) in which o3 and voc values
			//are switched: from voc - o3 order to o3 - voc
			/*
			double fixed_values[] = new double[l-2]; 
			
			for(int i = 2; i < l; i++ ) //from 2 to 11
			{
				if(i == 8)
					fixed_values[i-2] = newValues[i+1]; //copy o3 into voc
				else if(i == 9)
					fixed_values[i-2] = newValues[i-1]; //copy voc into o3
				else
					fixed_values[i-2] = newValues[i];

				//save values from 10 sensors
				measurements[iterations_counter][i-2] = fixed_values[i-2];
			}*/
			
			//AT EVERY CYCLE EXECPT THE CYCLE ON WHICH OPTIMAL BC VALUE IS CALCULATED, THIS IS DONE
			//copy newValues vector starting from index pos == 2 to the end into measurements matrix at the
			//iteration_counter row (voc - o3 sensors order is implemented)
			for(int i = 2; i < l; i++) //from 2 to 11
			{
				measurements[iterations_counter][i-2] = newValues[i];
				if(D)
					Log.d("Utils", "calcAirQualityIndex()--> measurements["+iterations_counter+"]["+(i-2)+"] = newValues["+i+"] = "+measurements[iterations_counter][i-2]);
			}
								/***/
			
			//AFTER 10 MEASUREMENTS, CALCULATE BLACK CARBON (NOT OPTIMAL PHASE)
			if(!is_optimal_calc)
			{
				//if number of iterations is >= 10, we can calculate the bc (not optimal!)
				//iterations_counter starts from 0, so it must be compared to MIN_NUM_OF_ITERATIONS_INDEX-1 == 9
				if((iterations_counter >= MIN_NUM_OF_ITERATIONS_INDEX-1)&&( (iterations_counter+1) % shiftStep) == 0)
				{
					int rows = iterations_counter;
					int columns = measurements[0].length;
					
					if(D)
						Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC measurements matrix # rows: " +rows+ " # columns: " +columns);
					
					//1 - calculate average vector
					for(int i = 0; i < columns; i++)
					{
						for(int j = 0; j < rows; j++)
						{
							//Log.d("Utils", "calcAirQualityIndex()--> column: " +i+ " # row: " +j);
							avg[i] += measurements[j][i];		
						}
						avg[i] = avg[i] / rows;
						
						if(D)
							Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC avg["+i+"] = " +avg[i]+" rows: " +rows);
					}
					
					//2 - scale pollutant avg (first 8 values of avg vector)
					for(int i = 0; i < 8; i++)
					{
						if(D)
							Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC ((avg["+i+"] - min["+i+"]) / (max["+i+"] - min["+i+"])) = (("+avg[i]+" - "+min[i]+") / ("+max[i]+" - "+min[i]+"))");
						
						scaled_values[i] = ((avg[i] - min[i]) / (max[i] - min[i])) * 0.5 + 0.2;
					}
					//3 - scale temp and hum avg (last 2 values of avg vector)
					for(int i = 8; i < 10; i++)
					{
						scaled_values[i] = ((avg[i] - min[i]) / (max[i] - min[i])) * 0.3 + 0.1;
					}
					
					//4 - apply neural network on scaled values (ann = artificial neural network)
					
					double ann = 0;
					
					double outerSum = 0;
					for(int j = 0; j < w1.length; j++)
					{	
						double innerSum = 0;			
						//inner sum cycle, it's the sum of products between each value inscaled_values vector and corresponding value on j-row of w1
						//(w1 is a matrix composed by the [2,11] rows of modelParams.txt
						for(int i = 0; i < scaled_values.length; i++)
						{
							innerSum += w1[j][i] * scaled_values[i];
							
							if(D)
								Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC w1["+j+"]["+i+"] = " +w1[j][i]+ " scaled values["+i+"] = " +scaled_values[i]+ " w1["+j+"]["+i+"] * scaled_values["+i+"] = " +w1[j][i] * scaled_values[i] + "  innerSum: " +innerSum);
						}
						
						//add to innerSum the j-value of b1 vector (b1 is a vector composed by first row of modelParams.txt), calculate tanh function on result and multiply this for j-value of w2 (w2 is a vector composed by
						//13th row of modelParams.txt)
	
						outerSum += w2[j] * Math.tanh(b1[j] + innerSum);
						
						if(D)
							Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC w2["+j+"] = " +w2[j]+ " b1["+j+"] = "+b1[j]+ " Math.tanh(b1["+j+"] + innerSum) = "+Math.tanh(b1[j] + innerSum)+ " outerSum: "+outerSum);
					}
					
					ann = Math.tanh(b2 + outerSum);
					
					if(D)
						Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC b2 = "+b2+ " outerSum = "+outerSum+ " ann: " +ann);
					
					//5 - scale ann to real black carbon value (this is the value to be shown to the user)		
					//this bc value is expressed in nanograms per cube meter. To get micrograms per cube meter, divide it per 1000
					bc = (21441.63 * ((ann + 1)/2)); 
					int bc_int = (int)(bc*1000); //e.g. from 9023.6784353 to 9023678.4353 to 9023678							
					bc = (double)bc_int / 10; //from 9023678 to 902367.8				
					bc = (double)Math.round(bc) / 100000; //from 902367.8 to 902368 to 9.02368 (from nanograms to micrograms per cube meter)
					
					show_bc = true; //to enable GUI to show black carbon level to the user
					
					if(D)
						Log.d("Utils", "calcAirQualityIndex()--> NOT OPTIMAL CALC black carbon: " +bc);
					
					//reset avg vector
					for(int i = 0; i < avg.length; i++)
						avg[i] = 0;
				}
			} // end of 'if(!is_optimal_calc)'
			
			iterations_counter++;
			return;
		}
		
									/***/
		
		//******** AFTER 60 measurements, calculate black carbon (optimal phase)!!! ***********

		is_optimal_calc = true;
		
		int rows = measurements.length;
		int columns = measurements[0].length;
		
		//copy newValues vector starting from index pos == 2 to the end into measurements matrix at the
		//iteration_counter row (voc - o3 sensors order is implemented)
		//at this point, iterations_counter must be 58
		for(int i = 2; i < newValues.length; i++) //from 2 to 11
		{
			measurements[iterations_counter][i-2] = newValues[i];
			if(D)
				Log.d("Utils", "calcAirQualityIndex()--> OPTIMAL BC CYCLE - measurements["+iterations_counter+"]["+(i-2)+"] = newValues["+i+"] = "+measurements[iterations_counter][i-2]);
		}
		
		iterations_counter++;
		
		if(D)
		{
			String str = "";
			for(int i = 0; i < rows; i++)
			{
				str = "";
				
				for(int j = 0; j < columns; j++)
					str += measurements[i][j] + " ";
				
				Log.d("Utils", "calcAirQualityIndex()--> row["+i+"] = " +str);
			}	
			Log.d("Utils", "calcAirQualityIndex()--> measurements matrix # rows: " +rows+ " # columns: " +columns);
		}
		
		//1 - calculate average vector
		for(int i = 0; i < columns; i++)
		{
			for(int j = 0; j < rows; j++)
			{
				//Log.d("Utils", "calcAirQualityIndex()--> column: " +i+ " # row: " +j);
				avg[i] += measurements[j][i];		
			}
			avg[i] = avg[i] / rows;
			
			if(D)
				Log.d("Utils", "calcAirQualityIndex()--> avg["+i+"] = " +avg[i]+" rows: " +rows);
		}
		
		//2 - scale pollutant avg (first 8 values of avg vector)
		for(int i = 0; i < 8; i++)
		{
			if(D)
				Log.d("Utils", "calcAirQualityIndex()--> ((avg["+i+"] - min["+i+"]) / (max["+i+"] - min["+i+"])) = (("+avg[i]+" - "+min[i]+") / ("+max[i]+" - "+min[i]+"))");
			
			scaled_values[i] = ((avg[i] - min[i]) / (max[i] - min[i])) * 0.5 + 0.2;
		}
		//3 - scale temp and hum avg (last 2 values of avg vector)
		for(int i = 8; i < 10; i++)
		{
			scaled_values[i] = ((avg[i] - min[i]) / (max[i] - min[i])) * 0.3 + 0.1;
		}
		
		if(D)
		{
			String str = "";
			for(int i = 0; i < 10; i++)
				str += scaled_values[i]+ " ";
			
			Log.d("Utils", "calcAirQualityIndex()--> scaled values = " +str);
			
		}
		
		//4 - apply neural network on scaled values (ann = artificial neural network)
		
		double ann = 0;
		
		double outerSum = 0;
		for(int j = 0; j < w1.length; j++)
		{	
			double innerSum = 0;			
			//inner sum cycle, it's the sum of products between each value inscaled_values vector and corresponding value on j-row of w1
			//(w1 is a matrix composed by the [2,11] rows of modelParams.txt
			for(int i = 0; i < scaled_values.length; i++)
			{
				innerSum += w1[j][i] * scaled_values[i];
				
				if(D)
					Log.d("Utils", "calcAirQualityIndex()--> w1["+j+"]["+i+"] = " +w1[j][i]+ " scaled values["+i+"] = " +scaled_values[i]+ " w1["+j+"]["+i+"] * scaled_values["+i+"] = " +w1[j][i] * scaled_values[i] + "  innerSum: " +innerSum);
			}
			
			//add to innerSum the j-value of b1 vector (b1 is a vector composed by first row of modelParams.txt), calculate tanh function on result and multiply this for j-value of w2 (w2 is a vector composed by
			//13th row of modelParams.txt)

			outerSum += w2[j] * Math.tanh(b1[j] + innerSum);
			
			if(D)
				Log.d("Utils", "calcAirQualityIndex()--> w2["+j+"] = " +w2[j]+ " b1["+j+"] = "+b1[j]+ " Math.tanh(b1["+j+"] + innerSum) = "+Math.tanh(b1[j] + innerSum)+ " outerSum: "+outerSum);
		}
		
		ann = Math.tanh(b2 + outerSum);
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> b2 = "+b2+ " outerSum = "+outerSum+ " ann: " +ann);
		
		//5 - scale ann to real black carbon value (this is the value to be shown to the user)		
		//this bc value is expressed in nanograms per cube meter. To get micrograms per cube meter, divide it per 1000
		bc = (21441.63 * ((ann + 1)/2)); 
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> black carbon(1): " +bc);

		int bc_int = (int)(bc*1000); //e.g. from 9023.6784353 to 9023678.4353 to 9023678
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> black carbon(2): " +bc_int);
		
		bc = (double)bc_int / 10; //from 9023678 to 902367.8
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> black carbon(3): " +bc);
		
		bc = (double)Math.round(bc) / 100000; //from 902367.8 to 902368 to 9.02368 (from nanograms to micrograms per cube meter)
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> black carbon(4): " +bc);
	
		show_bc = true; //to enable GUI to show black carbon level to the user
		
		if(D)
			Log.d("Utils", "calcAirQualityIndex()--> black carbon: " +bc);
		
		//6 - decrement iteration counter by five 
		iterations_counter -= shiftStep;
		
		//reset avg vector
		for(int i = 0; i < avg.length; i++)
			avg[i] = 0;
		
		//shift last 55 measurement to first 55 position (set last 5 rows to 0)
		shiftMeasurements(shiftStep);
	}
	
	public static void shiftMeasurements(int step)
	{
		boolean D = false;
		
		int rows = measurements.length;
		int columns = measurements[0].length;
		
		if(step >= rows)
			return;
		
		if(D)
		{
			String str = "";
			
			for(int i = 0; i < rows; i++)
			{
				str = "";
				for(int j = 0; j < columns; j++)
					str += measurements[i][j]+ " ";
				
				Log.d("Utils", "shiftMeasurements()--> measurements["+i+"] = " +str);
			}
		}
		
		for(int i = 0; i < rows - step; i++)
		{
			for(int j = 0; j < columns; j++)
				measurements[i][j] = measurements[i+step][j];
		}
		
		for(int i = rows - step; i < rows; i++)
			for(int j = 0; j < columns; j++)
				measurements[i][j] = 0;
		
		if(D)
		{
			String str = "";
			
			for(int i = 0; i < rows; i++)
			{
				str = "";
				for(int j = 0; j < columns; j++)
					str += measurements[i][j]+ " ";
				
				Log.d("Utils", "shiftMeasurements()--> measurements["+i+"] = " +str);
			}
		}
	}
	
	/**************** CREA DIRECTORY CON IL NOME DELL'APP ************************************************/
	
	public static void createAppDir()
	{
        File appDirectory = new File(Environment.getExternalStorageDirectory(), Constants.APP_NAME);
        if(!appDirectory.exists())
        {
        	appDirectory.mkdir();
        	Log.d("Utils", "Created " +appDirectory.toString());
        }
	}
	
	/************* TEST SE SD CARD E' INSTALLATA NELLO SMARTPHONE *****************************************/
	
	public static boolean isSdCardMounted()
	{
    	String state = android.os.Environment.getExternalStorageState();
    	if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  
    		return false;

    	return true;
	}
	
	/************ DATO TIMESTAMP IN MILLISEC, RESTITUISCE ORA *******************************************/
	
	public static String fromTimestampToTime(long timestamp)
	{
		return new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));
	}
	
    /************ DATO TIMESTAMP IN MILLISEC, RESTITUISCE DATA COMPLETA *********************************/
    
	public static String fromTimestampToCompleteDate(long timestamp)
	{
		return new SimpleDateFormat("HH:mm:ss - yyyy MM dd").format(new Date(timestamp));
	}
	
	/************ DATO TIMESTAMP IN MILLISEC, RESTITUISCE DATA *****************************************/
	
	public static String fromTimestampToDayDate(long timestamp)
	{
		return new SimpleDateFormat("yyyy MM dd").format(new Date(timestamp));
	}
	
	/************ READ APPLICATION VERSION NAME FROM MANIFEST ******************************************/
	
	public static String getAppVer(Activity activity)
	{
		try 
		{
			appVer = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
		} 
		catch (NameNotFoundException e) 
		{
			e.printStackTrace();
		}
		
		return appVer;
	}
	
	/****************** MANAGEMENT OF SEMANTIC SESSION DETAILS *******************/
	
	//returns the semantic session seed (as a random 20 alphanumeric chars string) and generated
	//one if it doesn't exist
	/*
	public static String getSemanticSessionSeed(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		semanticSessionSeed = preferences.getString("semanticSessionSeed", "");
		
		if(semanticSessionSeed.equals(""))
		{
			semanticSessionSeed = generateRandomString(20);
		
			Log.d("Utils", "getSemanticSessionSeed()--> generated a new semantic session seed: "+semanticSessionSeed);
			
			SharedPreferences.Editor editor = preferences.edit();
			editor.putString("semanticSessionSeed", semanticSessionSeed);
			editor.commit();						
		}
				
		return semanticSessionSeed;
	}*/
	
	//returns the semantic session number incremented by one at each call of this metod
	public static int increaseSemanticSessionNumber(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		semanticSessionNumber = preferences.getInt("semanticSessionNumber", 0);
		semanticSessionNumber++;
		
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("semanticSessionNumber", semanticSessionNumber);
		editor.commit();
		
		return semanticSessionNumber;
	}
	/*
	public static void setSourceSessionSeed(Context ctx, String seed)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("sourceSessionSeed", seed);
		editor.commit();
		
		sourceSessionSeed = seed;
	}
	
	public static String getSourceSessionSeed(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		sourceSessionSeed = preferences.getString("sourceSessionSeed", "");
		return sourceSessionSeed;
	}*/
	
	public static void setSourceSessionNumber(Context ctx, int number)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("sourceSessionNumber", number);
		editor.commit();
	}
	
	public static int getSourceSessionNumber(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getInt("sourceSessionNumber", -1);
	}
	
	//returns the semantic session number
	public static int getSemanticSessionNumber(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		semanticSessionNumber = preferences.getInt("semanticSessionNumber", 0);
		return semanticSessionNumber;
	}
	
	public static void setSemanticStartPointNumber(Context ctx, int number)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("semanticStartPointNumber", number);
		editor.commit();
		
		semanticStartPointNumber = number;
	}
	
	public static int getSemanticStartPointNumber(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getInt("semanticStartPointNumber", 0);
	}
	
	/******************* RECORDS COUNT TRACKING METHODS ************************************/
	
	//increment of 'quantity' the total number (uploaded + not uploaded) of records stored on DB
	//invoked in saveHistoryRecordsSerie() and saveRecord() of DbManager class
	public static void incrTotalStoredRecCount(Context ctx, int quantity)
	{
		int count;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		count = preferences.getInt("recordCount", 0);
		
		Log.d("Utils", "incrTotalStoredRecCount()--> total number of records saved on DB: "+count+ " quantity to increment: " +quantity);
		
		count += quantity;
		
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putInt("recordCount", count);
		editor.commit();
	}
	
	//decrement of 'quantity' the total number (uploaded + not uploaded) of records stored on DB
	//invoked in deleteUploadedRecords() of DBManager class
	public static void decrTotalStoredRecCount(Context ctx, int quantity)
	{
		int count;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		count = preferences.getInt("recordCount", 0);
		
		Log.d("Utils", "decrTotalStoredRecCount()--> total number of records saved on DB: "+count+ " quantity to decrement: " +quantity);
		
		if(quantity <= count)
		{
			count -= quantity;
	
			SharedPreferences.Editor editor = preferences.edit();	
			editor.putInt("recordCount", count);
			editor.commit();
		}
		else
		{		
			SharedPreferences.Editor editor = preferences.edit();	
			editor.putInt("recordCount", 0);
			editor.commit();
		}
	}
	
	public static void setTotalStoredRecCount(Context ctx, int total)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putInt("recordCount", total);
		editor.commit();
	}
	
	public static void resetUploadedRecCount(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt("uploadedRecordCount", 0);
		editor.commit();
	}
	
	//increment of 'quantity' the number of uploaded records stored on DB
	//invoked in run() of PostDataThread class in StoreAndForwardService class
	public static void incrUploadedRecCount(Context ctx, int quantity)
	{
		int uploadedCount;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		uploadedCount = preferences.getInt("uploadedRecordCount", 0);
		
		Log.d("Utils", "incrUploadedRecCount()--> uploaded record count: "+uploadedCount+ " quantity to increment: "+quantity);
		
		uploadedCount += quantity;
	
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putInt("uploadedRecordCount", uploadedCount);
		editor.commit();		
	}
	
	//decrement of 'quantity' the number of uploaded records stored on DB
	//invoked in deleteUploadedRecords() of DBManager class
	public static void decrUploadedRecCount(Context ctx, int quantity)
	{
		int uploadedCount;
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		uploadedCount = preferences.getInt("uploadedRecordCount", 0);
		
		Log.d("Utils", "decrUploadedRecCount()--> uploaded record count: "+uploadedCount+ " quantity to decrement: "+quantity);
		
		if(quantity <= uploadedCount)
		{
			uploadedCount -= quantity;
	
			SharedPreferences.Editor editor = preferences.edit();	
			editor.putInt("uploadedRecordCount", uploadedCount);
			editor.commit();
		}
		else
		{		
			SharedPreferences.Editor editor = preferences.edit();	
			editor.putInt("uploadedRecordCount", 0);
			editor.commit();
		}
	}
	
	//returns the total number of records stored on DB (uploaded + not uploaded)
	public static int getTotalStoredRecCount(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getInt("recordCount", 0);
	}
	
	//returns the number of uploaded records stored on DB 
	public static int getUploadedRecCount(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getInt("uploadedRecordCount", 0);
	}
	
	//returns the number of not uploaded records stored on DB
	public static int getNotUploadedRecCount(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("recordCount", 0) - preferences.getInt("uploadedRecordCount", 0);
	}
	
	/************************** END OF RECORDS COUNT TRACKING METHODS **********************************************/
	
	/************** METODI DI USO DELLE SHARED PREFERENCES *******************************************/
	
	//clear a lot of shared prefs
	public static void deleteSharedPrefs(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();	

		for(int i = 0; i < Constants.sharedPrefsKeys.length; i++)
			editor.remove(Constants.sharedPrefsKeys[i]);
		
		editor.commit();
	}
	
	public static void setBoxInfoMsg(String msg, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();	
		editor.putString("boxInfo", msg);
		editor.commit();
	}
	
	public static String getBoxInfoMsg(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getString("boxInfo", "");
	}
	
	public static void setGpsTrackingOn(boolean on, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putBoolean("gpsTrackingOn", on); 		
		editor.commit();
	}
	
	public static boolean getGpsTrackingOn(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getBoolean("gpsTrackingOn", false);
	}
	
	public static void setValidTwSession(boolean active, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("twitterOn", active);
		editor.commit();
	}
	
	public static boolean getValidTwSession(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getBoolean("twitterOn", false);
	}
	
	public static void setValidFbSession(boolean active, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("facebookOn", active);
		editor.commit();
	}
	
	public static boolean getValidFbSession(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getBoolean("facebookOn", false);
	}
	
	public static void setTrackLength(long millisec, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("trackLength", millisec);
		editor.commit();		
	}
	
	public static long getTrackLength(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("trackLength", Constants.FIVE_MINS);
	}
	
	public static void setSessionId(String sessionId, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("sessionId", sessionId);
		editor.commit();
	}
	
	public static String getSessionId(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("sessionId", "");
	}
	
	public static void setStoreForwInterval(long millis, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("interval", millis);
		editor.commit();
	}
	
	public static long getStoreForwInterval(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("interval", 30000);
	}
	
	//memorizza se le check box di condivisione su facebook e twitter sono checked oppure no
	public static void setShareCheckedOn(boolean facebook, boolean twitter, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("facebookCheckedOn", facebook);
		editor.putBoolean("twitterCheckedOn", twitter);
		editor.commit();
	}
	
	public static boolean[] getShareCheckedOn(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		boolean[] checkeds = new boolean[2];
		checkeds[0] = preferences.getBoolean("facebookCheckedOn", false);
		checkeds[1] = preferences.getBoolean("twitterCheckedOn", false);
		return checkeds;
	}
	
	//memorizza in quale activity ci si trova
	public static void setStep(int step , Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("step", step); 		
		editor.commit();
	}
	
	public static int getStep(Context ctx)
	{		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return preferences.getInt("step", -1);
	}
	
	//memorizza l'id del primo record ricevuto (utile per l'attività Graph)
	public static void setFirstRecordId(double firstRecordId , Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putFloat("firstRecordId", (float)firstRecordId); 		
		editor.commit();
	}
	
	public static double getFirstRecordId(Context ctx)
	{		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return (double)preferences.getFloat("firstRecordId", -1);
	}
	
	//memorizza l'id del record appena salvato
	public static void setNewRecordId(double newRecordId , Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putFloat("newRecordId", (float)newRecordId); 		
		editor.commit();
	}
	
	public static double getNewRecordId(Context ctx)
	{		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return (double)preferences.getFloat("newRecordId", -1);
	}
	
	public static void setDeviceAddress(String devAddr, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putString("deviceAddress", devAddr); 		
		editor.commit();
	}
	
	public static String getDeviceAddress(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return preferences.getString("deviceAddress", "");
	}
	
	public static void savePrefDeviceAddress(String devAddr, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putString("prefDeviceAddress", devAddr); 		
		editor.commit();
	}
	
	public static String getPrefDeviceAddress(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);		
		return preferences.getString("prefDeviceAddress", "");
	}	
	
	//user option prefs
	public static void setRecordAgesIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("recordAgeIndex", index); 		
		editor.commit();
	}
	
	public static int getRecordAgesIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("recordAgeIndex", 0);
	}
	
	public static void setStoreForwFreqIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("storeForwIndex", index); 		
		editor.commit();
	}
	
	public static int getStoreForwFreqIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("storeForwIndex", 0);
	}
	
	public static void setDownloadHistIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("downloadHistIndex", index); 		
		editor.commit();
	}
	
	public static int getDownloadHistIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("downloadHistIndex", 0);
	}
	
	public static void setUploadNetworkTypeIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("uploadNetworkTypeIndex", index); 		
		editor.commit();		
	}
	
	public static int getUploadNetworkTypeIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("uploadNetworkTypeIndex", 0);
	}
	
	public static void setUseNetworkProviderIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("useNetworkProviderIndex", index); 		
		editor.commit();		
	}
	
	public static int getUseNetworkProviderIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("useNetworkProviderIndex", 0);
	}
	
	public static void setCredentialsData(Context ctx, String accessToken, String refreshToken, String tokenType, long expiresIn, long creationTime)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("accessToken", accessToken);
		editor.putString("refreshToken", refreshToken);
		editor.putString("tokenType", tokenType);
		editor.putLong("expiresIn", expiresIn);
		editor.putLong("creationTime", creationTime);
		editor.commit();
	}
	
	public static String getAccessToken(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("accessToken", "");
	}

	public static String getRefreshToken(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("refreshToken", "");
	}	
	
	public static String getTokenType(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("tokenType", "");
	}	
	
	public static long getExpiresIn(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("expiresIn", -1);
	}
	
	public static long getCreationTime(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("creationTime", -1);
	}
	
	public static void setAccountActivationState(Context ctx, boolean activated)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("accountState", activated);
		editor.commit();
	}
	
	public static boolean getAccountActivationState(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getBoolean("accountState", false);
	}
	
	public static void setCredentialsDataForClient(Context ctx, String accessToken, String tokenType, long expiresIn, long creationTime)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("accessTokenForClient", accessToken);
		editor.putString("tokenTypeForClient", tokenType);
		editor.putLong("expiresInForClient", expiresIn);
		editor.putLong("creationTimeForClient", creationTime);
		editor.commit();
	}
	
	public static String getAccessTokenForClient(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("accessTokenForClient", "");
	}
	
	public static String getTokenTypeForClient(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getString("tokenTypeForClient", "");
	}	
	
	public static long getExpiresInForClient(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("expiresInForClient", -1);
	}
	
	public static long getCreationTimeForClient(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getLong("creationTimeForClient", -1);
	}
	
	public static void setAccountActivationStateForClient(Context ctx, boolean activated)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean("accountStateForClient", activated);
		editor.commit();
	}
	
	public static boolean getAccountActivationStateForClient(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		return preferences.getBoolean("accountStateForClient", false);
	}
		
	public static void setUsePhoneGpsIndex(int index, Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = preferences.edit();		
		editor.putInt("usePhoneGpsIndex", index); 		
		editor.commit();		
	}
	
	public static int getUsePhoneGpsIndex(Context ctx)
	{
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);	
		return preferences.getInt("usePhoneGpsIndex", 1); //default:  override
	}
	
	/********************* INFORMAZIONI SULLO SMARTPHONE ********************************************/
	
	//restituisce identificatore unico dello smartphone
	public static String readUniquePhoneID(Context ctx)
	{
		String ID = null;
		
		try
		{
			TelephonyManager tm = (TelephonyManager)(ctx.getSystemService(Context.TELEPHONY_SERVICE));
			ID = tm.getDeviceId();
	
			if((ID == null)||(ID.equals("")))
				ID = Settings.Secure.ANDROID_ID;
			
			//Build.SERIAL causes NoSuchFieldException on Galaxy Tab (GT-P1010)
			if((ID == null)||(ID.equals("")))		
				ID = Build.SERIAL;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(ID == null)
				ID = "N.A."; //not available id
		}
		return ID;
	}
	
	//restituisce modello dello smartphone
	public static String readPhoneModel()
	{
		try
		{
			return android.os.Build.MODEL;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "N.A.";
		}
	}
	
	/********* RIDUCE LUNGHEZZA NUMERO CON VIRGOLA A 4 CIFRE A DESTRA DELLA VIRGOLA ************************/
	
	public static String reduceNumLength(String coord)
	{
		if(coord.length() > 8)	
		{		
			String[] parts = new String[2]; 
			parts[0] = coord.substring(0, coord.indexOf("."));
			parts[1] = coord.substring(coord.indexOf(".")+1, coord.indexOf(".")+7);

			coord = parts[0]+"."+parts[1];		
		}	
		return coord;
	}
	
	/********************* METODO VERIFICA SE CONNESSIONE INTERNET E' ATTIVA ******************************/
	
	//returned array: first position is true if wifi network is connected, second pos. is true is mobile network is connected
	public static boolean[] haveNetworkConnection(Context ctx) 
	{
	    boolean haveConnectedWifi = false;
	    boolean haveConnectedMobile = false;

	    boolean[] response = new boolean[2];
	    
	    ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
	    
	    for (NetworkInfo ni : netInfo) 
	    {	
	        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
	            if (ni.isConnected())
	                haveConnectedWifi = true;
	        
	        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
	            if (ni.isConnected())
	                haveConnectedMobile = true;
	    }
	    
	    Log.d("Utils", "haveNetworkConnection()--> connected WIFI: "+haveConnectedWifi+" connected MOBILE: "+haveConnectedMobile);
	    
	    //return haveConnectedWifi || haveConnectedMobile;
	    response[0] = haveConnectedWifi;
	    response[1] = haveConnectedMobile;
	    
	    return response;
	}
	
	/******************** CHECK SCREEN SIZE: SMALL, NORMAL, LARGE, UNDEFINED *********************************/
	
	public static void determineScreenSize(Context ctx)
	{
	    if ((ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {     
	        Toast.makeText(ctx, "Large screen",Toast.LENGTH_LONG).show();

	    }
	    else if ((ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {     
	        Toast.makeText(ctx, "Normal sized screen" , Toast.LENGTH_LONG).show();

	    } 
	    else if ((ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {     
	        Toast.makeText(ctx, "Small sized screen" , Toast.LENGTH_LONG).show();
	    }
	    else {
	        Toast.makeText(ctx, "Screen size is neither large, normal or small" , Toast.LENGTH_LONG).show();
	        
	    }
	}
	
	/******************* CALCULATE TIMESTAMP IN MILLISECONDS FROM BOX TIMESTAMP RECEIVED FROM SENSOR BOX *******************/
	
	public static long getTsFromBoxTimestampFields(String firstField, String secondField)
	{
		String dateStr = firstField;
		int year = Integer.valueOf(dateStr.substring(4));
		int month = Integer.valueOf(dateStr.substring(2, 4)) - 1;
		int day = Integer.valueOf(dateStr.substring(0, 2));
		
		Calendar cal = null;
		
		try
		{
			cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")); // or GMT
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if(cal == null)
		{
			try
			{
				cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")); 
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		if(cal == null)
			cal = Calendar.getInstance();
		
		cal.set(year+2000, month, day);
		
		String minSecStr = secondField;
		int hour = Integer.valueOf(minSecStr.substring(0, 2));
		int min = Integer.valueOf(minSecStr.substring(2, 4));
		int sec = Integer.valueOf(minSecStr.substring(4,6));
		
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		
		return cal.getTimeInMillis();
	}
	
	/***************** CONVERT INT (32 bit) TO BYTE ARRAY (4 bytes array - each byte is of 8 bit) *******************************/ 
	
	public static byte[] intToBytes( final int i ) 
	{
	    ByteBuffer bb = ByteBuffer.allocate(4); 
	    bb.putInt(i); 
	    return bb.array();
	}
	
	public static byte[] intToByteArray ( final int i ) throws IOException 
	{  	
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(bos);
	    dos.writeInt(i);
	    dos.flush();
	    return bos.toByteArray();
	}
}
