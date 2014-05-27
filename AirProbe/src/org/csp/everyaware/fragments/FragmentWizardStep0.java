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

import org.csp.everyaware.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragmentWizardStep0  extends Fragment 
{
	//metodo statico di tipo factory per istanziare il fragment e passare i parametri  come bundle argument
    public static FragmentWizardStep0 newInstance()
    {
    	Log.d("FragmentWizardStep0", "newInstance()");
    	
    	FragmentWizardStep0 newFragment = new FragmentWizardStep0();
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
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_wizard_step0, container, false);
        
        Log.d("FragmentWizardStep0", "onCreateView()");
        
        return rootView; 
    }
    
    @Override
    public void onDestroyView()
    {
    	super.onDestroyView();
    	Log.d("FragmentWizardStep0", "onDestroyView()");
    }
}
