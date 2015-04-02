package com.nemesis.minisocialnetwork;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

import static com.nemesis.minisocialnetwork.R.color.darkmagenta;

/**
 * Created by Avijit Ghosh on 22 Mar 2015.
 */
public class TimelineAdapter extends BaseAdapter {

    private final String uid,token;
    //String[] options;
    //int[] images={R.drawable.newg,R.drawable.edit,R.drawable.flip,R.drawable.bt,R.drawable.pgn,R.drawable.settings,R.drawable.resign,R.drawable.goton,R.drawable.force,R.drawable.book,R.drawable.thumb,R.drawable.about};
    private Context context;
    Post[] postarr;
    Boolean network;


    public TimelineAdapter(Context context, Post[] postarr, boolean network)
    {
        this.context=context;
        this.postarr=postarr;
        this.network=network;

        SharedPreferences pref = context.getSharedPreferences("MyPref", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        uid=pref.getString("uid", null);
        token=pref.getString("token", null);
        //options=context.getResources().getStringArray(R.array.navigation_drawer_items);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return postarr.length;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return postarr[position];
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View row=null;


        if(convertView==null)
        {
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.timeline_item, parent, false);
        }
        else
        {
            row=convertView;
        }
        TextView tv1=(TextView) row.findViewById(R.id.user);
        TextView tv2=(TextView) row.findViewById(R.id.text);
        final TextView tv3=(TextView) row.findViewById(R.id.liketext);
        final TextView tv4=(TextView) row.findViewById(R.id.comenttext);
        final ImageView iv3=(ImageView) row.findViewById(R.id.likpic);
        final ImageView iv4=(ImageView) row.findViewById(R.id.compic);
        final ImageView av=(ImageView) row.findViewById(R.id.avataru);
        ImageButton like=(ImageButton) row.findViewById(R.id.likebutt);

        if(network==true)

        {
            String imgURI="http://api.wavit.co/v1.1/data/profiles/img/"+postarr[position].uid+".jpg";
            Picasso.with(context).load(imgURI).into(av);
        }
        else{

            final Resources res = context.getResources();
            final int tileSize = res.getDimensionPixelSize(R.dimen.letter_tile_size);
            Random ran=new Random();
            int key=ran.nextInt(8);
            final LetterTileProvider tileProvider = new LetterTileProvider(context);
            final Bitmap letterTile = tileProvider.getLetterTile(postarr[position].headby, key+"", tileSize, tileSize);

            av.setImageBitmap(letterTile);
            //av.setImageResource(R.drawable.me);

        }




        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(network==true)
                {


                    tv3.setText("Upvotes ("+(Integer.parseInt(postarr[position].numlikes)+1)+")");
                    tv3.setTextColor(darkmagenta);
                    iv3.setImageResource(R.drawable.upvote_blue);
                    NewLikeTask l=new NewLikeTask(postarr[position].fid);
                    l.execute();

                }
                else
                {
                    Toast.makeText(context, "App is offline", Toast.LENGTH_SHORT).show();
                }
            }
        });
        String list=postarr[position].likes;



        if(list.matches("(^|(.*,))"+postarr[position].uid+"((,.*)|$)"))
        {
            iv3.setImageResource(R.drawable.upvote_blue);
            tv3.setTextColor(darkmagenta);

        }
        final ImageButton comm=(ImageButton) row.findViewById(R.id.commbutt);
        comm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(context, DetailedPostActivity.class);
                i.putExtra("fid", postarr[position].fid);
                i.putExtra("uid", postarr[position].uid);
                i.putExtra("name", postarr[position].headby);
                i.putExtra("text", postarr[position].headtext);

                context.startActivity(i);
            }
        });


            tv1.setText(postarr[position].headby);
            tv2.setText(postarr[position].headtext);
            tv3.setText("Upvotes ("+postarr[position].numlikes+")");
            tv4.setText("Comments ("+postarr[position].numcomm+")");




        return row;
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

            //super.onPostExecute(postarray);
            if(s.equalsIgnoreCase("1")) {

            }

            //Toast.makeText(ConversationActivity.this, s, Toast.LENGTH_SHORT).show();



        }
    }





public class NewLikeTask extends AsyncTask<Void, Void, String> {
    String LOG_TAG = NewLikeTask.class.getSimpleName();

    String fid;



    public NewLikeTask(String fid) {
        this.fid=fid;

    }



    @Override
    protected String doInBackground(Void... params) {


        String responset = null;


        URL url = null;
        try {

            url = new URL("http://www.api.wavit.co/v1.1/index.php/feed/like/"+token);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        HttpURLConnection urlConnection = null;


        try {
            String postParameters="fid="+ fid;




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

        //super.onPostExecute(postarray);
       // if(s.equalsIgnoreCase("1")) {

       // }

        //Toast.makeText(ConversationActivity.this, s, Toast.LENGTH_SHORT).show();



    }
}


}


