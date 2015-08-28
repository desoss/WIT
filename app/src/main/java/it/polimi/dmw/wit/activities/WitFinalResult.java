package it.polimi.dmw.wit.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pkmmte.view.CircularImageView;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Story;
import com.sromku.simple.fb.listeners.OnCreateStoryObject;
import com.sromku.simple.fb.listeners.OnFriendsListener;
import com.sromku.simple.fb.listeners.OnPublishListener;


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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.Polygon.Point;
import it.polimi.dmw.wit.Polygon.Polygon;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.utilities.WitDownloadImageTask;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitPOI;
import it.polimi.dmw.wit.database.DbAdapter;

import com.sromku.simple.fb.entities.Profile.Properties;
import com.sromku.simple.fb.utils.Utils;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;


/**
 * Activity per gestire e visualizzare la lista dei risultati
 */
public class WitFinalResult extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {


    private SimpleFacebook mSimpleFacebook;
    private WitDownloadTask witDownloadTask;
    private WitDownloadImageTask witDownloadImageTask;
    private Profile profileFB;
    private String language;

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
    private int id;
    private int woeid;

    private URL photoURL;
    private byte[] img=null;
    private boolean imgExists = true;

    private boolean imgHandled = false;
    private boolean textHandled = false;

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private ProgressWheel progressWheel;
    private RecyclerView fbList;
    private ListView listView ;
    private MyRecyclerAdapter adapter;
    private CustomAdapter adapter2;
    private ArrayList<String> namesList;
    private ArrayList<byte[]> imagesList;
    private Bitmap image;

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

    public void setImage(Bitmap result, byte[] img) {
        image = result;
        this.img = img;
        imgHandled = true;
        conclude();
    }

    private void stopWheel(){
        findViewById(R.id.progress_wheel).setVisibility(View.GONE);
    }

    public void saveResult(String i, String t, String d, String wLink, URL imgURL) {
        title = t;
        description = d;
        wikiLink = wLink;
        photoURL = imgURL;
        this.id = Integer.parseInt(i);
        if (imgURL != null) {
            imgExists = true;
            witDownloadImageTask = new WitDownloadImageTask(this, null, witDownloadImageTask.POIDETAIL);
            witDownloadImageTask.execute(imgURL);
        }
        else{
            imgExists = false;
            imgHandled = true;
        }
        if(wikiLink!=null){
            //scarico testo
            try {
                URL url = new URL(wikiLink);
                String path = url.getPath();
                String wikipediaArticleTitle = path.substring(path.lastIndexOf('/') + 1);
                URL mediaWikiAPI = new URL("https://"+this.language+".wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles="+wikipediaArticleTitle);
                Log.d(LOG_TAG, "url_api_wiki: "+mediaWikiAPI);

                witDownloadTask = new WitDownloadTask(this, null, witDownloadTask.WIKIPEDIATEXT);
                witDownloadTask.execute(mediaWikiAPI);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        else{
            textHandled = true;
        }
        conclude();
    }

    private void conclude(){
        if(textHandled&&imgHandled) {
            stopWheel();
            titleText.setText(title);
            if(!imgExists){
                Log.d(LOG_TAG, "Risultato senza immagine ");
                mainImage.setVisibility(View.GONE);
            }
            else{
                mainImage.setImageBitmap(image);
            }
            descText.setText(description);
            if(!title.equalsIgnoreCase(getString(R.string.not_found_title_text))) {
                savePOI(id, title, description, img); //salvo nel database il poi
            }
        }
    }

    public void setDescription(String description, String title){
        if(description.length()>= this.description.length()) {
            this.description = description;
            this.title = title;
        }
        textHandled = true;
        conclude();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wit_detail);
        this.language = "it";

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        progressWheel = (ProgressWheel) findViewById(R.id.progress_wheel);


        progressWheel = new ProgressWheel(this);
        progressWheel.spin();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        fbList = (RecyclerView) findViewById(R.id.fb_list);
        listView = (ListView) findViewById(R.id.listView);
        fbList.setLayoutManager(layoutManager);

        imagesList = new ArrayList<>();

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_wit_list);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);

        mSimpleFacebook = SimpleFacebook.getInstance(this);

        configurationSimpleFacebook();
        namesList = new ArrayList<>();

       // adapter = new MyRecyclerAdapter(this, namesList);
        //fbList.setAdapter(adapter);

        adapter2 = new CustomAdapter(this, namesList, imagesList);
        listView.setAdapter(adapter2);

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

