package fr.ynov.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by laurent on 09/02/2016.
 */
public class WeatherProvider extends ContentProvider {

    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;
    static final int LOCATION_ID = 301;


    private static final UriMatcher matcher = buildUriMatcher();

    private WeatherDbHelper mOpenHelper;

    private static final SQLiteQueryBuilder mWeatherByLocationSettingQueryBuilder;

    static {
        mWeatherByLocationSettingQueryBuilder = new SQLiteQueryBuilder();
        mWeatherByLocationSettingQueryBuilder.setTables(WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                WeatherContract.LocationEntry.TABLE_NAME +
                " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                " = " + WeatherContract.LocationEntry.TABLE_NAME +
                "." + WeatherContract.LocationEntry._ID);
    }

    private static final String mLocationSettingSelection = WeatherContract.LocationEntry.TABLE_NAME + "." +
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";

    private static final String mLocationSettingWithStartDateSelection = WeatherContract.LocationEntry.TABLE_NAME + "." +
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?  AND " + WeatherContract.WeatherEntry.COLUMN_DATE + " >= ? ";

    private static final String mLocationSettingWithDaySelection = WeatherContract.LocationEntry.TABLE_NAME + "." +
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " + WeatherContract.WeatherEntry.COLUMN_DATE + " = ? ";



    private Cursor getWeatherByLocationSetting(Uri uri, String[] projection, String sortOrder) {
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        long startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if(startDate == 0){
            selection = mLocationSettingSelection;
            selectionArgs = new String[]{locationSetting};
        }else{
            selectionArgs = new String[]{locationSetting,Long.toString(startDate)};
            selection = mLocationSettingWithStartDateSelection;
        }

        return mWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }


    private Cursor getWeatherByLocationSettingAndDate(Uri uri, String[] projection, String sortOrder){
        long day = WeatherContract.WeatherEntry.getDateFromUri(uri);
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);

        return mWeatherByLocationSettingQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                mLocationSettingWithDaySelection,
                new String[]{locationSetting,Long.toString(day)},
                null,
                null,
                sortOrder);
    }



    private static UriMatcher buildUriMatcher()
    {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        String authority = WeatherContract.CONTENT_AUTHORITY;

        matcher.addURI(authority,WeatherContract.PATH_WEATHER,WEATHER);
        matcher.addURI(authority,WeatherContract.PATH_WEATHER + "/*",WEATHER_WITH_LOCATION);
        matcher.addURI(authority,WeatherContract.PATH_WEATHER + "/*/*",WEATHER_WITH_LOCATION_AND_DATE);

        matcher.addURI(authority,WeatherContract.PATH_LOCATION,LOCATION);
        matcher.addURI(authority,WeatherContract.PATH_LOCATION + "/#",LOCATION_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (matcher.match(uri)){
            case WEATHER:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case WEATHER_WITH_LOCATION:
            {
                retCursor = getWeatherByLocationSetting(uri,projection,sortOrder);
                break;
            }
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = getWeatherByLocationSettingAndDate(uri,projection,sortOrder);
                break;
            }
            case LOCATION:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.LocationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case LOCATION_ID:
            {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WeatherContract.WeatherEntry.TABLE_NAME,
                        projection,
                        WeatherContract.LocationEntry._ID + " = '" + ContentUris.parseId(uri) + "';",
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            default:
            {
                throw new UnsupportedOperationException("unknown Uri: " + uri);
            }
        }
        retCursor.setNotificationUri(getContext().getContentResolver(),uri);

        return retCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        int match = matcher.match(uri);
        switch (match){
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("unknown Uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri returnUri;
        final int match = matcher.match(uri);
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (match) {
            case WEATHER: {
                long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME,null,values);
                if(_id > 0){
                    returnUri = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                }else{
                    throw new android.database.SQLException("Fail to insert row into " + uri);
                }
                break;
            }
            case LOCATION: {
                long _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME,null,values);
                if(_id > 0){
                    returnUri = WeatherContract.LocationEntry.buildLocationUri(_id);
                }else{
                    throw new android.database.SQLException("Fail to insert row into " + uri);
                }
                break;
            }
            default:{
                returnUri = null;
                break;
            }
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final int match = matcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int affectedRowsCount = 0;
        switch (match){
            case WEATHER:
            {
                affectedRowsCount = db.delete(WeatherContract.WeatherEntry.TABLE_NAME,selection,selectionArgs);
                break;
            }
            case LOCATION:
            {
                affectedRowsCount = db.delete(WeatherContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            default:
                break;
        }
        if(selection == null || affectedRowsCount != 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }

        return affectedRowsCount;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final int match = matcher.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int affectedRowsCount = 0;
        switch (match){
            case WEATHER:
            {
                affectedRowsCount = db.update(WeatherContract.WeatherEntry.TABLE_NAME,values,selection,selectionArgs);
                break;
            }
            case LOCATION:
            {
                affectedRowsCount = db.update(WeatherContract.LocationEntry.TABLE_NAME,values,selection,selectionArgs);
                break;
            }
            default:
                break;
        }
        if(affectedRowsCount != 0)
            getContext().getContentResolver().notifyChange(uri,null);

        return affectedRowsCount;
    }


    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = matcher.match(uri);
        switch (match){
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues contentValues : values) {
                        long _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
                        if (_id != 1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();

                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }
}
