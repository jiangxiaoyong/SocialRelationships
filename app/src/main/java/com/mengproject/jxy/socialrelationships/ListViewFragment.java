package com.mengproject.jxy.socialrelationships;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;
import com.facebook.widget.WebDialog;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.GraphIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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

import org.jgrapht.*;
import org.jgrapht.graph.*;

// resolve ambiguity


/**
 * Created by jxy on 23/01/15.
 */
public class ListViewFragment extends Fragment {


    private static final String TAG = "ListViewFragment";
    private static final int REAUTH_ACTIVITY_CODE = 100;

    private ProfilePictureView profilePictureView;
    private TextView userNameView;
    private String hostUserName;
    private Button shareButton;
    private Button feedBackButton;
    private TextView current_alg;


    List<List<CoordinateOfOneTag>> all_photos_cooridinates = null;
    List<String> all_names = null;    //Array list to store the name of all people appeared in all photos
    List<String> all_scanned_photos = null;
    List<String> all_photos = null;
    List<String> top10_name_first = null;
    List<String> top10_name_second = null;
    Map<String, Map<String, Double>> all_friends_relativity = null;
    SortedSet<Map.Entry<String,Double>> sortedFriends = null;
    Map<String, String>taggable_friends = null;
    ListView theListView;
    ArrayAdapter<Friend> myAdapter;
    ProgressBar progressBar = null;

    Map<String, Map<String, Double>> SO_all_friends_relativity = null;


    boolean photosOfYouDone = false; // indicate that all pages of album 'Photos of you' have been fetched
    boolean uploadedPhotosDone = false; // indicate that all pages of all user uploaded photos have been fetched
    boolean response_have_photo_data = false; //indicate that the response JASON array is empty

    static int page_counter = 0;
    static int total_photos = 0;
    static int total_tagged_photos = 0;
    static int photos_of_user = 0;
    static int total_tag = 0;
    algorithm alg;

