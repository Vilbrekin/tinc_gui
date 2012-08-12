package org.poirsouille;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.CRC32;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;

public class TincdService extends Service implements ICallback
{
	static final String TAG = "tincd";
	static final String TINCBIN = "tincd";
	private static final String PIDFILE = "tinc.pid";
	// Unique Identification Number for the Notification.
	private int NOTIFICATION = R.string.local_service_started;
	private boolean _started = false;
	public boolean _debug = false;
	private int _debugLvl = 2;
	public ArrayList<String> _output = new ArrayList<String>();
	
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
    
	
    public static ArrayList<String> run(String shell, String command) 
    {
        return run(shell, new String[] {command}, null);
    }
    
    public static ArrayList<String> run(String shell, String command, ICallback ioCallBack) 
    {
        return run(shell, new String[] {command}, ioCallBack);
    }
    
    public static ArrayList<String> run(String shell, String[] commands, ICallback ioCallBack) 
    {
        ArrayList<String> output = new ArrayList<String>();

        try {
            Process process = Runtime.getRuntime().exec(shell);

            BufferedOutputStream shellInput = new BufferedOutputStream(process.getOutputStream());
            BufferedReader shellOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            for (String command : commands) 
            {
                Log.i(TAG, "command: " + command);
                shellInput.write((command + " 2>&1\n").getBytes());
            }

            shellInput.write("exit\n".getBytes());
            shellInput.flush();

            String line;
            while ((line = shellOutput.readLine()) != null) 
            {
                Log.d(TAG, "command output: " + line);
                
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
			e.printStackTrace();
		}

        return output;
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
	    	    		TincdService.run("su", "kill " + aPid + " || rm " + getFileStreamPath(PIDFILE));
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
	            	TincdService.run("su", "exec " + getFileStreamPath(TINCBIN) + " -D -d" + _debugLvl + " -c /data/tinc/vpn --pidfile=" + getFileStreamPath(PIDFILE), TincdService.this);
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
        		run("su", "kill " + aPid + " || rm " + getFileStreamPath(PIDFILE));
		}
    	stopForeground(true);
        stopSelf();
        Log.d(TAG, "killed");
    }
    
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
	        if(getFileStreamPath(TINCBIN).exists())
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
		        
		        // Set it as executable
		        TincdService.run("su", "chmod 770 " + getFileStreamPath(TINCBIN));
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
    
    public void signal(String iSigType)
    {
    	int aPid;
    	if (_started && (aPid = getPid()) != 0)
    	{
    		run("su", "kill -" + iSigType +" " + aPid);
    	}
    }
    
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

	public void call(String iData)
	{
		_output.add(iData);
		// Notify activity callback if any
		if (_callback != null)
		{
			_callback.call(iData);
		}
	}
}
