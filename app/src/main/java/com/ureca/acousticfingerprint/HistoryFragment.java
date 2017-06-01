package com.ureca.acousticfingerprint;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.test.espresso.core.deps.guava.reflect.TypeToken;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrew on 10/19/16.
 */

// In this case, the fragment displays simple text based on the page
public class HistoryFragment extends Fragment {

    SharedPreferences sharedpreferences;
    FloatingActionButton addAd;
    FloatingActionButton removeAd;
    Editor prefsEditor;
    Gson gson = new Gson();
    AdvertisementRecycleViewAdapter adapter;
    private Boolean isStarted = false;
    private Boolean isVisible = false;
    private List<Advertisement> ads = new ArrayList<>();
    private String jsonAds;
    private RecyclerView rv;

    public static HistoryFragment newInstance() {
        HistoryFragment fragment = new HistoryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        jsonAds = sharedpreferences.getString("adList", "");
        if (!jsonAds.isEmpty()) {
            Type type = new TypeToken<List<Advertisement>>() {
            }.getType();
            ArrayList<Advertisement> newlist = gson.fromJson(jsonAds, type);
            ads.addAll(newlist);
        }

        adapter = new AdvertisementRecycleViewAdapter(ads, getActivity().getApplication());
        adapter.setHasStableIds(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recyclerview_fragment, container, false);
        rv = (RecyclerView) view.findViewById(R.id.rv);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity()) {
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);

        /*
        addAd = (FloatingActionButton) view.findViewById(R.id.add_ad);
        addAd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rv.getLayoutManager().scrollToPosition(0);
                adapter.insert(0, new Advertisement("Lorem Ipsum", "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer luctus tellus vitae sapien tristique, a egestas odio vehicula. Donec at tincidunt tortor. Cras sed lacinia tortor. Vivamus porta egestas ante. Cras dignissim, enim vitae dictum ultricies, lacus nisl dapibus ligula, a sodales dolor dolor in neque. Fusce feugiat at erat non condimentum. Curabitur a aliquam lectus, eu facilisis ex.", "http://www.google.com", R.drawable.test, -1));
                //removeAd.setVisibility(adapter.getAds().isEmpty()?View.GONE:View.VISIBLE);
            }
        });
        */

        removeAd = (FloatingActionButton) view.findViewById(R.id.remove_ad);
        //removeAd.setVisibility(ads.isEmpty()?View.GONE:View.VISIBLE);
        removeAd.setImageResource(R.drawable.ic_clear_white_36px);
        removeAd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                // set title
                alertDialogBuilder.setTitle("Confirmation");

                // set dialog message
                alertDialogBuilder
                        .setMessage("Are you sure you want to delete all saved advertisements?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, close
                                // current activity
                                adapter.removeAll();
                                //removeAd.setVisibility(adapter.getAds().isEmpty()?View.GONE:View.VISIBLE);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                dialog.cancel();
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();

            }
        });

        return view;
    }

    /*
    private void refresh() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.fadein,
                R.anim.fadeout);
        ft.detach(this).attach(this).commit();
    }
    */

    @Override
    public void onStart() {
        super.onStart();
        isStarted = true;
        rv.setAdapter(adapter);

        /*Call your Fragment functions that uses getActivity()
        if (isVisible && isStarted) {
            updateView();
        }
        */
    }

    /*
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isStarted && isVisible) {
            updateView();
        }
    }
    */

    @Override
    public void onStop() {
        super.onStop();
        prefsEditor = sharedpreferences.edit();
        jsonAds = gson.toJson(adapter.getAds());
        prefsEditor.putString("adList", jsonAds);
        prefsEditor.apply();
        //Log.d("TAG","jsonCars = " + jsonAds);
    }
    /*
    public void updateView() {

    }
    */

    public void insertAd(Advertisement advertisement) {
        adapter.insert(0, advertisement);
    }
}
