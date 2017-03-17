package eu.opentransportnet.thisway.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import eu.opentransportnet.thisway.models.User;
import eu.opentransportnet.thisway.network.Requests;
import eu.opentransportnet.thisway.network.UploadTask;
import eu.opentransportnet.thisway.utils.Classificators;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.SessionManager;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.SaveSensorData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Create POI activity
 *
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class CreatePoiActivity extends AppCompatActivity implements View.OnClickListener {

    public static final double UNKNOWN = 200;
    private static final int RC_PIN_LOCATION = 1100;
    private static final Object REPORT_PREFIX = "Report_";

    private static double sStartLat = UNKNOWN;
    private static double sStartLng = UNKNOWN;
    private final int mAppId = Const.APPLICATION_ID;

    private int mTransport_type = Classificators.TRANSPORT_CAR_MOTORCYCLE;
    private int mCategory_type = 1;
    private EditText mStartLocAddress;
    private EditText mPoiName;
    private EditText mPoiDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(eu.opentransportnet.thisway.R.layout.activity_create_poi);
        Typeface tf = Typeface.createFromAsset(getAssets(), Const.FONTELLO_PATH);
        Button backButton = (Button) findViewById(eu.opentransportnet.thisway.R.id.back_button);
        backButton.setTypeface(tf);
        backButton.setOnClickListener(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        final ArrayList<Integer> id = getIntent().getIntegerArrayListExtra(MainActivity.EXTRA_ID);
        ArrayList<String> name = getIntent().getStringArrayListExtra(MainActivity.EXTRA_NAME);
        if (id==null || id.isEmpty()) {
            Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string
                    .server_error));
            finish();
        }
        else if (name.equals(null) || name.isEmpty()) {
            Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string
                    .server_error));
            finish();
        }

        TextView saveImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.save_image);
        saveImage.setTypeface(tf);

        TextView cancelImage = (TextView) findViewById(eu.opentransportnet.thisway.R.id.cancel_image);
        cancelImage.setTypeface(tf);

        TextView tf1 = (TextView) findViewById(eu.opentransportnet.thisway.R.id.tf_1);
        tf1.setTypeface(tf);

        Button myCoordinates = (Button) findViewById(eu.opentransportnet.thisway.R.id.my_coordinates);
        myCoordinates.setTypeface(tf);
        myCoordinates.setOnClickListener(this);

        Button pinLocation = (Button) findViewById(eu.opentransportnet.thisway.R.id.pin_location);
        pinLocation.setTypeface(tf);
        pinLocation.setOnClickListener(this);

        TextView title = (TextView) findViewById(eu.opentransportnet.thisway.R.id.title);
        title.setText(eu.opentransportnet.thisway.R.string.title_activity_create_poi);

        FloatingActionButton save = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.save);
        save.setOnClickListener(this);

        FloatingActionButton cancel = (FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.cancel);
        cancel.setOnClickListener(this);

        Spinner transport_spinner = (Spinner) findViewById(eu.opentransportnet.thisway.R.id.select_transport);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                eu.opentransportnet.thisway.R.array.create_poi_transport_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transport_spinner.setAdapter(adapter);

        transport_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {

                switch (arg2) {
                    case 0:
                        mTransport_type = Classificators.TRANSPORT_CAR_MOTORCYCLE;
                        break;
                    case 1:
                        mTransport_type = Classificators.TRANSPORT_BUS;
                        break;
                    case 2:
                        mTransport_type = Classificators.TRANSPORT_TRAIN;
                        break;
                    case 3:
                        mTransport_type = Classificators.TRANSPORT_WALK;
                        break;
                    case 4:
                        mTransport_type = Classificators.TRANSPORT_BIKE;
                        break;
                    case 5:
                        mTransport_type = Classificators.TRANSPORT_METRO;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        Spinner category_choice = (Spinner) findViewById(eu.opentransportnet.thisway.R.id.select_category);
        ArrayAdapter<String> category_adapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, name);

        category_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category_choice.setAdapter(category_adapter);

        category_choice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                mCategory_type=id.get(arg2);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        mPoiName = (EditText) findViewById(eu.opentransportnet.thisway.R.id.poi_text);
        mPoiDescription = (EditText) findViewById(eu.opentransportnet.thisway.R.id.poi_descripton_add);

        mStartLocAddress = (EditText) findViewById(eu.opentransportnet.thisway.R.id.edit);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PIN_LOCATION) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    sStartLat = extras.getDouble("latitude");
                    sStartLng = extras.getDouble("longitude");
                    String locAddress = Utils.getLocAddress(this, sStartLat, sStartLng);
                    mStartLocAddress.setText(locAddress);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        sStartLat = UNKNOWN;
        sStartLng = UNKNOWN;
        super.onDestroy();
    }

    /**
     * Save POI
     */
    private void save() {

        if (mPoiName.getText().length() == 0) {
            Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string.poi_text_empty));
            return;
        }

        File directory = new File(MainActivity.getContext().getFilesDir() + "/report");
        if (!directory.exists()) {
            directory.mkdir();
        }

        Date date = new Date();

        JSONObject location = new JSONObject();
        try {
            location.put("latitude", sStartLat);
            location.put("longitude", sStartLng);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject geometry = new JSONObject();
        try {
            geometry.put("geometry", location);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject reportObj = new JSONObject();
        try {
            JSONArray EMPTY = new JSONArray();
            SessionManager sessionManager = new SessionManager(this);
            User user = sessionManager.getUser();
            reportObj.put("userId", user.getHashedEmail());
            reportObj.put("appId", mAppId);
            reportObj.put("name", mPoiName.getText());
            reportObj.put("address", Utils.getLocAddress(this, sStartLat, sStartLng));
            reportObj.put("transportTypeId", mTransport_type);
            reportObj.put("poiRatings", EMPTY);
            reportObj.put("location", geometry);
            if (mPoiDescription.getText().length() != 0) {
                reportObj.put("description", mPoiDescription.getText());
            }
            reportObj.put("userGeneratedPoiCategory",mCategory_type);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setResult(RESULT_OK);

        boolean requestAdded = Requests.registerPoi(this, reportObj);
        if (requestAdded) {
            finish();
            Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string.report_saved));
        } else {
            if (saveReportLocally(this, date, reportObj)) {
                finish();
                Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string.report_saved));
                UploadTask.getInstance(getApplicationContext()).uploadRoutes();
            } else {
                Utils.showToastAtTop(this, getString(eu.opentransportnet.thisway.R.string.report_not_saved));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.thisway.R.id.back_button:
                finish();
                break;
            case eu.opentransportnet.thisway.R.id.pin_location:
                Intent intent = new Intent(this, PinLocationActivity.class);
                intent.putExtra("pinStart", true);
                startActivityForResult(intent, RC_PIN_LOCATION);
                break;
            case eu.opentransportnet.thisway.R.id.save:
                save();
                break;
            case eu.opentransportnet.thisway.R.id.cancel:
                finish();
                break;
            case eu.opentransportnet.thisway.R.id.my_coordinates:
                sStartLat = MainActivity.getRouteRecorder().getLatitude();
                sStartLng = MainActivity.getRouteRecorder().getLongitude();
                String startLocAddress = Utils.getLocAddress(this, sStartLat, sStartLng);
                mStartLocAddress.setText(startLocAddress);
                break;
        }
    }

    /**
     * If no internet, POI is saved locally
     * @param ctx context
     * @param date date of poi
     * @param reportObj poi json object
     * @return if everything success
     */
    public static boolean saveReportLocally(Context ctx, Date date, JSONObject reportObj) {
        // Report file name without extension
        String reportName = REPORT_PREFIX + SaveSensorData.TIME_FORMAT_FOR_FILE_NAME
                .format(date);

        File file = new File(ctx.getFilesDir(),
                "/" + Const.STORAGE_PATH_REPORT + "/" + reportName + ".json");

        if (file.exists()) {
            return true;
        }

        // Creates empty file
        File reportFile = com.library.routerecorder.Utils.createEmptyFile(ctx, "report", reportName,
                ".json");
        if (reportFile != null) {
            try {
                FileWriter fileWriter = new FileWriter(reportFile);

                fileWriter.write(reportObj.toString());
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
