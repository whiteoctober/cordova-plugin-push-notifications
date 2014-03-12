package uk.co.whiteoctober.cordova;

import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

    public static final String ME = "GCMReceiver";

    @Override
    public void onRegistered(Context context, String registrationId) {

        Log.v(ME + ":onRegistered", "Registration ID arrived!");
        Log.v(ME + ":onRegistered", registrationId);

        JSONObject json;

        try {
            json = new JSONObject().put("event", "registered");
            json.put("regid", registrationId);

            Log.v(ME + ":onRegistered", json.toString());
            try {
                PushNotificationPlugin.sendJavascript(json);
            } catch (NullPointerException e) {
                Log.e(ME + ":onRegistered", "NullPointerException, maybe viewport is not active?");
            }

        } catch (JSONException e) {
            // No message to the user is sent, JSON failed
            Log.e(ME + ":onRegistered", "JSON exception");
        }
    }

    @Override
    public void onUnregistered(Context context, String registrationId) {
        Log.v(ME + ":onUnregistered: ", registrationId);
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Log.v(ME + ":onMessage", "Received GCM message");

        // Extract the payload from the message
        Bundle extras = intent.getExtras();
        Log.v(ME + ":onMessage", extras.toString());
        if (extras != null) {
            try {
                JSONObject json = new JSONObject();
                json.putString("event", "message");

                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    Object o = extras.get(key);
                    json.put(key, o);
                }

                Log.v(ME + ":onMessage ", json.toString());

                try {
                    PushNotificationPlugin.sendJavascript(json);
                } catch (NullPointerException e) {
                    Log.v(ME + ":onMessage", "Null exception, never mind.");
                }
            } catch (JSONException e) {
                Log.e(ME + ":onMessage", "JSON exception");
            }
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        try {
            JSONObject json;
            json = new JSONObject().put("event", "error");
            json.put("msg", errorId);

            Log.e(ME + ":onError ", json.toString());

            PushNotificationPlugin.sendJavascript(json);
        } catch (JSONException e) {
            Log.e(ME + ":onMessage", "JSON exception");
        }
    }

    @Override
    protected boolean onRecoverableError(Context ctxt, String errorMsg) {
        Log.d(getClass().getSimpleName(), "onRecoverableError: " + errorMsg);

        return (true);
    }

    @Override
    protected String[] getSenderIds(Context context) {
        String[] x = new String[1];
        SharedPreferences settings = context.getSharedPreferences(PushNotificationPlugin.PREFERENCES_KEY, 0);
        x[0] = settings.getString("sender_id", "");

        return x;
    }
}
