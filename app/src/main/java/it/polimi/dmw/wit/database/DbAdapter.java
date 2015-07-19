package it.polimi.dmw.wit.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;

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


    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_SURNAME = "surname";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FB = "fb";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_DATE = "date";
    public static final String KEY_ISLOGGED = "isLogged";

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

    private ContentValues createContentValuesPOI(String name, String description, String date, byte[] img) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        values.put(KEY_DATE, date);
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

    public void savePOI(String name, String description, String date, byte[] img) {
        ContentValues initialValues = createContentValuesPOI(name, description, date, img);
         database.insertOrThrow(DATABASE_POI, null, initialValues);

    }

    public void saveUser(long id, String name, String surname, byte[] img, Boolean fb, Boolean isLogged) {
        ContentValues initialValues = createContentValuesUSER(id, name, surname, img, fb, isLogged);
        database.insertOrThrow(DATABASE_USER, null, initialValues);

    }

    public boolean updatePOI(long id, String name, String description, String date, byte[] img) {
        ContentValues updateValues = createContentValuesPOI(name, description, date, img);
        return database.update(DATABASE_POI, updateValues, KEY_ID + "=" + id, null) > 0;
    }

    public boolean updateUSER(long id, String name, String surname, byte[] img, Boolean fb, Boolean isLogged) {
        ContentValues updateValues = createContentValuesUSER(id, name, surname, img, fb, isLogged);
        return database.update(DATABASE_USER, updateValues, KEY_ID + "=" + id, null) > 0;

    }
    public void logoutUSER(long id) {
         database.execSQL("UPDATE userProfile SET isLogged = 0 where _id="+id+"");


    }

    //delete a contact
    public boolean deletePOI(long id) {
        return database.delete(DATABASE_POI, KEY_ID + "=" + id, null) > 0;
    }

    public boolean deleteUser(long id) {
        return database.delete(DATABASE_POI, KEY_ID + "=" + id, null) > 0;
    }

    //fetch all contacts
    public Cursor fetchAllPOIs() {
        return database.query(DATABASE_POI, new String[]{KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE, KEY_IMAGE}, null, null, null, null, null);
    }


    public Cursor fetchAllUSERS() {
        return database.query(DATABASE_USER, new String[]{KEY_ID, KEY_NAME, KEY_SURNAME, KEY_IMAGE, KEY_FB, KEY_ISLOGGED}, null, null, null, null, null,null);
    }

    //fetch contacts filter by a string
    public Cursor fetchPOIsByID(int filter) {
        Cursor mCursor = database.query(true, DATABASE_POI, new String[]{
                        KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE, KEY_IMAGE},
                KEY_ID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }

    public Cursor fetchUserByID(long filter) {
        Cursor mCursor = database.query(true, DATABASE_USER, new String[]{
                        KEY_ID, KEY_NAME, KEY_SURNAME, KEY_IMAGE, KEY_FB, KEY_ISLOGGED},
                KEY_ID + "=" + filter, null, null, null, null, null);

        return mCursor;
    }

}
