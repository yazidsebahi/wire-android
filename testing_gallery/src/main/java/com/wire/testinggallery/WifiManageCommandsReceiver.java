/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wire.testinggallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.widget.RemoteViews;

import com.google.common.base.Optional;

import java.util.List;

import static android.content.Context.WIFI_SERVICE;
import static com.wire.testinggallery.BuildConfig.APPLICATION_ID;

public class WifiManageCommandsReceiver extends BroadcastReceiver {
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_SSID = "SSID";
    private static final String EXTRA_PASSWORD = "PASSWORD";
    private final String SET_STATUS_ACTION = APPLICATION_ID + ".wifi.status.set";
    private final String CONNECT_ACTION = APPLICATION_ID + ".wifi.connect";
    private final String GET_SSID_ACTION = APPLICATION_ID + ".wifi.ssid.get";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action.toLowerCase()) {
                case SET_STATUS_ACTION:
                    setWifiStatus(context, intent);
                case CONNECT_ACTION:
                    connectToWiFi(context, intent);
                case GET_SSID_ACTION:
                    getSSIDName(context);
                default:
                    throw new RemoteViews.ActionException(String.format("Unknown action: %s", action));
            }
        }
    }

    private void getSSIDName(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration configuration : list) {
                if (configuration.status == WifiConfiguration.Status.CURRENT) {
                    setResultCode(Activity.RESULT_OK);
                    setResultData(configuration.SSID);
                    return;
                }
            }
        }
        setResultCode(Activity.RESULT_CANCELED);
        setResultData("No active WiFi connection");
    }

    private void connectToWiFi(Context context, Intent intent) {
        Optional<String> ssid = Optional.fromNullable(intent.getStringExtra(EXTRA_SSID));
        if (!ssid.isPresent()) {
            setResultCode(Activity.RESULT_CANCELED);
            setResultData("No SSID extra provided");
            return;
        }
        Optional<String> password = Optional.fromNullable(intent.getStringExtra(EXTRA_PASSWORD));
        if (password.isPresent() && !isSSIDKnown(context, ssid.get())) {
            addSsid(context, ssid.get(), password.get());
        }
        connectToSsid(context, ssid.get());
    }

    private void setWifiStatus(Context context, Intent intent) {
        Optional<String> newValue = Optional.fromNullable(intent.getStringExtra(EXTRA_VALUE));
        if (newValue.isPresent()) {
            setWifiStatus(context, Boolean.parseBoolean(newValue.get()));
            setResultCode(Activity.RESULT_OK);
            return;
        }
        setResultCode(Activity.RESULT_CANCELED);
        setResultData("No value extra provided");
    }

    private void setWifiStatus(Context context, boolean status) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.setWifiEnabled(status);
        }
    }

    private boolean isSSIDKnown(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID.equals(ssid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addSsid(Context context, String SSID, String password) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + SSID + "\"";   //SSID must be in quotes
        conf.wepKeys[0] = "\"" + password + "\"";
        conf.wepTxKeyIndex = 0;
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        conf.preSharedKey = "\"" + password + "\"";
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            wifiManager.addNetwork(conf);
        }
    }

    private void connectToSsid(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    setResultCode(Activity.RESULT_OK);
                    return;
                }
            }
            setResultCode(Activity.RESULT_CANCELED);
            setResultData(String.format("No SSID found: %s", ssid));
        }
    }
}
