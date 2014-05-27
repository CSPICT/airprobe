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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.util.Log;

/**
* Color gradient map from blue to red in 1200 steps.<br>
* Returns a Color for a double value.
* <ul>
* http://stackoverflow.com/questions/2245842/sorting-colors-in-matlab</url>
*
* @author timaschew
*
*/
public class ColorHelper 
{
	private final static int LOW = 0;
	private final static int HIGH = 255;
	private final static int HALF = (HIGH + 1) / 2;
	
	private final static Map<Integer, Integer> map = initNumberToColorMap();
	private static int factor;
	
	/**
	*
	* @param value
	* should be from 0 unti 100
	*/
	public static int numberToColor(final double value) 
	{
		if (value < 0 || value > 100) 
		{
			return -1;
		}
		return numberToColorPercentage(value / 100);
	}
	
	/**
	* @param value
	* should be from 0 unti 1
	* @return
	*/
	public static int numberToColorPercentage(double value) 
	{
		if (value < 0) 
		{
			return -1;
		}
		else if(value > 1)
			value = 1;
		
		//Log.d("ColorHelper", "numberToColorPercentage()--> value: " +value);
		
		Double d = value * factor;
		int index = d.intValue();
		if (index == factor) 
		{
			index--;
		}
		//Log.d("ColorHelper", "numberToColorPercentage()--> index: "+index);
		
		return map.get(index);
	}
	
	/**
	* @return
	*/
	private static Map<Integer, Integer> initNumberToColorMap() 
	{
		HashMap<Integer, Integer> localMap = new HashMap<Integer, Integer>();
		int r = LOW;
		int g = LOW;
		int b = HALF;
		
		// factor (increment or decrement)
		int rF = 0;
		int gF = 0;
		int bF = 1;
		
		int count = 0;
		// 1276 steps
		while (true) 
		{			
			localMap.put(count++, Color.rgb(r, g, b));
		
			if (b == HIGH)
			{
				gF = 1; // increment green
			}
			if (g == HIGH) 
			{
				bF = -1; // decrement blue
				// rF = +1; // increment red
			}
			if (b == LOW) 
			{
				rF = +1; // increment red
			}
			if (r == HIGH) 
			{
				gF = -1; // decrement green
			}
			if (g == LOW && b == LOW) 
			{
				rF = -1; // decrement red
			}
			if (r < HALF && g == LOW && b == LOW) 
			{
				break; // finish
			}
			r += rF;
			g += gF;
			b += bF;
			r = rangeCheck(r);
			g = rangeCheck(g);
			b = rangeCheck(b);
		}
		initList(localMap);
		return localMap;
	}
		
	/**
	* @param localMap
	*/
	private static void initList(final HashMap<Integer, Integer> localMap) 
	{
		List<Integer> list = new ArrayList<Integer>(localMap.keySet());
		Collections.sort(list);
		Integer min = list.get(0);
		Integer max = list.get(list.size() - 1);
		factor = max + 1;
		System.out.println(factor);
	}
		
	/**
	* @param value
	* @return
	*/
	private static int rangeCheck(final int value) 
	{
		if (value > HIGH) 
		{
			return HIGH;
		} 
		else if (value < LOW) 
		{
			return LOW;
		}
		return value;
		}
	}
