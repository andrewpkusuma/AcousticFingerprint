package com.ureca.acousticfingerprint;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by Andrew on 10/19/16.
 */

public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 3;
    ListenFragment listenFragment = ListenFragment.newInstance();
    HistoryFragment historyFragment = HistoryFragment.newInstance();
    SettingsFragment settingsFragment = SettingsFragment.newInstance();
    private String tabTitles[] = new String[] { "Listen", "History", "Settings" };
    private Context context;

    public SampleFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return listenFragment;
            case 1:
                return historyFragment;
            case 2:
                return settingsFragment;
            default:
                return PageFragment.newInstance();
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}