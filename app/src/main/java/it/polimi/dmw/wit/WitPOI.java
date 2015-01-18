package it.polimi.dmw.wit;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Classe per gestire le informazioni su un POI
 * Implementa Parcelable per essere passata tra un activity e l'altra
 *
 * Created by sivam on 1/17/15.
 */
public class WitPOI implements Parcelable{

    // Membri della classe
    private int poiId;
    private String poiName;
    private double distance;
    private double poiLat;
    private double poiLon;

    // Costruttore
    public WitPOI(int id, String name, double dist, double lat, double lon) {
        poiId = id;
        poiName = name;
        distance = dist;
        poiLat = lat;
        poiLon = lon;
    }

    // Quando un oggetto viene inserito in una ListView
    // di default viene chiamato toString()
    public String toString() {
        return poiName+" at "+String.valueOf(distance)+" meters";
    }

    // GETTER METHODS

    public double getPoiLon() {
        return poiLon;
    }

    public double getPoiLat() {
        return poiLat;
    }

    public double getDistance() {
        return distance;
    }

    public String getPoiName() {
        return poiName;
    }

    public int getPoiId() {
        return poiId;
    }

    // I METODI seguenti servono per renderlo serializzabile e passarlo tra activity diverse

    public static final Parcelable.Creator<WitPOI> CREATOR
            = new Parcelable.Creator<WitPOI>() {
        public WitPOI createFromParcel(Parcel in) {
            return new WitPOI(in);
        }

        public WitPOI[] newArray(int size) {
            return new WitPOI[size];
        }
    };

    /**
     * Ricrea un oggetto WitPOI leggendo da un Parcel
     *
     * @param in
     */
    private WitPOI(Parcel in) {
        poiId = in.readInt();
        poiName = in.readString();
        distance = in.readDouble();
        poiLat = in.readDouble();
        poiLon = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Scrive un oggetto WitPOI su un Parcel
     *
     * @param dest
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(poiId);
        dest.writeString(poiName);
        dest.writeDouble(distance);
        dest.writeDouble(poiLat);
        dest.writeDouble(poiLon);
    }
}
