package com.oose.postop.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;

import java.text.DateFormat;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.oose.postop.dao.DeviceIdDAO;
import com.oose.postop.helpers.ConnectionHelper;
import com.oose.postop.receivers.PushNotificationAlarm;

import org.json.JSONException;
import org.json.JSONObject;

/**Service to get google fit data
 * Created by Omotola on 11/9/2017.
 */

public class GoogleFitFetchService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String TAG = "BasicHistoryApi";
    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean authInProgress = false;

    public static GoogleApiClient googleApiClient = null;
    public static int dailyTotalCount = 0;
    public static int dailyCaloriesExpended = 0;
    public static float angle = 0;
    ConnectionHelper connectionHelper = new ConnectionHelper();
 /*if (savedInstanceState != null) {
        authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
    }*/


  //  public void buildFitnessClient(Context c) {

   // }

    /**
     * Invoked when service is started
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(this)
                //.enableAutoManage(this, 0, this)
                .build();
        googleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Invoked when connected to google fit
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected");
        new ReadFitnessData().execute();
    }


    /**
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Asynchronous task to connect to google fit api and retrieve fitness data in the background
     */
    private class ReadFitnessData extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            getFitnessDataForToday();
            return null;
        }

        /**
         * Invoked after the background tasks is finishes
         * @param aVoid
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            fetchFitData(dailyTotalCount,10000);
            buildRequest();
        }
    }

    /**
     *  retrieves the raw fitness data
     */
    public void getFitnessDataForToday(){
        DailyTotalResult resultSteps = Fitness.HistoryApi.
                readDailyTotal(googleApiClient, DataType.TYPE_STEP_COUNT_DELTA).await(1, TimeUnit.MINUTES);
        DataSet dataSet = resultSteps.getTotal();

        DateFormat dateFormat = DateFormat.getDateTimeInstance();

        for(DataPoint dp: dataSet.getDataPoints()){
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field: dp.getDataType().getFields()){
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
                dailyTotalCount = dp.getValue(field).asInt();
                Log.i(TAG, " Total Steps " + dailyTotalCount);

            }
        }

        DailyTotalResult resultCalories = Fitness.HistoryApi.
                readDailyTotal(googleApiClient, DataType.TYPE_CALORIES_EXPENDED).await(1, TimeUnit.MINUTES);
        dataSet = resultCalories.getTotal();

        for(DataPoint dp: dataSet.getDataPoints()){
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field: dp.getDataType().getFields()){
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
                dailyCaloriesExpended = (int)dp.getValue(field).asFloat();
                Log.i(TAG, " Calories Expended " + dailyCaloriesExpended);

            }
        }
    }

    /**
     * Invoked when connection is suspended
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.i(TAG, "Connection lost. Cause: Network Lost.");
        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.i(TAG, "Connection lost. Reason: Service Disconnected");
        }
    }


    /**
     * Invoked when connection fails
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Google Play services connection failed. Cause: " +
                connectionResult.toString());
    }

    /**
     * Fetch google fit data
     */

    public void fetchFitData(int steps, int total){
        int stepCount = steps;
        int totalCount = total;
        angle = ((float)stepCount/(float)totalCount)*360;
        //return angle;
    }

    public void buildRequest(){
    DeviceIdDAO d = new DeviceIdDAO(this);
    String id = d.retrieveID();

    //g.buildFitnessClient(context);
    Calendar c = Calendar.getInstance();
    //
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    String formattedDate = df.format(c.getTime());
    //Toast.makeText(context, formattedDate.toString(), Toast.LENGTH_LONG).show();

    JSONObject j = new JSONObject();
        try {
        j.put("id", id);
        j.put("captureDate", formattedDate);
        j.put("stepCount", dailyTotalCount);
        j.put("caloriesExpended", dailyCaloriesExpended);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    //  Toast.makeText(context, g.dailyTotalCount, Toast.LENGTH_LONG).show();
    volleyRequest(j, this);

}


    /**
     * Sends Volley Request
     * @param jsonRequestObject
     * @param context
     */
    public void volleyRequest(JSONObject jsonRequestObject, final Context context) {


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        final String url = connectionHelper.getRetrieveFitUrl();

        // Request a string response from the provided URL.
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonRequestObject, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            String n = response.get("notificationCount").toString();
                            Toast.makeText(context, "Count ="+n.toString(), Toast.LENGTH_LONG).show();
                            calculateInterval(Integer.parseInt(n));
                        } catch (Exception e) {
                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //mTextView.setText("That didn't work!");
                        Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();


                    }
                });
        queue.add(jsObjRequest);


    }


    /**
     * Calculates push notification interval
     * @param notificationCount
     */
    public void calculateInterval(int notificationCount){
        //int interval = (14*60)/notificationCount;
        int interval = 30;
        PushNotificationAlarm a = new PushNotificationAlarm();
        a.setAlarm(getApplicationContext(), interval);

}
}