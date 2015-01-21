package it.polimi.dmw.wit;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by sivam on 1/19/15.
 */
public class WitLocationAPI implements WitLocationProvider,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    // Nome per i messaggi del log
    private static final String LOG_TAG = "WitLocationAPI";

    // Parameters to establish which location is better

    // Time after which a location is old (milliseconds)
    private static final int MAX_CACHED_TIME = 1000 * 60;
    // Distance after which a location is considered not accurate (meters)
    private static final int MAX_DELTA_ACCURACY = 10;

    // Tempo tra una richiesta e l'altra
    private static final int MIN_TIME_BETWEEN_REQUEST = 100;
    // Distanza minima per aggiornare una posizione
    private static final int MIN_DISTANCE = 1;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private Location currentLocation;

    private boolean isConnected;

    public WitLocationAPI(GoogleApiClient.Builder builder) {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(100)
            .setInterval(250);

        googleApiClient = builder
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        isConnected = false;
    }


    @Override
    public void startGettingLocation() {
        googleApiClient.connect();
    }

    @Override
    public void stopGettingLocation() {
        if (isConnected) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(googleApiClient, this);

            googleApiClient.disconnect();
        }
    }

    @Override
    public Location getLocation() {
        return currentLocation;
    }

    @Override
    public boolean hasLocation() {
        return currentLocation != null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        isConnected = true;
        LocationServices.FusedLocationApi
                .requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        isConnected = false;
        Log.e(LOG_TAG,"ERROR: failed to connect to google services");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location)) {
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

}
