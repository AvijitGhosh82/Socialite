package com.nemesis.minisocialnetwork.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.nemesis.minisocialnetwork.Post;
import com.nemesis.minisocialnetwork.R;
import com.nemesis.minisocialnetwork.data.TimeLineProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class TimeLineSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = TimeLineSyncAdapter.class.getSimpleName();
    static String token;
    Post[] postarray;

    public static final int SYNC_INTERVAL = 60 * 15;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;

    public TimeLineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        TimeLineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }


    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;


        try {

            URL url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/"+token);

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
            }
            forecastJsonStr = buffer.toString();

            try {
                postarray = getWeatherDataFromJson(forecastJsonStr);
            } catch (JSONException j) {
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }


        Intent i = new Intent("SYNC_FINISHED");
        getContext().sendBroadcast(i);

    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static void initializeSyncAdapter(Context context, String Token) {
        getSyncAccount(context);
        token=Token;
    }


    private Post[] getWeatherDataFromJson(String forecastJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_HEAD = "feed_head";
        final String OWM_DATA = "feed_data";
        final String OWM_HEAD_TEXT = "post_text";
        final String OWM_HEAD_BY = "posted_by_name";
        final String OWM_HEAD_TIME = "timestamp";
        final String OWM_DATA_NUM_LIKES = "no_likes";
        final String OWM_HEAD_NUM_COMMENTS = "no_comments";
        final String OWM_DATA_COMMENTS = "comments";
        final String OWM_DATA_LIKES = "likes";
        final String OWM_HEAD_FID = "id";
        final String OWM_HEAD_UID = "posted_by";


        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray headArray = forecastJson.getJSONArray(OWM_HEAD);
        JSONArray dataArray = forecastJson.getJSONArray(OWM_DATA);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        //getActivity().getContentResolver().delete(TimeLineProvider.CONTENT_URI,null,null);

        Post[] resultp = new Post[headArray.length()];
        for (int i = 0; i < headArray.length(); i++) {

                /*String[] headby = new String[headArray.length()];
                String[] headtext= new String[headArray.length()];;
                String[] time= new String[headArray.length()];*/


            JSONObject head = headArray.getJSONObject(i);


            String headby = head.getString(OWM_HEAD_BY);
            String headtext = head.getString(OWM_HEAD_TEXT);
            String timest = head.getString(OWM_HEAD_TIME);

            JSONObject data = dataArray.getJSONObject(i);
            String likes=data.getString(OWM_DATA_LIKES);
            String numlikes = data.getString(OWM_DATA_NUM_LIKES);
            String numcomm = head.getString(OWM_HEAD_NUM_COMMENTS);
            String fid=head.getString(OWM_HEAD_FID);
            String uid=head.getString(OWM_HEAD_UID);

            resultp[i]=new Post(headby,headtext,timest,numcomm,numlikes,fid,uid,likes);

            ContentValues values = new ContentValues();

            values.put(TimeLineProvider.NAME,headby);
            values.put(TimeLineProvider.POST,headtext);
            values.put(TimeLineProvider.LIKES,numlikes);
            values.put(TimeLineProvider.UID,uid);
            values.put(TimeLineProvider.FID,fid);
            values.put(TimeLineProvider.COMMENTS,numcomm);


            try{
                final Uri uri = getContext().getContentResolver().insert(
                        TimeLineProvider.CONTENT_URI, values);}
            catch (Exception e)
            {
                //Only unique post ids are added in table
            }


        }


        return  resultp;

    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

        }
        return newAccount;
    }
}