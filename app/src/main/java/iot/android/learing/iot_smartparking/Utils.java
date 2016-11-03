package iot.android.learing.iot_smartparking;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import iot.android.learing.iot_smartparking.app.AppConfig;
import iot.android.learing.iot_smartparking.app.AppController;

/**
 * Created by Ahmed on 4/29/2016.
 */
public class Utils {

    /**
     * There's one example of moving marker in google map v2 demo app ..
     * In file adt-bundle-linux/sdk/extras/google/google_play_services/samples/maps/src/com/exa‌​mple/mapdemo/MarkerDemoActivity.java
     *
     * this code will animate the marker from one geopoint to another.
     * @param marker
     * @param toPosition
     * @param hideMarker
     */
    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker, final GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    public static void fetchCarEntries(final Context context, final TextView textView, final TextView timeStampTV) {
        // Tag used to cancel the request
        String tag_string_req = "req_login";

//        pDialog.setMessage("Getting Car Entries  ...");
//        showDialog(pDialog);

        StringRequest strReq = new StringRequest(Request.Method.GET,
                AppConfig.URL_Car_Entries_UTILS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d("Utils", "Fetching Cars Response: " + response.toString());
//                hideDialog(pDialog);

                try {
                    ArrayList<String> temp = new ArrayList<String>();
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // car entries fetched successfully
                        // Now store the user in SQLite
                        String uid = jObj.getString("car_entries");
                        int tempCarsEntries = 100 - Integer.parseInt(uid);
                        String timeStamp = jObj.getString("time_stamp");
                        textView.setText("" + tempCarsEntries);
                        timeStampTV.setText(timeStamp);

                    } else {
                        // Error in fetching videos. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(context,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(context, "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Utils", "Fetching Car Entries Error: " + error.getMessage());
                Toast.makeText(context,
                        error.getMessage(), Toast.LENGTH_LONG).show();
//                hideDialog(pDialog);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
//                params.put("table", table);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private static void showDialog(ProgressDialog pDialog) {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private static void hideDialog(ProgressDialog pDialog) {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    public static void startCellAnimation(ImageView cell) {

        cell.setImageResource(R.drawable.ic_cast_disabled_light);
        cell.setBackgroundResource(R.drawable.fetching_car_anim);

        AnimationDrawable frameAnimation = (AnimationDrawable) cell.getBackground();
        frameAnimation.start();
    }


    public static void drawPrimaryLinePath( ArrayList<Location> listLocationsToDraw, GoogleMap map )
    {
        if ( map == null )
        {
            return;
        }
        if ( listLocationsToDraw.size() < 2 )
        {
            return;
        }
        PolylineOptions options = new PolylineOptions();

        options.color(Color.parseColor("#CC0000FF"));
        options.width(5);
        options.visible( true );

        for ( Location locRecorded : listLocationsToDraw )
        {
            options.add( new LatLng( locRecorded.getLatitude(),
                    locRecorded.getLongitude() ) );
        }
        map.addPolyline(options);
    }
}
