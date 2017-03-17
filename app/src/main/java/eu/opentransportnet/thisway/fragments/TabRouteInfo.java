package eu.opentransportnet.thisway.fragments;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import eu.opentransportnet.thisway.activities.RouteInfoActivity;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.network.RequestQueueSingleton;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.SessionManager;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Ilmars Svilsts
 */
public class TabRouteInfo extends Fragment implements View.OnClickListener {

    public static final String PATH_ADD_VERIFICATION = "/platform/tracks/addVerification";
    public final String TAG_STOP_VERIFICATION = "delete user request";
    private static final String LOG_TAG = "TabRouteInfo";

    private Activity mActivity;
    private Typeface mTypeface;
    private final int mAppId = Const.APPLICATION_ID;
    public int mTrackId = 0;
    public View mView;
    public Boolean mMyRoute=false;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        mTypeface = Typeface.createFromAsset(getContext().getAssets(), Const.FONTELLO_PATH);
        View v = inflater.inflate(eu.opentransportnet.thisway.R.layout.tab_route_info, container, false);
        mView = v;

        if (getArguments().getBoolean("is_my_route")) {
            mMyRoute=true;
            v.findViewById(eu.opentransportnet.thisway.R.id.layout_is_public).setVisibility(View.VISIBLE);
        }

