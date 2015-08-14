package it.polimi.dmw.wit.activities;


import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.utilities.WitPOI;

public class WitSavedPOI extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    private int idPOI;
    private DbAdapter dbAdapter;
    private Cursor cursor;
    private String name;
    private String description;
    private ImageView mainImage;
    private TextView titleText;
    private TextView descText;
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private final static String LOG_TAG = "WitSavedPOI";
    private WitPOI poi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wit_detail);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        findViewById(R.id.progress_wheel).setVisibility(View.GONE);


        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_Saved_POI);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
        //  displayView(0);




        // Prendi l'intent che ha aperto questa activity, cioè
        // quello che viene dalla main activity

        mainImage = (ImageView)findViewById(R.id.poi_img);
        titleText = (TextView)findViewById(R.id.poi_name_text);
        descText = (TextView)findViewById(R.id.poi_desc_text);
}

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        poi = intent.getParcelableExtra(WitPOIsList.EXTRA_POI);
        poi = intent.getParcelableExtra(WitDetailJourney.EXTRA_POI);
        byte[] img = intent.getByteArrayExtra(WitPOIsList.EXTRA_IMG);



            name = poi.getPoiName();
            description = poi.getDescription();

            if (img!=null) {
                mainImage = (ImageView) findViewById(R.id.poi_img);
                mainImage.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
            }

        titleText.setText(name);
        descText.setText(description);

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




}
