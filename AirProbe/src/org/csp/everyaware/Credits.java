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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public class Credits extends Activity
{
	private TextView mAppVerTv;
	
	private View mIsi, mSapienza, mCsp, mL3s, mVito, mUcl, mCspLogo, mEaLogo, mAdditionalInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.credits);
        
        mAppVerTv = (TextView)findViewById(R.id.appVersionTv);
		mAppVerTv.setText("v"+Utils.appVer);
		
		mIsi = findViewById(R.id.isiLayout);
		mSapienza = findViewById(R.id.sapienzaLayout);
		mCsp = findViewById(R.id.cspLayout);
		mL3s = findViewById(R.id.l3sLayout);
		mVito = findViewById(R.id.vitoLayout);
		mUcl = findViewById(R.id.uclLayout);
		
		mIsi.setOnClickListener(new MyOnClickListener());
		mSapienza.setOnClickListener(new MyOnClickListener());
		mCsp.setOnClickListener(new MyOnClickListener());
		mL3s.setOnClickListener(new MyOnClickListener());
		mVito.setOnClickListener(new MyOnClickListener());
		mUcl.setOnClickListener(new MyOnClickListener());
		
		mCspLogo = findViewById(R.id.cspLogoIv);
		mCspLogo.setOnClickListener(new MyOnClickListener());
		
		mAdditionalInfo = findViewById(R.id.additionalInfoTv);
		mAdditionalInfo.setOnClickListener(new MyOnClickListener());
		
		mEaLogo = findViewById(R.id.eaLogoIv);
		mEaLogo.setOnClickListener(new MyOnClickListener());
    }
    
    private class MyOnClickListener implements OnClickListener
    {
		@Override
		public void onClick(View v) 
		{
			int id = v.getId();
			Intent i = null;
			
			switch(id)
			{
				case R.id.isiLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_ISI));
					startActivity(i);
				break;
				case R.id.sapienzaLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_SAPIENZA));
					startActivity(i);
				break;
				case R.id.cspLogoIv:
				case R.id.cspLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_CSP));
					startActivity(i);
				break;				
				case R.id.l3sLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_L3S));
					startActivity(i);
				break;
				case R.id.vitoLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_VITO));
					startActivity(i);
				break;
				case R.id.uclLayout:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_UCL));
					startActivity(i);
				break;	
				case R.id.eaLogoIv:
				case R.id.additionalInfoTv:
					i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse(Constants.URL_EA));
					startActivity(i);
				break;	
			}			
		}
    	
    }
}
