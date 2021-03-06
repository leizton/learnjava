package com.whiker.learn.common;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;

/**
 * @author leizton create on 17-3-9.
 */
public class NetUtil {

    public static Optional<String> localIp() {
        try {
            NetworkInterface ni;
            InetAddress inet;
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

            while (nis.hasMoreElements() && (ni = nis.nextElement()) != null) {
                Enumeration<InetAddress> inets = ni.getInetAddresses();
                while (inets.hasMoreElements() && (inet = inets.nextElement()) != null) {
                    if (inet instanceof Inet4Address && !inet.isLoopbackAddress()) {
                        return Optional.of(inet.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // ignore
        }
        return Optional.empty();
    }
}