        Toast.makeText(this, "Orientation: "+userOrientation, Toast.LENGTH_LONG).show();


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
                URL detailUrl = new URL("http://api.wikimapia.org/?key=example&function=place.getbyid&id=" + correctPoiList.get(0).getPoiId() + "&format=json&language="+language);
                Log.d(LOG_TAG, "SERVER URL: "+detailUrl);
                witDownloadTask = new WitDownloadTask(this, null, witDownloadTask.POIDETAIL);
                witDownloadTask.execute(detailUrl);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
       }
       else {
           // image = BitmapFactory.decodeResource(getResources(), R.drawable.sadface);
            mainImage.setVisibility(View.GONE);
            title = getString(R.string.not_found_title_text);
            description = getString(R.string.not_found_desc_text);
            textHandled = true;
            imgHandled = true;
            conclude();
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


    private void savePOI(int id, String name, String description, byte[] img){
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        SharedPreferences sharedPrefs = getSharedPreferences("WIT", MODE_PRIVATE); //recupero dal database automatico il woeid corrente
        woeid = sharedPrefs.getInt("woeid", 0);
        dbAdapter.savePOI(id, name, description, getCurrentDate(), woeid, img);
        dbAdapter.close();
        registerVisit();
        checkSettingFb();

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
        Log.d(LOG_TAG, ""+sharedPrefs.getBoolean("fb", false));
        if(sharedPrefs.getBoolean("fb", false)) {
            Log.d(LOG_TAG, "FACEBOOK");
            storyOnFacebook();
        }

    }

    private void registerVisit(){
        SharedPreferences sharedPrefs = getSharedPreferences("WIT", MODE_PRIVATE);
        Long fbId = sharedPrefs.getLong("fbId", 0);
        String fbStringInUrl = new String();
        try {
            if(fbId != null && fbId != 0){ //fbId nel logout viene settato a 0
                fbStringInUrl = "&fb_id="+fbId;
            }
            else{
                fbId = null;
            }
            dbAdapter.open();
            cursor = dbAdapter.fetchCityByID(woeid);
            String city=null, county=null, state=null, country = null;
            while(cursor.moveToNext()){
                city = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CITY));
                county = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COUNTY));
                state = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_STATE));
                country = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COUNTRY));
            }
            cursor.close();
            dbAdapter.close();
            URL detailUrl = new URL(getString(R.string.register_visit_url)+"?poi="+id+fbStringInUrl+"&city="+city+"&county="+county+"&state="+state+"&country="+country);
            Log.d(LOG_TAG, "SERVER URL: "+detailUrl);
            witDownloadTask = new WitDownloadTask(this, null, witDownloadTask.REGISTERVISIT);
            witDownloadTask.execute(detailUrl);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void checkIds(ArrayList<Long> l){
        getFacebookFrieds(this, l);

    }

    private void getFacebookFrieds(Activity a, ArrayList<Long> l){
      final  Activity activity = a;
        Properties properties = new Properties.Builder()
                .add(Properties.ID)
                .add(Properties.FIRST_NAME)
                .add(Properties.LAST_NAME)
                .add(Properties.NAME)
                .add(Properties.BIRTHDAY)
                .add(Properties.AGE_RANGE)
                .add(Properties.EMAIL)
                .add(Properties.GENDER)
                .add(Properties.INSTALLED)
                .build();
        final ArrayList<Long> idsList = l;
        //avevo aggiunto manualmente il tuo id jaco per pare le prove...sostituiscilo col mio se vuoi provare
        //idsList.add(Long.parseLong("10207291961247199"));
       // Log.d(LOG_TAG, "" + idsList.get(0));


        SimpleFacebook.getInstance().getFriends(properties, new OnFriendsListener() {

            @Override
            public void onThinking() {

            }

            @Override
            public void onException(Throwable throwable) {
            }

            @Override
            public void onFail(String reason) {
            }

            @Override
            public void onComplete(List<Profile> response) {
                for (int x = 0; x < response.size(); x++) {
                    String name = response.get(x).getFirstName();
                    String id = response.get(x).getId();
                    String img = response.get(x).getPicture();
                    Log.d(LOG_TAG, "id: " + id + " nome: " + name + " " + img);
                    if (idsList.contains(Long.parseLong(id))) {
                        namesList.add(name);
                        downloadImageProfile(id);
                        Log.d(LOG_TAG, "TRue" + namesList.get(0));
                    }
                }

            }
        });
    }

    private void downloadImageProfile(String id){
        URL photoURL = null;
        try {
            photoURL = new URL("https://graph.facebook.com/" + id + "/picture?type=large");
            witDownloadImageTask = new WitDownloadImageTask(this, null, witDownloadImageTask.IMAGEPROFILE);
            witDownloadImageTask.execute(photoURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void saveImage(byte[] img){
        imagesList.add(img);
        adapter2.notifyDataSetChanged();
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

    private class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.CustomViewHolder> {
        private Context mContext;
        private ArrayList<String> fbList;

        public MyRecyclerAdapter(Context context, ArrayList<String> l) {
            fbList = l;
            this.mContext = context;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_row, null);

            CustomViewHolder viewHolder = new CustomViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
           String name = fbList.get(i);


            //Setting text view title
            customViewHolder.textView.setText(name);
        }

        @Override
        public int getItemCount() {
            return fbList.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            protected ImageView imageView;
            protected TextView textView;

            public CustomViewHolder(View view) {
                super(view);
                this.imageView = (ImageView) view.findViewById(R.id.img);
                this.textView = (TextView) view.findViewById(R.id.name);
            }
        }
    }



    /**
     * Classe privata che gestisce la lista
     */
    private class CustomAdapter extends BaseAdapter {
        Context context;
        private ArrayList<String> fbList;
        ArrayList<byte[]> imagesList;
        private  LayoutInflater inflater=null;
        public CustomAdapter(Activity mainActivity,  ArrayList<String> l, ArrayList<byte[]> imagesList) {
            context=mainActivity;
            fbList = l;
            this.imagesList = imagesList;
            inflater = ( LayoutInflater )context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return fbList.size();
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
            String name = fbList.get(position);
            holder.tv.setText(name);
            byte [] img = imagesList.get(position);
            if(img!= null) {
                holder.img.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
            }
            else {

            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                }
            });
            return rowView;
        }

    }

    }
