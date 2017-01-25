package org.mistercyb.wifiinfo;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import pl.pawelkleczkowski.customgauge.CustomGauge;

/**
 *
 * Created by danno on 9/9/16.
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
class AccessPointDialog extends Dialog{

    private final ScanResult scanResult;
    private final Context context;
    private final boolean isCurrentConnection;

    public AccessPointDialog(Context context, ScanResult scanResult, boolean currentConnection)
    {
        super(context);
        this.context = context;
        this.scanResult = scanResult;
        this.isCurrentConnection = currentConnection;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.access_point_dialog);
        this.updateDialogSize();

        ImageView fastNetworkView = (ImageView)this.findViewById(R.id.f5ghz_indicator);
        TextView titleTextView = (TextView) this.findViewById(R.id.title);
        TextView addressTextView = (TextView) this.findViewById(R.id.address);
        TextView channelTextView = (TextView) this.findViewById(R.id.channel);
        TextView frequencyTextView = (TextView) this.findViewById(R.id.frequency);
        TextView intensityTextView = (TextView) this.findViewById(R.id.intensity);
        TextView passpointTextView = (TextView) this.findViewById(R.id.passpoint);
        TextView channelWidthTextView = (TextView) this.findViewById(R.id.channel_bandwidth);
        Button okButton = (Button)this.findViewById(R.id.ok_dialog);
        Button shareButton = (Button)this.findViewById(R.id.share_button);
        Button copyButton = (Button)this.findViewById(R.id.copy_button);
        Button connectButton = (Button)this.findViewById(R.id.connectButton);
        CustomGauge gauge = (CustomGauge)this.findViewById(R.id.gauge);

        // Set data
        boolean is5Ghz = WifiHelper.isFrequency5GHz(this.scanResult.frequency);
        int visibility = is5Ghz ? View.VISIBLE : View.INVISIBLE;
        fastNetworkView.setVisibility(visibility);
        String ssid = this.scanResult.SSID;
        if (ssid.equals(""))
            ssid = context.getString(R.string.hidden_access_point);
        titleTextView.setText(ssid);
        addressTextView.setText(this.scanResult.BSSID);
        String channelText = String.format(Locale.US, "%d", WifiHelper.convertFrequencyToChannel(this.scanResult.frequency));
        channelTextView.setText(channelText);
        String frequencyText = String.format(Locale.US, context.getString(R.string.mhz), this.scanResult.frequency);
        frequencyTextView.setText(frequencyText);
        String intensityText = String.format(Locale.US, context.getString(R.string.dbm), this.scanResult.level);
        intensityTextView.setText(intensityText);
        passpointTextView.setText(this.passpointDescription());
        String channelWidth;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            channelWidth = WifiHelper.channelWidthToString(this.scanResult.channelWidth);
        } else {
            channelWidth = context.getString(R.string.not_available);
        }
        channelWidthTextView.setText(channelWidth);


        int intensity = WifiManager.calculateSignalLevel(this.scanResult.level, 100);
        gauge.setValue(intensity);

        final Dialog dialog = this;

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String shareBody = accessPointDescription();
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.access_point));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                context.startActivity(Intent.createChooser(sharingIntent, context.getString(R.string.share)));
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(context.getString(R.string.wifi_access_point), accessPointDescription());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, R.string.ap_copied, Toast.LENGTH_SHORT)
                        .show();
            }
        });

        if (this.isCurrentConnection)
        {
            connectButton.setEnabled(false);
        }else {
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                }
            });
        }

    }

    private String accessPointDescription()
    {

        return context.getString(R.string.ssid_copy) +
                this.scanResult.SSID +
                "\n" +
                context.getString(R.string.bssid_copy) +
                this.scanResult.BSSID +
                "\n" +
                context.getString(R.string.frequency_copy) +
                this.scanResult.frequency +
                context.getString(R.string.mhz_copy) +
                "\n" +
                context.getString(R.string.intensity_copy) +
                this.scanResult.level +
                context.getString(R.string.dbm_copy) +
                context.getString(R.string.channel_copy) +
                WifiHelper.convertFrequencyToChannel(this.scanResult.frequency) +
                "\n" +
                context.getString(R.string.channel_width_copy) +
                this.channelWidthDescription() +
                "\n" +
                context.getString(R.string.passpoint_copy) +
                this.passpointDescription();
    }

    private String passpointDescription()
    {
        String passpoint;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            passpoint = this.scanResult.isPasspointNetwork() ? context.getString(R.string.yes) : context.getString(R.string.no);
        } else {
            passpoint = context.getString(R.string.unknown);
        }
        return passpoint;
    }

    private String channelWidthDescription()
    {
        String description;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            description = WifiHelper.channelWidthToString(this.scanResult.channelWidth);
        } else {
            description = context.getString(R.string.unknown);
        }
        return description;
    }

    private void updateDialogSize()
    {
        //This method is nasty, if the dialog size is not set up programmatically
        //the window height will be extremely large.
        final double factor1;
        final double factor2;

        if (getWindow() == null) {
            return;
        }

        DisplayMetrics displaymetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        int height = displaymetrics.heightPixels;
        height = (int)(height / (displaymetrics.density));

        factor1 = 0.9;
        factor2 = (height <= 592)? 0.85 : 0.75;

        if (displaymetrics.heightPixels > displaymetrics.widthPixels)
        {
            displaymetrics.widthPixels = (int) (displaymetrics.widthPixels * factor1);
            displaymetrics.heightPixels = (int) (displaymetrics.heightPixels * factor2);
        } else {
            displaymetrics.widthPixels = (int) (displaymetrics.widthPixels * factor2);
            displaymetrics.heightPixels = (int) (displaymetrics.heightPixels * factor1);
        }

        getWindow().setLayout(displaymetrics.widthPixels, displaymetrics.heightPixels);
    }

}
