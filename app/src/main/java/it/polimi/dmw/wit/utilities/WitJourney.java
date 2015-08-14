package it.polimi.dmw.wit.utilities;


import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WitJourney implements Parcelable {

    private Date startDate = null;
    private Date endDate = null;
    private String nameJourney;
    private ArrayList<WitCity> citiesList;
    private ArrayList<WitPOI> poisList;

   public WitJourney(WitCity city){
       citiesList = new ArrayList<>();
       citiesList.add(city);
       nameJourney = city.getName();
       poisList = new ArrayList<>();
}

    public void addPoi(WitPOI poi){
        poisList.add(poi);
        setDates(poi);
    }

    private void setDates(WitPOI poi){
       String s =  poi.getDate();
        Date d=null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
        try {
            d = sdf.parse(s);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(startDate!=null) {
            if (d.before(startDate)) {
                startDate = d;
            }
        }
        else{
            startDate = d;
            }
        if(endDate!=null) {
            if (d.after(endDate)) {
                endDate = d;
            }
        }
        else{
            endDate = d;
                }

    }

    public boolean contains(WitPOI poi){
        if(poisList.contains(poi)){
            return true;
        }
        return false;
    }

    public int size(){
        return poisList.size();
    }

    public ArrayList<WitPOI> getPois() {
        return poisList;
    }

    public Date getStartDate(){
        return startDate;
    }
    public Date getEndDate(){
        return endDate;
    }
    public WitCity getCity(){

        return citiesList.get(0);
    }
    public void addCity(WitCity city){
        citiesList.add(city);
        nameJourney = city.getCountry();
    }

    public ArrayList<WitCity> getCities(){

        return citiesList;
    }

    public String getNameJourney(){

        return nameJourney;
    }

    // I METODI seguenti servono per renderlo serializzabile e passarlo tra activity diverse

    public static final Parcelable.Creator<WitJourney> CREATOR
            = new Parcelable.Creator<WitJourney>() {
        public WitJourney createFromParcel(Parcel in) {
            return new WitJourney(in);
        }

        public WitJourney[] newArray(int size) {
            return new WitJourney[size];
        }
    };


    /**
     * Ricrea un oggetto WitJourney leggendo da un Parcel
     *
     * @param in
     */
    private WitJourney(Parcel in) {
        startDate = new Date(in.readLong());
        endDate = new Date(in.readLong());
        nameJourney = in.readString();
        citiesList = new ArrayList<WitCity>();
        poisList = new ArrayList<WitPOI>();
        in.readTypedList(citiesList, WitCity.CREATOR);
        in.readTypedList(poisList, WitPOI.CREATOR);
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
        dest.writeLong(startDate.getTime());
        dest.writeLong(endDate.getTime());
        dest.writeString(nameJourney);
        dest.writeTypedList(citiesList);
        dest.writeTypedList(poisList);


    }


}
