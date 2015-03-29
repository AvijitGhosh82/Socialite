package com.nemesis.minisocialnetwork;

/**
 * Created by Avijit Ghosh on 29 Mar 2015.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


/**
 * Created by Avijit Ghosh on 22 Mar 2015.
 */
public class CommentAdapter extends BaseAdapter {

    private final String timestamp;
    //String[] options;
    //int[] images={R.drawable.newg,R.drawable.edit,R.drawable.flip,R.drawable.bt,R.drawable.pgn,R.drawable.settings,R.drawable.resign,R.drawable.goton,R.drawable.force,R.drawable.book,R.drawable.thumb,R.drawable.about};
    private Context context;
    //Post[] postarr;
    //Boolean network;
    //private boolean canLike;
    String[] comments;
    String[] uid;


    public CommentAdapter(Context context, String[] comments, String[] uid, String timestamp)
    {
        this.context=context;
        this.comments=comments;
        this.uid=uid;
        this.timestamp=timestamp;

        //options=context.getResources().getStringArray(R.array.navigation_drawer_items);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return comments.length;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return comments[position];
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return arg0;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View row=null;


        if(convertView==null)
        {
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row=inflater.inflate(R.layout.list_item_comments, parent, false);
        }
        else
        {
            row=convertView;
        }
        TextView tv=(TextView) row.findViewById(R.id.textcomm);
        TextView td=(TextView) row.findViewById(R.id.design);
        td.setText(timestamp);


        final ImageView iv=(ImageView) row.findViewById(R.id.avatarcomm);



            String imgURI="http://api.wavit.co/v1.1/data/profiles/img/"+uid[position]+".jpg";
            Picasso.with(context).load(imgURI).into(iv);
            tv.setText(comments[position]);










        return row;
    }





}



