package it.polimi.dmw.wit.utilities;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import it.polimi.dmw.wit.R;


//Saves a json with a big list of monuments in the cache. Save the information to be checked in the main activity
//about this json in SharedPreferences.
public class BackgroundService extends IntentService {
    private final static String LOG_TAG = "WitBackgroundService";
    public static final double lat_OneKm  = 0.008991; //per aggiungere/sottrarre 1 km a una latitudine uso questa quantità
    public static final double lon_OneKm  = 0.012826; //per aggiungere/sottrarre 1 km a una longitudine uso questa quantità

    // Attributi per leggere la risposta HTTP
    private InputStream is;
    private BufferedReader br;
    private StringBuilder sb;
    private String line;

    private String lat;
    private String lon;

    private Double internalSideLength; //internalSquare side Length

    //define an inside square in the bigger one. It is used in the main activity to check if the current
    //location is valid. If the current location is inside this square so the json downloaded by this service is valid.
    private Double latMax;
    private Double lonMax;
    private Double latMin;
    private Double lonMin;


    //Creates an IntentService.  Invoked by your subclass's constructor.
    public BackgroundService() {
        super("IntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Extract the receiver passed into the service
        ResultReceiver rec = intent.getParcelableExtra("receiver");

        // Extract additional values from the bundle
        lat = intent.getStringExtra("lat");
        lon = intent.getStringExtra("lon");
        String range = intent.getStringExtra("range"); //side of the square
        String max = intent.getStringExtra("max"); //max number of POIs huge value

        String getParameters = "?lat="+lat+"&lon="+lon+"&side="+range+"&max="+max+"&json=true&cachejson=true";

        try {
            URL url = new URL(getString(R.string.get_monuments_base_url)+getParameters);
            // Crea la connessione HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(20000 /* milliseconds */);
            conn.setConnectTimeout(25000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            Log.d(LOG_TAG, "connected to " + url);

            // Riceve la risposta
            int response = conn.getResponseCode();

            // Logga il codice HTTP (200, 404, etc.)
            Log.d(LOG_TAG, "HTTP CODE: " + String.valueOf(response));

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
        String val = sb.toString();

        Log.d(LOG_TAG, "JSON received! Length = " + val.length());

        // Save the String in cache Internal Memory
        Writer out;

        File cacheFile = new File(((Context)this).getCacheDir(), "cacheFile.srl");
        if (!cacheFile.exists()) {
                try {
                    cacheFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        try {
            cacheFile.createNewFile();
            out = new OutputStreamWriter(new FileOutputStream(cacheFile), "UTF8");
            out.write(val);
            out.flush();
            out.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

        //Saves information to be checked in the main activity before using this json
        try {
            setInternalSideLength(val);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveInfoToBeChecked();
    }

    //information to be checked before using the saved json
    private void saveInfoToBeChecked(){
        SharedPreferences prefs = getSharedPreferences("bigSquareMonumentList", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        calculateInternalSquare();

        //anzichè salvarli come stringhe (con editor.putString("max_lon", lonMax);)
        // che sarebbero più lente da rinconvertire in double (visto che come double
        //non possono essere salvati in sharedPreferences),
        //le salvo come RawLongBits (con metodo http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences )
        putDouble(editor, "max_lat", latMax);
        putDouble(editor, "max_lon", lonMax);
        putDouble(editor, "min_lat", latMin);
        putDouble(editor, "min_lon", lonMin);

        editor.apply(); //!commit, apply is asynch
    }

    private void calculateInternalSquare(){
        double lat;
        double lon;
        double latMin;
        double lonMin;
        double latMax;
        double lonMax;

        //from string to double
        lat = Double.parseDouble(this.lat);
        lon = Double.parseDouble(this.lon);

        latMin = lat - lat_OneKm * internalSideLength/2;
        latMax = lat + lat_OneKm * internalSideLength/2;
        lonMin = lon - lon_OneKm * internalSideLength/2;
        lonMax = lon + lon_OneKm * internalSideLength/2;

        //sistema i nuovi valori della latitudine nel caso sforino da -90,90
        if(latMax > 90){
            latMax = -180 + latMax; //-90 + ($lat-90)
        }
        if(latMin < -90){
            latMin = 180 + latMin; //90 + ($lat+90), il valore tra parentesi è negativo
        }
        //sistema i nuovi valori della longitudine nel caso sforino da -180,180
        if(lonMax > 180){
            lonMax = -360 + lonMax; //-180 + ($lat-180)
        }
        if(lonMin < -180){
            lonMin = 360 + lonMin; //180 + ($lat+180), il valore tra parentesi è negativo
        }

        //approssimo i valori al sesto decimale con metodo da http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places e altre
        this.latMin = round(latMin,6);
        this.lonMin = round(lonMin,6);
        this.latMax = round(latMax,6);
        this.lonMax = round(lonMax,6);
    }

    private void setInternalSideLength(String jsonString) throws JSONException {
        JSONObject jsonObject = new JSONObject(jsonString);
        String tmp = jsonObject.getString("side") ;
        Double bigSideLength = Double.parseDouble(tmp);
        internalSideLength = bigSideLength - 1;  //<---- correggere il meno 1 con il default valore della max distanza a cui può trovarsi un POI

    }

    //http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}

