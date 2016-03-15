package com.jungkai.chessboardlayout;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

/**
 * Created by jungkai on 2014. 6. 27..
 */
public class LayoutUtils {

    //To support RTL layout (upper than sdk version 17)
    public static boolean isLayoutRTL(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Configuration config = context.getResources().getConfiguration();
            return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        } else {
            return false;
        }
    }
}
