package eu.opentransportnet.thisway.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.adapters.RoutesListView;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.models.RouteItem;
import eu.opentransportnet.thisway.network.RequestQueueSingleton;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.network.UploadTask;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * @author Kristaps Krumins
 */
public class RouteListActivity extends BaseActivity implements View.OnClickListener {

    private static final String REQUEST_TAG_GET_TRACKS = "getUsersTracks";
    private static final String REQUEST_TAG_GET_TRACK_INFO = "getTrackInfo";
    private static final String REQUEST_TAG_GET_TRACK_RATING = "getTrackRating";

    private static RouteListActivity sActivity;

    private JSONArray mLocalRoutesJson;
    private RoutesListView mItemsAdapter;
    private View mProgressBar;
    private String lastJsonFromGetTrack;
    private ArrayList<RouteItem> mListOfAllRoutes = new ArrayList<>();
    private RouteItem mLastLoadedRoute = null;
    private int mLastLoadedRoutePosition = -1;
    private boolean mShowNoRoutesUploadedView = false;
    boolean mOnlyMyRoutes = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        sActivity = this;
        setContentView(R.layout.activity_route_list);
        setToolbarTitle(R.string.title_activity_route_list);
        initToolbarBackBtn();
        Typeface tf = getTypeFace();
        Bundle extras = getIntent().getExtras();

        Button closeButton = (Button) findViewById(R.id.close_button);
        closeButton.setVisibility(View.VISIBLE);
        closeButton.setTypeface(tf);
        closeButton.setOnClickListener(this);

        TextView tf1 = (TextView) findViewById(R.id.tf_1);
        tf1.setTypeface(tf);

        TextView tf2 = (TextView) findViewById(R.id.tf_2);
        tf2.setTypeface(tf);

        mProgressBar = findViewById(R.id.loading_panel);

        boolean filterByPoints = false;
        if (extras != null) {
            mOnlyMyRoutes = extras.getBoolean("onlyMyRoutes");
            filterByPoints = extras.getBoolean("filterByPoints");
            boolean showAddressBox = extras.getBoolean("showAddressBox", true);

            if (showAddressBox == false) {
                findViewById(R.id.address_box).setVisibility(View.GONE);
                closeButton.setVisibility(View.INVISIBLE);
            }
        }

        UploadTask.sUploadTracks = false;
        mLocalRoutesJson = Utils.getAllLocalRoutes(this);

        if (mLocalRoutesJson.length() == 0) {
            mShowNoRoutesUploadedView = true;
        }

        String[] localRouteFileNames = Utils.getLocalRouteFileNames();

