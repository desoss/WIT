package it.polimi.dmw.wit.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;
import android.util.Log;

import java.sql.Blob;

public class DbAdapter {
    @SuppressWarnings("unused")
    private static final String LOG_TAG = DbAdapter.class.getSimpleName();

    private Context context;
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    // Database fields
    private static final String DATABASE_POI = "pois";
    private static final String DATABASE_TABLE2 = "pois2"; //senza duplicati
    private static final String DATABASE_USER = "userProfile";
    private static final String DATABASE_SETTINGS = "settings";
    private static final String DATABASE_CITY_INFO = "cityInfo";
    private static final String DATABASE_BEST_FIVE = "bestFive";



    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_SURNAME = "surname";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_THUMBNAIL = "thumbnail";
    public static final String KEY_FB = "fb";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_DATE = "date";
    public static final String KEY_ISLOGGED = "isLogged";
    public static final String KEY_CITY = "city";
    public static final String KEY_COUNTY = "county";
    public static final String KEY_STATE = "state";
    public static final String KEY_COUNTRY = "country";
    public static final String KEY_WOEID = "woeid";
    public static final String KEY_WIKIMAPIAID = "wikimapiaId";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LON = "lon";


    public DbAdapter(Context context) {
        this.context = context;
    }

    public DbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    private ContentValues createContentValuesPOI(int id, String name, String description, String date, int woeid, double lat, double lon, byte[] img, byte[] thumbnail) {
        ContentValues values = new ContentValues();
        values.put(KEY_WIKIMAPIAID,id);
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        values.put(KEY_DATE, date);
        values.put(KEY_WOEID, woeid);
        values.put(KEY_LAT, lat);
        values.put(KEY_LON, lon);
        values.put(KEY_IMAGE, img);
        values.put(KEY_THUMBNAIL, thumbnail);


        return values;
    }
    private ContentValues createContentValuesBESTFIVE(int id, String name, String description, double lat, double lon, byte[] img) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        values.put(KEY_IMAGE, img);
        values.put(KEY_WIKIMAPIAID,id);
        values.put(KEY_LAT, lat);
        values.put(KEY_LON, lon);
        values.put(KEY_IMAGE, img);


