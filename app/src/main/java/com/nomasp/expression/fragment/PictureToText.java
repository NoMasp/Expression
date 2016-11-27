package com.nomasp.expression.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nomasp.expression.R;


/**
 * Created by nomasp on 09/10/2016.
 */
public class PictureToText extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_picture2text, container, false);
        return rootView;
    }
}
