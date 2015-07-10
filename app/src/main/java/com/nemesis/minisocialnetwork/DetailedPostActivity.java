package com.nemesis.minisocialnetwork;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.widget.ListView;

import com.nemesis.minisocialnetwork.data.TimeLineProvider;


public class DetailedPostActivity extends ActionBarActivity {
    String[] commarray = null,uidarray=null;
    private ListView lv;
    private String f;
    private String likelist;
    private String timestamp;
    private String uid,token;
    private ShareActionProvider mShareActionProvider;
    private String t;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_post);
        /*lv= (ListView) findViewById(R.id.commlist);*/
        Intent intent = getIntent();
        f = intent.getExtras().getString("fid");
        final String[] PROJECTION = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                TimeLineProvider.STUDENTS_TABLE_NAME + "." + TimeLineProvider._ID,
                TimeLineProvider.NAME,
                TimeLineProvider.FID
        };
        Cursor cursor = getContentResolver()
                .query(TimeLineProvider.CONTENT_URI, PROJECTION, TimeLineProvider.FID+" = "+f, null, null);
        cursor.moveToPosition(0);
        String n=cursor.getString(cursor.getColumnIndex(TimeLineProvider.NAME));


        Toolbar toolbar = (Toolbar) findViewById(R.id.my_timeline_toolbar);
        SpannableString s = new SpannableString(n);
        if(toolbar != null)
        {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(s);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState==null)
        {DetailedPostFragment fragmentS1 = new DetailedPostFragment();
            Bundle bundle = new Bundle();
            bundle.putString("fid", f);

            fragmentS1.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_detail_container, fragmentS1).commit();}


    }


}