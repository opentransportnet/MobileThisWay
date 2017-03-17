package eu.opentransportnet.thisway.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.utils.Utils;

/**
 * @author Kristaps Krumins
 */
public class SearchRoutesActivity extends BaseActivity implements View.OnClickListener {
    public static final double UNKNOWN = 200;

    private static final int RC_PIN_START_LOC = 1100;
    private static final int RC_PIN_FINISH_LOC = 1101;

    private static double sStartLat = UNKNOWN;
    private static double sStartLng = UNKNOWN;
    private static double sDestLat = UNKNOWN;
    private static double sDestLng = UNKNOWN;
    private static int sDefaultRadius = 500;
    private static int sRadius = sDefaultRadius;

    private TextView radiusValue;
    private CheckBox mOnlyMyRoutes;
    private EditText mStartLocAddress;
    private EditText mFinishLocAddress;
    private boolean mStartEditedByUser = false;
    private boolean mFinishEditedByUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(R.layout.activity_search_routes);
        setToolbarTitle(R.string.title_activity_search_routes);
        initLoseFocusInContent();
        initToolbarBackBtn();
        Typeface tf = getTypeFace();

        TextView tf1 = (TextView) findViewById(R.id.tf_1);
        tf1.setTypeface(tf);

        Button myCoordStart = (Button) findViewById(R.id.my_coord_start);
        myCoordStart.setTypeface(tf);
        myCoordStart.setOnClickListener(this);

        Button pinLocStart = (Button) findViewById(R.id.pin_loc_start);
        pinLocStart.setTypeface(tf);
        pinLocStart.setOnClickListener(this);

        TextView tf2 = (TextView) findViewById(R.id.tf_2);
        tf2.setTypeface(tf);

        Button myCoordFinish = (Button) findViewById(R.id.my_coord_end);
        myCoordFinish.setTypeface(tf);
        myCoordFinish.setOnClickListener(this);

        Button pinLocEnd = (Button) findViewById(R.id.pin_loc_end);
        pinLocEnd.setTypeface(tf);
        pinLocEnd.setOnClickListener(this);

        radiusValue = (TextView) findViewById(R.id.radius_value);

