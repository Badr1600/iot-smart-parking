package iot.android.learing.iot_smartparking;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_CONTACTS;

public class ParkingActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final String TAG = "Parking Service";
    private static final int REQUEST_FINE_LOCATION = 0;

    private TextView parkingLocation;
    private TextView parkingTime;
    private GoogleMap parkingMap;
    private Location mLastLocation;
    private Marker mMarker;

    private GoogleApiClient mGoogleAPIClient;
    private LocationRequest mLocationRequest;
    private LocationManager lM;

    private ArrayList<Location> locationTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        parkingLocation = (TextView) findViewById(R.id.parking_location_data);
        parkingTime = (TextView) findViewById(R.id.parking_time_data);

        lM = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        buildGoogleAPIClient();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.parking_map);
        mapFragment.getMapAsync(this);

        Double lat = 0.0;
        Double lang = 0.0;
        Intent intent = getIntent();
        if (null != intent) {
//            lat = 30.0300865;
//            lang = 31.4059558;
            lat = intent.getDoubleExtra("lat",lat);
            lang = intent.getDoubleExtra("lang", lang);
            mLastLocation = new Location("");
            mLastLocation.setLatitude(lat);
            mLastLocation.setLongitude(lang);
        }
        Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses.size() > 0) {
            parkingLocation.setText(addresses.get(0).getCountryName() + ", "
                    + addresses.get(0).getFeatureName());
        } else {
            parkingLocation.setText("Address not Available" + "\n" + mLastLocation.getLatitude() + ":" + mLastLocation.getLongitude());
        }

        DateFormat df = new SimpleDateFormat("h:mm:ss a");
        String date = df.format(Calendar.getInstance().getTime());
        parkingTime.setText(date);

        locationTemp = new ArrayList<Location>();
        Location tempLocation = new Location("");
        tempLocation.setLatitude(mLastLocation.getLatitude());
        tempLocation.setLongitude(mLastLocation.getLongitude());

        locationTemp.add(tempLocation);
    }

    protected synchronized void buildGoogleAPIClient() {
        mGoogleAPIClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    private boolean mayRequestFineLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(parkingLocation, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
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
    public void onMapReady(GoogleMap googleMap) {
        parkingMap = googleMap;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (mayRequestFineLocation()) {
                parkingMap.setMyLocationEnabled(true);
            }
        } else {
            parkingMap.setMyLocationEnabled(true);
        }
        parkingMap.addMarker(new MarkerOptions()
                .position(new LatLng(locationTemp.get(0).getLatitude(), locationTemp.get(0).getLongitude()))
                .title("Parking Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        if (mLastLocation != null) {
            mMarker = parkingMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .title("Current Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 17);
            parkingMap.animateCamera(yourLocation);
        }

        parkingMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    mayRequestFineLocation();
                }
                if (lM.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIClient);
                    mMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                    CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 17);
                    parkingMap.animateCamera(yourLocation);
                } else {
                    requestEnableLocationServices();
                }
                return true;
            }
        });
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

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleAPIClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleAPIClient.isConnected()) {
            mGoogleAPIClient.disconnect();
        }
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
        locationTemp.add(1, location);
        mMarker.setPosition(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
//        Utils.drawPrimaryLinePath(locationTemp,parkingMap);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection Failed: " + connectionResult.getErrorCode());
    }

    private void requestEnableLocationServices() {
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
                            status.startResolutionForResult(ParkingActivity.this, 1000);
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
