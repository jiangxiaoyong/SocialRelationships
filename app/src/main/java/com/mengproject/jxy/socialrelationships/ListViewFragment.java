package com.mengproject.jxy.socialrelationships;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.ProfilePictureView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jxy on 23/01/15.
 */
public class ListViewFragment extends Fragment {


    private static final String TAG = "ListViewFragment";
    private static final int REAUTH_ACTIVITY_CODE = 100;

    private ProfilePictureView profilePictureView;
    private TextView userNameView;

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
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

        // Check for an open session
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            // Get the user's data
            makeUserInfoRequest(session);
        }


        /*
            fill in list view
         */
        String [] show = new String[] { "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2" };

       ArrayAdapter<String> theAdapter =  new MyAdapter(getActivity(), show);

        ListView theListView = (ListView)view.findViewById(R.id.list);
        theListView.setAdapter(theAdapter);

        return view;
    }


    /*  anoher method to create list view
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
            // Get the user's data.
            makeUserInfoRequest(session);
            makeRelationshipRequest(session);
        }else if (state.isClosed()) {
            Log.d(TAG, "Logged out...");
        }
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
                            }
                        }
                        if (response.getError() != null) {
                            // Handle errors, will do so later.
                        }
                    }
                });
        request.executeAsync();
    }

    private void makeRelationshipRequest(final Session session){

        new Request(session, "me/photos/uploaded",getRequestParameters(), HttpMethod.GET, new Request.Callback()
        {
            @Override
            public void onCompleted(Response response)
            {

                Log.i(TAG, "Response Result: " + response.toString());
                // Process the returned response
                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null) {
                    if (graphObject.getProperty("data") != null) {

                        //parse the JSON data and store in data structure
                        parseJSONData(graphObject);

                    }
                }

            }
        }).executeAsync();

    }

    //get specific tagged photos
    private Bundle getRequestParameters()
    {
        Bundle parameters = new Bundle(2);
        parameters.putString("fields", "tags");
        return parameters;
    }

    /*
        parse JSON data for each tagged photo, and store them in ArrayList
     */
    private void parseJSONData(GraphObject graphObject){

        // Get the data, parse info to get the key/value info
        JSONObject jsonObject = graphObject
                .getInnerJSONObject();

        //This is big array list to hold the coordinates of all photos
        List<List<CoordinateOfOneTag>> all_photos_cooridinates = new ArrayList<List<CoordinateOfOneTag>>();

        //Array list to store the name of all people appeared in all photos
        List<String> all_names = new ArrayList<String>();

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
                Log.d(TAG, "object of outmost array" + obj_of_outmostArray.toString());

                //Store the coordinates of people in ONE photo
                List<CoordinateOfOneTag> people_coordinates = new ArrayList<CoordinateOfOneTag>();

                /*
                    check if the return result contains object 'tags',
                    Due to some photos did not have any tags with it
                 */
                JSONObject obj_of_tags = (JSONObject) obj_of_outmostArray.optJSONObject("tags");
                if (obj_of_tags != null)
                {
                    Log.d(TAG, "object of tags" + obj_of_tags.toString());

                    JSONArray array_of_tags =  obj_of_tags.getJSONArray("data");
                    Log.d(TAG, "tag data" + array_of_tags.toString());

                    /*
                      find tagged people in ONE photo
                    */
                    int tag_counter = 0;
                    for(int j = 0; j < array_of_tags.length(); j++)
                    {
                        JSONObject  object = (JSONObject) array_of_tags.get(j);
                        String name = (String) object.get("name");
                        Number x = (Number) object.get("x");
                        Number y = (Number) object.get("y");
                        Log.d(TAG, "specific name and coordinates  " + name  +"  "+ x + "  " + y );


                        CoordinateOfOneTag xy = new CoordinateOfOneTag(name, x, y);
                        people_coordinates.add(xy);

                        //store all name appeared in all photos
                        addAllNames(name, all_names);

                        tag_counter ++;
                    }

                    /*
                        Ignore the photo that has one tag
                     */
                    if(tag_counter == 1)
                    {
                        all_names.remove((all_names.size() - 1));
                        continue;
                    }
                    else
                    {
                        //add the coordinates of ONE photo to the big outer ArrayList
                        all_photos_cooridinates.add(people_coordinates);
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

        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*
            Further calculate the distance between two tags
         */
        List<List<RelativityOfTwoTags>> relativity_AllPhotos = findDistanceBetweenTwoTags(all_photos_cooridinates);

        /*
            summation of relativity between two tags of all photos
         */
        HashMap<String, HashMap<String, Number>> summation_relativity = summationOfRelativity(all_names, relativity_AllPhotos);

    }

    public HashMap<String, HashMap<String, Number>> summationOfRelativity (List<String> all_names,
                                                                           List<List<RelativityOfTwoTags>> relativity_AllPhotos)
    {
        /*
            find out all pair of tags that include the specific name
         */
        HashMap<String, HashMap<String, Number>> summation_relativity = new HashMap<String, HashMap<String, Number>>();

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

                    if (nameToFind.equals(name1) || nameToFind.equals(name2))
                    {
                        target_relativity.add(r_iterator);
                    }

                }
            }

            //take summation of relativity of all possible two-tags
            HashMap<String, Number> summationResult = takeSummation(nameToFind, target_relativity);

        }

        return summation_relativity;
    }

    private HashMap<String, Number> takeSummation(String nameToFind, List<RelativityOfTwoTags> target_relativity)
    {
        HashMap<String, Number> sum_of_relativity = new HashMap<String, Number>();

        for (RelativityOfTwoTags r_iterator : target_relativity)
        {
            String name_twoTags = r_iterator.getNameOfTwoTags();
            String [] temp_name = name_twoTags.split(",",0);
            String name1 = temp_name[0];
            String name2 = temp_name[1];
        }
        return sum_of_relativity;
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

    public void addAllNames (String name, List<String> all_name)
    {
        boolean found = false;
        for (String name_iterator : all_name)
        {
            if (name_iterator.equals(name))
            {
                found = true;
            }
        }

        if (found == false)
        {
            all_name.add(name);
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
    }

}