        TextView c = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.views_and_navigations);
        c.setText(getString(eu.opentransportnet.thisway.R.string.viewed) + ": 0   " + getString(eu.opentransportnet.thisway.R.string.navigated) + ": 0");

        Typeface tf = getTypeFace();
        TextView d = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.verify_image);
        d.setTypeface(tf);

        v.findViewById(eu.opentransportnet.thisway.R.id.verify_poi).setOnClickListener(this);

        JSONObject obj;
        JSONObject objstats;
        String json = RouteInfoActivity.sJsonInfo;
        String jsonstats = RouteInfoActivity.sJsonStatistics;

        try {
            obj = new JSONObject(json);

            ((Switch) v.findViewById(eu.opentransportnet.thisway.R.id.is_public)).setChecked(obj.getBoolean("is_public"));

            TextView sd = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.distance_route_info);

            float f = Float.parseFloat(obj.getString("distance"));
            float fs = Float.parseFloat(obj.getString("duration"));

            sd.setText(String.format("%.02f", f / 1000) + " km");
            sd = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.duration_route_info);

            sd.setText(String.format("%.02f", fs).split("\\.|,")[0] + "h " + String.format("%.02f", fs).split("\\.|,")[1] + "min");
            sd = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.description_route_info);
            sd.setText(obj.getString("description"));

            mTrackId = obj.getInt("trackId");
            Utils.logD(LOG_TAG, String.valueOf(obj));
            try {
                if (obj.getBoolean("user_verified")) {
                    FloatingActionButton verify_poi = (FloatingActionButton) mActivity.findViewById(eu.opentransportnet.thisway.R.id.verify_poi);
                    verify_poi.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
                    TextView img = (TextView) mActivity.findViewById(eu.opentransportnet.thisway.R.id.verify_image);
                    img.setTextColor(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.white)));
                }
            } catch (Throwable t) {
                Utils.logD(LOG_TAG, "Could not parse malformed JSON: Not verified!");
            }
            try {
                if (!obj.getString("weatherList").equals(null) && !obj.getString("weatherList").isEmpty()) {
                    JSONArray dd = obj.getJSONArray("weatherList");
                    for (int i = 0; i < dd.length(); i++) {
                        JSONObject a = dd.getJSONObject(i);
                        int b = a.getInt("weatherTypeId");
                        if (b == 10) {
                            ImageView z = (ImageView) v.findViewById(eu.opentransportnet.thisway.R.id.sun_img);
                            z.setVisibility(View.VISIBLE);
                        }
                        if (b == 20) {
                            ImageView z = (ImageView) v.findViewById(eu.opentransportnet.thisway.R.id.cloud_img);
                            z.setVisibility(v.VISIBLE);
                        }
                        if (b == 30) {
                            ImageView z = (ImageView) v.findViewById(eu.opentransportnet.thisway.R.id.snow_img);
                            z.setVisibility(v.VISIBLE);
                        }
                    }
                }
            } catch (Throwable t) {
                Utils.logD(LOG_TAG, "Could not parse malformed JSON: No weather type!");
            }
            try {
                if (!obj.getString("trackRatings").equals(null) && !obj.getString("trackRatings").isEmpty()) {
                    JSONArray dd = obj.getJSONArray("trackRatings");
                    for (int i = 0; i < dd.length(); i++) {
                        JSONObject a = dd.getJSONObject(i);
                        int b = a.getInt("ratingTypeId");
                        if (b == 1) {
                            RatingBar z = (RatingBar) v.findViewById(eu.opentransportnet.thisway.R.id.rtbProductRating);
                            z.setRating(a.getInt("avgRate"));
                        }
                        if (b == 2) {
                            RatingBar z = (RatingBar) v.findViewById(eu.opentransportnet.thisway.R.id.rtb);
                            z.setRating(a.getInt("avgRate"));
                        }
                        if (b == 3) {
                            RatingBar z = (RatingBar) v.findViewById(eu.opentransportnet.thisway.R.id.ProductRating);
                            z.setRating(a.getInt("avgRate"));
                        }
                    }
                }
            } catch (Throwable t) {
                Utils.logD(LOG_TAG, "Could not parse malformed JSON: No ratings!");
            }

        } catch (Throwable t) {
            Utils.logD(LOG_TAG, "Could not parse malformed JSON: Something went totaly wrong!");
        }

        try {
            objstats = new JSONObject(jsonstats);
            TextView sd = (TextView) v.findViewById(eu.opentransportnet.thisway.R.id.views_and_navigations);
            if (10000 > Integer.parseInt(objstats.getString("views")) && 10000 > Integer.parseInt(objstats.getString("navigation"))) {
                sd.setText(getString(eu.opentransportnet.thisway.R.string.viewed) + ": " + objstats.getString("views") + "   " + getString(eu.opentransportnet.thisway.R.string.navigated) + ": " + objstats.getString("navigation"));
            } else if (10000 < Integer.parseInt(objstats.getString("views")) && 10000 > Integer.parseInt(objstats.getString("navigation"))) {
                sd.setText(getString(eu.opentransportnet.thisway.R.string.viewed) + ": 10000+   " + getString(eu.opentransportnet.thisway.R.string.navigated) + ": " + objstats.getString("navigation"));
            } else if (10000 > Integer.parseInt(objstats.getString("views")) && 10000 < Integer.parseInt(objstats.getString("navigation"))) {
                sd.setText(getString(eu.opentransportnet.thisway.R.string.viewed) + ": " + objstats.getString("views") + "   " + getString(eu.opentransportnet.thisway.R.string.navigated) + ": 10000+");
            } else {
                sd.setText(getString(eu.opentransportnet.thisway.R.string.viewed) + ": 10000+   " + getString(eu.opentransportnet.thisway.R.string.navigated) + ": 10000+");
            }

        } catch (Throwable t) {
            Utils.logD(LOG_TAG, "Could not parse malformed JSON: \"" + json + "\"");
        }

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(mActivity).cancelAllPendingRequests(TAG_STOP_VERIFICATION);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.thisway.R.id.verify_poi:
                sendVerification();
                break;
        }
    }

    public Typeface getTypeFace() {
        return mTypeface;
    }

    /**
     * Verify route
     */
    public void sendVerification() {

        if(mMyRoute) {
            Button deleteButton = (Button) mActivity.findViewById(eu.opentransportnet.thisway.R.id.delete_button);
            deleteButton.setVisibility(View.INVISIBLE);
        }
        final FrameLayout progress = (FrameLayout) mActivity.findViewById(eu.opentransportnet.thisway.R.id.vprogress);
        progress.setVisibility(View.VISIBLE);
        JSONObject obj = new JSONObject();
        try {
            obj.put("trackId", mTrackId);
            obj.put("appId", mAppId);
            obj.put("userId", new SessionManager(mActivity).getUser().getHashedEmail());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_ADD_VERIFICATION;

        boolean requestSent = Requests.sendRequest(mActivity, true, url, obj, new VolleyRequestListener<JSONObject>() {
            @Override
            public void onResult(JSONObject response) {
                int responseCode = 2;
                try {
                    responseCode = response.getInt("responseCode");

                    if (responseCode == 0 || responseCode == 501) {
                        Utils.showToastAtTop(mActivity, getString(eu.opentransportnet.thisway.R.string
                                .verification));
                        FloatingActionButton fabsnow = (FloatingActionButton) mActivity.findViewById(eu.opentransportnet.thisway.R.id.verify_poi);
                        fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
                        TextView img = (TextView) mActivity.findViewById(eu.opentransportnet.thisway.R.id.verify_image);
                        img.setTextColor(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.white)));

                        String verified_count = response.getString("verification");
                        TextView z = (TextView) mActivity.findViewById(eu.opentransportnet.thisway.R.id.verify_count_route_info);
                        z.setText(verified_count);

                        LinearLayout vshow = (LinearLayout) mActivity.findViewById(eu.opentransportnet.thisway.R.id.hhide);
                        vshow.setVisibility(View.GONE);
                        vshow = (LinearLayout) mActivity.findViewById(eu.opentransportnet.thisway.R.id.hshow);
                        vshow.setVisibility(View.VISIBLE);
                    } else {
                        Utils.showToastAtTop(mActivity, getString(eu.opentransportnet.thisway.R.string
                                .something_went_wrong));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Utils.showToastAtTop(mActivity, getString(eu.opentransportnet.thisway.R.string
                            .something_went_wrong));

                }
                if(mMyRoute) {
                    Button deleteButton = (Button) mActivity.findViewById(eu.opentransportnet.thisway.R.id.delete_button);
                    deleteButton.setVisibility(View.VISIBLE);
                }
                progress.setVisibility(View.GONE);
            }

            @Override
            public void onError(JSONObject object) {
                Utils.showToastAtTop(mActivity, getString(eu.opentransportnet.thisway.R.string
                        .something_went_wrong));
                if(mMyRoute) {
                    Button deleteButton = (Button) mActivity.findViewById(eu.opentransportnet.thisway.R.id.delete_button);
                    deleteButton.setVisibility(View.VISIBLE);
                }
                progress.setVisibility(View.GONE);
            }
        }, TAG_STOP_VERIFICATION);

        if (!requestSent) {
            progress.setVisibility(View.GONE);
        }
    }
}