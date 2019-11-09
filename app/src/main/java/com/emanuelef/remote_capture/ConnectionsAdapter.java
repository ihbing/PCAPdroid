package com.emanuelef.remote_capture;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class ConnectionsAdapter extends BaseAdapter {
    private static final String TAG = "ConnectionsAdapter";
    private MainActivity mActivity;
    private ArrayList<ConnDescriptor> mItems;
    private Drawable mUnknownIcon;

    ConnectionsAdapter(MainActivity context) {
        mActivity = context;
        mItems = new ArrayList<>();
        mUnknownIcon = mActivity.getResources().getDrawable(android.R.drawable.ic_menu_help);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ConnDescriptor conn = getItem(position);

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            convertView = inflater.inflate(R.layout.connection_item, parent, false);
        }

        assert conn != null;
        ImageView icon = convertView.findViewById(R.id.icon);
        TextView remote = convertView.findViewById(R.id.remote);
        TextView traffic = convertView.findViewById(R.id.traffic);
        AppDescriptor app = mActivity.findAppByUid(conn.uid);
        Drawable appIcon;

        if(app != null)
            appIcon = Objects.requireNonNull(app.getIcon().getConstantState()).newDrawable();
        else
            appIcon = mUnknownIcon;

        icon.setImageDrawable(appIcon);

        remote.setText(String.format(mActivity.getResources().getString(R.string.ip_and_port),
                conn.dst_ip, conn.dst_port));
        traffic.setText(Utils.formatBytes(conn.sent_bytes + conn.rcvd_bytes));

        return(convertView);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public ConnDescriptor getItem(int pos) {
        /* Prevent indexOutOfBounds exception in updateView() */
        if((pos < 0) || (pos >= getCount()))
            return null;

        return mItems.get(pos);
    }

    void updateConnections(ConnDescriptor[] connections) {
        /* Sort connections by ascending ID */
        Arrays.sort(connections, new Comparator<ConnDescriptor>() {
            @Override
            public int compare(ConnDescriptor connDescriptor, ConnDescriptor t1) {
                return Integer.compare(connDescriptor.incr_id, t1.incr_id);
            }
        });

        /* NOTE: we could just replace all the connections objects instead of the merge code
         * below. However, the code below might be useful in the future to implement more complex
         * status change/cleanup. */
        int adapter_pos = 0;

        for (ConnDescriptor eval_conn : connections) {
            ConnDescriptor adapter_conn = getItem(adapter_pos);

            /* Remove the closed connections */
            while ((adapter_conn != null) && (eval_conn.incr_id > adapter_conn.incr_id)) {
                mItems.remove(adapter_pos);
                adapter_conn = getItem(adapter_pos);
            }

            if (adapter_conn == null)
                mItems.add(eval_conn);
            else {
                if (eval_conn.incr_id == adapter_conn.incr_id) {
                    /* Update data */
                    mItems.set(adapter_pos, eval_conn);
                } else {
                    Log.e(TAG, "Logic error: missing item #" + eval_conn.incr_id +
                            " (adapter item: #" + adapter_conn.incr_id + ")");
                }
            }

            adapter_pos++;
        }

        notifyDataSetChanged();
    }

    void clear() {
        mItems.clear();
    }
}