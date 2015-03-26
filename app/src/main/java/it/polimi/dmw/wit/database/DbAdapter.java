package it.polimi.dmw.wit.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;

public class DbAdapter {
    @SuppressWarnings("unused")
    private static final String LOG_TAG = DbAdapter.class.getSimpleName();

    private Context context;
    private SQLiteDatabase database;
    private DatabaseHelper dbHelper;

    // Database fields
    private static final String DATABASE_TABLE = "pois";

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_DATE = "date";

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

    private ContentValues createContentValues(String name, String description, String date) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_DESCRIPTION, description);
        values.put(KEY_DATE, date);

        return values;
    }

    //create a contact
    public long savePOI(String name, String description, String date) {
        ContentValues initialValues = createContentValues(name, description, date);
        return database.insertOrThrow(DATABASE_TABLE, null, initialValues);
    }

    //update a contact
    public boolean updatePOI(long id, String name, String description, String date) {
        ContentValues updateValues = createContentValues(name, description, date);
        return database.update(DATABASE_TABLE, updateValues, KEY_ID + "=" + id, null) > 0;
    }

    //delete a contact
    public boolean deletePOI(long id) {
        return database.delete(DATABASE_TABLE, KEY_ID + "=" + id, null) > 0;
    }

    //fetch all contacts
    public Cursor fetchAllPOIs() {
        return database.query(DATABASE_TABLE, new String[]{KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE}, null, null, null, null, null);
    }

    //fetch contacts filter by a string
    public Cursor fetchPOIsByID(int filter) {
        Cursor mCursor = database.query(true, DATABASE_TABLE, new String[]{
                        KEY_ID, KEY_NAME, KEY_DESCRIPTION, KEY_DATE},
                KEY_ID + " like '%" + filter + "%'", null, null, null, null, null);

        return mCursor;
    }
}
