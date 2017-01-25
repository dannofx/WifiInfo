package org.mistercyb.wifiinfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

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


public class DiscoverDevicesFragment extends Fragment implements WifiEventReceiver{
    public  WifiHelper wifiHelper;
    private HostsAdapter listAdapter;
    private RoundCornerProgressBar progressBar;
    private RelativeLayout progressBarContainer;
    private Menu menuBar;
    private RetrieveHostsTask discoveryTask;
    private boolean automaticStart;

    public DiscoverDevicesFragment() {
        automaticStart = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (automaticStart) {
            startHostDiscovery();
            automaticStart = false;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        LinearLayout fLayout = (LinearLayout) inflater.inflate(R.layout.fragment_hosts, container, false);
        final Activity fActivity = this.getActivity();
        // Get ListView object from xml
        final ListView listView = (ListView) fLayout.findViewById(R.id.list);
        TextView emptyListView = (TextView) fLayout.findViewById(R.id.empty);
        listView.setEmptyView(emptyListView);

        if (listAdapter == null) {
            listAdapter = new HostsAdapter(fActivity);
        }

        // Assign adapter to ListView
        listView.setAdapter(listAdapter);

        // ListView Item Click Listener
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                WifiAddress wifiAddress = (WifiAddress) listView.getItemAtPosition(position);
                showDialogForAddress(fActivity, wifiAddress);
                return true;

            }


        });

        progressBar = (RoundCornerProgressBar) fLayout.findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBarContainer = (RelativeLayout) fLayout.findViewById(R.id.progress_bar_container);
        setHasOptionsMenu(true);

        updateTaskControlsConfiguration();

        return fLayout;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menuBar = menu;
        inflater.inflate(R.menu.discovery_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        menuBar.findItem(R.id.hosts_action).setIcon(R.drawable.barstop_icon);
        updateTaskControlsConfiguration();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.hosts_action)
        {
            if (discoveryTask != null) {
                this.cancelHostDiscovery();
            } else {
                this.restartDiscovery();
            }
        }
        return true;
    }

    private void showDialogForAddress(final Context context, final WifiAddress wifiAddress)
    {

        final AlertDialog.Builder alertDialog = new  AlertDialog.Builder(context);
        alertDialog.setTitle(getString(R.string.host_info));
        alertDialog.setMessage(wifiAddress.toString());
        alertDialog.setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               // Dismiss
            } });
        alertDialog.setPositiveButton(getString(R.string.copy), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.host_info), wifiAddress.toString());
                clipboard.setPrimaryClip(clip);

                // Show Alert
                Toast.makeText(context.getApplicationContext(),
                        getString(R.string.info_copied) , Toast.LENGTH_LONG)
                        .show();
            } });

        alertDialog.show();
    }

    private void addHostsToAdapter(ArrayList<WifiAddress> hosts)
    {
        for (WifiAddress host : hosts)
        {
            listAdapter.add(host);
        }

    }

    private void startHostDiscovery()
    {
        if (!wifiHelper.arePermissionsAvailable())
        {
            Toast.makeText(getActivity().getApplicationContext(),
                    "No permissions  available" , Toast.LENGTH_LONG)
                    .show();
            return;
        }
        if (!wifiHelper.isWifiConnected()) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "No connection available" , Toast.LENGTH_LONG)
                    .show();
            return;
        }

        startDiscoveryTask();
        updateTaskControlsConfiguration();

    }

    private void cancelHostDiscovery()
    {
        stopDiscoveryTask();
        finishDiscovery();
    }

    private void restartDiscovery()
    {
        listAdapter.clear();
        stopDiscoveryTask();
        startHostDiscovery();
    }

    private void startDiscoveryTask()
    {
        if (discoveryTask == null) {
            discoveryTask = new RetrieveHostsTask();
            discoveryTask.execute();
        }
    }

    private void stopDiscoveryTask()
    {
        if (discoveryTask != null ) {
            if (discoveryTask.getStatus() != AsyncTask.Status.FINISHED) {
                discoveryTask.cancel(true);
            }
            discoveryTask = null;
        }
    }

    private void updateTaskControlsConfiguration()
    {
        boolean barVisibility;
        int progressBarVisibility;

        if (discoveryTask != null)
        {
            barVisibility = true;
            progressBarVisibility = View.VISIBLE;
        }else {
            barVisibility = false;
            progressBarVisibility = View.GONE;
        }

        if (menuBar != null) {
            MenuItem actionItem = menuBar.findItem(R.id.hosts_action);
            if (actionItem != null) {
                if (barVisibility) {
                    actionItem.setIcon(R.drawable.barstop_icon);
                } else {
                    actionItem.setIcon(R.drawable.barrefresh_icon);
                }
            }
        }
        if (progressBarContainer != null)
            progressBarContainer.setVisibility(progressBarVisibility);
    }

    private void setProgressOnDiscovery(int progress)
    {
        progressBar.setProgress(progress);
    }

    private void finishDiscovery()
    {
        discoveryTask = null;
        updateTaskControlsConfiguration();

    }

    public void permissionGranted()
    {

    }
    public void wifiChangedState(boolean activated)
    {
        if (!activated)
            cancelHostDiscovery();
    }

    class RetrieveHostsTask extends AsyncTask<Void, Integer, ArrayList>
    {

        ArrayList<WifiAddress>  unreported;
        @Override
        protected ArrayList doInBackground(Void... params) {

            ArrayList<WifiAddress> hosts = new ArrayList<>();
            if (!wifiHelper.isWifiConnected())
            {
               return hosts;
            }
            int segmentLength = wifiHelper.getNetSegmentLength();
            int firstNode = wifiHelper.getFirstNodeAddress();
            unreported = new ArrayList<>();

            for (int i = 0; i < segmentLength; i++)
            {
                if(isCancelled())
                    break;

                int address = wifiHelper.incrementAddress(firstNode, i);
                try {
                    InetAddress inetAddress = InetAddress.getByName(WifiHelper.ipAddressToString(address));
                    if (inetAddress.isReachable(1000)) {
                        Log.d(LogConstants.DISCOVERY, inetAddress.getHostName());
                        WifiAddress wifiAddress = new WifiAddress(inetAddress);
                        hosts.add(wifiAddress);
                        unreported.add(wifiAddress);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                publishProgress((int) ((i / (float) segmentLength) * 100));

            }
            return hosts;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            setProgressOnDiscovery(progress[0]);

            if (unreported.size() > 0)
            {
                addHostsToAdapter(unreported);
                unreported.clear();
            }


        }

        @Override
        protected void onPostExecute(ArrayList hosts) {
            finishDiscovery();
        }
    }

    class HostsAdapter extends ArrayAdapter<WifiAddress> {


        public HostsAdapter(Context context) {
            super(context, R.layout.discovered_host);
        }

        @NonNull
        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.discovered_host, null);
            }

            WifiAddress wifiAddress = getItem(position);
            assert wifiAddress != null;
            InetAddress address = wifiAddress.getAddress();

            if (address != null) {
                TextView hostnameView = (TextView) v.findViewById(R.id.hostname);
                TextView macTextView = (TextView) v.findViewById(R.id.mac_address);
                TextView ipTextView = (TextView) v.findViewById(R.id.ip_address);
                ImageView hostImageView = (ImageView) v.findViewById(R.id.host_indicator);
                hostnameView.setText(address.getHostName());
                String mac = wifiAddress.getMacAddress();
                String macText = getResources().getString(R.string.mac_host_label, mac);
                macTextView.setText(macText);
                String ipText = getResources().getString(R.string.ip_host_label, address.getHostAddress());
                ipTextView.setText(ipText);
                boolean sameAddress = wifiHelper.getCurrentStringIP().equals(address.getHostAddress());
                int resource = sameAddress? R.drawable.hostactive_icon : R.drawable.hostinactive_icon;
                hostImageView.setImageResource(resource);

            }

            return v;
        }

    }

    private class WifiAddress
    {
        private final InetAddress address;
        private String mac;

        public WifiAddress(InetAddress address)
        {
            this.address = address;
        }

        public InetAddress getAddress()
        {
            return this.address;
        }

        public String getMacAddress()
        {
            if (mac == null)
            {
                mac = WifiHelper.getMacAddress(this.address);
            }

            return mac;
        }

        @Override
        public String toString()
        {
            return  getString(R.string.hostname_copy) +
                    this.getAddress().getHostName() + "\n" +
                    getString(R.string.hardware_copy) +
                    this.getMacAddress() + "\n" +
                    getString(R.string.ip_copy) +
                    this.getAddress().getHostAddress();
        }
    }

}


