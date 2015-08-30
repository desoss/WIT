package it.polimi.dmw.wit.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.app.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.FacebookSdk;
import com.google.android.gms.common.api.GoogleApiClient;
import android.content.SharedPreferences;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.utilities.BackgroundService;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitLocationAPI;
import it.polimi.dmw.wit.utilities.WitLocationProvider;
import it.polimi.dmw.wit.utilities.WitOrientationProvider;
import it.polimi.dmw.wit.utilities.WitPOI;
import it.polimi.dmw.wit.utilities.WitTimeoutThread;
import android.view.View.OnClickListener;

import org.json.JSONException;
import org.json.JSONObject;


public class WitScan extends Fragment {


    private boolean stop;
    private  View v;
    private Double latMax;
    private Double lonMax;
    private Double latMin;
    private Double lonMin;
    private boolean BiggerSquareUsable = true;


    /**
     * Tag per il log
     */
    private final static String LOG_TAG = "WitScan";

    /**
     * Chiavi per passare valori tra un activity e l'altra
     */
    public final static String EXTRA_USER_LAT = "it.polimi.dmw.wit.USER_LAT";
    public final static String EXTRA_USER_LON = "it.polimi.dmw.wit.USER_LON";
    public final static String EXTRA_USER_ORIENTATION = "it.polimi.dmw.wit.USER_ORIENTATION";
    public final static String EXTRA_POI_LIST = "it.polimi.dmw.wit.POI_LIST";

    /**
     * Minimum accuracy for a Location to be valid, in meters
     */
    public final static int MIN_ACCURACY = 30;
    public final static int MEDIUM_ACCURACY = 10;
    public final static int MAX_ACCURACY = 20; //VALORE CORRETTO 5 METTERE >16 SOLO PER VELOCIZZARE PROVE CON FAKE LOCATION


    /**
     * Riferimenti alla User Interface
     */
    TextView scanText;
    Button scanButton;
    Animation scanDefaultAnimation;
    Animation scanClickedAnimation;

    /**
     * Riferimenti ai sensori
     */
    WitLocationProvider locationProvider;
    WitOrientationProvider orientationProvider;
    WitDownloadTask witDownloadTask;

    /**
     * Gestori dei sensori del sistema
     */
    LocationManager locationManager;
    SensorManager sensorManager;

    /**
     * Flag per gestire l'orientazione e gps
     */
    boolean orientationEnabled = false;
    boolean gpsEnabled = false;


    /**
     * Migliore location ricevute
     */
    Location currentLocation = null;


    /**
     * Gestore dei messaggi tra timeoutThread e activity
     */
    private class TimeoutHandler extends Handler {

        private boolean messagesEnabled = true;
        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("WIT", getActivity().MODE_PRIVATE); //recupero dal database automatico il woeid corrente
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (messagesEnabled&&!stop) {
                int woeid = sharedPrefs.getInt("woeid", 0);
                // In base al codice del messaggio ricevuto
                switch (msg.what) {
                    case WitTimeoutThread.CHECK_LOCATION_CODE:
                        Log.d(LOG_TAG, "Check location <5m message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Location foundLocation = locationProvider.getLocation();
                            Log.d(LOG_TAG, "Location found A: "+foundLocation.getAccuracy());
                            if (foundLocation.getAccuracy() <= MAX_ACCURACY&&woeid!=-1) { //se ho accuratezza <=5m uso la location se no aspetto
                                Log.d(LOG_TAG, "Location accurated is founded before 10s A: "+foundLocation.getAccuracy());
                                Log.d(LOG_TAG, "Starting server request");
                                // Aggiorna la currentLocation
                                currentLocation = foundLocation;
                                messagesEnabled = false;
                                // Vai a mandare la richiesta al server
                                getPOIs();
                            }
                        }
                        break;
                    case WitTimeoutThread.TIMEOUT_5:
                        Log.d(LOG_TAG, "Check location after 5s message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Log.d(LOG_TAG, "Location found");
                            Location foundLocation = locationProvider.getLocation();
                            if (foundLocation.getAccuracy() <= MEDIUM_ACCURACY&&woeid!=-1) { //se ho accuratezza <=10m uso la location
                                Log.d(LOG_TAG, "Location found after 5s A: "+foundLocation.getAccuracy());
                                Log.d(LOG_TAG, "Starting server request");
                                // Aggiorna la currentLocation
                                currentLocation = foundLocation;
                                messagesEnabled = false;
                                // Vai a mandare la richiesta al server
                                getPOIs();
                            }
                        }
                        break;
                    case WitTimeoutThread.TIMEOUT_10:
                        Log.d(LOG_TAG, "Check location after 10s message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                        if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Log.d(LOG_TAG, "Location found");
                            Location foundLocation = locationProvider.getLocation();
                            if (foundLocation.getAccuracy() <= MIN_ACCURACY&&woeid!=-1) { //se ho accuratezza <30m uso la location
                                Log.d(LOG_TAG, "Location found after 10s A: "+foundLocation.getAccuracy());
                                Log.d(LOG_TAG, "Starting server request");
                                // Aggiorna la currentLocation
                                currentLocation = foundLocation;
                                messagesEnabled = false;
                                // Vai a mandare la richiesta al server
                                getPOIs();
                            }
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


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         v =inflater.inflate(R.layout.tab_scan,container,false);
        scanButton = (Button)v.findViewById(R.id.scan_button);
        scanText = (TextView) v.findViewById(R.id.scan_text);


        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity());
        /*
        // Add code to print out the key hash
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "it.polimi.dmw.wit",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
        */


        // Inizializza i riferimenti hai gestori di sistema
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Store button in member

        // Load animations
        scanDefaultAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.scan_default_animation);
        scanClickedAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.scan_clicked_animation);

