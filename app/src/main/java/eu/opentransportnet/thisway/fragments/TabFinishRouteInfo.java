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
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.activities.MainActivity;
import eu.opentransportnet.thisway.network.RequestQueueSingleton;
import eu.opentransportnet.thisway.network.UploadPoi;
import eu.opentransportnet.thisway.utils.Classificators;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.Utils;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tab which has information about finished route
 *
 * @author Ilmars Svilsts
 */
public class TabFinishRouteInfo extends Fragment implements View.OnClickListener {

    private Activity mActivity;
    private int sCloud = 0;
    private int sSnow = 0;
    private int sSun = 0;
    public double distance = 0;
    SimpleDateFormat FORMATER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
    String mStart_date = FORMATER.format(new Date());
    String mEnd_date = FORMATER.format(new Date());
    double[] startAndDestCoord = new double[4];
    long diff = 0;

    public final String TAG_STOP_VERIFICATION = "delete user request";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        View v = inflater.inflate(R.layout.tab_finish_route_info, container, false);

        Typeface tf = Typeface.createFromAsset(mActivity.getAssets(), Const.FONTELLO_PATH);

        FloatingActionButton saveBtn = (FloatingActionButton) v.findViewById(R.id.save_button);
        saveBtn.setOnClickListener(this);

        FloatingActionButton cancelBtn =
                (FloatingActionButton) v.findViewById(R.id.cancel_button);
        cancelBtn.setOnClickListener(this);

        TextView saveImage = (TextView) v.findViewById(R.id.save_image);
        saveImage.setTypeface(tf);

        TextView cancelImage = (TextView) v.findViewById(R.id.cancel_image);
        cancelImage.setTypeface(tf);

        TextView sun = (TextView) v.findViewById(R.id.sun);
        sun.setTypeface(tf);
        TextView cloud = (TextView) v.findViewById(R.id.cloud);
        cloud.setTypeface(tf);
        TextView snow = (TextView) v.findViewById(R.id.snow);
        snow.setTypeface(tf);

        v.findViewById(R.id.idsnow).setOnClickListener(this);
        v.findViewById(R.id.idsun).setOnClickListener(this);
        v.findViewById(R.id.idcloud).setOnClickListener(this);


        String csvFileName = MainActivity.getRouteRecorder().getCurrRecordedRoute();
        String csvFilePath = mActivity.getFilesDir() + "/" + Const.APP_ROUTES_CSV_PATH + "/" +
                csvFileName + ".csv";

        File FILES = new File(csvFilePath);

        try {

            if (FILES.exists()) {
                int mCount = 0;
                try {
                    CSVReader READER = new CSVReader(new FileReader(csvFilePath), ';');
                    String[] mNextLine;
                    while ((mNextLine = READER.readNext()) != null) {
                        if (mCount == 1) {

                            mStart_date = mNextLine[6];

                            startAndDestCoord[0] = Double.parseDouble(mNextLine[0]);
                            startAndDestCoord[1] = Double.parseDouble(mNextLine[1]);

                            startAndDestCoord[2] = Double.parseDouble(mNextLine[0]);
                            startAndDestCoord[3] = Double.parseDouble(mNextLine[1]);


                        } else if (!(mCount == 0)) {
                            distance += Double.parseDouble(mNextLine[4]);
                            startAndDestCoord[2] = Double.parseDouble(mNextLine[0]);
                            startAndDestCoord[3] = Double.parseDouble(mNextLine[1]);
                        }

                        mEnd_date = mNextLine[6];
                        mCount++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //The dynamic array would be empty
                }
            }

            Date d1 = null;
            Date d2 = null;

            try {
                d1 = (Date) FORMATER.parse(mStart_date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                d2 = (Date) FORMATER.parse(mEnd_date);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //in milliseconds
            diff = d2.getTime() - d1.getTime();
            TextView dist = (TextView) v.findViewById(R.id.distance_tab_finish_route_info_activity);
            dist.setText(String.format("%.02f", distance / 1000) + " km");
            dist = (TextView) v.findViewById(R.id.duration_tab_finish_route_info_activity);
            dist.setText(String.valueOf(getHederDate(diff)));
        } catch (Exception e) {
        }

        return v;
    }

    /**
     * Total route time for needed format to save in file
     * @param millis total time in milli seconds
     * @return returns total time for route
     */
    public static String getDat(long millis) {
        long MINUTE = (millis / (1000 * 60)) % 60;
        long HOUR = (millis / (1000 * 60 * 60)) % 24;

        String min = String.format("%02d", MINUTE);
        String TIME = HOUR + "." + min;

        if (Integer.parseInt(min) < 1) {
            TIME = HOUR + ".01";
        }
        return TIME;
    }

    /**
     * Return total time for route
     * @param millis total time in milli seconds
     * @return returns total time for route
     */
    public static String getHederDate(long millis) {
        long MINUTE = (millis / (1000 * 60)) % 60;
        long HOUR = (millis / (1000 * 60 * 60)) % 24;

        String min = String.format("%02d", MINUTE);
        String TIME = HOUR + "h " + min + " min";
        if (Integer.parseInt(min) < 1) {
            TIME = HOUR + "h 01min";
        }
        return TIME;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RequestQueueSingleton.getInstance(mActivity).cancelAllPendingRequests(TAG_STOP_VERIFICATION);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.idsnow:
                if (sSnow == 1) {
                    FloatingActionButton fabsnow = (FloatingActionButton) v.findViewById(R.id.idsnow);
                    fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.type_button)));
                    sSnow = 0;
                } else {
                    FloatingActionButton fabsnow = (FloatingActionButton) v.findViewById(R.id.idsnow);
                    fabsnow.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_register)));
                    sSnow = 1;
                }
                break;
            case R.id.idsun:
                if (sSun == 1) {
                    FloatingActionButton fabsun = (FloatingActionButton) v.findViewById(R.id.idsun);
                    fabsun.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.type_button)));
                    sSun = 0;
                } else {
                    FloatingActionButton fabsun = (FloatingActionButton) v.findViewById(R.id.idsun);
                    fabsun.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_register)));
                    sSun = 1;
                }
                break;
            case R.id.idcloud:
                if (sCloud == 1) {
                    FloatingActionButton fabcloud = (FloatingActionButton) v.findViewById(R.id.idcloud);
                    fabcloud.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.type_button)));
                    sCloud = 0;
                } else {
                    FloatingActionButton fabcloud = (FloatingActionButton) v.findViewById(R.id.idcloud);
                    fabcloud.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.color_register)));
                    sCloud = 1;
                }
                break;
            case R.id.save_button:
                MainActivity.getRouteRecorder().finishRoute();

                EditText Description = (EditText) v.getRootView().findViewById(R.id.descrption_tab_finish_route_info_activity);
                String Desc = Description.getText().toString();
                JSONArray myArray = new JSONArray();
                try {

                    if (sSun == 1) {
                        JSONObject jo = new JSONObject();
                        jo.put("weatherTypeId", Classificators.WEATHER_SUNNY);
                        myArray.put(jo);
                    }
                    if (sCloud == 1) {
                        JSONObject jo = new JSONObject();
                        jo.put("weatherTypeId", Classificators.WEATHER_CLOUDY);
                        myArray.put(jo);
                    }
                    if (sSnow == 1) {
                        JSONObject jo = new JSONObject();
                        jo.put("weatherTypeId", Classificators.WEATHER_RAINY);
                        myArray.put(jo);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                saveJson(Double.parseDouble(getDat(diff)), distance, startAndDestCoord, Desc,
                        myArray, ((Switch) v.getRootView().findViewById(R.id.is_public)).isChecked());

                MainActivity.sCanrecord = true;

                mActivity.setResult(mActivity.RESULT_OK);
                mActivity.finish();
                break;
            case R.id.cancel_button:
                mActivity.finish();
                break;
        }
    }

    /**
     * Saves route data after "save" button click
     * @param duration route duration
     * @param distance route distance
     * @param startAndDestCoord route start and destination coordinates
     * @param mDesc route description
     * @param weather route weather
     * @param isPublic is route public or private
     */
    public void saveJson(double duration, double distance, double[] startAndDestCoord, String
            mDesc, JSONArray weather, boolean isPublic) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("duration", duration);
            obj.put("distance", distance);
