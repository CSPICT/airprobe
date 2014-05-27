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

import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

public class ExtendedLatLng
{
	public double mRecordId;
	public long mSysTimestamp;
	public double[] mValues = new double[12];
	public double mAvgValue = 0;
	public LatLng mLatLng;
	public double mBc;
	public String mUserAnn;
	
	public ExtendedLatLng(double recordId, double lat, double lon, long sysTimestamp, double[] values, double bc, String userAnn) 
	{
		mRecordId = recordId;
		mSysTimestamp = sysTimestamp;			
		mValues = values;
		mAvgValue = calcAvgPoll();
		mLatLng = new LatLng(lat, lon);
		mBc = bc;
		mUserAnn = userAnn;
		
		if(!userAnn.equals(""))
			Log.d("ExtendedLatLng", "annotation: "+userAnn);
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
		
		//Log.d("EGP", "avg: " +avg);
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
}