package com.mengproject.jxy.socialrelationships;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.widget.ProfilePictureView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


public class FriendRelationship extends Activity {

    private static final String TAG = "FriendRelationship";

    private ProfilePictureView profilePictureView;
    private TextView userNameView;
    private ImageView imageview;
    ListView theListView;
    ArrayAdapter<Friend> myAdapter;
    SortedSet<Map.Entry<String,Double>> sortedFriends = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

        setContentView(R.layout.friend_relationship_layout);

        /*
         *  set user profile picture and user name
         */
        // Find the user's profile picture custom view

        imageview = (ImageView)findViewById(R.id.rowCellImageView);

        // Find the user's name view
        userNameView = (TextView)findViewById(R.id.friend_name);


        /*
            retrieve friend name and his/her friends info, which is treeMap
         */
        Intent intent = getIntent();
        String desired_friend_name =  intent.getExtras().getString("desired_friend");
        Map<String, Double> friends = (Map<String, Double>) intent.getExtras().getSerializable("friends");
        Map<String, String> taggable = (Map<String, String>) intent.getExtras().getSerializable("taggable_friends");
        String url = (String) intent.getExtras().getSerializable("url");

        /*
            this url is used for fetch the profile image of the desired friend
         */
        if (url != null)
        {
            new friendProfileImageAsyncTask().execute(url);
        }

        /*
            set user name
         */
        userNameView.setText(desired_friend_name);

        theListView = (ListView)findViewById(R.id.list);
        populateDataToListView(friends, taggable);


    }

    class friendProfileImageAsyncTask extends AsyncTask<String, String, Bitmap> {


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
            if (imageview != null){
                imageview.setImageBitmap(bitmap);
            }

        }
    }

    private void populateDataToListView(Map<String, Double> friends, Map<String, String> taggable_friends) {

        List<Friend> arrayOfFriends = new ArrayList<Friend>();

        /*
            sort friend based on relativity descend order
         */
        sortedFriends = entriesSortedByValues(friends);

        Iterator it = sortedFriends.iterator();
        while(it.hasNext())
        {
            Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
            String name = entry.getKey();
            String relativity = new DecimalFormat("#0.000").format(entry.getValue());// specify precision of double value
            String url = taggable_friends.get(name);


            Friend thefriend = new Friend(name, Double.parseDouble(relativity), url);
            arrayOfFriends.add(thefriend);
        }


        myAdapter =  new MyAdapter(this, arrayOfFriends);
        theListView.setAdapter(myAdapter);
    }

    /*
    sort tree map
 */
    static <K,V extends Comparable<? super V>>
    SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<Map.Entry<K,V>>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e2.getValue().compareTo(e1.getValue());
                        return res != 0 ? res : 1;
                    }
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        //getMenuInflater().inflate(R.menu.menu_friend_relationship, menu);
        return true;
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
}