        return values;
    }

    private ContentValues createContentValuesUSER(long id, String name, String surname, byte[]img, Boolean fb, Boolean isLogged) {
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_NAME, name);
        values.put(KEY_SURNAME, surname);
        values.put(KEY_IMAGE, img);
        values.put(KEY_FB, fb);
        values.put(KEY_ISLOGGED, isLogged);


        return values;
    }

    private ContentValues createContentValuesSETTINGS(long id, Boolean fb) {
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(KEY_FB, fb);

        return values;
    }

    private ContentValues createContentValuesCITYINFO(String city, String county, String state, String country, byte[]img, byte[] thumbnail) {
        ContentValues values = new ContentValues();
        values.put(KEY_CITY, city);
        values.put(KEY_COUNTY, county);
        values.put(KEY_STATE, state);
        values.put(KEY_COUNTRY, country);
        values.put(KEY_IMAGE, img);
        values.put(KEY_THUMBNAIL, thumbnail);

        return values;
    }

    public void savePOI(int id, String name, String description, String date, int woeid, double lat, double lon, byte[] img, byte[] thumbnail) {
        ContentValues initialValues = createContentValuesPOI(id, name, description, date, woeid, lat, lon, img, thumbnail);
         database.insertOrThrow(DATABASE_POI, null, initialValues);
    }

    public void saveBestFive(int id, String name, String description, double lat, double lon, byte[] img) {
        ContentValues initialValues = createContentValuesBESTFIVE(id, name, description, lat, lon, img);
        database.insertOrThrow(DATABASE_BEST_FIVE, null, initialValues);
    }

    public void saveUser(long id, String name, String surname, byte[] img, Boolean fb, Boolean isLogged) {
        ContentValues initialValues = createContentValuesUSER(id, name, surname, img, fb, isLogged);
        database.insertOrThrow(DATABASE_USER, null, initialValues);
    }

    public void saveSettings(long id, Boolean fb) {
        ContentValues initialValues = createContentValuesSETTINGS(id, fb);
        database.insertOrThrow(DATABASE_SETTINGS, null, initialValues);
    }

    public void saveCityInfo(String city, String county, String state, String country, byte[]img, byte[] thumbnail) {
        ContentValues initialValues = createContentValuesCITYINFO(city, county, state, country, img, thumbnail);
        database.insertOrThrow(DATABASE_CITY_INFO, null, initialValues);
    }

    public boolean updatePOI(int id, String name, String description, String date, int woeid, double lat, double lon, byte[] img, byte[] thumbnail) {
        ContentValues updateValues = createContentValuesPOI(id, name, description, date, woeid, lat, lon, img, thumbnail);
        return database.update(DATABASE_POI, updateValues, KEY_WIKIMAPIAID + "=" + id, null) > 0;
    }

    public boolean updateUSER(long id, String name, String surname, byte[] img, Boolean fb, Boolean isLogged) {
        ContentValues updateValues = createContentValuesUSER(id, name, surname, img, fb, isLogged);
        return database.update(DATABASE_USER, updateValues, KEY_ID + "=" + id, null) > 0;

    }
    public void logoutUSER(long id) {
         database.execSQL("UPDATE userProfile SET isLogged = 0 where _id="+id+"");

    }

    public boolean updateSETTINGS(long id, Boolean fb) {
        ContentValues updateValues = createContentValuesSETTINGS(id, fb);
        return database.update(DATABASE_SETTINGS, updateValues, KEY_ID + "=" + id, null) > 0;

    }

    //delete a contact
    public boolean deletePOI(long id) {
        return database.delete(DATABASE_POI, KEY_ID + "=" + id, null) > 0;
    }
    public void deleteBestFive() {
         database.execSQL("DROP TABLE IF EXISTS "+DATABASE_BEST_FIVE);
         database.execSQL(dbHelper.DATABASE_BEST_FIVE);

        Log.d("db", "drop");

    }

    public boolean deleteUser(long id) {
        return database.delete(DATABASE_POI, KEY_ID + "=" + id, null) > 0;
    }

    //fetch all contacts
    public Cursor fetchAllPOIs() {
        return database.query(DATABASE_POI, new String[]{KEY_ID,  KEY_WIKIMAPIAID,KEY_NAME, KEY_DESCRIPTION, KEY_DATE, KEY_WOEID, KEY_LAT, KEY_LON, KEY_IMAGE, KEY_THUMBNAIL}, null, null, null, null, null);
    }

    public Cursor fetchBestFive() {
        return database.query(DATABASE_BEST_FIVE, new String[]{KEY_ID, KEY_WIKIMAPIAID,KEY_NAME, KEY_DESCRIPTION, KEY_LAT, KEY_LON, KEY_IMAGE}, null, null, null, null, null);
    }


    public Cursor fetchAllUSERS() {
        return database.query(DATABASE_USER, new String[]{KEY_ID, KEY_NAME, KEY_SURNAME, KEY_IMAGE, KEY_FB, KEY_ISLOGGED}, null, null, null, null, null,null);
    }

    public Cursor fetchSETTINGS() {
        return database.query(DATABASE_SETTINGS, new String[]{KEY_ID, KEY_FB}, null, null, null, null, null,null);
    }

    public Cursor fetchCITYINFO() {
        return database.query(DATABASE_CITY_INFO, new String[]{KEY_ID, KEY_CITY, KEY_COUNTY, KEY_STATE, KEY_COUNTRY, KEY_IMAGE, KEY_THUMBNAIL}, null, null, null, null, null,null);
    }

    //fetch contacts filter by a string
    public Cursor fetchPOIsByID(int filter) {
        Cursor mCursor = database.query(true, DATABASE_POI, new String[]{
                        KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE, KEY_WOEID, KEY_LAT, KEY_LON, KEY_IMAGE, KEY_THUMBNAIL},
                KEY_ID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }

    public Cursor fetchUserByID(long filter) {
        Cursor mCursor = database.query(true, DATABASE_USER, new String[]{
                        KEY_ID, KEY_NAME, KEY_SURNAME, KEY_IMAGE, KEY_FB, KEY_ISLOGGED},
                KEY_ID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }

    public Cursor fetchBestFiveByID(int filter) {
        Cursor mCursor = database.query(true, DATABASE_BEST_FIVE, new String[]{
                KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_LAT, KEY_LON, KEY_IMAGE},
                KEY_WIKIMAPIAID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }

    public Cursor fetchCityByID(int filter){
        Cursor mCursor = database.query(true, DATABASE_CITY_INFO, new String[]{
                        KEY_ID, KEY_CITY, KEY_COUNTY, KEY_STATE, KEY_COUNTRY, KEY_IMAGE, KEY_THUMBNAIL},
                KEY_ID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }
    public Cursor fetchCityByCCSC(String a, String b, String c, String d){
        Cursor mCursor = database.query(true, DATABASE_CITY_INFO, new String[]{
                        KEY_ID, KEY_CITY, KEY_COUNTY, KEY_STATE, KEY_COUNTRY, KEY_IMAGE, KEY_THUMBNAIL},
                 KEY_CITY + "='" + a +"' AND "+ KEY_COUNTY + "='" + b+"' AND "+ KEY_STATE + "='" + c+"' AND "+ KEY_COUNTRY + "='" + d+"'",null, null, null, null, null);

        return mCursor;
    }

    public Cursor fetchPOIByWoeid(int filter){
        Cursor mCursor = database.query(true, DATABASE_POI, new String[]{
                        KEY_ID, KEY_WIKIMAPIAID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE, KEY_WOEID,  KEY_LAT, KEY_LON,KEY_IMAGE, KEY_THUMBNAIL},
                KEY_WOEID + "=" + filter, null, null, null, null, null, null);

        return mCursor;
    }

}
