package uk.co.whiteoctober.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import java.io.IOException;
import java.lang.Void;
import java.lang.String;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class PushNotificationPlugin extends CordovaPlugin {

    public static final String ME = "PushNotificationPlugin";
    public static final String PREFERENCES_KEY = "CORDOVA_" + ME;

    public static final String SETUP = "setup";
    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";

    public static final String PROPERTY_REGISTRATION_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "app_version";

    private static CordovaWebView webView = null;
    private Context context = null;

    private static String eventCallback;
    private static String senderID;

    private GoogleCloudMessaging gcm;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        PushNotificationPlugin.webView = webView;
        context = this.cordova.getActivity();
        eventCallback = new String();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.v(ME + ":execute", "action=" + action);

        if (SETUP.equals(action)) {
            JSONObject jo = new JSONObject(args.toString().substring(1, args.toString().length() - 1));
            if (!jo.has("event_callback")) {
                Log.e(ME + ":execute", "No event_callback specified");

                return false;
            }

            try {
                eventCallback = jo.getString("event_callback");
                callbackContext.success();

                return true;
            } catch (JSONException e) {
                return false;
            }
        }

        if (REGISTER.equals(action)) {

            try {
                JSONObject jo = new JSONObject(args.toString().substring(1, args.toString().length() - 1));

                Log.v(ME + ":execute", jo.toString());
                senderID = jo.getString("sender_id");
                if (senderID.isEmpty()) {
                    return false;
                }

                gcm = GoogleCloudMessaging.getInstance(context);
                String regid = getRegistrationId(context);
                if (regid.isEmpty()) {
                    registerInBackground(senderID);
                } else {
                    Log.v(ME + ":execute", "success, registration ID is " + regid);
                    JSONObject json = new JSONObject().put("event", "registered");
                    json.put(PROPERTY_REGISTRATION_ID, regid);
                    sendJavascript(json);
                }

                callbackContext.success();
                return true;

            } catch (JSONException e) {
                Log.e(ME, "Got JSON Exception " + e.getMessage());
            }
        }

        return false;
    }

    public static void sendJavascript(JSONObject json) throws JSONException {

        if (eventCallback.length() == 0) {
            return;
        }
        String js = "setTimeout(function() { " + eventCallback + "(";
        if (json.length() > 0) {
            js += json.toString();
        }
        js += "); },0)";
        Log.v(ME + ":sendJavascript", js);

        if (webView != null) {
            webView.sendJavascript(js);
        } else {
            Log.v(ME + ":sendJavascript", "webView is null, we should try again later..."); // FIXME
        }
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        SharedPreferences prefs = this.cordova.getActivity().getSharedPreferences(PREFERENCES_KEY, 0);
        String registrationId = prefs.getString(PROPERTY_REGISTRATION_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(ME + ":getRegistrationId", "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(ME + ":getRegistrationId", "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground(final String gSenderID) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    String regid = gcm.register(gSenderID);
                    msg = "Device registered, registration ID=" + regid;
                    Log.v(ME + ":registerInBackground", msg);

                    // Persist the regID - no need to register again.
                    storeRegistrationID(context, regid);

                    JSONObject json = new JSONObject().put("event", "registered");
                    json.put(PROPERTY_REGISTRATION_ID, regid);

                    Log.v(ME + ":registerInBackground", json.toString());
                    try {
                        PushNotificationPlugin.sendJavascript(json);
                    } catch (NullPointerException e) {
                        Log.e(ME + ":onRegistered", "NullPointerException, maybe viewport is not active?");
                    }

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    Log.e(ME + ":registerInBackground", msg);
                } catch (JSONException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    Log.e(ME + ":registerInBackground", msg);
                }

                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute();
    }

    /**
     * Store the registration ID against our application version
     * in shared preferences
     *
     * @param context
     * @param registrationID
     */
    private void storeRegistrationID(Context context, String registrationID) {
        SharedPreferences settings = context.getSharedPreferences(PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PROPERTY_REGISTRATION_ID, registrationID);
        editor.putInt(PROPERTY_APP_VERSION, getAppVersion(context));
        editor.commit();
    }
}
