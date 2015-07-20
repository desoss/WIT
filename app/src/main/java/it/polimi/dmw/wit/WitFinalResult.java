package it.polimi.dmw.wit;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.widget.ShareButton;
import com.facebook.share.widget.ShareDialog;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Story;
import com.sromku.simple.fb.listeners.OnCreateStoryObject;
import com.sromku.simple.fb.listeners.OnPublishListener;
import com.sromku.simple.fb.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import it.polimi.dmw.wit.Polygon.Point;
import it.polimi.dmw.wit.Polygon.Polygon;
import it.polimi.dmw.wit.database.DbAdapter;


/**
 * Activity per gestire e visualizzare la lista dei risultati
 */
public class WitFinalResult extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {


    private SimpleFacebook mSimpleFacebook;


    private final static String LOG_TAG = "WitFinalResult";

    /**
     * Ampiezza del cono di visione, per adesso ho fatto un paio di prove,
     * andrebbe verificata
     */
    private final static double coneWidth = Math.PI/4;

    /**
     * Membri per gestire le View
     */
    private ImageView mainImage;
    private TextView titleText;
    private TextView descText;

    private String title;
    private String description;

    private URL photoURL;
    private byte[] img=null;
    private boolean imgExists = true;

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;



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
     * Gestore del database
     */
    private DbAdapter dbAdapter;
    private Cursor cursor;


    private String wikiLink;

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
            ByteArrayOutputStream bos=new ByteArrayOutputStream();
            result.compress(Bitmap.CompressFormat.PNG, 100, bos);
            img=bos.toByteArray();
            savePOI(title,description,img); //salvo nel database il poi
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
                photos : [],
                wikipedia link: ""
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
            if(documentObject.getString("description")!=null) {
                description = documentObject.getString("description");
            }

           try{
                wikiLink = documentObject.getString("wikipedia"); }
           catch (JSONException e){
               Log.d(LOG_TAG,"No link a Wikipedia");
           }


            titleText.setText(title);
            descText.setText(description);


