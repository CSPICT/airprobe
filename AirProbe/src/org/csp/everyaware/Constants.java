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

public class Constants 
{
	//************* message IDs for handler communication ******************************
	
	public static final int REQUEST_ENABLE_BT = 1010;
	public static final int BT_ACTIVATED = 1011;
	
	public static final int DISCOVERY_STARTED = 1012;
	public static final int DISCOVERY_FINISHED = 1013;
	public static final int DEVICE_DISCOVERED = 1014;
	
	public static final int CONNECTION_FAILED = 1015;
	public static final int CONNECTION_LOST = 1016;
	
	public static final int DOWNLOADING_HISTORY_STARTED = 1017;
	public static final int DOWNLOADING_HISTORY_FINISHED = 1018;
	
	public static final int DATA_TRANSFER_STARTED = 1019; //real time data transfer on
	
	public static final int SENSOR_BOX_MAC_NOT_READ = 1020;
	
    //************** Constants that show current connection status *********************
	
    public static final int STATE_NONE = 2010;       // we're doing nothing
    public static final int STATE_CONNECTING = 2011; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2012;  // now connected to a remote device
	
    //************** Constants that show gps status of sensor box *********************
      
    public static final int DEVICE_GPS_ON = 2013;
    public static final int DEVICE_GPS_OFF = 2014;
    
    //************** Constants that show witch activity is foreground *****************
    
    public static final int START = 3010;
    public static final int TRACK_MAP = 3011;
    public static final int COMM_MAP = 3012;
    public static final int GRAPH = 3013;
    public static final int SHARE = 3014;
    public static final int SBOX = 3015;
    
	//**************** messages received from store'n'forward service ********************/
	
	public static final String INTERNET_OFF = "internet_off";
	public static final String INTERNET_ON = "internet_on";	
	public static final String UPLOAD_ON = "upload_on";
	public static final String UPLOAD_OFF = "upload_off";
	public static final String FINISHED_UPLOAD = "finished_upload";
	
	public static final int INTERNET_OFF_INT = 3016;
	public static final int INTERNET_ON_INT = 3017;
	public static final int UPLOAD_ON_INT = 3018;
	
	//**************** messages about smartphone gps service *****************************/
	
	public static final String PHONE_GPS_ON = "phone_gps_on";
	public static final String PHONE_GPS_OFF = "phone_gps_off";
	public static final String NETWORK_GPS_ON = "network_gps_on";
    
	//*********** messages from Bluetooth History records download Manager ***************/
	
	public static final int UPDATE_PROGRESS = 2015;
	public static final int TOTAL_HISTORY_NUM = 2016;
	public static final int FINISHED_HIST_DOWN = 2017;
	public static final int NO_HIST_RECS = 2018;
	//Added by Patrick for improve bluetooth comunication
	public static final int BLUETOOTH_BUFFER_SIZE = 10000;
	public static final int BLUETOOTH_BUFFER_SIZE_LIVE = 4096;
	//----------------
	
    //************** Strings ************************************************************
    
	public final static String ROOT_DEVICE_NAME = "SensorBox";
	public final static String DEVICE = "device";
	
	public static final String DB_NAME = "airprobe_db";	
	public static final int DB_VERSION = 3; //v3 from AP 1.4	
    public static final String APP_NAME = "AirProbe";
    
    //hash algorithm calculated on json object
    public static final String hmac = "HMACSHA256";
    public static final String KEY = "SARACINO";

    //redirect server
    public static final String REDIRECT_ADDR = "http://smartcity.csp.it:8080/geoea/redirector.php";
    
    //server endpoints

    //black carbon cluster retrieval endpoint
    public static final String GET_BC_LEVELS_ADDR = "http://cs.everyaware.eu/event/airprobe/api/data/air/grid/kml?dim=1900,537&zoom="; //dim=1900, 537    	
    //everyaware official page register new login url
    public static final String CREATE_LOGIN_URL = "http://cs.everyaware.eu/event/overview/register";
    //everyaware official page perform login url to require an access token
    public static final String LOGIN_URL = "https://cs.everyaware.eu/oauth/token"; //?grant_type=password&client_id=airprobe_android_client&client_secret=SECRET&username=**email**&password=***";
    //everyaware official page perform refresh token to require a new access token
    public static final String REFRESH_TOKEN_URL = "https://cs.everyaware.eu/oauth/token"; //?grant_type=refresh_token&client_id=airprobe_android_client&client_secret=SECRET&refresh_token=8764812c-sdff-4a4d-982b-b4d666397baa";
    
    //secret key to log on official everyaware secret
    public static final String SECRET_KEY = "friiidsttldwii";
    
    //server error response message when asked to refresh access token
    public static final String INVALID_TOKEN = "invalid_token";

