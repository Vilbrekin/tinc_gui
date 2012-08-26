package org.poirsouille;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener
{
	SharedPreferences _sharedPreferences;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        
        _sharedPreferences = getPreferenceScreen().getSharedPreferences();
    }
    
    private void updateConfig()
    {
    	Log.d(TincdService.TAG, "Updating configuration");
    	
    	// Parent group
    	PreferenceGroup aGroup = (PreferenceGroup) getPreferenceScreen().findPreference("pref_key_config");
		PreferenceGroup aHostsGroup = (PreferenceGroup) getPreferenceScreen().findPreference("pref_key_hosts");
    	// Clean existing ones
    	aGroup.removeAll();
		aHostsGroup.removeAll();
		
    	// Look for existing configuration files
    	String aConfigDir = _sharedPreferences.getString("pref_key_config_path", "<None>");
    	File aConfFile = new File(aConfigDir + "/tinc.conf");
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
        	Preference aPref = new Preference(getActivity());
        	aPref.setPersistent(false);
        	aPref.setTitle("No valid configuration found");
        	aPref.setSummary("Can't find tinc.conf in " + aConfigDir);
        	aGroup.addPreference(aPref);
    	}
    }
    
    private void addHostsToGroup(File iHostsDir, PreferenceGroup oGroup)
    {
        File[] aChildren = iHostsDir.listFiles();
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
    
    private void addFilePreference(File iFile, PreferenceGroup oGroup, int iIconResId)
    {
    	Preference aPref = new Preference(getActivity());
    	aPref.setPersistent(false);
    	aPref.setTitle(iFile.getName());
    	//aPref.setSummary("Can't find tinc.conf in " + aConfigDir);
    	aPref.setIcon(iIconResId);//numberpicker_up_disabled_holo_dark
    	// Use an intent to open with external text editor
    	Intent aIntent = new Intent();
    	aIntent.setAction(android.content.Intent.ACTION_VIEW);
    	aIntent.setDataAndType(Uri.fromFile(iFile), "text/plain");
    	aPref.setIntent(aIntent);
    	
    	oGroup.addPreference(aPref);
    }
    
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
        super.onResume();
        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        updateSummaries(getPreferenceScreen());
        updateConfig();
    }

    public void onPause() 
    {
        super.onPause();
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {
    	Log.d(TincdService.TAG,"Pref changed: " + key);
        Preference pref = findPreference(key);
        pref.setSummary(sharedPreferences.getString(key, "<None>"));
        if (key.equals("pref_key_config_path"))
        	updateConfig();
    }
}
