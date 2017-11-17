package com.oose.postop.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.oose.postop.data.Database;

/**SqlLiteHelper class for Database
 * Created by Omotola on 10/26/2017.
 */

public class DatabaseHelper  extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PostOpDatabase";

    /**
     * Constructor
     * @param context
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * Invoked On Create of a database
     * @param sqLiteDatabase
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(Database.DeviceId.CREATE_TABLE);
    }

    /**
     * Invoked on upgrade of a databasee
     * @param sqLiteDatabase
     * @param i
     * @param i1
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Database.DeviceId.TABLE_NAME);
        onCreate(sqLiteDatabase);


    }





}
