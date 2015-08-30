package it.polimi.dmw.wit.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.slidingTabs.SlidingTabLayout;
import it.polimi.dmw.wit.slidingTabs.ViewPagerAdapter;
import it.polimi.dmw.wit.utilities.WitDownloadTask;
import it.polimi.dmw.wit.utilities.WitLocationAPI;
import it.polimi.dmw.wit.utilities.WitLocationProvider;
import it.polimi.dmw.wit.utilities.WitOrientationProvider;
import it.polimi.dmw.wit.utilities.WitPOI;
import it.polimi.dmw.wit.utilities.WitTimeoutThread;


/**
 * Main activity dell'applicazione, gestisce i sensori e i download.
 *
 * Coordinate della Tour Eiffel (non si sa mai):
 *  lat=48.858252
 *  lon=2.29451
 *
 */
public class WitMainActivity extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {



    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;


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
     * Tag per il log
     */
    private final static String LOG_TAG = "WitMainActivity";

    private Toolbar toolbar;
    private ViewPager pager;
    private ViewPagerAdapter adapter;
    private SlidingTabLayout tabs;
    private CharSequence Titles[] = {"Scan","Info"};
    private int Numboftabs =2;
    private Double latMax;
    private Double lonMax;
    private Double latMin;
    private Double lonMin;
    private boolean BiggerSquareUsable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_wit_main);



        // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
        adapter =  new ViewPagerAdapter(getSupportFragmentManager(),Titles,Numboftabs);

        // Assigning ViewPager View and setting the adapter
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width

        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.tabsScrollColor);
            }
        });

        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(pager);



        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
      //  displayView(0);

        SharedPreferences prefs = getSharedPreferences("bigSquareMonumentList", Context.MODE_PRIVATE);
        //uso il metodo definito da me altrimenti avrei dovuto usare stringhe (prefs.getString("max_lat",""); )
        latMax = getDouble(prefs,"max_lat");
        latMin = getDouble(prefs,"min_lat");
        lonMax = getDouble(prefs,"max_lon");
        lonMin = getDouble(prefs, "min_lon");

        TextView text =  (TextView) findViewById(R.id.jsonText);
        text.append("latMin: "+latMin+"\n");
        text.append("latMax: "+latMax+"\n");
        text.append("lonMin: "+lonMin+"\n");
        text.append("lonMax: "+lonMax+"\n");

        try {
                JSONObject jsonObject = readCache();
                if(jsonObject!=null){
                    String found = jsonObject.getString("found") ;
                    text.append("found: " + found + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

    }
    private JSONObject readCache() throws IOException, JSONException {
        InputStreamReader in;
        JSONObject jsonObject = null;

        File cacheFile = new File(((Context) this).getCacheDir(), "cacheFile.srl");
        if (cacheFile.exists()) {//se il file non esiste ritorna null

            StringBuilder responseStrBuilder = new StringBuilder();

            in = new InputStreamReader(new FileInputStream(cacheFile), "UTF-8");
            BufferedReader buffReader = new BufferedReader(in);

            String inputStr;
            while ((inputStr = buffReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            jsonObject = new JSONObject(responseStrBuilder.toString());
            in.close();
        }
        return jsonObject;
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



    @Override
    protected void onStart() {
        super.onStart();

        Log.d(LOG_TAG, "onStart()");

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onResume() {
        // Always call superclass method first
        super.onResume();

        Log.d(LOG_TAG, "onResume()");



    }

    @Override
    protected void onPause() {
        // Always call superclass method first
        super.onPause();
        Log.d(LOG_TAG, "onPause()");

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
}