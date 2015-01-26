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
        List<Map<String, List<Number>>> all_photos_cooridinates = new ArrayList<Map<String, List<Number>>>();

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
                Map<String, List<Number>> people_coordinates = new HashMap<String, List<Number>>();

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

                        List<Number> coordinate = new ArrayList<Number>();
                        coordinate.add(x);
                        coordinate.add(y);

                        people_coordinates.put(name, coordinate);
                        tag_counter ++;
                    }


                    if(tag_counter == 1)
                    {
                        continue;//Ignore the photo that has one tag
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
        findDistanceBetweenTwoTag(all_photos_cooridinates);

    }

    public void findDistanceBetweenTwoTag(List<Map<String, List<Number>>> all_photos_cooridinates)
    {

    }

    class relativityOfTwoTags {


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
