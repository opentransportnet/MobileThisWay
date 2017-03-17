package eu.opentransportnet.thisway.activities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.SessionManager;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Kristaps Krumins
 * @author Ilmārs Svilsts
 */
public class ShowPOIActivity extends BaseActivity implements View.OnClickListener {
    public static final String PATH_ADD_VERIFICATION = "/platform/pois/addVerification";
    public static final String PATH_RATE_POI = "/platform/pois/addPoiRatings";

    private int mPoiId = 0;
    private boolean mIsPublic = false;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        mContext = this;
        setContentView(eu.opentransportnet.thisway.R.layout.activity_show_poi);
        setToolbarTitle(eu.opentransportnet.thisway.R.string.title_activity_show_poi);
        initToolbarBackBtn();
        Typeface tf = getTypeFace();
        initIcons(tf);
        findViewById(eu.opentransportnet.thisway.R.id.verify_poi).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.rate_poi).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.rate_button).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.discard_button).setOnClickListener(this);

        Bundle bundle = getIntent().getExtras();
        mPoiId = bundle.getInt("poi_id");
        mIsPublic = bundle.getBoolean("is_public");

        if (!mIsPublic) {
            Button deleteButton = (Button) findViewById(eu.opentransportnet.thisway.R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setTypeface(tf);
            deleteButton.setOnClickListener(this);
        }

        RatingBar z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.complexity_raiting);
        z.setRating(0);
        z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.trafic_raiting);
        z.setRating(0);
        z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.quciknes_raiting);
        z.setRating(0);

        TextView a = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi);
        a.setText("");
        a = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
        a.setText("");

        FloatingActionButton fabsnow = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.verify_poi);
        fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.white)));
        TextView f = (TextView) findViewById(eu.opentransportnet.thisway.R.id.verify_count);
        f.setText("0");
        LinearLayout vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vhide);
        vshow.setVisibility(View.VISIBLE);
        vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vshow);
        vshow.setVisibility(View.GONE);

        String poi = Requests.PATH_LOAD_PUBLIC_POI;
        FrameLayout spinner;
        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
        spinner.setVisibility(View.VISIBLE);
        JSONObject objs = new JSONObject();

        if (mIsPublic) {
            try {
                objs.put("poiId", mPoiId);
                objs.put("lang", getResources().getConfiguration().locale.getLanguage());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            try {
                objs.put("appId", Const.APPLICATION_ID);
                objs.put("userId", new SessionManager(this).getUser().getHashedEmail());
                objs.put("poiId", mPoiId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            poi = Requests.PATH_LOAD_POI;
        }

        Requests.sendRequest(this, "http://" + Utils.getHostname() + Utils.getUrlPathStart() + poi,
                objs, new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject mUseritem) {
                        if (mUseritem != null) {
                            String a = "2";
                            String check = "0";
                            try {
                                a = mUseritem.getString("responseCode");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            if (check.equals(a)) {

                                try {
                                    if (!mUseritem.getString("transportName").equals(null) && !mUseritem.getString("transportName").isEmpty() && mUseritem.getString("transportName") != "null") {
                                        if (mUseritem.getString("transportName").equals("Bike")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_bicycle_rate);
                                        }
                                        if (mUseritem.getString("transportName").equals("Train")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_tram_rate);
                                        }
                                        if (mUseritem.getString("transportName").equals("Bus")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_bus_rate);
                                        }
                                        if (mUseritem.getString("transportName").equals("Metro")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_underground_rate);
                                        }
                                        if (mUseritem.getString("transportName").equals("Car_Motorcycle")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_car_rate);
                                        }
                                        if (mUseritem.getString("transportName").equals("Walk")) {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
                                            z.setText(eu.opentransportnet.thisway.R.string.icon_walking_rate);
                                        }
                                    }
                                } catch (Throwable t) {
                                }

                                if (!mIsPublic) {
                                    try {
                                        if (!mUseritem.getString("description").equals(null) && !mUseritem.getString("description").isEmpty() && mUseritem.getString("description") != "null") {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.description_poi);
                                            z.setText(mUseritem.getString("description"));
                                            LinearLayout n = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.showPOIActivity_description_layout);
                                            n.setVisibility(View.VISIBLE);
                                        }

                                    } catch (Throwable t) {
                                    }
                                    try {
                                        if (!mUseritem.getString("name").equals(null) && !mUseritem.getString("name").isEmpty() && mUseritem.getString("name") != "null") {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi);
                                            z.setText(mUseritem.getString("name"));
                                        }

                                    } catch (Throwable t) {
                                    }
                                } else {
                                    try {
                                        if (!mUseritem.getString("categoryName").equals(null) && !mUseritem.getString("categoryName").isEmpty() && mUseritem.getString("categoryName") != "null") {
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi);
                                            z.setText(mUseritem.getString("categoryName"));
                                        }
                                    } catch (Throwable t) {
                                    }
                                }
                                try {
                                    if (!mUseritem.getString("poiRatings").equals(null) && !mUseritem.getString("poiRatings").isEmpty()) {
                                        JSONArray dd = mUseritem.getJSONArray("poiRatings");
                                        for (int i = 0; i < dd.length(); i++) {
                                            JSONObject ab = dd.getJSONObject(i);
                                            int b = ab.getInt("ratingTypeId");
                                            if (b == 1) {
                                                RatingBar z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.complexity_raiting);
                                                z.setRating(ab.getInt("avgRate"));
                                            }
                                            if (b == 2) {
                                                RatingBar z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.trafic_raiting);
                                                z.setRating(ab.getInt("avgRate"));
                                            }
                                            if (b == 3) {
                                                RatingBar z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.quciknes_raiting);
                                                z.setRating(ab.getInt("avgRate"));
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                }

                                try {
                                    String verified = mUseritem.getString("user_verified");
                                    if (verified == "true") {
                                        FloatingActionButton fabsnow = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.verify_poi);
                                        fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));

                                        try {
                                            String verified_count = mUseritem.getString("verified");
                                            TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.verify_count);
                                            z.setText(verified_count);
                                        } catch (Throwable t) {

                                        }
                                        LinearLayout vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vhide);
                                        vshow.setVisibility(View.GONE);
                                        vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vshow);
                                        vshow.setVisibility(View.VISIBLE);
                                    }
                                } catch (Throwable t) {
                                    try {
                                        String verified_count = mUseritem.getString("verified");
                                        TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.verify_count_showPoi);
                                        z.setText(verified_count);
                                    } catch (Throwable z) {

                                    }
                                }

                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                spinner.setVisibility(View.GONE);
                            } else {
                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                spinner.setVisibility(View.GONE);
                                Utils.showToastAtTop(mContext, getString(eu.opentransportnet.thisway.R.string
                                        .server_error));
                            }
                        }
                    }

                    @Override
                    public void onError(JSONObject error) {
                        FrameLayout spinner;
                        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                        spinner.setVisibility(View.GONE);
                        Utils.showToastAtTop(mContext, getString(eu.opentransportnet.thisway.R.string.server_error));
                    }
                }, "");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case eu.opentransportnet.thisway.R.id.verify_poi:
                sendVerification();
                break;
            case eu.opentransportnet.thisway.R.id.rate_poi:
                FrameLayout rate = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.add_poi_raiting);
                rate.setVisibility(View.VISIBLE);
                break;
            case eu.opentransportnet.thisway.R.id.rate_button:

                FrameLayout rate_hide = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.add_poi_raiting);
                rate_hide.setVisibility(View.GONE);
                sendrating();

                break;
            case eu.opentransportnet.thisway.R.id.discard_button:
                rate_hide = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.add_poi_raiting);
                rate_hide.setVisibility(View.GONE);
                break;
            case eu.opentransportnet.thisway.R.id.delete_button:
                deletePoi();
                break;
        }
    }

    /**
     * Delete user POI
     */
    private void deletePoi() {
        findViewById(eu.opentransportnet.thisway.R.id.progresss).setVisibility(View.VISIBLE);

        boolean requestSent = Requests.deletePoi(this, mPoiId, new
                VolleyRequestListener<JSONObject>
                        () {
                    @Override
                    public void onResult(JSONObject response) {
                        findViewById(eu.opentransportnet.thisway.R.id.progresss).setVisibility(View.GONE);
                        int rc = Utils.getResponseCode(response);

                        if (rc == 0) {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.thisway.R.string.poi_deleted));
                            finish();
                            return;
                        } else if (rc == 502) {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.thisway.R.string.poi_delete_only_owner));
                        } else {
                            Utils.showToastAtTop(mContext, mContext.getString(eu.opentransportnet.thisway.R.string.something_went_wrong));
                        }
                    }

                    @Override
                    public void onError(JSONObject response) {
                        findViewById(eu.opentransportnet.thisway.R.id.progresss).setVisibility(View.GONE);
                    }
                }, null);

        if (!requestSent) {
            findViewById(eu.opentransportnet.thisway.R.id.progresss).setVisibility(View.GONE);
        }
    }

    /**
     * Verify poi, if user wants to
     */
    public void sendVerification() {
        final FrameLayout progress = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
        progress.setVisibility(View.VISIBLE);
        JSONObject obj = new JSONObject();
        try {
            obj.put("poiId", mPoiId);
            obj.put("appId", Const.APPLICATION_ID);
            obj.put("userId", new SessionManager(this).getUser().getHashedEmail());
            obj.put("is_public", mIsPublic);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_ADD_VERIFICATION;

        boolean requestSent = Requests.sendRequest(this, true, url, obj,
                new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject response) {
                        int responseCode = 2;
                        try {
                            responseCode = response.getInt("responseCode");

                            if (responseCode == 0 || responseCode == 501) {
                                Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                        .verification));
                                FloatingActionButton fabsnow = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.verify_poi);
                                fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));


                                try {
                                    String verified_count = response.getString("verification");
                                    TextView z = (TextView) findViewById(eu.opentransportnet.thisway.R.id.verify_count);
                                    z.setText(verified_count);
                                } catch (Throwable t) {

                                }
                                LinearLayout vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vhide);
                                vshow.setVisibility(View.GONE);
                                vshow = (LinearLayout) findViewById(eu.opentransportnet.thisway.R.id.vshow);
                                vshow.setVisibility(View.VISIBLE);

                                progress.setVisibility(View.GONE);


                            } else {
                                Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                        .something_went_wrong));
                                progress.setVisibility(View.GONE);
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                    .something_went_wrong));
                            progress.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(JSONObject object) {
                        Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                .something_went_wrong));
                        progress.setVisibility(View.GONE);
                    }
                }, null);

        if (!requestSent) {
            progress.setVisibility(View.GONE);
        }
    }

    /**
     * rate POI
     */
    public void sendrating() {
        final FrameLayout progress = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
        progress.setVisibility(View.VISIBLE);
        JSONObject obj = new JSONObject();


        try {
            obj.put("poiId", mPoiId);
            obj.put("appId", Const.APPLICATION_ID);
            obj.put("is_public", mIsPublic);
            obj.put("userId", new SessionManager(this).getUser().getHashedEmail());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            JSONArray myArray = new JSONArray();
            try {
                RatingBar z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.complexity_ratingBar);
                float s = z.getRating();
                if (s > 0) {
                    JSONObject jo = new JSONObject();
                    jo.put("ratingTypeId", 1);
                    jo.put("rate", s);
                    myArray.put(jo);
                }

                z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.traffic_ratingBar);
                s = z.getRating();
                if (s > 0) {
                    JSONObject jo1 = new JSONObject();
                    jo1.put("ratingTypeId", 2);
                    jo1.put("rate", s);
                    myArray.put(jo1);
                }

                z = (RatingBar) findViewById(eu.opentransportnet.thisway.R.id.quickness_ratingBar);
                s = z.getRating();
                if (s > 0) {
                    JSONObject jo2 = new JSONObject();
                    jo2.put("ratingTypeId", 3);
                    jo2.put("rate", s);
                    myArray.put(jo2);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            obj.put("poiRatings", myArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_RATE_POI;

        boolean requestSent = Requests.sendRequest(this, true, url, obj, new VolleyRequestListener<JSONObject>() {
            @Override
            public void onResult(JSONObject response) {
                int responseCode = 2;
                try {
                    responseCode = response.getInt("responseCode");

                    if (responseCode == 0) {
                        Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                .saved_raiting));

                        progress.setVisibility(View.GONE);
                    } else {
                        Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                                .something_went_wrong));
                        progress.setVisibility(View.GONE);
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                            .something_went_wrong));
                    progress.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(JSONObject object) {
                Utils.showToastAtTop(getApplicationContext(), getString(eu.opentransportnet.thisway.R.string
                        .something_went_wrong));
                progress.setVisibility(View.GONE);
            }
        }, null);

        if (!requestSent) {
            progress.setVisibility(View.GONE);
        }
    }

    private void initIcons(Typeface tf) {
        TextView s = (TextView) findViewById(eu.opentransportnet.thisway.R.id.name_poi_img);
        s.setTypeface(tf);
        s = (TextView) findViewById(eu.opentransportnet.thisway.R.id.verify_image);
        s.setTypeface(tf);
    }
}
