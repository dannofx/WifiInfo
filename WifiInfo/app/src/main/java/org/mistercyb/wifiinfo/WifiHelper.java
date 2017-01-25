package org.mistercyb.wifiinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 *
 * Created by danno on 10/19/16.
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
public class WifiHelper {

    private static final int PERMISSIONS_REQUEST_CODE = 1;
    private final ArrayList<WifiEventReceiver> eventReceivers;
    private final Activity activity;
    private final WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private boolean isWifiConnected;

    public WifiHelper(Activity activity)
    {
        eventReceivers = new ArrayList<>();
        this.activity = activity;
        this.wifiManager = (WifiManager) this.activity.getSystemService(Context.WIFI_SERVICE);
        loadConnectionInfo();
    }

    public void registerWifiStatusChange() {

        boolean previousState = isWifiConnected;
        loadConnectionInfo();
        if (previousState == isWifiConnected)
            return;
        for (WifiEventReceiver receiver : eventReceivers)
        {
            receiver.wifiChangedState(isWifiConnected());
        }
    }

    public void addEventReceiver(WifiEventReceiver receiver)
    {
        if (!eventReceivers.contains(receiver))
        {
            eventReceivers.add(receiver);
        }
    }

//    public void removeEventReceiver(WifiEventReceiver receiver)
//    {
//        if (eventReceivers.contains(receiver))
//        {
//            eventReceivers.remove(receiver);
//        }
//    }

    private void loadConnectionInfo()
    {
        ConnectivityManager connectivityManager = ((ConnectivityManager) activity.getSystemService
                (Context.CONNECTIVITY_SERVICE));
        isWifiConnected = false;
        Network[] networks = connectivityManager.getAllNetworks();
        if (networks == null) {
            isWifiConnected = false;
        } else {
            for (Network network : networks) {
                NetworkInfo info = connectivityManager.getNetworkInfo(network);
                if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    if (info.isAvailable() && info.isConnected()) {
                        isWifiConnected = true;
                        break;
                    }
                }
            }
        }

