package com.mengproject.jxy.socialrelationships;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jxy on 23/01/15.
 */
public class MyAdapter extends ArrayAdapter<Friend> {

    /*
        for improving performance, which speedup the population of listView
     */
    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView relativity;
    }

    public MyAdapter(Context context, List<Friend> values) {
        super(context, 0, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Friend friend = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null)
        {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_layout, parent, false);

            viewHolder.name = (TextView) convertView.findViewById(R.id.rowCellTextView1);
            viewHolder.relativity = (TextView) convertView.findViewById(R.id.rowCellTextView2);


            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        // Populate the data into the template view using the data object, improving performance
        viewHolder.name.setText(friend.name);
        viewHolder.relativity.setText(friend.relativity.toString());

        /*
        // Lookup view for data population
        TextView theTextView1 = (TextView) convertView.findViewById(R.id.rowCellTextView1);
        TextView theTextView2 = (TextView) convertView.findViewById(R.id.rowCellTextView2);

        // Populate the data into the template view using the data object
        theTextView1.setText(friend.name);
        theTextView2.setText(friend.relativity.toString());
        */

        // Return the completed view to render on screen
        return convertView;

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


    }
}
