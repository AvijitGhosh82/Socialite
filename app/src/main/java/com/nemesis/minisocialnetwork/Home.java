package com.nemesis.minisocialnetwork;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import net.i2p.android.ext.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;


public class Home extends ActionBarActivity {

    private SwipeRefreshLayout swipeContainer;
    public ListView lv;
    private String uid,token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        SharedPreferences pref = getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        uid=pref.getString("uid", null);
        token=pref.getString("token", null);
        if(uid==null || token==null)
        {
            Intent i=new Intent(Home.this,LoginActivity.class);
            startActivity(i);
            finish();
        }

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                FetchWeatherTask fw=new FetchWeatherTask();
                fw.execute();
            }
        });
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


        FetchWeatherTask fw=new FetchWeatherTask();
        fw.execute();
        




        FloatingActionButton newpost=(FloatingActionButton)findViewById(R.id.button_newpost);
        newpost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
                    builder.setTitle("What would you like to post?");


                    final EditText input = new EditText(Home.this);
                    //input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String m_Text = input.getText().toString();
                            Toast.makeText(Home.this, m_Text, Toast.LENGTH_SHORT).show();
                            NewPostTask np = new NewPostTask(m_Text, "1", null);
                            np.execute();
                            dialog.cancel();

                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();}
                else
                {Toast.makeText(Home.this,"Sorry, app is offline.",Toast.LENGTH_SHORT).show();}
            }
        });



        //TextView tv=(TextView)findViewById(R.id.textView4);
        //tv.setText("My TimeLine");
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_timeline_toolbar);
        SpannableString s = new SpannableString("My Timeline");



        if(toolbar != null)
        {

            toolbar.setTitle(s);
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        lv = (ListView) findViewById(R.id.timelinelistview);

        fw=new FetchWeatherTask();
        fw.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
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

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public class FetchWeatherTask extends AsyncTask<Void, Void, Post[]> {
        String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        String postcode = null;

        public FetchWeatherTask() {

        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }


        @Override
        protected Post[] doInBackground(Void... params) {

            Post[] postarray=null;

            SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
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
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                /*String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                String QUERY_PARAM = "q";
                String FORMATS_PARAM = "mode", DAYS_PARAM = "cnt", UNITS_PARAM = "units";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, postcode)
                        .appendQueryParameter(FORMATS_PARAM, "json")
                        .appendQueryParameter(UNITS_PARAM, "metric")
                        .appendQueryParameter(DAYS_PARAM, "7")
                        .build();*/
                URL url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/"+token);
                //URL url=new URL(builtUri.toString());

                //Log.v(LOG_TAG, "Built URI " + builtUri.toString());
                // Create the request to OpenWeatherMap, and open the connection
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


        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
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
            }


            return  resultp;

        }

        @Override
        protected void onPostExecute(Post[] postarray) {

            //super.onPostExecute(postarray);
            if(postarray!=null) {
                TimelineAdapter mForeCastAdapter = new TimelineAdapter(Home.this, postarray, isNetworkAvailable());
                //ListView listView = (ListView) findViewById(R.id.listview_forecast);
                lv.setAdapter(mForeCastAdapter);
                swipeContainer.setRefreshing(false);
            }

        }
    }



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
                /*Writer writer = new OutputStreamWriter(connection.getOutputStream());
                JSONObject jsonParam = new JSONObject();
                try {
                    jsonParam.put("post_text", posttxt);
                    jsonParam.put("type", type);
                    jsonParam.put("files", "");

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                writer.write(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                writer.flush();
                writer.close();*/
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
            FetchWeatherTask fw=new FetchWeatherTask();
            fw.execute();

            Toast.makeText(Home.this, s, Toast.LENGTH_SHORT).show();



        }
    }

}
