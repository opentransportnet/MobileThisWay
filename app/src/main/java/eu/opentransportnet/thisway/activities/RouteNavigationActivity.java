package eu.opentransportnet.thisway.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.utils.Classificators;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.RouteRecorder;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class RouteNavigationActivity extends BaseActivity implements View.OnClickListener, LocationListener {
    private RouteRecorder mRouteRec;
    private String mEncodedKml;
    private String mTmpKmlFilePath;
    private ChangePoint[] mChangePoints = null;
    private int mRadius1 = 100;
    private boolean mShowChangePointAlert = false;
    // Index 0 is latitude and index 1 is longitude
    private double[] mDestPoint = new double[2];
    private boolean mDestReached = false;
    private int mDestRadius = 10;
    private Typeface mTypeFace;
    private int mTrackId = -1;
    public static final String PATH_ADD_RATING = "/platform/tracks/addTrackRatings";
    public static final String PATH_ADD_NAVIGATED = "/platform/tracks/addStatistics/navigation";
    public Context mContext;

    private class ChangePoint {
        double lat;
        double lng;
        int transportId;
        boolean showRadius = true;
        boolean showTime = true;
        int distance;
        int seconds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(R.layout.activity_route_navigation);
        setToolbarTitle(R.string.title_activity_route);
        initToolbarBackBtn();
        mContext = getBaseContext();
        mEncodedKml = getIntent().getStringExtra("encodedKml");
        mTrackId = getIntent().getIntExtra("trackId", -1);

        mTmpKmlFilePath = Utils.getRouteKmlFile(this, mEncodedKml);
        initChangePoints();
        mTypeFace = getTypeFace();

        mRouteRec = (RouteRecorder) getFragmentManager().findFragmentById(R.id.route_recorder);
        mRouteRec.setDefaultLocation(Const.DEFAULT_LATITUDE, Const.DEFAULT_LONGITUDE);
        mRouteRec.setTracking(true);
        mRouteRec.addLocationListener(this);
        mRouteRec.addJavascriptInterface(this, "NavigationActivity");
        mRouteRec.loadWebView();
        mRouteRec.loadJsFile("www/js/KML.js");
        mRouteRec.loadJsFile("www/js/navigation.js");

        FloatingActionButton stop = (FloatingActionButton) findViewById(R.id.stop);
        stop.setOnClickListener(this);

        Button ok = (Button) findViewById(R.id.ok_button);
        ok.setOnClickListener(this);

        Button cancel = (Button) findViewById(R.id.skip_button);
        cancel.setOnClickListener(this);


        JSONObject obj = new JSONObject();
        try {
            obj.put("trackId", mTrackId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_ADD_NAVIGATED;

        Requests.sendRequest(this, true, url, obj, new VolleyRequestListener<JSONObject>() {
            @Override
            public void onResult(JSONObject object) {}
            @Override
            public void onError(JSONObject object) {}
        }, null);
    }

    public void onDestroy() {
        super.onDestroy();
        mRouteRec.removeListener(this);
        // RequestQueueSingleton.getInstance(mContext).cancelAllPendingRequests();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
            case R.id.stop:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                finish();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.exit).setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener).show();
                break;
            case R.id.ok_button:
                if (RouteInfoActivity.sLocalRoute) {
                    Intent mainActivity = new Intent(getApplicationContext(),
                            MainActivity.class);
                    mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainActivity.addFlags(Intent
                            .FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    startActivity(mainActivity);
                    Utils.showToastAtTop(mContext, getString(R.string
                            .saved_raiting));
                } else {


                    final FrameLayout stop2 = (FrameLayout) findViewById(R.id.rate);
                    final FrameLayout rate = (FrameLayout) findViewById(R.id.frameLayout);
                    rate.setVisibility(View.VISIBLE);


                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("trackId", mTrackId);
                        obj.put("appId", Const.APPLICATION_ID);
                        obj.put("userId", Utils.getHashedUserEmail(this));
                        JSONArray myArray = new JSONArray();
                        try {
                            RatingBar z = (RatingBar) findViewById(R.id.rtbProductRating);
                            float s = z.getRating();
                            if (s > 0) {
                                JSONObject jo = new JSONObject();
                                jo.put("ratingTypeId", 1);
                                jo.put("rate", s);
                                myArray.put(jo);
                            }

                            z = (RatingBar) findViewById(R.id.rtb);
                            s = z.getRating();
                            if (s > 0) {
                                JSONObject jo1 = new JSONObject();
                                jo1.put("ratingTypeId", 2);
                                jo1.put("rate", s);
                                myArray.put(jo1);
                            }

                            z = (RatingBar) findViewById(R.id.ProductRating);
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
                        obj.put("trackRatings", myArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_ADD_RATING;

                    boolean requestSent = Requests.sendRequest(this, true, url, obj, new VolleyRequestListener<JSONObject>() {
                        @Override
                        public void onResult(JSONObject response) {
                            int responseCode = 0;
                            try {
                                responseCode = response.getInt("responseCode");

                                if (responseCode != 0) {
                                    Utils.showToastAtTop(mContext, getString(R.string
                                            .something_went_wrong));
                                    rate.setVisibility(View.GONE);
                                    return;
                                } else {
                                    stop2.setVisibility(View.GONE);
                                    Intent mainActivity = new Intent(getApplicationContext(),
                                            MainActivity.class);
                                    mainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    mainActivity.addFlags(Intent
                                            .FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                                    startActivity(mainActivity);
                                    Utils.showToastAtTop(mContext, getString(R.string
                                            .saved_raiting));
                                    rate.setVisibility(View.GONE);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Utils.showToastAtTop(mContext, getString(R.string
                                        .something_went_wrong));
                            }
                        }

                        @Override
                        public void onError(JSONObject object) {
                            Utils.showToastAtTop(mContext, getString(R.string
                                    .something_went_wrong));
                            rate.setVisibility(View.GONE);
                        }
                    }, null);

                    if (!requestSent) {
                        rate.setVisibility(View.GONE);
                    }
                }
                break;
            case R.id.skip_button:
                FrameLayout stop3 = (FrameLayout) findViewById(R.id.rate);
                stop3.setVisibility(View.GONE);
                break;

        }
    }

    @JavascriptInterface
    public String getRouteKmlFile() {
        return mTmpKmlFilePath;
    }

    @Override
    public void onLocationChanged(Location location) {
        double currLat = location.getLatitude();
        double currLng = location.getLongitude();

        if (mShowChangePointAlert) {
            boolean useRadius = false;
            boolean useTime = false;
            ChangePoint minPoint = new ChangePoint();
            minPoint.distance = 999999;
            minPoint.seconds = 60 * 9999;
            minPoint.showTime = false;

            for (int i = 0; i < mChangePoints.length; i++) {
                ChangePoint currPoint = mChangePoints[i];
                double nextPointLat = currPoint.lat;
                double nextPointLng = currPoint.lng;

                int distance = calculateDistance(currLat, currLng, nextPointLat, nextPointLng);
                if (distance <= mRadius1) {
                    useRadius = true;
                    if (distance < minPoint.distance) {
                        mChangePoints[i].distance = distance;
                        minPoint = mChangePoints[i];
                    }
                } else if (!useRadius) {
                    double secondsTillChangePoint = distance / location.getSpeed();
                    if (secondsTillChangePoint <= 60 * 5) {
                        useTime = true;
                        if (secondsTillChangePoint < minPoint.seconds) {
                            mChangePoints[i].distance = distance;
                            mChangePoints[i].seconds = (int) secondsTillChangePoint;
                            minPoint = mChangePoints[i];
                        }
                    }
                }
            }

            if (!useRadius && !useTime) {
                for (int i = 0; i < mChangePoints.length; i++) {
                    if (mChangePoints[i] != minPoint) {
                        mChangePoints[i].showTime = true;
                        mChangePoints[i].showRadius = true;
                    }
                }
            } else if (minPoint.showTime && minPoint.seconds > 60 * 3) {
                showChangePointAlert(false, 0, minPoint.transportId);
                minPoint.showTime = false;

                for (int i = 0; i < mChangePoints.length; i++) {
                    if (mChangePoints[i] != minPoint) {
                        mChangePoints[i].showTime = true;
                        mChangePoints[i].showRadius = true;
                    }
                }
            } else if (minPoint.showRadius && useRadius) {
                showChangePointAlert(true, mRadius1, minPoint.transportId);
                minPoint.showRadius = false;
            }
        }

        if (!mDestReached) {
            double distance = calculateDistance(currLat, currLng, mDestPoint[0], mDestPoint[1]);
            if (distance <= mDestRadius) {
                FrameLayout stop1 = (FrameLayout) findViewById(R.id.rate);
                stop1.setVisibility(View.VISIBLE);
                //showDestinationAlert();
                mDestReached = true;
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }

    private void initChangePoints() {
        try {
            SAXBuilder builder = new SAXBuilder();

            File tmpKmlFile = new File(mTmpKmlFilePath.substring(6, mTmpKmlFilePath.length()));
            Document doc = builder.build(tmpKmlFile);
            Element rootNode = doc.getRootElement();

            Element document = rootNode.getChild("Document");
            List list = document.getChildren();

            Element placemark = (Element) list.get(1);
            Element lineString = placemark.getChild("LineString");
            String coordinates = lineString.getChild("coordinates").getValue();
            String[] allCoords = coordinates.split(" ");
            String[] destCoords = allCoords[allCoords.length - 1].split(",");
            mDestPoint[0] = Double.valueOf(destCoords[1]);
            mDestPoint[1] = Double.valueOf(destCoords[0]);

            int skipCount = 2;
            int points = list.size() - skipCount;

            if (points > 0) {
                mChangePoints = new ChangePoint[points];
                for (int i = skipCount; i < list.size(); i++) {
                    mChangePoints[i - skipCount] = new ChangePoint();
                    placemark = (Element) list.get(i);
                    Element point = placemark.getChild("Point");
                    mChangePoints[i - skipCount].transportId =
                            Integer.valueOf(point.getAttributeValue("id"));
                    coordinates = point.getChild("coordinates").getValue();
                    String[] lngAndLat = coordinates.split(",");
                    mChangePoints[i - skipCount].lat = Double.valueOf(lngAndLat[1]);
                    mChangePoints[i - skipCount].lng = Double.valueOf(lngAndLat[0]);
                }

                mShowChangePointAlert = true;
            }
        } catch (IOException io) {
            io.printStackTrace();
        } catch (JDOMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. Uses Haversine method.
     *
     * @return The distance in meters
     */
    public static int calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // Radius of the earth
        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lng2 - lng1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        // Calculate distance and convert to meters
        double distance = R * c * 1000;
        return (int) distance;
    }

    private void showChangePointAlert(boolean isMeters, int ramainingValue, int id) {
        String icon;
        if (id * 1000 == Classificators.TRANSPORT_WALK) {
            icon = getString(R.string.icon_walking);
        } else if (id * 1000 == Classificators.TRANSPORT_BIKE) {
            icon = getString(R.string.icon_bicycle);
        } else if (id * 1000 == Classificators.TRANSPORT_TRAM) {
            icon = getString(R.string.icon_tram);
        } else if (id * 1000 == Classificators.TRANSPORT_BUS) {
            icon = getString(R.string.icon_bus);
        } else if (id * 1000 == Classificators.TRANSPORT_CAR_MOTORCYCLE) {
            icon = getString(R.string.icon_car);
        } else {
            icon = getString(R.string.icon_train);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout rootLayout = (LinearLayout) inflater.inflate(R.layout.dialog_poi, null);
        TextView leftText = (TextView) rootLayout.findViewById(R.id.textLeft);
        leftText.setTypeface(mTypeFace);
        leftText.setText(icon);
        TextView rightText = (TextView) rootLayout.findViewById(R.id.textRight);
        if (isMeters) {
            rightText.setText(getString(R.string.cp_1_after) + " " + ramainingValue + " " +
                    "" + getString(R.string.cp_2_change));
        } else {
            rightText.setText(getString(R.string.be_ready));
        }

        builder.setView(rootLayout)
                .setTitle(getString(R.string.movement_type_change))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNegativeButton(getString(R.string.dont_show_again),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mShowChangePointAlert = false;
                            }
                        });
        builder.create().show();
    }
}