    //************** distance (millisec) between different history tracks ***************************
    //this is a value that means that let to separate two different series of records from temporal distance
    public static final long mHistoryTracksDistance = 60*5*1000; //five minutes
    
	//*************** values read from smartphone ****************************************
	
	public static String mUniquePhoneId = "";
	public static String mPhoneModel = "";
	
	//*************** values read from sensorbox *****************************************
	
	public static String mFirmwareVersion = "";
	public static String mMacAddress = "";
	
	//*************** server received status codes ***************************************
	
	public static final int STATUS_OK = 200;
	public static final int STATUS_JSON_NOT_READ = 400;
	public static final int STATUS_DUPLICATE_REQUEST = 409;
	public static final int STATUS_UNSUPPORTED_MEDIA_TYPE = 415;
	public static final int STATUS_INTERNAL_SERVER_ERROR = 500;
	public static final int STATUS_UNAUTHORIZED = 401;
	
	//*************** record age *********************************************************
	
	public static final long QUARTER_DAY_DELTA_TS = 60*60*6*1000;
	public static final long HALF_DAY_DELTA_TS = 60*60*12*1000;
	public static final long ONE_DAY_DELTA_TS = 60*60*24*1000;
	public static final long THREE_DAY_DELTA_TS = 60*60*24*3*1000;
	
	//*************** available track lengths ********************************************/
	
	public static final long FIVE_MINS = 60*5*1000; //5 minutes
	public static final long FIFTEEN_MINS = 60*15*1000; //15 minutes
	public static final long SIXTY_MINS = 60*60*1000; //60 minutes
	
	//**************** variables for store'n'forward service **********************/
	
	public static final int REC_TO_UPLOAD_MAX_NUM = 512; //max number of records uploadable in one time
	public static final String[] separators = {"-", "."}; // '-' is for italian CSP server, '.' is for official cs.everyaware.eu
	
	//**************** connection status visible as info in sensorbox activity ***********/
	
	public static final String CONNECTED = "Connected";
	public static final String DISCONNECTED = "Disconnected";
	public static final String DOWNLOADING = "Downloading";
	
	//**************** constants about facebook handler messages *****************************/
	
	public static final int LOGIN_COMPLETED = 4010;
	public static final int LOGIN_ERROR = 4011;
	public static final int LOGIN_CANCEL = 4012;
	public static final int LOGIN_FACEBOOK_ERROR = 4013;
	public static final int LOGIN_CLOSED = 4014;
	
	//*************** byte array messages sent to sensor box *********************************/
	
	public static final byte[] askForInfo = {'%','I'};
	public static final byte[] askForNumberHist = {'%', 'N'};
	public static final byte[] askForTurnOffRealTime = {'%', 'Y'};
	public static final byte[] askForHistory = {'%','S'};
	public static final byte[] askForRealTime = {'%','R'};
	
	public static final byte[] stopReceivingHist = {'%', 'T'}; 	
	public static final byte[] receivedRecordsNum = {'%','Z'}; //%Zn where n is the cardinality of the received set of <hr>
	
	public static final byte[] realTimeTagOpen = {'<', 'r', 't', '>'};
	public static final byte[] realTimeTagClose = {'<', '/', 'r', 't', '>'};
	public static final byte[] historyTagOpen = {'<', 'h', 'i', '>'};
	public static final byte[] historyTagClose = {'<', '/', 'h', 'i', '>'};
	/*
	public static final String realTimeTagOpen = "<rt>";
	public static final String realTimeTagClose = "</rt>";
	public static final String historyTagOpen = "<hi>";
	public static final String historyTagClose = "</hi>";
	*/
	//*************** byte array messages received from sensor box **************************/
	
	public static final byte[] endHistorySet = {'%', 'K'}; //every 50 history records (37 75)
	public static final byte[] endHistory = {'%', 'K', 'K'};
	
	//*************** for option menu ********************************************************/
	
    public static final String ITEM_TITLE = "title";  
    public static final String ITEM_CAPTION = "caption";  
    
    //*************** shared prefs references ************************************************/
    
    public static final String GPS_TRACKING = "gpsTrackingOn";
    public static final String COORDS = "coords";
    public static final String TWITTER_ON = "twitterOn";
    public static final String FACEBOOK_ON = "facebookOn";
    public static final String TRACK_LENGTH = "trackLength";
    public static final String SESSION_ID = "sessionId";
    public static final String INTERVAL = "interval";
    public static final String FACEBOOK_CHECKED_ON = "facebookCheckedOn";
    public static final String TWITTER_CHECKED_ON = "twitterCheckedOn";
    public static final String STEP = "step";
    public static final String FIRST_RECORD_ID = "firstRecordId";
    public static final String NEW_RECORD_ID = "newRecordId";
    public static final String DEVICE_ADDRESS = "deviceAddress";
    public static final String RECORD_AGE_IDX = "recordAgeIndex";
    public static final String STORE_FORW_IDX = "storeForwIndex";
    public static final String DOWN_HIST_IDX = "downloadHistIndex";
    public static final String DEVICE_FIRMWARE = "deviceFirmware";
    
