package com.nemesis.minisocialnetwork;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

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
import java.util.Date;


public class DetailedPostFragment extends Fragment {
    String[] commarray = null,uidarray=null;
    private ListView lv;
    private String f;
    private String likelist;
    private String timestamp;
    private String uid,token;
    private ShareActionProvider mShareActionProvider;
    private String t;
    private TextView tv2;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = (View) inflater.inflate(R.layout.fragment_detail, container, false);

        lv = (ListView) v.findViewById(R.id.commlist);
        Intent intent = getActivity().getIntent();
        Bundle bundle = this.getArguments();
        String n = null,u=null,t=null;
        if (bundle != null) {

            f = bundle.getString("fid");
            n = bundle.getString("name");
            u = bundle.getString("uid");
            t = bundle.getString("text");
       }

        ImageButton sh=(ImageButton)v.findViewById(R.id.sharebut);
        if(sh!=null)
        {
            final String finalT = t;
            sh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");

                    //String forecastStr=getActivity().getIntent().getExtras().getString("text");

                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            finalT + " #MyTimeLine");

                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                }
            });
        }

        tv2=(TextView)v.findViewById(R.id.liketext);

        SharedPreferences pref = getActivity().getSharedPreferences("MyPref", getActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        uid=pref.getString("uid", null);
        token=pref.getString("token", null);
        TextView tv=(TextView)v.findViewById(R.id.user);
        tv.setText(n);
        TextView tv2=(TextView)v.findViewById(R.id.text);
        tv2.setText(t);
        FetchCommTask fc=new FetchCommTask(f);
        fc.execute();
        ImageView av=(ImageView)v.findViewById(R.id.avataru);
        String imgURI="http://api.wavit.co/v1.1/data/profiles/img/"+u+".jpg";
        Picasso.with(getActivity()).load(imgURI).into(av);




        final EditText edittext = (EditText) v.findViewById(R.id.edittext);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String comm=edittext.getText().toString();
                    NewCommTask t=new NewCommTask(f,comm);
                    t.execute();
                    edittext.setText("");

                    // Perform action on key press
                    //.makeText(HelloFormStuff.this, edittext.getText(), Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        setHasOptionsMenu(true);


        return v;
    }




    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String forecastStr=getActivity().getIntent().getExtras().getString("text");

        shareIntent.putExtra(Intent.EXTRA_TEXT,
                forecastStr+ " #MyTimeLine");
        return shareIntent;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_detailed_post, menu);


        MenuItem menuItem = menu.findItem(R.id.menu_item_share);
// Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        /*if(mShareActionProvider==null)
        {
            ShareActionProvider mShareActionProvider;
            mShareActionProvider = new ShareActionProvider(getActivity());
            mShareActionProvider.setShareIntent(createShareIntent());
            MenuItemCompat.setActionProvider(menuItem, mShareActionProvider);
        }*/
// Attach an intent to this ShareActionProvider. You can update this at any time,
// like when the user selects a new piece of data they might like to share.

        if (mShareActionProvider != null ) {
            mShareActionProvider.setShareIntent(createShareIntent());
        } else {
            //Log.d(LOG_TAG, "Share Action Provider is null?");

        };
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }



    public class FetchCommTask extends AsyncTask<Void, Void, Void> {
        String LOG_TAG = FetchCommTask.class.getSimpleName();

        String fid = null;

        public FetchCommTask(String fid) {
            this.fid=fid;
        }

        @Override
        protected Void doInBackground(Void... params) {


            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;


            //Comm[] postarray;
            try {

                URL url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/"+token+"/" + fid);



                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);

                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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
                //final String finalForecastJsonStr = forecastJsonStr;


                try {

                    commarray = getComments(forecastJsonStr);
                    uidarray = getuid(forecastJsonStr);
                    likelist=getLikes(forecastJsonStr);

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

            //return result;
            return null;
        }



        private String getLikes(String forecastJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.


            final String OWM_LIKES = "feed_likes";
            final String OWM_LIKES_NAME = "name";
            String ctxt="";



            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray feedArray = forecastJson.getJSONArray(OWM_LIKES);


            for (int i = 0; i < feedArray.length(); i++) {


                JSONObject feedobj = feedArray.getJSONObject(i);


                ctxt= ctxt+feedobj.getString(OWM_LIKES_NAME)+",";



            }


            return ctxt;

        }



        private String[] getComments(String forecastJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.

            final String OWM_DATA = "feed_data";

            final String OWM_COMMENTS = "feed_comments";
            final String OWM_LIKES = "feed_likes";
            final String OWM_COMMENT_TEXT = "text";
            final String OWM_LIKES_NAME = "name";
            final String OWM_COMMENT_UID = "posted_by";



            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray feedArray = forecastJson.getJSONArray(OWM_COMMENTS);
            //JSONArray dataArray = forecastJson.getJSONArray(OWM_DATA);


            String[] resultp = new String[feedArray.length()];
            for (int i = 0; i < feedArray.length(); i++) {


                JSONObject feedobj = feedArray.getJSONObject(i);


                String ctxt = feedobj.getString(OWM_COMMENT_TEXT);
                // String cuid = feedobj.getString(OWM_COMMENT_UID);

                //Toast.makeText(DetailedPostActivity.this, ctxt+"\n"+cuid, Toast.LENGTH_SHORT).show();


                resultp[i] = ctxt;
            }


            return resultp;

        }

        private String[] getuid(String forecastJsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.

            //final String OWM_DATA = "feed_data";

            final String OWM_COMMENTS = "feed_comments";
            final String OWM_COMMENT_TEXT = "text";
            final String OWM_COMMENT_UID = "posted_by";


            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray feed = forecastJson.getJSONArray(OWM_COMMENTS);
            //JSONArray dataArray = forecastJson.getJSONArray(OWM_DATA);


            String[] resultp = new String[feed.length()];
            for (int i = 0; i < feed.length(); i++) {


                JSONObject feedobj = feed.getJSONObject(i);


                String ctxt = feedobj.getString(OWM_COMMENT_TEXT);
                String cuid = feedobj.getString(OWM_COMMENT_UID);


                resultp[i] = cuid;
            }


            return resultp;

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(commarray!=null)
            {CommentAdapter c=new CommentAdapter(getActivity(),commarray, uidarray,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
                lv.setAdapter(c);}

            tv2.setText(likelist);
            super.onPostExecute(aVoid);
        }
    }



    public class NewCommTask extends AsyncTask<Void, Void, String> {
        String LOG_TAG = NewCommTask.class.getSimpleName();

        String fid,comm;



        public NewCommTask(String fid, String comm) {
            this.fid=fid;
            this.comm=comm;
        }



        @Override
        protected String doInBackground(Void... params) {


            String responset = null;


            URL url = null;
            try {

                url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/comment/"+token);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }


            HttpURLConnection urlConnection = null;


            try {
                String postParameters="fid="+ fid+"&text="+comm;




                HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);

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


            FetchCommTask fc=new FetchCommTask(f);
            fc.execute();




        }
    }
}