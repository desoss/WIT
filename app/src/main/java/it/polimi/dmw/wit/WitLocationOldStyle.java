package it.polimi.dmw.wit;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Classe che implementa il metodo vecchio di gestione GPS
 *
 * Created by sivam on 1/17/15.
 */
public class WitLocationOldStyle implements WitLocationProvider, LocationListener {

    // Parameters to establish which location is better

    // Time after which a location is old (milliseconds)
    private static final int MAX_CACHED_TIME = 1000 * 60;
    // Distance after which a location is considered not accurate (meters)
    private static final int MAX_DELTA_ACCURACY = 10;

    // Tempo tra una richiesta e l'altra
    private static final int MIN_TIME_BETWEEN_REQUEST = 100;
    // Distanza minima per aggiornare una posizione
    private static final int MIN_DISTANCE = 1;

    // Members
    Location currentLocation;
    LocationManager locationManager;

    // Il criterio per descrivere il risultato
    Criteria accurateCriteria = new Criteria();

    // Costruttore
    public WitLocationOldStyle(LocationManager locMan) {
        locationManager = locMan;

        // Critera definisce come vuoi il risultato
        accurateCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        accurateCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
    }

    /**
     * Metodo per iniziare a ricevere notifiche
     */
    @Override
    public void startGettingLocation() {
        locationManager.requestLocationUpdates(MIN_TIME_BETWEEN_REQUEST, MIN_DISTANCE, accurateCriteria, this, null);
    }

    /**
     * Smette di ricever notifiche
     */
    @Override
    public void stopGettingLocation() {
        locationManager.removeUpdates(this);
    }

    /**
     * Metodo che viene chiamato quando una nuove location è disponibile
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        // Se ho una posizione migliore
        if(isBetterLocation(location)) {
            // Aggiorna la location
            currentLocation = location;
        }
    }

    /**
     * Metodo per verificare se una posizione è meglio
     * di quella che ho già
     *
     * @param location
     * @return
     */
    private boolean isBetterLocation(Location location) {
        if (currentLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MAX_CACHED_TIME;
        boolean isSignificantlyOlder = timeDelta < -MAX_CACHED_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > MAX_DELTA_ACCURACY;

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }


    // I seguenti sono METODI del Sensor listener che non usiamo

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // I seguenti sono METODI dell'interfaccia per ottenere una location

    @Override
    public Location getLocation() {
        return currentLocation;
    }

    @Override
    public boolean hasLocation() {
        return currentLocation != null;
    }
}