        if (isWifiConnected) {
            wifiInfo = wifiManager.getConnectionInfo();
        } else {
            wifiInfo = null;
        }
    }

    public boolean startAPScan()
    {
        if (this.wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
            return true;
        } else
        {
            Toast.makeText(this.activity, "Wifi scan is not available", Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
    }

    public List<ScanResult> getAPScanResults()
    {
        return this.wifiManager.getScanResults();
    }

    // Properties of the current connection

    public String getCurrentBSSID() {

        String bssid = null;
        if (wifiInfo != null)
        {
            if (!TextUtils.isEmpty(wifiInfo.getSSID())) {
                bssid = wifiInfo.getBSSID();
            }
        }
        return bssid;
    }

    public String getCurrentSSID() {

        if (wifiInfo == null)
        {
            return null;
        } else
        {
            String ssid = wifiInfo.getSSID();
            if (ssid.charAt(0) == '"' && ssid.charAt(ssid.length() - 1) == '"')
            {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            return ssid;
        }
    }

    private int getCurrentIP() {
        DhcpInfo dhcp = wifiManager.getDhcpInfo();
        return dhcp.ipAddress;
    }

    public String getCurrentStringIP() {
        return ipAddressToString(this.getCurrentIP());
    }

    private int getNetGateway() {

        DhcpInfo dhcp = wifiManager.getDhcpInfo();
        return dhcp.gateway;
    }

    public String getStringNetGateway()
    {
        int gateway = this.getNetGateway();
        return ipAddressToString(gateway);
    }

    public String[] getStringDNSs() {

        DhcpInfo dhcp = wifiManager.getDhcpInfo();
        return new String[]{ipAddressToString(dhcp.dns1), ipAddressToString(dhcp.dns2)};
    }


    public String getCurrentStringMacAddress()
    {
        try {
            int currentIp = this.getCurrentIP();
            byte[] ipBytes = this.extractBytes(currentIp);
            InetAddress address = InetAddress.getByAddress(ipBytes);
            return WifiHelper.getLocalMacAddress(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

    public int getCurrentLinkSpeed()
    {
        return this.wifiInfo.getLinkSpeed();
    }

    private byte[] extractBytes(int ipAddress)
    {
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((ipAddress >> k * 8) & 0xFF);
        }

        return quads;
    }

    public int incrementAddress(int address, int increment)
    {
        long normalized = invertSegments(address);
        normalized += increment;
        return (int)invertSegments(normalized);

    }

    private long invertSegments(long ip)
    {
        int segments = 4;
        long result = 0;
        for (int i = 0 ; i < segments ; i++)
        {
            int ca = i*8;
            long segment = ((ip & (0xff << ca)) >> ca);
            ca = (segments - i -1) * 8;
            long value = (segment << ca);
            result += value;
        }

        return result;
    }


    private int getNetworkPrefixLength()
    {
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        try {
            InetAddress inetAddress = InetAddress.getByAddress(extractBytes(dhcpInfo.ipAddress));
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {

                if (address.getBroadcast() != null) {
                    return address.getNetworkPrefixLength();
                }
            }
        } catch (IOException e) {
            Log.e(LogConstants.NETWORK_UTILS, e.toString());
        }

        return -1;
    }

    public  int getNetSegmentLength() {
        int prefixLength = getNetworkPrefixLength();

        if (prefixLength != -1)
        {
            int len = 32 - prefixLength;
            len = 1 << len;
            return len;
        } else
        {
            return -1;
        }
    }

    public  int getFirstNodeAddress() {

        int prefixLength = getNetworkPrefixLength();

        if (prefixLength != -1)
        {
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            int len = 32 - prefixLength;
            long mask = (0xffffffff >>> len);
            return (int)(dhcpInfo.gateway & mask);
        } else
        {
            return -1;
        }
    }

    public String getNetworkMaskString()
    {
        int prefixLength = getNetworkPrefixLength();
        long mask = (0xffffffff >>> (32 - prefixLength));
        return ipAddressToString((int)mask);
    }

    public static String ipAddressToString(int ipAddress) {

        return String.format(Locale.US, "%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }


    public boolean isWifiConnected ()
    {
        return isWifiConnected;
    }

    public void processGrantedPermissions(int requestCode)
    {
        if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            for (WifiEventReceiver receiver : eventReceivers)
            {
                receiver.permissionGranted();
            }
        }
    }

    public boolean arePermissionsAvailable()
    {

        List<String> permissions = this.getRequiredPermissions();
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : permissions)
        {
            boolean granted = ContextCompat.checkSelfPermission(this.activity, permission) == PackageManager.PERMISSION_GRANTED;
            if (!granted)
            {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.size() > 0)
        {
            String[] permissionsArray = permissionsToRequest.toArray(new String[permissionsToRequest.size()]);
            ActivityCompat.requestPermissions(this.activity, permissionsArray, PERMISSIONS_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    private List<String> getRequiredPermissions() {
        List<String> granted = new ArrayList<>();
        try {
            PackageInfo pi = this.activity.getPackageManager().getPackageInfo(this.activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            Collections.addAll(granted, pi.requestedPermissions);
        } catch (Exception ignored) {
        }
        return granted;
    }

    // Scan Results utils

    public static  boolean isFrequency5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    public static String extractScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WEP", "WPA", "WPA2", "WPA_EAP", "IEEE8021X" };
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    public static int convertFrequencyToChannel(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return (frequency - 2412) / 5 + 1;
        } else if (frequency >= 5170 && frequency <= 5825) {
            return (frequency - 5170) / 5 + 34;
        } else {
            return -1;
        }
    }

    public static String channelWidthToString(int channelWidth)
    {

        String description;
        switch (channelWidth)
        {
            case ScanResult.CHANNEL_WIDTH_20MHZ:
                description = "20 MHz";
                break;
            case ScanResult.CHANNEL_WIDTH_40MHZ:
                description = "40 MHz";
                break;
            case ScanResult.CHANNEL_WIDTH_80MHZ:
                description = "80 MHz";
                break;
            case ScanResult.CHANNEL_WIDTH_160MHZ:
                description = "160 MHz";
                break;
            default:
                description = "80MHZ + 80MHZ";
        }

        return description;
    }

    private static String getLocalMacAddress(InetAddress address)
    {
        NetworkInterface network;
        byte[] mac;
        try {
            network = NetworkInterface.getByInetAddress(address);
            if (network == null)
            {
                return null;
            }
            mac = network.getHardwareAddress();
        } catch (SocketException e) {
            e.printStackTrace();
            return "(unknown)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
        }

        return sb.toString();
    }

    private static String getMacAddressRemote(InetAddress address)
    {
        String ip = address.getHostAddress();
        if (ip == null)
            return null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted.length >= 4 && ip.equals(splitted[0])) {
                    // Basic sanity check
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        return mac;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "(unknown)";
    }

    public static String getMacAddress(InetAddress address) {

        String mac = getLocalMacAddress(address);
        if (mac == null)
        {
            mac = getMacAddressRemote(address);
        }

        return mac;
    }

}