    //Number of vertices
    static int size = 10;

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };


    public ListViewFragment(){

    }

    public enum photoCategory{
        photosOfYou, uploadedPhotos
    }

    public enum algorithm {
        firstOrder, secondOrder
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);

        /*
            claim that this fragment will participate receiving menu clicking
         */
        setHasOptionsMenu(true);
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // TODO Add your menu entries here
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.listview, container, false);

        /*
         *  set user profile picture and user name
         */
        // Find the user's profile picture custom view
        profilePictureView = (ProfilePictureView) view.findViewById(R.id.selection_profile_pic);
        profilePictureView.setCropped(true);

        // Find the user's name view
        userNameView = (TextView) view.findViewById(R.id.selection_user_name);
        // Find the text view of current algorithm
        current_alg = (TextView) view.findViewById(R.id.current_alg);

        // Check for an open session
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            // Get the user's data
            makeUserInfoRequest(session);
        }

        /*
        Toast.makeText(getActivity(),
                "Analyzing you relationships, few seconds", Toast.LENGTH_LONG)
                .show();
        */

        progressBar = (ProgressBar) view.findViewById(R.id.pbHeaderProgress);


        /*
            fill in list view
         */
        ArrayList<Friend> arrayOfFriends = new ArrayList<Friend>();

       myAdapter =  new MyAdapter(getActivity(), arrayOfFriends);

       theListView = (ListView)view.findViewById(R.id.list);
       //theListView.setAdapter(myAdapter);

        /*
            set up click listener to share button
         */
        shareButton = (Button) view.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishFeedDialog();
            }
        });

        /*
            set up click listener to send feedback button
         */
        feedBackButton = (Button) view.findViewById(R.id.feedBackButton);
        feedBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFeedback();
            }
        });

        /*
            set up the click listener for listview
         */
        theListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                /*
                    figure out which friend has been clicked
                 */

                int counter = 0;
                String friendName = null;
                Iterator it = sortedFriends.iterator();
                while(it.hasNext())
                {
                    Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
                    String name = entry.getKey();

                    if (counter == position)
                    {
                        friendName = name;
                        break;
                    }
                    counter ++;
                }

                /*
                    start new activity to show desired friend's relativity
                 */

                TreeMap<String, Double> friends = (TreeMap<String, Double>) all_friends_relativity.get(friendName);
                Intent friendRelationship = new Intent(getActivity(), FriendRelationship.class);
                friendRelationship.putExtra("desired_friend",friendName);
                friendRelationship.putExtra("friends", friends);
                HashMap<String, String> taggable = (HashMap<String, String>) taggable_friends;
                friendRelationship.putExtra("taggable_friends", taggable);
                String url = taggable_friends.get(friendName);
                if (url != null)
                {
                    friendRelationship.putExtra("url", url);
                }
                startActivity(friendRelationship);
            }
        });


        /*
            set current type of algorithm
         */
        alg = algorithm.firstOrder;

        return view;
    }


    /* anoher method to create list view
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2" };
        ArrayAdapter<String> adapter = new MyAdapter(getActivity(), values);
        setListAdapter(adapter);
    }
    */


    /*
     * Deal with the style of listview, e.g background, font
     */
    /*
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = getListView();

        //listView.setDivider(new ColorDrawable(Color.WHITE));
        //listView.setDividerHeight(3); // 3 pixels height

        listView.setBackgroundColor(Color.BLACK);
    }
    */

    private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
        if (session != null && session.isOpened()) {
            Log.d(TAG, "Logged in...");
            /*
                Get the user's data.
              */
            makeUserInfoRequest(session);

            progressBar.setVisibility(View.VISIBLE);

            firstOrderRelativity(session);



        }else if (state.isClosed()) {
            Log.d(TAG, "Logged out...");
        }
    }

    private void firstOrderRelativity(Session session) {

        if (alg == algorithm.firstOrder)
        {
            current_alg.setText("Algo : First Order");
        }
        /*
            Get the user's relativity based on all uploaded photos and 'photos of you'
         */
        all_names = new ArrayList<String>();
        all_scanned_photos = new ArrayList<String>();
        all_photos = new ArrayList<String>();
        all_photos_cooridinates = new ArrayList<List<CoordinateOfOneTag>>();
        taggable_friends = new HashMap<String, String>();

        makeRelationshipRequest(session,"me/photos/uploaded/", null, photoCategory.uploadedPhotos );
        makeRelationshipRequest(session,"me/photos/", null, photoCategory.photosOfYou);

        /*
            Get the user's taggable friends list
         */
        makeTaggableFriendsRequest(session, "me/taggable_friends");
    }


    private void makeUserInfoRequest(final Session session) {
        // Make an API call to get user data and define a
        // new callback to handle the response.
        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        // If the response is successful
                        if (session == Session.getActiveSession()) {
                            if (user != null) {

                                Log.d(TAG, user.getId());
                                Log.d(TAG, user.getName());
                                // Set the id for the ProfilePictureView
                                // view that in turn displays the profile picture.
                                profilePictureView.setProfileId(user.getId());

                                // Set the Textview's text to the user's name.
                                userNameView.setText(user.getName());

                                //get host user name
                                hostUserName = user.getName();

                                //call display friend relativity in list view
                                showFriendsRelativity();

                            }
                        }
                        if (response.getError() != null) {
                            // Handle errors, will do so later.
                            Log.d(TAG, "strange bug");
                        }
                    }
                });
        request.executeAsync();
    }

    private void makeTaggableFriendsRequest(Session session, String friendsPath) {

        new Request(session, friendsPath, null, HttpMethod.GET, new Request.Callback()
        {
            @Override
            public void onCompleted(Response response) {

                Log.i(TAG, "Taggabel friends Response Result: " + response.toString());
                // Process the returned response
                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null)
                {
                    JSONObject jsonObject = graphObject.getInnerJSONObject();
                    int response_JsonArray_len = 0;

                    try {
                        JSONArray outmostArray = jsonObject.getJSONArray("data");
                        response_JsonArray_len = outmostArray.length();

                        if (graphObject.getProperty("data") != null && response_JsonArray_len != 0)
                        {
                            //parse the JSON data and store in data structure
                            parseTaggableFriendsJSONData(graphObject);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).executeAsync();

    }




    private void makeRelationshipRequest(final Session session, final String photoPath, String nextPage, final photoCategory p_category){



        new Request(session, photoPath, getRequestParameters(nextPage), HttpMethod.GET, new Request.Callback()
        {
            @Override
            public void onCompleted(Response response)
            {

                Log.i(TAG, "Response Result: " + response.toString());
                // Process the returned response
                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null)
                {

                    JSONObject jsonObject = graphObject.getInnerJSONObject();
                    int response_JsonArray_len = 0;

                    try {
                        JSONArray outmostArray = jsonObject.getJSONArray("data");
                        response_JsonArray_len = outmostArray.length();

                        if (graphObject.getProperty("data") != null && response_JsonArray_len != 0)
                        {
                            //parse the JSON data and store in data structure
                            parseJSONData(graphObject);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    /*
                        Check if the response contains pagination
                     */
                    JSONObject pagingObj = (JSONObject) graphObject.getProperty("paging");
                    boolean nextPageOrNot = false;
                    if (pagingObj != null)
                    {
                        nextPageOrNot = pagingObj.has("next");

                    }

                    if (pagingObj!= null && nextPageOrNot)
                    {
                        /*
                            The response contains next page, continue to request next page
                         */
                        try {

                            String nextPageToken = extractNextPageToken(pagingObj.getString("next"));
                            System.out.println("next page token " + nextPageToken);
                            makeRelationshipRequest(session, photoPath, nextPageToken, p_category);
                            page_counter ++;

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }


                    }
                    else
                    {
                         /*
                            reach here means no next page, or end of page
                         */
                        if (response_JsonArray_len != 0)
                        {
                            /*
                                flag there indicating at least either 'Photos Of You' and 'uploaded photos'
                                is not empty, so that list view can show friends relativity
                             */
                            response_have_photo_data = true;
                        }

                        if (p_category == photoCategory.photosOfYou)
                        {
                            photosOfYouDone = true;
                        }
                        else if(p_category == photoCategory.uploadedPhotos)
                        {
                            uploadedPhotosDone = true;
                        }

                    }

                    showFriendsRelativity();

                }

            }
        }).executeAsync();

    }

    class ShowResultsAsync extends AsyncTask< Void, Void, Map<String, Map<String, Double>>  >
    {

        @Override
        protected Map<String, Map<String, Double>> doInBackground(Void... params) {

             /*
                Further calculate the distance between two tags
             */
            List<List<RelativityOfTwoTags>> relativity_AllPhotos = findDistanceBetweenTwoTags(all_photos_cooridinates);

            /*
                summation of relativity between two tags of all photos
             */
            Map<String, Map<String, Double>> summation_relativity = summationOfRelativity(all_names, relativity_AllPhotos);
            return summation_relativity;
        }

        protected void onPreExecute (){
            Log.d("PreExceute", "On pre Exceute......");

        }

        protected void onProgressUpdate(Integer...a){
            Log.d(TAG,"You are in progress update ... ");
        }

        @Override
        protected void onPostExecute(Map<String, Map<String, Double>> summation_relativity) {
            super.onPostExecute(summation_relativity);

            Log.d(TAG,"on Post Execute");

            showResults(summation_relativity);

        }

    }

    private void showResults(Map<String, Map<String, Double>> summation_relativity) {

        //parse the JSON data and store in data structure
        ListViewFragment.this.all_friends_relativity = summation_relativity;
        Map<String, Double> friends = all_friends_relativity.get(hostUserName);
            /*
                Due to Async request of user info and tag
                So we have to ensure both host user name and all relativity have beed filled
             */
        if (friends == null)
        {
            Toast.makeText(getActivity(),
                    ":( Seems you don't have photos tagged on you", Toast.LENGTH_LONG)
                    .show();

            progressBar.setVisibility(View.INVISIBLE);

        }
        if (hostUserName != null && all_friends_relativity != null && friends != null)
        {
            populateDataToListView(friends);

            /*
            Toast.makeText(getActivity().getApplicationContext(),
                    "total photos " + total_photos + "tagged photos " + total_tagged_photos + "you " + photos_of_user + "T " + total_tag,
                    Toast.LENGTH_SHORT).show();
            */
        }

    }


    private void showFriendsRelativity() {

        if (hostUserName != null && photosOfYouDone == true && uploadedPhotosDone == true && response_have_photo_data == true )
        {
            /*
                Due to large amount of calculation, do the computation in background
                and then show the relativity results
             */
            new ShowResultsAsync().execute();

        }
        else if (hostUserName != null && photosOfYouDone == true && uploadedPhotosDone == true && response_have_photo_data == false)
        {
            Toast.makeText(getActivity(),
                    ":( Seems you don't have photos tagged on you", Toast.LENGTH_LONG)
                    .show();

            progressBar.setVisibility(View.INVISIBLE);

        }


    }


    private String extractNextPageToken(String nextLink) throws UnsupportedEncodingException {

        String delims = "[&]+";
        String subString = "after";
        String [] tokens = nextLink.split(delims);
        int index = 0;

        for (int i = 0; i < tokens.length; i++)
        {
            String string = tokens[i];
            if (string.contains(subString))
            {
                index = i;
                break;
            }
        }

        //decode or convert the percent encoding character, = sign, %3D
        String result = java.net.URLDecoder.decode(tokens[index], "UTF-8");
        if(tokens[index] != null)
        {
            String[] str = result.split("=", 2);
            return str[1];

        }
        else{
            return null;
        }
    }

    private void populateDataToListView( Map<String, Double> friends) {

        List<Friend> arrayOfFriends = new ArrayList<Friend>();

        /*
            store top 10 friends name
         */
        if (top10_name_first ==  null){
            top10_name_first = new ArrayList<String>();
        }

        if (top10_name_second == null){
            top10_name_second = new ArrayList<String>();
        }
        int limit = 10; // this limit the number of storing friends name to top 10

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

            /*
                add top 10 friend name, so that the email can be automatically fill with top 10 friend name
             */
            if (sortedFriends.size() <= 10){
                if ( alg == algorithm.firstOrder && top10_name_first.size() < sortedFriends.size()){
                    top10_name_first.add(name);
                }
                else if( alg == algorithm.secondOrder && top10_name_second.size() < sortedFriends.size()){
                    top10_name_second.add(name);
                }
            }
            else if (sortedFriends.size() > 10){
                if ( alg == algorithm.firstOrder && top10_name_first.size() < limit){
                    top10_name_first.add(name);
                }
                else if( alg == algorithm.secondOrder && top10_name_second.size() < limit){
                    top10_name_second.add(name);
                }
            }

        }
        /*
        for (Map.Entry<String, Double> entry : friends.entrySet())
        {
            String name = entry.getKey();
            Number relativity = entry.getValue();

            Friend thefriend = new Friend(name, relativity);
            arrayOfFriends.add(thefriend);
        }
        */
        Log.d(TAG, " " + top10_name_first);
        myAdapter =  new MyAdapter(getActivity(), arrayOfFriends);
        theListView.setAdapter(myAdapter);


        /*
            hide the pregress bar
         */
        progressBar.setVisibility(View.INVISIBLE);

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


    //get specific tagged photos

    private Bundle getRequestParameters(String pageNext)
    {
        Bundle parameters = new Bundle(0);
        parameters.putString("fields", "tags");
        parameters.putString("limit", "25");

        if (pageNext != null)
        {
            parameters.putString("after",pageNext);

        }
        return parameters;
    }

    /*
        parse taggable friends jason data to fill friend's profile image in list view
     */
    private void parseTaggableFriendsJSONData(GraphObject graphObject) {

        // Get the data, parse info to get the key/value info
        JSONObject jsonObject = graphObject
                .getInnerJSONObject();

        try {
            JSONArray outmostArray = jsonObject
                    .getJSONArray("data");
            Log.d(TAG, "outmostArray" + outmostArray.toString());

            /*
                loop tagged photos
             */
            for(int i = 0; i < outmostArray.length(); i++)
            {
                JSONObject obj_of_outmostArray = (JSONObject) outmostArray.get(i);
                System.out.println("aaa " + outmostArray.length());
                Log.d(TAG, "object of outmost array" + obj_of_outmostArray.toString());

                JSONObject obj_of_picture = (JSONObject) obj_of_outmostArray.optJSONObject("picture");
                String taggable_friend_name = obj_of_outmostArray.getString("name");
                if (obj_of_picture != null)
                {
                    JSONObject profile_picture_info = obj_of_picture.getJSONObject("data");
                    String url = (String) profile_picture_info.get("url");

                    taggable_friends.put(taggable_friend_name, url);
                }
            }
            Log.d(TAG,"");


        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*
        parse JSON data for each tagged photo, and store them in ArrayList
     */
    private void parseJSONData(GraphObject graphObject){

        // Get the data, parse info to get the key/value info
        JSONObject jsonObject = graphObject
                .getInnerJSONObject();

        try {
            JSONArray outmostArray = jsonObject
                    .getJSONArray("data");
            Log.d(TAG, "outmostArray" + outmostArray.toString());

            /*
                loop tagged photos
             */
            for(int i = 0; i < outmostArray.length(); i++)
            {
                JSONObject obj_of_outmostArray = (JSONObject) outmostArray.get(i);
                System.out.println("aaa " + outmostArray.length());
                Log.d(TAG, "object of outmost array" + obj_of_outmostArray.toString());

                //Store the coordinates of people in ONE photo
                List<CoordinateOfOneTag> people_coordinates = new ArrayList<CoordinateOfOneTag>();

                /*
                    check if the return result contains object 'tags',
                    Due to some photos did not have any tags with it
                 */
                JSONObject obj_of_tags = (JSONObject) obj_of_outmostArray.optJSONObject("tags");
                String photo_id = obj_of_outmostArray.getString("id");

                /*
                    counting the total number of processed photos
                 */
                boolean check_dup = checkPhotoDuplication(photo_id);
                if (check_dup == false)
                {
                    total_photos ++;
                }

                if (obj_of_tags != null)
                {
                    Log.d(TAG, "object of tags" + obj_of_tags.toString());

                    JSONArray array_of_tags =  obj_of_tags.getJSONArray("data");
                    Log.d(TAG, "tag data" + array_of_tags.toString());
                    int tag_lenth = array_of_tags.length();

                    /*
                        Ignore the photo that has one tag
                     */
                    if (tag_lenth > 1)
                    {
                        /*
                            find tagged people in ONE photo
                        */
                        int tag_counter = 0;

                        //add photo id and check duplication
                        boolean duplicated_photo = addScannedPhoto(photo_id);
                        boolean duplicated_name = false;
                        List<String> DuplicatedName = new ArrayList<String>();

                        if (duplicated_photo == false)//it is NOT duplicated photos
                        {
                            total_tagged_photos++;

                            for(int j = 0; j < array_of_tags.length(); j++)
                            {

                                JSONObject  object = (JSONObject) array_of_tags.get(j);
                                String name = (String) object.get("name");
                                Number x = (Number) object.get("x");
                                Number y = (Number) object.get("y");
                                Log.d(TAG, "specific name and coordinates  " + name  +"  "+ x + "  " + y );

                                duplicated_name = findDuplicatedName(name, DuplicatedName);

                                if( duplicated_name == false)
                                {
                                    CoordinateOfOneTag xy = new CoordinateOfOneTag(name, x, y);
                                    addAllNames(name, all_names);//store all name appeared in all photos

                                    //store all people coordinates appeared in one photo
                                    people_coordinates.add(xy);

                                    //counting total tags
                                    total_tag ++;

                                    // counting photos that user was in it
                                    if (name.equals(hostUserName))
                                    {
                                        photos_of_user ++;
                                    }
                                    tag_counter ++;
                                }

                            }

                            //add the coordinates of ONE photo to the big outer ArrayList
                            all_photos_cooridinates.add(people_coordinates);

                        }

                    }


                    /*
                        print out result for debug
                     */
                    /*
                    for(Map.Entry <String, List<Number>> entry :  people_coordinates.entrySet())
                    {
                        String key = entry.getKey();
                        List<Number> values = entry.getValue();
                        Log.d(TAG, "people coordinates" + key + " " + values);
                    }
                    */

                }
                else
                    continue; //if no tags with this photos, just continue

            }

            List<String> allname = all_names;

            Log.d(TAG, "");

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public Map<String, Map<String, Double>> summationOfRelativity (List<String> all_names,
                                                                           List<List<RelativityOfTwoTags>> relativity_AllPhotos)
    {
        /*
            find out all pair of tags that include the specific name
         */
        TreeMap<String, Map<String, Double>> summation_relativity = new TreeMap<String, Map<String, Double>>(String.CASE_INSENSITIVE_ORDER);

        //loop all names
        for (String nameToFind : all_names)
        {

            List<RelativityOfTwoTags> target_relativity = new ArrayList<RelativityOfTwoTags>();

            //loop for all photos to find out desired target pair of two tags
            for (List<RelativityOfTwoTags> coordinates_one_photo : relativity_AllPhotos)
            {
                //loop all coordinates in one photo
                for (RelativityOfTwoTags r_iterator : coordinates_one_photo)
                {
                    String name_twoTags = r_iterator.getNameOfTwoTags();
                    String [] temp_name = name_twoTags.split(",",0);
                    String name1 = temp_name[0];
                    String name2 = temp_name[1];

                    if (nameToFind.equalsIgnoreCase(name1) || nameToFind.equalsIgnoreCase(name2))
                    {
                        target_relativity.add(r_iterator);
                    }

                }
            }

            //take summation of relativity of all possible two-tags

            Map<String, Double> summationResult = takeSummation(nameToFind, target_relativity);



            summation_relativity.put(nameToFind, summationResult);

        }

        return summation_relativity;
    }

    private Map<String, Double> takeSummation(String nameToFind, List<RelativityOfTwoTags> target_relativity)
    {
        Map<String, Double> sum_of_relativity = new TreeMap<String, Double>(String.CASE_INSENSITIVE_ORDER);


        for (int i = 0; i < target_relativity.size(); i ++)
        {
            RelativityOfTwoTags r_iterator = target_relativity.get(i);

            String name_twoTags = r_iterator.getNameOfTwoTags();
            String [] temp_name = name_twoTags.split(",",0);

            Double sumRelativity = 0.0;
            String friendName0 = null;

            friendName0 = findFriendName(nameToFind,temp_name);


            //check to see if the name already stored in hash map
            Number sum = sum_of_relativity.get(friendName0);

            if (sum == null)
            {
                for(int j = i; j < target_relativity.size(); j ++)
                {

                    RelativityOfTwoTags search_itr = target_relativity.get(j);
                    String search_name_twoTags = search_itr.getNameOfTwoTags();
                    String [] search_temp_name = search_name_twoTags.split(",",0);

                    String friendName1 = findFriendName(nameToFind, search_temp_name);

                    if (friendName0.equalsIgnoreCase(friendName1))
                    {
                        sumRelativity += search_itr.getNormalizedValue().doubleValue();
                    }
                }


                sum_of_relativity.put(friendName0, sumRelativity);
            }

        }
        return sum_of_relativity;
    }

    private String findFriendName(String nameToFind, String[] temp_name)
    {
        String friendName = null;
        for(String friend : temp_name)
        {
            if(!friend.equalsIgnoreCase(nameToFind))
            {
                friendName = friend;
            }
        }
        return friendName;
    }

    /*
        Caculate the distance between two tags on a photo and nomorlized it
     */
    public List<List<RelativityOfTwoTags>> findDistanceBetweenTwoTags(List<List<CoordinateOfOneTag>> all_photos_cooridinates)
    {

        //new array list to hold the relativity of all tagged photos
        List<List<RelativityOfTwoTags>> all_photos_relativity = new ArrayList<List<RelativityOfTwoTags>>();
        /*
            Loop all photos and calculate distance between two tags
         */
        for(int i = 0; i < all_photos_cooridinates.size(); i ++)
        {
            List<CoordinateOfOneTag> coordinates_one_photo = all_photos_cooridinates.get(i);
            //Array list to hold all tagged people in a photo
            List<RelativityOfTwoTags> array_relativity_twoTags = new ArrayList<RelativityOfTwoTags>();

            /*
                 following two for loop used for find all possible combination of two tags in a photo
             */
            for (int j = 0; j < coordinates_one_photo.size(); j++)
            {
                CoordinateOfOneTag nameXY = coordinates_one_photo.get(j);
                String name0 = nameXY.getName();
                Number x0 = nameXY.getX();
                Number y0 = nameXY.getY();

                for (int k = j + 1; k < coordinates_one_photo.size(); k++)
                {
                    CoordinateOfOneTag next_nameXY = coordinates_one_photo.get(k);
                    String name1 = next_nameXY.getName();
                    Number x1 = next_nameXY.getX();
                    Number y1 = next_nameXY.getY();

                    Number distance = Math.sqrt(Math.pow(Math.abs(x1.doubleValue()-x0.doubleValue()),2) +
                                                Math.pow(Math.abs(y1.doubleValue()-y0.doubleValue()), 2));

                    if (distance.doubleValue() == 0)
                    {
                        continue;
                    }
                    String nameOfTwoTag = new StringBuilder().append(name0).append(",").append(name1).toString();

                    RelativityOfTwoTags r = new RelativityOfTwoTags(nameOfTwoTag, distance);
                    array_relativity_twoTags.add(r);

                }
            }

            all_photos_relativity.add(array_relativity_twoTags);
        }

        normalizedDistance(all_photos_relativity);

        return all_photos_relativity;
    }

    public void normalizedDistance(List<List<RelativityOfTwoTags>> all_photos_relativity)
    {
        /*
            loop all photos and find the mallest distance between two tags
         */
        for (List<RelativityOfTwoTags> array_relativity_twoTags : all_photos_relativity)
        {
            /*
                find the smallest distance
             */
            Number minValue = Integer.MAX_VALUE;

            for (RelativityOfTwoTags R_TwoTags : array_relativity_twoTags)
            {
                Number tempValue = R_TwoTags.getDistance();
                if (minValue.doubleValue() > tempValue.doubleValue())
                {
                    minValue = tempValue;
                }
            }

            /*
                 fill in the normalized distance for all pair of two tags in a photo
             */
            for (RelativityOfTwoTags R_TwoTags : array_relativity_twoTags)
            {
                R_TwoTags.setNormalizedValue((minValue.doubleValue()/R_TwoTags.getDistance().doubleValue()));
            }

        }

    }

    class CoordinateOfOneTag{

        private String name;
        private Number x;
        private Number y;

        CoordinateOfOneTag(String name, Number x, Number y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Number getX() {
            return x;
        }

        public void setX(Number x) {
            this.x = x;
        }

        public Number getY() {
            return y;
        }

        public void setY(Number y) {
            this.y = y;
        }
    }

    class RelativityOfTwoTags {

        private String nameOfTwoTags;
        private Number distance;
        private Number normalizedValue;

        RelativityOfTwoTags(String nameOfTwoTags, Number distance) {
            this.nameOfTwoTags = nameOfTwoTags;
            this.distance = distance;
        }

        public String getNameOfTwoTags() {
            return nameOfTwoTags;
        }

        public void setNameOfTwoTags(String nameOfTwoTags) {
            this.nameOfTwoTags = nameOfTwoTags;
        }

        public Number getDistance() {
            return distance;
        }

        public void setDistance(Number distance) {
            this.distance = distance;
        }

        public Number getNormalizedValue() {
            return normalizedValue;
        }

        public void setNormalizedValue(Number normalizedValue) {
            this.normalizedValue = normalizedValue;
        }
    }

    /*
        add all friend name appeared in all photos, and check duplication
     */
    public boolean addAllNames (String name, List<String> all_name)
    {
        //check duplication
        boolean found = false;
        for (String name_iterator : all_name)
        {

            if (name_iterator.equalsIgnoreCase(name))
            {
                found = true;
                return found;
            }
        }

        if (found == false)
        {
            all_name.add(name);
        }

        return found;

    }


    private boolean findDuplicatedName(String name, List<String> duplicatedName) {

        boolean found = false;
        for (String str : duplicatedName)
        {
            if (str.equalsIgnoreCase(name))
            {
                found = true;
                return found;
            }
        }

        if (found == false)
        {

            duplicatedName.add(name);
        }
        return found;
    }

    /*
        add this scanned photo id, in case of duplication of photos in
        'Photo Of You' and 'uploaded photos'
    */
    private boolean addScannedPhoto(String photo_id) {

        boolean found = false;
        for (String id : all_scanned_photos)
        {
            if (id.equals(photo_id))
            {
                found = true;
                return found;
            }

        }

        if( found == false)
        {
            all_scanned_photos.add(photo_id);
        }

        return found;

    }

    /*
        check duplicated photo including  'uploaded' and 'photos of you'
        This is used for counting all photos that had been processed
     */
    private boolean checkPhotoDuplication(String photo_id){

        boolean found = false;
        for (String id : all_photos)
        {
            if (id.equals(photo_id))
            {
                found = true;
                return found;
            }

        }

        if( found == false)
        {
            all_photos.add(photo_id);
        }

        return found;
    }

    /*
        This is the menu options in the action bar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_one) {

            if(alg != algorithm.firstOrder){

                 /*
                    clear all data stored in first order
                    so that first order can be recompute
                 */
                all_names.clear();
                all_scanned_photos.clear();
                all_photos.clear();
                all_photos_cooridinates.clear();
                all_friends_relativity.clear();
                sortedFriends.clear();
                photosOfYouDone = false;
                uploadedPhotosDone = false;
                response_have_photo_data = false;
                total_photos = 0;
                total_tagged_photos = 0;
                photos_of_user = 0;
                total_tag = 0;

                Session session = Session.getActiveSession();

                alg = algorithm.firstOrder;

                progressBar.setVisibility(View.VISIBLE);
                theListView.setAdapter(null);//clear list view when tranfer from second order to first order
                firstOrderRelativity(session);
            }

            return true;
            
        }
        else if (id == R.id.action_two){
            
            /*
                show relativity of second order
             */
            if (alg != algorithm.secondOrder){

                progressBar.setVisibility(View.VISIBLE);
                theListView.setAdapter(null);

                secondOrderRelativity();

                alg = algorithm.secondOrder;
                current_alg.setText("Algo : Second Order");




            }

            return true;
        }
        else if (id == R.id.action_three)
        {
            /*
                logout from facebook
            */
            closeThisApp();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void closeThisApp() {

        Session session = Session.getActiveSession();
        if (session != null) {

            if (!session.isClosed()) {
                session.closeAndClearTokenInformation();

                    /*
                        clear all saved data when logout
                     */
                if (all_friends_relativity.size() != 0){

                    all_names.clear();
                    all_scanned_photos.clear();
                    all_photos.clear();
                    all_photos_cooridinates.clear();
                    all_friends_relativity.clear();
                    sortedFriends.clear();
                    photosOfYouDone = false;
                    uploadedPhotosDone = false;
                    response_have_photo_data = false;
                    total_photos = 0;
                    total_tagged_photos = 0;
                    photos_of_user = 0;
                    total_tag = 0;
                    alg = algorithm.firstOrder;
                    top10_name_first.clear();
                    top10_name_second.clear();

                    theListView.setAdapter(null);
                }


            }
        } else {

            session = new Session(getActivity());
            Session.setActiveSession(session);

            session.closeAndClearTokenInformation();
            //clear your preferences if saved

        }
    }

    private void publishFeedDialog() {

        Bundle params = new Bundle();
        params.putString("name", "Social Relationships");
        params.putString("caption", "Please send us feedback, Thank you");
        params.putString("description", "Exploring you and your friends social relationships based on facebook tagged photos by two types of algorithms: First order and Second order.");
        params.putString("link", "https://play.google.com/store/apps/details?id=com.mengproject.jxy.socialrelationships&hl=en");
        params.putString("picture", "https://raw.github.com/fbsamples/ios-3.x-howtos/master/Images/iossdk_logo.png");

        WebDialog feedDialog = (
                new WebDialog.FeedDialogBuilder(getActivity(),
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error == null) {
                            // When the story is posted, echo the success
                            // and the post Id.
                            final String postId = values.getString("post_id");
                            if (postId != null) {

                                // User clicked the Cancel button
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Successful published",
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                // User clicked the Cancel button
                                Toast.makeText(getActivity().getApplicationContext(),
                                        "Publish cancelled",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else if (error instanceof FacebookOperationCanceledException) {
                            // User clicked the "x" button
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "Publish cancelled",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Generic, ex: network error
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "Error posting story",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                })
                .build();
        feedDialog.show();

    }

    private void sendFeedback() {

        /* Create the Intent */
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String first_order_name = "First order:\n";
        String second_order_name = "Second order:\n";

        if (top10_name_first != null){

            for (String str : top10_name_first){
                first_order_name += str + "\n";
            }
        }

        if (top10_name_second != null){
            for (String str : top10_name_second){
                second_order_name += str + "\n";
            }
        }

        String questions =  "Total number of photos: " + total_photos + "\n"+
                            "Total number of tags: " + total_tag + "\n" +
                            "Total number of photos with more than two tags: " + total_tagged_photos + "\n" +
                            "Total number of photos with more than two tags and tagged on YOU: " + photos_of_user + "\n" +
                            "---------------------------------------------------------" + "\n" +
                            "1. How many of the 10 names shown are your close personal connections?" + "\n" +
                            "First order: " + "\n" +
                            "Second order: "+ "\n\n" +
                            "2. How many close personal connections are missing from this list?" + "\n" +
                            "First order: " + "\n" +
                            "Second order: "+ "\n\n" +
                            "3. Here is your friends list of calculated order, please rank this calculated order by number based on your real case:" + "\n" +
                            "e.g." + "\n" +
                            "Sample 2" + "\n" +
                            "Sample 4" + "\n" +
                            "Sample 6" + "\n" +
                            "Sample 1" + "\n" +
                            "..." + "\n\n" +
                            "Please rank your friends list:" + "\n" +
                            "";

        String feedback = questions + first_order_name + "\n" + second_order_name;

        /* Fill it with Data */
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"jiangxiaoyong904@gmail.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, feedback );

        /* Send it off to the Activity-Chooser */
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    /*********************************************************************************************************

        Code Below deal with second order relationships

     *********************************************************************************************************/

    private void secondOrderRelativity() {

        //constructSeondOrderGraph();


        new SecondOrderAsync().execute();

    }

    public void secondOrderDirect(){

        /*
            store new second order relativity of all friends
         */
        SO_all_friends_relativity = new TreeMap<String, Map<String, Double>>(String.CASE_INSENSITIVE_ORDER);

        /*
            loop all people name, perform second order for each people with direct relationships
         */
        for (Map.Entry<String, Map<String, Double>> entry : all_friends_relativity.entrySet())
        {
            Log.d(TAG, "name " + entry.getKey() );
            String current_name = entry.getKey().toString();

            /*
                    construct new all_friends_relativity graph
                    and write into new second order relativity
             */
            Map<String, Double> SO_treemap_one_friend = new TreeMap<String, Double>(String.CASE_INSENSITIVE_ORDER);

            /*
                Retrieve the friend list of current people
             */
            Map<String, Double> friends_list = all_friends_relativity.get(current_name);
            for(Map.Entry<String, Double> friends_R : friends_list.entrySet())
            {

                String target_name = friends_R.getKey();
                Double relativity_target = friends_R.getValue();

                Double SO_relativity = relativity_target;


                /*
                    loop the remaining friends, except target friend
                    to see if remaining friends have direct relationship with the target
                 */
                for(Map.Entry<String, Double> remaining_friends : friends_list.entrySet())
                {
                    String friend_name = remaining_friends.getKey();
                    Double relativity_friend = remaining_friends.getValue();


                    /*
                        if current loop friend name equal to target friend name, ignore it.
                     */
                    if(friend_name.equals(target_name))
                    {
                        continue;
                    }

                    /*
                        find the friend list of remaining friends
                        to see if the friend list containg target friend name
                     */
                    Map<String, Double> friend_list_remaining = all_friends_relativity.get(friend_name);
                    if(friend_list_remaining.containsKey(target_name))
                    {
                        /*
                            retrieve the relativity
                         */
                        Double relativity_more = friend_list_remaining.get(target_name);
                        Double temp_ratio = (relativity_more*relativity_friend) / (relativity_friend + relativity_more);
                        SO_relativity += temp_ratio;
                    }

                }
                Log.d(TAG, "SO_relativity = " + SO_relativity);

                //record new second order into the tree map of this specific friend relativity
                SO_treemap_one_friend.put(target_name, SO_relativity);
            }

            //record new second order relativity for all people
            SO_all_friends_relativity.put(current_name, SO_treemap_one_friend);

        }


    }

    public void secondOrderIndirect(){

        /*
            find friends that do not have direct relationship with current showing people
            for each people
         */
        for (Map.Entry<String, Map<String, Double>> entry : all_friends_relativity.entrySet())
        {
            String current_name = entry.getKey().toString();


            /*
                Retrieve the friends list of current people
                and get the friends list of indirect relationships
             */
            List<String> indirect_friends = findIndirectFriendsList(current_name);

            /*
                loop indirect friend list and calculate second order
             */
            Map<String, Double> friends_list = all_friends_relativity.get(current_name);
            for (String indirect_name : indirect_friends)
            {
                Double r_indirect = 0.0;
                /*
                    loop friends list to see if the list contain this specific friend name
                 */
                for (Map.Entry<String, Double> iterator : friends_list.entrySet())
                {
                    String friend_name = iterator.getKey();
                    Double r1 = iterator.getValue();

                    /*
                        acquire the friend list of friends
                     */
                    Map<String, Double> friends_list_more = all_friends_relativity.get(friend_name);
                    if (friends_list_more.containsKey(indirect_name))
                    {
                        Double r2= friends_list_more.get(indirect_name);
                        Double temp_ratio = (r1 * r2) / (r1 + r2);
                        r_indirect += temp_ratio;
                    }

                }
                /*
                    record this new indirect relationships of second order for current people
                 */
                if(r_indirect != 0.0)
                {
                    Map<String, Double> SO_friends_list = SO_all_friends_relativity.get(current_name);
                    SO_friends_list.put(indirect_name, r_indirect);
                }

            }

        }

    }

    class SecondOrderAsync extends AsyncTask< Void, Void, Void >
    {

        @Override
        protected Void doInBackground(Void... params) {

         /*
            calculation of direct relationships
         */
            secondOrderDirect();

        /*
            calculation of indirect relationships
         */
            secondOrderIndirect();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            showResults(SO_all_friends_relativity);
        }
    }

    private List<String> findIndirectFriendsList(String current_name) {

        Map<String, Double> friends_list = all_friends_relativity.get(current_name);
        List<String> indirect_friends = new ArrayList<String>();

        for(Map.Entry<String, Map<String, Double>> entry : all_friends_relativity.entrySet())
        {
            String friend_name = entry.getKey();

            /*
                ignore the name of current computing people
             */
            if (friend_name.equals(current_name))
            {
                continue;
            }
            if (!friends_list.containsKey(friend_name))// find the indirect friend
            {
                indirect_friends.add(friend_name);
            }
        }
        return indirect_friends;
    }


    private void constructSeondOrderGraph() {

        WeightedGraph<String, DefaultWeightedEdge> graph =
                new ListenableUndirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        //temporary store all friends name
        List<String> friends_Name = new ArrayList<String>();

        /*
            add vertices by using all friends name
         */
        for (Map.Entry<String, Map<String, Double>> entry : all_friends_relativity.entrySet())
        {
            Log.d(TAG, "name " + entry.getKey() );
            graph.addVertex(entry.getKey().toString());

            friends_Name.add(entry.getKey().toString());
        }

        /*
            add edge and corresponding weight
         */
        for (int i = 0 ; i < friends_Name.size(); i ++)
        {
            String name1 = friends_Name.get(i);
            Map<String, Double> friend_R_info =  all_friends_relativity.get(name1);

            for (Map.Entry<String, Double> entry : friend_R_info.entrySet())
            {
                String name2 = entry.getKey();
                Double relativity = entry.getValue();

                /*
                    only add non-exist edge
                 */
                if (graph.getEdge(name1, name2) == null)
                {
                    DefaultWeightedEdge we = graph.addEdge(name1,name2);//add edge
                    graph.setEdgeWeight(we, relativity);//set weight
                }

            }
            Log.d(TAG, graph.toString());

        }

        GraphIterator<String, DefaultWeightedEdge> iterator = new BreadthFirstIterator<String, DefaultWeightedEdge>(graph,"Xiaoyong Jiang");
        while (iterator.hasNext())
        {
            Log.d(TAG, iterator.next());
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REAUTH_ACTIVITY_CODE) {
            uiHelper.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        /*
            during dedug press back button to re-show listview
            I thought it should call onSessionStateChange
        */

        /*
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session, session.getState(), null);
        }
        */

        uiHelper.onResume();

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        uiHelper.onSaveInstanceState(bundle);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();

        closeThisApp();
    }

}
