package uk.co.whiteoctober.cordova;

import android.content.Context;

import com.google.android.gcm.GCMBroadcastReceiver;

class CordovaGCMBroadcastReceiver extends GCMBroadcastReceiver
{
    /**
     * Gets the class name of the intent service that will handle GCM messages.
     */
    protected String getGCMIntentServiceClassName(Context context) {
        return "uk.co.whiteoctober.cordova.GCMIntentService";
    }
}