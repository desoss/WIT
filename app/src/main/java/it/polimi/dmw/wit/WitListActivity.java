package it.polimi.dmw.wit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.ArrayList;

/**
 * Activity per gestire e visualizzare la lista dei risultati
 */
public class WitListActivity extends ActionBarActivity {

    private final static String LOG_TAG = "WitListActivity";

    /**
     * Ampiezza del cono di visione, per adesso ho fatto un paio di prove,
     * andrebbe verificata
     */
    private final static double coneWidth = Math.PI/6;

    /**
     * Membri per gestire le View
     */
    private ImageView mainImage;
    private TextView titleText;
    private TextView descText;

    String title;
    String description;

    /**
     * Lista di POI e lista dei POI filtrata
     */
    private ArrayList<WitPOI> poiList;
    private ArrayList<WitPOI> correctPoiList;

    /**
     * Coordinate dell'utente
     */
    double userLatitude;
    double userLongitude;

    /**
     * Orientazione del telefono dell'utente
     *
     * In radians considering
     *  NORTH = 0
     *  EAST = +90
     *  WEST = -90
     *  SUD = +180,-180
     */
    double userOrientation;

    /**
     * Classe privata per downloadare il JSON senza bloccare la user interface, viene eseguita
     * su un thread a parte. E' una classe interna così può chiamare i metodi dell'activity
     * direttamente
     */
    class WitDownloadTask extends AsyncTask<URL, Void, String> {

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
                    Log.e("WitMainActivity", "ERROR: cannot read input stream");
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
            // Chiama il metodo dell'activity per stampare i dettagli
            parseJsonDetail(s);

        }

    }
    /**
     * Classe privata per downloadare le immagini da stampare. Viene eseguita
     * su un thread a parte. E' una classe interna così può chiamare i metodi dell'activity
     * direttamente
     */
    private class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {

        protected Bitmap doInBackground(URL... urls) {
            Bitmap downloadBitmap = null;
            try {
                InputStream in = urls[0].openStream();
                downloadBitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return downloadBitmap;
        }

        protected void onPostExecute(Bitmap result) {
            mainImage.setImageBitmap(result);
        }
    }

    /**
     * Parsa il json dei dettagli e trova:
     *  titolo
     *  descrizione
     *  una foto
     *
     * @param resultJson
     */
    private void parseJsonDetail(String resultJson) {
        /*
            struttura JSON

            {
                title : "",
                description : "",
                photos : []
                ...
            }


         */

        JSONTokener tokener = null;
        JSONObject documentObject = null;
        JSONObject photo = null;
        JSONArray photos = null;

        Log.d(LOG_TAG,"JSON received! Length = "+resultJson.length());
        Log.d(LOG_TAG,resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            // Prendi il primo oggetto JSON,
            documentObject = (JSONObject)tokener.nextValue();

            // Prendi i campi di interesse
            title = documentObject.getString("title");
            description = documentObject.getString("description");

            titleText.setText(title);
            descText.setText(description);

            // Se l'array dei places non è vuoto
            if (!documentObject.isNull("photos")) {
                // Prendi l'array delle photo
                photos = documentObject.getJSONArray("photos");
                photo = photos.getJSONObject(0);
                if (photo != null) {
                    URL photoURL = new URL(photo.getString("960_url"));
                    new DownloadImageTask().execute(photoURL);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wit_detail);

        // Prendi l'intent che ha aperto questa activity, cioè
        // quello che viene dalla main activity
        Intent intent = getIntent();

        // Estrai la lista dei POI dall'intent.
        poiList = intent.getParcelableArrayListExtra(WitMainActivity.EXTRA_POI_LIST);

        // Estrai i dati dell'utente dall'intent
        userLatitude = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_LAT, 0.0);
        userLongitude = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_LON, 0.0);
        userOrientation = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_ORIENTATION, 0.0);

        mainImage = (ImageView)findViewById(R.id.poi_img);
        titleText = (TextView)findViewById(R.id.poi_name_text);
        descText = (TextView)findViewById(R.id.poi_desc_text);

        title = getString(R.string.not_found_title_text);
        description = getString(R.string.not_found_desc_text);

        // Applica l'algoritmo geometrico alla lista e ottieni la lista filtrata
        correctPoiList = new ArrayList<WitPOI>();

        Log.d(LOG_TAG, "Orientation NORTH from sensor : " + String.valueOf(Math.toDegrees(userOrientation)));
        Log.d(LOG_TAG, "Rotation from algorithm EAST : " + String.valueOf(Math.toDegrees(adjustAngle(userOrientation))));

        // TODO rimettere filtering
        for (WitPOI poi : poiList) {
            if (geometricCheck(userLongitude, userLatitude, poi.getPoiLon(), poi.getPoiLat(),userOrientation)) {
                correctPoiList.add(poi);
            }
        }

        if (correctPoiList.size()>0) {
            try {
                URL detailUrl = new URL("http://api.wikimapia.org/?key=example&function=place.getbyid&id=" + correctPoiList.get(0).getPoiId() + "&format=json&language=en");

                new WitDownloadTask().execute(detailUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
       }
       else {
           Drawable sadFace = getResources().getDrawable(R.drawable.sadface);
           mainImage.setImageDrawable(sadFace);
           titleText.setText(title);
           descText.setText(description);
       }
    }

    /**
     * The angle from the orientation sensor is clockwise starting from west
     * We need an angle counterclockwise starting from east
     *
     * @param userOrientation
     * @return
     */
    private double adjustAngle(double userOrientation){
        // Tra 0 e +180 -> -90  -270
        return (userOrientation - Math.PI/2);
    }

    /**
     * Returns if a given POI is inside a cone that starts from the user and has orientation
     *
     * @param userX latitude of user
     * @param userY longitude of user
     * @param poiX latitude of POI
     * @param poiY longitude of POI
     * @param userOrientation orientation of the phone in radians

     * @return true if the POI is inside the cone, false otherwise
     */
    private boolean geometricCheck(double userX, double userY, double poiX, double poiY, double userOrientation) {

        // Rotaton angle
        double theta;

        // Local variables
        double rotatedX;
        double rotatedY;
        double alpha;
        double beta;

        // Translate the user into the origin
        poiX -= userX;
        poiY -= userY;

        theta = adjustAngle(userOrientation);

        // Rotate the POI around the origin (aka the user) with the rotation matrix.
        // We rotate with an angle of theta in clockwise sense.
        rotatedX = Math.cos(theta)*poiX - Math.sin(theta)*poiY;
        rotatedY = Math.sin(theta)*poiX + Math.cos(theta)*poiY;

        // Use the pois coordinate in the expressions of the sides of the cone,
        // alpha is to the left of the user, beta is to its right.
        alpha = rotatedY - Math.tan(coneWidth/2)*rotatedX;
        beta = rotatedY + Math.tan(coneWidth/2)*rotatedX;

        // Check if the point is inside the cone, sides are valid.
        return (alpha <= 0 && beta >= 0);
    }

    // I seguenti sono METODI per gestire l'actionbar in alto

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wit_list, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
