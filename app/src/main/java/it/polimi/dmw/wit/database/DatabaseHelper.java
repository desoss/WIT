package it.polimi.dmw.wit.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "wit.db";
    private static final int DATABASE_VERSION = 1;

    // Lo statement SQL di creazione del database
    private static final String DATABASE_POIS = "create table pois (_id integer primary key autoincrement, name text not null, description text not null,date text not null, image blob);";
    private static final String DATABASE_CREATE2 = "create table pois2 (_id integer primary key autoincrement, name text not null, description text not null,date text not null);";
    private static final String DATABASE_USER_PROFILE = "create table userProfile (_id long primary key, name text not null, surname text not null,image blob, fb boolean not null, isLogged boolean not null);";
    private static final String DATABASE_SETTINGS = "create table settings (_id integer primary key,fb boolean not null);";

    // Costruttore
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Questo metodo viene chiamato durante la creazione del database
    @Override
    public void onCreate(SQLiteDatabase database) {

       database.execSQL(DATABASE_POIS);
       database.execSQL(DATABASE_USER_PROFILE);

    }

    // Questo metodo viene chiamato durante l'upgrade del database, ad esempio quando viene incrementato il numero di versione
    @Override
    public void onUpgrade( SQLiteDatabase database, int oldVersion, int newVersion ) {

        database.execSQL("DROP TABLE IF EXISTS contact");
        onCreate(database);

    }
}
