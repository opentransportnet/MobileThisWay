package eu.opentransportnet.thisway.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.utils.Utils;
import com.library.routerecorder.RouteRecorder;

/**
 * @author Kristaps Krumins
 */
public class PinLocationActivity extends BaseActivity implements View.OnClickListener {
    private static final String LOG_TAG = "PinLocation.java";
    private WebView mWebView;
    private RouteRecorder mRouteRec;
    private double mLatitude = 200;
    private double mLongitude = 200;
    private double[] mStartAndDest = SearchRoutesActivity.getStartAndDestPoints();
    private int mRadius = SearchRoutesActivity.getRadius();
    private boolean mPinStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Initialization */
        setContentView(eu.opentransportnet.thisway.R.layout.activity_pin_location);
        setToolbarTitle(eu.opentransportnet.thisway.R.string.title_activity_pin_location);
        initToolbarBackBtn();
        mRouteRec = MainActivity.getRouteRecorder();
        Bundle extras = getIntent().getExtras();

        mPinStart = extras.getBoolean("pinStart");

        Button backButton = (Button) findViewById(eu.opentransportnet.thisway.R.id.back_button);
        backButton.setText(eu.opentransportnet.thisway.R.string.icon_cancel);

        Button okButton = (Button) findViewById(eu.opentransportnet.thisway.R.id.ok);
        okButton.setOnClickListener(this);

        mWebView = (WebView) findViewById(eu.opentransportnet.thisway.R.id.web_view);
        WebSettings webSettings = mWebView.getSettings();
        // Enable Javascript
        webSettings.setJavaScriptEnabled(true);
        // Sets whether JavaScript running in the sContext of a file scheme
        // URL should be allowed to access content from any origin
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setCacheMode(webSettings.LOAD_CACHE_ELSE_NETWORK);

        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.setWebChromeClient(new WebChromeClient() {
            //Enable console.log() from JavaScript
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Utils.logD(LOG_TAG, cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        mWebView.addJavascriptInterface(this, "Activity");
        mWebView.loadUrl("file:///android_asset/www/pinLocation.html");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.thisway.R.id.back_button:
                finish();
                break;
            case eu.opentransportnet.thisway.R.id.ok:
                if (mLatitude != 200 && mLongitude != 200) {
                    Intent data = new Intent();
                    data.putExtra("latitude", mLatitude);
                    data.putExtra("longitude", mLongitude);
                    setResult(Activity.RESULT_OK, data);
                }

                finish();
                break;
        }
    }

    @JavascriptInterface
    public double getLatitude() {
        return mRouteRec.getLatitude();
    }

    @JavascriptInterface
    public double getLongitude() {
        return mRouteRec.getLongitude();
    }

    @JavascriptInterface
    public void setCoordinates(double lat, double lng) {
        mLatitude = lat;
        mLongitude = lng;
    }

    @JavascriptInterface
    public double getRadius() {
        return mRadius;
    }

    @JavascriptInterface
    public double getStartLatitude() {
        return mStartAndDest[0];
    }

    @JavascriptInterface
    public double getStartLongitude() {
        return mStartAndDest[1];
    }

    @JavascriptInterface
    public double getDestLatitude() {
        return mStartAndDest[2];
    }

    @JavascriptInterface
    public double getDestLongitude() {
        return mStartAndDest[3];
    }

    @JavascriptInterface
    public boolean isPinStart() {
        return mPinStart;
    }

}
