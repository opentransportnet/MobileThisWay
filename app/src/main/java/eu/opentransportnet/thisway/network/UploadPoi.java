package eu.opentransportnet.thisway.network;

import android.content.Context;
import android.util.Base64;

import eu.opentransportnet.thisway.interfaces.VolleyRequestListener;
import eu.opentransportnet.thisway.utils.Const;
import eu.opentransportnet.thisway.utils.Utils;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Kristaps Krumins
 */
public class UploadPoi {
    public static final String REQUEST_TAG_ADD_POI = "addPoi";
    private static final String LOG_TAG = "uploadPoi";

    private Context mContex;
    private LinkedList<Integer> mPoiIndexes = new LinkedList<>();
    private File mKmlFile;
    private boolean mSomePoiHaveNotBeenUploaded = false;
    private JSONObject mRoute;
    private String mJsonFilePath;

    public UploadPoi(Context ctx, String jsonFilePath) {
        Utils.logD(LOG_TAG, "new UploadPoi called");
        mContex = ctx;
        mJsonFilePath = jsonFilePath;
        mRoute = Utils.getJsonFromFile(new File(mJsonFilePath));
        mKmlFile = new File(mContex.getCacheDir(), "routePoi.kml");
        String encodedKml;

        try {
            encodedKml = mRoute.getString("route_kml");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // Decode file
        byte[] decodedBytes = Base64.decode(encodedKml, Base64.DEFAULT);

        try {
            BufferedOutputStream writer =
                    new BufferedOutputStream(new FileOutputStream(mKmlFile));
            writer.write(decodedBytes);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uploadRouteAllPoiAndThenUploadRoute();
    }

    public void uploadRouteAllPoiAndThenUploadRoute() {
        try {
            SAXBuilder builder = new SAXBuilder();

            Document doc = builder.build(mKmlFile);
            Element rootNode = doc.getRootElement();

            Element document = rootNode.getChild("Document");
            List list = document.getChildren();

            String url = "http://" + Utils.getHostname() + Utils.getUrlPathStart() + Requests.PATH_ADD_POI;

            for (int i = 2; i < list.size(); i++) {
                Element placemark = (Element) list.get(i);
                Element point = placemark.getChild("Point");
                Attribute poiId = point.getAttribute("poiId");

                if (poiId != null) {
                    // POI already added, skip this one
                    continue;
                }

                String movementTypeId = point.getAttribute("id").getValue() + "000";
                String coordinates = point.getChild("coordinates").getValue();
                String[] lngLat = coordinates.split(",");
                String address = Utils.getLocAddress(mContex, Double.parseDouble(lngLat[1]),
                        Double.parseDouble(lngLat[0]));
                Element descriptionElem = placemark.getChild("description");
                String encodedDesc = descriptionElem.getText();
                String description = new String(Base64.decode(encodedDesc, Base64.DEFAULT));

                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("appId", Const.APPLICATION_ID);
                    jsonBody.put("userId", Utils.getHashedUserEmail(mContex));
                    jsonBody.put("name", "POI");
                    jsonBody.put("location", new JSONObject("{\"type\": \"Feature\", \"properties\": { \"name\": \" Location \"}, \"geometry\": { \"type\": \"Point\", \"latitude\": "
                            + lngLat[1] + ", \"longitude\": " + lngLat[0] + "} }"));
                    jsonBody.put("address", address);
                    jsonBody.put("poiSourceId", 1);
                    jsonBody.put("transportTypeId", movementTypeId);
                    jsonBody.put("description", description);
                    jsonBody.put("poiRatings", new JSONArray());
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
                Utils.logD(LOG_TAG, jsonBody.toString());

                mPoiIndexes.add(i);
                Requests.sendRequest(mContex, url, jsonBody, new VolleyRequestListener<JSONObject>() {
                    @Override
                    public void onResult(JSONObject object) {
                        Utils.logD(LOG_TAG, "onResult " + object.toString());
                        try {
                            int index;
                            index = mPoiIndexes.removeFirst();
                            poiUploaded(object.getString("poiId"), index);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (NoSuchElementException e) {
                            return;
                        }
                    }

                    @Override
                    public void onError(JSONObject object) {
                        Utils.logD(LOG_TAG, "onError " + object.toString());
                        mSomePoiHaveNotBeenUploaded = true;
                        try {
                            int index;
                            index = mPoiIndexes.removeFirst();
                        } catch (NoSuchElementException e) {
                            return;
                        }
                    }
                }, REQUEST_TAG_ADD_POI);

            }

        } catch (IOException io) {
            io.printStackTrace();
            return;
        } catch (JDOMException e) {
            e.printStackTrace();
            return;
        }
        return;
    }

    private void poiUploaded(String poiId, int index) {
        Utils.logD(LOG_TAG, "POI ID:" + poiId + " Index:" + index);

        try {
            SAXBuilder builder = new SAXBuilder();

            Document doc = builder.build(mKmlFile);
            Element rootNode = doc.getRootElement();

            Element document = rootNode.getChild("Document");
            List list = document.getChildren();

            Element placemark = (Element) list.get(index);
            Element point = placemark.getChild("Point");
            point.setAttribute("poiId", poiId);

            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(mKmlFile));
            Utils.logD(LOG_TAG, "POI ID saved in tmp KML file");

        } catch (IOException io) {
            io.printStackTrace();
            return;
        } catch (JDOMException e) {
            e.printStackTrace();
            return;
        }

        if (mPoiIndexes.peekFirst() == null) {
            Utils.logD(LOG_TAG, "method poiUploaded reached end");
            String encodedKmlFile;
            try {
                encodedKmlFile = Utils.encodeBase64(mKmlFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            mRoute.remove("route_kml");

            try {
                mRoute.put("route_kml", encodedKmlFile);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            File jsonFile = new File(mJsonFilePath);
            String fileName = jsonFile.getName().replace(".json", "");

            File originalKmlFile = new File(mContex.getFilesDir() + "/" + Const.APP_ROUTES_CSV_PATH + "/" +
                    fileName + ".kml");
            // Rewrite original KML with modified.
            try {
                Utils.copyFile(mKmlFile, originalKmlFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Save modified KML in JSON
            Utils.saveStringInFile(mJsonFilePath, mRoute.toString());

            if (mSomePoiHaveNotBeenUploaded == false) {
                Requests.registerTrack(mContex, mRoute, fileName);
            }
        }
    }
}
