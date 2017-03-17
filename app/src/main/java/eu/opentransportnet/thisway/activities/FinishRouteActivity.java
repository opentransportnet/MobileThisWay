package eu.opentransportnet.thisway.activities;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.adapters.FinishRouteAdapter;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.utils.CustomViewPager;
import eu.opentransportnet.thisway.utils.SlidingTabLayout;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.RouteRecorder;

/**
 * @author Kristaps Krumins
 */
public class FinishRouteActivity extends BaseActivity implements View.OnClickListener {

    private CustomViewPager mPager;
    private FinishRouteAdapter mAdapter;
    private SlidingTabLayout mTabs;
    private int mNumberOfTabs = 2;
    private String mTabTitles[] = new String[mNumberOfTabs];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(R.layout.activity_finish_route);
        setToolbarTitle(R.string.title_activity_finish_route);
        initToolbarBackBtn();
        initLoseFocusInContent();
        Typeface tf = getTypeFace();
        Bundle extras = getIntent().getExtras();

        Resources resources = getResources();
        mTabTitles[0] = resources.getString(R.string.icon_info) + "  "
                + resources.getString(R.string.route_info);
        mTabTitles[1] = resources.getString(R.string.icon_map) + "  "
                + resources.getString(R.string.route_map);

        // Creating The ViewPagerAdapter and Passing Fragment Manager, mTabTitles fot the Tabs and Number Of Tabs.
        mAdapter = new FinishRouteAdapter(getSupportFragmentManager(), mTabTitles, mNumberOfTabs);

        // Assigning ViewPager View and setting the mAdapter
        mPager = (CustomViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPagingEnabled(false);

        // Assiging the Sliding Tab Layout View
        mTabs = (SlidingTabLayout) findViewById(R.id.tabs);
        mTabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.colorTabsScroll);
            }

            @Override
            public int getDividerColor(int position) {
                return getResources().getColor(R.color.colorTabsDivider);
            }
        });
        mTabs.setDistributeEvenly(true);
        // Setting the ViewPager For the SlidingTabsLayout
        mTabs.setViewPager(mPager);

        TextView tf1 = (TextView) findViewById(R.id.tf_1);
        tf1.setTypeface(tf);
        TextView tf2 = (TextView) findViewById(R.id.tf_2);
        tf2.setTypeface(tf);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        RouteRecorder rr = MainActivity.getRouteRecorder();
        double[] startPoint = rr.getStartPointCoord();
        TextView startAddress = (TextView) findViewById(R.id.start_address);
        startAddress.setText(
                Utils.getLocAddress(this, startPoint[0], startPoint[1])
        );
        TextView endAddress = (TextView) findViewById(R.id.end_address);
        endAddress.setText(
                Utils.getLocAddress(this, extras.getDouble("latitude"),
                        extras.getDouble("longitude"))
        );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
            case R.id.close_button:
                Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainActivity.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                startActivity(mainActivity);
                break;
        }
    }
}
