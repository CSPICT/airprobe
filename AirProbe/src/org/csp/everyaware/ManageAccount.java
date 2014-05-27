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

import org.csp.everyaware.fragments.FragmentWizardStep0;
import org.csp.everyaware.fragments.FragmentWizardStep1;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class ManageAccount extends FragmentActivity 
{
	private Button mStartProc;
	private TextView mExplTv;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        Log.d("ManageAccount", "onCreate()");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.manage_account);
        
        mExplTv = (TextView)findViewById(R.id.explanationTv);
        mStartProc = (Button)findViewById(R.id.startProcBtn);
        mStartProc.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
		        FragmentWizardStep1 fragment1 = FragmentWizardStep1.newInstance();
		        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.anchor_layout, fragment1);
		        transaction.commit();
		        
		        mStartProc.setVisibility(View.GONE);
		        mExplTv.setText("");
			}        	
        });
        
        getSupportFragmentManager().executePendingTransactions(); //try to avoid fragment overlapping
        
        FragmentWizardStep0 fragment0 = FragmentWizardStep0.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.anchor_layout, fragment0).commit();
    }
    
	@Override
	protected void onStop() 
	{
		super.onStop();	
		Log.d("ManageAccount", "onStop()");
	}	    
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy(); 	
    	Log.d("ManageAccount", "onDestroy()");
    	
    }      
    
    @Override
    public void onPause()
    {
    	super.onPause();  
    	Log.d("ManageAccount", "onPause()");
    }
    
	@Override
	public void onResume()
	{
		super.onResume();
		Log.d("ManageAccount", "onResume()");
	}
	
	public void updateExpl(String text)
	{
		mExplTv.setText(text);
	}
}
