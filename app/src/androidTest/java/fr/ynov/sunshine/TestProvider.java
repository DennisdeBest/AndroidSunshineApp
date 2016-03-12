package fr.ynov.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

import fr.ynov.sunshine.data.WeatherContract;

/**
 * Created by laurent on 09/02/2016.
 */
public class TestProvider extends AndroidTestCase {


    public void testDeleteAllRecord(){
        deleteAllRecords();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    public void deleteAllRecords() {
        mContext.getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,null,null);
        mContext.getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI, null, null);

        Cursor cursor = mContext.getContentResolver().query(WeatherContract.WeatherEntry.CONTENT_URI,null,null,null,null);
        assertEquals(cursor.getCount(), 0);
        cursor.close();

        cursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,null,null,null,null);
        assertEquals(cursor.getCount(),0);
        cursor.close();
    }


    public String testCityName = "Bordeaux";
    public String testLocation = "Bordeaux";
    public long testDate = 1419033600L;

    ContentValues getLocationContentValues(){
        String testLocationSetting = "Bordeaux";
        double testLatitude = 64.772;
        double testLongitude = -147.233;

        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,testCityName);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,testLocationSetting);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,testLatitude);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,testLongitude);
        return contentValues;
    }

    ContentValues getWeatherContentValuesForLocation(long locationRowId){
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY,locationRowId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE,testDate);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES,1.1);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,1.2);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,1.3);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,15);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,10);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,"Asteroids");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,5.5);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,321);
        return weatherValues;
    }


    static public void validateCursor(ContentValues expectedValues, Cursor valueCursor){

        Set<Map.Entry<String,Object>> valueSet = expectedValues.valueSet();

        for(Map.Entry<String,Object> entry : valueSet){
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(-1 == idx);
            String expectedValue = entry.getValue().toString();
            String fetchedValue = valueCursor.getString(idx);
            assertEquals(expectedValue,fetchedValue);
        }
    }


    public void testGetType() {

        String type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.CONTENT_URI);
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "BORDEAUX";
        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals(WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE,type);

        type = mContext.getContentResolver().getType(WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(testLocation, testDate));
        assertEquals(WeatherContract.WeatherEntry.CONTENT_TYPE,type);

        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.CONTENT_URI);
        assertEquals(WeatherContract.LocationEntry.CONTENT_TYPE,type);

        type = mContext.getContentResolver().getType(WeatherContract.LocationEntry.buildLocationUri(1L));
        assertEquals(WeatherContract.LocationEntry.CONTENT_ITEM_TYPE,type);

    }




    public void testInsertReadProvider() {

        ContentValues locationContentValues= getLocationContentValues();
        //now insert these data into the base
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locationContentValues);
        long locationRowId = ContentUris.parseId(locationUri);
        //verify we got a row back
        assertTrue(locationRowId != -1);

        //Now test than we can read the base thruth the Content Provider
        Cursor locationCursor = mContext.getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );


        if(locationCursor.moveToFirst()) {
            //Validate the Weather returned by our Content Provider match the data we inserted into the base
            validateCursor(locationContentValues, locationCursor);

            //Tests d'écriture table Weather
            ContentValues weatherValues = getWeatherContentValuesForLocation(locationRowId);
            Uri weatherUri = mContext.getContentResolver().insert(WeatherContract.WeatherEntry.CONTENT_URI, weatherValues);
            long weatherRowId = ContentUris.parseId(weatherUri);
            assertTrue(weatherRowId != -1);

            validateWeatherRead(weatherValues, WeatherContract.WeatherEntry.CONTENT_URI);

            validateWeatherRead(weatherValues, WeatherContract.WeatherEntry.buildWeatherLocation(testLocation));

            validateWeatherRead(weatherValues, WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(testLocation, testDate));

            //validateWeatherRead(weatherValues, WeatherContract.WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));

        }else{
            fail("No value returned");
        }
    }

    @NonNull
    private void validateWeatherRead(ContentValues weatherValues,Uri requestUri) {
        //Test de lecture table Weather.
        Cursor weatherCursor = mContext.getContentResolver().query(requestUri,
                null,//leaving column null just return all
                null,//cols for where clause
                null,//values for where clause
                null//sort order
                );

        if(weatherCursor.moveToFirst()){
            validateCursor(weatherValues, weatherCursor);
        }else{
            fail("No value returned");
        }
        weatherCursor.close();
    }

    public void testUpdate() {

        ContentValues locationContentValues = getLocationContentValues();
        Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, locationContentValues);
        long locationRowId = ContentUris.parseId(locationUri);
        assertTrue(locationRowId != -1);

        ContentValues updatedContentValues  = new ContentValues(locationContentValues);
        updatedContentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, "Paris");

        int updatedRows = getContext().getContentResolver().update(WeatherContract.LocationEntry.CONTENT_URI,
                updatedContentValues,
                WeatherContract.LocationEntry._ID + "= ?",
                new String[] {Long.toString(locationRowId)});

        assertTrue(updatedRows == 1);

        Cursor cursor = getContext().getContentResolver().query(WeatherContract.LocationEntry.CONTENT_URI,
                null,
                WeatherContract.LocationEntry._ID + " = " + locationRowId,
                null,
                null);

        if(cursor.moveToFirst()){
            validateCursor(updatedContentValues,cursor);
        }else{
            fail("No value returned");
        }

    }
}