            // Se l'array dei places non è vuoto
            if (!documentObject.isNull("photos")) {
                // Prendi l'array delle photo
                photos = documentObject.getJSONArray("photos");
                if(photos.length()>0){
                photo = photos.getJSONObject(0);}

                if (photo != null) {
                    photoURL = new URL(photo.getString("960_url"));
                    imgExists = true;
                    new DownloadImageTask().execute(photoURL);
                }
                else{
                    imgExists = false;
                    savePOI(title,description,img); //salvo nel database il poi
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        checkSettingFb();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wit_detail);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_wit_list);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);

        mSimpleFacebook = SimpleFacebook.getInstance(this);

        configurationSimpleFacebook();



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
        int h=0;
        // TODO rimettere filtering
        for (WitPOI poi : poiList) {
            //if (geometricCheck(userLongitude, userLatitude, poi.getPoiLon(), poi.getPoiLat(),userOrientation)) {
           h++;
            Log.d(LOG_TAG, poi.getPoiName()+" "+h);
            if (polygonCheck(userLatitude, userLongitude, poi.getX(), poi.getY(),userOrientation)){
                correctPoiList.add(poi);
                Log.d(LOG_TAG, "POI aggiunto: "+poi.getPoiName());
            }
        }

        if (correctPoiList.size()>0) {
            try {
                URL detailUrl = new URL("http://api.wikimapia.org/?key=example&function=place.getbyid&id=" + correctPoiList.get(0).getPoiId() + "&format=json&language=it");

                new WitDownloadTask().execute(detailUrl);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if(!imgExists){
                Log.d(LOG_TAG, "Risultato senza immagine ");
                mainImage.setVisibility(View.GONE);
            }
       }
       else {
           Drawable sadFace = getResources().getDrawable(R.drawable.sadface);
           mainImage.setImageDrawable(sadFace);
           titleText.setText(title);
           descText.setText(description);
       }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSimpleFacebook = SimpleFacebook.getInstance(this);
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


    private boolean polygonCheck(double userX, double userY, float[] poiX, float[] poiY, double userOrientation){
        double lat;
        double lon;
        double x = Math.toRadians(userX);
        double y = Math.toRadians(userY);
        float earthR = (float)6371.009 ; //raggio terrestre approssimato
        Polygon polygon;
        Polygon.Builder builder = new Polygon.Builder();
      //  Log.d(LOG_TAG, "Lat long iniziali"+userX+" , "+userY);
       // Log.d(LOG_TAG, "O: "+userOrientation+" "+earthR);


        for(int i=0; i<poiX.length; i++){
            builder.addVertex(new Point(poiX[i],poiY[i]));
           // Log.d(LOG_TAG,poiX[i]+" , "+poiY[i]);

        }
        polygon=builder.build();
        //elimino un posto se ci sono dentro
        if(polygon.contains(new Point((float)userX,(float)userY))){
            Log.d(LOG_TAG, "ci sono dentro lo scarto");
            return false;

        }
        //prendo 500 campioni a distanza 1m partendo dalla posizione dell'utente in direzione data da userOrientation
        for(int j=1; j<=500; j++){
            float d = (float)(j*0.001); //converto i metri in km
            // formule per trovare lat e lon dato un punto, la distanza e l'angolo http://www.movable-type.co.uk/scripts/latlong.html
            lat = (Math.asin( Math.sin(x)*Math.cos(d/earthR) + Math.cos(x)*Math.sin(d/earthR)*Math.cos(userOrientation)));
            lon = (y + Math.atan2(Math.sin(userOrientation)*Math.sin(d/earthR)*Math.cos(x), Math.cos(d/earthR)-Math.sin(x)*Math.sin(lat)));
            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);
          //  Log.d(LOG_TAG, "lat: "+lat+"lon: "+lon);
            if(polygon.contains(new Point((float)lat,(float)lon))){
                return true;
            }
        }
       Log.d(LOG_TAG, "-------------------------");
         return false;
        }



        /**
     * Metodo per salvare il POI trovato
     * @param name
     * @param description
     */


    private void savePOI(String name, String description, byte[] img){
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        dbAdapter.savePOI(name, description, getCurrentDate(), img);

        Cursor cursor = dbAdapter.fetchAllPOIs();
        String n;

    /*    while ( cursor.moveToNext() ) {
            n = cursor.getString( cursor.getColumnIndex(DbAdapter.KEY_NAME) );
            Log.d(LOG_TAG, "nome POI = " + n);
        } */
        cursor.close();


        dbAdapter.close();

    }

    private String getCurrentDate(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String date = df.format(c.getTime());
        return date;
    }



    private void configurationSimpleFacebook(){
        Permission[] permissions = new Permission[] {
                Permission.USER_PHOTOS,
                Permission.PUBLISH_ACTION,
        };
        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId("747369465380873")
                .setNamespace("wit_discover")
                .setPermissions(permissions)
                .build();
        SimpleFacebook.setConfiguration(configuration);

    }
    OnPublishListener onPublishListener = new OnPublishListener() {
        @Override
        public void onComplete(String id) {
            Log.d(LOG_TAG, "Published successfully. id = " + id);
        }
    };

    private void checkSettingFb(){
        SharedPreferences sharedPrefs = getSharedPreferences("WIT", MODE_PRIVATE);
        if(sharedPrefs.getBoolean("fb", false)){
            storyOnFacebook();
        }

    }



    private void storyOnFacebook(){
        String p;
        URL u = null;
        try {
            u = new URL("http://uxrepo.com/static/icon-sets/open-maps/svg/monument.svg");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if(photoURL!=null){
            p = photoURL.toString();
        }
        else {
            p = u.toString();
        }

        Story.StoryObject storyObject = new Story.StoryObject.Builder()
                .setNoun("Monument")
                .setTitle(title)
                .setImage(p)
                .setDescription(description)
                .build();


        mSimpleFacebook.create(storyObject, new OnCreateStoryObject() {
            public void onComplete(String response) {


                Story.StoryObject so = new Story.StoryObject.Builder()
                        .setId(response)
                        .setNoun("Monument")
                        .build();

                Story.StoryAction storyAction = new Story.StoryAction.Builder()
                        .setAction("Discover")
                        .build();

                Story story = new Story.Builder()
                        .setObject(so)
                        .setAction(storyAction)
                        .build();


                mSimpleFacebook.publish(story, onPublishListener);

            }
        });
        Log.d(LOG_TAG, "Pubblicato post su FB");








   /*     mCallbackManager = CallbackManager.Factory.create();
        ShareOpenGraphObject object = new ShareOpenGraphObject.Builder()
                .putString("og:type", "monuments.monument")
                .putString("og:title", title)
                .build();

        ShareOpenGraphAction action = new ShareOpenGraphAction.Builder()
                .setActionType("monument.discover")
                .putObject("monument", object)
                .build();

        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setPreviewPropertyName("discover")
                .setAction(action)
                .build();

        //ShareDialog.show(this, content);



       ShareButton shareButton = (ShareButton)findViewById(R.id.share_button);
        shareButton.setShareContent(content);
        shareButton.registerCallback(mCallbackManager, new FacebookCallback<Sharer.Result>() {

            @Override
            public void onSuccess(Sharer.Result result) {
                Log.i(LOG_TAG, "SHARING SUCCESS!");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(LOG_TAG, "SHARING ERROR! - " + error.getMessage());
            }

            @Override
            public void onCancel() {
                Log.w(LOG_TAG, "SHARING CANCEL!");
            }
        });  */

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
