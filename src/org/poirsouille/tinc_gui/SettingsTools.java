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

import java.io.File;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * Settings tools, common to both SettingsFragment and SettingsActivityOld (used for gingerbread compatibility).
 *
 */
public class SettingsTools implements OnSharedPreferenceChangeListener
{
    SharedPreferences _sharedPreferences;
    PreferenceScreen _preferenceScreen;
    Context _context;
    static final String CONF_FILE = "tinc.conf";
    
    public SettingsTools(Context iContext, PreferenceScreen iPreferenceScreen)
    {
        _context = iContext;
        _preferenceScreen = iPreferenceScreen;
        _sharedPreferences = _preferenceScreen.getSharedPreferences();
    }
    
   /**
    * Create/replace dynamic menu items for tinc configuration. 
    */
    private void updateConfig()
    {
        Log.d(Tools.TAG, "Updating configuration");
        
        // Parent group
        PreferenceGroup aGroup = (PreferenceGroup) _preferenceScreen.findPreference("pref_key_config");
        PreferenceGroup aHostsGroup = (PreferenceGroup) _preferenceScreen.findPreference("pref_key_hosts");
        // Clean existing ones
        aGroup.removeAll();
        aHostsGroup.removeAll();
        
        // Look for existing configuration files
        String aConfigDir = _sharedPreferences.getString("pref_key_config_path", "<None>");
        File aConfFile = new File(aConfigDir + "/" + CONF_FILE);
        if (aConfFile.exists())
        {
            // Found main configuration file
            addFilePreference(aConfFile, aGroup, R.drawable.ic_menu_manage);
            addStandardConfig(new File(aConfigDir), aGroup);
            
            // Get hosts details from subfolder
            File aHostDir = new File(aConfigDir + "/hosts");
            if (aHostDir.exists() && aHostDir.isDirectory())
            {
                addHostsToGroup(aHostDir, aHostsGroup);
            }
        }
        else
        {
            Preference aPref = new Preference(_context);
            aPref.setPersistent(false);
            aPref.setTitle("No valid configuration found");
            aPref.setSummary("Can't find " + CONF_FILE + " in " + aConfigDir);
            aGroup.addPreference(aPref);
        }
    }
    
   /**
    * Add hosts configurations, with up/down scripts as well. 
    * @param iHostsDir
    * @param oGroup
    */
    private void addHostsToGroup(File iHostsDir, PreferenceGroup oGroup)
    {
        File[] aChildren = iHostsDir.listFiles();
        // listFiles might return null even if it's a proper directory but we can't access it
        if (aChildren != null)
        {
            for (File aChild : aChildren) 
            {
                // Apply similar check to tinc's check_id to filter only valid hosts configuration files
                if (aChild.getName().matches("[a-zA-Z0-9_]+"))
                {
                    addFilePreference(aChild, oGroup, R.drawable.ic_menu_manage);
                    // Look for -up/-down scripts for this host
                    addUpDown(aChild, oGroup);
                }
            }
        }
    }
    
   /**
    * Build up/down menu items for tinc standard config files.  
    * @param iRootDir
    * @param oGroup
    */
    private void addStandardConfig(File iRootDir, PreferenceGroup oGroup)
    {
        final String[] kFilesPfx = {"tinc", "subnet", "host"};
        for (String aPfx : kFilesPfx)
        {
            addUpDown(new File(iRootDir.getPath() + "/" + aPfx), oGroup);
        }
    }
    
   /**
    * For a given host file, look for xxx-up/xxx-down script and add them to oGroup if they exist 
    * @param iHostFile
    * @param oGroup
    */
    private void addUpDown(File iHostFile, PreferenceGroup oGroup)
    {
        File aUpFile = new File (iHostFile.getPath() + "-up");
        if (aUpFile.exists()) 
            addFilePreference(aUpFile, oGroup, R.drawable.ic_menu_play_clip);
        File aDownFile = new File (iHostFile.getPath() + "-down");
        if (aDownFile.exists()) 
            addFilePreference(aDownFile, oGroup, R.drawable.ic_menu_stop);
    }
    
   /**
    * Low-level addition of a item to the menu. 
    * @param iFile
    * @param oGroup
    * @param iIconResId
    */
    @SuppressLint("NewApi")
    private void addFilePreference(File iFile, PreferenceGroup oGroup, int iIconResId)
    {
        Preference aPref = new Preference(_context);
        aPref.setPersistent(false);
        aPref.setTitle(iFile.getName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            aPref.setIcon(iIconResId);
        // Use an intent to open with external text editor
        Intent aIntent = new Intent();
        aIntent.setAction(android.content.Intent.ACTION_VIEW);
        aIntent.setDataAndType(Uri.fromFile(iFile), "text/plain");
        aPref.setIntent(aIntent);
        
        oGroup.addPreference(aPref);
    }
    
   /**
    * Update all summaries with values content. 
    * @param iPrefGrp
    */
    private void updateSummaries(PreferenceGroup iPrefGrp)
    {
        for (int i = 0; i < iPrefGrp.getPreferenceCount(); ++ i)
        {
            Preference aPref = iPrefGrp.getPreference(i);
            if (aPref instanceof PreferenceGroup)
            {
                // Recursive call
                updateSummaries((PreferenceGroup) aPref);
            }
            else if (aPref instanceof DialogPreference)
            {
                aPref.setSummary(_sharedPreferences.getString(aPref.getKey(), "<None>"));
            }
        }
    }
    
    public void onResume() 
    {
        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummaries(_preferenceScreen);
        updateConfig();
    }

    public void onPause() 
    {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {
        Log.d(Tools.TAG,"Pref changed: " + key);
        Preference pref = _preferenceScreen.findPreference(key);
        if (!(pref instanceof CheckBoxPreference))
        {
            // Update summary with current value
            pref.setSummary(sharedPreferences.getString(key, "<None>"));
        }
        if (key.equals("pref_key_config_path"))
            updateConfig();
    }
}
