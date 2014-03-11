package uk.co.whiteoctober.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.content.SharedPreferences;

import com.google.android.gcm.GCMRegistrar;

public class GCMPlugin extends CordovaPlugin {

    public static final String ME = "GCMPlugin";
    public static final String PREFERENCES_KEY = "CORDOVA_" + ME;

    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";

    private static String gECB;
    private static String gSenderID;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.v(ME + ":execute", "action=" + action);

        if (REGISTER.equals(action)) {

            try {
                JSONObject jo = new JSONObject(args.toString().substring(1, args.toString().length() - 1));

                Log.v(ME + ":execute", jo.toString());
                gECB = (String) jo.get("ecb");
                gSenderID = (String) jo.get("senderID");

                // Store the sender ID in shared preferences, so we can retrieve it later
                SharedPreferences settings = this.ctx.getSharedPreferences(PREFERENCES_KEY, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("senderID", gSenderID);
                editor.commit();

                final String regId = GCMRegistrar.getRegistrationId(this.cordova.getActivity());
                if (regId.equals("")) {
                    GCMRegistrar.register(this.cordova.getActivity(), gSenderID);
                } else {
                    Log.v(ME + ":execute", "GCM - already registered");
                    JSONObject json = new JSONObject().put("event", "registered");
                    json.put("regid", regId);
                    this.sendJavascript(json);
                }

                Log.v(ME + ":execute", "GCM register called");

                callbackContext.success();
                return true;

            } catch (JSONException e) {
                Log.e(ME, "Got JSON Exception " + e.getMessage());
                return false;
            }
        }

        if (UNREGISTER.equals(action)) {
            GCMRegistrar.unregister(this.cordova.getActivity());
            Log.v(ME + ":" + UNREGISTER, "GCM unregister called");

            return true;
        }

        // No idea what to do here
        Log.e(ME, "Invalid action : " + action);

        return false;
    }


    public static void sendJavascript(JSONObject _json) {
        String _d = "javascript:" + gECB + "(" + _json.toString() + ")";
        Log.v(ME + ":sendJavascript", _d);

        super.webView.sendJavascript(_d);
    }
}
