package org.poirsouille;

import java.io.IOException;

import org.poirsouille.TincdService.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TincActivity extends Activity 
{
    TincdService _service;
    boolean _bound = false;
    TextView _txtView;
    ScrollView _scroll;
    TextView _logTextView;
    Button _startStopButton;
    ToggleButton _debugButton;
	boolean _logInitialized = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection _connection = new ServiceConnection() 
    {
        //@Override
        public void onServiceConnected(ComponentName className,
                IBinder service) 
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            _service = binder.getService();
            _bound = true;
            _service._callback = new ICallback() 
            {
            	public void call(final String iData)
            	{
            		_logTextView.post(new Runnable()
            		{
						public void run()
						{	
							updateLog(iData);
							updateStatus();
						}
            		});
            	}
            };
            updateLog(null);
            updateStatus();
        }

        //@Override
        public void onServiceDisconnected(ComponentName arg0) 
        {
        	_bound = false;
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        _txtView = (TextView)findViewById(R.id.textView1);
        _scroll = (ScrollView)findViewById(R.id.scrollView1);
        _logTextView = (TextView)findViewById(R.id.logTextView);
        _startStopButton = (Button)findViewById(R.id.button1);
        _debugButton = (ToggleButton)findViewById(R.id.debugButton);
    }
    
    @Override
    protected void onStart()
    {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, TincdService.class);
        bindService(intent, _connection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onStop() 
    {
        super.onStop();
        // Unbind from the service
        if (_bound) 
        {
            unbindService(_connection);
            _service._callback = null;
            _bound = false;
        }
    }
    
    @Override
    protected void onResume()
    {
    	super.onResume();
    }
    
    public void click(View view) throws IOException 
    {
        if (! _service.isStarted())
        {
        	Intent intent = new Intent(this,TincdService.class); 
        	startService(intent);
        }
        else
        {
        	Log.d(TincdService.TAG, "Requesting stop");
        	_service.stopTincd();
        }
        //updateStatus();

    }
    
    public void debugClick(View view) throws IOException 
    {
    	_service.toggleDebug();
    }
    
    private void updateStatus()
    {
    	//Log.d(TincdService.TAG, "Updating status");
        if (_service.isStarted())
        {
        	_txtView.setText("Started");
        	_startStopButton.setText(getText(R.string.stop));
        }
        else
        {
        	_txtView.setText("Stopped");
        	_startStopButton.setText(getText(R.string.start));
        }
        _debugButton.setChecked(_service._debug);
    }
    
    private void updateLog(String iData)
    {
        // Update log
		if (iData != null && _logInitialized)
			_logTextView.append(iData + "\n");
		else
		{
			for (String aLine : _service._output)
			{
				_logTextView.append(aLine + "\n");
			}
			_logInitialized = true;
		}
		// Scroll down
		_scroll.smoothScrollTo(0, _logTextView.getHeight());

    }
    
    

}

