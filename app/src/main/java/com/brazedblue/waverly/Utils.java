package com.brazedblue.waverly;

import android.content.Context;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by stevemeyer on 12/26/15.
 */
class Utils
{
    static DisplayMetrics getDisplayMetrics(Context context)
    {
        DisplayMetrics result = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(result);
        return result;
    }

    static float scaleFactorToFit(int bmapH, int bmapV, int displayH, int displayV, boolean max)
    {
        float result = 1;

        if (bmapH == 0 || bmapV == 0)
        {
            return result;
        }

        float hScale = (float)displayH / (float)bmapH;
        float vScale = (float)displayV / (float)bmapV;

        if (max)
        {
            result = hScale > vScale ? hScale : vScale;
        }
        else
        {
            result = hScale < vScale ? hScale : vScale;
        }

        return result;
    }

    static String getStringSuffix(String string) {
        String result = null;
        if (null != string ) {
            int suffixStart = string.lastIndexOf('.');
            if (suffixStart >= 0) {
                result = string.substring(suffixStart + 1);
            }
        }

        return result;
    }

    static float getTextLineHeight(TextView view)
    {
        float result = 0;
        Paint paint = view.getPaint();
        if (paint != null) {
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            if (fontMetrics != null) {
                result =  fontMetrics.bottom - fontMetrics.top;
            }
        }
        if (result <= 0.0)
        {
            result = view.getContext().getResources().getDimension(R.dimen.defaultLineHt);
        }

        return result;
    }
}
