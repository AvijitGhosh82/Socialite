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
import android.provider.Settings;
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
import android.widget.FrameLayout;
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

    FrameLayout container;




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
        public void onItemSelected(String s) {

        }

    };
    private OnItemClickedListener mListener=sDummyCallbacks ;
    private Parcelable state;



    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(syncFinishedReceiver, new IntentFilter("SYNC_FINISHED"));
    }


    @Override
    public void onPause() {
        super.onPause();
        state = lv.onSaveInstanceState();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //FragmentActivity    faActivity  = (FragmentActivity)    super.getActivity();
        // Replace LinearLayout by the type of the root element of the layout you're trying to load
        View v    = (View) inflater.inflate(R.layout.fragment_home, container, false);

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



        lv.setAdapter(adapter);
        if(state != null) {
            lv.onRestoreInstanceState(state);
        }

        return v; // We must return the loaded Layout
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.

        super.onSaveInstanceState(outState);
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
        setAutoOrientationEnabled(getActivity(),false);

        CursorLoader loader = new CursorLoader(
                this.getActivity(),
                TimeLineProvider.CONTENT_URI,
                PROJECTION,
                null,
                null,
                TimeLineProvider._ID+" DESC"
                );
        return loader;

    }



    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        adapter.swapCursor(cursor);


        //lv.setAdapter(adapter);

        //adapter.notifyDataSetChanged();
        cursor.moveToFirst();
        cursor.moveToPosition(0);
        int size=cursor.getCount();
        if(size!=0)
        {
            final String[] fidarray= new String[size];

            for(int i=0;i<size;i++){
                fidarray[i]=cursor.getString(cursor
                        .getColumnIndex(TimeLineProvider.FID));

                cursor.moveToNext();
            }


        //mListener.OnItemClicked(Parameters params);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                mListener.onItemSelected(fidarray[position]);
            }
        });

            setAutoOrientationEnabled(getActivity(), true);

    }}

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
        setAutoOrientationEnabled(getActivity(),true);

    }



    public interface OnItemClickedListener {
        void onItemSelected(String s);
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

            //getLoaderManager().initLoader(FORECAST_LOADER, null, HomeFragment.this);
           // getLoaderManager().restartLoader(FORECAST_LOADER, null, HomeFragment.this);
            // getLoaderManager().getLoader(FORECAST_LOADER).onContentChanged();


        }
    }


    public static void setAutoOrientationEnabled(Context context, boolean enabled)
    {
        Settings.System.putInt( context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }
}
