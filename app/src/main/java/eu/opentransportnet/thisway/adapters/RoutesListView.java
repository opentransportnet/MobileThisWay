package eu.opentransportnet.thisway.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import eu.opentransportnet.thisway.R;
import eu.opentransportnet.thisway.models.RouteItem;

import java.util.ArrayList;

/**
 * @author Kristaps Krumins
 */
public class RoutesListView extends ArrayAdapter<RouteItem> {
    private final Context context;
    private ArrayList<RouteItem> mRoutes = new ArrayList<>();

    public RoutesListView(Context context, ArrayList<RouteItem> routes) {
        super(context, R.layout.route_listview);
        this.context = context;

        for (int i = 0; i < routes.size(); i++) {
            add(routes.get(i));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.route_listview, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        textView.setText(mRoutes.get(position).toString());

        if (mRoutes.get(position).getLocalFileName() != null) {
           RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textView.getLayoutParams();
            params.rightMargin = (int) (35f * context.getResources().getDisplayMetrics().density);
            textView.setLayoutParams(params);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            imageView.setVisibility(View.VISIBLE);
        }

        return rowView;
    }

    @Override
    public void add(RouteItem route) {
        super.add(route);
        mRoutes.add(route);
    }
}