package fr.ynov.sunshine;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

import fr.ynov.sunshine.data.WeatherContract;
import fr.ynov.sunshine.data.WeatherDbHelper;

/**
 * Created by laurent on 09/02/2016.
 */
public class TestDb extends AndroidTestCase {


    void deleteTheDatabase(){
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTheDatabase();
    }

    public void testCreateDb() {
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }


    public String cityName = "Bordeaux";

    ContentValues getLocationContentValues(){
        String testLocationSetting = "Bordeaux";
        double testLatitude = 64.772;
        double testLongitude = -147.233;

        ContentValues contentValues = new ContentValues();
        contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,testLocationSetting);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,testLatitude);
        contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,testLongitude);
        return contentValues;
    }

    static public void validateCursor(ContentValues expectedValues, Cursor valueCursor){

        Set<Map.Entry<String,Object>> valueSet = expectedValues.valueSet();

        for(Map.Entry<String,Object> entry : valueSet){
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse(-1 == idx);
            String expectedValue = entry.getValue().toString();
            assertEquals(expectedValue,valueCursor.getString(idx));
        }
    }


    public void testInsertReadDb() {

        //This is test data that we will insert in our Location table


        //get a writable database thru db Helper
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());



        ContentValues contentValues = getLocationContentValues();
        //now insert these data into the base
        long locationRowId;
        locationRowId = db.insert(WeatherContract.LocationEntry.TABLE_NAME,null,contentValues);

        //verify we got a row back
        assertTrue(locationRowId != -1);

        //Now test than we can read the base


        Cursor cursor = db.query(WeatherContract.LocationEntry.TABLE_NAME,//table name to query
                null,
                null,//columns for the where clause
                null,//values for the where clause
                null,//columns to group by
                null, //columns to filter by rows group
                null //sort order
        );

        if(cursor.moveToFirst()){
            validateCursor(contentValues,cursor);


            //Tests d'écriture table Weather
            ContentValues weatherValues = new ContentValues();
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOC_KEY,locationRowId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE,"01012016");
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES,1.1);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY,1.2);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE,1.3);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,15);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,10);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,"Asteroids");
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,5.5);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,321);

            long weatherRowId;
            weatherRowId = db.insert(WeatherContract.WeatherEntry.TABLE_NAME,null,weatherValues);
            assertTrue(weatherRowId != -1);


            //Test de lecture table Weather.
            Cursor weatherCursor = db.query(
                    WeatherContract.WeatherEntry.TABLE_NAME,
                    null,//leaving the columns null return all the columns
                    null,
                    null,
                    null,
                    null,
                    null
            );

            if(weatherCursor.moveToFirst()){

                int dateIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
                String date = weatherCursor.getString(dateIndex);

                int degreesIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DEGREES);
                double degrees = weatherCursor.getDouble(degreesIndex);

                int humidityIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_HUMIDITY);
                double humidity = weatherCursor.getDouble(humidityIndex);

                int pressureIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_PRESSURE);
                double pressure = weatherCursor.getDouble(pressureIndex);

                int maxTempIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP);
                double maxTemp = weatherCursor.getDouble(maxTempIndex);

                int minTempIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP);
                double minTemp = weatherCursor.getDouble(minTempIndex);

                int shortDescIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC);
                String shortDesc = weatherCursor.getString(shortDescIndex);

                int windSpeedIndex = weatherCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED);
                double windSpeed = weatherCursor.getDouble(windSpeedIndex);

            }else{
                fail("No value returned");
            }

        }else{
            fail("No value returned");
        }
    }
}
