package com.mengproject.jxy.socialrelationships;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jxy on 23/01/15.
 */
public class MyAdapter extends ArrayAdapter<Friend> {

    private static final String TAG = "MyAdapter";
    private Context context = null;

    /*
        for improving performance, which speedup the population of listView
     */
    // View lookup cache
    private static class ViewHolder {
        TextView name;
        TextView relativity;
        ImageView image;
    }

    public MyAdapter(Context context, List<Friend> values) {
        super(context, 0, values);
        this.context = context;
    }

    class friendProfileImageAsyncTask extends AsyncTask<String, String, Bitmap> {

        /*
            this variable used for holding imageview passed by custom MyAdapter
            so that can set thumbnail of listview later on
         */
        private final WeakReference imageViewReference;

        /*
            this constructor used for holding the passed in imageview
         */
        public friendProfileImageAsyncTask(ImageView imageView)
        {
            imageViewReference = new WeakReference(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            Bitmap bitmap = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){

                    HttpEntity entity = response.getEntity();
                    BufferedHttpEntity b_entity = new BufferedHttpEntity(entity);
                    InputStream input = b_entity.getContent();

                    bitmap = BitmapFactory.decodeStream(input);

                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //Do anything with response..

            if (imageViewReference != null) {
                ImageView imageView = (ImageView) imageViewReference.get();
                if (imageView != null) {

                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageDrawable(imageView.getContext().getResources()
                                .getDrawable(R.drawable.icon));
                    }
                }

            }

        }
    }

    /*
        limit the number of items showing in list view
     */

    @Override
    public int getCount() {

        if (super.getCount() >= 10)
        {
            return 10;
        }
        else
        {
            return super.getCount();
        }
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
            viewHolder.image = (ImageView) convertView.findViewById(R.id.rowCellImageView);


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
            Async to download friend's profile image
         */

        if (viewHolder.image != null && friend.url != null)
        {
            new friendProfileImageAsyncTask(viewHolder.image).execute(friend.url);
        }
        else if (viewHolder.image != null && friend.url == null)
        {
            /*
                fill default icon from drawable folder when fiend does not contain url
             */
            int id = context.getResources().getIdentifier("com.mengproject.jxy.socialrelationships:drawable/icon", null, null);
            viewHolder.image.setImageResource(id);
        }

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
