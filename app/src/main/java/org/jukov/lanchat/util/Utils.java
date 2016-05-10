package org.jukov.lanchat.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by jukov on 05.02.2016.
 */
public class Utils {

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        if(dhcp == null)
            return InetAddress.getByName("255.255.255.255");
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public static String getWifiAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return android.text.format.Formatter.formatIpAddress(wifiInfo.getIpAddress());
    }

    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String newRoomUID(Context context) {
        return getAndroidID(context) + Long.toString(new Date().getTime());
    }

    public static String getSendMessageDate(Context context, long millis) {
        long now = System.currentTimeMillis();
        long diff = now - millis;
        String format = "HH:mm";
        if (diff > TimeUnit.DAYS.toMillis(365)) {
            format = "MMM d, HH:mm";
        } else if (diff > TimeUnit.DAYS.toMillis(1)) {
            format = "MMM d yyyy, HH:mm";
        }
//        SimpleDateFormat simpleDateFormat = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(context);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.getDefault());
//        return dateFormat.format(new Date(millis));
        return simpleDateFormat.format(new Date(millis));
    }
}
