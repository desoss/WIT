package it.polimi.dmw.wit;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity per gestire e visualizzare la lista dei risultati
 */
public class WitListActivity extends ActionBarActivity {

    // TODO set the cone width to select pois in front of the user
    /**
     * Ampiezza del cono di visione, per adesso ho fatto un paio di prove,
     * andrebbe verificata
     */
    private final static double coneWidth = Math.PI/6;

    /**
     * Membri per gestire la ListView
     */
    private ListView poiListView;
    private ArrayAdapter<WitPOI> poiListAdapter;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wit_list);

        // Prendi l'intent che ha aperto questa activity, cioè
        // quello che viene dalla main activity
        Intent intent = getIntent();

        // Estrai la lista dei POI dall'intent.
        poiList = intent.getParcelableArrayListExtra(WitMainActivity.EXTRA_POI_LIST);

        // Estrai i dati dell'utente dall'intent
        userLatitude = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_LAT, 0.0);
        userLongitude = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_LON, 0.0);
        userOrientation = intent.getDoubleExtra(WitMainActivity.EXTRA_USER_ORIENTATION, 0.0);

        // Salva il riferimento alla ListView dell'interfaccia
        poiListView = (ListView)findViewById(R.id.poi_list);

        // Applica l'algoritmo geometrico alla lista e ottieni la lista filtrata
        correctPoiList = new ArrayList<WitPOI>();

        // TODO debug da togliere
        Toast.makeText(this, "Orientation NORTH from sensor : " + String.valueOf(Math.toDegrees(userOrientation)) +
                        "\nRotation from algorithm EAST : " + String.valueOf(Math.toDegrees(adjustAngle(userOrientation))),
                Toast.LENGTH_LONG).show();


        for (WitPOI poi : poiList) {
            if (geometricCheck(userLongitude, userLatitude, poi.getPoiLon(), poi.getPoiLat(),userOrientation)) {
                correctPoiList.add(poi);
            }
        }

        // Piccolo messaggio di informazione TODO da togliere per la versione finale
//        Toast.makeText(this, "Items are: "+String.valueOf(poiList.size())+"\nCorrect items are: "+String.valueOf(correctPoiList.size()),
//               Toast.LENGTH_LONG).show();

        // Inserisci la lista filtrata nella ListView
        poiListAdapter = new ArrayAdapter<WitPOI>(this, android.R.layout.simple_list_item_1, correctPoiList);
        poiListAdapter.notifyDataSetChanged();

        poiListView.setAdapter(poiListAdapter);

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

    // I seguenti sono METODI per gestire l'actionbar in alto

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wit_list, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
