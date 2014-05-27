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

package org.csp.everyaware.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.csp.everyaware.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.util.Log;

public class Record 
{
	public double mId;	
	
	public String mUniquePhoneId;
	public String mPhoneModel;
	
	public long mSysTimestamp;
	public long mBoxTimestamp;
	public double[] mValues = new double[12];
	
	public String mLocalization = ""; //localization type constants: indoor, outdoor
	public String mGpsProvider = ""; //gps provider type constants: sensorbox, gps
	public double mAccuracy;
	//public long mSbPowerOn; ////power on time from sensor box (in seconds) (not used anymore from AP 1.4)
	
	/* from AP 1.4 */
	public String mSourceSessionSeed; //random generated string from sensor box
	public int mSourceSessionNumber; //increasing integer from sensor box (identifies a single shut on-off period)
	public int mSourcePointNumber; //power on time from sensor box (in seconds)
	public String mSemanticSessionSeed; //random generated string from AP when user presses 'start'
	public int mSemanticSessionNumber; //
	public int mSemanticPointNumber; //difference between the source point number when user pressed 'start' and current source point number
	
	public double mPhoneLat, mPhoneLon;
	public double mPhoneAcc, mPhoneSpeed, mPhoneBear, mPhoneAltitude;
	public long mPhoneTimestamp;
	
	public double mBoxLat, mBoxLon;
	public double mBoxAcc, mBoxSpeed, mBoxBear, mBoxAltitude;
	public int mBoxNumSats;
	
	public double mNetworkBear, mNetworkSpeed, mNetworkAltitude;
	
	public String mBoxMac;
	
	/* end of from AP 1.4 */
	
	//network localization data in addtion to phone/sensor box gps data
	public double mNetworkLat, mNetworkLon;
	public double mNetworkAcc;
	public long mNetworkTimestamp;
	
	public double mBcMobile;
	
	//user's data
	public String mUserData1; //contains user's annotations
	public String mUserData2; //firmware version of sensor box
	public String mUserData3; //mac address of sensor box
	
	public long mUploadSysTs; //timestamp when record is sent to server (written only if server response is HTTP 200 OK)
	
	public String mSessionId;
	
	public static class RecordMetaData 
	{
		//table name
		public static final String TABLE_NAME = "records";
		
		public static final String TABLE_TAGS_NAME = "records_with_tags";
		
		//fields name
		public static final String ID = "_id"; 
		
		public static final String PHONE_ID = "phone_id";
		public static final String PHONE_MODEL = "phone_model";
		
		public static final String SYS_TS = "sys_ts";	
		public static final String LAT = "lat";
		public static final String LON = "lon";
		public static final String CO_1 = "co_1";
		public static final String CO_2 = "co_2";
		public static final String CO_3 = "co_3";
		public static final String CO_4 = "co_4";
		public static final String NO2_1 = "no2_1";
		public static final String NO2_2 = "no2_2";
		public static final String VOC = "voc_1";
		public static final String O3 = "o3_1";
		public static final String TEMP = "temp_1";
		public static final String HUM = "hum_1";
		
		public static final String LOCALIZATION = "localization";
		public static final String GPS_PROVIDER = "gps_provider";
		public static final String ACCURACY = "accuracy"; //valid only for phone gps and network gps providers (not used anymore from AP 1.4)
		//public static final String SB_TIME_ON = "sb_time_on"; //sinceOn field in output json (from AP 1.4 there is also source_point_number field with the same value)
		
		/* from AP 1.4 */		
		public static final String SOURCE_SESSION_SEED = "source_session_seed";
		public static final String SOURCE_SESSION_NUMBER = "source_session_number";
		public static final String SOURCE_POINT_NUMBER = "source_point_number";
		public static final String SEMANTIC_SESSION_SEED = "semantic_session_seed";
		public static final String SEMANTIC_SESSION_NUMBER = "semantic_session_number";
		public static final String SEMANTIC_POINT_NUMBER = "sematic_point_number";
		/* end of from AP 1.4 */
		
		/* from AP 1.4 */
		public static final String PHONE_LAT = "phone_lat";
		public static final String PHONE_LON = "phone_lon";
		public static final String PHONE_ACC = "phone_acc";
		public static final String PHONE_BEAR = "phone_bear";
		public static final String PHONE_SPEED = "phone_speed";
		public static final String PHONE_ALTITUDE = "phone_altitude";
		public static final String PHONE_TS = "phone_timestamp";
		/* end of from AP 1.4 */
		
		/* from AP 1.4 */
		public static final String BOX_LAT = "box_lat";
		public static final String BOX_LON = "box_lon";
		public static final String BOX_ACC = "box_acc";
		public static final String BOX_BEAR = "box_bear";
		public static final String BOX_SPEED = "box_speed";
		public static final String BOX_ALTITUDE = "box_altitude";
		public static final String BOX_NUM_SATS = "box_num_sats";
		/* end of from AP 1.4 */
		
