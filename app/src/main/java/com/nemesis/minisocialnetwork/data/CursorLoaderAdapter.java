package com.nemesis.minisocialnetwork.data;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nemesis.minisocialnetwork.LetterTileProvider;
import com.nemesis.minisocialnetwork.R;
import com.squareup.picasso.Picasso;

import java.util.Random;

import static android.graphics.Color.parseColor;

public class CursorLoaderAdapter extends CursorAdapter {
    final String token;
    final boolean network;




    public CursorLoaderAdapter(Context context, Cursor c, String token, boolean network) {
        super(context, c);

        this.token=token;
        this.network=network;
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // when the view will be created for first time,
        // we need to tell the adapters, how each item will look
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View retView = inflater.inflate(R.layout.timeline_item, parent, false);

        return retView;
    }

    @Override
    public boolean isEnabled(int position) {
        //Set a Toast or Log over here to check.
        return true;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return true;
    }


    @Override
    public void bindView(View row, final Context context, Cursor cursor) {
        // here we are setting our data
        // that means, take the data from the cursor and put it in views

        TextView tv1=(TextView) row.findViewById(R.id.user);
        TextView tv2=(TextView) row.findViewById(R.id.text);
        final TextView tv4=(TextView) row.findViewById(R.id.comenttext);
        final ImageView iv4=(ImageView) row.findViewById(R.id.compic);
        final ImageView av=(ImageView) row.findViewById(R.id.avataru);
        ImageButton like=(ImageButton) row.findViewById(R.id.likebutt);

        String name=cursor.getString(cursor.getColumnIndex(TimeLineProvider.NAME));
        String post=cursor.getString(cursor.getColumnIndex(TimeLineProvider.POST));
        String likes=cursor.getString(cursor.getColumnIndex(TimeLineProvider.LIKES));
        String comments=cursor.getString(cursor.getColumnIndex(TimeLineProvider.COMMENTS));
        final String uid=cursor.getString(cursor.getColumnIndex(TimeLineProvider.UID));
        final String fid=cursor.getString(cursor.getColumnIndex(TimeLineProvider.FID));


        if(network)

        {
            String imgURI="http://api.wavit.co/v1.1/data/profiles/img/"+uid+".jpg";
            Picasso.with(context).load(imgURI).into(av);
        }
        else{

            final Resources res = context.getResources();
            final int tileSize = res.getDimensionPixelSize(R.dimen.letter_tile_size);
            Random ran=new Random();
            int key=ran.nextInt(8);
            final LetterTileProvider tileProvider = new LetterTileProvider(context);
            final Bitmap letterTile = tileProvider.getLetterTile(name, key+"", tileSize, tileSize);

            av.setImageBitmap(letterTile);
            //av.setImageResource(R.drawable.me);

        }

        tv1.setText(name);
        tv2.setText(post);

        tv4.setText("Comments ("+comments+")");


        //The web api for the like function is buggy,
        // until it is fixed, I'm saving the likes locally in sharedprefernces using a checkbox.

        CheckBox cb=(CheckBox)row.findViewById(R.id.likebox);

        SharedPreferences pref = context.getSharedPreferences("LikeStatus", context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = pref.edit();
        boolean status=pref.getBoolean(fid,false);
        cb.setChecked(status);
        if(status)
        {
            cb.setTextColor(parseColor ("#ff259b24"));
            cb.setText("Unfavorite");
        }


        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // perform logic
                    editor.putBoolean(fid, true);
                    editor.commit();
                    buttonView.setTextColor(parseColor ("#ff259b24"));
                    buttonView.setText("Unfavorite");
                }
                else
                {
                    editor.putBoolean(fid, false);
                    editor.commit();
                    buttonView.setTextColor(context.getResources().getColor(R.color.primarytext));
                    buttonView.setText("Favorite");


                }

                Intent i = new Intent("SYNC_LIKE");
                context.sendBroadcast(i);

            }
        });





    }


}