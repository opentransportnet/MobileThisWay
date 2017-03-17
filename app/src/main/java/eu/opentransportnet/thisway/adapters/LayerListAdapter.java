package eu.opentransportnet.thisway.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import eu.opentransportnet.thisway.activities.MainActivity;
import eu.opentransportnet.thisway.models.WmsLayer;
import com.library.routerecorder.RouteRecorder;

import java.util.ArrayList;

/**
 * @author Kristaps Krumins
 */
public class LayerListAdapter extends ArrayAdapter<WmsLayer> {
    private ArrayList<WmsLayer> mLayerList;
    private Activity mContext;

    public LayerListAdapter(Activity context, int textViewResourceId,
                            ArrayList<WmsLayer> layerList) {
        super(context, textViewResourceId, layerList);

        mContext = context;
        this.mLayerList = new ArrayList<WmsLayer>();
        this.mLayerList.addAll(layerList);
    }

    private class ViewHolder {
        TextView title;
        CheckBox checkBox;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        // reuse views
        if (convertView == null) {
            LayoutInflater vi = mContext.getLayoutInflater();
            convertView = vi.inflate(eu.opentransportnet.thisway.R.layout.listview_layers, null);

            // configure view holder
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(eu.opentransportnet.thisway.R.id.title);
            holder.checkBox = (CheckBox) convertView.findViewById(eu.opentransportnet.thisway.R.id.check_box);
            convertView.setTag(holder);

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v;
                    WmsLayer layer = (WmsLayer) cb.getTag();
                    boolean isSelected = cb.isChecked();
                    layer.setSelected(isSelected);
                    RouteRecorder rr = MainActivity.getRouteRecorder();

                    if (isSelected == false) {
                        rr.loadUrl("javascript:removeWmsLayer(" + position + ")");
                    } else {
                        rr.loadUrl("javascript:addWmsLayer(" + position + ",'"
                                + layer.getWmsUrl() + "','" + layer.getName() + "')");
                    }
                }
            });
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // fill data
        WmsLayer layer = mLayerList.get(position);
        holder.title.setText(layer.getTitle());
        holder.checkBox.setChecked(layer.isSelected());
        holder.checkBox.setTag(layer);

        return convertView;
    }

    @Override
    public void add(WmsLayer layer) {
        mLayerList.add(layer);
        super.add(layer);
    }

    @Override
    public void clear() {
        super.clear();
        mLayerList.clear();
    }
}
