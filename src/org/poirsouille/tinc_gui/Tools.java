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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Tools
{
    // Log's tag
    static final String TAG = "tinc_gui";

    public static List<String> Run(String shell, String command) 
    {
        return Tools.Run(shell, new String[] {command}, null);
    }

    public static List<String> Run(String shell, String command, ICallback ioCallBack) 
    {
        return Tools.Run(shell, new String[] {command}, ioCallBack);
    }

   /**
    * Execute the gien list of commands using the given shell. 
    * If ioCallBack is null, output will be returned. Otherwise callback is used for each new output line.
    * @param shell
    * @param commands
    * @param ioCallBack
    * @return
    */
    public static List<String> Run(String shell, String[] commands, ICallback ioCallBack) 
    {
        List<String> output = new ArrayList<String>();
    
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
                {
                    output.add(line);
                }
            }
        } 
        catch (IOException e)
        {
            Log.e(TAG, "Can't execute shell: " + shell);
            e.printStackTrace();
        }
    
        return output;
    }

   /**
    * Convert an List<String> into a string with new lines.
    * @param iList
    * @return
    */
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

}
