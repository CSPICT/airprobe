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

package org.csp.everyaware.tabactivities;


import org.csp.everyaware.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TableLayout.LayoutParams;

public class Tabs extends TabActivity 
{
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.tabs);
        
        TabHost tabHost = getTabHost();
        
        // Tab for Map
        TabSpec mapspec = tabHost.newTabSpec("Map");
        //mapspec.setIndicator("Map", getResources().getDrawable(R.drawable.world_icon));
        mapspec.setIndicator("Map", getResources().getDrawable(android.R.drawable.ic_menu_myplaces));
        Intent mapIntent = new Intent(this, Map.class);     
        mapspec.setContent(mapIntent);
        
        // Tab for Graph
        TabSpec graphspec = tabHost.newTabSpec("Graph");
        //graphspec.setIndicator("Graph", getResources().getDrawable(R.drawable.graph_icon));
        graphspec.setIndicator("Graph", getResources().getDrawable(android.R.drawable.ic_menu_view));
        Intent graphIntent = new Intent(this, Graph.class);
        graphspec.setContent(graphIntent);
        
        // Tab for SensorBox
        TabSpec monitorspec = tabHost.newTabSpec("Monitor");
        //sboxspec.setIndicator("SensorBox", getResources().getDrawable(R.drawable.box_icon));     
        monitorspec.setIndicator("Monitor", getResources().getDrawable(android.R.drawable.ic_menu_manage));     
        Intent sboxIntent = new Intent(this, Monitor.class);
        monitorspec.setContent(sboxIntent);     
        
        // Adding all TabSpec to TabHost      
        tabHost.addTab(mapspec); // Adding map tab
        tabHost.addTab(graphspec); // Adding feel tab
        tabHost.addTab(monitorspec); // Adding monitor tab
        
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        
        //for large screen, higher tab icons
    	if((screenWidth > 540)&&(screenHeight > 960))
    	{
            double height = tabHost.getTabWidget().getChildAt(0).getLayoutParams().height;
            for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) 
            {
                tabHost.getTabWidget().getChildAt(i).getLayoutParams().height =  (int) (1.4 * height);
            }
    	}
    }
}