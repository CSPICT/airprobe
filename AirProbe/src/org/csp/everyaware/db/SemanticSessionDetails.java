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

public class SemanticSessionDetails 
{
	public double mId;
	public String mSourceSessionSeed;
	public int mSourceSessionNumber;
	public String mSemanticSessionSeed;
	public int mSemanticSessionNumber;
	public int mPointNumberStart, mPointNumberEnd;

	public SemanticSessionDetails(String sourceSessionSeed, int sourceSessionNumber, String semanticSessionSeed, int semanticSessionNumber, int pointNumberStart, int pointNumberEnd)
	{
		mSourceSessionSeed = sourceSessionSeed;
		mSourceSessionNumber = sourceSessionNumber;		
		mSemanticSessionSeed = semanticSessionSeed;
		mSemanticSessionNumber = semanticSessionNumber;
		mPointNumberStart = pointNumberStart;
		mPointNumberEnd = pointNumberEnd;
	}
	
	public static class SemanticMetaData 
	{
		public static final String TABLE_NAME = "semantics_table";
		public static final String ID = "_id";
		public static final String SOURCE_SESSION_SEED = "source_session_seed";
		public static final String SOURCE_SESSION_NUMBER = "source_session_number";
		public static final String SEMANTIC_SESSION_SEED = "semantic_session_seed";
		public static final String SEMANTIC_SESSION_NUMBER = "semantic_session_number";
		public static final String POINT_NUMBER_START = "point_number_start";
		public static final String POINT_NUMBER_END = "point_number_stop";
		
		public static final String[] COLUMNS = new String[] { ID, SOURCE_SESSION_SEED, SOURCE_SESSION_NUMBER, SEMANTIC_SESSION_SEED, SEMANTIC_SESSION_NUMBER, POINT_NUMBER_START, POINT_NUMBER_END};
	}
	
	public static StringBuilder createSemanticTableSQL()
	{
		StringBuilder createSemanticTable = new StringBuilder();
		
		createSemanticTable.append("CREATE TABLE \"" +SemanticMetaData.TABLE_NAME+ "\" (");
		createSemanticTable.append("	    \"" +SemanticMetaData.ID+ "\" INTEGER PRIMARY KEY AUTOINCREMENT,");
		createSemanticTable.append("	    \"" +SemanticMetaData.SOURCE_SESSION_SEED+ "\" TEXT,");
		createSemanticTable.append("	    \"" +SemanticMetaData.SOURCE_SESSION_NUMBER+ "\" INTEGER,");
		createSemanticTable.append("	    \"" +SemanticMetaData.SEMANTIC_SESSION_SEED+ "\" TEXT,");
		createSemanticTable.append("	    \"" +SemanticMetaData.SEMANTIC_SESSION_NUMBER+ "\" INTEGER,");
		createSemanticTable.append("	    \"" +SemanticMetaData.POINT_NUMBER_START+ "\" INTEGER,");
		createSemanticTable.append("	    \"" +SemanticMetaData.POINT_NUMBER_END+ "\" INTEGER DEFAULT -1");		
		createSemanticTable.append(")");
		
		return createSemanticTable;
	}
}
