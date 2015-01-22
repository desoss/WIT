package it.polimi.dmw.wit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
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


    private JSONObject jSondetail;


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
         * Questo viene chiamato quando il thread a finito e gli viene passato il risultato
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
            try {
                printDetail(s);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * Classe privata per downloadare le immagini da stampare. Viene eseguita
     * su un thread a parte. E' una classe interna così può chiamare i metodi dell'activity
     * direttamente
     */
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    private int near(ArrayList<WitPOI> correctPL) {
        double small = correctPL.get(0).getDistance();
        int smallid = correctPL.get(0).getPoiId();

        for (int i = 1; i < correctPL.size(); i++) {
            if (small > correctPL.get(i).getDistance()) {
                small = correctPL.get(i).getDistance();
                smallid = correctPL.get(i).getPoiId();
            }
        }
        System.out.println(smallid + " " + small);
        return smallid;
    }

    public void printDetail(String detail) throws JSONException {
        // CONVERT RESPONSE STRING TO JSON ARRAY
        jSondetail = new JSONObject(detail);


        //Ho il json dei dettagli.
        if (jSondetail != null) {
            try {
                System.out.println(jSondetail.getString("id"));
                System.out.println(jSondetail.getString("title"));
                System.out.println(jSondetail.getString("description"));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            //controllo che campi non vuoti
            //inoltre json non ha sempre gli stessi campi, quindi svolgiamo le funzioni in un try catch.

            //INIZIO PER DESCRIZIONE
            String description = "";
            try {
                description = description + jSondetail.getString("description");

            } catch (JSONException e) {
            }
            if (description == "") {
                description = "Campo non presente";
            }
            //FINE PER DESCRIZIONE

            //INIZIO PER WIKI
            String wiki = "";
            try {
                wiki = wiki + jSondetail.getString("wikipedia");

            } catch (JSONException e) {
                wiki = "Campo non presente";
            }

            if (wiki == "") {
                wiki = "Campo non presente";
            }
            //FINE PER WIKI

            //Prendere link foto INIZIO
            String photos = "";
            try {
                photos = photos + jSondetail.getString("photos");

            } catch (JSONException e) {
            }

            ArrayList<String> linkapp = new ArrayList<String>();
            ArrayList<String> link = new ArrayList<String>();
            if (photos != "") {
                //prendi i link
                int i = 0;
                int pos = 0;
                while (i < photos.length()) {
                    i = photos.indexOf("http", i);
                    if (i != -1) {
                        int stop = photos.indexOf("jpg", i) + 3;

                        if (stop != -1) {
                            linkapp.add(pos, photos.substring(i, stop));
                            i = i + linkapp.get(pos).length();
                            pos++;
                        } else {
                            i = photos.length();
                        }
                    } else {
                        i = photos.length();
                    }
                }


                //PROBLEMA FORMATO: Wikimapia mi restituisce i link alle foto con un escape (\/), devo sostituire tutti i \ con ""
                //Inoltre ho foto "doppioni"

                String app;
                int j=0;
                while (j<linkapp.size()) {
                    link.add(linkapp.get(j).replaceAll("\\\\", ""));
                    app = linkapp.get(j).substring(linkapp.get(j).indexOf("wikimapia.org"), linkapp.get(j).indexOf("_"));
                    boolean diverso = false;
                    while (!diverso) {
                        if (j < linkapp.size() - 1) {
                            String confronto = linkapp.get(j).substring(linkapp.get(j).indexOf("wikimapia.org"), linkapp.get(j).indexOf("_"));
                            if (app.equals(confronto)) {
                                j++;
                            } else {
                                diverso = true;
                            }
                        } else {
                            diverso = true;
                        }
                    }
                    j++;
                }
            }
            //FINE PRENDERE LINK, in ogni posizione i di "link" ora avrò una foto.

            // Mostra da 0 a 4 immagini
            if (link.size()>0) {
                //chiamo il download dell'immagine che viene inserita anche nell'imageView.
                new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                        .execute(link.get(0));

                if (link.size()>1)
                {
                    new DownloadImageTask((ImageView) findViewById(R.id.imageView1))
                            .execute(link.get(1));
                }
            }

            //richiamo oggetti e li scrivo
            TextView T1 = (TextView) findViewById(R.id.textView3);
            T1.setText("Nome: " + jSondetail.getString("title"));
            T1 = (TextView) findViewById(R.id.textView4);
            T1.setText("Descrizione: " + description);
            T1 = (TextView) findViewById(R.id.textView5);
            T1.setText("Wikipedia: " + wiki);
            Linkify.addLinks(T1, Linkify.WEB_URLS);
        }
    }

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

        //Se ho elementi nella lista allora prendo il più vicino
        int smallid = 0;
        if (correctPoiList.size() > 0) {
            smallid = near(correctPoiList);
        }

        //Hai ID più vicino, ora vai a lavorare sul link.
        URL url = null;
        try {
            // TODO cambiare il 55 con "smallid, ho messo 55 per i test"
            url = new URL("http://api.wikimapia.org/?key=example&function=place.getbyid&id=" + smallid + "&format=json&language=it");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        new WitDownloadTask().execute(url);

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