		public static final String BOX_TS = "box_ts";
		
		public static final String NETWORK_LAT = "network_lat";
		public static final String NETWORK_LON = "network_lon";
		public static final String NETWORK_ACC = "network_acc";
		public static final String NETWORK_TS = "network_timestamp";		
		/* from AP 1.4 */
		public static final String NETWORK_BEAR = "network_bear";
		public static final String NETWORK_SPEED = "network_speed";
		public static final String NETWORK_ALTITUDE = "network_altitude";
		/* end of from AP 1.4 */
		
		/* from AP 1.4 */
		public static final String BOX_MAC_ADDRESS = "box_mac_address";
		/* end of from AP 1.4 */
		
		public static final String BLACK_CARBON_MOBILE = "black_carbon_mobile";
		
		//data expressed by user
		public static final String USER_DATA_1 = "user_data_1"; //user annotation
		public static final String USER_DATA_2 = "user_data_2"; //sensor box firmware version
		public static final String USER_DATA_3 = "user_data_3"; //mac address
		
		//this field containes system timestamp when records are sent to server and only if server
		//response is HTTP 200 OK. Default field value is 0 (= record not sent to server yet)
		public static final String UPLOAD_SYS_TS = "upload_sys_ts";
		
		//the same value for entire session
		public static final String SESSION_ID = "session_id";
		
		public static final String[] COLUMNS = new String[] { ID, PHONE_ID, PHONE_MODEL, SYS_TS, BOX_TS, LAT, 
			LON, CO_1, CO_2, CO_3, CO_4, NO2_1, NO2_2, VOC, O3, TEMP, HUM, LOCALIZATION, GPS_PROVIDER, ACCURACY, NETWORK_LAT, NETWORK_LON, NETWORK_ACC, NETWORK_TS, BLACK_CARBON_MOBILE, USER_DATA_1, 
			USER_DATA_2, USER_DATA_3, UPLOAD_SYS_TS, SESSION_ID, SOURCE_SESSION_SEED, SOURCE_SESSION_NUMBER, SOURCE_POINT_NUMBER, 
			SEMANTIC_SESSION_SEED, SEMANTIC_SESSION_NUMBER, SEMANTIC_POINT_NUMBER,
			PHONE_LAT, PHONE_LON, PHONE_ACC, PHONE_BEAR, PHONE_SPEED, PHONE_ALTITUDE, PHONE_TS, BOX_LAT, BOX_LON, BOX_ACC,
			BOX_BEAR, BOX_SPEED, BOX_ALTITUDE, BOX_NUM_SATS, NETWORK_BEAR, NETWORK_SPEED, NETWORK_ALTITUDE, BOX_MAC_ADDRESS};
	}	
	
