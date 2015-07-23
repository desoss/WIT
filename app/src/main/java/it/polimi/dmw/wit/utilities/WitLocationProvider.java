package it.polimi.dmw.wit.utilities;

import android.location.Location;
/**
 * Interfaccia per unire i due metodi di ricerca GPS.
 * La main activity si aspetta un oggetto che implementi
 * quest'interfaccia così non dobbiamo riscrivere codice per
 * cambiare tra uno e l'altro.
 *
 * Created by sivam on 1/17/15.
 */
public interface WitLocationProvider{

    /**
     * Inizia a ricevere notifiche GPS
     */
    public void startGettingLocation();

    /**
     * Smetti di ricevere notifiche GPS, risparmia batteria
     */
    public void stopGettingLocation();

    /**
     * Metodo per recuperare la migliore Location ottenuta
     * @return la miglior Location ottenuta
     */
    public Location getLocation();

    /**
     * Metodo per verificare se almeno una Location
     * è stata ottenuta
     * @return true or false
     */
    public boolean hasLocation();

}
