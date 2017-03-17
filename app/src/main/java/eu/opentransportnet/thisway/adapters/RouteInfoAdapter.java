package eu.opentransportnet.thisway.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import eu.opentransportnet.thisway.fragments.TabRouteInfo;
import eu.opentransportnet.thisway.fragments.TabRouteMap;

/**
 * @author Kristaps Krumins
 */
public class RouteInfoAdapter extends FragmentStatePagerAdapter {

    private CharSequence mTitles[];
    private int mNumbOfTabs;
    private String mEncodedKml;
    private boolean mIsMyRoute = false;

    // Build a Constructor and assign the passed Values to appropriate values in the class
    public RouteInfoAdapter(FragmentManager fm, CharSequence mTitles[], int mNumbOfTabsumb) {
        super(fm);

        this.mTitles = mTitles;
        this.mNumbOfTabs = mNumbOfTabsumb;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            TabRouteInfo tab1 = new TabRouteInfo();
            Bundle args = new Bundle();
            args.putBoolean("is_my_route", mIsMyRoute);
            tab1.setArguments(args);
            return tab1;
        } else {
            TabRouteMap tab2 = new TabRouteMap();
            Bundle args = new Bundle();
            args.putString("encodedKml", mEncodedKml);
            args.putInt("addMyMarker", 1); // always add my loc marker
            tab2.setArguments(args);
            return tab2;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getCount() {
        return mNumbOfTabs;
    }

    public void setEncodedKml(String encodedKml) {
        mEncodedKml = encodedKml;
    }

    public void setMyRoute(boolean value){
        mIsMyRoute = value;
    }
}