	public Record(long sysTs, long boxTs, double lat, double lon, double co_1, double co_2,
			double co_3, double co_4, double no2_1, double no2_2, double voc, double o3,
			double temp, double hum, String sessionId, String localization, String gpsProvider, double accuracy, double[] networkLoc, long networkTimestamp, 
			String sourceSessionSeed, int sourceSessionNumber, int sourcePointNumber, String semanticSessionSeed, int semanticSessionNumber, int semanticPointNumber, double phoneLat, double phoneLon,
			double phoneAcc, double phoneBear, double phoneSpeed, double phoneAltitude, long phoneTimestamp, double boxLat, double boxLon, double boxAcc, double boxBear,
			double boxSpeed, double boxAltitude, int boxNumSats, double networkBear, double networkSpeed, double networkAltitude, String boxMac)
	{
		mSysTimestamp = sysTs;
		mBoxTimestamp = boxTs;
		
		mValues[0] = lat;
		mValues[1] = lon;
		
		mValues[2] = co_1;
		mValues[3] = co_2;
		mValues[4] = co_3;
		mValues[5] = co_4;
		
		mValues[6] = no2_1;
		mValues[7] = no2_2;
		
		mValues[8] = voc;
		mValues[9] = o3;
		
		mValues[10] = temp;
		mValues[11] = hum;
		
		mLocalization = localization;
		mGpsProvider = gpsProvider;
		mAccuracy = accuracy;
		mSourcePointNumber = sourcePointNumber;
		
		mNetworkLat = networkLoc[0];
		mNetworkLon = networkLoc[1];
		mNetworkAcc = networkLoc[2];
		mNetworkTimestamp = networkTimestamp;
			
		mUserData1 = "";
		mUserData2 = "";
		mUserData3 = mBoxMac;
		mBcMobile = 0;
		
		mUploadSysTs = 0;
		
		mSessionId = sessionId;
		
		/* from AP 1.4 */
		mSourceSessionSeed = sourceSessionSeed;
		mSourceSessionNumber = sourceSessionNumber;
		mSourcePointNumber = sourcePointNumber;
		mSemanticSessionSeed = semanticSessionSeed;
		mSemanticSessionNumber = semanticSessionNumber;
		mSemanticPointNumber = semanticPointNumber;
		
		mPhoneLat = phoneLat;
		mPhoneLon = phoneLon;
		mPhoneAcc = phoneAcc;
		mPhoneBear = phoneBear;
		mPhoneSpeed = phoneSpeed;
		mPhoneAltitude = phoneAltitude;
		mPhoneTimestamp = phoneTimestamp;
		
		mBoxLat = boxLat;
		mBoxLon = boxLon;
		mBoxAcc = boxAcc;
		mBoxBear = boxBear;
		mBoxSpeed = boxSpeed;
		mBoxAltitude = boxAltitude;
		mBoxNumSats = boxNumSats;
		
		mNetworkBear = networkBear;
		mNetworkSpeed = networkSpeed;
		mNetworkAltitude = networkAltitude;
		
		mBoxMac = boxMac;
		/* end of from AP 1.4 */
		
		Log.d("Record", "Record()--> mSourceSessionSeed: "+mSourceSessionSeed+" mSourceSessionNumber: "+mSourceSessionNumber+" mSourcePointNumber: "+mSourcePointNumber);
		Log.d("Record", "Record()--> mSemanticSessionSeed: "+mSemanticSessionSeed+" mSemanticSessionNumber: "+mSemanticSessionNumber+" mSemanticPointNumber: "+mSemanticPointNumber);
		Log.d("Record", "Record()--> box acc: "+mBoxAcc+" box bear: "+mBoxBear+" box speed: "+mBoxSpeed+" box altitude: "+mBoxAltitude+" box num sats: "+mBoxNumSats);
	}
	
	public Record(long sysTs, long boxTs, double[] values, String sessionId, String localization, String gpsProvider,  double accuracy, double[] networkLoc, long networkTimestamp, 
			String sourceSessionSeed, int sourceSessionNumber, int sourcePointNumber, String semanticSessionSeed, int semanticSessionNumber, int semanticPointNumber, double phoneLat, double phoneLon,
			double phoneAcc, double phoneBear, double phoneSpeed, double phoneAltitude, long phoneTimestamp, double boxLat, double boxLon, double boxAcc, double boxBear,
			double boxSpeed, double boxAltitude, int boxNumSats, double networkBear, double networkSpeed, double networkAltitude, String boxMac)
	{
		mSysTimestamp = sysTs;
		mBoxTimestamp = boxTs;
		
		mValues[0] = values[0];
		mValues[1] = values[1];
		
		mValues[2] = values[2];
		mValues[3] = values[3];
		
		mValues[4] = values[4];
		mValues[5] = values[5];
		mValues[6] = values[6];
		mValues[7] = values[7];
		
		mValues[8] = values[8];
		mValues[9] = values[9];
		
		mValues[10] = values[10];
		mValues[11] = values[11];
		
		mLocalization = localization;
		mGpsProvider = gpsProvider;
		mAccuracy = accuracy;
		
		mNetworkLat = networkLoc[0];
		mNetworkLon = networkLoc[1];
		mNetworkAcc = networkLoc[2];
		mNetworkTimestamp = networkTimestamp;
		
		mUserData1 = "";
		mUserData2 = "";
		mUserData3 = mBoxMac;
		mBcMobile = 0;
		
		mUploadSysTs = 0;
		
		mSessionId = sessionId;
		
		/* from AP 1.4 */
		mSourceSessionSeed = sourceSessionSeed;
		mSourceSessionNumber = sourceSessionNumber;
		mSourcePointNumber = sourcePointNumber;
		mSemanticSessionSeed = semanticSessionSeed;
		mSemanticSessionNumber = semanticSessionNumber;
		mSemanticPointNumber = semanticPointNumber;
		
		mPhoneLat = phoneLat;
		mPhoneLon = phoneLon;
		mPhoneAcc = phoneAcc;
		mPhoneBear = phoneBear;
		mPhoneSpeed = phoneSpeed;
		mPhoneAltitude = phoneAltitude;
		mPhoneTimestamp = phoneTimestamp;
		
		mBoxLat = boxLat;
		mBoxLon = boxLon;
		mBoxAcc = boxAcc;
		mBoxBear = boxBear;
		mBoxSpeed = boxSpeed;
		mBoxAltitude = boxAltitude;
		mBoxNumSats = boxNumSats;
		
		mNetworkBear = networkBear;
		mNetworkSpeed = networkSpeed;
		mNetworkAltitude = networkAltitude;
		
		mBoxMac = boxMac;
		/* end of from AP 1.4 */
		
		//Log.d("Record", "Record()--> sessionId: "+mSessionId);
		Log.d("Record", "Record()--> mSourceSessionSeed: "+mSourceSessionSeed+" mSourceSessionNumber: "+mSourceSessionNumber+" mSourcePointNumber: "+mSourcePointNumber);
		Log.d("Record", "Record()--> mSemanticSessionSeed: "+mSemanticSessionSeed+" mSemanticSessionNumber: "+mSemanticSessionNumber+" mSemanticPointNumber: "+mSemanticPointNumber);
		Log.d("Record", "Record()--> box acc: "+mBoxAcc+" box bear: "+mBoxBear+" box speed: "+mBoxSpeed+" box altitude: "+mBoxAltitude+" box num sats: "+mBoxNumSats);
	}
	