        for (int i = 0; i < mLocalRoutesJson.length(); i++) {
            try {
                JSONObject route = mLocalRoutesJson.getJSONObject(i);
                mListOfAllRoutes.add(
                        new RouteItem(
                                "",
                                route.getString("start_address"),
                                route.getString("end_address"),
                                localRouteFileNames[i]
                        )
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mItemsAdapter = new RoutesListView(this, mListOfAllRoutes);

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(mItemsAdapter);

        boolean requestSent = getUsersTracks(mOnlyMyRoutes, filterByPoints);
        if (requestSent) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            if (!Utils.isConnected(this)) {
                Utils.showToastAtTop(this, getString(R.string.network_unavailable));
            }
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {

                if (position < mLocalRoutesJson.length()) {
                    String jsonString = null;
                    try {
                        jsonString = mLocalRoutesJson.getJSONObject(position).toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Utils.showToastAtTop(sActivity, getResources().getString(R.string.track_cloud));
                    Intent routeInfo = new Intent(sActivity, RouteInfoActivity.class);
                    // Put local JSON file
                    routeInfo.putExtra("jsonInfo", jsonString);
                    routeInfo.putExtra("localRoute", true);
                    routeInfo.putExtra("localFileName", mListOfAllRoutes.get(position).getLocalFileName());
                    startActivity(routeInfo);
                    mLastLoadedRoute = mListOfAllRoutes.get(position);
                    mLastLoadedRoutePosition = position;
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    boolean requestSent = getTrackInfo(position);
                    if (!requestSent) {
                        mProgressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(getApplicationContext(),
                                getString(R.string.failed_to_load_track));
                    }
                }
            }
        });

        TextView startPoint = (TextView) findViewById(R.id.start_address);
        startPoint.setText(extras.getString("startPoint"));

        TextView destPoint = (TextView) findViewById(R.id.end_address);
        destPoint.setText(extras.getString("destPoint"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        RequestQueueSingleton requestQueueSingleton = RequestQueueSingleton.getInstance(this);
        requestQueueSingleton.cancelAllPendingRequests(REQUEST_TAG_GET_TRACKS);
        requestQueueSingleton.cancelAllPendingRequests(REQUEST_TAG_GET_TRACK_INFO);
        requestQueueSingleton.cancelAllPendingRequests(REQUEST_TAG_GET_TRACK_RATING);

        UploadTask.sUploadTracks = true;
    }

    private boolean getUsersTracks(boolean onlyMyRoutes, boolean filterByPoints) {
        return Requests.getUsersTracks(this, onlyMyRoutes, filterByPoints,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject response) {
                        mProgressBar.setVisibility(View.GONE);
                        JSONArray routes;
                        try {
                            int responseCode = response.getInt("responseCode");

                            if (responseCode == 0) {
                                routes = response.getJSONArray("trackList");

                                for (int i = 0; i < routes.length(); i++) {
                                    JSONObject route = (JSONObject) routes.get(i);
                                    RouteItem routeItem = new RouteItem(
                                            route.getString("trackId"),
                                            route.getString("start_address"),
                                            route.getString("end_address")
                                    );
                                    mListOfAllRoutes.add(routeItem);
                                    mItemsAdapter.add(routeItem);
                                }
                                mItemsAdapter.notifyDataSetChanged();
                            } else if (responseCode == 1) {
                                if (mShowNoRoutesUploadedView) {
                                    findViewById(R.id.list).setVisibility(View.GONE);
                                    findViewById(R.id.no_routes_uploaded).setVisibility(View.VISIBLE);
                                }

                                return;
                            } else {
                                Utils.showToastAtTop(getApplicationContext(), getString(R.string.something_went_wrong));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.showToastAtTop(getApplicationContext(), getString(R.string.something_went_wrong));
                            return;
                        }
                    }

                    @Override
                    public void onError(JSONObject response) {
                        mProgressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(getApplicationContext(), getString(R.string.server_error));
                    }
                }, REQUEST_TAG_GET_TRACKS);
    }

    private boolean getTrackInfo(final int position) {
        final String trackId = mListOfAllRoutes.get(position).getRouteId();
        return Requests.getTrackInfo(getApplicationContext(), trackId,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject object) {
                        lastJsonFromGetTrack = object.toString();
                        getTrackStatistics(trackId);
                        mLastLoadedRoute = mListOfAllRoutes.get(position);
                    }

                    @Override
                    public void onError(JSONObject object) {
                        mProgressBar.setVisibility(View.GONE);
                        Utils.showToastAtTop(getApplicationContext(),
                                getString(R.string.failed_to_load_track));
                    }
                }, REQUEST_TAG_GET_TRACK_INFO);
    }

    private boolean getTrackStatistics(String trackId) {
        return Requests.getTrackStatistics(getApplicationContext(), trackId,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject object) {
                        Intent routeInfo = new Intent(sActivity, RouteInfoActivity.class);
                        routeInfo.putExtra("jsonInfo", lastJsonFromGetTrack);
                        routeInfo.putExtra("jsonStatistics", object.toString());
                        routeInfo.putExtra("localRoute", false);
                        routeInfo.putExtra("isMyRoute", mOnlyMyRoutes);
                        startActivity(routeInfo);
                        mProgressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(JSONObject object) {
                        Intent routeInfo = new Intent(sActivity, RouteInfoActivity.class);
                        routeInfo.putExtra("jsonInfo", lastJsonFromGetTrack);
                        routeInfo.putExtra("jsonStatistics", object.toString());
                        routeInfo.putExtra("localRoute", false);
                        routeInfo.putExtra("isMyRoute", mOnlyMyRoutes);
                        startActivity(routeInfo);
                        mProgressBar.setVisibility(View.GONE);
                    }
                }, REQUEST_TAG_GET_TRACK_RATING);
    }

    public void removeLastLoadedRouteFromList() {
        if (mLastLoadedRoute != null) {
            mItemsAdapter.remove(mLastLoadedRoute);
            mListOfAllRoutes.remove(mLastLoadedRoute);
            mItemsAdapter.notifyDataSetChanged();
            if (mLastLoadedRoutePosition > -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mLocalRoutesJson.remove(mLastLoadedRoutePosition);
                } else {
                    mLocalRoutesJson = Utils.removeJSONArrayItem(mLocalRoutesJson,
                            mLastLoadedRoutePosition);
                }
            }
        }
    }

    public static RouteListActivity getRouteListActivity() {
        return sActivity;
    }
}
