package eu.opentransportnet.thisway.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.adapters.RouteInfoAdapter;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.CustomViewPager;
import eu.opentransportnet.thisway.utils.SessionManager;
import eu.opentransportnet.thisway.utils.SlidingTabLayout;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class RouteInfoActivity extends BaseActivity implements View.OnClickListener {
    private CustomViewPager mPager;
    private RouteInfoAdapter mAdapter;
    private SlidingTabLayout mTabs;
    private int mNumberOfTabs = 2;
    private String mTabTitles[] = new String[mNumberOfTabs];
    private JSONObject mRouteInfo;
    private String mEncodedKml;
    public static String sJsonInfo = null;
    public static String sJsonStatistics = null;
    public static boolean sLocalRoute = false;
    public static String sLocalFileName = null;
    public static int sTrackId = -1;
    private Context mContext;
    private SessionManager mSessionManager;

    public static final String PATH_ADD_VIEWS = "/platform/tracks/addStatistics/views";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        mSessionManager = new SessionManager(this);
        setContentView(R.layout.activity_route_info);
        setToolbarTitle(R.string.title_activity_route);
        mContext = getBaseContext();
        initToolbarBackBtn();
        Typeface tf = getTypeFace();
        Bundle extras = getIntent().getExtras();

        Button closeButton = (Button) findViewById(R.id.close_button);
        closeButton.setVisibility(View.VISIBLE);
        closeButton.setTypeface(tf);
        closeButton.setOnClickListener(this);

        boolean isMyRoute = extras.getBoolean("isMyRoute");

        if (isMyRoute) {
            Button editButton = (Button) findViewById(R.id.edit_button);
            editButton.setVisibility(View.VISIBLE);
            editButton.setTypeface(tf);
            editButton.setOnClickListener(this);

            Button deleteButton = (Button) findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setTypeface(tf);
            deleteButton.setOnClickListener(this);
        }

        if (extras != null) {
            sJsonInfo = extras.getString("jsonInfo");
            sJsonStatistics = extras.getString("jsonStatistics");
            sLocalRoute = extras.getBoolean("localRoute");
            if (sLocalRoute) {
                sLocalFileName = extras.getString("localFileName");
            }
        }

        Resources resources = getResources();
        mTabTitles[0] = resources.getString(R.string.icon_info) + "  " + resources.getString(R.string.route_info);
        mTabTitles[1] = resources.getString(R.string.icon_map) + "  " + resources.getString(R.string.route_map);
        mAdapter = new RouteInfoAdapter(getSupportFragmentManager(), mTabTitles, mNumberOfTabs);
        mAdapter.setMyRoute(isMyRoute);

        if (sJsonInfo != null) {
            try {
                mRouteInfo = new JSONObject(sJsonInfo);
                extractDataFromJson(mRouteInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                JSONObject id = new JSONObject(sJsonInfo);
                sTrackId = id.getInt("trackId");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Assigning ViewPager View and setting the mAdapter
        mPager = (CustomViewPager) findViewById(R.id.pager);

        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(1);
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

        FloatingActionButton goButton = (FloatingActionButton) findViewById(R.id.go);
        goButton.setOnClickListener(this);


        JSONObject obj = new JSONObject();
        try {
            obj.put("trackId", sTrackId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_ADD_VIEWS;

        Requests.sendRequest(this, false, url, obj,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject response) {
                    }

                    @Override
                    public void onError(JSONObject object) {
                    }
                }, null);

        TextView goBtn = (TextView) findViewById(R.id.go_btn_txt);
        String goBtnTxt = (String) goBtn.getText();

        if (goBtnTxt.length() > 2) {
            goBtn.setTextSize(16);
        }

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
            case R.id.delete_button:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete) + "!")
                        .setMessage(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                DeleteTrack(sLocalRoute);
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;
            case R.id.go:
                try {
                    byte[]  decodedBytes = Base64.decode(mEncodedKml, Base64.DEFAULT);
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.showToastAtTop(mContext, getString(R.string.something_went_wrong));
                    break;
                }
                Intent routeNavigation = new Intent(this, RouteNavigationActivity.class);
                routeNavigation.putExtra("encodedKml", mEncodedKml);
                routeNavigation.putExtra("trackId", sTrackId);
                startActivity(routeNavigation);
                break;
            case R.id.edit_button:
                findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

                boolean requestSent = Requests.updateTrackPublicStatus(this, sTrackId, ((Switch) findViewById(R.id.is_public)).isChecked(),
                        new VolleyRequestListener<JSONObject>() {
                            @Override
                            public void onResult(JSONObject response) {
                                Utils.showToastAtTop(mContext, getString(R.string
                                        .route_updated));
                                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(JSONObject response) {
                                Utils.showToastAtTop(mContext, getString(R.string.something_went_wrong));
                                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                            }
                        },
                        null);

                if (requestSent) {
                    //good
                } else {
                    Utils.showToastAtTop(mContext, getString(R.string.something_went_wrong));
                    findViewById(R.id.progress_bar).setVisibility(View.GONE);
                }
                break;
        }
    }

    private void extractDataFromJson(JSONObject routeInfo) {
        try {
            mEncodedKml = routeInfo.getString("route_kml");
            mAdapter.setEncodedKml(mEncodedKml);
            TextView startAddressV = (TextView) findViewById(R.id.start_address);
            TextView endAddressV = (TextView) findViewById(R.id.end_address);
            String startAddress = routeInfo.getString("start_address");
            String endAddress = routeInfo.getString("end_address");
            startAddressV.setText(startAddress);
            endAddressV.setText(endAddress);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete users track if chosen so
     * @param local if track is local or on server
     */
    public void DeleteTrack(boolean local) {
        if (local) {
            String path = sLocalFileName;
            // Delete local files
            File file = new File(sLocalFileName);
            file.delete();
            path = path.replace(".json", ".csv");
            file = new File(path);
            file.delete();
            RouteListActivity.getRouteListActivity().removeLastLoadedRouteFromList();

            finish();

            Utils.showToastAtTop(mContext, getString(R.string.delete_activity_stats));
        } else {
            FrameLayout spinner;
            spinner = (FrameLayout) findViewById(R.id.progress_bar);
            spinner.setVisibility(View.VISIBLE);
            int path = sTrackId;
            org.json.simple.JSONObject objs = new org.json.simple.JSONObject();

            objs.put("trackId", path);
            objs.put("userId", mSessionManager.getUser().getHashedEmail());
            objs.put("appId", Const.APPLICATION_ID);


            String jsonBodyString = ((org.json.simple.JSONObject) objs).toJSONString();
            JSONObject jsonBody = null;
            try {
                jsonBody = new JSONObject(jsonBodyString);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Requests.sendRequest(this, "http://" + Utils.getHostname() + Utils.getUrlPathStart() + Requests.PATH_DELETE_TRACK,
                    jsonBody, new VolleyRequestListener<JSONObject>() {
                        @Override
                        public void onResult(JSONObject trackList) {
                            if (trackList != null) {
                                String start = getString(R.string.delete_activity_stats);
                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(R.id.progress_bar);
                                spinner.setVisibility(View.GONE);
                                finish();
                                RouteListActivity.getRouteListActivity().removeLastLoadedRouteFromList();
                                Utils.showToastAtTop(mContext, start);
                            } else {
                                String start = getString(R.string.something_went_wrong);
                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(R.id.progress_bar);
                                spinner.setVisibility(View.GONE);
                                Utils.showToastAtTop(mContext, start);
                            }

                        }

                        @Override
                        public void onError(JSONObject error) {
                            String start = getString(R.string.something_went_wrong);
                            FrameLayout spinner;
                            spinner = (FrameLayout) findViewById(R.id.progress_bar);
                            spinner.setVisibility(View.GONE);
                            Utils.showToastAtTop(mContext, start);
                        }
                    }, "edittrack");
        }
    }
}
