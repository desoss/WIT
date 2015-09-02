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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import it.polimi.dmw.wit.R;
import it.polimi.dmw.wit.database.DbAdapter;
import it.polimi.dmw.wit.sliderMenu.FragmentDrawer;
import it.polimi.dmw.wit.utilities.WitCity;
import it.polimi.dmw.wit.utilities.WitJourney;
import it.polimi.dmw.wit.utilities.WitPOI;

public class WitDiary extends ActionBarActivity implements FragmentDrawer.FragmentDrawerListener {

    private DbAdapter dbAdapter;
    private Cursor cursor;
    private final static String LOG_TAG = "WitDiaryActivity";
    private Toolbar mToolbar;
    private FragmentDrawer drawerFragment;
    //private int[] woeid;
    private ArrayList<WitCity> citieList;
    private HashMap woeidMap;
    private ArrayList<WitPOI> poisList;
    private ArrayList<WitJourney> journeysList;
    private ArrayList<WitJourney> completeList;
    private final static int maxDays = 30;
    private WitJourney journey;
    private GridView gridview;
    private Intent intent;
    private Intent intent2;
    public final static String EXTRA_JOURNEY= "it.polimi.dmw.wit.JOURNEY";
    private TextView titleText;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wit_diary);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        gridview = (GridView) findViewById(R.id.gridview);
        titleText = (TextView)findViewById(R.id.journey_name_text);


        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.title_activity_Diary);


        drawerFragment = (FragmentDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
        drawerFragment.setDrawerListener(this);
        //  displayView(0);

    }

    @Override
    protected void onStart() {
        super.onStart();
        titleText.setVisibility(View.GONE);

        loadFromDatabase();
        divideJourneysByDate();
        printTest();
        orderJourneys();
        Log.d(LOG_TAG, "Viaggi ordinati: ");
        printFinalTest();
        collapseJourneys();
        Log.d(LOG_TAG, "Viaggi collassati: ");
        printFinalTest();

        gridview.setAdapter(new CustomAdapter(this, completeList));
       intent = new Intent(this, WitDetailJourney.class);
        intent2 = new Intent(this, WitDetailState.class);

    }

    private void loadFromDatabase(){
        dbAdapter = new DbAdapter(this);
        dbAdapter.open();
        cursor = dbAdapter.fetchCITYINFO();
        citieList = new ArrayList<>();
        woeidMap = new HashMap();
        while (cursor.moveToNext()) {
            int woeid = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));
            String name = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_CITY));
            String county = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COUNTY));
            String state = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_STATE));
            String country = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_COUNTRY));
            byte [] img = cursor.getBlob(cursor.getColumnIndex(DbAdapter.KEY_THUMBNAIL));
            WitCity city = new WitCity(woeid, name, county, state, country, img);
            citieList.add(city);
        }
        for (int x = 0; x < citieList.size(); x++) {
            cursor = dbAdapter.fetchPOIByWoeid(citieList.get(x).getWoeid());
            poisList = new ArrayList<>();
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_ID));
                int wikimapiaId = cursor.getInt(cursor.getColumnIndex(DbAdapter.KEY_WIKIMAPIAID));
                String name = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_NAME));
                String description = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_DESCRIPTION));
                String date = cursor.getString(cursor.getColumnIndex(DbAdapter.KEY_DATE));
                double lat = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LAT));
                double lon = cursor.getDouble(cursor.getColumnIndex(DbAdapter.KEY_LON));
                WitPOI poi = new WitPOI(id, wikimapiaId, name, description, date,lat, lon,citieList.get(x).getWoeid());
                poisList.add(poi);
            }
            if (cursor.getCount() > 0) {
                woeidMap.put(citieList.get(x), poisList);
            }
        }

        cursor.close();
        dbAdapter.close();
    }

   private void divideJourneysByDate(){
       for (int x = 0; x < citieList.size(); x++) {
           WitCity city = citieList.get(x);
           if (woeidMap.containsKey(city)) {
               poisList = (ArrayList) woeidMap.get(city);
               journeysList = new ArrayList<>();
               if(poisList.size()==1){
                   journey = new WitJourney(city);
                   journey.addPoi(poisList.get(0));
                   journeysList.add(journey);
               }
               for (int t = 0; t < poisList.size(); t++) {
                   for (int y = t+1; y < poisList.size(); y++) {
                       String s1 = poisList.get(t).getDate();
                       String s2 = poisList.get(y).getDate();
                       if (Math.abs(compareDates(s1, s2)) < maxDays) {
                           if (!contains(poisList.get(t), poisList.get(y))) {
                               journey = new WitJourney(city);
                               journey.addPoi(poisList.get(t));
                               journey.addPoi(poisList.get(y));
                               journeysList.add(journey);
                           }
                       }
                       else {
                           if(!contains(poisList.get(t))) {
                               journey = new WitJourney(city);
                               journey.addPoi(poisList.get(t));
                               journeysList.add(journey);
                           }
                           if(!contains(poisList.get(y))) {
                               journey = new WitJourney(city);
                               journey.addPoi(poisList.get(y));
                               journeysList.add(journey);
                           }
                       }
                   }
               }

               woeidMap.put(city, journeysList);
           }
       }

   }
    private void printTest() {
        completeList = new ArrayList<>();
        for (int x = 0; x < citieList.size(); x++) {
            WitCity city = citieList.get(x);
            String nameCity = city.getName();
            Log.d(LOG_TAG, "Citta': " + nameCity);
            if (woeidMap.containsKey(city)) {
                journeysList = (ArrayList) woeidMap.get(city);
                Log.d(LOG_TAG, "Numero viaggi: " + journeysList.size());
                for (int y = 0; y < journeysList.size(); y++) {
                    completeList.add(journeysList.get(y));
                    Log.d(LOG_TAG, "Viaggio numero: " + y);
                    Log.d(LOG_TAG, "Numero POI: " + journeysList.get(y).size());
                    for (int t = 0; t < journeysList.get(y).size(); t++) {
                        String namePoi = journeysList.get(y).getPois().get(t).getPoiName();
                        Log.d(LOG_TAG, "Nome POI: " + namePoi);
                    }
                }
            }
        }
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
        TimeUnit tu = TimeUnit.DAYS;
        long diffInMillies = d1.getTime() - d2.getTime();
        Long r = tu.convert(diffInMillies, TimeUnit.MILLISECONDS);
        Log.d(LOG_TAG, "Ris: " + r);
        return r;

    }

    private boolean contains(WitPOI poi1, WitPOI poi2) {
        for (int x = 0; x < journeysList.size(); x++) {
            if (journeysList.get(x).contains(poi1) && !journeysList.get(x).contains(poi2)) {
                journeysList.get(x).addPoi(poi2);
                return true;
            }
            if (journeysList.get(x).contains(poi1) && journeysList.get(x).contains(poi2)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(WitPOI poi) {
        for (int x = 0; x < journeysList.size(); x++) {
            if (journeysList.get(x).contains(poi)) {
                return true;
            }
        }
        return false;
    }

    private void orderJourneys(){
        for(int x=0; x<completeList.size();x++){
            Date d1 = completeList.get(x).getStartDate();
            for(int y=x+1; y<completeList.size();y++){
                Date d2 = completeList.get(y).getStartDate();
                if(d1.before(d2)){
                    completeList.add(x, completeList.get(y));
                    completeList.remove(y+1);
                }
            }
        }

    }

    private void collapseJourneys(){
        for(int x=0; x<completeList.size()-1;x++) {
            WitJourney j1 = completeList.get(x);
            WitJourney j2 = completeList.get(x + 1);
            Log.d(LOG_TAG,j1.getCity().getCountry()+" "+j2.getCity().getCountry());
            if(j1.getCity().getCountry().equals(j2.getCity().getCountry())){
                Log.d(LOG_TAG,"true");
                for(int y=0;y<j2.getPois().size();y++){
                    j1.addPoi(j2.getPois().get(y));
                }
                for(int t=0; t<j2.getCities().size();t++){
                    j1.addCity(j2.getCities().get(t));
                }
                completeList.remove(x+1);
                x--;
            }
        }
    }

    private void printFinalTest(){
        for(int x=0; x<completeList.size();x++){
            Log.d(LOG_TAG, "Viaggio n : " +x);
            String nameJ = completeList.get(x).getNameJourney();
            Log.d(LOG_TAG, "A: " + nameJ);
            Date d1 = completeList.get(x).getStartDate();
            Date d2 = completeList.get(x).getEndDate();
            Log.d(LOG_TAG, "Da: " + d1+"A: "+d2);
            Log.d(LOG_TAG,"POI visitati: ");
            ArrayList<WitPOI> l = completeList.get(x).getPois();
            for(int y=0; y<l.size();y++){
                String name = l.get(y).getPoiName();
                Log.d(LOG_TAG,name+" " +l.get(y).getDate());
            }

        }
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
        private LayoutInflater inflater=null;
        public CustomAdapter(Activity activity, ArrayList<WitJourney> l) {
            // TODO Auto-generated constructor stub
            journeysList = l;
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
           // ((holder.img.setScaleType(ImageView.ScaleType.CENTER);
            // circularImageView.setSelectorColor(getResources().getColor(R.color.colorPrimary));
            //circularImageView.setSelectorStrokeColor(getResources().getColor(R.color.colorPrimaryDark));
            holder.img.setSelectorStrokeWidth(10);
            holder.img.addShadow();
            j = journeysList.get(position);
            holder.tv.setText(j.getNameJourney());
            //holder.img.setImageResource(imageId[position]);
            byte[] image = j.getCity().getImg();
            if(image!= null) {
                holder.img.setImageBitmap(BitmapFactory.decodeByteArray(image, 0,image.length));
            }
            else {

            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   j = journeysList.get(position);
                    if(j.getCities().size()==1){
                        intent.putExtra(EXTRA_JOURNEY,j);
                    startActivity(intent);}
                    if(j.getCities().size()>1){
                        intent2.putExtra(EXTRA_JOURNEY,j);
                        startActivity(intent2);
                    }

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
