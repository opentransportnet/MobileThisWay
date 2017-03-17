package eu.opentransportnet.thisway.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.activities.RouteInfoActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Kristaps Krumins
 */
public class TabListOfRoutes extends Fragment {

    private Activity mActivity;
    private JSONArray routesJson;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mActivity = getActivity();
        View v = inflater.inflate(R.layout.tab_list_of_routes, container, false);

        if(getArguments().getBoolean("onlyMyRoutes")){
//            routesJson = Download.getMyRoutes(mActivity);
        } else {
//            routesJson = Download.getAllRoutes(mActivity);
        }

        String routes[] = new String[routesJson.length()];

        for (int i = 0; i < routesJson.length(); i++) {
            try {
                JSONObject route = routesJson.getJSONObject(i);
                routes[i] = route.getString("RouteName");
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
        }

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(
                        getActivity(),
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        routes);

        ListView listView = (ListView) v.findViewById(R.id.list_view);
        listView.setAdapter(itemsAdapter);

        listView.setOnItemClickListener(new AdapterView
                .OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Intent routeInfo = new Intent(getActivity(), RouteInfoActivity.class);
                JSONObject route;
                int routeId = -1;
                try {
                    route = routesJson.getJSONObject(position);
                    routeId = route.getInt("RouteId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                routeInfo.putExtra("routeId", routeId);
                startActivity(routeInfo);
            }
        });
        return v;
    }
}
