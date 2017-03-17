package eu.opentransportnet.thisway.network;

import android.content.Context;

import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Kristaps Krumins
 */
public class UploadTask {
    public static boolean sUploadTracks = true;

    private static UploadTask mInstance;
    private static Context sAppCtx;

    private boolean mUploadStarted = false;

    private UploadTask(Context ctx) {
        sAppCtx = ctx.getApplicationContext();
    }

    public static synchronized UploadTask getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new UploadTask(context);
        }
        return mInstance;
    }

    public void startScheduledUpload() {
        if (!mUploadStarted) {
            // Sets a new Timer
            Timer timer = new Timer();
            // Initializes the TimerTask's job
            TimerTask timerTask = initializeScheduledUpload();
            // Schedules the timer, after the first 1000ms theTimerTask will run every minute
            timer.schedule(timerTask, 1000, 60000);
            mUploadStarted = true;
        }
    }

    private TimerTask initializeScheduledUpload() {
        TimerTask timerTask = new TimerTask() {
            public void run() {
                if (sUploadTracks && Utils.isConnected(sAppCtx)) {
                    uploadRoutes();
                    uploadPOIData();
                }

            }
        };
        return timerTask;
    }

    public void uploadRoutes() {
        File dir = new File(sAppCtx.getFilesDir() + "/" + Const.APP_ROUTES_JSON_PATH);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                JSONObject jsonBody = Utils.getJsonFromFile(file);
                if (jsonBody == null) {
                    continue;
                }

                String fileName = file.getName();
                int pos = fileName.lastIndexOf(".");
                if (pos > 0) {
                    fileName = fileName.substring(0, pos);
                }

                Requests.registerTrack(sAppCtx, jsonBody, fileName);
            }
        }
    }

    private void uploadPOIData() {
        String filePath = "";
        filePath = Const.STORAGE_PATH_REPORT;

        File dir = new File(sAppCtx.getFilesDir() + "/" + filePath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            JSONParser parser = new JSONParser();
            for (File file : directoryListing) {
                Object obj;
                try {
                    obj = parser.parse(new FileReader(file));
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                } catch (ParseException e) {
                    e.printStackTrace();
                    continue;
                }

                if (obj == null) {
                    continue;
                }

                String jsonBodyString = ((org.json.simple.JSONObject) obj).toJSONString();
                JSONObject jsonBody;
                try {
                    jsonBody = new JSONObject(jsonBodyString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }

                String fileName = file.getName();
                int pos = fileName.lastIndexOf(".");
                if (pos > 0) {
                    fileName = fileName.substring(0, pos);
                }

                Requests.registerPoi(sAppCtx, jsonBody, false, file.getAbsolutePath());
            }
        }

    }
}
