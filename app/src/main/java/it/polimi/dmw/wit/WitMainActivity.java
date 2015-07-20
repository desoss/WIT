package it.polimi.dmw.wit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;



/**
 * Main activity dell'applicazione, gestisce i sensori e i download.
 *
 * Coordinate della Tour Eiffel (non si sa mai):
 *  lat=48.858252
 *  lon=2.29451
 *
 */
public class WitMainActivity extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {



    private boolean stop;
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;






    /**
     * Tag per il log
     */
    private final static String LOG_TAG = "WitMainActivity";

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
     * ArrayList per conteneri i POIs
     */
    ArrayList<WitPOI> poiList;

    /**
     * Classe privata per downloadare il JSON senza bloccare la user interface, viene eseguita
     * su un thread a parte. E' una classe interna così può chiamare i metodi dell'activity
     * direttamente
     */
    private class WitDownloadTask extends AsyncTask<URL, Void, String> {

        // Membri per leggere la risposta HTTP
        private InputStream is;
        private BufferedReader br;
        private StringBuilder sb;
        private String line;

        public WitDownloadTask() {
            is = null;
            br = null;
            sb = null;
            line = "";
        }

        /**
         * Metodo che viene chiamato quando parte il thread secondario. Fa la richiesta e
         * riceve il JSON come una stringa
         *
         * @param params array che contiene gli URL da contattare, params[0] è il server con già
         *               la richiesta pronta.
         * @return la String contenente il JSON scaricato
         */
        @Override
        protected String doInBackground(URL... params) {

            try {
                // Crea la connessione HTTP
                HttpURLConnection conn = (HttpURLConnection) params[0].openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();

                // Riceve la risposta
                int response = conn.getResponseCode();

                // Logga il codice HTTP (200, 404, etc.)
                Log.d("WitMainActivity", "HTTP CODE: " + String.valueOf(response));

                // Prende il contenuto della risposta come un InputStream
                is = conn.getInputStream();

                sb = new StringBuilder();

                try {
                    // Legge la risposta linea per linea e la inserisce nello StringBuilder
                    br = new BufferedReader(new InputStreamReader(is));
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                } catch (IOException e) {
                    Log.e("WitMainActivity","ERROR: cannot read input stream");
                    e.printStackTrace();
                } finally {
                    if (br != null) {
                        br.close();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Dallo string builder esce la string con il JSON
            return sb.toString();
        }

        /**
         * Questo viene chiamato quando il thread a finito e gli viene passato il risultato
         * del metodo precedente. Con questo risultato chiama il metodo della Activity per
         * parsare il JSON.
         *
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            // I super lasciamoli che fa serio
            super.onPostExecute(s);

            // Chiama il metodo dell'activity per gestire il JSON

            if(stop){
                onPause();
            }
            else{
            parseJsonPOIs(s);}

        }
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
                        Log.d(LOG_TAG, "Check location <5m message received");
                        // Se non hai ancora una location e il provider ha trovato una location
                         if ((currentLocation == null) && locationProvider.hasLocation()) {
                            Location foundLocation = locationProvider.getLocation();
                            Log.d(LOG_TAG, "Location found A: "+foundLocation.getAccuracy());
                            if (foundLocation.getAccuracy() <= MAX_ACCURACY) { //se ho accuratezza <=5m uso la location se no aspetto
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
                            if (foundLocation.getAccuracy() <= MEDIUM_ACCURACY) { //se ho accuratezza <=10m uso la location
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
                            if (foundLocation.getAccuracy() <= MIN_ACCURACY) { //se ho accuratezza <30m uso la location
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

       /* // Add code to print out the key hash
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

        setContentView(R.layout.activity_wit_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
      //  displayView(0);




        // Inizializza la lista monumenti
        poiList = new ArrayList<WitPOI>();

        // Inizializza i riferimenti hai gestori di sistema
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Store button in member
        scanButton = (Button)findViewById(R.id.scan_button);

        // Load animations
        scanDefaultAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_default_animation);
        scanClickedAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_clicked_animation);

        // Inizializza riferimenti alla UI
        scanText = (TextView)findViewById(R.id.scan_text);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart()");
        stop=false;

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
            locationProvider = new WitLocationAPI(new GoogleApiClient.Builder(this));

            locationProvider.startGettingLocation();

            // Lo inizializzo ma non lo attivo, per l'orientamento non abbiamo
            // bisogno di aspettare quindi lo attiviamo al click dello scan
            orientationProvider = new WitOrientationProvider(sensorManager);
        }
    }

    @Override
    protected void onStop() {
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
    protected void onResume() {
        // Always call superclass method first
        super.onResume();

        Log.d(LOG_TAG, "onResume()");


        // start default animation
        scanButton.startAnimation(scanDefaultAnimation);
        scanText.setText(R.string.scan_button_text);

    }

    @Override
    protected void onPause() {
        // Always call superclass method first
        super.onPause();
        Log.d(LOG_TAG, "onPause()");

        // Spegni l'animazione quando l'utente non sta guardando
        scanButton.clearAnimation();
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



    /**
     * Metodo per gestire il click sul bottone scan,
     *
     * @param view la View che è stata cliccata
     */
    public void scanClickHandler(View view) {

        // Inizia l'animazione e cambia il testo
        scanText.setText(R.string.scanning_button_text);
        scanButton.startAnimation(scanClickedAnimation);

        // Verifico se ho una connessione internet
        ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        // Se sono connesso
        if (networkInfo != null && networkInfo.isConnected()) {
            // Attiva il sensore di orientamento
            if (!orientationEnabled) {
                orientationProvider.startGettingOrientation();
                orientationEnabled = true;
            }

            // Fa partire il timeout thread che a sua volta farà il check periodico della Location
            WitTimeoutThread timeoutThread = new WitTimeoutThread(new TimeoutHandler());
            timeoutThread.start();
        } else {
            // display error
            showWirelessSettingsAlert();
            stopAnimation();

        }

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

        Toast.makeText(this, "Unable to get GPS location.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Crea la richiesta per il server con le coordinate più recenti
     * e verifica che internet sia attivo, come per il GPS.
     */
    private void getPOIs() {
        String lat = String.valueOf(currentLocation.getLatitude());
        String lon = String.valueOf(currentLocation.getLongitude());


        // Crea l'url con i parametri giusti per il server
        final String url = getString(R.string.get_monuments_base_url)+"?lat=" + lat + "&lon=" + lon + "&json=true&side=1&max=100";

            getMonumentsFromServer(url);

    }

    /**
     * Prende una stringa di input, crea un ArrayList con i POI contenuti nel JSON
     * e fa partire l'activity per mostrare i risultati.
     *
     * NOTA: questo metodo è chiamato del thread che scarica il JSON dopo che ha finito
     * di scaricare, però viene eseguito dal thread principale dell'applicazione.
     *
     * @param resultJson la stringa contenente il JSON
     */
    public void parseJsonPOIs(String resultJson) {
        /*

            Struttura Json

            {
                places: [array] o null
                found: value,
                side: value
            }

         */

        // Inizializza gli oggetti per il parsing
        JSONTokener tokener = null;
        JSONObject documentObject = null;
        JSONObject place = null;
        JSONArray places = null;
        JSONArray polygon = null;
        JSONObject coords = null;
        float[] x;
        float[] y;


        // Contiene il numero di monumenti ritornati
        int poisNumber = 0;

        Log.d(LOG_TAG,"JSON received! Length = "+resultJson.length());
        Log.d(LOG_TAG,resultJson);

        // Pulisci la lista dei monumenti
        poiList.clear();

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            // Prendi il primo oggetto JSON,
            // Sarebbe l'oggetto {places :[] , found: ..., side : ...}
            documentObject = (JSONObject)tokener.nextValue();

            // Verifica quanti monumenti ho
            poisNumber = documentObject.getInt("found");
            Log.d(LOG_TAG,"Trovati: "+poisNumber);

            // Se l'array dei places non è vuoto
            if (!documentObject.isNull("places")) {
                // Prendi l'array dei monumenti
                places = documentObject.getJSONArray("places");
            }

            // Per ogni monumento nell'array places
            for (int i = 0; i < poisNumber; i++) {

                // Prendi l'oggetto corrispondente al monumenti i-esimo
                // sarebbe l'oggetto con i campi id, name, distance, lat, lon e l'array polygon
                place = places.getJSONObject(i);
                polygon = place.getJSONArray("polygon");
                x = new float [polygon.length()];
                y = new float [polygon.length()];
             //   Log.d(LOG_TAG,place.getString("name"));


                //salvo in due array x e y le coordinate dei punti del poligono di ogni place
                for(int j=0; j<polygon.length();j++){
                    coords = polygon.getJSONObject(j);
                    x[j] = Float.parseFloat(coords.getString("y"));
                    y[j] = Float.parseFloat(coords.getString("x"));
                  //  Log.d(LOG_TAG,"x: "+x[j]+" y: "+y[j]);

                }
               // Log.d(LOG_TAG,"-------------------------------");

                // Crea un oggetto WitPOI per ogni monumento del JSON
                poiList.add(new WitPOI(
                        place.getInt("id"),
                        place.getString("name"),
                        place.getDouble("distance"),
                        place.getDouble("lat"),
                        place.getDouble("lon"),
                        x,
                        y
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Crea un intent per far partire un'altra Activity
        Intent intent = new Intent(this, WitFinalResult.class);

        Log.d(LOG_TAG,"Latitude = "+String.valueOf(currentLocation.getLatitude()));
        Log.d(LOG_TAG,"Longitude = "+String.valueOf(currentLocation.getLongitude()));
        Log.d(LOG_TAG,"Accuracy = "+currentLocation.getAccuracy());
        Log.d(LOG_TAG,"Orientation = "+String.valueOf(Math.toDegrees(orientationProvider.getOrientation(currentLocation))));

        // Inserisci come dati
        // - la lista dei POI
        // - latitude e longitudine dell'utente
        // - orientazione del telefono.
        intent.putParcelableArrayListExtra(EXTRA_POI_LIST, poiList);
        intent.putExtra(EXTRA_USER_LAT,currentLocation.getLatitude());
        intent.putExtra(EXTRA_USER_LON,currentLocation.getLongitude());
        intent.putExtra(EXTRA_USER_ORIENTATION,orientationProvider.getOrientation(currentLocation));

        // Fai partire l'attività dei risultati
        startActivity(intent);
    }

    /**
     * Crea un URL e fa partire il thread che gestisce il download del JSON
     *
     * @param serverUrl url del server già completo di parametri
     */
    private void getMonumentsFromServer(String serverUrl) {

        Log.d("WitMainActivity","SERVER URL: "+serverUrl);


        try {
            URL url = new URL(serverUrl);

            // WitDownloadTask è la classe private che gestisce il download, vedere sopra.
            new WitDownloadTask().execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mostra un dialog per attivare il WiFi
     * cancel chiude il dialogo
     */
    private void showWirelessSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Unable to connect");

        // Setting Dialog Message
        alertDialog.setMessage("You need a network connection to use this app. Please turn on mobile network in Settings?");

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
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

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
                finish();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    private void displayView(int position) {
        Fragment fragment = null;
        Intent i = null;
        String title = getString(R.string.app_name);
        switch (position) {
            case 0:
               // i = new Intent(this, WitMainActivity.class);
                //startActivity(i);
                break;
            case 1:
                i = new Intent(this, WitPOIsList.class);
                startActivity(i);
                break;
            case 2:
                i = new Intent(this, WitFacebookLogin.class);
                startActivity(i);
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



}