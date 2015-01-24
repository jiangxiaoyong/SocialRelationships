package com.mengproject.jxy.socialrelationships;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by jxy on 23/01/15.
 */
public class MyAdapter extends ArrayAdapter<String> {


    public MyAdapter(Context context, String [] values) {
        super(context, R.layout.row_layout, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

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
    }
}
