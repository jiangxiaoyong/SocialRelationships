package com.mengproject.jxy.socialrelationships;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.widget.LoginButton;

import java.util.Arrays;

/**
 * Created by jxy on 19/01/15.
 */
public class WelcomFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.welcom,container, false);

        LoginButton authButton = (LoginButton) view.findViewById(R.id.login_button);
        authButton.setReadPermissions(Arrays.asList("user_photos", "user_friends"));

        return view;
    }
}
