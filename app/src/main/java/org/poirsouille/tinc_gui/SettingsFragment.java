/*
 *    Copyright (C) 2012 Vilbrekin <vilbrekin@gmail.com>
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.poirsouille.tinc_gui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

@SuppressLint("NewApi")
public class SettingsFragment extends PreferenceFragment
{
    SettingsTools _settingsTools;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
    }
    
    public void onResume() 
    {
        super.onResume();
        _settingsTools = new SettingsTools(getActivity(), getPreferenceScreen());
        _settingsTools.onResume();
    }

    public void onPause() 
    {
        super.onPause();
        _settingsTools.onPause();
    }
}
