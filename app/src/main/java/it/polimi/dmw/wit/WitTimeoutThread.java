package it.polimi.dmw.wit;

import android.os.Handler;
import android.util.Log;

/**
 * Classe per gestire le richieste GPS
 * con il vecchio metodo.
 * Invia REQUEST_NUMBER richiest all'activity.
 * Ogni richiesta l'activity controlla se c'è una posizione disponibile.
 * Dopo REQUEST_NUMBER * REQUEST_INTERVAL invia un messaggio di timeout.
 *
 * Created by sivam on 1/17/15.
 */
public class WitTimeoutThread extends Thread {

    // Nome per i messaggi del log
    private static final String LOG_TAG = "WitTimeoutThread";

    // Numero totale di richieste di update e intervallo tra richieste
    private static final int REQUEST_NUMBER = 30;
    private static final int REQUEST_INTERVAL = 500;

    // Codici per comunicare con l'activity
    public static final int CHECK_LOCATION_CODE = 0;
    public static final int TIMEOUT_CODE = 1;

    // Handler che gestisce i messaggi tra thread e activity
    private Handler handler;

    // Costruttore, riceve l'handler per comunicare con l'activity
    public WitTimeoutThread(Handler hand) {
        handler = hand;
    }

    @Override
    public void run() {

        for (int i = 0; i < REQUEST_NUMBER; i++) {
            // Imposta REQUEST_NUMBER messaggi da consegnare all'activity
            // a intervalli di tempo regolari
            // empty message è un messaggio che contiene solo il codice.

            if (!handler.sendEmptyMessageDelayed(CHECK_LOCATION_CODE, i*REQUEST_INTERVAL)) {
                // Se il messaggio non è consegnato scrive sul log
                Log.e(LOG_TAG,"ERROR: a request message was not delivered");
            }
        }

        // Imposta il messaggio di timeout
        if (!handler.sendEmptyMessageDelayed(TIMEOUT_CODE, REQUEST_NUMBER*REQUEST_INTERVAL)){
            Log.e(LOG_TAG,"ERROR: timeout message was not delivered");
        }
    }
}