        SeekBar radius = (SeekBar) findViewById(R.id.radius);
        radius.setMax(29);
        radius.setProgress(sRadius / 100);
        radiusValue.setText(sRadius + " m");
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                sRadius = progresValue * 100 + 100;
                radiusValue.setText(sRadius + " m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button search = (Button) findViewById(R.id.search);
        search.setOnClickListener(this);

        mOnlyMyRoutes = (CheckBox) findViewById(R.id.only_my_routes);

        mStartLocAddress = (EditText) findViewById(R.id.edit_start);
        mStartLocAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (getCurrentFocus() == mStartLocAddress) {
                    mStartEditedByUser = true;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mFinishLocAddress = (EditText) findViewById(R.id.edit_end);
        mFinishLocAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (getCurrentFocus() == mFinishLocAddress) {
                    mFinishEditedByUser = true;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

    }

    @Override
    protected void onDestroy() {
        sStartLat = UNKNOWN;
        sStartLng = UNKNOWN;
        sDestLat = UNKNOWN;
        sDestLng = UNKNOWN;
        sRadius = sDefaultRadius;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PIN_START_LOC) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    sStartLat = extras.getDouble("latitude");
                    sStartLng = extras.getDouble("longitude");
                    String locAddress = Utils.getLocAddress(this, sStartLat, sStartLng);
                    mStartLocAddress.setText(locAddress);
                }
            }
        } else if (requestCode == RC_PIN_FINISH_LOC) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    sDestLat = extras.getDouble("latitude");
                    sDestLng = extras.getDouble("longitude");
                    String locAddress = Utils.getLocAddress(this, sDestLat, sDestLng);
                    mFinishLocAddress.setText(locAddress);
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                finish();
                break;
            case R.id.pin_loc_start:
                Intent pinStartLoc = new Intent(this, PinLocationActivity.class);
                pinStartLoc.putExtra("pinStart", true);
                startActivityForResult(pinStartLoc, RC_PIN_START_LOC);
                break;
            case R.id.pin_loc_end:
                Intent pinFinishLoc = new Intent(this, PinLocationActivity.class);
                pinFinishLoc.putExtra("pinStart", false);
                startActivityForResult(pinFinishLoc, RC_PIN_FINISH_LOC);
                break;
            case R.id.search:
                String startAddress = String.valueOf(mStartLocAddress.getText());
                String finishAddress = String.valueOf(mFinishLocAddress.getText());
                if (!startAddress.isEmpty() || !finishAddress.isEmpty()) {
                    Intent routeList = new Intent(this, RouteListActivity.class);
                    routeList.putExtra("onlyMyRoutes", mOnlyMyRoutes.isChecked());

                    String delims = "[,]+";

                    if (startAddress.isEmpty()) {
                        routeList.putExtra("startPoint", getString(R.string.any_address));
                    } else {
                        if (mStartEditedByUser) {
                            //check if string contains coordinates separated by comma or
                            // space/spaces, else geocode like an address
                            String[] coordinates = startAddress.split(delims);
                            if (coordinates.length == 2) {
                                try {
                                    double lat = Double.valueOf(coordinates[0]);
                                    double lng = Double.valueOf(coordinates[1]);
                                    sStartLat = lat;
                                    sStartLng = lng;
                                } catch (NumberFormatException e) {
                                    Utils.showToastAtTop(this,
                                            getString(R.string.invalid_start_coord));
                                    break;
                                }
                            } else {
                                double[] coord = Utils.geocode(this, startAddress);
                                if (coord[0] == UNKNOWN || coord[1] == UNKNOWN) {
                                    Utils.showToastAtTop(this,
                                            getString(R.string.invalid_start_address));
                                    break;
                                } else {
                                    sStartLat = coord[0];
                                    sStartLng = coord[1];
                                }
                            }
                        }
                        routeList.putExtra("startPoint", startAddress);
                    }

                    if (finishAddress.isEmpty()) {
                        routeList.putExtra("destPoint", getString(R.string.any_address));
                    } else {
                        if (mFinishEditedByUser) {
                            //check if string contains coordinates separated by comma or
                            // space/spaces, else geocode like an address
                            String[] coordinates = finishAddress.split(delims);
                            if (coordinates.length == 2) {
                                try {
                                    double lat = Double.valueOf(coordinates[0]);
                                    double lng = Double.valueOf(coordinates[1]);
                                    sDestLat = lat;
                                    sDestLng = lng;
                                } catch (NumberFormatException e) {
                                    Utils.showToastAtTop(this,
                                            getString(R.string.invalid_desti_coord));
                                    break;
                                }
                            } else {
                                double[] coord = Utils.geocode(this, finishAddress);
                                if (coord[0] == UNKNOWN || coord[1] == UNKNOWN) {
                                    Utils.showToastAtTop(this,
                                            getString(R.string.invalid_desti_address));
                                    break;
                                } else {
                                    sDestLat = coord[0];
                                    sDestLng = coord[1];
                                }
                            }
                        }
                        routeList.putExtra("destPoint", finishAddress);
                    }

                    routeList.putExtra("filterByPoints", true);
                    startActivity(routeList);
                } else {
                    if (mOnlyMyRoutes.isChecked()) {
                        Intent searchRoutes2 = new Intent(this, RouteListActivity.class);
                        searchRoutes2.putExtra("onlyMyRoutes", mOnlyMyRoutes.isChecked());
                        searchRoutes2.putExtra("startPoint", getString(R.string.any_address));
                        searchRoutes2.putExtra("destPoint", getString(R.string.any_address));
                        searchRoutes2.putExtra("filterByPoints", false);
                        startActivity(searchRoutes2);
                    } else {
                        Utils.showToastAtTop(this, getString(R.string.select_start_or_destination));
                    }
                }
                break;
            case R.id.my_coord_start:
                sStartLat = MainActivity.getRouteRecorder().getLatitude();
                sStartLng = MainActivity.getRouteRecorder().getLongitude();
                String startLocAddress = Utils.getLocAddress(this, sStartLat, sStartLng);
                mStartLocAddress.setText(startLocAddress);
                break;
            case R.id.my_coord_end:
                sDestLat = MainActivity.getRouteRecorder().getLatitude();
                sDestLng = MainActivity.getRouteRecorder().getLongitude();
                String finishLocAddress = Utils.getLocAddress(this, sDestLat, sDestLng);
                mFinishLocAddress.setText(finishLocAddress);
                break;
        }
    }

    public static double[] getStartAndDestPoints() {
        return new double[]{sStartLat, sStartLng, sDestLat, sDestLng};
    }

    public static int getRadius() {
        return sRadius;
    }
}