	//calcola la media complessiva come media delle 4 medie sugli inquinanti e la normalizza
	public double calcAvgPoll()
	{/*
		double co_avg = 0;
		double no2_avg = 0;
		double voc_avg = 0;
		double o3_avg = 0;
		*/
		double avg = 0;
		
		//calcolo la media sui 4 sensori di co e la sommo all'accumulatore
		//co_avg = (mValues[2] + mValues[3] + mValues[4] + mValues[5]) / 4;
		
		//calcolo la media sui 2 sensori di no2 e la sommo all'accumulatore
		//no2_avg = (mValues[6] + mValues[7]) / 2;
		
		//voc_avg = mValues[8];
		//o3_avg = mValues[9];	
		
		//Log.d("Record", mValues[2]+","+mValues[3]+","+mValues[4]+","+mValues[5]+","+mValues[6]+","+mValues[7]+","+mValues[8]+","+mValues[9]);
		avg = (mValues[2] + mValues[3] + mValues[4] + mValues[5] + mValues[6] + mValues[7] + mValues[8] + mValues[9]) /8; 
		
		return avg;
		//return normalizeValue(avg);
	}
	
	//normalizza un valore. Usa la formula : differenza fra il valore e il minimo della serie,
	//fratto differenza fra il massimo e il minimo della serie
	
	//se il valore in input supera il massimo deciso per la serie, la frazione assume un valore
	//maggiore di 1. Se il valore in input è inferiore al minimo deciso per la serie, la frazione
	//diventa negativa
	public static double normalizeValue(double avg)
	{
		double min = 2;
		double max = 5;
		double norm_avg = 0;
	
		norm_avg = (avg - min)/(max - min);
		
		if(norm_avg > 1)
			norm_avg = 1;
		else if(norm_avg < 0)
			norm_avg = 0;
		
		return norm_avg;
	}
	
	/******************** PER INTERAZIONE CON DATABASE *********************************************/
	
