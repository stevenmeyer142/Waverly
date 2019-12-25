package com.brazedblue.waverly;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by stevemeyer on 1/21/16.
 */
public class CustomLog {
    private PrintWriter m_Writer = null;
    private File    m_File;


    static private CustomLog gLogger = null;
    static private final String LOG_FILE_NAME = "mylog.txt";
    static private final String TAG_GAP = "                              -- ";
    static private final String TYPE_ERR =      "ERROR   -- ";
    static private final String TYPE_DEBUG =    "DEBUG   -- ";
    static private final String TYPE_VERBOSE =  "VERBOSE -- ";

    static void initializeLog(Context context)
    {
        if (gLogger == null)
        {
            gLogger = new CustomLog(context);
        }
    }
    CustomLog(Context context)
    {
        m_File = new File(context.getCacheDir(), LOG_FILE_NAME);
        try {
            if (!m_File.exists()) {
                if (!m_File.createNewFile())
                {
                    android.util.Log.e("myLog", "could not create file " + m_File);
                }
            }

            m_Writer = new PrintWriter(m_File);
            m_Writer.println(DateFormat.getDateTimeInstance().format(new Date()));
            m_Writer.println(Build.DEVICE);
            m_Writer.println(Build.DISPLAY);
            m_Writer.println(Build.HOST);
            m_Writer.println();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void writeText(String type, String tag, String text)
    {
        if (gLogger != null)
        {
            gLogger.write(type, tag, text);
        }

    }

    private void write(String type, String tag, String text)
    {
        if (m_Writer != null) {
            m_Writer.write(type);
            m_Writer.write(tag);

            int gapLength = Math.max(3, TAG_GAP.length() - tag.length());
            m_Writer.write(TAG_GAP, TAG_GAP.length() - gapLength, gapLength);
            m_Writer.println(text);
        }
     }

    static void e(String tag, String text)
    {

        if (gLogger != null) {
            writeText(TYPE_ERR, tag, text);
        }
        else
        {
            Log.e(tag, text);
        }
    }

    static void e(String tag, String text, Throwable t) {
        if (gLogger != null) {
            writeText(TYPE_ERR, tag, text + "\n" + Log.getStackTraceString(t));
        }
        else
        {
            Log.e(tag, text + "\n" + Log.getStackTraceString(t));
        }
    }

    static void d(String tag, String text)
    {

        if (gLogger != null) {
            writeText(TYPE_DEBUG, tag, text);
        }
        else
        {
            Log.d(tag, text);
        }
    }
    static void v(String tag, String text)
    {

        if (gLogger != null) {
            writeText(TYPE_VERBOSE, tag, text);
        }
        else
        {
            Log.v(tag, text);
        }
    }

    static String getLogText()
    {
        String result = "";
        if (gLogger != null)
        {
            result = gLogger.logText();
        }

        return result;
    }

    private String logText()
    {
        String result = "";
        m_Writer.flush();
        try {
            StringBuilder outBuffer = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(m_File));
            String text;
            while ((text = reader.readLine()) != null)
            {
                outBuffer.append(text);
                outBuffer.append('\n');
            }
            result = outBuffer.toString();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return result;
    }

}

