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

package org.csp.everyaware.fragments;

import org.csp.everyaware.Constants;
import org.csp.everyaware.ManageAccount;
import org.csp.everyaware.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class FragmentWizardStep1  extends Fragment 
{
	private Button mRegisterBtn, mIhaveAccountBtn;
	
	//metodo statico di tipo factory per istanziare il fragment e passare i parametri  come bundle argument
    public static FragmentWizardStep1 newInstance()
    {
    	Log.d("FragmentWizardStep1", "newInstance()");
    	
    	FragmentWizardStep1 newFragment = new FragmentWizardStep1();
    	/*
    	Bundle argsBundle = new Bundle();
    	argsBundle.putSerializable(Constants.PAGE_INSTANCE, pagina);
    	argsBundle.putInt(Constants.PAGE_INDEX, pageIndex);
    	newFragment.setArguments(argsBundle);
    	*/
    	return newFragment;
    }	
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
    {   	
    	//ottengo il bundle associato al fragment e leggo i parametri passati
    	//Bundle bundle = getArguments(); 	
    	
    	//inflating del layout del fragment
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_wizard_step1, container, false);
        
        Log.d("FragmentWizardStep1", "onCreateView()");
        
        mRegisterBtn = (Button)rootView.findViewById(R.id.registerBtn);
        mRegisterBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
				//launch external browser to view official site registration form page
				Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.CREATE_LOGIN_URL));
				startActivity(newIntent);
			}       	
        });
        
        mIhaveAccountBtn = (Button)rootView.findViewById(R.id.i_have_accountBtn);
        mIhaveAccountBtn.setOnClickListener(new OnClickListener()
        {
			@Override
			public void onClick(View v) 
			{
		        FragmentWizardStep2 fragment2 = FragmentWizardStep2.newInstance();
		        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.anchor_layout, fragment2);
		        transaction.commit();
		        
		        ManageAccount activity = (ManageAccount) getActivity();
		        activity.updateExpl(getResources().getString(R.string.username_psw_exp));
			}        	
        });
        
        return rootView; 
    }
    
    @Override
    public void onDestroyView()
    {
    	super.onDestroyView();
    	Log.d("FragmentWizardStep1", "onDestroyView()");
    }
}