	//crea l'oggetto ContentValues per l'istanza attuale. NB non la memorizza da nessuna 
	//parte, la genera solo e la restituisce
	public ContentValues getRecordValues()
	{
		ContentValues recordValues = new ContentValues();
		
		recordValues.put(RecordMetaData.PHONE_ID, mUniquePhoneId);
		recordValues.put(RecordMetaData.PHONE_MODEL, mPhoneModel);
		
		recordValues.put(RecordMetaData.SYS_TS, mSysTimestamp);
		recordValues.put(RecordMetaData.BOX_TS, mBoxTimestamp);
		recordValues.put(RecordMetaData.LAT, mValues[0]);		
		recordValues.put(RecordMetaData.LON, mValues[1]);	
		recordValues.put(RecordMetaData.CO_1, mValues[2]);
		recordValues.put(RecordMetaData.CO_2, mValues[3]);
		recordValues.put(RecordMetaData.CO_3, mValues[4]);
		recordValues.put(RecordMetaData.CO_4, mValues[5]);
		recordValues.put(RecordMetaData.NO2_1, mValues[6]);
		recordValues.put(RecordMetaData.NO2_2, mValues[7]);
		recordValues.put(RecordMetaData.VOC, mValues[8]);
		recordValues.put(RecordMetaData.O3, mValues[9]);
		recordValues.put(RecordMetaData.TEMP, mValues[10]);
		recordValues.put(RecordMetaData.HUM, mValues[11]);
		
		recordValues.put(RecordMetaData.LOCALIZATION, mLocalization);
		recordValues.put(RecordMetaData.GPS_PROVIDER, mGpsProvider);
		recordValues.put(RecordMetaData.ACCURACY, mAccuracy);
		//recordValues.put(RecordMetaData.SOURCE_POINT_NUMBER, mSourcePointNumber); //removed from AP 1.4
		
		recordValues.put(RecordMetaData.NETWORK_LAT, mNetworkLat);
		recordValues.put(RecordMetaData.NETWORK_LON, mNetworkLon);
		recordValues.put(RecordMetaData.NETWORK_ACC, mNetworkAcc);
		recordValues.put(RecordMetaData.NETWORK_TS, mNetworkTimestamp);
		
		recordValues.put(RecordMetaData.BLACK_CARBON_MOBILE, mBcMobile);
		
		recordValues.put(RecordMetaData.USER_DATA_1, mUserData1);
		recordValues.put(RecordMetaData.USER_DATA_2, mUserData2);
		recordValues.put(RecordMetaData.USER_DATA_3, mBoxMac);
		
		recordValues.put(RecordMetaData.UPLOAD_SYS_TS, mUploadSysTs);
		recordValues.put(RecordMetaData.SESSION_ID, mSessionId);
		
		/* from AP 1.4 */
		recordValues.put(RecordMetaData.SOURCE_SESSION_SEED, mSourceSessionSeed);
		recordValues.put(RecordMetaData.SOURCE_SESSION_NUMBER, mSourceSessionNumber);
		recordValues.put(RecordMetaData.SOURCE_POINT_NUMBER, mSourcePointNumber);
		recordValues.put(RecordMetaData.SEMANTIC_SESSION_SEED, mSemanticSessionSeed);
		recordValues.put(RecordMetaData.SEMANTIC_SESSION_NUMBER, mSemanticSessionNumber);
		recordValues.put(RecordMetaData.SEMANTIC_POINT_NUMBER, mSemanticPointNumber);		
		
		recordValues.put(RecordMetaData.PHONE_LAT, mPhoneLat);
		recordValues.put(RecordMetaData.PHONE_LON, mPhoneLon);
		recordValues.put(RecordMetaData.PHONE_ACC, mPhoneAcc);
		recordValues.put(RecordMetaData.PHONE_BEAR, mPhoneBear);
		recordValues.put(RecordMetaData.PHONE_SPEED, mPhoneSpeed);
		recordValues.put(RecordMetaData.PHONE_ALTITUDE, mPhoneAltitude);
		recordValues.put(RecordMetaData.PHONE_TS, mPhoneTimestamp);
		
		recordValues.put(RecordMetaData.BOX_LAT, mBoxLat);
		recordValues.put(RecordMetaData.BOX_LON, mBoxLon);
		recordValues.put(RecordMetaData.BOX_ACC, mBoxAcc);
		recordValues.put(RecordMetaData.BOX_BEAR, mBoxBear);
		recordValues.put(RecordMetaData.BOX_SPEED, mBoxSpeed);
		recordValues.put(RecordMetaData.BOX_ALTITUDE, mBoxAltitude);
		recordValues.put(RecordMetaData.BOX_NUM_SATS, mBoxNumSats);
		
		recordValues.put(RecordMetaData.NETWORK_BEAR, mNetworkBear);
		recordValues.put(RecordMetaData.NETWORK_SPEED, mNetworkSpeed);
		recordValues.put(RecordMetaData.NETWORK_ALTITUDE, mNetworkAltitude);
		
		recordValues.put(RecordMetaData.BOX_MAC_ADDRESS, mBoxMac);
		/* end of from AP 1.4 */
		
		return recordValues;
	}	
	
	//crea l'istruzione sql per la creazione della corrispettiva tabella RECORDS
	public static StringBuilder createRecordsTableSQL()
	{
		StringBuilder createRecordsTable = new StringBuilder();
		
		createRecordsTable.append("CREATE TABLE \"" +RecordMetaData.TABLE_NAME+ "\" (");
		createRecordsTable.append("	    \"" +RecordMetaData.ID+ "\" INTEGER PRIMARY KEY AUTOINCREMENT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ID+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_MODEL+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.SYS_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_3+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_4+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NO2_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NO2_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.VOC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.O3+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.TEMP+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.HUM+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.LOCALIZATION+ "\" TEXT,"); 
		createRecordsTable.append("     \"" +RecordMetaData.GPS_PROVIDER+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.ACCURACY+ "\" TEXT,");
		//createRecordsTable.append("	    \"" +RecordMetaData.SB_TIME_ON+ "\" TEXT,"); //power on time from sensor box (in seconds)
		
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_LAT+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_LON+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_ACC+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_TS+ "\" TEXT,");
		
