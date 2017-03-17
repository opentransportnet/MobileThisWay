package eu.opentransportnet.thisway.network;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.activities.CreatePoiActivity;
import eu.opentransportnet.thisway.activities.SearchRoutesActivity;
import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.OtnCrypto;
import eu.opentransportnet.thisway.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Kristaps Krumins
 */
public class Requests {
    public static final String PATH_REGISTER_USER = "/platform/users/addUser";
    public static final String PATH_REGISTER_TRACK = "/platform/tracks/addTracks";
    public static final String PATH_DELETE_TRACK = "/platform/tracks/deleteTrack";
    public static final String PATH_ADD_POI = "/platform/pois/addPois";
    public static final String PATH_DELETE_POI = "/platform/pois/deletePoi";
    public static final String PATH_DELETE_USER = "/platform/users/deleteUserContent";
    public static final String PATH_LOAD_PUBLIC_POI = "/platform/pois/loadPublicPoi";
    public static final String PATH_LOAD_POI = "/platform/pois/loadPoi";
    public static final String PATH_GET_TRACKS = "/platform/tracks/getTracks";
    public static final String PATH_GET_TRACK = "/platform/tracks/getTrack";
    public static final String PATH_GET_STATISTICS = "/platform/tracks/getStatistics";
    public static final String PATH_REGISTER_POI = "/platform/pois/addPois";
    public static final String PATH_UPDATE_TRACK = "/platform/tracks/updateTrack";
    public static final String PATH_GET_POI_CATEGORY = "/platform/poicategory";

    public static final String TAG_REGISTER_TRACK_REQUEST = "register track request";

    private static final String LOG_TAG = "Requests";

    private static Integer mNotificationId = Const.NOTIFICATION_BASE_FOR_UPLOAD;

