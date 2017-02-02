package org.mistercyb.wifiinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 *
 * Created by danno on 10/20/16.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */


public class ScannerFragment extends Fragment implements WifiEventReceiver {

    private static final String IS_REFRESHING = "isRefreshing";
    private static final String MULTIPLE_GROUP_TAG = "multiple";
    private static final String SINGLE_GROUP_TAG = "single";

    private FragmentActivity fActivity;
    private TextView currentConnectionLabel;
    private TextView currentConnection;
    private TextView currentHW;
    private TextView currentIP;

    private TextView emptyListView;
    private AccessPointAdapter listAdapter;
    private final WifiScanReceiver wifiReceiver;
    private SwipeRefreshLayout swipeContainer;
    private String currentBSSID;
    private String currentSSID;
    private boolean automaticStart;
    private boolean isRefreshing;
    public  WifiHelper wifiHelper;
    public boolean receiverRegistered;


    public ScannerFragment()
    {
        automaticStart = true;
        isRefreshing = false;
        wifiReceiver = new WifiScanReceiver();
        receiverRegistered = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (savedInstanceState != null)
        {
            isRefreshing = savedInstanceState.getBoolean(IS_REFRESHING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout fLayout = (LinearLayout) inflater.inflate(R.layout.fragment_scanner, container, false);
        fActivity = super.getActivity();
        setHasOptionsMenu(true);

        wifiReceiver.registerForScans(fActivity);
        receiverRegistered = true;

        ExpandableListView listAPs = (ExpandableListView) fLayout.findViewById(R.id.apsList);
        if (listAdapter == null) {
            listAdapter = new AccessPointAdapter(this.fActivity);
        }

        listAPs.setAdapter(listAdapter);

        //Set empty view
        emptyListView = (TextView) fLayout.findViewById(R.id.empty_view);
        listAPs.setEmptyView(emptyListView);

        // Refresh control settings
        swipeContainer = (SwipeRefreshLayout) fLayout.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                startAccessPointDiscovery();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(R.color.blue,
                R.color.mid_blue,
                R.color.dark_blue);

        //Wifi Setup
        if (automaticStart) {
            startAccessPointDiscovery();
            automaticStart = false;
        }
        swipeContainer.setRefreshing(false);
        swipeContainer.post(new Runnable() {
            @Override public void run() {
                swipeContainer.setRefreshing(isRefreshing);
            }
        });

        final Context context = this.fActivity;
        //User interaction listeners
        listAPs.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {

                AccessPointResult apResult = (AccessPointResult) listAdapter.getGroup(groupPosition);
                if (apResult.scanResults.size() > 1)
                    return false; //If the group is expandable the click will be ignored.
                ScanResult result = apResult.getMainScanResult();
                boolean currentlyConnected = currentBSSID != null && currentBSSID.equals(result.BSSID);
                AccessPointDialog dialog = new AccessPointDialog(context, result, currentlyConnected);
                dialog.show();
                return true;
            }
        });

        listAPs.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                AccessPointResult apResult = (AccessPointResult) listAdapter.getGroup(groupPosition);
                ScanResult result = apResult.scanResults.get(childPosition);
                boolean currentlyConnected = currentBSSID != null && currentBSSID.equals(result.BSSID);
                AccessPointDialog dialog = new AccessPointDialog(context, result, currentlyConnected);
                dialog.show();
                return false;
            }
        });

        currentConnectionLabel = (TextView) fLayout.findViewById(R.id.current_connection_label);
        currentConnection = (TextView) fLayout.findViewById(R.id.current_connection);
        currentHW = (TextView) fLayout.findViewById(R.id.hw);
        currentIP = (TextView) fLayout.findViewById(R.id.ip);
        loadCurrentConnectionInfo();


        return fLayout;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ap_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_refresh:
                this.refreshAccessPointList();
                break;
            // action with ID action_settings was selected
            default:
                break;
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_REFRESHING, isRefreshing);
    }

    public BroadcastReceiver getBroadcastReceiver()
    {
        return wifiReceiver;
    }

    private void loadCurrentConnectionInfo()
    {
        currentSSID = wifiHelper.getCurrentSSID();
        String currentIPString = wifiHelper.getCurrentStringIP();
        currentBSSID = wifiHelper.getCurrentBSSID();
        if (currentBSSID != null) {
            currentConnectionLabel.setText(R.string.connected_to);
            currentConnection.setText(currentSSID);
            String currentHWText = String.format(Locale.US, getString(R.string.HW_placeholder), currentBSSID);
            currentHW.setText(currentHWText);
            String currentIPText = String.format(Locale.US, getString(R.string.ip_placeholder), currentIPString);
            currentIP.setText(currentIPText);
        }
        else {
            currentConnectionLabel.setText(R.string.not_network_connected);
            currentConnection.setText("");
            currentHW.setText("");
            currentIP.setText("");
        }
    }

    private void startAccessPointDiscovery()
    {
        if (wifiHelper.arePermissionsAvailable()) {
            boolean started = wifiHelper.startAPScan();
            if (started)
            {
                this.startRefreshViewActions();
            } else
            {
                emptyListView.setText(R.string.scan_unavailable);
            }

        }
    }


    private void refreshAccessPointList()
    {
        this.startAccessPointDiscovery();
    }

    private void startRefreshViewActions()
    {
        if (swipeContainer != null && !swipeContainer.isRefreshing())
        {
            swipeContainer.setRefreshing(true);
        }
        isRefreshing = true;
        emptyListView.setText(R.string.loading_results);
        Toast.makeText(this.fActivity, "Loading list", Toast.LENGTH_SHORT)
                .show();
    }

    private void stopRefreshViewActions()
    {
        if (swipeContainer != null && swipeContainer.isRefreshing())
        {
            swipeContainer.setRefreshing(false);
        }
        isRefreshing = false;
    }

    public void permissionGranted()
    {
        this.startAccessPointDiscovery();
    }
    public void wifiChangedState(boolean activated)
    {
        loadCurrentConnectionInfo();
        this.listAdapter.notifyDataSetChanged();
    }

    private boolean isGPSEnabled() {

        LocationManager lm = (LocationManager)fActivity.getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = false;
        try {
            enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ignored) {}

        return enabled;
    }

    private class WifiScanReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            List<ScanResult> wifiScanList;
            try {
                wifiScanList = wifiHelper.getAPScanResults();
                listAdapter.updateResults(wifiScanList);
                for (ScanResult result : wifiScanList)
                {
                    Log.d(LogConstants.NETWORK_UTILS, "Result name: " + result.SSID);
                }
            } catch (Exception e) {

                Log.d(LogConstants.NETWORK_UTILS, e.getLocalizedMessage());
            }
            stopRefreshViewActions();
        }

        public void registerForScans(Context context)
        {
            context.registerReceiver(this, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
    }

    class AccessPointResult
    {
        public final ArrayList<ScanResult> scanResults;

        public  AccessPointResult(ScanResult scanResult)
        {
            this.scanResults = new ArrayList<>();
            this.scanResults.add(scanResult);

        }

        public ScanResult getMainScanResult()
        {
            if (scanResults != null && scanResults.size() > 0)
                return scanResults.get(0);
            else
                return null;
        }

        public String getBSSID()
        {
            ScanResult result = getMainScanResult();
            if (result != null)
            {
                return result.BSSID;
            } else
            {
                return null;
            }
        }


        public void updateResult(ScanResult result)
        {
            for (int i = 0 ; i < this.scanResults.size(); i++)
            {
                ScanResult currentResult = this.scanResults.get(i);

                if (currentResult.BSSID.equals(result.BSSID))
                {
                    this.scanResults.set(i, result);
                    return;
                }
            }

            this.scanResults.add(result);
        }

    }

    class AccessPointAdapter extends BaseExpandableListAdapter {

        private final Context context;
        private final ArrayList<AccessPointResult> accessPoints;

        public AccessPointAdapter(Context context) {

            //super();
            //super(context, resource);
            this.context = context;
            this.accessPoints = new ArrayList<>();
        }

        public void updateResults(List<ScanResult> scanResults) {

            if (scanResults.size() == 0)
            {

                boolean gps_enabled = isGPSEnabled();
                if (gps_enabled) {
                    emptyListView.setText(R.string.results_not_available);
                } else {
                    emptyListView.setText(R.string.location_is_off);
                }
                return;
            }

            Collections.sort(scanResults, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult o1, ScanResult o2) {
                    return o1.SSID.compareToIgnoreCase(o2.SSID);
                }
            });

            int aIndex = 0;

            for (int i = 0 ; i < scanResults.size() ; i++)
            {
                ScanResult scanResult = scanResults.get(i);
                AccessPointResult accessPoint = null;
                int comparison = 0;

                if (this.accessPoints.size() > aIndex) {
                    accessPoint = this.accessPoints.get(aIndex);

                    comparison = scanResult.SSID.compareToIgnoreCase(accessPoint.getMainScanResult().SSID);
                }

                if (comparison < 0 || accessPoint == null) {
                    //smaller
                    AccessPointResult aResult = new AccessPointResult(scanResult);
                    this.accessPoints.add(aIndex, aResult);
                } else if (comparison > 0) {
                    //larger
                    aIndex++;
                    i--;
                } else {
                    //equals
                    accessPoint.updateResult(scanResult);
                }
            }
            this.notifyDataSetChanged();
        }

        @Override
        public int getGroupCount() {
            return this.accessPoints.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            int count = this.accessPoints.get(groupPosition).scanResults.size();
            return count > 1? count : 0;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return this.accessPoints.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return this.accessPoints.get(groupPosition).scanResults.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            AccessPointResult accessPoint = this.accessPoints.get(groupPosition);
            ScanResult scanResult = accessPoint.getMainScanResult();
            boolean currentConnection = isCurrentConnection(accessPoint);

            int children = accessPoint.scanResults.size();
            int groupResource;
            String tag;

            if ( children > 1)
            {
                groupResource = R.layout.access_point_list_multiple_item;
                tag = MULTIPLE_GROUP_TAG;
            } else
            {
                groupResource = R.layout.access_point_list_item;
                tag = SINGLE_GROUP_TAG;
            }

            if (convertView == null || !convertView.getTag().equals(tag)) {
                convertView = LayoutInflater.from(this.context).inflate(groupResource, parent, false);
                convertView.setTag(tag);
            }

            TextView nameView = (TextView)convertView.findViewById(R.id.apName);
            ImageView f5ghzView = (ImageView)convertView.findViewById(R.id.f5ghz_indicator);
            ImageView intensityImageView = (ImageView)convertView.findViewById(R.id.wifi_indicator);
            ImageView currentConnectionImageView = (ImageView)convertView.findViewById(R.id.check_indicator);

            // SSID
            String name = scanResult.SSID;
            if (name.equals(""))
                name = getString(R.string.ssid_hidden);

            nameView.setText(name);


            // 5GHz
            boolean is5GHz = WifiHelper.isFrequency5GHz(scanResult.frequency);
            int visibility = is5GHz ? View.VISIBLE : View.INVISIBLE;
            f5ghzView.setVisibility(visibility);

            // Current connection
            visibility = currentConnection ? View.VISIBLE : View.INVISIBLE;
            currentConnectionImageView.setVisibility(visibility);

            // Intensity icon
            int intensity = WifiManager.calculateSignalLevel(scanResult.level, 100);
            int resource = intensityImageResource(intensity, currentConnection);
            intensityImageView.setImageResource(resource);

            // Expandable indicator
            if (children == 1)
            {
                TextView addressView = (TextView)convertView.findViewById(R.id.apAddress);
                TextView generalInfo = (TextView)convertView.findViewById(R.id.general_info);
                RoundCornerProgressBar intensityBar = (RoundCornerProgressBar)convertView.findViewById(R.id.intensity_bar);

                // Mac address
                addressView.setText(accessPoint.getBSSID());

                // General info
                String security = WifiHelper.extractScanResultSecurity(scanResult);
                int channel = WifiHelper.convertFrequencyToChannel(scanResult.frequency);
                generalInfo.setText("CH " + channel +" | Frequency " + scanResult.frequency + " MHz | " + security);

                // Intensity bar
                intensityBar.setMax(100);
                intensityBar.setProgress(intensity);
                int color = intensityColor(intensity);
                intensityBar.setProgressColor(color);

            }
            else
            {
                ImageView expIndicator = (ImageView) convertView.findViewById( R.id.exp_indicator);
                TextView groupLabelView = (TextView)convertView.findViewById(R.id.groupLabel);
                int drawable = isExpanded? R.drawable.icon_less : R.drawable.icon_more;
                expIndicator.setImageResource(drawable);

                String groupLabelText = String.format(Locale.US, "%d access points", accessPoint.scanResults.size());
                groupLabelView.setText(groupLabelText);

            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ScanResult scanResult = this.accessPoints.get(groupPosition).scanResults.get(childPosition);

            boolean currentConnection = isCurrentConnection(scanResult);
            boolean currentSSID = isCurrentSSID(scanResult);

            if (convertView == null) {
                convertView = LayoutInflater.from(this.context).inflate(R.layout.access_point_list_subitem, parent, false);
            }

            TextView nameView = (TextView)convertView.findViewById(R.id.apName);
            TextView addressView = (TextView)convertView.findViewById(R.id.apAddress);
            TextView generalInfo = (TextView)convertView.findViewById(R.id.general_info);
            ImageView f5ghzView = (ImageView)convertView.findViewById(R.id.f5ghz_indicator);
            RoundCornerProgressBar intensityBar = (RoundCornerProgressBar)convertView.findViewById(R.id.intensity_bar);
            ImageView intensityImageView = (ImageView)convertView.findViewById(R.id.wifi_indicator);
            ImageView currentConnectionImageView = (ImageView)convertView.findViewById(R.id.check_indicator);

            String name = scanResult.SSID;
            if (name.equals(""))
                name = "(SSID hidden)";

            nameView.setText(name);
            addressView.setText(scanResult.BSSID);

            // 5GHz
            boolean is5GHz = WifiHelper.isFrequency5GHz(scanResult.frequency);
            int visibility = is5GHz ? View.VISIBLE : View.INVISIBLE;
            f5ghzView.setVisibility(visibility);

            // Intensity bar and icon
            int intensity = WifiManager.calculateSignalLevel(scanResult.level, 100);
            intensityBar.setMax(100);
            intensityBar.setProgress(intensity);
            int color = intensityColor(intensity);
            intensityBar.setProgressColor(color);
            int resource = intensityImageResource(intensity, currentSSID);
            intensityImageView.setImageResource(resource);

            // Current connection
            visibility = currentConnection ? View.VISIBLE : View.INVISIBLE;
            currentConnectionImageView.setVisibility(visibility);

            // General info
            String security = WifiHelper.extractScanResultSecurity(scanResult);
            int channel = WifiHelper.convertFrequencyToChannel(scanResult.frequency);
            generalInfo.setText("CH " + channel +" | Frequency " + scanResult.frequency + " MHz | " + security);

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        private boolean isCurrentConnection(ScanResult scanResult)
        {
            return currentBSSID != null && scanResult.BSSID.equals(currentBSSID);
        }

        private boolean isCurrentSSID(ScanResult scanResult)
        {
            return currentSSID != null && scanResult.SSID.equals(currentSSID);
        }

        private boolean isCurrentConnection(AccessPointResult accessPointResult)
        {
            for (ScanResult scanResult : accessPointResult.scanResults)
            {
                if (isCurrentConnection(scanResult))
                    return true;
            }
            return false;
        }

        //Util methods

        private int intensityColor(int intensity) {
            int phase = intensity / 33;
            int color;
            switch (phase) {
                case 0:
                    color = ContextCompat.getColor(context, R.color.red);
                    break;
                case 1:
                    color = ContextCompat.getColor(context, R.color.yellow);
                    break;
                default:
                    color = ContextCompat.getColor(context, R.color.green);
                    break;
            }
            return color;
        }

        private int intensityImageResource(int intensity, boolean current) {
            int phase = intensity / 33;
            int resource;
            switch (phase) {
                case 0:
                    resource = current? R.drawable.wifiactive1_icon : R.drawable.wifiinactive1_icon;
                    break;
                case 1:
                    resource = current? R.drawable.wifiactive2_icon : R.drawable.wifiinactive2_icon;
                    break;
                default:
                    resource = current? R.drawable.wifiactive3_icon : R.drawable.wifiinactive3_icon;
                    break;
            }
            return resource;
        }

    }
}
