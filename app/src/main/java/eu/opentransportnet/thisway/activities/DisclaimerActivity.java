package eu.opentransportnet.thisway.activities;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.opentransportnet.thisway.models.BaseActivity;
import eu.opentransportnet.thisway.models.User;
import eu.opentransportnet.thisway.utils.ObservableWebView;
import eu.opentransportnet.thisway.utils.SessionManager;

import java.util.Locale;

/**
 *
 * @author Ilmars Svilsts
 */

public class DisclaimerActivity extends BaseActivity implements View.OnClickListener {
    private boolean agree = false;
    private SessionManager mSessionManager;
    private ObservableWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(eu.opentransportnet.thisway.R.layout.activity_disclaimer);
        findViewById(eu.opentransportnet.thisway.R.id.scroll_button).setOnClickListener(this);
        initToolbar();
        Typeface tf = getTypeFace();
        Button closeButton = (Button) findViewById(eu.opentransportnet.thisway.R.id.back_button);
        closeButton.setVisibility(View.VISIBLE);
        closeButton.setTypeface(tf);
        closeButton.setOnClickListener(this);
        mSessionManager = new SessionManager(this);

        User user = mSessionManager.getUser();

        int vers = 0;
        try {

            if (getString(eu.opentransportnet.thisway.R.string.svn_version).equals("null")) {
            } else {
                vers = Integer.parseInt(getString(eu.opentransportnet.thisway.R.string.svn_version));
            }
        } catch (Exception e) {
        }

        mWebView = (ObservableWebView) findViewById(eu.opentransportnet.thisway.R.id.web_view);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        if (user.getDisclaimer() == true && user.getDisclaimerV() >= vers) {
            RelativeLayout dis = (RelativeLayout) findViewById(eu.opentransportnet.thisway.R.id.dis_frame);
            dis.setVisibility(View.GONE);

            //Remove bottom margin
            ViewGroup.MarginLayoutParams webViewParms = (ViewGroup.MarginLayoutParams) mWebView
                    .getLayoutParams();
            webViewParms.bottomMargin = 0;
            mWebView.setLayoutParams(webViewParms);
        }

        mWebView.setOnScrollChangedCallback(new ObservableWebView.OnScrollChangedCallback() {
            @Override
            public void onScroll(int l, int t) {
                int tek = (int) Math.floor(mWebView.getContentHeight() * mWebView.getScale());

                if (tek - mWebView.getScrollY() == mWebView.getHeight()) {
                    //Web view scrolled to end
                    FloatingActionButton image=(FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.scroll_button);
                    // image.setImageDrawable(ContextCompat.getDrawable(sContext, R.drawable.u7));
                    image.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.ok)));
                    TextView text=(TextView) findViewById(eu.opentransportnet.thisway.R.id.scroll_text);
                    text.setText(eu.opentransportnet.thisway.R.string.agree);
                    agree= true;
                }
            }
        });
        if(Locale.getDefault().getLanguage().equals("fr")){mWebView.loadUrl("file:///android_asset/frdisclaimerText.html");}
        else{ mWebView.loadUrl("file:///android_asset/disclaimerText.html");}

        FloatingActionButton image=(FloatingActionButton) findViewById(eu.opentransportnet.thisway.R.id.scroll_button);
        image.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(eu.opentransportnet.thisway.R.color.stop)));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case eu.opentransportnet.thisway.R.id.back_button:
                finish();
                break;
            case eu.opentransportnet.thisway.R.id.scroll_button:
                if (agree == false) {
                    int tek = (int) Math.floor(mWebView.getContentHeight() * mWebView.getScale());
                    mWebView.scrollTo(0, tek - mWebView.getHeight());
                } else {
                    int vers = 0;
                    try {
                        if (getString(eu.opentransportnet.thisway.R.string.svn_version).equals("null")) {
                        } else {
                            vers = Integer.parseInt(getString(eu.opentransportnet.thisway.R.string.svn_version));
                        }
                    } catch (Exception e) {
                    }
                    mSessionManager.saveDisclaimer(true, vers);
                    finish();
                }
                break;
        }
    }

    /**
     * Initiate toolbar
     */
    private void initToolbar() {
        setToolbarTitle(eu.opentransportnet.thisway.R.string.title_activity_disclaimer);
    }
}


