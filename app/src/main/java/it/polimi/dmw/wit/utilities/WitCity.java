package it.polimi.dmw.wit.utilities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Mattia on 12/08/2015.
 */
public class WitCity implements Parcelable {

    private String name;
    private String county;
    private String state;
    private String country;
    private int woeid;
    private byte [] img;


    public WitCity(int woeid, String name, String county, String state, String country, byte[] img){
        this.woeid = woeid;
        this.name = name;
        this.county = county;
        this.state = state;
        this.country = country;
        this.img = img;
    }

    public String getName() {
        return name;
    }

    public String getCounty() {
        return county;
    }

    public String getState() {
        return state;
    }

    public String getCountry() {
        return country;
    }

    public int getWoeid() {
        return woeid;
    }

    public byte[] getImg(){
        return img;
    }

    // I METODI seguenti servono per renderlo serializzabile e passarlo tra activity diverse

    public static final Parcelable.Creator<WitCity> CREATOR
            = new Parcelable.Creator<WitCity>() {
        public WitCity createFromParcel(Parcel in) {
            return new WitCity(in);
        }

        public WitCity[] newArray(int size) {
            return new WitCity[size];
        }
    };



    /**
     * Ricrea un oggetto WitJourney leggendo da un Parcel
     *
     * @param in
     */
    private WitCity(Parcel in) {
        name = in.readString();
        county = in.readString();
        state = in.readString();
        country = in.readString();
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
        dest.writeString(name);
        dest.writeString(county);
        dest.writeString(state);
        dest.writeString(country);
        dest.writeInt(woeid);
       // dest.writeInt(img.length);
       // dest.writeByteArray(img);


    }





}
