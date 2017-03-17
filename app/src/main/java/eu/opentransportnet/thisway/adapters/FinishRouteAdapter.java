package eu.opentransportnet.thisway.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import eu.opentransportnet.thisway.activities.MainActivity;
import eu.opentransportnet.thisway.fragments.TabFinishRouteInfo;
import eu.opentransportnet.thisway.fragments.TabRouteMap;
import eu.opentransportnet.thisway.utils.Const;

/**
 * @author Kristaps Krumins
 */
public class FinishRouteAdapter extends FragmentStatePagerAdapter {

    private CharSequence mTitles[];
    private int mNumbOfTabs;

    public FinishRouteAdapter(FragmentManager fm, CharSequence mTitles[], int mNumbOfTabs) {
        super(fm);

        this.mTitles = mTitles;
        this.mNumbOfTabs = mNumbOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            TabFinishRouteInfo tab1 = new TabFinishRouteInfo();
            return tab1;
        } else {
            TabRouteMap tab2 = new TabRouteMap();
            Bundle args = new Bundle();
            String routeFileName = MainActivity.getRouteRecorder().getCurrRecordedRoute();
            String kmlPath = MainActivity.getAppCtx().getFilesDir() + "/" +
                    Const.APP_ROUTES_CSV_PATH + "/" + routeFileName + ".kml";
            args.putString("kmlPath", kmlPath);
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
}
