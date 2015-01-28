package com.mengproject.jxy.socialrelationships;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jxy on 23/01/15.
 */
public class MyAdapter extends ArrayAdapter<Friend> {


    public MyAdapter(Context context, List<Friend> values) {
        super(context, 0, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Friend friend = getItem(position);

        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout, parent, false);
        }

        /*
        LayoutInflater theInflater = LayoutInflater.from(getContext());
        View theView = theInflater.inflate(R.layout.row_layout, parent, false);

        //set the string in row cell
        String name = getItem(position);
        TextView theTextView = (TextView) theView.findViewById(R.id.rowCellTextView);
        theTextView.setText(name);

        //set the imageview of row cell
        ImageView theImageView = (ImageView) theView.findViewById(R.id.rowCellImageView);
        theImageView.setImageResource(R.drawable.icon);

        return theView;

        */
        // Lookup view for data population
        TextView theTextView1 = (TextView) convertView.findViewById(R.id.rowCellTextView1);
        TextView theTextView2 = (TextView) convertView.findViewById(R.id.rowCellTextView2);

        // Populate the data into the template view using the data object
        theTextView1.setText(friend.name);
        theTextView2.setText(friend.relativity.toString());

        // Return the completed view to render on screen
        return convertView;

    }
}
