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
import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FilePreference extends DialogPreference
{
    private static final String DEFAULT_VALUE = "<None>";
    private static final String PARENT_FOLDER = "..";
    private String _filePath;
    ArrayList<String> _fileList = new ArrayList<String>();
    TextView _currPathTxtView = null;
    
    public FilePreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        
        setDialogLayoutResource(R.layout.file_preference);
        
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }
    
    @Override
    public void onBindDialogView(View view)
    {
        ListView aList = (ListView)view.findViewById(R.id.listView1);
        _currPathTxtView = (TextView)view.findViewById(R.id.currPathTextView);

        // Update files list content, and set it as adapter for list view
        updateFilesList("");
        final ArrayAdapter<String> fileList = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, _fileList);
        aList.setAdapter(fileList);
        
        // Setup onclick handler
        aList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String aItem = (String)parent.getItemAtPosition(position);
                updateFilesList(aItem);
                fileList.notifyDataSetChanged();
            }
        });
    }
    
   /**
    * Update current file path by adding iNext to it (might be ".." to go back up one level).
    * @param iNext
    */
    private void updateFilePath(String iNext)
    {
        File aDirFile = null;
        if (iNext.equals(PARENT_FOLDER))
        {
            aDirFile = new File(_filePath);
            aDirFile = aDirFile.getParentFile();
        }
        else
        {
            aDirFile = new File(_filePath + "/" + iNext);
        }
        // Update state if this is a valid directory
        if (aDirFile.exists() && aDirFile.isDirectory())
        {
            _filePath = aDirFile.getAbsolutePath();
            _currPathTxtView.setText(_filePath);
        }
    }
    
   /**
    * Update current file path by adding iNext to it (might be ".." to go back up one level).
    * @param iNext
    */
    private void updateFilesList(String iNext)
    {
        updateFilePath(iNext);
        _fileList.clear();
        File aDirFile = new File(_filePath);

        File[] aChildren = aDirFile.listFiles();
        // Do not propose going past root
        if (! _filePath.equals("/"))
            _fileList.add(PARENT_FOLDER);
        // List child folders or conf file
        if (aChildren != null)
        {
            for (File aChild : aChildren) 
            {
                if (aChild.isDirectory())
                {
                    _fileList.add(aChild.getName() + "/");
                }
                else if (aChild.getName().equals(SettingsTools.CONF_FILE))
                {
                    _fileList.add(aChild.getName());
                }
            }
        }
        
        Collections.sort(_fileList);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) 
    {
        if (positiveResult) 
        {
            // When the user selects "OK", persist the new value
            persistString(_filePath);
        }
        else
        {
            // Restore previous setting on cancel
            _filePath = getPersistedString(DEFAULT_VALUE);
        }
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
    {
        if (restorePersistedValue)
        {
            // Restore existing state
            _filePath = this.getPersistedString(DEFAULT_VALUE);
        } 
        else
        {
            // Set default state from the XML attribute
            _filePath = (String) defaultValue;
            persistString(_filePath);
        }
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return a.getString(index);
    }
    
}
