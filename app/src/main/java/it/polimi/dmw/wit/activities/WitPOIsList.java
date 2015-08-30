package it.polimi.dmw.wit.activities;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.pkmmte.view.CircularImageView;

import java.util.ArrayList;

import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.utilities.WitPOI;

public class WitPOIsList extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    private DbAdapter dbAdapter;
    private Cursor cursor;
    private ListView listView ;
    private final static String LOG_TAG = "WitPOIsListActivity";
    private Intent intent;
    public final static String EXTRA_POI= "it.polimi.dmw.wit.POI";
    public final static String EXTRA_IMG= "it.polimi.dmw.wit.IMG";
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    private WitPOI [] poisList;
    private byte [][] imagesList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_wit_list);

        listView = (ListView) findViewById(R.id.listView);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_POIs_list);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
        //  displayView(0);


    }



    @Override
    protected void onStart() {
        super.onStart();

        dbAdapter=new DbAdapter(this);
        dbAdapter.open();
        cursor=dbAdapter.fetchAllPOIs();
        int i = cursor.getCount()-1;
        poisList = new WitPOI[i+1];
        imagesList = new byte[i+1][];
        while ( cursor.moveToNext() ) {

          String name=cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_NAME));
          int id  = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));
//          int wikimapiaId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_WIKIMAPIAID));
          String description = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_DESCRIPTION));
          String date = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_DATE));
          double lat = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LAT));
          double lon = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LON));
          byte[] img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_IMAGE));
            imagesList [i] = img;

          WitPOI poi = new WitPOI(id, 0, name, description, date, lat, lon, 0);
          poisList[i] = poi;
          i--;


           //  n = cursor.getString( cursor.getColumnIndex(DbAdapter.KEY_NAME) );
           // int a=cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));

        }
        cursor.close();

        dbAdapter.close();

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

      //  ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        //        android.R.layout.simple_list_item_1, android.R.id.text1, values);


        // Assign adapter to ListView
        listView.setAdapter(new CustomAdapter(this, poisList, imagesList));
         intent = new Intent(this, WitSavedPOI.class);

          /*
        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {



                intent.putExtra(EXTRA_POI,idPOI[position]);
                startActivity(intent);

                Log.d( LOG_TAG,"nome POI = "+ values[position]+ idPOI[position]);


            }

        });
        */
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
        WitPOI[] poisList;
        WitPOI poi;
        byte [][] imagesList;
        private  LayoutInflater inflater=null;
        public CustomAdapter(Activity mainActivity, WitPOI[] l, byte [][] b) {
            context=mainActivity;
            poisList = l;
            imagesList = b;
            inflater = ( LayoutInflater )context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return poisList.length;
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
            poi = poisList[position];
            holder.tv.setText(poi.getPoiName());
            byte [] img = imagesList[position];
            if(img!= null) {
                holder.img.setImageBitmap(BitmapFactory.decodeByteArray(img, 0, img.length));
            }
            else {
                holder.img.setImageResource(R.drawable.gray);


            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    poi = poisList[position];
                    intent.putExtra(EXTRA_POI,poi);
                    startActivity(intent);

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
              //  i = new Intent(this, WitPOIsList.class);
            //    startActivity(i);
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
