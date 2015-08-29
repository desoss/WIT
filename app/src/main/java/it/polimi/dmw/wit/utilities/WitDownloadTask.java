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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import it.polimi.dmw.wit.activities.WitFinalResult;
import it.polimi.dmw.wit.activities.WitInfo;
import it.polimi.dmw.wit.activities.WitScan;

public class WitDownloadTask extends AsyncTask<URL, Void, String> {
// Attributi per leggere la risposta HTTP
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
    private String cityUrl;
    public static final int POISLIST = 0, POIDETAIL = 1, WOEID = 2, IMAGECITY = 3, WEATHER = 4, REGISTERVISIT = 5, BESTFIVE = 6, WIKIPEDIATEXT = 7;
    private int c;
    private boolean useCacheJSON = false;
    private Double lat;
    private Double lon;

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
     * @param params array che contiene gli URL da contattare, params[0] e il server con gi�
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
     * Questo viene chiamato quando il thread ha finito e gli viene passato il risultato
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
          case REGISTERVISIT:
              parseJsonFriendsVisit(s);
              break;
          case BESTFIVE:
              parseJsonBestFive(s);
              break;
          case WIKIPEDIATEXT:
              parseJsonWikipediaDescription(s);
              break;
      }
    }

    /**
     * Prende una stringa di input, crea un ArrayList con i POI contenuti nel JSON
     * e fa partire l'activity per mostrare i risultati.
     *
     * NOTA: questo metodo � chiamato del thread che scarica il JSON dopo che ha finito
     * di scaricare, per� viene eseguito dal thread principale dell'applicazione.
     *
     * @param resultJson la stringa contenente il JSON
     */
    public void parseJsonPOIs(String resultJson) {
        /*Struttura Json
            {
                places: [array] o null
                found: value,
                side: value
            } */
        JSONTokener tokener;
        JSONObject documentObject;
        JSONObject place;
        JSONArray places = null;
        JSONArray polygon;
        JSONObject coords;
        float[] x;
        float[] y;
        Double distance;

        // Contiene il numero di monumenti ritornati
        int poisNumber;

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

            // Se l'array dei places non � vuoto
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

                //salvo in due array x e y le coordinate dei punti del poligono di ogni place
                for(int j=0; j<polygon.length();j++){
                    coords = polygon.getJSONObject(j);
                    x[j] = Float.parseFloat(coords.getString("y"));
                    y[j] = Float.parseFloat(coords.getString("x"));
                }
                Double lat = place.getDouble("lat");
                Double lon = place.getDouble("lon");
                distance = useCacheJSON ? distanceBetween2points(this.lat, this.lon,lat,lon) : Double.valueOf(place.getDouble("distance"));

                // Crea un oggetto WitPOI per ogni monumento del JSON
                poiList.add(new WitPOI(
                        place.getInt("id"),
                        place.getString("name"),
                        distance,
                        lat,
                        lon,
                        x,
                        y
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //devo ordinare la lista di object in base alla distanza
        if(useCacheJSON){
            Collections.sort(poiList, new Comparator<WitPOI>() {
                public int compare(WitPOI o1, WitPOI o2) {
                    if (o1.getDistance() == o2.getDistance()) {
                        return 0;
                    } else {
                        return o1.getDistance() > o2.getDistance() ? 1 : -1;
                    }
                }
            });
        }

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
        /* struttura JSON
            {
                title : "",
                description : "",
                photos : [],
                wikipedia link: ""
                ...
            } */

        JSONTokener tokener;
        JSONObject documentObject;
        JSONObject photo = null;
        JSONArray photos;
        String id = null;

        Log.d(LOG_TAG, "JSON received! Length = " + resultJson.length());
        Log.d(LOG_TAG, resultJson);

        // Inizializza il tokener
        tokener = new JSONTokener(resultJson);
        try {
            // Prendi il primo oggetto JSON,
            documentObject = (JSONObject)tokener.nextValue();

            // Prendi i campi di interesse
            title = documentObject.getString("title");
            id = documentObject.getString("id");
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
        finalR.saveResult(id, title, description, wikiLink, photoURL);
    }

    private void parseJsonWoeid(String resultJson){
        /* struttura JSON
            {
                query
                 - results
                   - result
                      - city
                      - county
                      - state
                      - woeid
            } */

        JSONTokener tokener;
        JSONObject object;
        JSONObject query;
        JSONObject results;
        JSONObject result;
        String city;
        String county;
        String state;
        String country;
        String woeid;

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
            country = result.getString("country");
            woeid = result.getString("woeid");
            info = (WitInfo) fragment;
            info.saveInfo(city, county, state, country, woeid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJsonImageCity(String resultJson){
      /* struttura JSON
        {
           responseData
            -results []
              - url
        }  */
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
        } */
        JSONTokener tokener;
        JSONObject object;
        JSONObject query;
        JSONObject results;
        JSONObject channel;
        JSONObject item;
        JSONObject condition;
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

    private void parseJsonFriendsVisit(String resultJson) {

        JSONTokener tokener;
        JSONObject object;
        JSONArray list;
        ArrayList<Long> idsList = new ArrayList<>();

        Log.d(LOG_TAG, "friend visits JSON received! Length = " + resultJson.length());

        tokener = new JSONTokener(resultJson);
        try {
            object = (JSONObject) tokener.nextValue();
            if (!object.isNull("fb_id_list")) {
                list = object.getJSONArray("fb_id_list");
                int l = list.length();
                for (int i = 0; i < l; i++) {
                    Long id = Long.parseLong(String.valueOf(list.get(i)));
                    idsList.add(id);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        finalR = (WitFinalResult) activity;
        finalR.checkIds(idsList);
    }

    public void refreshPOIsList() {
        is = null;
        br = null;
        sb = null;
        line = "";
        poiList = new ArrayList<WitPOI>();
        photoURL = null;
    }

   private void parseJsonBestFive(String resultJson){
       JSONTokener tokener;
       JSONObject object;
       JSONObject pois;
       JSONObject poi;
       int num;
       String name;
       String description;
       String urlImg;
       WitPOI p;
       ArrayList<WitPOI> list = new ArrayList<>();

       Log.d(LOG_TAG, "Best 5 JSON received! Length = " + resultJson.length());

       tokener = new JSONTokener(resultJson);
       try {
           object = (JSONObject) tokener.nextValue();
           num = object.getInt("num");
           if(num>0){
           pois = object.getJSONObject("pois");
           for(int x=1; x<=num;x++){
               poi = pois.getJSONObject(""+x);
               name = poi.getString("name");
               description = poi.getString("description");
               urlImg = poi.getString("photo");
               p = new WitPOI(0,0,name,description,urlImg,0);
               list.add(p);
                 }
           }
           info =  (WitInfo) fragment;
           info.saveBestFive(list);
       }
           catch (JSONException e) {
               e.printStackTrace();
           }
   }

    private void parseJsonWikipediaDescription(String resultJson){
        JSONTokener tokener;
        JSONObject object;
        JSONObject pois;
        JSONObject poi;
        JSONObject page;
        String title;
        String description;
        Log.d(LOG_TAG, "Wikipedia JSON received! Length = " + resultJson.length());

        tokener = new JSONTokener(resultJson);
        try {
            object = (JSONObject) tokener.nextValue();
            pois = object.getJSONObject("query");
            poi = pois.getJSONObject("pages");
            Iterator keys = poi.keys();
            String firstDynamicKey = (String)keys.next();
            page = poi.getJSONObject(firstDynamicKey);
            description = page.getString("extract");
            title = page.getString("title");
            finalR = (WitFinalResult) activity;
            finalR.setDescription(description, title);
        }
        catch (JSONException e) {
            e.printStackTrace();

        }
    }

    public void setUseCacheJSON(){
        useCacheJSON = true;
    }

    public void setLat(Double lat){
        this.lat = lat;
    }

    public void setLon(Double lon){
        this.lon = lon;
    }

    private Double distanceBetween2points(Double lat1, Double lon1, Double lat2, Double lon2){
        Double distance;
        Double theta = lon1 - lon2;
        distance = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        distance = Math.acos(distance);
        distance = Math.toDegrees(distance);
        distance = distance * 60 * 1.1515* 1.609344 * 1000;
        return distance;
    }
}

