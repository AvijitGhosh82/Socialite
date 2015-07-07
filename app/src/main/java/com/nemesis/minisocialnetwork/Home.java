package com.nemesis.minisocialnetwork;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.widget.ListView;


public class Home extends ActionBarActivity implements HomeFragment.OnItemClickedListener{

    private SwipeRefreshLayout swipeContainer;
    public ListView lv;
    private String uid,token;
    public static boolean mTwoPane;
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    public static Context c;



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
            //return;
        }




        Toolbar toolbar = (Toolbar) findViewById(R.id.my_timeline_toolbar);
        SpannableString s = new SpannableString("My Timeline");

        c=Home.this;

        if(toolbar != null)
        {

            toolbar.setTitle(s);
        }


        if (findViewById(R.id.fragment_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_detail_container, new DetailedPostFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            //getSupportActionBar().setElevation(0f);
        }

    }

    @Override
    public void onItemSelected(Bundle bundle) {
       if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.


            DetailedPostFragment fragment = new DetailedPostFragment();
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
       } else {

               String f = bundle.getString("fid");
               String n = bundle.getString("name");
               String u = bundle.getString("uid");
               String t = bundle.getString("text");
            Intent i = new Intent(this, DetailedPostActivity.class);
           i.putExtra("fid", f);
           i.putExtra("uid", u);
           i.putExtra("name",n);
           i.putExtra("text", t);

                   // .setData(contentUri);
            startActivity(i);
        }
    }

}
