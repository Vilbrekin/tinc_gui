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

import java.io.IOException;
import java.util.List;

import org.poirsouille.tinc_gui.TincdService.LocalBinder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TincActivity extends Activity implements ICallback
{
    TincdService _service;
    TextView _txtView;
    ScrollView _scroll;
    TextView _logTextView;
    Button _startStopButton;
    ToggleButton _debugButton;
    
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
            Log.d(Tools.TAG, "Service connected");
            _service._callback = TincActivity.this;
            updateLog(null);
            updateStatus();
        }

        //@Override
        public void onServiceDisconnected(ComponentName arg0) 
        {
            Log.d(Tools.TAG, "Service disconnected");
            _service = null;
        }
    };
    
    /** Called when the activity is first created. */
    @TargetApi(11)
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            // Ensure to scroll down on each text change
            _logTextView.addOnLayoutChangeListener(new OnLayoutChangeListener()
            {
                public void onLayoutChange(View v, int left, int top, int right,
                        int bottom, int oldLeft, int oldTop, int oldRight,
                        int oldBottom)
                {
                    _scroll.smoothScrollTo(0, _logTextView.getHeight());
                }
            });
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater aInflater = getMenuInflater();
        aInflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        // Handle item selection
        switch (item.getItemId()) 
        {
            case R.id.settings:
            {
                Intent aIntent = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                {
                    aIntent = new Intent(this, SettingsActivity.class);
                }
                else
                {
                    aIntent = new Intent(this, SettingsActivityOld.class);
                }
                startActivity(aIntent);
                return true;
            }
            case R.id.about:
            {
                Intent aIntent = new Intent(this, AboutActivity.class);
                startActivity(aIntent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
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
        if (_service != null) 
        {
            unbindService(_connection);
            _service._callback = null;
        }
    }
    
    @Override
    protected void onPause()
    {
        super.onPause();
        if (_service != null) 
        {
            // Unset service callback
            _service._callback = null;
        }
    }
    
    @Override
    protected void onResume() 
    {
        super.onResume();
        if (_service != null) 
        {
            // Set activity as service callback
            _service._callback = this;
        }
    }
    
    private boolean checkRoot()
    {
        if (_service != null && _service._useSU)
        {
            // Ensure su is working correctly, as some implementations do simply nothing, without error...
            String aOut = Tools.ToString(Tools.Run("su", "id"));
            return (aOut.length() >= 5 && aOut.substring(0, 5).equals("uid=0"));
        }
        return true;
    }
    
    public void click(View view) throws IOException 
    {
        if (_service == null || ! _service.isStarted())
        {
            // Ensure device is rooted if SU is activated
            if (! checkRoot())
            {
                // Display error dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.no_su_error)
                       .setCancelable(false)
                       .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() 
                       {
                           public void onClick(DialogInterface dialog, int id) 
                           {
                                dialog.dismiss();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else
            {
                // Start tincd service
                Intent intent = new Intent(this,TincdService.class).setAction("org.poirsouille.tinc_gui.TincdService.START"); 
                startService(intent);
            }
        }
        else
        {
            Log.d(Tools.TAG, "Requesting stop");
            _service.stopTincd();
        }
    }
    
    public void debugClick(View view) throws IOException 
    {
        if (_service != null) 
        {
            _service.toggleDebug();
        }
    }
    
    private void updateStatus()
    {
        if (_service != null && _service.isStarted())
        {
            _txtView.setText("Started");
            _startStopButton.setText(getText(R.string.stop));
            _debugButton.setEnabled(true);
        }
        else
        {
            _txtView.setText("Stopped");
            _startStopButton.setText(getText(R.string.start));
            _debugButton.setEnabled(false);
        }
        if (_service != null) 
            _debugButton.setChecked(_service._debug);
        else
            _debugButton.setChecked(false);
    }
    
    private void updateLog(String iData)
    {
    	_logTextView.setTextSize(getLogFontSize());

        if (_service != null)
        {
            List<String> aTempOut = _service.popOutput();
            if (aTempOut != null)
            {
                Log.d(Tools.TAG, "Popping temporary logs (" + aTempOut.size() + " lines)");
                _logTextView.append(Tools.ToString(aTempOut));
            }
        }
        if (iData != null)
            _logTextView.append(iData + "\n");
        
        // Limit log size (allow 10% to avoid doing it on each iteration)
        if (_service != null && _logTextView.getLineCount() > 1.1 * _service._maxLogSize) 
        {
            int excessLineNumber = _logTextView.getLineCount() - _service._maxLogSize;
            Log.d(Tools.TAG, "Truncating logs (deleting " + excessLineNumber + " lines)");
            int eolIndex = -1;
            CharSequence charSequence = _logTextView.getText();
            for(int i = 0; i < excessLineNumber; i++) 
            {
                do 
                {
                    eolIndex++;
                } while(eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');             
            }
            if (eolIndex < charSequence.length()) 
            {
                _logTextView.getEditableText().delete(0, eolIndex+1);
            }
            else 
            {
                _logTextView.setText("");
            }
        }
            

    }
    
    public void clickClear(View iView)
    {
        _logTextView.setText("");
    }
    
    public void clickStatus(View iView)
    {
        if (_service != null) 
            _logTextView.append(_service.getStatus() + "\n");
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //Log.d(Tools.TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putCharSequence("_logTextView", _logTextView.getText());
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        //Log.d(Tools.TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        _logTextView.setText(savedInstanceState.getCharSequence("_logTextView"));
        // Append any text saved in service's internal buffer
        updateLog(null);
    }

    /// ICallback interface implementation
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
    
   /**
    * Get font size for log window
    * clamp it to a suitable range (8 -100)
    * @return
    */
    public Integer getLogFontSize()
    {
        int aLogFontSize = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_key_font_size_log", "" + 8));

        return Math.max(8, Math.min(100, aLogFontSize));
    }


}

