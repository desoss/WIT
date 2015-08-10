package it.polimi.dmw.wit.activities;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pnikosis.materialishprogress.ProgressWheel;
import java.net.MalformedURLException;
import java.net.URL;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.utilities.WitDownloadImageTask;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitLocationAPI;
import it.polimi.dmw.wit.utilities.WitLocationProvider;
import it.polimi.dmw.wit.utilities.WitTimeoutThread;

public class WitInfo extends Fragment {

    private boolean gpsEnabled = false;
    private Location currentLocation = null;
    private View v;
    private WitLocationProvider locationProvider;
    private LocationManager locationManager;
    private final static String LOG_TAG = "WitInfo";
    private boolean stop;
    private WitDownloadTask witDownloadTask;
    private String city,  county,  state,  woeid, imageCityUrl, code, temp, text;
    private WitDownloadImageTask witDownloadImageTask;
    private ImageView mainImage, weatherImage;
    private TextView titleText, weatherText;
    private ProgressWheel progressWheel;
    private DbAdapter dbAdapter;
    private Cursor cursor;





    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         v = inflater.inflate(R.layout.tab_info,container,false);
        mainImage = (ImageView)v.findViewById(R.id.city_img);
        weatherImage = (ImageView)v.findViewById(R.id.weather_img);
        titleText = (TextView)v.findViewById(R.id.city_name_text);
        weatherText = (TextView)v.findViewById(R.id.weather_text);
        progressWheel = (ProgressWheel) v.findViewById(R.id.progress_wheel);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart()");
        stop = false;

        // Verifica che il GPS sia acceso
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            showGPSSettingsAlert();
        } else {
            // Resetta la posizione
            currentLocation = null;

            // Inizia a cercare GPS e inizializza il provider

            // Provider vecchio
            //locationProvider = new WitLocationOldStyle(locationManager);

            // Provider nuovo
            locationProvider = new WitLocationAPI(new GoogleApiClient.Builder(getActivity()));

            locationProvider.startGettingLocation();

        }
        ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // Se sono connesso
        if (networkInfo != null && networkInfo.isConnected()) {

            // Fa partire il timeout thread che a sua volta farà il check periodico della Location
            WitTimeoutThread timeoutThread = new WitTimeoutThread(new TimeoutHandler());
            timeoutThread.start();
        } else {
            // display error
          //  showWirelessSettingsAlert();

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

    private void startDownloadInfo(){

        progressWheel = new ProgressWheel(getActivity());
        progressWheel.spin();


        currentLocation = locationProvider.getLocation();
        String lat = String.valueOf(currentLocation.getLatitude());
        String lon = String.valueOf(currentLocation.getLongitude());

        final String url = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20geo.placefinder%20where%20text%3D%22"+lat+"%2C%20"+lon+"%22%20and%20gflags%3D%22R%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";

        getWoeid(url);

    }

    private void getWoeid(String serverUrl) {

        Log.d(LOG_TAG, "SERVER URL: " + serverUrl);


        try {
            URL url = new URL(serverUrl);

            // WitDownloadTask è la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.WOEID);
            witDownloadTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void saveInfo(String city, String county, String state, String woeid){
        this.city = city;
        this.county = county;
        this.state = state;
        this.woeid = woeid;
        if(!checkWoeid()) {
            searchImageCity();
            getWeather();
        }
    }


    private boolean checkWoeid() {
        dbAdapter = new DbAdapter(getActivity());
        dbAdapter.open();
        cursor = dbAdapter.fetchCITYINFO();
        while (cursor.moveToNext()) {
            int w = Integer.parseInt(woeid);
            if (cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID)) == w) {
                v.findViewById(R.id.progress_wheel).setVisibility(View.GONE);
                titleText.setText(city);
                weatherText.setText(cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_WTEXT)));
                code = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_WCODE));
                setImageWeather();
                byte[] img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_IMAGE));
                if (img != null) {
                    mainImage.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
                }
                return true;

            }
        }
        cursor.close();
        dbAdapter.close();
        return false;
    }


    private void searchImageCity(){
        String city2 = city.replace(" ","%20");
        final String u = "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q="+city2+"%20city&imgsz=small|medium|large|xlarge";
        Log.d(LOG_TAG, "SERVER URL: " + u);
        try {
            URL url = new URL(u);
            // WitDownloadTask è la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.IMAGECITY);
            witDownloadTask.execute(url);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public void saveImageCityUrl(String imageCityUrl){
        this.imageCityUrl = imageCityUrl;
        Log.d(LOG_TAG,"imagecityurl: "+imageCityUrl);
        if(imageCityUrl!=null) {
            downloadImageCity();
        }

    }

    private void downloadImageCity(){
        Log.d(LOG_TAG, "SERVER URL: " + imageCityUrl);

        try {
            URL url = new URL(imageCityUrl);
            // WitDownloadTask è la classe che gestisce il download
            witDownloadImageTask = new WitDownloadImageTask(null, this, witDownloadImageTask.CITY);
            witDownloadImageTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void setImageCity(Bitmap result, byte[] img) {
        //progressWheel.setVisibility(View.INVISIBLE);
        v.findViewById(R.id.progress_wheel).setVisibility(View.GONE);
        mainImage.setImageBitmap(result);
        titleText.setText(city);
        String w = temp +"c, "+text;
        weatherText.setText(w);
        setImageWeather();
        saveCityInfo(w, img);
    }

    private void getWeather(){
        final String u = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20("+woeid+")%20and%20u%3D'c'&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
        Log.d(LOG_TAG, "SERVER URL: " + u);
        try {
            URL url = new URL(u);
            // WitDownloadTask è la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.WEATHER);
            witDownloadTask.execute(url);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void saveWeather(String code, String temp, String text){
        this.code = code;
        this.temp = temp;
        this.text = text;


    }

    private void setImageWeather(){
        String uri = "@drawable/a"+code;
        Log.d(LOG_TAG,uri);
        Log.d(LOG_TAG,getActivity().getPackageName());

        int imageResource = getResources().getIdentifier(uri, null,  getActivity().getPackageName());

        Drawable res = getResources().getDrawable(imageResource);

        weatherImage.setImageDrawable(res);


    }

    private void saveCityInfo(String w, byte[] img){
        dbAdapter = new DbAdapter(getActivity());
        dbAdapter.open();
        dbAdapter.saveCityInfo(Long.parseLong(woeid), city, w, code, img);
        dbAdapter.close();
    }










    /* Mostra un dialog per attivare il GPS
    * qui cancel chiude l'applicazione
            */
    private void showGPSSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("Unable to connect");

        // Setting Dialog Message
        alertDialog.setMessage("You need a GPS connection to use this app. Please turn on GPS in Settings?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                // Chiude l'applicazione, o GPS o nada.
                getActivity().finish();
            }
        });

        // Showing Alert Message
        alertDialog.show();
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
                            Log.d(LOG_TAG, "Location found A: "+foundLocation.getAccuracy());
                                // Aggiorna la currentLocation
                                currentLocation = foundLocation;
                                messagesEnabled = false;
                                // Vai a mandare la richiesta al server
                            startDownloadInfo();
                            Log.d(LOG_TAG, "Start download info");

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
                                // Vai a mandare la richiesta al server
                            startDownloadInfo();
                            Log.d(LOG_TAG, "Start download info");
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
                            startDownloadInfo();
                            Log.d(LOG_TAG, "Start download info");
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

        Toast.makeText(getActivity(), "Unable to get GPS location.", Toast.LENGTH_LONG).show();
    }




}