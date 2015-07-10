package com.nemesis.minisocialnetwork;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.nemesis.minisocialnetwork.data.CursorLoaderAdapter;
import com.nemesis.minisocialnetwork.data.TimeLineProvider;
import com.nemesis.minisocialnetwork.sync.TimeLineSyncAdapter;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class HomeFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int FORECAST_LOADER = 0 ;
    private SwipeRefreshLayout swipeContainer;
    public ListView lv;
    private String uid,token;

    CursorLoaderAdapter adapter =null;

    private static final String[] PROJECTION = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            TimeLineProvider.STUDENTS_TABLE_NAME + "." + TimeLineProvider._ID,
            TimeLineProvider.NAME,
            TimeLineProvider.POST,
            TimeLineProvider.LIKES,
            TimeLineProvider.COMMENTS,
            TimeLineProvider.UID,
            TimeLineProvider.FID
    };


    OnItemClickedListener sDummyCallbacks = new OnItemClickedListener() {
        @Override
        public void onItemSelected(Bundle bundle) {

        }

    };
    private OnItemClickedListener mListener=sDummyCallbacks ;
    private Parcelable mListInstanceState;
    private int mCurCheckPosition;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
        outState.putParcelable("LIST_INSTANCE_STATE", lv.onSaveInstanceState());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(syncFinishedReceiver, new IntentFilter("SYNC_FINISHED"));
            lv.setItemChecked(mCurCheckPosition, true);

    }


    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(syncFinishedReceiver);
    }

    private BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(Const.TAG, "Sync finished, should refresh nao!!");
            swipeContainer.setRefreshing(false);

        }
    };

    void updateTimeline()
    {
        TimeLineSyncAdapter.syncImmediately(getActivity());

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //FragmentActivity    faActivity  = (FragmentActivity)    super.getActivity();
        // Replace LinearLayout by the type of the root element of the layout you're trying to load
        View v    = (View) inflater.inflate(R.layout.fragment_home, container, false);

        if(savedInstanceState!=null) {
            mListInstanceState = savedInstanceState.getParcelable("LIST_INSTANCE_STATE");
        }
        //llLayout.findViewById(R.id.someGuiElement);

        SharedPreferences pref = getActivity().getSharedPreferences("MyPref", getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        uid=pref.getString("uid", null);
        token=pref.getString("token", null);

        adapter=new CursorLoaderAdapter(getActivity(), null, token, isNetworkAvailable());


        swipeContainer = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                updateTimeline();
            }
        });

        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);



        FloatingActionButton newpost=(FloatingActionButton)v.findViewById(R.id.button_newpost);
        newpost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()){


                    final EditText input = new EditText(getActivity());


                    final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle("What would you like to post?")
                            .setView(input)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String m_Text = input.getText().toString();
                                    Toast.makeText(getActivity(), m_Text, Toast.LENGTH_SHORT).show();
                                    NewPostTask np = new NewPostTask(m_Text, "1", null);
                                    np.execute();
                                    dialog.cancel();

                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();

                    doKeepDialog(dialog);
                }
                else
                {Toast.makeText(getActivity(),"Sorry, app is offline.",Toast.LENGTH_SHORT).show();}
            }
        });



        lv = (ListView) v.findViewById(R.id.timelinelistview);


        setHasOptionsMenu(true);

        if(isNetworkAvailable())
            updateTimeline();
        else
            Toast.makeText(getActivity(),"App is offline.",Toast.LENGTH_SHORT).show();


        return v; // We must return the loaded Layout
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_home, menu);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);

    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = new CursorLoader(
                this.getActivity(),
                TimeLineProvider.CONTENT_URI,
                PROJECTION,
                null,
                null,
                null);
        return loader;
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

           adapter.swapCursor(cursor);
           lv.setAdapter(adapter);

        if(mListInstanceState!=null)
        {
            lv.onRestoreInstanceState(mListInstanceState);
            lv.setItemChecked(mCurCheckPosition, true);
            /*if (mCurCheckPosition != ListView.INVALID_POSITION) {
                // If we don't need to restart the loader, and there's a desired position to restore
                // to, do so now.
                lv.smoothScrollToPosition(mCurCheckPosition);
            }*/
        }

        cursor.moveToFirst();
        cursor.moveToPosition(0);
        int size=cursor.getCount();
        if(size!=0)
        {
            final String[] fidarray= new String[size];
            final String[] uidarray= new String[size];
            final String[] namearray= new String[size];
            final String[] postarray= new String[size];


            for(int i=0;i<size;i++){
                fidarray[i]=cursor.getString(cursor
                        .getColumnIndex(TimeLineProvider.FID));
                uidarray[i]=cursor.getString(cursor
                        .getColumnIndex(TimeLineProvider.UID));
                namearray[i]=cursor.getString(cursor
                        .getColumnIndex(TimeLineProvider.NAME));
                postarray[i]=cursor.getString(cursor
                        .getColumnIndex(TimeLineProvider.POST));
                cursor.moveToNext();
            }


        //mListener.OnItemClicked(Parameters params);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                Bundle bundle = new Bundle();
                bundle.putString("fid", fidarray[position]);
                bundle.putString("uid", uidarray[position]);
                bundle.putString("name", namearray[position]);
                bundle.putString("text", postarray[position]);
                mCurCheckPosition=position;
                mListener.onItemSelected(bundle);
            }
        });
    }}

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);

    }



    public interface OnItemClickedListener {
        void onItemSelected(Bundle bundle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnItemClickedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnItemClickedListener");
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static void doKeepDialog(Dialog dialog){
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(lp);
    }


    /*public class FetchWeatherTask extends AsyncTask<Void, Void, Post[]> {
        String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        String postcode = null;

        public FetchWeatherTask() {

        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }


        @Override
        protected Post[] doInBackground(Void... params) {

            Post[] postarray=null;

            SharedPreferences pref = getActivity().getSharedPreferences("MyPref", getActivity().MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            String jsondata=pref.getString("timeline_json", null);
            if(!isNetworkAvailable())
            {
                try {
                    if(jsondata!=null)
                        postarray = getWeatherDataFromJson(jsondata);
                    return postarray;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }




            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
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
                    return null;
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
                    return null;
                }
                forecastJsonStr = buffer.toString();
                editor.putString("timeline_json", forecastJsonStr);
                editor.commit();
                try {

                    postarray = getWeatherDataFromJson(forecastJsonStr);
                } catch (JSONException j) {
                    return null;
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
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

            return postarray;
        }



        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
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
                    final Uri uri = getActivity().getContentResolver().insert(
                            TimeLineProvider.CONTENT_URI, values);}
                catch (Exception e)
                {}




            }


            return  resultp;

        }

        @Override
        protected void onPostExecute(final Post[] postarray) {

            if(postarray!=null) {
                //TimelineAdapter mForeCastAdapter = new TimelineAdapter(getActivity(), postarray, isNetworkAvailable());
                //lv.setAdapter(mForeCastAdapter);
                //lv.setAdapter(adapter);



                swipeContainer.setRefreshing(false);
            }

        }
    }*/



    public class NewPostTask extends AsyncTask<Void, Void, String> {
        String LOG_TAG = NewPostTask.class.getSimpleName();

        String posttxt,files,type;

        public NewPostTask(String posttxt, String files,String type) {
            this.posttxt=posttxt;
            this.files=files;
            this.type=type;
        }


        @Override
        protected String doInBackground(Void... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.

            //BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String responset = null;


            URL url = null;
            try {

                url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/add/"+token);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }


            HttpURLConnection urlConnection = null;


            try {
                String postParameters="post_text="+ posttxt+"&type=1"+"&files=";
                /*String postParameters=
                URLEncoder.encode("post_text=", "UTF-8")
                        + "=" + URLEncoder.encode(posttxt, "UTF-8");

                postParameters += "&" + URLEncoder.encode("type", "UTF-8") + "="
                        + URLEncoder.encode("1", "UTF-8");

                postParameters += "&" + URLEncoder.encode("files", "UTF-8")
                        + "=" + URLEncoder.encode("", "UTF-8");*/


                //URL url = new URL("https:www.example.com/login.php");
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                //connection.setRequestProperty("Cookie", cookie);
                //Set to POST
                connection.setDoOutput(true);
                connection.setDoInput(true);
                //connection.setInstanceFollowRedirects(false);
                //connection.setUseCaches (false);
                //connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestMethod("POST");
                connection.setReadTimeout(10000);
                connection.connect();

                //conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write( postParameters );
                wr.flush();

                // Get the server response

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = null;

                // Read Server Response
                while((line = reader.readLine()) != null)
                {
                    // Append server response in string
                    sb.append(line + "\n");
                }


                return sb.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }


            // Read the input stream into a String


            return responset;
        }







        @Override
        protected void onPostExecute(String s) {

            //super.onPostExecute(postarray);
            updateTimeline();

            Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();



        }
    }

}
