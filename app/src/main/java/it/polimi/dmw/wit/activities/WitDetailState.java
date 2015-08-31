package it.polimi.dmw.wit.activities;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.pkmmte.view.CircularImageView;
import java.util.ArrayList;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.utilities.WitCity;
import it.polimi.dmw.wit.utilities.WitJourney;




public class WitDetailState extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private final static String LOG_TAG = "WitDetailJourney";
    private WitJourney journey;
    private ImageView mainImage;
    private TextView titleText;
    private GridView gridView;
    private Intent intent;
    public final static String EXTRA_JOURNEY= "it.polimi.dmw.wit.JOURNEY";
    private DbAdapter dbAdapter;
    private Cursor cursor;
    private byte [][] imagesListCities;
    private ArrayList<WitJourney> journeysList;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wit_diary);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        gridView = (GridView) findViewById(R.id.gridview);



        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_detail_journey);

        titleText = (TextView)findViewById(R.id.journey_name_text);



        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        intent = getIntent();
        journey = intent.getParcelableExtra(WitDiary.EXTRA_JOURNEY);

        Log.d(LOG_TAG,""+journey.getNameJourney());
        titleText.setText(journey.getNameJourney());
        Log.d(LOG_TAG, journey.getNameJourney());

        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        int i = journey.getCities().size();
        imagesListCities = new byte[i][];
        journeysList = new ArrayList<>();
        for(int x=0;x<i;x++) {
            int woeid = journey.getCities().get(x).getWoeid();
            WitCity city = journey.getCities().get(x);
            divideJourney(woeid, city);
            cursor = dbAdapter.fetchCityByID(woeid);
            while(cursor.moveToNext()) {
                imagesListCities[x] = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_THUMBNAIL));
            }
        }
        cursor.close();
        dbAdapter.close();


        gridView.setAdapter(new CustomAdapter(this, journeysList, imagesListCities));
        intent = new Intent(this, WitDetailJourney.class);

    }

    private void divideJourney(int woeid, WitCity city){
        WitJourney j = new WitJourney(city);
        for(int x=0; x< journey.size();x++){
            if(journey.getPois().get(x).getWoeid()== woeid){
                j.addPoi(journey.getPois().get(x));
            }
        }
        journeysList.add(j);
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
     * Classe privata che gestisce la lista
     */
    private class CustomAdapter extends BaseAdapter {
        Context context;
        WitJourney j;
        ArrayList<WitJourney> journeysList;
        byte imagesList [][];
        private LayoutInflater inflater=null;
        public CustomAdapter(Activity activity, ArrayList<WitJourney> p, byte[][] l) {
            // TODO Auto-generated constructor stub
            imagesList = l;
            journeysList = p;
            context=activity;
            inflater = ( LayoutInflater )context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return journeysList.size();
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
            rowView = inflater.inflate(R.layout.places_list, null);
            holder.tv=(TextView) rowView.findViewById(R.id.textView);
            holder.img=(CircularImageView) rowView.findViewById(R.id.img);
            holder.img.setBorderColor(getResources().getColor(R.color.colorPrimary));
            holder.img.setBorderWidth(4);
            holder.img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // circularImageView.setSelectorColor(getResources().getColor(R.color.colorPrimary));
            //circularImageView.setSelectorStrokeColor(getResources().getColor(R.color.colorPrimaryDark));
            holder.img.setSelectorStrokeWidth(10);
            holder.img.addShadow();
            j = journeysList.get(position);
            holder.tv.setText(j.getNameJourney());
            //holder.img.setImageResource(imageId[position]);
            byte[] image = imagesList[position];
            if(image!= null) {
                holder.img.setImageBitmap(BitmapFactory.decodeByteArray(image, 0,image.length));
            }
            else {
                holder.img.setImageResource(R.drawable.gray);


            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    j = journeysList.get(position);
                    intent.putExtra(EXTRA_JOURNEY, j);
                    startActivity(intent);



                    //Log.d( LOG_TAG,"nome POI = "+ result[position]+ id[position]);

                }
            });
            return rowView;
        }

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
