package org.mistercyb.wifiinfo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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


public class CurrentConnectionFragment extends Fragment implements WifiEventReceiver{

    private FragmentActivity fActivity;
    private TextView ssidTextView;
    private TextView ipTextView;
    private TextView macTextView;
    private TextView gatewayTextView;
    private TextView netmaskTextView;
    private TextView dnsTextView;
    private TextView linkspeedTextView;
    private Button copyButton;
    public WifiHelper wifiHelper;

    public CurrentConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fActivity = super.getActivity();
        // Inflate the layout for this fragment
        RelativeLayout fLayout = (RelativeLayout) inflater.inflate(R.layout.fragment_current_connection, container, false);
        ssidTextView = (TextView) fLayout.findViewById(R.id.ssid);
        ipTextView = (TextView) fLayout.findViewById(R.id.address);
        macTextView = (TextView) fLayout.findViewById(R.id.hardware);
        gatewayTextView = (TextView) fLayout.findViewById(R.id.gateway);
        netmaskTextView = (TextView) fLayout.findViewById(R.id.netmask);
        dnsTextView = (TextView) fLayout.findViewById(R.id.dns);
        linkspeedTextView = (TextView) fLayout.findViewById(R.id.linkspeed);
        copyButton = (Button) fLayout.findViewById(R.id.copy_button);
        copyButton.setEnabled(false);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyInfo();
            }
        });
        loadConnectionInfo();

        return fLayout;
    }


    private void loadConnectionInfo()
    {
        if (wifiHelper.isWifiConnected()) {
            ssidTextView.setText(this.getSSID());
            ipTextView.setText(wifiHelper.getCurrentStringIP());
            macTextView.setText(wifiHelper.getCurrentStringMacAddress());
            gatewayTextView.setText(wifiHelper.getStringNetGateway());
            netmaskTextView.setText(wifiHelper.getNetworkMaskString());
            String dnss = this.getDNS();
            dnsTextView.setText(dnss);
            String linkspeed = getResources().getString(R.string.linkspeed_format, wifiHelper.getCurrentLinkSpeed());
            linkspeedTextView.setText(linkspeed);
            copyButton.setEnabled(true);
        } else {
            ssidTextView.setText(R.string.not_connected);
            ipTextView.setText("--");
            macTextView.setText("--");
            gatewayTextView.setText("--");
            netmaskTextView.setText("--");
            dnsTextView.setText("--");
            linkspeedTextView.setText("--");
            copyButton.setEnabled(false);
        }
    }

    private String getDNS()
    {
        StringBuilder dnss = new StringBuilder();
        for (String dns : wifiHelper.getStringDNSs()) {
            dnss.append(dns);
            dnss.append(", ");
        }
        dnss.delete(dnss.length() - 2, dnss.length() - 1);
        return dnss.toString();
    }

    private String getSSID()
    {
        String ssid;
        if (!TextUtils.isEmpty(wifiHelper.getCurrentSSID())) {
            ssid = wifiHelper.getCurrentSSID();
        } else {
            ssid = "(hidden)";
        }
        return ssid;
    }

    private void copyInfo()
    {
        ClipboardManager clipboard = (ClipboardManager) fActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.current_connection), getDescription());
        clipboard.setPrimaryClip(clip);

        // Show Alert
        Toast.makeText(fActivity.getApplicationContext(),
                getString(R.string.current_conn_info_copied) , Toast.LENGTH_LONG)
                .show();
    }

    private String getDescription()
    {
        StringBuilder description = new StringBuilder();
        String linkspeed = getResources().getString(R.string.linkspeed_format, wifiHelper.getCurrentLinkSpeed());
        description.append(getString(R.string.network_copy)).append(this.getSSID()).append("\n").
                append(getString(R.string.ip_copy)).append(wifiHelper.getCurrentStringIP()).append("\n").
                append(getString(R.string.hardware_copy)).append(wifiHelper.getCurrentStringMacAddress()).append("\n").
                append(getString(R.string.gateway_copy)).append(wifiHelper.getStringNetGateway()).append("\n").
                append(getString(R.string.netmask_copy)).append(wifiHelper.getNetworkMaskString()).append("\n").
                append(getString(R.string.dns_copy)).append(this.getDNS()).append("\n").
                append(getString(R.string.linkspeed_copy)).append(linkspeed);
        return description.toString();

    }

    public void permissionGranted()
    {
        loadConnectionInfo();
    }
    public void wifiChangedState(boolean activated)
    {
        loadConnectionInfo();
    }

}
