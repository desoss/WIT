package it.polimi.dmw.wit.utilities;


import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

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

import it.polimi.dmw.wit.activities.WitFinalResult;
import it.polimi.dmw.wit.activities.WitInfo;
import it.polimi.dmw.wit.activities.WitMainActivity;
import it.polimi.dmw.wit.activities.WitScan;

public class WitDownloadTask extends AsyncTask<URL, Void, String> {
    // Membri per leggere la risposta HTTP
    private InputStream is;
    private BufferedReader br;
    private StringBuilder sb;
    private String line;
    ArrayList<WitPOI> poiList;
    private final static String LOG_TAG = "WitDownloadTask";
    Activity activity;
    Fragment fragment;
    WitInfo info;
    WitScan scan;
    WitFinalResult finalR;
    private String title;
    private String description;
    private String wikiLink;
    private URL photoURL;
    private String city;
    private String county;
    private String state;
    private String woeid;
    private String cityUrl;
    public static final int POISLIST = 0, POIDETAIL = 1, WOEID = 2, IMAGECITY = 3, WEATHER = 4;
    private int c;




    public WitDownloadTask(Activity activity, Fragment fragment,int c) {
        is = null;
        br = null;
        sb = null;
        line = "";
        poiList = new ArrayList<WitPOI>();
        this.activity = activity;
        this.fragment = fragment;
        photoURL = null;
        this.c = c;

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

      switch (c){
          case POISLIST:
              parseJsonPOIs(s);
              break;
          case POIDETAIL:
              parseJsonDetail(s);
              break;
          case WOEID:
              parseJsonWoeid(s);
              break;
          case IMAGECITY:
              parseJsonImageCity(s);
              break;
          case WEATHER:
              parseJsonWeather(s);
              break;
      }

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
    private void parseJsonPOIs(String resultJson) {
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
        //mainA = (WitMainActivity) activity;
        scan =  (WitScan) fragment;
        scan.startfinalResult(poiList);

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
                photos : [],
                wikipedia link: ""
                ...
            }


         */

        JSONTokener tokener = null;
        JSONObject documentObject = null;
        JSONObject photo = null;
        JSONArray photos = null;

        Log.d(LOG_TAG, "JSON received! Length = " + resultJson.length());
        Log.d(LOG_TAG, resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            // Prendi il primo oggetto JSON,
            documentObject = (JSONObject)tokener.nextValue();

            // Prendi i campi di interesse
            title = documentObject.getString("title");
            if(documentObject.getString("description")!=null) {
                description = documentObject.getString("description");
            }

            try{
                wikiLink = documentObject.getString("wikipedia"); }
            catch (JSONException e){
                Log.d(LOG_TAG,"No link a Wikipedia");
            }

            if (!documentObject.isNull("photos")) {
                // Prendi l'array delle photo
                photos = documentObject.getJSONArray("photos");
                if(photos.length()>0){
                    photo = photos.getJSONObject(0);}

                if (photo != null) {
                    photoURL = new URL(photo.getString("960_url"));
                }


            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        finalR = (WitFinalResult) activity;
        finalR.saveResult(title, description, wikiLink, photoURL);

    }

    private void parseJsonWoeid(String resultJson){
        /*
            struttura JSON

            {
                query
                 - results
                   - result
                      - city
                      - county
                      - state
                      - woeid
            }


         */

        JSONTokener tokener = null;
        JSONObject object = null;
        JSONObject query = null;
        JSONObject results = null;
        JSONObject result = null;

        Log.d(LOG_TAG, "JSON received! Length = " + resultJson.length());
        Log.d(LOG_TAG,resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            // Prendi il primo oggetto JSON,
            object = (JSONObject)tokener.nextValue();
            query = object.getJSONObject("query");
            results = query.getJSONObject("results");
            result = results.getJSONObject("Result");


            // Prendi i campi di interesse
            city = result.getString("city");
            county = result.getString("county");
            state = result.getString("state");
            woeid = result.getString("woeid");
            info =  (WitInfo) fragment;
            info.saveInfo(city, county, state, woeid);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJsonImageCity(String resultJson){
      /*
        struttura JSON

        {
           responseData
            -results []
              - url
        }

        */
        JSONTokener tokener = null;
        JSONObject object = null;
        JSONObject responseData = null;
        JSONArray results = null;
        JSONObject result = null;


        Log.d(LOG_TAG, "JSON received! Length = " + resultJson.length());
        Log.d(LOG_TAG,resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            object = (JSONObject)tokener.nextValue();
            responseData = object.getJSONObject("responseData");
            results = responseData.getJSONArray("results");


                result = results.getJSONObject(0);
                cityUrl = result.getString("url");

            info =  (WitInfo) fragment;
            info.saveImageCityUrl(cityUrl);




        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJsonWeather(String resultJson) {
       /* struttura JSON

        {
            responseData
                    -results []
            - url
        }

        */
        JSONTokener tokener = null;
        JSONObject object = null;
        JSONObject query = null;
        JSONObject results = null;
        JSONObject channel = null;
        JSONObject item = null;
        JSONObject condition = null;
        String temp;
        String code;
        String text;

        Log.d(LOG_TAG, "JSON received! Length = " + resultJson.length());
        Log.d(LOG_TAG, resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            object = (JSONObject) tokener.nextValue();
            query = object.getJSONObject("query");
            results = query.getJSONObject("results");
            channel = results.getJSONObject("channel");
            item = channel.getJSONObject("item");
            condition = item.getJSONObject("condition");
            temp = condition.getString("temp");
            code = condition.getString("code");
            text = condition.getString("text");

            info =  (WitInfo) fragment;
            info.saveWeather(code, temp, text);



        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

}

