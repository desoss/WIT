package it.polimi.dmw.wit.utilities;

import android.os.Parcel;
import android.os.Parcelable;

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
    private float [] x;
    private float [] y;

    private int wikimapiaId;
    private String description;
    private String date;
    private int woeid;

    private String correct; // serve solo per la classe DisplayMap, per il parcelable con boolean ma da errore http://stackoverflow.com/questions/6201311/how-to-read-write-a-boolean-when-implementing-the-parcelable-interface

    // Costruttore
    public WitPOI(int id, String name, double dist, double lat, double lon, float [] x, float [] y) {
        poiId = id;
        poiName = name;
        distance = dist;
        poiLat = lat;
        poiLon = lon;
        this.x = x;
        this.y = y;
        correct = "0";
    }

    public WitPOI (int id, int wikimapiaId, String name, String description, String date, int woeid){
        poiId = id;
        this.wikimapiaId = wikimapiaId;
        poiName = name;
        this.description = description;
        this.date = date;
        this.woeid = woeid;
    }

    // Quando un oggetto viene inserito in una ListView
    // di default viene chiamato toString()
    public String toString() {
        return poiName+" at "+String.valueOf(distance)+" meters";
    }

    // GETTER METHODS

    public void setCorrect(){
        correct = "1";
    }
    public String getCorrect(){ return correct; }

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

    public float [] getX() { return x;}

    public float [] getY() { return y;}

    public int getWoeid() { return woeid;}

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
        x = in.createFloatArray();
        y = in.createFloatArray();
        correct =   in.readString(); //in.readByte() != 0;     //coorect == true if byte != 0
        wikimapiaId = in.readInt();
        description = in.readString();
        date = in.readString();
        woeid = in.readInt();

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
        dest.writeFloatArray(x);
        dest.writeFloatArray(y);
        dest.writeString(correct);//writeByte((byte) (correct ? 1 : 0));     //if correct == true, byte == 1
        dest.writeInt(wikimapiaId);
        dest.writeString(description);
        dest.writeString(date);
        dest.writeInt(woeid);
        }



    public int getWikimapiaId() {
        return wikimapiaId;
    }


    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }


}
