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

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";

    private static CordovaWebView webView = null;
    private Context context = null;

    private static String gECB;
    private static String gSenderID;

    private GoogleCloudMessaging gcm;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        PushNotificationPlugin.webView = webView;
        context = this.cordova.getActivity();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.v(ME + ":execute", "action=" + action);

        if (REGISTER.equals(action)) {

            try {
                JSONObject jo = new JSONObject(args.toString().substring(1, args.toString().length() - 1));

                Log.v(ME + ":execute", jo.toString());
                gSenderID = jo.getString("sender_id");
                if (gSenderID.isEmpty()) {
                    return false;
                }

                gcm = GoogleCloudMessaging.getInstance(context);
                String regid = getRegistrationId(context);
                if (regid.isEmpty()) {
                    registerInBackground(gSenderID);
                } else {
                    Log.v(ME + ":execute", "success, registration ID is " + regid);
                    JSONObject json = new JSONObject(), params = new JSONObject();
                    if (jo.getString("on_success") != null) {
                        json.put("callback", jo.getString("on_success"));
                        params.put("registration_id", regid);
                        sendJavascript(json, params);
                    }
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
        sendJavascript(json, new JSONObject());
    }

    public static void sendJavascript(JSONObject json, JSONObject params) throws JSONException {

        String js = "setTimeout(function() { " + json.getString("callback") + "(";
        if (params.length() > 0) {
            js += "\"" + params.toString() + "\"";
        }
        js += "); },0)";
        Log.v(ME + ":sendJavascript", js);

        if (webView != null) { // FIXME
            webView.sendJavascript(js);
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
        String registrationId = prefs.getString("sender_id", "");
        if (registrationId.isEmpty()) {
            Log.i(ME + ":getRegistrationId", "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt("app_version", Integer.MIN_VALUE);
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

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
//                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    SharedPreferences settings = context.getSharedPreferences(PREFERENCES_KEY, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("sender_id", regid);
                    editor.commit();

                } catch (IOException ex) {
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
}
