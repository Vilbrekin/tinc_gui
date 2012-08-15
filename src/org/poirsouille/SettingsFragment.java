package org.poirsouille;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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
    	// Parent group
    	PreferenceGroup aGroup = (PreferenceGroup) getPreferenceScreen().findPreference(getResources().getString(R.string.pref_key_config, ""));
    	// TODO: parse config files
    	Preference aPref = new Preference(getActivity());
    	aPref.setPersistent(false);
    	aPref.setTitle("No valid configuration found");
    	aPref.setSummary("Check folder " + _sharedPreferences.getString("pref_key_config_path", "<None>"));
    	aGroup.addPreference(aPref);
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
        if (key == "pref_key_config_path")
        	updateConfig();
    }
}