//        obj.put("transportId", Utils.getMovementType(this));
            obj.put("transportId", "1000");
            obj.put("appId", Const.APPLICATION_ID);
            obj.put("userId", Utils.getHashedUserEmail(getActivity()));
            obj.put("lat_start", startAndDestCoord[0]);
            obj.put("lon_start", startAndDestCoord[1]);
            obj.put("lat_end", startAndDestCoord[2]);
            obj.put("lon_end", startAndDestCoord[3]);
            obj.put("description", mDesc);
            obj.put("start_address", Utils.getLocAddress(getActivity(),
                    startAndDestCoord[0], startAndDestCoord[1]));
            obj.put("end_address", Utils.getLocAddress(getActivity(),
                    startAndDestCoord[2], startAndDestCoord[3]));
            obj.put("is_public", isPublic);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String encodedCsvFile;
        try {
            encodedCsvFile = Utils.encodeBase64(getActivity().getFilesDir() + "/" + Const
                    .APP_ROUTES_CSV_PATH +
                    "/" + MainActivity.getRouteRecorder().getCurrRecordedRoute() + ".csv");
        } catch (Exception e) {
            e.printStackTrace();
            encodedCsvFile = "null";
        }
        try {
            obj.put("trackFileCsv", encodedCsvFile);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String encodedKmlFile;
        try {
            encodedKmlFile = Utils.encodeBase64(getActivity().getFilesDir() + "/" + Const
                    .APP_ROUTES_CSV_PATH +
                    "/" + MainActivity.getRouteRecorder().getCurrRecordedRoute() + ".kml");
        } catch (Exception e) {
            e.printStackTrace();
            encodedKmlFile = "null";
        }

        try {
            obj.put("route_kml", encodedKmlFile);
            JSONArray EMPTY = new JSONArray();
            obj.put("trackRatings", EMPTY);
            obj.put("weatherList", weather);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        saveRouteLocally(obj);
    }

    /**
     * Save route locally, to file
     * @param route object of route
     * @return if success
     */
    public boolean saveRouteLocally(JSONObject route) {
        String filePath = getActivity().getFilesDir() + "/" + Const.APP_ROUTES_JSON_PATH + "/" +
                MainActivity
                        .getRouteRecorder().getCurrRecordedRoute() + ".json";
        String jsonString;
        jsonString = route.toString();
        // write back JSON file
        if (!writeJSON(filePath, jsonString)) {
            // If writing failed
            String start = getString(R.string.something_went_wrong);
            Utils.showToastAtTop(getContext(), start);
            return false;
        }
        String start = getString(R.string.saved_activity_stats);
        Utils.showToastAtTop(getContext(), start);
        System.gc();
        return true;
    }

    /**
     * Write string in file
     * @param filePath file location
     * @param obj string that will be written in file
     * @return if success
     */
    public boolean writeJSON(String filePath, String obj) {
        try {
            File report = new File(filePath);
            if (!report.exists()) {
                report.createNewFile();
            }
            FileWriter file = new FileWriter(filePath);
            file.write(obj);
            file.flush();
            file.close();
            new UploadPoi(getContext(), filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}