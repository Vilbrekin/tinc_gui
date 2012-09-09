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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class TincdService extends Service implements ICallback
{
	static final String TAG = "tincd";
	static final String TINCBIN = "tincd";
	private static final String PIDFILE = "tinc.pid";
	String _configPath;
	// Unique Identification Number for the Notification.
	private int NOTIFICATION = R.string.local_service_started;
	private boolean _started = false;
	public boolean _debug = false;
	private int _debugLvl = 2;
	private boolean _useSU = true;
	// Temporary tincd output buffer, used when activity is not in foreground
	private List<String> _tempOutput = Collections.synchronizedList(new LinkedList<String>());
	SharedPreferences _sharedPref;
	public int _maxLogSize = 1000;
	private OnSharedPreferenceChangeListener _prefChangeListener;
	
	public ICallback _callback = null;

	
	// Binder given to clients
    private final IBinder _binder = new LocalBinder();
    
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder 
    {
    	TincdService getService() 
    	{
            // Return this instance of LocalService so clients can call public methods
            return TincdService.this;
        }
    }
    
   /**
    * Execute given command, with either su or sh as shell. 
    * @param command
    * @param ioCallBack
    * @return
    */
    public ArrayList<String> run(String command, ICallback ioCallBack) 
    {
        String aShell = "su";
        if (! _useSU)
            aShell = "sh";
        return Run(aShell, new String[] {command}, ioCallBack);
    }
	
    public static ArrayList<String> Run(String shell, String command) 
    {
        return Run(shell, new String[] {command}, null);
    }
    
    public static ArrayList<String> Run(String shell, String command, ICallback ioCallBack) 
    {
        return Run(shell, new String[] {command}, ioCallBack);
    }
    
    public static ArrayList<String> Run(String shell, String[] commands, ICallback ioCallBack) 
    {
        ArrayList<String> output = new ArrayList<String>();

        try
        {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput = new BufferedOutputStream(process.getOutputStream());
            BufferedReader shellOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            for (String command : commands) 
            {
                Log.i(TAG, "Shell: " + shell + "; command: " + command);
                shellInput.write((command + " 2>&1\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) 
            {
                // Send output either to callback or to output list
                if (ioCallBack != null)
                {
                	ioCallBack.call(line);
                }
                else
                	output.add(line);
            }
        } 
        catch (IOException e)
		{
			// TODO Auto-generated catch block
            Log.e(TAG, "Can't execute shell: " + shell);
			e.printStackTrace();
		}

        return output;
    }
    
    public static String ToString(ArrayList<String> iList)
    {
    	String aRes = "";
    	synchronized(iList)
    	{
			for (String aLine : iList)
			{
				aRes += aLine + "\n";
			}
    	}
		return aRes;
    }
    
    public static String ToString(List<String> iList)
    {
    	String aRes = "";
    	synchronized(iList)
    	{
			for (String aLine : iList)
			{
				aRes += aLine + "\n";
			}
    	}
		return aRes;
    }
    
    public void startTinc() 
    {
    	if (! _started)
    	{
			// Start tincd in a dedicated thread
	        new Thread(new Runnable() 
	        {
	            public void run()
	            {
	            	installTincd();
	            	
	    	    	int aPid;
	    	    	if ((aPid = getPid()) != 0)
	    	    	{
	    	    		// Kill old running daemon if any
	    	    		TincdService.this.run("kill " + aPid + " || rm " + getFileStreamPath(PIDFILE), null);
	    	    		try
						{
							Thread.sleep(500);
						} 
	    	    		catch (InterruptedException e)
						{
						}
	    	    	}

	    	    	_started = true;
	    	    	_debug = false;
	            	// Use exec to replace shell with executable
	            	//TincdService.Run("su", "exec " + getFileStreamPath(TINCBIN) + " -D -d" + _debugLvl + " -c " + _configPath + " --pidfile=" + getFileStreamPath(PIDFILE), TincdService.this);
                    TincdService.this.run("exec " + getFileStreamPath(TINCBIN) + " -D -d" + _debugLvl + " -c " + _configPath + " --pidfile=" + getFileStreamPath(PIDFILE), TincdService.this);
	            	// Process returns only when ended
	            	_started = false;
	                Log.d(TAG, "End of tincd thread");
	                TincdService.this.stopTincd();
	            }
	        }).start();
    	}
	    
    }
    
    public void stopTincd()
    {
    	if (_started)
		{
    		int aPid = getPid();
        	if (aPid != 0)
        		run("kill " + aPid + " || rm " + getFileStreamPath(PIDFILE), null);
		}
        _debug = false;
    	stopForeground(true);
        stopSelf();
        Log.d(TAG, "killed");
    }
 
   /**
    * Install tincd binary on file system if needed (either it does not exist yet, or it's different from the bindled one). 
    */
    void installTincd()
    {
        try
		{
        	boolean aInstallNeeded = true;
	    	InputStream aIS = getResources().openRawResource(R.raw.tincd);  
	    	int aInLen = aIS.available();
	    	byte[] buffer = new byte[aInLen];  
	        //read the text file as a stream, into the buffer  
			aIS.read(buffer);
	        aIS.close();
	        File aTincBinFile = getFileStreamPath(TINCBIN);
	        if(aTincBinFile.exists())
        	{
        		InputStream aIS2 = openFileInput(TINCBIN);
        		// Compare files sizes first
		    	if (aInLen == aIS2.available())
		    	{
		    		byte[] aBuffer2 = new byte[aInLen];
					aIS2.read(aBuffer2);
			        aIS2.close();
			        CRC32 aCkSum1 = new CRC32(), aCkSum2 = new CRC32();
			        aCkSum1.update(buffer);
			        aCkSum2.update(aBuffer2);
		    		if (aCkSum1.getValue() == aCkSum2.getValue())
		    		{
		    			// Same file, skip install
		    			aInstallNeeded = false;
		    		}
		    	}
        		
        	}
        	if (aInstallNeeded)
        	{
        		Log.i(TAG, "Installing tincd binary");
		    	FileOutputStream aOS = openFileOutput(TINCBIN, MODE_PRIVATE);
        		// Copy file from raw resources
		    	aOS.write(buffer);
		        //Close the Input and Output streams  
		    	aOS.close();  
		        
        	}
        	
        	// Ensure binary is executable
        	if (! aTincBinFile.canExecute())
        	{
                // Set it as executable
        	    aTincBinFile.setExecutable(true, false);
        	}
		} 
        catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    }
    
   /**
    * Get PID from PIDFILE, if exists 
    * @return
    */
    public int getPid()
    {
    	int aPid = 0;
    	try 
    	{
    		String aStr;
			InputStream aInstream = openFileInput(PIDFILE);
			BufferedReader aReader = new BufferedReader(new InputStreamReader(aInstream));
			if ((aStr = aReader.readLine()) != null)
			{
				aPid = Integer.parseInt(aStr);
			}
		} 
    	catch (FileNotFoundException e) 
    	{
    		// Not found is expected, do nothing
		} 
    	catch (IOException e) 
		{
			e.printStackTrace();
		}
    	Log.d(TAG, "Returning PID " + aPid);
    	return aPid;
    }
    
    public boolean isStarted()
    {
    	return _started;
    }
    
   /**
    * Send given signal to the daemon. 
    * @param iSigType
    * @return
    */
    public String signal(String iSigType)
    {
    	String aRes ="";
    	int aPid;
    	if (_started && (aPid = getPid()) != 0)
    	{
    		aRes = ToString(run("kill -" + iSigType +" " + aPid, null));
    	}
    	return aRes;
    }
    
   /**
    * Toggle debug level 5 on/off by sending SIGINT to the daemon. 
    */
    public void toggleDebug()
    {
    	signal("SIGINT");
    	_debug = !_debug;
    }
    
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) 
    {
	    startTinc();
	    Log.d(TAG, "Service started");
	    showNotification();
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}

    public void onCreate()
    {
    	_sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	// Refresh local preferences when there are some updates - keep listener in a class member to avoid GC 
    	// (see http://stackoverflow.com/questions/2542938/sharedpreferences-onsharedpreferencechangelistener-not-being-called-consistently)
    	_prefChangeListener = new OnSharedPreferenceChangeListener()
    	{
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
			{
				refreshPrefs();
			}
    	};
    	_sharedPref.registerOnSharedPreferenceChangeListener(_prefChangeListener);
    	// Refresh at startup as well
    	refreshPrefs();
    }
    
   /**
    * Refresh member variables from preferences screen. 
    */
    private void refreshPrefs()
    {
    	Log.d(TAG, "Refreshing preferences");
    	_configPath = _sharedPref.getString("pref_key_config_path", _configPath);
    	_maxLogSize = Integer.parseInt(_sharedPref.getString("pref_key_max_log_size", "" + _maxLogSize));
    	_debugLvl = Integer.parseInt(_sharedPref.getString("pref_key_debug_level", "" + _debugLvl));
    	_useSU = _sharedPref.getBoolean("pref_key_super_user", _useSU);
    }
    
    public void onDestroy ()
    {
    	stopTincd();
    	Log.d(TAG, "Service destroyed");
    }
    
	@Override
	public IBinder onBind(Intent intent) 
	{
		return _binder;
	}
	
	
    /**
     * Show a notification while this service is running.
     */
    private void showNotification() 
    {
    	
    	Notification notification = new Notification(R.drawable.favicon, getText(R.string.local_service_started),
    	        System.currentTimeMillis());
    	Intent notificationIntent = new Intent(this, TincActivity.class);
    	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    	notification.setLatestEventInfo(this, getText(R.string.local_service_label),
    	        getText(R.string.local_service_started), pendingIntent);
    	startForeground(NOTIFICATION, notification);
    }

   /**
    * Pop temporary output: fetch it and clear internal list. 
    * @return
    */
    public List<String> popOutput()
    {
        List<String> aRes = null;
        if (! _tempOutput.isEmpty())
        {
            aRes = new LinkedList<String>(_tempOutput);
            _tempOutput.clear();
        }
        return aRes;
    }

   /**
    * Callback implementation. Used when new line is printed by the service.
    * Either forwards it to activity's callback (if available, on the foreground), or stores the result in temporary output. 
    */
    public void call(String iData)
	{
		Date aDate = new Date();
		SimpleDateFormat aFormat = new SimpleDateFormat("HH:mm:ss");
		String aTxt = aFormat.format(aDate) + " " + iData;
		// Notify activity callback if any
		if (_callback != null)
		{
			_callback.call(aTxt);
		}
		else
		{
		    // Activity is not available. Store temporary output.
	        _tempOutput.add(aTxt);
	        // Limit log size
	        while (_maxLogSize > 0 && _tempOutput.size() >= _maxLogSize)
	        {
	            _tempOutput.remove(0);
	        }
		}
	}
    
   /**
    * Get tincd status and routing table. 
    * @return
    */
    public String getStatus()
    {
    	String aStatus = signal("SIGUSR1");
    	aStatus += signal("SIGUSR2");
    	aStatus += ToString(run("ip route", null));
		return aStatus;
    }
}
