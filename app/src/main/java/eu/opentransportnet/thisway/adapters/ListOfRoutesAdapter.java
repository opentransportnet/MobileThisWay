package eu.opentransportnet.thisway.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import eu.opentransportnet.thisway.fragments.TabListOfRoutes;

/**
 * @author Kristaps Krumins
 */
public class ListOfRoutesAdapter extends FragmentStatePagerAdapter {

    private CharSequence mTitles[];
    private int mNumbOfTabs;
    private boolean mOnlyMyRoutes;

    // Build a Constructor and assign the passed Values to appropriate values in the class
    public ListOfRoutesAdapter(FragmentManager fm, CharSequence mTitles[], int mNumbOfTabs,
                               boolean onlyMyRoutes) {
        super(fm);

        this.mTitles = mTitles;
        this.mNumbOfTabs = mNumbOfTabs;
        this.mOnlyMyRoutes = onlyMyRoutes;
    }

    @Override
    public Fragment getItem(int position) {
        TabListOfRoutes tab2 = new TabListOfRoutes();
        Bundle args = new Bundle();
        args.putBoolean("onlyMyRoutes", mOnlyMyRoutes);
        tab2.setArguments(args);
        return tab2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getCount() {
        return mNumbOfTabs;
    }
}
