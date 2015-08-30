package it.polimi.dmw.wit.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pkmmte.view.CircularImageView;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.utilities.ObjectSerializer;
import it.polimi.dmw.wit.utilities.WitDownloadImageTask;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitLocationAPI;
import it.polimi.dmw.wit.utilities.WitLocationProvider;
import it.polimi.dmw.wit.utilities.WitPOI;
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
    private String city,  county,  state, country, woeid, imageCityUrl, code, temp, text;
    private WitDownloadImageTask witDownloadImageTask;
    private ImageView mainImage, weatherImage;
    private TextView titleText, weatherText;
    private ProgressWheel progressWheel;
    private DbAdapter dbAdapter;
    private Cursor cursor;
    private Double latMax;
    private Double lonMax;
    private Double latMin;
    private Double lonMin;
    private boolean BiggerSquareUsable = true;
    private ArrayList<byte[]> imgList;
    private ArrayList<WitPOI> poisList;
    private ListView listView ;
    private Intent intent;
    public final static String EXTRA_POI= "it.polimi.dmw.wit.POI";
    public final static String EXTRA_B5= "it.polimi.dmw.wit.B5";
    private CustomAdapter adapter;
    private double lat, lon;
    ArrayList<Double> distList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
         v = inflater.inflate(R.layout.tab_info,container,false);
        progressWheel = (ProgressWheel) v.findViewById(R.id.progress_wheel);
        listView = (ListView) v.findViewById(R.id.listView);
        View header = getActivity().getLayoutInflater().inflate(R.layout.header_info, null);
        listView.addHeaderView(header);
        mainImage = (ImageView)v.findViewById(R.id.city_img);
        weatherImage = (ImageView)v.findViewById(R.id.weather_img);
        titleText = (TextView)v.findViewById(R.id.city_name_text);
        weatherText = (TextView)v.findViewById(R.id.weather_text);



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

        imgList = new ArrayList<>();

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
        if (networkInfo != null && networkInfo.isConnected() && gpsEnabled) {

            // Fa partire il timeout thread che a sua volta fare il check periodico della Location
            WitTimeoutThread timeoutThread = new WitTimeoutThread(new TimeoutHandler());
            timeoutThread.start();
        } else {
            // display error
          //  showWirelessSettingsAlert();

        }
        poisList = new ArrayList<>();
        distList = new ArrayList<>();
        adapter = new CustomAdapter(getActivity(),poisList, imgList);
        listView.setAdapter(adapter);
        intent = new Intent(getActivity(), WitSavedPOI.class);



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

        Toast.makeText(getActivity(), "" + currentLocation.getLatitude() + " " + currentLocation.getLongitude(), Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG,"D si");
            progressWheel = new ProgressWheel(getActivity());
            progressWheel.spin();
            currentLocation = locationProvider.getLocation();
            String lat = String.valueOf(currentLocation.getLatitude());
            String lon = String.valueOf(currentLocation.getLongitude());
            final String url = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20geo.placefinder%20where%20text%3D%22" + lat + "%2C%20" + lon + "%22%20and%20gflags%3D%22R%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
            getWoeid(url);
        }



    private void setWeatherSaved(){
        v.findViewById(R.id.progress_wheel).setVisibility(View.GONE);
        Log.d(LOG_TAG,"D no");
        SharedPreferences prefs = getActivity().getSharedPreferences("WEATHER", Context.MODE_PRIVATE);
        code = prefs.getString("code", "0");
        temp =  prefs.getString("temp", "0");
        text =  prefs.getString("text", "0");
        setImageWeather();


    }

    private void getSquarePosition(){
        Activity a = getActivity();
        SharedPreferences prefs = a.getSharedPreferences("bigSquareMonumentList", Context.MODE_PRIVATE);
        //uso il metodo definito da me altrimenti avrei dovuto usare stringhe (prefs.getString("max_lat",""); )
        latMax = getDouble(prefs, "max_lat");
        latMin = getDouble(prefs, "min_lat");
        lonMax = getDouble(prefs, "max_lon");
        lonMin = getDouble(prefs, "min_lon");
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

    private void getWoeid(String serverUrl) {

        Log.d(LOG_TAG, "SERVER URL: " + serverUrl);


        try {
            URL url = new URL(serverUrl);

            // WitDownloadTask � la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.WOEID);
            witDownloadTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "URL Fail: " + serverUrl);

        }
    }

    public void saveInfo(String city, String county, String state, String country, String woeid){
        this.city = city;
        this.county = county;
        this.state = state;
        this.country = country;
        this.woeid = woeid;
        boolean b = checkWoeid();
        SharedPreferences prefs = getActivity().getSharedPreferences("LastLocation", getActivity().MODE_PRIVATE);
        String currentLocation = city+county+state+country;
        String location = prefs.getString("location","");
        String currentDate = getCurrentDate();
        String date = prefs.getString("date", getCurrentDate());
        Long compare = compareDates(currentDate,date);
        Log.d(LOG_TAG,"nuova: "+currentLocation+" vecchia: "+location+" compare date: "+compare);
        if(!currentLocation.equalsIgnoreCase(location)||compare>1) {
            getWeather();
            getBestFive();
            Log.d(LOG_TAG,"Uso info nuove");
        }
        else{
            setWeatherSaved();
                readFromDbBestFive();
            Log.d(LOG_TAG,"Uso info salvate");

        }
            if (!b) {
                searchImageCity();
            }
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("LastLocation",getActivity().MODE_PRIVATE).edit();
        editor.putString("location", currentLocation);
        editor.putString("date",getCurrentDate());
        editor.commit();

    }

    private void readFromDbBestFive(){
        if(poisList.size()==0) {
            dbAdapter = new DbAdapter(getActivity());
            dbAdapter.open();
            cursor = dbAdapter.fetchBestFive();
            String name, description;
            double lat, lon;
            byte[] img;
            int id;
            WitPOI p;
            while (cursor.moveToNext()) {
                img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_IMAGE));
                imgList.add(img);
                name = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_NAME));
                description = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_DESCRIPTION));
                lat = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LAT));
                lon = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LON));
                id = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_WIKIMAPIAID));
                p = new WitPOI(0, id, name, description, null, lat, lon,0);
                poisList.add(p);
                double dist = distanceBetween2points(lat,lon,currentLocation.getLatitude(),currentLocation.getLongitude())/1000;
                distList.add((dist));
            }
            cursor.close();
            dbAdapter.close();
        }
        adapter.notifyDataSetChanged();


    }


    private void getBestFive(){
        final String u = "http://desoss.altervista.org/wit/android_best5pois_request.php?city="+city+"&county="+county+"&state="+state+"&country="+country;
        //final String u = "http://desoss.altervista.org/wit/android_best5pois_request.php?&city=2&county=3&state=3&country=234";
        Log.d(LOG_TAG, "SERVER URL: " + u);
        try {
            URL url = new URL(u);
            // WitDownloadTask � la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.BESTFIVE);
            witDownloadTask.execute(url);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "URL Fail: " + u);
        }
    }

    public void saveBestFive(ArrayList<WitPOI> l, ArrayList<Double> d){
        String url = null;
        distList = d;
        for(int x=0;x<l.size();x++){
            poisList.add(l.get(x));
            url = poisList.get(x).getDate();
            if(url!=null){
                downloadImagePoi(url);
            }
            else{
                imgList.add(null);
            }
        }
        if(imgList.size()==poisList.size()){
            adapter.notifyDataSetChanged();
            saveInDbBestFive();

        }

    }

    private void saveInDbBestFive(){
        dbAdapter = new DbAdapter(getActivity());
        dbAdapter.open();
        dbAdapter.deleteBestFive();
        String name, description;
        double lat, lon;
        byte [] img;
        int id;
        for(int x =0;x<poisList.size();x++){
            name = poisList.get(x).getPoiName();
            description = poisList.get(x).getDescription();
            id = poisList.get(x).getWikimapiaId();
            lat = poisList.get(x).getPoiLat();
            lon = poisList.get(x).getPoiLon();
            img = imgList.get(x);
            dbAdapter.saveBestFive(id, name,description,lat,lon,img);
        }
        dbAdapter.close();
    }

    /*

    private void saveInCacheBestPois(){

        Writer out;
        ObjectSerializer objectSerializer = new ObjectSerializer();


        File cacheFile = new File((getActivity()).getCacheDir(), "cacheBestPois.srl");
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
            out.write( objectSerializer.serialize(poisList));
            out.flush();
            out.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
        cacheFile = new File((getActivity()).getCacheDir(), "cacheBestPoisImg.srl");
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
            out.write( objectSerializer.serialize(imgList));
            out.flush();
            out.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromCacheBestFive() throws IOException {
    InputStreamReader in;
    StringBuilder responseStrBuilder = new StringBuilder();
    ObjectSerializer objectSerializer = new ObjectSerializer();


        File cacheFile = new File(( getActivity()).getCacheDir(), "cacheBestPois.srl");
    if (cacheFile.exists()) {//se il file non esiste ritorna null

        in = new InputStreamReader(new FileInputStream(cacheFile), "UTF-8");
        BufferedReader buffReader = new BufferedReader(in);

        String inputStr;
        while ((inputStr = buffReader.readLine()) != null)
            responseStrBuilder.append(inputStr);

        in.close();
        String result = responseStrBuilder.toString();
        try {
            poisList = (ArrayList<WitPOI>)objectSerializer.deserialize(result);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



        cacheFile = new File(( getActivity()).getCacheDir(), "cacheBestPoisImg.srl");
        if (cacheFile.exists()) {//se il file non esiste ritorna null

            in = new InputStreamReader(new FileInputStream(cacheFile), "UTF-8");
            BufferedReader buffReader = new BufferedReader(in);

            String inputStr;
            while ((inputStr = buffReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            in.close();
            String result = responseStrBuilder.toString();
            try {
                imgList = (ArrayList<byte[]>)objectSerializer.deserialize(result);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();

    }
*/


    private void downloadImagePoi(String u){
        Log.d(LOG_TAG, "SERVER URL: " + u);
        try {
            URL url = new URL(u);
            witDownloadImageTask = new WitDownloadImageTask(null, this, witDownloadImageTask.POIBEST);
            witDownloadImageTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG,"URL Fail: "+u);
        }
    }

    public void saveImagePoi(byte[] img){
        imgList.add(img);
        if(imgList.size()==poisList.size()){
            adapter.notifyDataSetChanged();
            saveInDbBestFive();
        }
        Log.d(LOG_TAG, "" + imgList.size());

    }
    //  salvo il woeid corrente nel database automatico
    private void saveCurrentWoeid(int woeid){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("WIT",getActivity().MODE_PRIVATE).edit();
        editor.putInt("woeid", woeid);
        editor.commit();
    }


    private boolean checkWoeid() {
        dbAdapter = new DbAdapter(getActivity());
        dbAdapter.open();
        cursor = dbAdapter.fetchCityByCCSC(city, county, state, country);
        while (cursor.moveToNext()) {
            Log.d(LOG_TAG,"Uso info city salvate");
            v.findViewById(R.id.progress_wheel).setVisibility(View.GONE);
                titleText.setText(city);
                byte[] img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_IMAGE));
                int woeid = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));
                saveCurrentWoeid(woeid);
                if (img != null) {
                    mainImage.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
                }
                return true;

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
            // WitDownloadTask � la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.IMAGECITY);
            witDownloadTask.execute(url);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "URL Fail: " + u);


        }

    }

    public void saveImageCityUrl(String u){
        imageCityUrl = u;
        Log.d(LOG_TAG,"imagecityurl: "+u);
        if(u!=null) {
            downloadImageCity();
        }

    }

    private void downloadImageCity() {
        Log.d(LOG_TAG, "SERVER URL: " + imageCityUrl);

        try {
            URL url = new URL(imageCityUrl);
            // WitDownloadTask � la classe che gestisce il download
            witDownloadImageTask = new WitDownloadImageTask(null, this, witDownloadImageTask.CITY);
            witDownloadImageTask.execute(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "URL Fail: " + imageCityUrl);

        }
    }

    public void setImageCity(Bitmap result, byte[] img) {
        //progressWheel.setVisibility(View.INVISIBLE);
        v.findViewById(R.id.progress_wheel).setVisibility(View.GONE);
        mainImage.setImageBitmap(result);
        titleText.setText(city);
        setImageWeather();
        saveCityInfo(img);
    }

    private void getWeather(){
        final String u = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20("+woeid+")%20and%20u%3D'c'&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";
        Log.d(LOG_TAG, "SERVER URL: " + u);
        try {
            URL url = new URL(u);
            // WitDownloadTask � la classe che gestisce il download
            witDownloadTask = new WitDownloadTask(null, this, witDownloadTask.WEATHER);
            witDownloadTask.execute(url);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "URL Fail: " + u);

        }
    }

    public void saveWeather(String code, String temp, String text){
        this.code = code;
        this.temp = temp;
        this.text = text;
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("WEATHER",getActivity().MODE_PRIVATE).edit();
        editor.putString("code", code);
        editor.putString("temp", temp);
        editor.putString("text", text);
        editor.commit();
        setImageWeather();

    }

    private void setImageWeather(){
        if(code.equalsIgnoreCase("0")){
            code = "00";
        }
        String uri = "@drawable/a"+code;
        Log.d(LOG_TAG, uri);
        Log.d(LOG_TAG,getActivity().getPackageName());

        int imageResource = getResources().getIdentifier(uri, null,  getActivity().getPackageName());

        Drawable res = getResources().getDrawable(imageResource);

        weatherImage.setImageDrawable(res);
        String w = temp +"c, "+text;
        weatherText.setText(w);


    }

    private void saveCityInfo(byte[] img){
        if(img!=null){
            Log.d(LOG_TAG,"NOT NULLLL");
        }
        dbAdapter = new DbAdapter(getActivity());
        dbAdapter.open();
        dbAdapter.saveCityInfo(city, county, state, country, img);
        cursor = dbAdapter.fetchCityByCCSC(city, county, state, country);
        while (cursor.moveToNext()) {
            int woeid = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));
            saveCurrentWoeid(woeid);
        }
        cursor.close();
        dbAdapter.close();
    }




    /**
     * Classe privata che gestisce la lista
     */
    private class CustomAdapter extends BaseAdapter {
        Context context;
        WitPOI poi;
        ArrayList<WitPOI> poisList;
        ArrayList<byte[]> imagesList;
        private LayoutInflater inflater=null;
        public CustomAdapter(Activity activity, ArrayList<WitPOI> p, ArrayList<byte[]> l) {
            // TODO Auto-generated constructor stub
            imagesList = l;
            poisList = p;
            context=activity;
            inflater = ( LayoutInflater )context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return poisList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public class Holder
        {
            TextView tv;
            CircularImageView img;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Holder holder=new Holder();
            View rowView;
            rowView = inflater.inflate(R.layout.pois_list, null);
            holder.tv=(TextView) rowView.findViewById(R.id.textView);
            holder.img=(CircularImageView) rowView.findViewById(R.id.img);
            holder.img.setBorderColor(getResources().getColor(R.color.colorPrimary));
            holder.img.setBorderWidth(4);
            // circularImageView.setSelectorColor(getResources().getColor(R.color.colorPrimary));
            //circularImageView.setSelectorStrokeColor(getResources().getColor(R.color.colorPrimaryDark));
            holder.img.setSelectorStrokeWidth(10);
            holder.img.addShadow();
            poi = poisList.get(position);
            String dist = String.format("%.2f", distList.get(position));
            holder.tv.setText(poi.getPoiName()+" - "+dist+" km");
            //holder.img.setImageResource(imageId[position]);
            byte[] image = imagesList.get(position);
            if(image!= null) {
                holder.img.setImageBitmap(BitmapFactory.decodeByteArray(image, 0,image.length));
            }
            else {
                holder.img.setImageResource(R.drawable.gray);


            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    poi = poisList.get(position);
                    intent.putExtra(EXTRA_POI, poi);
                    intent.putExtra(EXTRA_B5,1);
                    startActivity(intent);



                    //Log.d( LOG_TAG,"nome POI = "+ result[position]+ id[position]);

                }
            });
            return rowView;
        }

    }







    /* Mostra un dialog per attivare il GPS
    * qui cancel chiude l'applicazione
            */
    private void showGPSSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("Unable to connect");

        // Setting Dialog Message
        alertDialog.setMessage("You need a GPS connection to use this app. Please turn on GPS in Settings");

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

    private String getCurrentDate(){
        GregorianCalendar c = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        String date = df.format(c.getTime());
        /*Date d = null;
        try {
            d = df.parse("11-nov-2015 12:14:14");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String date = df.format(d);
        */

        return date;
    }

    private Long compareDates(String s1, String s2) {
        Date d1 = null;
        Date d2 = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        try {
            d1 = sdf.parse(s1);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            d2 = sdf.parse(s2);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        TimeUnit tu = TimeUnit.HOURS;
        long diffInMillies = d1.getTime() - d2.getTime();
        Long r = tu.convert(diffInMillies, TimeUnit.MILLISECONDS);
        Log.d(LOG_TAG, "Ris: " + r);
        return r;

    }

    private double distanceBetween2points(double lat1, double lon1, double lat2, double lon2){
        double distance;
        double theta = lon1 - lon2;
        distance = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        distance = Math.acos(distance);
        distance = Math.toDegrees(distance);
        distance = distance * 60 * 1.1515* 1.609344 * 1000;
        return distance;
    }






}