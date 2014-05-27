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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csp.everyaware.db.Record;
import org.csp.everyaware.Constants;
import org.csp.everyaware.ExtendedLatLng;
import org.csp.everyaware.Utils;
import org.csp.everyaware.db.Record.RecordMetaData;
import org.csp.everyaware.db.SemanticSessionDetails.SemanticMetaData;
import org.csp.everyaware.db.Track.TrackMetaData;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DbManager 
{
	private DbHelper mDbHelper;
	private Context mContext;
	private SQLiteDatabase mDb; //riferimento al DB	
	private static DbManager mDbManager = null;
	
	//restituisce un'istanza di DbManager
	public static DbManager getInstance(Context ctx)
	{
		//se passo il context, creo l'istanza se non è stata già creata, oppure
		//restituisco l'istanza già creata
		if(ctx != null) 
		{
			if(mDbManager == null)
				mDbManager = new DbManager(ctx);
			return mDbManager;
		}
		//se non passo il context (ovvero passo null), restituisco l'istanza già creata
		//se non è già stata creata restituisco null
		else
			return mDbManager;
	}
	
	private DbManager(Context ctx)
	{
		mContext = ctx;
		mDbHelper = new DbHelper(ctx, Constants.DB_NAME, null, Constants.DB_VERSION);
		Constants.mUniquePhoneId = Utils.readUniquePhoneID(mContext);
		Constants.mPhoneModel = Utils.readPhoneModel();
	}
	
	public void openDb()
	{		
		mDb = mDbHelper.getWritableDatabase();		
	}
	
	public void closeDb()
	{
		if(mDb != null)
			mDb.close();
	}
	
	public void createDb()
	{
		mDbHelper.onCreate(mDbHelper.getWritableDatabase());
	}
	
	public void deleteDb()
	{
		mDbHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+RecordMetaData.TABLE_NAME);		
		mDbHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+RecordMetaData.TABLE_TAGS_NAME);	
		mDbHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS "+TrackMetaData.TABLE_NAME);	
	}
	
    /**************** DEFINIZIONE SQLiteOpenHelper ********************************************************/
	
	private class DbHelper extends SQLiteOpenHelper
	{
		public DbHelper(Context ctx, String name, CursorFactory factory,int version) 
		{
			super(ctx, name, factory, version);
		}
		
		//Attenzione: questo metodo viene invocato solo alla creazione del database!
		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			//creazione della tabella records
			db.execSQL(Record.createRecordsTableSQL().toString());			
			db.execSQL(Record.createRecordsWithTagsTableSQL().toString());
			db.execSQL(Track.createTracksTableSQL().toString());
			db.execSQL(SemanticSessionDetails.createSemanticTableSQL().toString());
			
			Log.d("DbManager", "onCreate()--> created DB: " +db.getPath());
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			if((oldVersion == 1)&&(newVersion >= 2))
			{
				Log.d("DbManager", "onUpgrade()--> upgrading DB version from V1 to V2");
				db.execSQL(Track.createTracksTableSQL().toString());				
				db.execSQL(SemanticSessionDetails.createSemanticTableSQL().toString());
			}
			else if((oldVersion == 2)&&(newVersion > 2))
			{
				Log.d("DbManager", "onUpgrade()--> upgrading DB version from V2 to V3");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_SESSION_SEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_SESSION_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_POINT_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_SESSION_SEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_SESSION_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_POINT_NUMBER+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_LAT+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_LON+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_ACC+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_ALTITUDE+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.PHONE_TS+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_LAT+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_LON+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_ACC+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_ALTITUDE+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_ALTITUDE+" TEXT");
		
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_NAME+" ADD COLUMN "+RecordMetaData.BOX_MAC_ADDRESS+" TEXT");
				
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_SESSION_SEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_SESSION_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SOURCE_POINT_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_SESSION_SEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_SESSION_NUMBER+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.SEMANTIC_POINT_NUMBER+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_LAT+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_LON+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_ACC+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_ALTITUDE+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.PHONE_TS+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_LAT+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_LON+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_ACC+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_ALTITUDE+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_NUM_SATS+" TEXT");
				
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_BEAR+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_SPEED+" TEXT");
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.NETWORK_ALTITUDE+" TEXT");
		
				db.execSQL("ALTER TABLE "+RecordMetaData.TABLE_TAGS_NAME+" ADD COLUMN "+RecordMetaData.BOX_MAC_ADDRESS+" TEXT");
				
				db.execSQL(SemanticSessionDetails.createSemanticTableSQL().toString());
			}
		}
	};	//fine SQLiteOpenHelper  
	
	/************************ COMANDI SQLITE *****************************************************/

	//create a new entry in the table, by specifying souceId, semanticId and onSinceStart fields (onSinceStop remanins empty for now)
	//RETURNS true if the entry is successfully created
	public boolean createSemanticSessionEntry(String sourceSessionSeed, int sourceSessionNumber, String semanticSessionSeed, int semanticSessionNumber, int onSinceStart)
	{
		ContentValues values = new ContentValues();
		values.put(SemanticMetaData.SOURCE_SESSION_SEED, sourceSessionSeed);
		values.put(SemanticMetaData.SOURCE_SESSION_NUMBER, sourceSessionNumber);
		values.put(SemanticMetaData.SEMANTIC_SESSION_SEED, semanticSessionSeed);
		values.put(SemanticMetaData.SEMANTIC_SESSION_NUMBER, semanticSessionNumber);
		values.put(SemanticMetaData.POINT_NUMBER_START, onSinceStart);
		
		return (mDb.insert(SemanticMetaData.TABLE_NAME, null, values) != -1);
	}
	
	//update onSinceStop column for couple <souceSessionSeed, sourceSessionNumber> entry: when onSinceStop acquires a value != null, the semantic session has to be considered closed
	//RETURNS true if the entry is successfully updated (--> closed)
	/*
	public boolean closeSemanticSessionEntry(String semanticSessionSeed, int semanticSessionNumber, int onSinceStop)
	{
		ContentValues values = new ContentValues();
		values.put(SemanticMetaData.POINT_NUMBER_END, onSinceStop);
		
		return (mDb.update(SemanticMetaData.TABLE_NAME, values, SemanticMetaData.SEMANTIC_SESSION_SEED+"='"+semanticSessionSeed+"' AND "+SemanticMetaData.SEMANTIC_SESSION_NUMBER+"="+semanticSessionNumber, 
				null) > 0);
	}*/
	
	//improved to the above version: close all entry (eventually) open with the same onSinceStop value. This is conceptually correct 
	//because only one entry per time should be open, so this approach corrects possibile errors
	public boolean closeSemanticSessionEntry(int onSinceStop)
	{
		ContentValues values = new ContentValues();
		values.put(SemanticMetaData.POINT_NUMBER_END, onSinceStop);
		
		return (mDb.update(SemanticMetaData.TABLE_NAME, values, null, null) > 0);
	}
	
	public SemanticSessionDetails getOpenSemanticSessionEntry()
	{
		Cursor c = mDb.query(SemanticMetaData.TABLE_NAME, SemanticMetaData.COLUMNS, SemanticMetaData.POINT_NUMBER_END+"=-1", null, null, null, null);
		while(c.moveToNext())
		{
			Log.d("DbManager", "getOpenSemanticSessionEntry()--> "+c.getString(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_SEED))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_NUMBER))+" "
					+c.getString(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_SEED))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_NUMBER))+" "
					+c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_START))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_END)));
			
			return new SemanticSessionDetails(c.getString(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_SEED)), c.getInt(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_NUMBER)), 
					c.getString(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_SEED)), c.getInt(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_START)), c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_END)));
		}
		Log.d("DbManager", "getOpenSemanticSessionEntry()--> NOT FOUND open semantic session");
		return null;
	}
	
	//only to log purposes
	public void printSemanticSessionEntries()
	{
		Cursor c = mDb.query(SemanticMetaData.TABLE_NAME, SemanticMetaData.COLUMNS, null, null, null, null, null);
		
		while(c.moveToNext())
			Log.d("DbManager", "printSemanticSessionEntries()--> "+c.getString(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_SEED))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_NUMBER))+" "
					+c.getString(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_SEED))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_NUMBER))+" "
					+c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_START))+" "+c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_END)));
	}
	
	//query the table to check if the actual couple <souceSessionSeed, sourceSessionNumber> (taken from actual record) is assigned to any semanticId entry
	//returns the semantic object if the entry is found
	//the actual record is assigned to a couple <semanticSessionSeed, semanticSessionNumber> if its onSince value is bigger than the onSinceStart value 
	//(and smaller than onSinceStop value, if this is different than 'null') of the entry
	//NOTE: THIS METHOD IS USEFUL ONLY FOR HISTORY RECORDS!!!!!!
	public SemanticSessionDetails checkIfActualSourceIdBelongsToASemanticSessionEntry(String sourceSessionSeed, int sourceSessionNumber, int onSince)
	{
		Cursor c = mDb.query(SemanticMetaData.TABLE_NAME, SemanticMetaData.COLUMNS, SemanticMetaData.SOURCE_SESSION_SEED+"='"+sourceSessionSeed+"' AND "
				+SemanticMetaData.SOURCE_SESSION_NUMBER+"="+sourceSessionNumber, null, null, null, null);
		
		SemanticSessionDetails semantic = null;
		boolean found = false;

		//printSemanticSessionEntries();
		
		Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> number of entries: "+c.getCount());
		Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> input sourceSessionSeed: "+sourceSessionSeed+" sourceSessionNumber: "+sourceSessionNumber+" onSince: "+onSince);
		
		//the query can return 0, 1 or more entries with the same couple <souceSessionSeed, sourceSessionNumber>. Now I check if there is an entry 'compatible' on which
		//onSinceStart is < actual onSince and onSinceStop doesn't exist or is > actual onSince
		while(c.moveToNext()&&(!found))
		{
			semantic = new SemanticSessionDetails(c.getString(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_SEED)), c.getInt(c.getColumnIndex(SemanticMetaData.SOURCE_SESSION_NUMBER)), 
					c.getString(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_SEED)), c.getInt(c.getColumnIndex(SemanticMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_START)), c.getInt(c.getColumnIndex(SemanticMetaData.POINT_NUMBER_END)));
				
			if(onSince >= semantic.mPointNumberStart)
			{
				if(semantic.mPointNumberEnd != -1) //close semantic session entry found
				{
					//if actual record onSince value is betweend point number start and point number end, the record must be marked with the actual couple <semanticSessionSeed, semanticSessionNumber>
					if(onSince <= semantic.mPointNumberEnd)
					{
						Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> found - onSince: "+onSince+" onSinceStart: "+semantic.mPointNumberStart+" onSinceStop: "+semantic.mPointNumberEnd);
						Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> found semantic session seed: "+semantic.mSemanticSessionSeed+ " semantic session number: "
								+semantic.mSemanticSessionNumber);
						
						found = true;						
					}
					//else, if the onSince value of the actual record is bigger than the point number end value of a closed entry, the entry must be removed, but only if the actual record is a
					//history record!!!
					else if(onSince > semantic.mPointNumberEnd)
					{
						if(mDb.delete(SemanticMetaData.TABLE_NAME, SemanticMetaData.SOURCE_SESSION_SEED+"='"+sourceSessionSeed+"' AND "
								+SemanticMetaData.SOURCE_SESSION_NUMBER+"="+sourceSessionNumber+" AND "+SemanticMetaData.POINT_NUMBER_START+"="+semantic.mPointNumberStart+" AND "+SemanticMetaData.POINT_NUMBER_END+"="+semantic.mPointNumberEnd, null) == 1)
							Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> found and removed old entry with semantic session seed: "+semantic.mSemanticSessionSeed
									+" and semantic session number: "+semantic.mSemanticSessionNumber+ " onSinceStart: "+semantic.mPointNumberStart+" onSinceEnd: "+semantic.mPointNumberEnd);
						else
							Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> error on deleting old entry in semantic table");
					}
				}			
				else //semantic session entry found, but still open!
				{
					Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> found - onSince: "+onSince+" onSinceStart: "+semantic.mPointNumberStart+" onSinceStop: "+semantic.mPointNumberEnd);
					Log.d("DbManager", "checkIfActualSourceIdBelongsToASemanticSessionEntry()--> found semantic session seed: "+semantic.mSemanticSessionSeed+ " semantic session number: "
							+semantic.mSemanticSessionNumber);
					
					found = true;
				}
			}
		}
		if(found)
			return semantic;
		else
			return null;
	}
	
	
	//load all entry from 'tracks' table and return a list of HashMap<String, String> where the List represent a single track
	public List<Map<String, String>> loadAllTracks()
	{
		List<Map<String,String>> trackList = new ArrayList<Map<String,String>>();
		
		Cursor c = mDb.query(TrackMetaData.TABLE_NAME, TrackMetaData.COLUMNS, null, null, null, null, null);	
		
		while(c.moveToNext())
		{
			String sid = c.getString(c.getColumnIndex(TrackMetaData.SESSION_ID));
			
			//check if actual session id has matching records on 'records' table
			if(checkIfTrackRecordsExist(sid))
			{
				Track track = new Track(sid, c.getLong(c.getColumnIndex(TrackMetaData.FIRST_SYS_TS)), c.getLong(c.getColumnIndex(TrackMetaData.FIRST_BOX_TS)),
						c.getLong(c.getColumnIndex(TrackMetaData.LAST_SYS_TS)), c.getLong(c.getColumnIndex(TrackMetaData.LAST_BOX_TS)), c.getInt(c.getColumnIndex(TrackMetaData.NUM_OF_RECS)),
						c.getInt(c.getColumnIndex(TrackMetaData.NUM_OF_UPL_RECS)));

				HashMap<String, String> trackMap = new HashMap<String, String>();				
				trackMap.put("sessionId", track.mSessionId);
				trackMap.put("formattedSessionId", track.getFormattedSessionId());
				trackMap.put("trackLength", track.getTrackLength());
				trackMap.put("firstSysTs", String.valueOf(track.mFirstSysTimestamp));
				trackMap.put("firstBoxTs", String.valueOf(track.mFirstBoxTimestamp));
				trackMap.put("lastSysTs", String.valueOf(track.mLastSysTimestamp));
				trackMap.put("lastBoxTs", String.valueOf(track.mLastBoxTimestamp));
				trackMap.put("numOfRecs", String.valueOf(track.mNumOfRecords));
				trackMap.put("numOfUploadedRecs", String.valueOf(track.mUploadedRecs));
	
				trackList.add(trackMap);
			}
			else //delete actual track entry in 'tracks' table
				mDb.delete(TrackMetaData.TABLE_NAME, TrackMetaData.SESSION_ID+"='"+sid+"'", null);
		}
		
		return trackList;
	}	
	
	//check if actual entry on 'tracks' table, identified by sessionId, has match on 'records' table
	public boolean checkIfTrackRecordsExist(String sessionId)
	{
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, new String[]{RecordMetaData.SESSION_ID}, RecordMetaData.SESSION_ID+"='"+sessionId+"'", null, null, null, null, "1");
		if(c.getCount() > 0)
		{
			//Log.d("DbManager", "checkIfTrackRecordsExist()--> sessionId: " +sessionId+" exists");
			return true;
		}
		else
		{
			Log.d("DbManager", "checkIfTrackRecordsExist()--> sessionId: " +sessionId+" DOES NOT exist");
			return false;
		}
	}

	//return track specified by session id
	public Track loadTrackBySessionId(String sessionId)
	{
		Track track = null;
		
		Cursor c = mDb.query(TrackMetaData.TABLE_NAME,  TrackMetaData.COLUMNS, TrackMetaData.SESSION_ID+"="+"'"+sessionId+"'", null, null, null, null);
		
		while(c.moveToNext())
		{
			track = new Track(c.getString(c.getColumnIndex(TrackMetaData.SESSION_ID)), c.getLong(c.getColumnIndex(TrackMetaData.FIRST_SYS_TS)), c.getLong(c.getColumnIndex(TrackMetaData.FIRST_BOX_TS)),
					c.getLong(c.getColumnIndex(TrackMetaData.LAST_SYS_TS)), c.getLong(c.getColumnIndex(TrackMetaData.LAST_BOX_TS)), c.getInt(c.getColumnIndex(TrackMetaData.NUM_OF_RECS)), 0);
		}
		return track;
	}
	
	/*
	//load from records table grouping by session id field
	public List<Map<String, String>> loadTracks()
	{
		List<Map<String,String>> trackList = new ArrayList<Map<String,String>>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				null, null, RecordMetaData.SESSION_ID, null, null);	
		
		while(c.moveToNext())
		{
			String sessionId = c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID));
	
			Track track = getTrackDetails(sessionId);
				
			HashMap<String, String> trackMap = new HashMap<String, String>();				
			trackMap.put("sessionId", sessionId);
			trackMap.put("formattedSessionId", track.getFormattedSessionId());
			trackMap.put("trackLength", track.getTrackLength());
			trackMap.put("firstSysTs", String.valueOf(track.mFirstSysTimestamp));
			trackMap.put("firstBoxTs", String.valueOf(track.mFirstBoxTimestamp));
			trackMap.put("lastSysTs", String.valueOf(track.mLastSysTimestamp));
			trackMap.put("lastBoxTs", String.valueOf(track.mLastBoxTimestamp));
			trackMap.put("numOfRecs", String.valueOf(track.mNumOfRecords));
			trackMap.put("numOfUploadedRecs", String.valueOf(track.mUploadedRecs));

			trackList.add(trackMap);
		}
		
		return trackList;
	}
	*/
	//read all records for a given session id, get first ts, last ts, number of records and
	//returns a track object
	/*
	public Track getTrackDetails(String sessionId)
	{
		long firstSysTs = 0;
		long firstBoxTs = 0;
		long lastSysTs = 0;
		long lastBoxTs = 0;
		
		Track track = null;
		
		//read all records for sessionId
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.SESSION_ID+"='"+sessionId+"'", null, null, null, null);	
		
		int counter = 0;
		int indexLast = c.getCount()-1;
		
		int countUploaded = 0;
		
		double topLat = 0;
		double leftLon = 0;
		double bottomLat = 0;
		double rightLon = 0;
		
		while(c.moveToNext())
		{
			long uploadTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			if(uploadTs > 0)
				countUploaded++;
			
			double lat = c.getDouble(c.getColumnIndex(RecordMetaData.LAT));
			double lon = c.getDouble(c.getColumnIndex(RecordMetaData.LON));
			
			//get timestamps of first record
			if(counter == 0)
			{
				firstSysTs = c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS));
				firstBoxTs = c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS));
				
				topLat = lat;
				leftLon = lon;
				
				bottomLat = lat;
				rightLon = lon;
			}
			else
			{
				if(lat > topLat)
					topLat = lat;
				else if(lat < bottomLat)
					bottomLat = lat;
				
				if(lon < leftLon)
					leftLon = lon;
				else if(lon > rightLon)
					rightLon = lon;
			}
			
			//get timestamps of last record
			if(counter == indexLast)
			{
				lastSysTs = c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS));
				lastBoxTs = c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS));
			}
			counter++;
		}
		
		track = new Track(sessionId, firstSysTs, firstBoxTs, lastSysTs, lastBoxTs, indexLast+1, countUploaded);
			
		return track;
	}*/
	
	//load extended latlng by sessionId
	public List<ExtendedLatLng> loadLatLngPointsBySessionId(String sessionId, int div)
	{
		List<ExtendedLatLng>latlngPoints = new ArrayList<ExtendedLatLng>();
		
		double[] values = new double[12];
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME,  RecordMetaData.COLUMNS, 
				RecordMetaData.SESSION_ID+"='"+sessionId+"'", 
				null, null, null, null);	
		
		int counter = 0;
		
		while(c.moveToNext())
		{
			if(c.getDouble(c.getColumnIndex(RecordMetaData.LAT)) != 0)
			{
				String annotation = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
				
				if((counter % div == 0)||(!annotation.equals("")))
				{
					values[0] = c.getDouble(c.getColumnIndex(RecordMetaData.LAT));
					values[1] = c.getDouble(c.getColumnIndex(RecordMetaData.LON));
					values[2] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_1));
					values[3] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_2));
					values[4] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_3));
					values[5] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_4));
					values[6] = c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1));
					values[7] = c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2));
					values[8] = c.getDouble(c.getColumnIndex(RecordMetaData.VOC));
					values[9] = c.getDouble(c.getColumnIndex(RecordMetaData.O3));
					values[10] = c.getDouble(c.getColumnIndex(RecordMetaData.TEMP));
					values[11] = c.getDouble(c.getColumnIndex(RecordMetaData.HUM));
										
					ExtendedLatLng geoPoint = new ExtendedLatLng(c.getDouble(c.getColumnIndex(RecordMetaData.ID)),
						c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
						c.getDouble(c.getColumnIndex(RecordMetaData.LON)), 
						c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)), values, 
						c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE)),
						c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1)));
					
					latlngPoints.add(geoPoint);
				}
			}
			counter++;
		}

		return latlngPoints;
	}	
	
	//query table of records with tags grouping records by session ids and returns the list of those session ids
	public List<String> getSidsOfRecordsWithTags()
	{
		List<String> sids = new ArrayList<String>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_TAGS_NAME, RecordMetaData.COLUMNS, null, null, RecordMetaData.SESSION_ID, null, null);
		
		while(c.moveToNext())
			sids.add(c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)));		
		
		return sids;
	}
	
	//load records for a given sessionId from table of records with tags 
	public List<Record>loadRecordsWithTagBySessionId(String sessionId)
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_TAGS_NAME, RecordMetaData.COLUMNS, RecordMetaData.SESSION_ID+"='"+sessionId+"'", null, null, null, null);
		
		while(c.moveToNext())
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),					 
					networkLoc, networkTs, 
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),	
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */
					);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			recordsList.add(record);
		}
		
		return recordsList;
	}
	
	public void deleteRecordsWithTagsBySessionId(String sessionId)
	{
		int result = mDb.delete(RecordMetaData.TABLE_TAGS_NAME, RecordMetaData.SESSION_ID+"='"+sessionId+"'", null);
		Log.d("DbManager", "deleteRecordsWithTagsBySessionId()--> # of deleted records: "+result);
	}
	
	//update record with inserted user annotation
	public int updateRecordAnnotation(double recordId, String annotation)
	{
		ContentValues recordValue = new ContentValues();
		recordValue.put(RecordMetaData.USER_DATA_1, annotation);
		
		//update tag on recorded record
		int result = mDb.update(RecordMetaData.TABLE_NAME, recordValue, RecordMetaData.ID+"=" + recordId, null);
	
		//save the record with tags on a different table containing only records with tag
		Record recordWithTags = loadRecordById(recordId);		
		mDb.insert(RecordMetaData.TABLE_TAGS_NAME, null, recordWithTags.getRecordValues());
		
		return result;
	}
	
	//marks uploded records with upload timestamp in a FAST WAY using sql statement
	//invoked in PostDataThread class in StoreAndForward class when upload is finished successfully
	public int updateUploadedRecord(List<Record>recordsToSend, long uploadSysTs)
	{
		boolean done = false;
		
		String updateRecordQuery = "UPDATE "+RecordMetaData.TABLE_NAME+" SET " +RecordMetaData.UPLOAD_SYS_TS+" = "+uploadSysTs
				+" WHERE "+RecordMetaData.ID+"=?";
		
		String updateTrackQuery="UPDATE "+TrackMetaData.TABLE_NAME+" SET \"" +TrackMetaData.NUM_OF_UPL_RECS+"\" = "+TrackMetaData.NUM_OF_UPL_RECS+" + 1 "
		//String updateTrackQuery="UPDATE "+TrackMetaData.TABLE_NAME+" SET \"" +TrackMetaData.NUM_OF_UPL_RECS+"\" = 100 "
				+" WHERE "+TrackMetaData.SESSION_ID+"=?";
		
		int size = recordsToSend.size();
		int updatedRecCount = 0;
		
		//Log.d("DbManager", "updateUploadedRecord()--> # recs to update: "+size);
		try 
		{
			//PRAGMA synchronous=OFF can speed up the insertion process quite a lot, but it leaves you vulnerable 
			//to database corruption if the application crashes during the bulk insertion. 
			mDb.execSQL("PRAGMA synchronous=OFF");

			mDb.beginTransaction();
			
			SQLiteStatement stRec = mDb.compileStatement(updateRecordQuery);
			SQLiteStatement stTrack = mDb.compileStatement(updateTrackQuery);
			
			for(int i = 0; i < size; i++)
			{
            	stRec.bindDouble(1, recordsToSend.get(i).mId);			
            	stRec.execute();
            	
            	stTrack.bindString(1, recordsToSend.get(i).mSessionId);
            	stTrack.execute();     
            	
            	updatedRecCount++;
			}
			
            mDb.setTransactionSuccessful();
            done = true;
        } 
		catch(java.util.ConcurrentModificationException e)
		{
			e.printStackTrace();
		}
		catch (SQLException e) 
		{
            e.printStackTrace();
        } 
		finally 
		{
        	mDb.endTransaction();
        	mDb.execSQL("PRAGMA synchronous=NORMAL");            
        }
		
		if(done)
			return size;
		else 
			return updatedRecCount;
	}
	
	//DEBUG METHOD
	public void countUplodedRecords()
	{
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, RecordMetaData.UPLOAD_SYS_TS+">0", null, null, null, null);
		Log.d("DbManager", "updateUploadedRecord()--> count total uploaded: "+c.getCount());
	}
	
	//DEBUG METHOD
	public void printTracks()
	{
		Cursor c = mDb.query(TrackMetaData.TABLE_NAME, TrackMetaData.COLUMNS, null, null, null, null, null);
		while(c.moveToNext())
		{
			Log.d("DbManager", "printTracks()--> sid: " +c.getString(c.getColumnIndex(TrackMetaData.SESSION_ID))+ " # of recs: " +c.getInt(c.getColumnIndex(TrackMetaData.NUM_OF_RECS))+ 
					" # of upl recs: " +c.getInt(c.getColumnIndex(TrackMetaData.NUM_OF_UPL_RECS)));
		}
	}
	
	//aggiorna un record inviato al server con la data di sistema al momento dell'invio
	//invoked in PostDataThread class in StoreAndForward class when upload is finished successfully
	/*
	public int updateUploadedRecord(double recordId, long uploadSysTs)
	{
		ContentValues recordValue = new ContentValues();
		recordValue.put(RecordMetaData.UPLOAD_SYS_TS, uploadSysTs);
		
		return mDb.update(RecordMetaData.TABLE_NAME, recordValue, RecordMetaData.ID+"=" + recordId, null);
	}*/
	
	//increment number of uploaded records field in track specified by sessionId
	//invoked in PostDataThread class in StoreAndForward class when upload is finished successfully
	/*
	public void incrUploadNumberInTrack(String sessionId, int quantity)
	{
		mDb.beginTransaction();
		
		//update num of uploaded records for entry in 'tracks' table with session id
		String updateQuery="update "+TrackMetaData.TABLE_NAME+" SET \"" +TrackMetaData.NUM_OF_UPL_RECS+"\" = "+TrackMetaData.NUM_OF_UPL_RECS+" + "+quantity
				+" WHERE "+TrackMetaData.SESSION_ID+" = '"+sessionId+"'";
		mDb.execSQL(updateQuery);
		
		mDb.setTransactionSuccessful();
		mDb.endTransaction();
		
		Log.d("DbManager", "incrUploadNumberInTrack()--> "+updateQuery);
	}*/

	//delete an entry specified by sessionId in 'tracks' table
	public void deleteTrack(String sessionId)
	{
		int count = mDb.delete(TrackMetaData.TABLE_NAME, TrackMetaData.SESSION_ID+"='"+sessionId+"'", null);
		
		Log.d("DbManager", "deleteTrack()--> count deleted track: " +count);
	}
	
	//save new real time record on DB and store its id in shared prefs
	public double saveRecord(long sysTs, long boxTs, double[] values, String sessionId, String localization, String gpsProvider, double accuracy, double[] networkLoc, long networkTs, 
			String sourceSessionSeed, int sourceSessionNumber, int sourcePointNumber, String semanticSessionSeed, int semanticSessionNumber, int semanticPointNumber, 
			double phoneLat, double phoneLon, double phoneAcc, double phoneBear, double phoneSpeed, double phoneAltitude, long phoneTimestamp, 
			double boxLat, double boxLon, double boxAcc, double boxBear, double boxSpeed, double boxAltitude, int boxNumSats,
			double networkBear, double networkSpeed, double networkAltitude, 
			String boxMac)
	{
		Record newRec = new Record(sysTs, boxTs, values, sessionId, localization, gpsProvider, accuracy, networkLoc, networkTs, 
				sourceSessionSeed, sourceSessionNumber, sourcePointNumber, semanticSessionSeed, semanticSessionNumber, semanticPointNumber, 
				phoneLat, phoneLon, phoneAcc, phoneBear, phoneSpeed, phoneAltitude, phoneTimestamp, 
				boxLat, boxLon, boxAcc, boxBear, boxSpeed, boxAltitude, boxNumSats,
				networkBear, networkSpeed, networkAltitude, 
				boxMac);
				
		newRec.mUniquePhoneId = Constants.mUniquePhoneId;
		newRec.mPhoneModel = Constants.mPhoneModel;
		newRec.mUserData2 = Constants.mFirmwareVersion;
		//newRec.mUserData3 = Constants.mMacAddress;
		
		try
		{
			newRec.mBcMobile = Utils.bc;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			newRec.mBcMobile = 0;
		}
		
		//1 - save real time record on 'records' table 
		double newRecordId = mDb.insert(RecordMetaData.TABLE_NAME, null, newRec.getRecordValues());
		
		/*************** UPDATE SECTION OF 'TRACKS' TABLE ************************/
		
		//2 - update 'tracks' table with new data
		ContentValues trackValue = new ContentValues();
		trackValue.put(TrackMetaData.LAST_SYS_TS, sysTs);
		trackValue.put(TrackMetaData.LAST_BOX_TS, boxTs);
		
		Log.d("DbManager", "saveRecord()--> " +newRec.toString());
		
		//try to update entry for actual sessionId on 'tracks' table
		if(mDb.update(TrackMetaData.TABLE_NAME, trackValue, TrackMetaData.SESSION_ID+"='"+sessionId+"'", null) != 1)
		{
			Log.d("DbManager", "saveRecords()--> track entry doesn't exist, creating new...");
			
			//if update fails, no entry for actual sessionId exists, so create it
			//create new Track object
			Track track = new Track(sessionId, sysTs, boxTs, sysTs, boxTs, 1, 0);			
			//store it on 'tracks' table
			mDb.insert(TrackMetaData.TABLE_NAME, null, track.getTrackValues());
		}
		//if update works on an existent entry, update the same entry again to increment by one the num of records
		else
		{
			//update num of records count for entry in 'tracks' table with session id
			String updateQuery="update "+TrackMetaData.TABLE_NAME+" SET \"" +TrackMetaData.NUM_OF_RECS+"\" = \""+TrackMetaData.NUM_OF_RECS+"\" +  1 "
					+" WHERE "+TrackMetaData.SESSION_ID+" = '"+sessionId+"'";
	        
			mDb.beginTransaction();
			try 
			{
			    mDb.execSQL(updateQuery);
				mDb.setTransactionSuccessful();
				
				Log.d("DbManager", "saveRecords()--> executed query: "+updateQuery);
			}
			finally 
			{
				mDb.endTransaction();
			}	
		}
		/* DEBUG
		Cursor debug = mDb.query(TrackMetaData.TABLE_NAME, TrackMetaData.COLUMNS, null, null, null, null, null);
		while(debug.moveToNext())
		{
			long sid = debug.getLong(debug.getColumnIndex(TrackMetaData.SESSION_ID));
			long firstSysTs = debug.getLong(debug.getColumnIndex(TrackMetaData.FIRST_SYS_TS));
			long firstBoxTs = debug.getLong(debug.getColumnIndex(TrackMetaData.FIRST_BOX_TS));
			long lastSysTs = debug.getLong(debug.getColumnIndex(TrackMetaData.LAST_SYS_TS));
			long lastBoxTs = debug.getLong(debug.getColumnIndex(TrackMetaData.LAST_BOX_TS));
			
			int count = debug.getInt(debug.getColumnIndex(TrackMetaData.NUM_OF_RECS));
			int uplCount = debug.getInt(debug.getColumnIndex(TrackMetaData.NUM_OF_UPL_RECS));
			
			Log.d("DbManager", "saveRecords()--> track: " +sid+" "+firstSysTs+" "+firstBoxTs+" "+lastSysTs+" "+lastBoxTs+" "+count+" "+uplCount);
			
		}*/
		
		/**************** END OF UPDATE SECTION OF 'TRACKS' TABLE **********************/
		
		//memorizzo nelle SharedPrefs l'id del record appena salvato
		Utils.setNewRecordId(newRecordId, mContext);
		
		//update total number of records saved on db by 1
		Utils.incrTotalStoredRecCount(mContext, 1);
		
		//make a shared ram copy of record
		newRec.mId = newRecordId;
		Utils.lastSavedRecord = newRec;
		
		return newRecordId;
	}

	//carica tutti i records e ne restituisce la lista
	public List<AnnotatedRecord> loadAnnotatedRecords()
	{
		List<AnnotatedRecord>recordsList = new ArrayList<AnnotatedRecord>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.USER_DATA_1 + "<> ''", null, null, null, null);	

		while(c.moveToNext())
		{
			double avgPoll = c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)) +  
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)) + 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)) +
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)) +
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)) +
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)) +
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)) +
					c.getDouble(c.getColumnIndex(RecordMetaData.O3));
		
			avgPoll = avgPoll/8;
			
			AnnotatedRecord record = new AnnotatedRecord(c.getDouble(c.getColumnIndex(RecordMetaData.ID)), 
					avgPoll, c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)), 
					c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)));
			
			Log.d("DbManager", record.toString());
			
			recordsList.add(record);
		}
		
		return recordsList;
	}	
	
	//carica tutti i records e ne restituisce la lista
	/*
	public List<Record> loadAllRecords()
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				null, null, null, null, null);	

		while(c.moveToNext())
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					c.getLong(c.getColumnIndex(RecordMetaData.SB_TIME_ON)),
					networkLoc, networkTs);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			recordsList.add(record);
		}
		
		return recordsList;
	}*/
	
	//carica tutti i records e ne restituisce la lista
	public List<Record> loadAllRecordsBySessionId(String sessionId, int div)
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.SESSION_ID+"='"+sessionId+"'", null, null, null, null);	

		int counter = 0;
		
		while(c.moveToNext())
		{
			if(counter % div == 0)
			{
				double[] networkLoc = new double[3];		
				networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
				networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
				networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
				long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
				
				Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs, 
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),	
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */					
						);

				record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
				record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
				record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
				record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
				
				record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
				record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
				record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));
				record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
				recordsList.add(record);
			}
			counter++;
		}
		
		return recordsList;
	}	
	
	//carica gli ultimi n records e ne restituisce la lista
	public List<Record> loadLastRecords(int n)
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				null, null, null, null, null);	

		int i = 0;
		
		while((c.moveToNext())&&(i < n))
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs, 
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),	
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */					
					);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));		
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			recordsList.add(record);
			
			i++;
		}
		
		return recordsList;
	}
	
	//carica un record specificato tramite suo id 
	public Record loadRecordById(double id)
	{
		Record record = null;
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.ID+"="+id, null, null, null, null);	
		
		while(c.moveToNext())
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs, 
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),	
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */
					);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
		}
		
		return record;
	}	
	
	public int countNotUploadedRecord()
	{
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.UPLOAD_SYS_TS+"="+0, null, null, null, RecordMetaData.SYS_TS);
		
		return c.getCount();
	}
	
	/*
	public int countAllRecords()
	{
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				null, null, null, null, RecordMetaData.SYS_TS);
		
		return c.getCount();
	}*/
	
	//return total number of records on db and number of uploaded records
	
	public int resetUploadedRecords()
	{
		ContentValues values = new ContentValues();
		values.put(RecordMetaData.UPLOAD_SYS_TS, 0);
		
		int affectedRows = mDb.update(RecordMetaData.TABLE_NAME, values, null, null);
		
		Log.d("DbManager", "resetUploadedRecords()--> affected rows: "+affectedRows);
		return affectedRows;
	}
	
	public int[] countRecords()
	{
		int[] num = {0, 0};
		
		Long uploadSysTs;
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, new String[]{RecordMetaData.ID, RecordMetaData.SYS_TS, RecordMetaData.UPLOAD_SYS_TS}, 
				null, null, null, null, RecordMetaData.SYS_TS);
		
		num[0] = c.getCount();
		
		c.moveToFirst();
		
		while(c.moveToNext())
		{
			uploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			if(uploadSysTs != 0)
				num[1]++;
		}
		
		return num;
	}
	
	//carica al massimo n records più vecchi, ordinati in base al timestamp di sistema, non ancora
	//inviati al server
	public List<Record> loadOlderNotUploadedRecords(int n, boolean onlyActualSid)
	{
		List<Record>recordsList = new ArrayList<Record>();
		String sessionId = Utils.getSessionId(mContext);
		Cursor c = null;
		
		if(onlyActualSid)
		{
			c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
					RecordMetaData.UPLOAD_SYS_TS+"="+0+" AND " +RecordMetaData.SESSION_ID+"='"+sessionId+"'", 
					null, null, null, RecordMetaData.SYS_TS, String.valueOf(n)); //order by SYS_TS (--> history records have this field to 0, so they are the first!)
		}
		else
		{
			c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, RecordMetaData.UPLOAD_SYS_TS+"="+0, null, null, null, RecordMetaData.SYS_TS, String.valueOf(n));
			/*
			c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
					RecordMetaData.UPLOAD_SYS_TS+"="+0, null, null, null, RecordMetaData.SYS_TS); //order by SYS_TS (--> history records have this field to 0, so they are the first!)
			*/
			Log.d("DbManager", "loadOlderNotUploadedRecords()--> get rec count: " +c.getCount());
		}
		
		int i = 0;
		
		while(c.moveToNext() &&(i < n))
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs,
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),	
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */					
					);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			i++;
			
			recordsList.add(record);
		}		
		return recordsList;
	}
	
	//restituisce tutti i record successivi al record di cui viene passato l'id (incluso nella risposta)
	public List<Record>loadRecordsAfterFirst(double firstRecordId)
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.UPLOAD_SYS_TS+"="+0+" AND "+RecordMetaData.ID+">="+firstRecordId, 
				null, null, null, RecordMetaData.SYS_TS);	
		
		while(c.moveToNext())
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs,
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */					
					);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));

			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			recordsList.add(record);
		}
		
		return recordsList;
	}
	
	//load extended latlng starting from specified timestamp (included)
	public List<ExtendedLatLng> loadLatLngPointsFromTimestamp(long timestamp, int div)
	{
		List<ExtendedLatLng>latlngPoints = new ArrayList<ExtendedLatLng>();
		
		double[] values = new double[12];
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME,  RecordMetaData.COLUMNS, 
				RecordMetaData.SYS_TS+">="+timestamp, 
				null, null, null, RecordMetaData.SYS_TS);	
		
		int counter = 0;
		
		while(c.moveToNext())
		{
			if(c.getDouble(c.getColumnIndex(RecordMetaData.LAT)) != 0)
			{
				String annotation = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
				
				if((counter % div == 0)||(!annotation.equals("")))
				{
					values[0] = c.getDouble(c.getColumnIndex(RecordMetaData.LAT));
					values[1] = c.getDouble(c.getColumnIndex(RecordMetaData.LON));
					values[2] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_1));
					values[3] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_2));
					values[4] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_3));
					values[5] = c.getDouble(c.getColumnIndex(RecordMetaData.CO_4));
					values[6] = c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1));
					values[7] = c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2));
					values[8] = c.getDouble(c.getColumnIndex(RecordMetaData.VOC));
					values[9] = c.getDouble(c.getColumnIndex(RecordMetaData.O3));
					values[10] = c.getDouble(c.getColumnIndex(RecordMetaData.TEMP));
					values[11] = c.getDouble(c.getColumnIndex(RecordMetaData.HUM));
										
					ExtendedLatLng geoPoint = new ExtendedLatLng(c.getDouble(c.getColumnIndex(RecordMetaData.ID)),
						c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
						c.getDouble(c.getColumnIndex(RecordMetaData.LON)), 
						c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)), values,
						c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE)),
						c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1)));
	
					//Log.d("DbManager","loadGeoPointsFromTimestamp()--> " +geoPoint.getLatitudeE6()+", "
					//		+geoPoint.getLongitudeE6()+", " +geoPoint.mSysTimestamp);					
					latlngPoints.add(geoPoint);
				}
			}
			counter++;
		}
		Log.d("DbManager", "loadLatLngPointsFromTimestamp()--># records: " +latlngPoints.size());
		return latlngPoints;
	}
	
	//load records starting from specified timestamp (included)
	public List<Record> loadRecordsFromTimestamp(long timestamp, int div)
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS,
				RecordMetaData.SYS_TS+">="+timestamp, 
				null, null, null, RecordMetaData.SYS_TS);	
		
		int counter = 0;
		
		while(c.moveToNext())
		{
			if(counter % div == 0)
			{				
				double[] networkLoc = new double[3];		
				networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
				networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LON));
				networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
				long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
				
				Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					networkLoc, networkTs,
					/* from AP 1.4 */
					c.getString(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SOURCE_POINT_NUMBER)),
					c.getString(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_SEED)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_SESSION_NUMBER)),
					c.getInt(c.getColumnIndex(RecordMetaData.SEMANTIC_POINT_NUMBER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.PHONE_ALTITUDE)),
					c.getLong(c.getColumnIndex(RecordMetaData.PHONE_TS)),					
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ACC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.BOX_ALTITUDE)),
					c.getInt(c.getColumnIndex(RecordMetaData.BOX_NUM_SATS)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_BEAR)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_SPEED)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ALTITUDE)),
					c.getString(c.getColumnIndex(RecordMetaData.BOX_MAC_ADDRESS))
					/* end of from AP 1.4 */					
					);

				record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
				record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
				record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
				record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
				
				record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
				record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
				record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));
		
				record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
				recordsList.add(record);				
			}
			counter++;
		}
		
		return recordsList;
	}
	
	//cancella tutti i records dalla tabella
	/*
	public int deleteAllRecords()
	{
		//il parametro "1" fa in modo che delete restituisca il numero di righe eliminate quando
		//non viene specificata la whereClause
		return mDb.delete(RecordMetaData.TABLE_NAME, "1", null);
	}*/
	
	//cancella tutti i records già inviati al server e più vecchi di millisec rispetto ad ora
	public int deleteUploadedRecords(long millisec)
	{
		Date date = new Date();
		long sysTs = date.getTime(); //data attuale in formato timestamp
		
		long olderThanTs = sysTs - millisec; //soglia temporale prima della quale cancellare
		
		Log.d("DbManager", "sysTs: " +sysTs+ " - olderThanTs: " +olderThanTs+ " - millisec: " +millisec);
		
		//cancello record con timestamp più vecchio della soglia impostata e timestamp di upload presente
		int deletedCount =  mDb.delete(RecordMetaData.TABLE_NAME, RecordMetaData.SYS_TS+"<="+olderThanTs
				+" AND "+RecordMetaData.UPLOAD_SYS_TS+">0", null);	
		
        //update the number of uploaded records stored on DB decreasing it by the number of uploaded records deleted from DB 
        Utils.decrUploadedRecCount(mContext, deletedCount);
		
        //update the number of total records stored on DB decreasing it by the number of uploaded records deleted from DB 
        Utils.decrTotalStoredRecCount(mContext, deletedCount);
        
		return deletedCount;
	}
	
	/********************** HISTORY RECORDS METHODS ****************************************************/
	
	//saves a list of history records in a single statement (faster)
	public boolean saveHistoryRecordsSerie(Record[] serie)
	{	
		boolean done = false;
		int size = serie.length;
		
		String insert = "insert into " +RecordMetaData.TABLE_NAME+ " ("+RecordMetaData.PHONE_ID+", "+RecordMetaData.PHONE_MODEL+", "+RecordMetaData.SYS_TS+", "
	            +RecordMetaData.BOX_TS+", "+RecordMetaData.LAT+", "+RecordMetaData.LON+", "+RecordMetaData.CO_1+", "+RecordMetaData.CO_2+", "
	            +RecordMetaData.CO_3+", "+RecordMetaData.CO_4+", "+RecordMetaData.NO2_1+", "+RecordMetaData.NO2_1+", "+RecordMetaData.VOC+", "
	            +RecordMetaData.O3+", "+RecordMetaData.TEMP+", "+RecordMetaData.HUM+", "+RecordMetaData.LOCALIZATION+", "+RecordMetaData.GPS_PROVIDER+", "
	            +RecordMetaData.ACCURACY+", "+RecordMetaData.NETWORK_LAT+", "+RecordMetaData.NETWORK_LON+", "+RecordMetaData.NETWORK_ACC+", "+RecordMetaData.NETWORK_TS+", "
	            +RecordMetaData.BLACK_CARBON_MOBILE+", "+RecordMetaData.USER_DATA_1+", "+RecordMetaData.USER_DATA_2+", "
	            +RecordMetaData.USER_DATA_3+", "+RecordMetaData.UPLOAD_SYS_TS+", "+RecordMetaData.SESSION_ID+", "+RecordMetaData.SOURCE_SESSION_SEED+", "
	            +RecordMetaData.SOURCE_SESSION_NUMBER+", "+RecordMetaData.SOURCE_POINT_NUMBER+", "+RecordMetaData.SEMANTIC_SESSION_SEED+", "
	            +RecordMetaData.SEMANTIC_SESSION_NUMBER+", "+RecordMetaData.SEMANTIC_POINT_NUMBER+", "+RecordMetaData.PHONE_LAT+", "+RecordMetaData.PHONE_LON+", "
	            +RecordMetaData.PHONE_ACC+", "+RecordMetaData.PHONE_BEAR+", "+RecordMetaData.PHONE_SPEED+", "+RecordMetaData.PHONE_ALTITUDE+", "
	            +RecordMetaData.PHONE_TS+", "+RecordMetaData.BOX_LAT+", "+RecordMetaData.BOX_LON+", "+RecordMetaData.BOX_ACC+", "+RecordMetaData.BOX_BEAR+", "
	            +RecordMetaData.BOX_SPEED+", "+RecordMetaData.BOX_ALTITUDE+", "+RecordMetaData.BOX_NUM_SATS+", "+RecordMetaData.NETWORK_BEAR+", "+RecordMetaData.NETWORK_SPEED+", "
	            +RecordMetaData.NETWORK_ALTITUDE+", "+RecordMetaData.BOX_MAC_ADDRESS
	            + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
	            	" ?, ?, ?, ?, ?)";

		try 
		{
			//PRAGMA synchronous=OFF can speed up the insertion process quite a lot, but it leaves you vulnerable 
			//to database corruption if the application crashes during the bulk insertion. 
			mDb.execSQL("PRAGMA synchronous=OFF");
			
			//mDb.setLockingEnabled(false); //deprecated
            
			mDb.beginTransaction();
			
			SQLiteStatement st = mDb.compileStatement(insert);

			if(size > 0)
			{
				for(int i = 0; i < size; i++)
				{
	            	Record rec = serie[i];
	
	            	st.bindString(1, rec.mUniquePhoneId);
	            	st.bindString(2, rec.mPhoneModel);
	            	st.bindLong(3, rec.mSysTimestamp);
	            	st.bindLong(4, rec.mBoxTimestamp);
	            	st.bindDouble(5, rec.mValues[0]);
	            	st.bindDouble(6, rec.mValues[1]);
	            	st.bindDouble(7, rec.mValues[2]);
	            	st.bindDouble(8, rec.mValues[3]);
	            	st.bindDouble(9, rec.mValues[4]);
	            	st.bindDouble(10, rec.mValues[5]);
	            	st.bindDouble(11, rec.mValues[6]);
	            	st.bindDouble(12, rec.mValues[7]);
	            	st.bindDouble(13, rec.mValues[8]);
	            	st.bindDouble(14, rec.mValues[9]);
	            	st.bindDouble(15, rec.mValues[10]);
	            	st.bindDouble(16, rec.mValues[11]);
	            	st.bindString(17, rec.mLocalization);
	            	st.bindString(18, rec.mGpsProvider);
	            	st.bindDouble(19, rec.mAccuracy);
	            	//st.bindLong(20, rec.mSbPowerOn); //removed from AP 1.4
	            	st.bindDouble(20, rec.mNetworkLat);
	            	st.bindDouble(21, rec.mNetworkLon);
	            	st.bindDouble(22, rec.mNetworkAcc);
	            	st.bindLong(23, rec.mNetworkTimestamp);
	            	st.bindDouble(24, rec.mBcMobile);
	            	st.bindString(25, rec.mUserData1);
	            	st.bindString(26, rec.mUserData2);
	            	st.bindString(27, rec.mUserData3);
	            	st.bindLong(28, rec.mUploadSysTs);
	            	st.bindString(29, rec.mSessionId);
	            	
	            	/* from AP 1.4 */
	            	st.bindString(30, rec.mSourceSessionSeed);
	            	st.bindString(31, String.valueOf(rec.mSourceSessionNumber));
	            	st.bindString(32, String.valueOf(rec.mSourcePointNumber));
	            	st.bindString(33, rec.mSemanticSessionSeed);
	            	st.bindString(34, String.valueOf(rec.mSemanticSessionNumber));
	            	st.bindString(35, String.valueOf(rec.mSemanticPointNumber));
	            	st.bindDouble(36, rec.mPhoneLat);
	            	st.bindDouble(37, rec.mPhoneLon);
	            	st.bindDouble(38, rec.mPhoneAcc);
	            	st.bindDouble(39, rec.mPhoneBear);
	            	st.bindDouble(40, rec.mPhoneSpeed);
	            	st.bindDouble(41, rec.mPhoneAltitude);
	            	st.bindLong(42, rec.mPhoneTimestamp);
	            	st.bindDouble(43, rec.mBoxLat);
	            	st.bindDouble(44, rec.mBoxLon);
	            	st.bindDouble(45, rec.mBoxAcc);
	            	st.bindDouble(46, rec.mBoxBear);
	            	st.bindDouble(47, rec.mBoxSpeed);
	            	st.bindDouble(48, rec.mBoxAltitude);
	            	st.bindDouble(49, rec.mBoxNumSats);
	            	st.bindDouble(50, rec.mNetworkBear);
	            	st.bindDouble(51, rec.mNetworkSpeed);
	            	st.bindDouble(52, rec.mNetworkAltitude);
	            	st.bindString(53, rec.mBoxMac);
	            	/* end of from AP 1.4 */
	            
	            	st.executeInsert();
	            	
	            	/*************** UPDATE SECTION OF 'TRACKS' TABLE ************************/
	        		//2 - update 'tracks' table with new data
	        		ContentValues trackValue = new ContentValues();
	        		trackValue.put(TrackMetaData.LAST_SYS_TS, rec.mSysTimestamp);
	        		trackValue.put(TrackMetaData.LAST_BOX_TS, rec.mBoxTimestamp);
	        		
	        		//try to update entry for actual sessionId on 'tracks' table
	        		if(mDb.update(TrackMetaData.TABLE_NAME, trackValue, TrackMetaData.SESSION_ID+"='"+rec.mSessionId+"'", null) != 1)
	        		{
	        			Log.d("DbManager", "saveHistoryRecordsSerie()--> track entry doesn't exist, creating new...");
	        			
	        			//if update fails, no entry for actual sessionId exists, so create it
	        			//create new Track object
	        			Track track = new Track(rec.mSessionId, rec.mSysTimestamp, rec.mBoxTimestamp, rec.mSysTimestamp, rec.mBoxTimestamp, 1, 0);			
	        			//store it on 'tracks' table
	        			mDb.insert(TrackMetaData.TABLE_NAME, null, track.getTrackValues());
	        		}
	        		//if update works on an existent entry, update the same entry again to increment by one the num of records
	        		else
	        		{
	        			//update num of records count for entry in 'tracks' table with session id
	        			String updateQuery="update "+TrackMetaData.TABLE_NAME+" SET \"" +TrackMetaData.NUM_OF_RECS+"\" = \""+TrackMetaData.NUM_OF_RECS+"\" + 1 "
	        					+" WHERE "+TrackMetaData.SESSION_ID+" = '"+rec.mSessionId+"'";
	        			mDb.execSQL(updateQuery);
	        			//Log.d("DbManager", "saveHistoryRecordsSerie()--> executed query: "+updateQuery);	
	        		}
	            }
			}

            mDb.setTransactionSuccessful();
            done = true;
        } 
		catch(java.util.ConcurrentModificationException e)
		{
			e.printStackTrace();
		}
		catch (SQLException e) 
		{
            e.printStackTrace();
        } 
		finally 
		{
        	mDb.endTransaction();
        	mDb.execSQL("PRAGMA synchronous=NORMAL");            
        }
		
		//if transaction ended successfully, increment the number of records saved on DB by 'size' quantity
		if(done)
			Utils.incrTotalStoredRecCount(mContext, size);
		
		return done;
	}
	
	//loads all history records (history records doesn't have system timestamp but only box timestamp) and returns a list
	/*
	public List<Record> loadAllHistoryRecords()
	{
		List<Record>recordsList = new ArrayList<Record>();
		
		Cursor c = mDb.query(RecordMetaData.TABLE_NAME, RecordMetaData.COLUMNS, 
				RecordMetaData.SYS_TS+"=0", null, null, null, null);	

		while(c.moveToNext())
		{
			double[] networkLoc = new double[3];		
			networkLoc[0] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[1] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_LAT));
			networkLoc[2] = c.getDouble(c.getColumnIndex(RecordMetaData.NETWORK_ACC));			
			long networkTs = c.getLong(c.getColumnIndex(RecordMetaData.NETWORK_TS));
			
			Record record = new Record(c.getLong(c.getColumnIndex(RecordMetaData.SYS_TS)),
					c.getLong(c.getColumnIndex(RecordMetaData.BOX_TS)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.LAT)),
					c.getDouble(c.getColumnIndex(RecordMetaData.LON)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_1)), 
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.CO_4)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_1)),
					c.getDouble(c.getColumnIndex(RecordMetaData.NO2_2)),
					c.getDouble(c.getColumnIndex(RecordMetaData.VOC)),
					c.getDouble(c.getColumnIndex(RecordMetaData.O3)),
					c.getDouble(c.getColumnIndex(RecordMetaData.TEMP)),
					c.getDouble(c.getColumnIndex(RecordMetaData.HUM)),
					c.getString(c.getColumnIndex(RecordMetaData.SESSION_ID)),
					c.getString(c.getColumnIndex(RecordMetaData.LOCALIZATION)),
					c.getString(c.getColumnIndex(RecordMetaData.GPS_PROVIDER)),
					c.getDouble(c.getColumnIndex(RecordMetaData.ACCURACY)),
					c.getLong(c.getColumnIndex(RecordMetaData.SB_TIME_ON)),
					networkLoc, networkTs);

			record.mId = c.getDouble(c.getColumnIndex(RecordMetaData.ID));	
			
			record.mUniquePhoneId = c.getString(c.getColumnIndex(RecordMetaData.PHONE_ID));
			record.mPhoneModel = c.getString(c.getColumnIndex(RecordMetaData.PHONE_MODEL));
			record.mBcMobile = c.getDouble(c.getColumnIndex(RecordMetaData.BLACK_CARBON_MOBILE));
			
			record.mUserData1 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_1));
			record.mUserData2 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_2));
			record.mUserData3 = c.getString(c.getColumnIndex(RecordMetaData.USER_DATA_3));
			
			record.mUploadSysTs = c.getLong(c.getColumnIndex(RecordMetaData.UPLOAD_SYS_TS));
			
			recordsList.add(record);
		}
		
		return recordsList;
	}*/
	/*
	public int deleteAllHistoryRecords()
	{
		return mDb.delete(RecordMetaData.TABLE_NAME, RecordMetaData.SYS_TS+"=0", null);
	}*/
}
