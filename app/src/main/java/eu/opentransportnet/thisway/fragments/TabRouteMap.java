package eu.opentransportnet.thisway.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import eu.opentransportnet.thisway.activities.MainActivity;
import eu.opentransportnet.thisway.activities.SearchRoutesActivity;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.RouteRecorder;

/**
 * @author Kristaps Krumins
 */
public class TabRouteMap extends Fragment {

    private Activity mActivity;
    private String mEncodedKml = null;
    private RouteRecorder mRouteRec;
    private String mKmlFilePath = null;

    private static final String LOG_TAG = "TabRouteMap.java";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        mRouteRec = MainActivity.getRouteRecorder();
        View v = inflater.inflate(eu.opentransportnet.thisway.R.layout.tab_route_map, container, false);
        Bundle arg = getArguments();
        int addMyMarker = 0;

        if (arg != null) {
            mEncodedKml = arg.getString("encodedKml");
            if (mEncodedKml == null) {
                mKmlFilePath = arg.getString("kmlPath");
            }
            addMyMarker = arg.getInt("addMyMarker", 0);
        }

        final WebView webView = (WebView) v.findViewById(eu.opentransportnet.thisway.R.id.web_view);
        WebSettings webSettings = webView.getSettings();
        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);
        // Sets whether JavaScript running in the sContext of a file scheme
        // URL should be allowed to access content from any origin
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setCacheMode(webSettings.LOAD_CACHE_ELSE_NETWORK);

        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        // Force links and redirects to open in the WebView instead of in a browser
        webView.setWebChromeClient(new WebChromeClient() {
            //Enable console.log() from JavaScript
            public boolean onConsoleMessage(ConsoleMessage cm) {

                Utils.logD(LOG_TAG, cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        webView.addJavascriptInterface(this, "Activity");
        webView.loadUrl("file:///android_asset/www/route.html");

        if (addMyMarker == 1) {
            // Wait till page gets loaded
            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    double lat = mRouteRec.getLatitude();
                    double lng = mRouteRec.getLongitude();
                    webView.loadUrl("javascript:addMyMarker(" + lat + "," + lng + ")");
                    double[] pCoords = SearchRoutesActivity.getStartAndDestPoints();
                    int radius = SearchRoutesActivity.getRadius();
                    webView.loadUrl("javascript:addStartAndDestPoints(" + pCoords[0] + "," +
                            pCoords[1] + "," + pCoords[2] + "," + pCoords[3] + "," + radius + ")");
                }
            });
        }

        return v;
    }

    @JavascriptInterface
    public String getRouteKmlFile() {
        if (mEncodedKml != null) {
            return Utils.getRouteKmlFile(mActivity, mEncodedKml);
        } else {
            return mKmlFilePath;
        }
    }

    @JavascriptInterface
    public String getIconNames() {
        return mRouteRec.getIconNames();
    }

    @JavascriptInterface
    public void poiClicked(int poiId, String description) {
        Utils.logD(LOG_TAG, "POI clicked. ID:" + poiId + " Desc:" + description);
    }
}