		createRecordsTable.append("     \"" +RecordMetaData.BLACK_CARBON_MOBILE+ "\" TEXT," );
		
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_3+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.UPLOAD_SYS_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SESSION_ID+ "\" TEXT,");
		
		/* from AP 1.4 */
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_SESSION_SEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_SESSION_NUMBER+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_POINT_NUMBER+ "\" TEXT,"); //the same of sb time on
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_SESSION_SEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_SESSION_NUMBER+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_POINT_NUMBER+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ACC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ALTITUDE+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_TS+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_ACC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_ALTITUDE+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_NUM_SATS+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_ALTITUDE+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_MAC_ADDRESS+ "\" TEXT");
		/* end of from AP 1.4 */
		
		createRecordsTable.append(")");
		
		return createRecordsTable;
	}	
	
	public static StringBuilder createRecordsWithTagsTableSQL()
	{
		StringBuilder createRecordsTable = new StringBuilder();
		
		createRecordsTable.append("CREATE TABLE \"" +RecordMetaData.TABLE_TAGS_NAME+ "\" (");
		createRecordsTable.append("	    \"" +RecordMetaData.ID+ "\" INTEGER PRIMARY KEY AUTOINCREMENT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ID+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_MODEL+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.SYS_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_3+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.CO_4+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NO2_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NO2_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.VOC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.O3+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.TEMP+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.HUM+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.LOCALIZATION+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.GPS_PROVIDER+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.ACCURACY+ "\" TEXT,");
		//createRecordsTable.append("	    \"" +RecordMetaData.SB_TIME_ON+ "\" TEXT,"); 
		
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_LAT+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_LON+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_ACC+ "\" TEXT,");
		createRecordsTable.append("     \"" +RecordMetaData.NETWORK_TS+ "\" TEXT,");
		
		createRecordsTable.append("     \"" +RecordMetaData.BLACK_CARBON_MOBILE+ "\" TEXT," );
		
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_1+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_2+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.USER_DATA_3+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.UPLOAD_SYS_TS+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SESSION_ID+ "\" TEXT,");
		
		/* from AP 1.4 */
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_SESSION_SEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_SESSION_NUMBER+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SOURCE_POINT_NUMBER+ "\" TEXT,"); //the same of sb time on
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_SESSION_SEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_SESSION_NUMBER+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.SEMANTIC_POINT_NUMBER+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ACC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_ALTITUDE+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.PHONE_TS+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_LAT+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_LON+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_ACC+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_ALTITUDE+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_NUM_SATS+ "\" TEXT,");		
		
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_BEAR+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_SPEED+ "\" TEXT,");
		createRecordsTable.append("	    \"" +RecordMetaData.NETWORK_ALTITUDE+ "\" TEXT,");
		
		createRecordsTable.append("	    \"" +RecordMetaData.BOX_MAC_ADDRESS+ "\" TEXT");
		/* end of from AP 1.4 */		
		
		createRecordsTable.append(")");
		
		return createRecordsTable;
	}	
		
	@Override
	public String toString()
	{
		String recToStr = "";
		
		recToStr += mId+" "+mUniquePhoneId+" "+mPhoneModel+" "+mSysTimestamp+" "+mBoxTimestamp+" "
				+mUploadSysTs+" "+mSessionId+" - network lat: " +mNetworkLat+ " - network lon: "+mNetworkLon;
		
		for(int i = 0; i < mValues.length; i++)
		{
			recToStr += mValues[i]+" ";
		}
		
		recToStr += mUserData1+" ";
		
		return recToStr;
	}
	
	public JSONObject toJson() 
	{			
		boolean insertLegacyField = false;
		
		JSONObject object = new JSONObject();
	
		try 
		{	
			object.put("co_1", mValues[2]);
			object.put("co_2", mValues[3]);
			object.put("co_3", mValues[4]);
			object.put("co_4", mValues[5]);			
			object.put("no2_1", mValues[6]);
			object.put("no2_2", mValues[7]);
			object.put("voc_1", mValues[8]);
			object.put("o3_1", mValues[9]);
			object.put("bc_1", mBcMobile);
			
			/************* LEGACY FIELDS ***************/
			
			if(insertLegacyField)
			{
				//creating json object
				object.put("uid", md5(mUniquePhoneId));
				object.put("device", mPhoneModel);			
				object.put("session_id", mSessionId);		
				
				ArrayList<String> coords = new ArrayList<String>();
				//first lon, than lat!!!
				coords.add(String.valueOf(mValues[1]));
				coords.add(String.valueOf(mValues[0]));
				object.put("geo_coord", new JSONArray(coords));
				
				//1 - history records have only box timestamp!! (sys timestamp = 0)
				//2 - records recorded when sensor box is connected to air probe have always sys timestamp
				
				if(mSysTimestamp != 0)
				{
					object.put("timestamp", mSysTimestamp); 
					
					if((mSysTimestamp <= 0)||(mSysTimestamp > (mSysTimestamp + 48*60*60*1000)))
						Log.d("Record", "timestamp sys < 0!!! --> " +mSysTimestamp);
				}
				else
				{
					object.put("timestamp", mBoxTimestamp);
					
					if((mBoxTimestamp <= 0)||(mBoxTimestamp > (mBoxTimestamp + 48*60*60*1000)))
						Log.d("Record", "timestamp box < 0!!! --> " +mBoxTimestamp);
				}
				
				object.put("user_data_1", mUserData1); 
				object.put("user_data_2", mUserData2); 
				object.put("user_data_3", mBoxMac); 
				object.put("user_data_4", mBcMobile); 
				
				/*
				object.put("hash", ""); //hmac is calculated on entire array with empty hash field
				
				Mac mac = Mac.getInstance(Constants.hmac);
				SecretKeySpec secret = new SecretKeySpec(Constants.KEY.getBytes(), Constants.hmac);
				
				mac.init(secret);
				
				String jsonString = object.toString();
				String resultString = new String(Base64.encode(mac.doFinal(jsonString.getBytes("UTF-8")), Base64.DEFAULT));
				
				//deleting "\n" char
				resultString = resultString.substring(0, resultString.length()-2);
				
				object.put("hash", resultString);
				*/
			}
			
			/************* END OF LEGACY FIELDS ***************/
			
			/**************** NEW API FIELDS *******************/
			
			object.put("temp_1", mValues[10]); //from temp to temp_1 for new V1 API
			object.put("hum_1", mValues[11]); //from hum to hum_1 for new V1 API

			JSONObject timestamps = new JSONObject();
			timestamps.put("sensorbox", mBoxTimestamp);
			timestamps.put("phone", mSysTimestamp);
			object.put("timestamps", timestamps);
			
			/* from AP 1.4 */
			JSONObject sourceSessionDetails = new JSONObject();
			sourceSessionDetails.put("sessionSeed", mSourceSessionSeed);
			sourceSessionDetails.put("sessionNumber", mSourceSessionNumber);
			sourceSessionDetails.put("pointNumber", mSourcePointNumber);
			object.put("sourceSessionDetails", sourceSessionDetails);
			/* end of from AP 1.4 */
			
			if((mSemanticSessionSeed != null)&&(!mSemanticSessionSeed.equals("")))
			{
				JSONObject semanticSessionDetails = new JSONObject();
				semanticSessionDetails.put("sessionSeed", mSemanticSessionSeed);
				semanticSessionDetails.put("sessionNumber", mSemanticSessionNumber);
				semanticSessionDetails.put("pointNumber", mSemanticPointNumber);
				object.put("semanticSessionDetails", semanticSessionDetails);
			}
			
			JSONArray locations = new JSONArray();
			
			//sensor box gps data
			if(mBoxLat != 0)
			{
				JSONObject boxLocation = new JSONObject();
				boxLocation.put("latitude", mBoxLat);
				boxLocation.put("longitude", mBoxLon);
				if(mBoxAcc != 0)
					boxLocation.put("hdop", mBoxAcc);
				if(mBoxAltitude != 0)
					boxLocation.put("altitude", mBoxAltitude);
				if(mBoxSpeed != 0)
					boxLocation.put("speed", mBoxSpeed);
				if(mBoxBear != 0)
					boxLocation.put("bearing", mBoxBear);
				if(mBoxNumSats != 0)
					boxLocation.put("numberOfSatellites", mBoxNumSats);
				boxLocation.put("provider", Constants.GPS_PROVIDERS[0]);
				boxLocation.put("timestamp", mBoxTimestamp);
				
				locations.put(0, boxLocation);
			}
			
			//phone gps data
			if(mPhoneLat != 0)
			{
				JSONObject phoneLocation = new JSONObject();
				phoneLocation.put("latitude", mPhoneLat);
				phoneLocation.put("longitude", mPhoneLon);
				if(mPhoneAcc != 0)
					phoneLocation.put("accuracy", mPhoneAcc);
				if(mPhoneAltitude != 0)
					phoneLocation.put("altitude", mPhoneAltitude);
				if(mPhoneSpeed != 0)
					phoneLocation.put("speed", mPhoneSpeed);
				if(mPhoneBear != 0)
					phoneLocation.put("bearing", mPhoneBear);
				phoneLocation.put("provider", Constants.GPS_PROVIDERS[1]);
				phoneLocation.put("timestamp", mPhoneTimestamp);
				
				locations.put(1, phoneLocation);
			}
			
			//network gps data
			if(mNetworkLat != 0)
			{
				JSONObject netLocation = new JSONObject();
				netLocation.put("latitude", mNetworkLat);
				netLocation.put("longitude", mNetworkLon);
				if(mNetworkAcc != 0)
					netLocation.put("accuracy", mNetworkAcc);
				if(mNetworkAltitude != 0)
					netLocation.put("altitude", mNetworkAltitude);
				if(mNetworkSpeed != 0)
					netLocation.put("speed", mNetworkSpeed);
				if(mNetworkBear != 0)
					netLocation.put("bearing", mNetworkBear);				
				netLocation.put("provider", Constants.GPS_PROVIDERS[2]);
				netLocation.put("timestamp", mNetworkTimestamp);

				locations.put(2, netLocation);
			}
			
			/*
			//sensor box gps provider
			if(mGpsProvider.equals(Constants.GPS_PROVIDERS[0]))
			{
				JSONObject boxLocation = new JSONObject();
				boxLocation.put("latitude", mValues[0]);
				boxLocation.put("longitude", mValues[1]);
				//boxLocation.put("accuracy", "null");
				boxLocation.put("provider", mGpsProvider);
				if(mSysTimestamp > 0)
					boxLocation.put("timestamp", mSysTimestamp);
				else
					boxLocation.put("timestamp", mBoxTimestamp);
				locations.put(0, boxLocation);
			}
			//phone gps provider
			else if(mGpsProvider.equals(Constants.GPS_PROVIDERS[1]))
			{
				JSONObject phoneLocation = new JSONObject();
				phoneLocation.put("latitude", mValues[0]);
				phoneLocation.put("longitude", mValues[1]);
				phoneLocation.put("accuracy", mAccuracy);
				phoneLocation.put("provider", mGpsProvider);
				if(mSysTimestamp > 0)
					phoneLocation.put("timestamp", mSysTimestamp);
				else
					phoneLocation.put("timestamp", mBoxTimestamp);
				locations.put(1, phoneLocation);
			}
			
			//network gps provider (addition to phone/sensor box gps provider)
			if(mNetworkLat != 0.0)
			{
				JSONObject netLocation = new JSONObject();
				netLocation.put("latitude", mNetworkLat);
				netLocation.put("longitude", mNetworkLon);
				netLocation.put("accuracy", mNetworkAcc);
				netLocation.put("provider", Constants.GPS_PROVIDERS[2]);
				netLocation.put("timestamp", mNetworkTimestamp);

				locations.put(2, netLocation);				
			}	
			*/
			object.put("locations", locations);
			
			JSONObject sensorbox = new JSONObject();
			sensorbox.put("mac", mBoxMac);
			sensorbox.put("version", "1");
			sensorbox.put("firmwareVersion", Constants.mFirmwareVersion);
			sensorbox.put("firmwareCompiler", "Arduino 1.0.2");
			sensorbox.put("onSince", mSourcePointNumber);
			sensorbox.put("calibration", new JSONObject());
			object.put("sensorbox", sensorbox);

			if((mUserData1 != null)&&(!mUserData1.equals("")))
			{
				String[] tags = mUserData1.split(" ");
				JSONArray tagsArray = new JSONArray();
				if((tags != null)&&(tags.length > 0))
				{
					for(int k = 0; k < tags.length; k++)
					{
						if((!tags[k].equals(""))||(!tags[k].equals("")))
							tagsArray.put(k, tags[k]);
					}
				}
					
				object.put("tags", tagsArray);
				
				//Log.d("Record", "toJson()--> "+object.toString());
			}
			//object.put("tagsCause", new JSONArray());
			//object.put("tagsLocation", new JSONArray());
			//object.put("tagsPerception", new JSONArray());			
		} 
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		/*
		catch(NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		catch(InvalidKeyException e)
		{
			e.printStackTrace();
		}
		catch(UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}*/
		
		//Log.d("Record", "toJson()--> json object: " +object.toString());
		return object;
	}
	
	//from a string, this method calculates a hash md5 hex string (128 bit)
	public static String md5(String s) 
	{
	    try 
	    {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        /*
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        */
	        
	        return convertToHex(messageDigest); 
	        
	    } 
	    catch (NoSuchAlgorithmException e) 
	    {
	        e.printStackTrace();
	    }
	    return "";
	}
	
	//improved algorithm to convert a md5 digest into an hex string
	private static final String convertToHex(byte[] data) { 
		if (data == null || data.length == 0) { 
			return null; 
		} 

		final StringBuffer buffer = new StringBuffer(); 
		for (int byteIndex = 0; byteIndex < data.length; byteIndex++) 
		{ 
			int halfbyte = (data[byteIndex] >>> 4) & 0x0F; 
		    int two_halfs = 0; 
		    do { 
		      if ((0 <= halfbyte) && (halfbyte <= 9)) 
		        buffer.append((char) ('0' + halfbyte)); 
		      else 
		        buffer.append((char) ('a' + (halfbyte - 10))); 
		      
		      halfbyte = data[byteIndex] & 0x0F; 
		    } 
		    while (two_halfs++ < 1); 
		} 

		return buffer.toString(); 
	}
}
