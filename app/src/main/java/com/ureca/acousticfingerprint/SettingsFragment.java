package com.ureca.acousticfingerprint;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;

/**
 * Created by Andrew on 10/19/16.
 */

// In this case, the fragment displays simple text based on the page
public class SettingsFragment extends PreferenceFragmentCompat {

    SharedPreferences sharedpreferences;
    EditTextPreference userName;
    ListPreference recInterval;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        //Call your Fragment functions that uses getActivity()
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        userName = (EditTextPreference) findPreference("username");
        recInterval = (ListPreference) findPreference("recInterval");
    }

    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_page, container, false);
        return view;
    }
    */
}
