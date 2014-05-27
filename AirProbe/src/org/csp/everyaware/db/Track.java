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

import java.text.SimpleDateFormat;

import org.csp.everyaware.Utils;
import org.csp.everyaware.db.Record.RecordMetaData;

import com.google.android.gms.maps.model.LatLng;

import android.content.ContentValues;
import android.util.Log;

//a track is a sequence of records of the same session id
public class Track 
{
	public String mSessionId;
	public long mFirstSysTimestamp;
	public long mFirstBoxTimestamp;
	public long mLastSysTimestamp;
	public long mLastBoxTimestamp;
	
	public int mNumOfRecords;
	public int mUploadedRecs;
		
	public Track(String sessionId, long firstSysTs, long firstBoxTs, long lastSysTs, long lastBoxTs, int numOfRecs, int uploadedRecs)
	{
		mSessionId = sessionId;
		mFirstSysTimestamp = firstSysTs;
		mFirstBoxTimestamp = firstBoxTs;
		mLastSysTimestamp = lastSysTs;
		mLastBoxTimestamp = lastBoxTs;
		mNumOfRecords = numOfRecs;
		mUploadedRecs = uploadedRecs;
	}
	
	public String getFormattedSessionId()
	{
		String formattedDate = "";
		
		try
		{
			if(mSessionId.contains("offline_"))
			{
				String sid = mSessionId.replace("offline_", "");			
				sid = "offline_" + new SimpleDateFormat("HH:mm:ss MM/dd/yyyy").format(Long.valueOf(sid));			
				return sid;
			}
			formattedDate = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy").format(Long.valueOf(mSessionId));
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			Log.d("Track", "getFormattedSessionId()--> NumberFormatException on sid: " +mSessionId);
		}
		return formattedDate;
	}
	
	public String getTrackLength()
	{
		String hourStr, minStr, secStr;
		long lengthTs = 0;
		
		if((mLastSysTimestamp != 0)&&(mFirstSysTimestamp != 0))
			lengthTs = mLastSysTimestamp - mFirstSysTimestamp;
		else if((mLastBoxTimestamp != 0)&&(mFirstBoxTimestamp != 0))
			lengthTs = mLastBoxTimestamp - mFirstBoxTimestamp;
		
		if(lengthTs > 0)
		{
			int secDiff = (int)lengthTs/1000; //get track length in seconds
			int minDiff = secDiff / 60; //get track length in minutes
			
			int sec = secDiff % 60;
			int min = minDiff % 60;			
			int hour = minDiff / 60;
			
			hourStr = String.valueOf(hour);
			minStr = String.valueOf(min);
			secStr = String.valueOf(sec);
			
			if(hour < 10)
				hourStr = "0"+hourStr;
			if(min < 10)
				minStr = "0"+minStr;
			if(sec < 10)
				secStr = "0"+secStr;
			
			return hourStr+":"+minStr+":"+secStr;
		}
		
		return "";
	}
	
	@Override
	public String toString()
	{
		return mSessionId+" first sys ts: " +mFirstSysTimestamp+" first box ts: " +mFirstBoxTimestamp+ " last sys ts: " +mLastSysTimestamp+" last box ts: " +mLastBoxTimestamp+ " # of records: " +mNumOfRecords+" # of uploaded records: "+mUploadedRecs;
	}
	
	public static class TrackMetaData 
	{		
		public static final String TABLE_NAME = "tracks";
		
		public static final String ID = "_id"; 
		public static final String SESSION_ID = "session_id";
		
		public static final String FIRST_SYS_TS = "first_sys_ts";
		public static final String FIRST_BOX_TS = "first_box_ts";
		public static final String LAST_SYS_TS = "last_sys_ts";
		public static final String LAST_BOX_TS = "last_box_ts";
		
		public static final String NUM_OF_RECS = "num_of_recs";
		public static final String NUM_OF_UPL_RECS = "num_of_upl_recs";
		
		public static final String[] COLUMNS = new String[] { ID,SESSION_ID, FIRST_SYS_TS, FIRST_BOX_TS, LAST_SYS_TS, LAST_BOX_TS, NUM_OF_RECS, NUM_OF_UPL_RECS};
	}
	
	public ContentValues getTrackValues()
	{
		ContentValues trackValues = new ContentValues();
		
		trackValues.put(TrackMetaData.SESSION_ID, mSessionId);
		
		trackValues.put(TrackMetaData.FIRST_SYS_TS, mFirstSysTimestamp);
		trackValues.put(TrackMetaData.FIRST_BOX_TS, mFirstBoxTimestamp);
		trackValues.put(TrackMetaData.LAST_SYS_TS, mLastSysTimestamp);
		trackValues.put(TrackMetaData.LAST_BOX_TS, mLastBoxTimestamp);

		trackValues.put(TrackMetaData.NUM_OF_RECS, mNumOfRecords);
		trackValues.put(TrackMetaData.NUM_OF_UPL_RECS, mUploadedRecs);
		
		return trackValues;
	}
	
	public static StringBuilder createTracksTableSQL()
	{
		StringBuilder createTracksTable = new StringBuilder();
		
		createTracksTable.append("CREATE TABLE \"" +TrackMetaData.TABLE_NAME+ "\" (");
		createTracksTable.append("	    \"" +TrackMetaData.ID+ "\" INTEGER PRIMARY KEY AUTOINCREMENT,");

		createTracksTable.append("	    \"" +TrackMetaData.SESSION_ID+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.FIRST_SYS_TS+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.FIRST_BOX_TS+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.LAST_SYS_TS+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.LAST_BOX_TS+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.NUM_OF_RECS+ "\" TEXT,");
		createTracksTable.append("	    \"" +TrackMetaData.NUM_OF_UPL_RECS+ "\" TEXT");
		createTracksTable.append(")");
		
		return createTracksTable;
	}
}