    public static final String[] sharedPrefsKeys = {GPS_TRACKING, COORDS, TWITTER_ON, FACEBOOK_ON, 
    	TRACK_LENGTH, SESSION_ID, FACEBOOK_CHECKED_ON, TWITTER_CHECKED_ON, 
    	STEP, FIRST_RECORD_ID, NEW_RECORD_ID, DEVICE_ADDRESS, DEVICE_FIRMWARE};
    
    //***************** user options real values **********************************************/
    
    public static final long[] recordAges = {1000*60*60*24, 1000*60*60*6, 1000*60*60*12, 1000*60*60*36};
    public static final long[] storeForwFreqs = {1000*30, 1000, 1000*15, 1000*60, 1000*60*5};
    public static final boolean[] historyDown = {true, false};
    public static final long storeForwHistFreqs = 1000*10; //store'n'forward invocation frequency for synchronization mode
    
    //***************** constants for twitter **************************************************/
    
    public static final String CONSUMER_KEY = "koLpZkDCsn3lTd4nxNetA";
    public static final String CONSUMER_SECRET = "QP0p1lFD5tHAUwCEFbWp2RtACp3DMVIwVhEg3abE6p0";
    public static final String ACCESS_TOKEN = "552769274-2ymHI84bxjJa2zlTAXtM4Ynv2cIaVfL7vSNgozE7";
    public static final String ACCESS_TOKEN_SECRET = "6hcIU5ZhZd3cxwELKejVbks3JHDUjdzFWrlHxfwGYgQ";
	
    public static final String OAUTH_CALLBACK_SCHEME = "x-stn-oauth-twitter";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://callback";
    
    public static final String CALLBACK_URL = "airprobecsp://oauth";
    
    public static final String IEXTRA_AUTH_URL = "auth_url";
    public static final String IEXTRA_OAUTH_VERIFIER = "oauth_verifier";
    public static final String IEXTRA_OAUTH_TOKEN = "oauth_token";
    
    public static final String PREF_NAME = "com.example.android-twitter-oauth-demo";
    public static final String PREF_KEY_ACCESS_TOKEN = "access_token";
    public static final String PREF_KEY_ACCESS_TOKEN_SECRET = "access_token_secret";
    
    //******************* Black Carbon colors level ************************************/
    
    //very low, low, moderate, high, very high
    public static final String[] BC_COLORS = {"#dd0000ff", "#dd007fff", "#dd3fffbf", "#ddffff00", "#d8ffbf00", "#ddff3f00", "#d88b0000"};
    public static final String[] BC_LEVELS = {"Very low", "Low", "Moderate", "High", "Very high"};

    //******************* constants for http header ****************************************/
    
    public static final String[] DATA_VISIBILITY = {"DETAILS", "STATISTICS", "ANONYMOUS", "NONE"};
    public static final String[] GPS_PROVIDERS = {"sensorbox", "gps", "network", "none"};
    
    //******************* localization type constants **************************************/
    
    //used only during record saving on internal DB; not used to send data to server in json
    public static final String[] LOCALIZATION ={"indoor", "outdoor"};
    
    //city names and their centres coords are necessary to load the correct modelParams[cityname].txt file relative to the city
    public static final String[] CITY_NAMES = {"Turin", "Kassel", "Antwerp", "London"};
    public static final double[][] CITIES_COORDS = {{45.077157, 7.686253}, {51.325463, 9.476852}, {51.230108, 4.406776}, {51.519853, -0.110591}};
    public static final String[] MODEL_PARAMS_NAMES = {"modelParamsTurin.txt", "modelParamsKassel.txt", "modelParamsAntwerp.txt", "modelParamsLondon.txt"};
    
    //************************ credits urls ************************************************/
    
    public static final String URL_ISI = "http://www.isi.it/";
    public static final String URL_SAPIENZA = "http://www.phys.uniroma1.it/fisica/";
    public static final String URL_CSP = "http://www.csp.it/";
    public static final String URL_L3S = "http://www.l3s.de/en/home/";
    public static final String URL_VITO = "http://www.vito.be/VITO/EN/HomepageAdmin/Home";
    public static final String URL_UCL = "http://www.ucl.ac.uk/excites";
    public static final String URL_EA = "http://cs.everyaware.eu";
}
