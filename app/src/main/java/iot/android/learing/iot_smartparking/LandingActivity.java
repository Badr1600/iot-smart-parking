package iot.android.learing.iot_smartparking;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import iot.android.learing.iot_smartparking.helper.UIUpdater;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;

public class LandingActivity extends AppCompatActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, OnMapReadyCallback {

    private static final String TAG = "Landing Service";
    private static final int REQUEST_FINE_LOCATION = 0;

    private GoogleMap mMap;
    private Marker mMarker;

    private TextView carEntriesNum;
    private TextView carEntriesTime;
    private ImageView imageView;
    protected UIUpdater uiUpdater;

    private GoogleApiClient mGoogleAPIClient;
    protected static Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private LocationManager lM;

    private double accelerationOld = 0.0;
    private double[] avgAcceleration = new double[11];
    private int index = 0;

    private TextView stateIndicatorTextView;
    private TextView locationFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        stateIndicatorTextView = (TextView) findViewById(R.id.movment_state_indecator);
        stateIndicatorTextView.setMovementMethod(new ScrollingMovementMethod());
        locationFound = (TextView) findViewById(R.id.parking_location_found);
        locationFound.setMovementMethod(new ScrollingMovementMethod());
        // initiate sensor manger
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        lM = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //initiate accelerometer sensor from the sensor manger
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLastLocation = null;
        buildGoogleAPIClient();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        carEntriesNum = (TextView) findViewById(R.id.textView);
        carEntriesTime = (TextView) findViewById(R.id.textView8);
        imageView = (ImageView) findViewById(R.id.imageView);

        uiUpdater = new UIUpdater(new Runnable() {
            @Override
            public void run() {
                Utils.fetchCarEntries(getApplicationContext(), carEntriesNum,carEntriesTime);
                Utils.startCellAnimation(imageView);
//                DateFormat df = new SimpleDateFormat("h:mm:ss a");
//                String date = df.format(Calendar.getInstance().getTime());
//                carEntriesTime.setText(date);
            }
        }, 5000);
        uiUpdater.startUpdates();


        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleAPIClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleAPIClient.isConnected()) {
            stopLocationUpdates();
            mGoogleAPIClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleAPIClient, this);
    }

    protected synchronized void buildGoogleAPIClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        final double alpha = 0.8;

        //gravity is calculated here
        double[] gravityV = new double[3];
        gravityV[0] = alpha * gravityV[0] + (1 - alpha) * event.values[0];
        gravityV[1] = alpha * gravityV[1] + (1 - alpha) * event.values[1];
        gravityV[2] = alpha * gravityV[2] + (1 - alpha) * event.values[2];

        //acceleration retrieved from the event and the gravity is removed
        double x = event.values[0] - gravityV[0];
        double y = event.values[1] - gravityV[1];
        double z = event.values[2] - gravityV[2];
        double acceleration = Math.sqrt(Math.pow(x, 2) +
                Math.pow(y, 2) +
                Math.pow(z, 2));

        storeAcceleration(acceleration);
    }

    /**
     * store the difference between each two intervals (old and new).
     * @param acceleration
     */
    private void storeAcceleration(double acceleration) {
        avgAcceleration[index] = Math.abs(acceleration - accelerationOld);
        accelerationOld = acceleration;
        index++;
        if (index > 10) {
            index = 0;
            detectState();
        }
    }

    /**
     * Detect the state of the moving according to the summation of the difference of each interval.
     */
    private void detectState() {
        double sumOfDiff = 0;
        for (int i = 1; i < 10; i++) {
            sumOfDiff += avgAcceleration[i];
        }
        if (sumOfDiff > 10.0) {
            stateIndicatorTextView.setText("WALKING = " + sumOfDiff);
        } else {
            stateIndicatorTextView.setText("DRIVING = " + sumOfDiff);
        }
    }

    public void detectMovementState(View v) {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        requestLocationUpdates();
    }

    public void arrivedToDestinationHandler(View v) {
        sensorManager.unregisterListener(this);
        Location tempLocation = mLastLocation;
        Intent i = new Intent(this,ParkingActivity.class);
        i.putExtra("lat",mLastLocation.getLatitude());
        i.putExtra("lang", mLastLocation.getLongitude());
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleAPIClient,this);
        startActivity(i);
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        requestEnableLocationServices();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (mayRequestFineLocation()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, mLocationRequest, this);
            }
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIClient, mLocationRequest, this);
        }
    }

    private boolean mayRequestFineLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(stateIndicatorTextView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
                        }
                    });
        } else {
            requestPermissions(new String[]{ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        }
        return false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleAPIClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location Changed!" + "\n" + "New Location: " + location.getLongitude() + ":" + location.getLatitude());
        mLastLocation = location;
        locationFound.setText("New Location: " + location.getLongitude() + ":" + location.getLatitude());

        mMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection Failed: " + connectionResult.getErrorCode());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (mayRequestFineLocation()) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }
        mMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(29, 30))
                .title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        if(mLastLocation != null){
            mMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        }

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mayRequestFineLocation();
                }
                //check if location is enabled
                // search how to remove the blue circle
                if (lM.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
                    mMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 17);
                    mMap.animateCamera(yourLocation);
                } else {
                    requestEnableLocationServices();
                }
                return true;
            }
        });
    }


    private void requestEnableLocationServices(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        // **************************
        // builder.setAlwaysShow(true); // this is the key ingredient
        // **************************
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                .checkLocationSettings(mGoogleAPIClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result
                        .getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                    // All location settings are satisfied. The client can initialize location requests here.
                     break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                     try {
                     // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                      status.startResolutionForResult(LandingActivity.this, 1000);
                     } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                     }
                        break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                             break;
                }
            }
        });
    }

}
