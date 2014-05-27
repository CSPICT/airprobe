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

public class MapCluster 
{
	public double mBcLevel;
	public double mMinLat;
	public double mMinLon;
	public double mMaxLat;
	public double mMaxLon;
	
	public MapCluster()
	{
		mBcLevel = 0;
		mMinLat = 0;
		mMinLon = 0;
		mMaxLat = 0;
		mMaxLon = 0;
	}
	
	public MapCluster(double bcLevel, double minLat, double minLon, double maxLat, double maxLon)
	{
		mBcLevel = bcLevel;
		mMinLat = minLat;
		mMinLon = minLon;
		mMaxLat = maxLat;
		mMaxLon = maxLon;
	}
}
