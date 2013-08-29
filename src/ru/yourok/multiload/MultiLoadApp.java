package ru.yourok.multiload;

import android.app.Application;

/**
 * Created with IntelliJ IDEA.
 * User: yourok
 * Date: 23.08.12
 * Time: 16:07
 */
public class MultiLoadApp extends Application
{
    private static boolean activityVisible;
    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }
}
