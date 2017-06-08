package com.ureca.acousticfingerprint;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;

public class MainActivity extends FragmentActivity implements ListenFragment.MyInterface {

    SharedPreferences sharedpreferences;
    ViewPagerNoSwipe viewPager;
    SampleFragmentPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        viewPager = (ViewPagerNoSwipe) findViewById(R.id.viewpager);
        viewPager.setPagingEnabled(false);

        pagerAdapter =
                new SampleFragmentPagerAdapter(getSupportFragmentManager(), MainActivity.this);
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position != 0)
                    pagerAdapter.listenFragment.onPause();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);

    }

    @Override
    public void storeAd(final Advertisement advertisement) {
        final HistoryFragment fragment = (HistoryFragment) pagerAdapter.getItem(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.insertAd(advertisement);
            }
        });
    }

}
