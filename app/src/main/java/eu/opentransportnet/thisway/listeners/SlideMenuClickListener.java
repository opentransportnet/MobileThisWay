package eu.opentransportnet.thisway.listeners;

import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import eu.opentransportnet.thisway.activities.ChangeLangActivity;
import eu.opentransportnet.thisway.activities.DisclaimerActivity;
import eu.opentransportnet.thisway.activities.MainActivity;
import eu.opentransportnet.thisway.activities.RouteListActivity;
import eu.opentransportnet.thisway.utils.SessionManager;

/**
 * @author Ilmars Svilsts
 */
public class SlideMenuClickListener implements ListView.OnItemClickListener {
    private DrawerLayout drawer;
    MainActivity activity;

    public SlideMenuClickListener(DrawerLayout drawer, MainActivity activity) {
        this.drawer = drawer;
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display view for selected nav drawer item
        switch (position) {
            case 0:
                drawer.closeDrawers();
                Intent changeLang = new Intent(activity, ChangeLangActivity.class);
                activity.startActivity(changeLang);
                break;
            case 1:
                drawer.closeDrawers();
                Intent routeList = new Intent(activity, RouteListActivity.class);
                routeList.putExtra("onlyMyRoutes", true);
                routeList.putExtra("filterByPoints", false);
                routeList.putExtra("showAddressBox", false);
                activity.startActivity(routeList);
                break;
            case 2:
                drawer.closeDrawers();
                Intent dis = new Intent(activity, DisclaimerActivity.class);
                activity.startActivity(dis);
                break;
            case 3:
                drawer.closeDrawers();
                activity.deleteUser();
                break;
            case 4:
                drawer.closeDrawers();
                new SessionManager(activity).logoutUser();
                break;
        }
    }
}