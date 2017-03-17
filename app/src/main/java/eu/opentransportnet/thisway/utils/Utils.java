package eu.opentransportnet.thisway.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import eu.opentransportnet.thisway.activities.SearchRoutesActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Kristaps Krumins
 * @author Ilmars Svilsts
 */
public class Utils {
    // Should be set on app start
    private static Context mContext;
    private static String[] sLocalRouteFileNames;

    /**
     * Gets JSONObject from given JSON file
     *
     * @param filename the JSON file
     * @return JSONObject if reading successful, {@code null} otherwise
     */
    public static JSONObject getJsonObj(String filename) {
        try {
            String jsonData = "";
            String line;
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null) {
                jsonData += line + "\n";
            }
            if (br != null) br.close();
            return new JSONObject(jsonData);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Makes given image in circle form
     *
     * @param bitmap the image
     * @return circle image
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = bitmap.getWidth() / 2;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Downloads route
     *
     * @param ctx        The context
     * @param encodedKml The encoded KML file
     * @return Path to temporary KML file
     */
    public static String getRouteKmlFile(Context ctx, String encodedKml) {
        // Decode file
        byte[] decodedBytes;
            decodedBytes = Base64.decode(encodedKml, Base64.DEFAULT);

        File kmlFile;
        kmlFile = new File(ctx.getCacheDir(), "route.kml");
        try {
            BufferedOutputStream writer =
                    new BufferedOutputStream(new FileOutputStream(kmlFile));
            writer.write(decodedBytes);
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "file://" + kmlFile.getAbsolutePath();
    }

    /**
     * Gets all routes stored locally
     * @param ctx context
     * @return JSON array with local routes
     */
    public static JSONArray getAllLocalRoutes(Context ctx) {
        JSONArray localRoutes = new JSONArray();
        File dir = new File(ctx.getFilesDir() + "/" + Const.APP_ROUTES_JSON_PATH);
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            sLocalRouteFileNames = new String[directoryListing.length];
            int i = 0;
            for (File file : directoryListing) {
                JSONParser parser = new JSONParser();
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
                sLocalRouteFileNames[i++] = file.getAbsolutePath();
                localRoutes.put(jsonBody);
            }
        }

        return localRoutes;
    }

    /**
     * Returns address of latitude and longitude
     * @param context context
     * @param latitude address latitude
     * @param longitude address longitude
     * @return return address
     */
    public static String getLocAddress(Context context, double latitude, double longitude) {
        Geocoder gc = new Geocoder(context);
        if (gc.isPresent()) {
            List<Address> list = null;
            try {
                list = gc.getFromLocation(latitude, longitude, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (list != null) {
                if (list.size() != 0) {
                    Address address = list.get(0);

                    try {
                        if (address.getAddressLine(0).equals(null)) {
                            return String.valueOf((double) Math.round(latitude * 100000) / 100000) + ", " +
                                    String.valueOf((double) Math.round(longitude * 100000) / 100000);
                        }

                        String Adrese = address.getAddressLine(0);
                        return String.valueOf(Adrese);
                    } catch (Exception e) {
                    }
                }
            }
        }
        // Returns coordinates rounded to 5 digit precision
        return String.valueOf((double) Math.round(latitude * 100000) / 100000) + ", " +
                String.valueOf((double) Math.round(longitude * 100000) / 100000);
    }

    /**
     * Shows toast at the top of necessary activity
     * @param ctx activity context
     * @param message string that will be in message
     */
    public static void showToastAtTop(Context ctx, String message) {
        CharSequence text = message;
        Toast toast = Toast.makeText(ctx, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 110);
        toast.show();
    }

    /**
     * encode file to base64
     * @param sourceFile file location
     * @return encoded string
     */
    public static String encodeBase64(String sourceFile) throws Exception {

        byte[] base64EncodedData = getFileAsBytesArray(sourceFile);

        String imageEncodeds = Base64.encodeToString(base64EncodedData, Base64.NO_WRAP);

        return imageEncodeds;
    }

    /**
     * Return file as byte array
     * @param fileName file location
     * @return file as byte array
     */
    public static byte[] getFileAsBytesArray(String fileName) throws Exception {

        File file = new File(fileName);
        int length = (int) file.length();
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[length];
        reader.read(bytes, 0, length);
        reader.close();
        return bytes;

    }

    /**
     * Get json object from file
     * @param file file
     * @return json object from file
     */
    public static JSONObject getJsonFromFile(File file) {
        JSONParser parser = new JSONParser();
        Object obj = null;
        try {
            obj = parser.parse(new FileReader(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        if (obj == null) {
            return null;
        }

        String jsonBodyString = ((org.json.simple.JSONObject) obj).toJSONString();
        JSONObject json;
        try {
            json = new JSONObject(jsonBodyString);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return json;
    }

    /**
     * Saves string in file
     * @param absolutePathOfFile file path
     * @param jsonString string that will be writen in file
     */
    public static void saveStringInFile(String absolutePathOfFile, String jsonString) {
        try {
            FileWriter file = new FileWriter(absolutePathOfFile);
            file.write(jsonString);
            file.flush();
            file.close();
        } catch (IOException e) {
            return;
        }
    }

    /**
     * The ConnectivityManager to query the active network and determine if it has Internet
     * connectivity
     *
     * @param ctx The context
     * @return {@code true} if there is Internet connectivity, otherwise {@code false}
     */
    public static boolean isConnected(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static void saveLocale(Context ctx, String lang) {
        SharedPreferences langPref = ctx.getSharedPreferences("language", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = langPref.edit();
        editor.putString("languageToLoad", lang);
        editor.commit();
    }

    public static void loadLocale(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("language", Activity.MODE_PRIVATE);
        String language = prefs.getString("languageToLoad", "");
        changeLanguage(ctx, language);
    }


    public static void changeLanguage(Context ctx, String lang) {
        if (lang.equalsIgnoreCase("")) {
            // Use default
            lang = "en";
        }

        Locale locale = new Locale(lang);
        saveLocale(ctx, lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        ctx.getResources().updateConfiguration(config, ctx.getResources().getDisplayMetrics());
    }

    public static String[] getLocalRouteFileNames() {
        return sLocalRouteFileNames;
    }

    public static void enableLoseFocusInContent(Activity activity) {
        View view = activity.findViewById(android.R.id.content);
        enableLoseFocusInView(activity, view);
    }

    /**
     * Enables to lose focus form EditText when clicked outside
     */
    public static void enableLoseFocusInView(final Activity activity, View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(
                            activity.INPUT_METHOD_SERVICE);
                    View currentView = activity.getCurrentFocus();
                    if (currentView != null) {
                        imm.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
                    }
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                enableLoseFocusInView(activity, innerView);
            }
        }
    }

    public static double[] geocode(Context ctx, String address) {
        double[] coordinates = new double[2];
        List<Address> foundGeocode;
        try {
            foundGeocode = new Geocoder(ctx).getFromLocationName(address, 1);
        } catch (IOException e) {
            e.printStackTrace();
            coordinates[0] = SearchRoutesActivity.UNKNOWN;
            coordinates[1] = SearchRoutesActivity.UNKNOWN;
            return coordinates;
        }
        try {
            coordinates[0] = foundGeocode.get(0).getLatitude();
            coordinates[1] = foundGeocode.get(0).getLongitude();
        } catch (IndexOutOfBoundsException e) {
            coordinates[0] = SearchRoutesActivity.UNKNOWN;
            coordinates[1] = SearchRoutesActivity.UNKNOWN;
        } catch (IllegalStateException e) {
            coordinates[0] = SearchRoutesActivity.UNKNOWN;
            coordinates[1] = SearchRoutesActivity.UNKNOWN;
        }
        return coordinates;
    }

    public static JSONArray removeJSONArrayItem(JSONArray array, int pos) {
        JSONArray newArray = new JSONArray();
        try {
            for (int i = 0; i < array.length(); i++) {
                if (i != pos) {
                    newArray.put(array.get(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newArray;
    }

    public static String getHashedUserEmail(Context ctx) {
        return new SessionManager(ctx).getUser().getHashedEmail();
    }

    public static String getUserEmail(Context ctx) {
        return new SessionManager(ctx).getUser().getEmail();
    }

    public static void deleteAllLocalFiles(Context ctx) {
        File jsonDir = new File(ctx.getFilesDir() + "/" + Const.APP_ROUTES_JSON_PATH);
        File[] routesJson = jsonDir.listFiles();

        if (routesJson != null) {
            for (File file : routesJson) {
                file.delete();
            }
        }

        File csvDir = new File(ctx.getFilesDir() + "/" + Const.APP_ROUTES_CSV_PATH);
        File[] routesCsv = csvDir.listFiles();

        if (routesCsv != null) {
            for (File file : routesCsv) {
                file.delete();
            }
        }
    }

    public static void copyFile(File src, File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static final String hashMd5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Document loadXMLFromString(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static String getHostname() {
        if (Config.getInstance(mContext).isRelease()) {
            return Config.getInstance(mContext).getReleaseHostname();
        } else {
            return Config.getInstance(mContext).getDebugHostname();
        }
    }

    public static void logD(String tag, String message) {
        if (Config.getInstance(mContext).logMessages()) {
            Log.d(tag, message);
        }
    }

    public static boolean isEncryption() {
        return Config.getInstance(mContext).isEncryption();
    }

    public static void setContext(Context ctx){
        mContext = ctx;
    }

    public static String getUrlPathStart(){
        if(isEncryption()){
            return "/otnMobileServicesEncrypted";
        } else {
            return "/otnMobileServices";
        }
    }

    public static int getResponseCode(JSONObject response){
        int responseCode = -1;
        try {
            responseCode = response.getInt("responseCode");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return responseCode;
    }
}
