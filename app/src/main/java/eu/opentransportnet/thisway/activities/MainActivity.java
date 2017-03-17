package eu.opentransportnet.thisway.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.opentransportnet.thisway.BuildConfig;
import eu.opentransportnet.thisway.adapters.CustomList;
import eu.opentransportnet.thisway.adapters.LayerListAdapter;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.listeners.SlideMenuClickListener;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.models.User;
import eu.opentransportnet.thisway.models.WmsLayer;
import eu.opentransportnet.thisway.network.NetworkReceiver;
import eu.opentransportnet.thisway.network.RequestQueueSingleton;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.network.UploadTask;
import eu.opentransportnet.thisway.utils.Classificators;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.SessionManager;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.RouteAlert;
import com.library.routerecorder.RouteRecorder;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, RouteAlert
        .RouteAlertListener {
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_NAME = "name";
    private static final int RC_FINISH_ROUTE_ACTIVITY = 1100;
    private static final int RC_FINISH_DO_NOT_CONTINUE = 1101;
    private static final String LOG_TAG = "MainActivity";

    public static boolean sCanrecord = true;
    private static Context sContext;
    private static Context sAppCtx;
    private static RouteRecorder sRouteRec;
    private static boolean sLanguageChanged = false;

    private DrawerLayout mDrawer;
    private SlidingUpPanelLayout mSelectLayersPanel;
    private NetworkReceiver mNetworkReceiver;
    private RelativeLayout mSearchLayout;
    private RelativeLayout mAddLayout;
    private RelativeLayout mBicycleLayout;
    private RelativeLayout mStopLayout;
    private ListView mDrawerList;
    private int mMeansOfTransport = Classificators.TRANSPORT_WALK;
    private double[] mChangePoint = new double[2];
    private SessionManager mSessionManager;
    private LayerListAdapter mLayerListAdapter = null;

    public final String TAG_DELETE_USER = "delete user request";
    String poi = Requests.PATH_GET_POI_CATEGORY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        sContext = this;
        sAppCtx = getApplicationContext();
        mSessionManager = new SessionManager(this);
        setContentView(eu.opentransportnet.thisway.R.layout.activity_home);
        Utils.enableLoseFocusInContent(this);
        initRouteRecorder();
        setToolbarTitle(eu.opentransportnet.thisway.R.string.title_activity_home);
        initToolbarBackBtn();
        Typeface tf = getTypeFace();
        initIcons(tf);
        initMainButtonLayouts();
        mNetworkReceiver = new NetworkReceiver(this);

        Button drawer = (Button) findViewById(eu.opentransportnet.thisway.R.id.back_button);
        drawer.setText(eu.opentransportnet.thisway.R.string.icon_menu);
        drawer.setTextSize(45);
        drawer.setOnClickListener(this);
        initDrawer();

        mSelectLayersPanel = (SlidingUpPanelLayout) findViewById(eu.opentransportnet.thisway.R.id.sliding_layout);
        mSelectLayersPanel.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectLayersPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        ArrayList<WmsLayer> layerList = new ArrayList<>();

        mLayerListAdapter = new LayerListAdapter(this, eu.opentransportnet.thisway.R.layout.listview_layers, layerList);
        ListView listView = (ListView) findViewById(eu.opentransportnet.thisway.R.id.layer_list);
        listView.setAdapter(mLayerListAdapter);
        // Download and add layers to adapter
        addLayers();

        TextView poi_image = (TextView) findViewById(eu.opentransportnet.thisway.R.id.poi_image);
        poi_image.setTypeface(tf);
        TextView route_image = (TextView) findViewById(eu.opentransportnet.thisway.R.id.route_image);
        route_image.setTypeface(tf);
        TextView x_image = (TextView) findViewById(eu.opentransportnet.thisway.R.id.close_choise);
        x_image.setTypeface(tf);
        // Hide bicycle and stop button
        mBicycleLayout.setVisibility(View.GONE);
        mStopLayout.setVisibility(View.GONE);

        // Set on click listeners for FAB's
        findViewById(eu.opentransportnet.thisway.R.id.search).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.add).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.select_bike).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.stop).setOnClickListener(this);

        createFolders();

        TextView version = (TextView) findViewById(eu.opentransportnet.thisway.R.id.version);
        String versionName = BuildConfig.VERSION_NAME;
        try {
            if (getString(eu.opentransportnet.thisway.R.string.svn_version).equals("null")) {
                version.setText("v" + versionName);
            } else {
                version.setText("v" + versionName + " (" + getString(eu.opentransportnet.thisway.R.string.svn_version) + ")");
            }
        } catch (Exception e) {
        }

        findViewById(eu.opentransportnet.thisway.R.id.idwalk).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.idbike).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.idmetro).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.idbus).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.idcar).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.idtrain).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.poi).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.close_choise).setOnClickListener(this);
        findViewById(eu.opentransportnet.thisway.R.id.route).setOnClickListener(this);

        FloatingActionButton fabwalk = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idwalk);
        fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));

        UploadTask.getInstance(this).startScheduledUpload();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(sLanguageChanged) {
            setToolbarTitle(eu.opentransportnet.thisway.R.string.title_activity_home);
            setDrawerAdapter();
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.transport_title)).setText(eu.opentransportnet.thisway.R.string.movement);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.current_means_of_transport)).setText(eu.opentransportnet.thisway.R.string.curmovement);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.MOT_description)).setHint(eu.opentransportnet.thisway.R.string.description);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.save_movment_type)).setText(eu.opentransportnet.thisway.R.string.save);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.cancel_movement_type)).setText(eu.opentransportnet.thisway.R.string.cancel);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.selectLayers)).setText(eu.opentransportnet.thisway.R.string.select_layers);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.walk_movement_type)).setText(eu.opentransportnet.thisway.R.string.walk);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.bike_movement_type)).setText(eu.opentransportnet.thisway.R.string.bike);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.metro_movement_type)).setText(eu.opentransportnet.thisway.R.string.underground);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.bus_movement_type)).setText(eu.opentransportnet.thisway.R.string.bus);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.car_movement_type)).setText(eu.opentransportnet.thisway.R.string.car);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.train_movement_type)).setText(eu.opentransportnet.thisway.R.string.train);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.text_poi)).setText(eu.opentransportnet.thisway.R.string.point_of_interest);
            ((TextView) findViewById(eu.opentransportnet.thisway.R.id.text_route)).setText(eu.opentransportnet.thisway.R.string.route);

            addLayers();
            sLanguageChanged = false;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(mNetworkReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(sContext).cancelAllPendingRequests(TAG_DELETE_USER);
        sRouteRec.stopRecording(false);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_FINISH_ROUTE_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                setRouteButtons(false);
            } else {
                sRouteRec.getMainLocation().removeFinishPoint();
            }
        } else if (requestCode == RC_FINISH_DO_NOT_CONTINUE) {
            if (resultCode != Activity.RESULT_OK) {
                // No activity from user for long time and track too short
                // Delete current track files
                String fileName = sRouteRec.getCurrRecordedRoute();
                File file = new File(getFilesDir(),
                        "/" + Const.APP_ROUTES_CSV_PATH + "/" + fileName + ".csv");
                file.delete();

                file = new File(getFilesDir(),
                        "/" + Const.APP_ROUTES_CSV_PATH + "/" + fileName + ".kml");
                file.delete();
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.thisway.R.id.search:
                Intent intent = new Intent(this, SearchRoutesActivity.class);
                startActivity(intent);
                break;
            case eu.opentransportnet.thisway.R.id.close_choise:
                FrameLayout action = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.choice);
                action.setVisibility(View.GONE);
                break;
            case eu.opentransportnet.thisway.R.id.add:
                action = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.choice);
                action.setVisibility(View.VISIBLE);
                break;
            case eu.opentransportnet.thisway.R.id.stop:
                if (sRouteRec.isRouteFileCreated()) {
                    sRouteRec.getMainLocation().setFinishPoint();
                    Intent finishRoute = new Intent(this, FinishRouteActivity.class);
                    finishRoute.putExtra("latitude", sRouteRec.getLatitude());
                    finishRoute.putExtra("longitude", sRouteRec.getLongitude());
                    startActivityForResult(finishRoute, RC_FINISH_ROUTE_ACTIVITY);
                } else {
                    RouteAlert routeAlert = new RouteAlert(this, sRouteRec);
                    routeAlert.showRouteTooShortDialog();
                }
                break;
            case eu.opentransportnet.thisway.R.id.idwalk:
                mMeansOfTransport = Classificators.TRANSPORT_WALK;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.idbike:
                mMeansOfTransport = Classificators.TRANSPORT_BIKE;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.idmetro:
                mMeansOfTransport = Classificators.TRANSPORT_METRO;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.idbus:
                mMeansOfTransport = Classificators.TRANSPORT_BUS;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.idcar:
                mMeansOfTransport = Classificators.TRANSPORT_CAR_MOTORCYCLE;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.idtrain:
                mMeansOfTransport = Classificators.TRANSPORT_TRAIN;
                changeButtons(mMeansOfTransport);
                change();
                break;
            case eu.opentransportnet.thisway.R.id.select_bike:
                FrameLayout ratef = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.movement_type);
                ratef.setVisibility(View.VISIBLE);
                FrameLayout type = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.type);
                type.setVisibility(View.VISIBLE);
                FrameLayout change = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.change);
                change.setVisibility(View.GONE);

                sRouteRec.getMainLocation().setMovementChangePoint();
                mChangePoint[0] = sRouteRec.getLatitude();
                mChangePoint[1] = sRouteRec.getLongitude();
                break;
            case eu.opentransportnet.thisway.R.id.poi:
                openCreatePoiActivity();
                break;
            case eu.opentransportnet.thisway.R.id.route:
                action = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.choice);
                action.setVisibility(View.GONE);

                if (sRouteRec.canStartRecNewRoute()) {
                    ratef = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.movement_type);
                    ratef.setVisibility(View.VISIBLE);
                    type = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.type);
                    type.setVisibility(View.VISIBLE);
                    change = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.change);
                    change.setVisibility(View.GONE);

                    sRouteRec.getMainLocation().setMovementChangePoint();
                    mChangePoint[0] = sRouteRec.getLatitude();
                    mChangePoint[1] = sRouteRec.getLongitude();
                }

                break;
            case eu.opentransportnet.thisway.R.id.back_button:
                mDrawer.openDrawer(Gravity.START);
                break;
        }
    }

    /**
     * Open Create POI Activity, to do so we first need to get poi categorys from server
     */
    private void openCreatePoiActivity() {
        FrameLayout spinner;
        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
        spinner.setVisibility(View.VISIBLE);
        Requests.getPOICategory(this, "http://" + Utils.getHostname() + Utils.getUrlPathStart() + poi, new VolleyRequestListener<String>() {
            @Override
            public void onResult(String response) {
                if(!response.equals(null) && !response.isEmpty() && !response.equals("null")){
                    JSONArray jArray=null;
                    try {
                        jArray = new JSONArray(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String language= Locale.getDefault().getLanguage();

                    final List<String> name = new ArrayList<String>();
                    List<Integer> id = new ArrayList<Integer>();

                    for(int i=0; i<jArray.length(); i++)
                    {
                        JSONObject json_data;
                        try {
                            json_data = jArray.getJSONObject(i);
                            if(language.equals("fr")) {
                                String checkForNull = json_data.getString("nameFr");
                                if (!checkForNull.equals(null) && !checkForNull.isEmpty() && !checkForNull.equals("null")) {
                                    name.add(json_data.getString("nameFr"));
                                    id.add(json_data.getInt("categoryId"));
                                }
                            }
                            else{
                                String checkForNull = json_data.getString("name");
                                if (!checkForNull.equals(null) && !checkForNull.isEmpty() && !checkForNull.equals("null")) {
                                    name.add(json_data.getString("name"));
                                    id.add(json_data.getInt("categoryId"));
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    FrameLayout action = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.choice);
                    action.setVisibility(View.GONE);
                    Intent intent = new Intent(sContext, CreatePoiActivity.class);

                    intent.putExtra(EXTRA_ID, (ArrayList<Integer>)id);
                    intent.putExtra(EXTRA_NAME, (ArrayList<String>)name);
                    startActivity(intent);

                } else {
                    Utils.showToastAtTop(sContext, getString(eu.opentransportnet.thisway.R.string.server_error));
                }
                FrameLayout spinner;
                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                spinner.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                FrameLayout spinner;
                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                spinner.setVisibility(View.GONE);
                Utils.showToastAtTop(sContext, getString(eu.opentransportnet.thisway.R.string.server_error));
            }
        }, "");
    }

    @Override
    public void onDialogPositiveClick(int id) {
    }

    @Override
    public void onDialogNegativeClick(int id) {
        if (id == RouteAlert.ROUTE_TOO_SHORT_ALERT) {
            // Stop recording
            sRouteRec.stopRecording(false);
            sCanrecord = true;
            setRouteButtons(false);
        } else if (id == RouteAlert.ROUTE_NO_MOVEMENT) {
            if (sRouteRec.isRouteFileCreated()) {
                Intent finishRoute = new Intent(this, FinishRouteActivity.class);
                finishRoute.putExtra("latitude", sRouteRec.getLatitude());
                finishRoute.putExtra("longitude", sRouteRec.getLongitude());
                startActivityForResult(finishRoute, RC_FINISH_DO_NOT_CONTINUE);
                setRouteButtons(false);
            } else {
                setRouteButtons(false);
            }
        }
    }

    //MainActivity sContext
    public static Context getContext() {
        return sContext;
    }

    public static RouteRecorder getRouteRecorder() {
        return sRouteRec;
    }

    /**
     * Creates folders in internal storage
     */
    private void createFolders() {
        File filesDir = getFilesDir();
        File newFolder = new File(filesDir + "/routes");
        newFolder.mkdir();
        newFolder = new File(filesDir + "/" + Const.APP_ROUTES_JSON_PATH);
        newFolder.mkdir();
        newFolder = new File(filesDir + "/" + Const.APP_ROUTES_CSV_PATH);
        newFolder.mkdir();
    }

    /**
     *  Hide transport movement type choice for user
     */
    public void hideType(View v) {
        FrameLayout ratef = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.movement_type);
        ratef.setVisibility(View.GONE);

        sRouteRec.getMainLocation().removeMovementChangePoint();
        // reset text
        ((EditText) findViewById(eu.opentransportnet.thisway.R.id.MOT_description)).setText("");
        findViewById(android.R.id.content).performClick();
    }


    public void changeMovementType(View v) {
        EditText commentView = (EditText) findViewById(eu.opentransportnet.thisway.R.id.MOT_description);
        Editable commentE = commentView.getText();
        String comment = "";
        if (commentE != null) {
            comment = commentE.toString();
        }

        byte[] encodedCommentB = Base64.encode(comment.getBytes(), Base64.NO_WRAP);
        String encodedComment = new String(encodedCommentB);

        if (sCanrecord) {
            if (sRouteRec.startRecNewRoute(mMeansOfTransport)) {
                sRouteRec.setStartPointInfo(mMeansOfTransport, comment);
                double[] startPoint = sRouteRec.getStartPointCoord();
                sRouteRec.loadUrl("javascript:addChangePoint(" + startPoint[0] + ","
                        + startPoint[1] + "," + mMeansOfTransport / 1000 + ",true,'"
                        + encodedComment + "')");
                setRouteButtons(true);
                sCanrecord = false;
                setAndCloseMoveType();
            }
        } else {
            if (sRouteRec.isRouteFileCreated()) {
                if (!sRouteRec.saveMovementType(mMeansOfTransport, comment)) {
                    final String finalComment = encodedComment;
                    new AlertDialog.Builder(this)
                            .setTitle(getString(eu.opentransportnet.thisway.R.string.overwrite_movement_type))
                            .setMessage(getString(eu.opentransportnet.thisway.R.string.overwrite_movement_type_message))
                            .setPositiveButton(getString(eu.opentransportnet.thisway.R.string.yes),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            sRouteRec.saveMovementType(mMeansOfTransport, finalComment, true);
                                            sRouteRec.loadUrl("javascript:addChangePoint(" + mChangePoint[0] + ","
                                                    + mChangePoint[1] + "," + mMeansOfTransport / 1000 + ",false,'" + finalComment + "')");
                                            setAndCloseMoveType();
                                        }
                                    })
                            .setNegativeButton(getString(eu.opentransportnet.thisway.R.string.no), new DialogInterface.OnClickListener
                                    () {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    sRouteRec.loadUrl("javascript:addChangePoint(" + mChangePoint[0] + ","
                            + mChangePoint[1] + "," + mMeansOfTransport / 1000 + ",false,'"
                            + encodedComment + "')");
                    setAndCloseMoveType();
                }
            } else {
                sRouteRec.setStartPointInfo(mMeansOfTransport, comment);
                double[] startPoint = sRouteRec.getStartPointCoord();
                sRouteRec.loadUrl("javascript:addChangePoint(" + startPoint[0] + ","
                        + startPoint[1] + "," + mMeansOfTransport / 1000 + ",true,'"
                        + encodedComment + "')");
                setAndCloseMoveType();
            }
        }

        // reset text
        ((EditText) findViewById(eu.opentransportnet.thisway.R.id.MOT_description)).setText("");
        findViewById(android.R.id.content).performClick();
    }

    /**
     * Change the means of transport user is using
     */
    private void setAndCloseMoveType() {
        FrameLayout type = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.change);
        type.setVisibility(View.GONE);
        FrameLayout ratef = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.movement_type);
        ratef.setVisibility(View.GONE);

        sChangeButtons(mMeansOfTransport);
    }

    /**
     * after transport choice open next view, where user can add description
     */
    public void change() {
        FrameLayout ratef = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.movement_type);
        ratef.setVisibility(View.VISIBLE);
        FrameLayout type = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.type);
        type.setVisibility(View.GONE);
        FrameLayout change = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.change);
        change.setVisibility(View.VISIBLE);
    }

    private void setRouteButtons(boolean recording) {
        if (recording) {
            mSearchLayout.setVisibility(View.GONE);
            mAddLayout.setVisibility(View.GONE);
            mBicycleLayout.setVisibility(View.VISIBLE);
            mStopLayout.setVisibility(View.VISIBLE);
        } else {
            mSearchLayout.setVisibility(View.VISIBLE);
            mAddLayout.setVisibility(View.VISIBLE);
            mBicycleLayout.setVisibility(View.GONE);
            mStopLayout.setVisibility(View.GONE);
        }
    }

    void changeButtons(int buttonNumber) {
        TextView bike_icon = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bike_icon);
        bike_icon.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

        if (buttonNumber == Classificators.TRANSPORT_WALK) {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_walking);
        } else if (buttonNumber == Classificators.TRANSPORT_BIKE) {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_bicycle);
        } else if (buttonNumber == Classificators.TRANSPORT_METRO) {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_underground);
        } else if (buttonNumber == Classificators.TRANSPORT_BUS) {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_bus);
        } else if (buttonNumber == Classificators.TRANSPORT_CAR_MOTORCYCLE) {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_car);
        } else {
            bike_icon.setText(eu.opentransportnet.thisway.R.string.icon_train);
        }
    }

    /**
     * Change color of movement type button
     * @param buttonnumber button pressed
     */
    void sChangeButtons(int buttonnumber) {

        FloatingActionButton fabwalk = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idwalk);
        FloatingActionButton fabbike = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idbike);
        FloatingActionButton fabmetro = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idmetro);
        FloatingActionButton fabbus = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idbus);
        FloatingActionButton fabcar = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idcar);
        FloatingActionButton fabtrain = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.idtrain);

        TextView walk = (TextView) findViewById(eu.opentransportnet.thisway.R.id.walk);
        TextView bike = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bike);
        TextView metro = (TextView) findViewById(eu.opentransportnet.thisway.R.id.tram);
        TextView bus = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bus);
        TextView car = (TextView) findViewById(eu.opentransportnet.thisway.R.id.car);
        TextView train = (TextView) findViewById(eu.opentransportnet.thisway.R.id.metro);
        TextView button = (TextView) findViewById(eu.opentransportnet.thisway.R.id.select_bike_image);

        if (buttonnumber == Classificators.TRANSPORT_WALK) {
            fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            fabbike.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabmetro.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabcar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabtrain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));

            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_walking);

        } else if (buttonnumber == Classificators.TRANSPORT_BIKE) {
            fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabmetro.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabcar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabtrain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbike.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_bicycle);
        } else if (buttonnumber == Classificators.TRANSPORT_METRO) {
            fabmetro.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            fabbike.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabcar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabtrain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_underground);
        } else if (buttonnumber == Classificators.TRANSPORT_BUS) {
            fabbus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            fabbike.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabmetro.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabcar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabtrain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_bus);

        } else if (buttonnumber == Classificators.TRANSPORT_CAR_MOTORCYCLE) {
            fabcar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            fabbike.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabmetro.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbus.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabwalk.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabtrain.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_car);
        } else {
            fabtrain.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.color_register)));
            fabbike.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bike.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabmetro.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            metro.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabbus.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            bus.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabcar.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            car.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            fabwalk.setBackgroundTintList(
                    ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.type_button)));
            walk.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.text_secondary));
            train.setTextColor(getResources().getColor(eu.opentransportnet.thisway.R.color.white));

            button.setText(eu.opentransportnet.thisway.R.string.icon_train);

        }
    }

    public static Context getAppCtx() {
        return sAppCtx;
    }

    private void setDrawerAdapter() {
        String[] drawerTitles = getResources().getStringArray(eu.opentransportnet.thisway.R.array.drawer_titles_array);
        String[] drawerImages = getResources().getStringArray(eu.opentransportnet.thisway.R.array.drawer_img_array);

        CustomList adapter = new CustomList(
                MainActivity.this,
                drawerTitles,
                drawerImages);

        mDrawerList.setAdapter(adapter);
    }

    private void initRouteRecorder() {
        sRouteRec = (RouteRecorder) getFragmentManager().findFragmentById(eu.opentransportnet.thisway.R.id.route_recorder);
        sRouteRec.setDefaultLocation(Const.DEFAULT_LATITUDE, Const.DEFAULT_LONGITUDE);
        sRouteRec.setTracking(true);
        sRouteRec.setRouteFilePath(Const.APP_ROUTES_CSV_PATH);
        sRouteRec.setRouteAlert(new RouteAlert(this, sRouteRec));
        sRouteRec.addJavascriptInterface(this, "MainActivity");
        sRouteRec.loadWebView();
    }

    private void initIcons(Typeface tf) {
        TextView addImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.add_image);
        addImage.setTypeface(tf);
        TextView searchImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.search_image);
        searchImage.setTypeface(tf);
        TextView bicycleImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.select_bike_image);
        bicycleImage.setTypeface(tf);
        TextView stopImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.stop_image);
        stopImage.setTypeface(tf);
        TextView exit = (TextView) findViewById(eu.opentransportnet.thisway.R.id.hide_movement_type_layout);
        exit.setTypeface(tf);
        TextView walk = (TextView) findViewById(eu.opentransportnet.thisway.R.id.walk);
        walk.setTypeface(tf);
        TextView bike = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bike);
        bike.setTypeface(tf);
        TextView tram = (TextView) findViewById(eu.opentransportnet.thisway.R.id.tram);
        tram.setTypeface(tf);
        TextView bus = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bus);
        bus.setTypeface(tf);
        TextView car = (TextView) findViewById(eu.opentransportnet.thisway.R.id.car);
        car.setTypeface(tf);
        TextView metro = (TextView) findViewById(eu.opentransportnet.thisway.R.id.metro);
        metro.setTypeface(tf);
        TextView tf1 = (TextView) findViewById(eu.opentransportnet.thisway.R.id.hide_comments_movement_type_layout);
        tf1.setTypeface(tf);
        TextView tf2 = (TextView) findViewById(eu.opentransportnet.thisway.R.id.bike_icon);
        tf2.setTypeface(tf);
        TextView tf3 = (TextView) findViewById(eu.opentransportnet.thisway.R.id.start_route);
        tf3.setTypeface(tf);
        TextView tf4 = (TextView) findViewById(eu.opentransportnet.thisway.R.id.cancel_route);
        tf4.setTypeface(tf);
    }

    private void initMainButtonLayouts() {
        mSearchLayout = (RelativeLayout) findViewById(eu.opentransportnet.thisway.R.id.search_layout);
        mAddLayout = (RelativeLayout) findViewById(eu.opentransportnet.thisway.R.id.add_layout);
        mBicycleLayout = (RelativeLayout) findViewById(eu.opentransportnet.thisway.R.id.bicycle_layout);
        mStopLayout = (RelativeLayout) findViewById(eu.opentransportnet.thisway.R.id.stop_layout);
    }

    public void setPhotoInDrawer(final Bitmap photo) {
        // Sets profile photo in drawer
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView profilePhoto = (ImageView) findViewById(eu.opentransportnet.thisway.R.id.profile_photo);
                profilePhoto.setImageBitmap(photo);
            }
        });
    }

    private void initDrawer() {
        mDrawer = (DrawerLayout) findViewById(eu.opentransportnet.thisway.R.id.drawer);
        mDrawerList = (ListView) findViewById(eu.opentransportnet.thisway.R.id.drawerlist);
        setDrawerAdapter();
        mDrawerList.setOnItemClickListener(new SlideMenuClickListener(mDrawer, this));

        User user = mSessionManager.getUser();
        // Sets name in drawer
        String fullName = user.getFirstName() + " " + user.getLastName();
        if (fullName == null || fullName == "") {
            fullName = "[no name provided]";
        }
        TextView displayName = (TextView) findViewById(eu.opentransportnet.thisway.R.id.display_name);
        displayName.setText(fullName);

        Bitmap photo = user.getRoundedPhoto();
        setPhotoInDrawer(photo);

        if (!user.hasPhoto()) {
            user.downloadPhotoAndPutInDrawer(this);
        }
    }

    @JavascriptInterface
    public void poiClicked(int poiId) {
        if (poiId > 1) {
            showPOI(poiId, false);
        }
    }

    @JavascriptInterface
    public void publicPoiClicked(int poiId) {
        if (poiId > 1) {
            showPOI(poiId, true);
        }
    }

    private void addLayers() {
        mLayerListAdapter.clear();

        mLayerListAdapter.add(new WmsLayer("pois_issy", getContext().getString(eu.opentransportnet.thisway.R.string.layer_poi_name), "http://"+Utils.getHostname() + Const.WMS_PATH_POI_ISSY, false));
        mLayerListAdapter.add(new WmsLayer("routes_issy", getContext().getString(eu.opentransportnet.thisway.R.string.layer_routes_name), "http://"+Utils.getHostname() + Const.WMS_PATH_ROUTES_ISSY, false));

        mLayerListAdapter.notifyDataSetChanged();
        findViewById(eu.opentransportnet.thisway.R.id.layers_loading_panel).setVisibility(View.VISIBLE);

        final String url = "http://" + Utils.getHostname() + Const.WMS_PATH_ISSY_POI;
        Requests.getWmsCapabilities(this, url, new
                        VolleyRequestListener<String>() {
                            @Override
                            public void onResult(String response) {
                                parseWmsCapabilities(response, url);
                                findViewById(eu.opentransportnet.thisway.R.id.layers_loading_panel).setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(String object) {
                                Utils.logD(LOG_TAG, "WMS capabilities download error");
                                findViewById(eu.opentransportnet.thisway.R.id.layers_loading_panel).setVisibility(View.GONE);
                            }
                        },
                "downloadWms");
    }

    private boolean parseWmsCapabilities(String xml, String wmsUrl) {
        try {
            Document doc = Utils.loadXMLFromString(xml);
            doc.getDocumentElement().normalize();

            NodeList layers = doc.getElementsByTagName("Layer");

            // skip first element
            for (int i = 1; i < layers.getLength(); i++) {
                Node node = layers.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String name = element.getElementsByTagName("Name").item(0).getTextContent();
                    String title = element.getElementsByTagName("Title").item(0).getTextContent();
                    WmsLayer layer = new WmsLayer(name, title, wmsUrl, false);
                    mLayerListAdapter.add(layer);
                }
            }

            mLayerListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Delete user content from server if user chooses so
     */
    public void deleteUser() {
        FrameLayout spinner;
        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
        spinner.setVisibility(View.VISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(sContext);
        builder.setTitle(sContext.getString(eu.opentransportnet.thisway.R.string.delete_user_title))
                .setMessage(sContext.getString(eu.opentransportnet.thisway.R.string.delete_user_content))
                .setPositiveButton(sContext.getString(eu.opentransportnet.thisway.R.string.yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                JSONObject objs = new JSONObject();
                                try {
                                    objs.put("appId", Const.APPLICATION_ID);

                                    objs.put("userId", Utils.getHashedUserEmail(sContext));

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                Requests.sendRequest(sContext, "http://" + Utils.getHostname() + Utils.getUrlPathStart() + Requests
                                                .PATH_DELETE_USER,
                                        objs, new VolleyRequestListener<JSONObject>() {
                                            @Override
                                            public void onResult(JSONObject mUseritem) {
                                                if (mUseritem != null) {
                                                    String a = "2";
                                                    try {
                                                        a = mUseritem.getString("responseCode");
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }

                                                    String check = "0";
                                                    if (check.equals(a)) {
                                                        FrameLayout spinner;
                                                        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                                        spinner.setVisibility(View.GONE);

                                                        Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.thisway.R.string.delete_user));
                                                        Utils.deleteAllLocalFiles(sContext);
                                                        new SessionManager(sContext).forceLogoutUser();
                                                    } else {
                                                        FrameLayout spinner;
                                                        spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                                        spinner.setVisibility(View.GONE);
                                                        Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.thisway.R.string.server_error));
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onError(JSONObject error) {
                                                FrameLayout spinner;
                                                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                                spinner.setVisibility(View.GONE);
                                                Utils.showToastAtTop(sContext, sContext.getString(eu.opentransportnet.thisway.R.string.server_error));
                                            }
                                        }, TAG_DELETE_USER);
                            }
                        })
                .setNegativeButton(sContext.getString(eu.opentransportnet.thisway.R.string.no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                FrameLayout spinner;
                                spinner = (FrameLayout) findViewById(eu.opentransportnet.thisway.R.id.progresss);
                                spinner.setVisibility(View.GONE);
                            }
                        });
        builder.create().show();
    }

    public void showPOI(final int id, final boolean is_public) {
        runOnUiThread(new Runnable() {
            public void run() {
                Intent intent = new Intent(sContext, ShowPOIActivity.class);
                intent.putExtra("poi_id", id);
                intent.putExtra("is_public", is_public);
                startActivity(intent);
            }
        });
    }

    public static void languageChanged(){
        sLanguageChanged = true;
    }
}