        // Inizializza riferimenti alla UI
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart()");
        stop=false;
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("WIT",getActivity().MODE_PRIVATE).edit();
        editor.putInt("woeid", -1);
        editor.commit();
        // Verifica che il GPS sia acceso
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            showGPSSettingsAlert();
        }
        else {
            // Resetta la posizione
            currentLocation = null;

            // Inizia a cercare GPS e inizializza il provider

            // Provider vecchio
            //locationProvider = new WitLocationOldStyle(locationManager);

            // Provider nuovo
            locationProvider = new WitLocationAPI(new GoogleApiClient.Builder(getActivity()));

            locationProvider.startGettingLocation();

            // Lo inizializzo ma non lo attivo, per l'orientamento non abbiamo
            // bisogno di aspettare quindi lo attiviamo al click dello scan
            orientationProvider = new WitOrientationProvider(sensorManager);
        }

        scanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                scanText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 21);
                // Inizia l'animazione e cambia il testo
                scanText.setText(R.string.scanning_button_text);
                scanButton.startAnimation(scanClickedAnimation);

                // Verifico se ho una connessione internet
                ConnectivityManager connMgr = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                // Se sono connesso
                if (networkInfo != null && networkInfo.isConnected()) {
                    // Attiva il sensore di orientamento
                    if (!orientationEnabled) {
                        orientationProvider.startGettingOrientation();
                        orientationEnabled = true;
                    }

                    // Fa partire il timeout thread che a sua volta fare il check periodico della Location
                    WitTimeoutThread timeoutThread = new WitTimeoutThread(new TimeoutHandler());
                    timeoutThread.start();
                } else {
                    // display error
                    showWirelessSettingsAlert();
                    stopAnimation();

                }

            }
        });
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

        if (orientationEnabled) {
            orientationProvider.stopGettingOrientation();
            orientationEnabled = false;
        }
        orientationProvider = null;
    }

    @Override
    public void onResume() {
        // Always call superclass method first
        super.onResume();

        Log.d(LOG_TAG, "onResume()");


        // start default animation
        scanButton.startAnimation(scanDefaultAnimation);
        scanText.setText(R.string.scan_button_text);

    }

    @Override
    public void onPause() {
        // Always call superclass method first
        super.onPause();
        Log.d(LOG_TAG, "onPause()");

        // Spegni l'animazione quando l'utente non sta guardando
        scanButton.clearAnimation();
    }


    /**
     * Metodo per fermare l'animazione di scan e rimettere quella di default
     */
    private void stopAnimation() {
        // Stop animation
        scanButton.clearAnimation();

        // Restore default scan button
        scanText.setText(R.string.scan_button_text);
        scanButton.startAnimation(scanDefaultAnimation);
    }

    /**
     * Metodo chiamato dopo il GPS timeout, mostra un messaggio di errore
     */
    private void reportTimeout() {

        stopAnimation();

        Toast.makeText(getActivity(), "Unable to get GPS location.", Toast.LENGTH_LONG).show();
    }

    /**
     * Crea la richiesta per il server con le coordinate pi� recenti
     * e verifica che internet sia attivo, come per il GPS.
     */
    private void getPOIs() {
        String lat = String.valueOf(currentLocation.getLatitude());
        String lon = String.valueOf(currentLocation.getLongitude());

        SharedPreferences sharedPrefs = getActivity().getSharedPreferences("WIT", getActivity().MODE_PRIVATE); //recupero dal database automatico il woeid corrente
        int woeid = sharedPrefs.getInt("woeid", 0);
        Log.d(LOG_TAG,"WOEID: "+woeid);

        // Crea l'url con i parametri giusti per il server
        witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.POISLIST);

        SharedPreferences prefs = getActivity().getSharedPreferences("bigSquareMonumentList", Context.MODE_PRIVATE);
        //uso il metodo definito da me altrimenti avrei dovuto usare stringhe (prefs.getString("max_lat",""); )
        latMax = getDouble(prefs,"max_lat");
        latMin = getDouble(prefs,"min_lat");
        lonMax = getDouble(prefs,"max_lon");
        lonMin = getDouble(prefs, "min_lon");

        Double latDouble = Double.parseDouble(lat);
        Double lonDouble = Double.parseDouble(lon);
        //se current pos è dentro l'internal square
        if(pointIntoInternalSquare(latDouble, lonDouble)) {
            try {
                String jsonString = readCache();
                if (jsonString != null){
                    Toast.makeText(getActivity(), "Ho usato il json della cache", Toast.LENGTH_LONG).show();
                    witDownloadTask.refreshPOIsList();
                    witDownloadTask.setLat(latDouble);
                    witDownloadTask.setLon(lonDouble);
                    witDownloadTask.setUseCacheJSON();
                    witDownloadTask.parseJsonPOIs(jsonString);
                }
               else{

                    final String url = getString(R.string.get_monuments_base_url)+"?lat=" + lat + "&lon=" + lon + "&json=true&side=1&max=100";
                    getMonumentsFromServer(url);
                    startBackgroundWorks(lat, lon);//<------------------------------ non sono sicuro di metterlo qui
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            final String url = getString(R.string.get_monuments_base_url)+"?lat=" + lat + "&lon=" + lon + "&json=true&side=1&max=100";
            getMonumentsFromServer(url);
            startBackgroundWorks(lat, lon);//<------------------------------ non sono sicuro di metterlo qui
        }
}
    private String readCache() throws IOException, JSONException {
        InputStreamReader in;
        StringBuilder responseStrBuilder = new StringBuilder();

        File cacheFile = new File(((Context) getActivity()).getCacheDir(), "cacheFile.srl");
        if (cacheFile.exists()) {

            in = new InputStreamReader(new FileInputStream(cacheFile), "UTF-8");
            BufferedReader buffReader = new BufferedReader(in);

            String inputStr;
            while ((inputStr = buffReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            in.close();
        }
        else{//se il file non esiste ritorna null
            return null;
        }
        return responseStrBuilder.toString();
    }

    double getDouble(final SharedPreferences prefs, final String key) {
        if ( !prefs.contains(key))
            BiggerSquareUsable = false;
        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }

    private boolean pointIntoInternalSquare(double lat, double lon){
        if(!BiggerSquareUsable)
            return false;

        if(lat >= latMin && lat <= latMax){
            if(lon >= lonMin && lon <= lonMax){
                return true;
            }
        }
        return false;
    }


     //Creates a new Intent to start the BackgroundService IntentService.s
    public void startBackgroundWorks(String lat, String lon) {
        Intent mServiceIntent = new Intent(getActivity(), BackgroundService.class);

        mServiceIntent.putExtra("lat", lat);
        mServiceIntent.putExtra("lon", lon);
        mServiceIntent.putExtra("range", "4");
        mServiceIntent.putExtra("max", "800");

        getActivity().startService(mServiceIntent);
        // Log.d("DownloadService", "Service Started!");
    }

    /**
     * Crea un URL e fa partire il thread che gestisce il download del JSON
     *
     * @param serverUrl url del server gi� completo di parametri
     */
    private void getMonumentsFromServer(String serverUrl) {

        Log.d(LOG_TAG, "SERVER URL: "+serverUrl);


        try {
            URL url = new URL(serverUrl);

            // WitDownloadTask � la classe che gestisce il download
            witDownloadTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void startfinalResult( ArrayList<WitPOI> poiList){
        // Crea un intent per far partire un'altra Activity
        Intent intent = new Intent(getActivity(), WitFinalResult.class);

        Log.d(LOG_TAG,"Latitude = "+String.valueOf(currentLocation.getLatitude()));
        Log.d(LOG_TAG,"Longitude = "+String.valueOf(currentLocation.getLongitude()));
        Log.d(LOG_TAG,"Accuracy = "+currentLocation.getAccuracy());
        Log.d(LOG_TAG,"Orientation = "+String.valueOf(Math.toDegrees(orientationProvider.getOrientation(currentLocation))));


        // Inserisci come dati
        // - la lista dei POI
        // - latitude e longitudine dell'utente
        // - orientazione del telefono.
        intent.putParcelableArrayListExtra(EXTRA_POI_LIST, poiList);
        intent.putExtra(EXTRA_USER_LAT, currentLocation.getLatitude());
        intent.putExtra(EXTRA_USER_LON, currentLocation.getLongitude());
        intent.putExtra(EXTRA_USER_ORIENTATION, orientationProvider.getOrientation(currentLocation));

        // Fai partire l'attivit� dei risultati
        startActivity(intent);

    }

    /**
     * Mostra un dialog per attivare il WiFi
     * cancel chiude il dialogo
     */
    private void showWirelessSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("Unable to connect");

        // Setting Dialog Message
        alertDialog.setMessage("You need a network connection to use this app. Please turn on mobile network in Settings");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Showing Alert Message
        alertDialog.show();
    }

    /**
     * Mostra un dialog per attivare il GPS
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



}
