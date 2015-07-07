package com.nemesis.minisocialnetwork;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity {

    private String forecastJsonStr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText login = (EditText) findViewById(R.id.email);
        final EditText pass = (EditText) findViewById(R.id.password);
        Button b=(Button)findViewById(R.id.go);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NewCommTask n=new NewCommTask(login.getText().toString(),pass.getText().toString());
                n.execute();
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_timeline_toolbar);
        SpannableString s = new SpannableString("Welcome");



        if(toolbar != null)
        {

            toolbar.setTitle(s);
            //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    public class NewCommTask extends AsyncTask<Void, Void, String> {
        String LOG_TAG = NewCommTask.class.getSimpleName();

        String email,pass;



        public NewCommTask(String email, String pass) {
            this.email=email;
            this.pass=pass;
        }



        @Override
        protected String doInBackground(Void... params) {


            String responset = null;


            URL url = null;
            try {

                url = new URL("http://www.api.wavit.co/v1.1/index.php/login");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }


            HttpURLConnection urlConnection = null;


            try {
                String postParameters="email="+email+"&password="+pass;

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
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    sb.append(line + "\n");
                }

                if (sb.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = sb.toString();


                return sb.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }





            return responset;
        }







        @Override
        protected void onPostExecute(String s) {


            try {
                saveToken(s);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent i=new Intent(LoginActivity.this,Home.class);
            startActivity(i);
            finish();

        }
    }


    void saveToken(String forecastJsonStr)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_HEAD = "feed_head";
        final String OWM_DATA = "feed_data";



        JSONObject obj = new JSONObject(forecastJsonStr);



            String uid = obj.getString("uid");
            String token = obj.getString("id");

        SharedPreferences pref = getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("uid", uid);
        editor.putString("token", token);
        editor.commit();
        }






}



