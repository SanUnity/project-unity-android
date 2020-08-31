package com.main.covid.utils;


import android.content.Context;
import android.content.res.Resources;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


/**
 * Created by Carlos Olmedo on 6/4/16.
 */
public class UtilsHelper {

    public static ActionBar setupToolbar(AppCompatActivity activity, Toolbar toolbar, String title) {

        activity.setSupportActionBar(toolbar);

        ActionBar actionBar = activity.getSupportActionBar();

        if (actionBar!=null) {
            if (title != null) {
                actionBar.setTitle(title);
            }

        }

        return actionBar;
    }

    public static boolean gotDrawableRessource(Context context, String ressourceName)
    {
        Integer identifier = context.getResources().getIdentifier(ressourceName,"drawable",context.getPackageName());

        return identifier != 0;
    }

    public static boolean isLargeScreen(Context context) throws Resources.NotFoundException
    {
        try
        {
            Integer largeScreen = context.getResources().getIdentifier("isLargeScreen","bool", context.getPackageName());

            return context.getResources().getBoolean(largeScreen);

        }catch (Resources.NotFoundException e){

            throw new Resources.NotFoundException();

        }
    }






}
