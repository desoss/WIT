package it.polimi.dmw.wit.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitLocationAPI;
import it.polimi.dmw.wit.utilities.WitLocationProvider;
import it.polimi.dmw.wit.utilities.WitTimeoutThread;

public class WitMapsActivity extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    GoogleMap map;
    private WitDownloadTask witDownloadTask;
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private double poiLat, poiLon, currentLat, currentLon;
    private Intent intent;
    private Location currentLocation;
    private WitLocationProvider locationProvider;
    private LocationManager locationManager;
    private boolean gpsEnabled;
    private final static String LOG_TAG = "WitMap";
    private boolean stop;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wit_maps);


        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_Maps);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);

        intent = getIntent();
        poiLat = intent.getDoubleExtra(WitSavedPOI.EXTRA_LAT, 0);
        poiLon = intent.getDoubleExtra(WitSavedPOI.EXTRA_LON,0);


        }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart()");
        stop = false;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
        } else {
            // Resetta la posizione
            currentLocation = null;

            locationProvider = new WitLocationAPI(new GoogleApiClient.Builder(this));

            locationProvider.startGettingLocation();

        }
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // Se sono connesso
        if (networkInfo != null && networkInfo.isConnected() && gpsEnabled) {

            // Fa partire il timeout thread che a sua volta fare il check periodico della Location
            WitTimeoutThread timeoutThread = new WitTimeoutThread(new TimeoutHandler());
            timeoutThread.start();
        } else {
            // display error
            //  showWirelessSettingsAlert();

        }



    }

    private void setMap(){


        // Getting reference to SupportMapFragment of the activity_main
        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Getting Map for the SupportMapFragment
        map = fm.getMap();


        if (map != null) {

            // Enable MyLocation Button in the Map
            map.setMyLocationEnabled(true);

            currentLat = currentLocation.getLatitude();
            currentLon = currentLocation.getLongitude();




            LatLng point = new LatLng(poiLat, poiLon);
            LatLng point2 = new LatLng(currentLat, currentLon);



            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(point)      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));




            // Creating MarkerOptions
            MarkerOptions options = new MarkerOptions();

            // Setting the position of the marker
            options.position(point);

            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            map.addMarker(options);
            options.position(point2);

            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));


            // Add new marker to the Google Map Android API V2
            map.addMarker(options);


                LatLng origin = point;
                LatLng dest = point2;

                // Getting URL to the Google Directions API
                String u = getDirectionsUrl(origin, dest);
                URL url = null;
                try {
                    url = new URL(u);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.d(LOG_TAG, "URL Fail: " + u);

                }
                downloadMapInfo(url);


            }
        }



    @Override
    public void onStop() {
        super.onStop();
        stop=true;

        Log.d(LOG_TAG, "onStop()");

        if (gpsEnabled) {
            // Spegni i provider per non consumare batteria
            locationProvider.stopGettingLocation();
            locationProvider = null;
        }

    }


    private void downloadMapInfo(URL url) {

    WitDownloadTask witdownloadTask = new WitDownloadTask(this, null, WitDownloadTask.Maps);

    // Start downloading json data from Google Directions API
    witdownloadTask.execute(url);
}


    @Override
    public void onDrawerItemSelected(View view, int position) {
        displayView(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startSettingPage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }




    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        String mode ="mode=walking";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor+"&"+mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }



        public void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(15);
                lineOptions.color(Color.BLUE);
            }

            // Drawing polyline in the Google Map for the i-th route
            map.addPolyline(lineOptions);
        }
    private void displayView(int position) {
        Fragment fragment = null;
        Intent i = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case 0:
                i = new Intent(this, WitMainActivity.class);
                startActivity(i);
                break;
            case 1:
                i = new Intent(this, WitPOIsList.class);
                startActivity(i);
                break;
            case 2:
                i = new Intent(this, WitDiary.class);
                startActivity(i);
                break;
            case 3:

                break;


            default:
                break;
        }

        if (fragment != null) {
          /*  FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container_body, fragment);
            fragmentTransaction.commit();  */

            // set the toolbar title
            getSupportActionBar().setTitle(title);
        }
    }

    private void startSettingPage(){
        Intent i = new Intent(this, WitSettings.class);
        startActivity(i);

    }


    /**
     * Gestore dei messaggi tra timeoutThread e activity
     */
    private class TimeoutHandler extends Handler {

        private boolean messagesEnabled = true;
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (messagesEnabled&&!stop) {
                // In base al codice del messaggio ricevuto
                switch (msg.what) {
                    case WitTimeoutThread.CHECK_LOCATION_CODE:
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Location foundLocation = locationProvider.getLocation();
                            Log.d(LOG_TAG, "Location found A: " + foundLocation.getAccuracy());
                            // Aggiorna la currentLocation
                            currentLocation = foundLocation;
                            messagesEnabled = false;
                            // Vai a mandare la richiesta al server
                            setMap();
                            Log.d(LOG_TAG, "Set Map");

                        }
                        break;
                    case WitTimeoutThread.TIMEOUT_5:
                        Log.d(LOG_TAG, "Check location after 5s message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Log.d(LOG_TAG, "Location found");
                            Location foundLocation = locationProvider.getLocation();
                            Log.d(LOG_TAG, "Location found after 5s A: "+foundLocation.getAccuracy());
                            // Aggiorna la currentLocation
                            currentLocation = foundLocation;
                            messagesEnabled = false;
                            setMap();
                            Log.d(LOG_TAG, "Set Map");
                        }
                        break;
                    case WitTimeoutThread.TIMEOUT_10:
                        Log.d(LOG_TAG, "Check location after 10s message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Log.d(LOG_TAG, "Location found");
                            Location foundLocation = locationProvider.getLocation();
                            Log.d(LOG_TAG, "Location found after 10s A: "+foundLocation.getAccuracy());
                            // Aggiorna la currentLocation
                            currentLocation = foundLocation;
                            messagesEnabled = false;
                            // Vai a mandare la richiesta al server
                            setMap();
                            Log.d(LOG_TAG, "Set Map");
                        }
                        break;
                    case WitTimeoutThread.TIMEOUT_CODE:
                        Log.d(LOG_TAG, "Timeout message received");
                        // Se hai un messaggio di timeout e non hai ancora nessuna location
                        if (currentLocation == null) {
                            // Notifica il timeout
                            reportTimeout();
                        }
                        break;
                    default:
                }
            }
        }
    }

    private void reportTimeout() {

        Toast.makeText(this, "Unable to get GPS location.", Toast.LENGTH_LONG).show();
    }



}
