package com.smartlocker.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@RestController
@RequestMapping("/api/class7")
@Profile("local")
public class LocalPortalAddressController {
    private final int serverPort;

    public LocalPortalAddressController(@Value("${server.port:8080}") int serverPort) {
        this.serverPort = serverPort;
    }

    @GetMapping("/portal-addresses")
    public List<PortalAddress> addresses() {
        List<PortalAddress> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                var interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    var address = interfaceAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress() && isPrivateAddress(address.getAddress())) {
                        String host = address.getHostAddress();
                        addresses.add(new PortalAddress(networkInterface.getDisplayName(),
                                "http://" + host + ":" + serverPort + "/"));
                    }
                }
            }
        } catch (SocketException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not inspect local network interfaces for QR setup.", exception);
        }
        if (addresses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No private network address was found. Connect this computer to the school Wi-Fi and try again.");
        }
        return addresses;
    }

    private boolean isPrivateAddress(byte[] address) {
        int first = Byte.toUnsignedInt(address[0]);
        int second = Byte.toUnsignedInt(address[1]);
        return first == 10
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
    }

    public record PortalAddress(String network, String url) {
    }
}