    /**
     * Sends JSONObject to server
     *
     * @param ctx            The context
     * @param showErrorToast If true then error toast will be shown when error is detected
     * @param url            The URL for request
     * @param jsonBody       The JSONObject to be sent
     * @param listener       The listener for request response
     * @param tag            The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean sendRequest(final Context ctx, final boolean showErrorToast, String url, JSONObject
            jsonBody, final VolleyRequestListener<JSONObject> listener, String tag) {

        if (!Utils.isConnected(ctx)) {
            Utils.logD(LOG_TAG, "No Internet connectivity");

            if (showErrorToast) {
                Utils.showToastAtTop(ctx, ctx.getString(R.string.network_unavailable));
            }

            return false;
        } else if (jsonBody == null) {
            Utils.logD(LOG_TAG, "JSONObject is 'null'");

            if (showErrorToast) {
                Utils.showToastAtTop(ctx, ctx.getString(R.string.something_went_wrong));
            }

            return false;
        }

        Utils.logD(LOG_TAG, "Request url:" + url);
        Utils.logD(LOG_TAG, "JSON data:" + jsonBody.toString());

        Request request;

        if (Utils.isEncryption() == false) {
            request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Utils.logD(LOG_TAG, "JSON response:" + response);
                            listener.onResult(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (showErrorToast) {
                                Utils.showToastAtTop(ctx, ctx.getString(R.string.something_went_wrong));
                            }

                            if (error != null && error.networkResponse != null) {
                                Utils.logD(LOG_TAG, "Error response. Response code " + error
                                        .networkResponse.statusCode);
                            }

                            listener.onError(null);
                        }
                    });
        } else {
            final String encryptedBody = OtnCrypto.encrypt(jsonBody.toString());
            Utils.logD(LOG_TAG, "encrypted JSON data:" + encryptedBody);

            request = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String encryptedResponse) {
                            String responseString = OtnCrypto.decrypt(encryptedResponse);
                            JSONObject response;

                            try {
                                response = new JSONObject(responseString);
                                Utils.logD(LOG_TAG, "JSON response:" + response);
                                listener.onResult(response);
                                return;
                            } catch (JSONException e) {
                                e.printStackTrace();

                                if (showErrorToast) {
                                    Utils.showToastAtTop(ctx, ctx.getString(R.string.something_went_wrong));
                                }

                                Utils.logD(LOG_TAG, "Decrypted message is not in JSON format");
                                listener.onError(null);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (showErrorToast) {
                                Utils.showToastAtTop(ctx, ctx.getString(R.string.something_went_wrong));
                            }

                            if (error != null && error.networkResponse != null) {
                                Utils.logD(LOG_TAG, "Error response. Response code " + error
                                        .networkResponse.statusCode);
                            }

                            listener.onError(null);
                        }
                    }) {

                @Override
                public byte[] getBody() throws AuthFailureError {
                    return encryptedBody.getBytes();
                }

                @Override
                public String getBodyContentType() {
                    return "text/plain";
                }
            };
        }

        // Adds request to request queue
        RequestQueueSingleton.getInstance(ctx.getApplicationContext())
                .addToRequestQueue(request, tag);

        return true;
    }

    /**
     * Sends JSONObject to server and does not show error messages (toasts)
     *
     * @param ctx      The context
     * @param url      The URL for request
     * @param jsonBody The JSONObject to be sent
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean sendRequest(Context ctx, String url, JSONObject jsonBody,
                                      final VolleyRequestListener<JSONObject> listener,
                                      String tag) {
        return sendRequest(ctx, false, url, jsonBody, listener, tag);
    }

    public static boolean registerUser(Context ctx, String userEmail,
                                       final VolleyRequestListener<JSONObject> listener) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_REGISTER_USER;

        return sendRequest(ctx, url, jsonBody, listener, null);
    }

    /**
     * Uploads track (register track) and on success deletes local version
     */
    public static boolean registerTrack(final Context ctx, JSONObject jsonBody, final String fileName) {
        Notification.Builder notificationBuilder = new Notification.Builder(ctx
                .getApplicationContext());
        notificationBuilder.setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setTicker("");
        final Integer notificationID = mNotificationId;
        final NotificationManager mNotificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() +
                PATH_REGISTER_TRACK;

        boolean requestSent = sendRequest(ctx, url, jsonBody, new
                VolleyRequestListener<JSONObject>
                        () {
                    @Override
                    public void onResult(JSONObject response) {
                        mNotificationManager.cancel(notificationID);
                        try {
                            int responseCode = response.getInt("responseCode");

                            if (responseCode != 0) {
                                // Error
                                return;
                            } else {
                                // track saved on server

                                // Delete local files
                                File file = new File(ctx.getFilesDir(),
                                        "/" + Const.APP_ROUTES_JSON_PATH + "/" + fileName + ".json");
                                file.delete();

                                file = new File(ctx.getFilesDir(),
                                        "/" + Const.APP_ROUTES_CSV_PATH + "/" + fileName + ".csv");
                                file.delete();

                                file = new File(ctx.getFilesDir(),
                                        "/" + Const.APP_ROUTES_CSV_PATH + "/" + fileName + ".kml");
                                file.delete();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(JSONObject object) {
                        mNotificationManager.cancel(notificationID);
                    }
                }, TAG_REGISTER_TRACK_REQUEST);

        if (!requestSent) {
            return false;
        }

        Notification notification = notificationBuilder.build();
        //Send the notification
        mNotificationManager.notify(notificationID, notification);

        return true;
    }

    public static boolean registerPoi(final Context ctx, final JSONObject jsonBody,
                                      final boolean save, final String filePath) {
        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_REGISTER_POI;

        return sendRequest(ctx, url, jsonBody, new VolleyRequestListener<JSONObject>() {
            @Override
            public void onResult(JSONObject response) {
                try {
                    int responseCode = response.getInt("responseCode");
                    if (responseCode != 0) {
                        // Error
                        if (save) {
                            Date date = new Date();
                            CreatePoiActivity.saveReportLocally(ctx, date, jsonBody);
                        }
                    } else {
                        // Report issue saved on server
                        if (save == false) {
                            File file = new File(filePath);
                            file.delete();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(JSONObject object) {
                if (save) {
                    Date date = new Date();
                    CreatePoiActivity.saveReportLocally(ctx, date, jsonBody);
                }
            }
        }, null);
    }

    public static boolean registerPoi(final Context ctx, final JSONObject jsonBody) {
        return registerPoi(ctx, jsonBody, true, null);
    }

    private Integer getNotificationId() {
        return mNotificationId;
    }

    /**
     * Downloads track list from server
     *
     * @param ctx            The context
     * @param onlyMyTracks   If true then downloads only users tracks, else public
     * @param filterByPoints If true then adds start and destination points
     * @param listener       The listener for request response
     * @param tag            The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getUsersTracks(Context ctx, boolean onlyMyTracks, boolean filterByPoints,
                                         final VolleyRequestListener<JSONObject> listener,
                                         String tag) {
        JSONObject jsonBody = new JSONObject();
        try {
            if (onlyMyTracks) {
                jsonBody.put("isMine", true);
                //jsonBody.put("isPublic", true);
            } else {
                // jsonBody.put("isMine", true);
                jsonBody.put("isPublic", true);
            }
            String userEmail = Utils.getHashedUserEmail(ctx);
            jsonBody.put("userId", userEmail);
            jsonBody.put("appId", Const.APPLICATION_ID);
            if (filterByPoints) {
                double[] pCoords = SearchRoutesActivity.getStartAndDestPoints();
                double unknown = SearchRoutesActivity.UNKNOWN;
                if (pCoords[0] != unknown && pCoords[1] != unknown) {
                    jsonBody.put("fromLat", pCoords[0]);
                    jsonBody.put("fromLon", pCoords[1]);
                }
                if (pCoords[2] != unknown && pCoords[3] != unknown) {
                    jsonBody.put("toLat", pCoords[2]);
                    jsonBody.put("toLon", pCoords[3]);
                }
                jsonBody.put("radius", SearchRoutesActivity.getRadius());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_GET_TRACKS;

        return sendRequest(ctx, url, jsonBody, listener, tag);
    }

    /**
     * Downloads track information for given track
     *
     * @param ctx      The context
     * @param trackId  The track ID
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getTrackInfo(Context ctx, String trackId,
                                       final VolleyRequestListener<JSONObject> listener,
                                       String tag) {
        String userEmail = Utils.getHashedUserEmail(ctx);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("trackId", trackId);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_GET_TRACK;

        return sendRequest(ctx, url, jsonBody, listener, tag);
    }

    /**
     * Downloads track statistics for given track
     *
     * @param ctx      The context
     * @param trackId  The track ID
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getTrackStatistics(Context ctx, String trackId,
                                             final VolleyRequestListener<JSONObject> listener,
                                             String tag) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("trackId", trackId);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() +
                PATH_GET_STATISTICS;

        return sendRequest(ctx, url, jsonBody, listener, tag);
    }

    /**
     * Downloads WMS capabilities
     *
     * @param ctx      The context
     * @param wmsUrl   The WMS URL
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If {@link StringRequest} has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean getWmsCapabilities(Context ctx, String wmsUrl,
                                             final VolleyRequestListener<String> listener,
                                             String tag) {
        if (!Utils.isConnected(ctx)) {
            return false;
        }

        String lang = "eng";

        if (Locale.getDefault().getLanguage().equals("fr")) {
            lang = "fre";
        }

        String url = wmsUrl + "&SERVICE=WMS&VERSION=1.3.0&language=" + lang + "&REQUEST=GetCapabilities";
        Utils.logD(LOG_TAG, "Request url:" + url);

        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        listener.onResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utils.logD(LOG_TAG, error.toString());
                        listener.onError(null);
                    }
                }
        );

        // Increase timeout, because their server is slow
        req.setRetryPolicy(new DefaultRetryPolicy(
                RequestQueueSingleton.LONG_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Adds request to request queue
        RequestQueueSingleton.getInstance(ctx.getApplicationContext())
                .addToRequestQueue(req, tag);

        return true;
    }

    /**
     * Update route public status
     *
     * @param ctx      The context
     * @param trackId  The track ID
     * @param isPublic Is route public
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean updateTrackPublicStatus(Context ctx, int trackId, boolean isPublic,
                                                  final VolleyRequestListener<JSONObject> listener,
                                                  String tag) {
        String userEmail = Utils.getHashedUserEmail(ctx);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("trackId", trackId);
            jsonBody.put("appId", Const.APPLICATION_ID);
            jsonBody.put("is_public", isPublic);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_UPDATE_TRACK;

        return sendRequest(ctx, true, url, jsonBody, listener, tag);
    }

    /**
     * Delete users POI. Default request error toasts enabled.
     *
     * @param ctx      The context
     * @param poiId    The POI ID
     * @param listener The listener for request response
     * @param tag      The request tag, used for canceling request
     * @return If request has been added to request queue returns {@code true},
     * otherwise{@code false}
     */
    public static boolean deletePoi(Context ctx, int poiId,
                                    final VolleyRequestListener<JSONObject> listener,
                                    String tag) {
        String userEmail = Utils.getHashedUserEmail(ctx);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userEmail);
            jsonBody.put("poiId", poiId);
            jsonBody.put("appId", Const.APPLICATION_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + PATH_DELETE_POI;

        return sendRequest(ctx, true, url, jsonBody, listener, tag);
    }


    public static boolean getPOICategory(Context ctx, String url,
                                             final VolleyRequestListener<String> listener,
                                             String tag) {
        if (!Utils.isConnected(ctx)) {
            return false;
        }


        Utils.logD(LOG_TAG, "Request url:" + url);

        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        listener.onResult(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utils.logD(LOG_TAG, error.toString());
                        listener.onError(null);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", "application/json");
                return headers;
            };
        };

        // Increase timeout, because their server is slow
        req.setRetryPolicy(new DefaultRetryPolicy(
                RequestQueueSingleton.LONG_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Adds request to request queue
        RequestQueueSingleton.getInstance(ctx.getApplicationContext())
                .addToRequestQueue(req, tag);

        return true;
    }
}
