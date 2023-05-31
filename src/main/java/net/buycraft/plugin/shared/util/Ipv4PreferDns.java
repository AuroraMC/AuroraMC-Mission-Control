/*
 * Copyright (c) 2023 AuroraMC Ltd. All Rights Reserved.
 *
 * PRIVATE AND CONFIDENTIAL - Distribution and usage outside the scope of your job description is explicitly forbidden except in circumstances where a company director has expressly given written permission to do so.
 */

package net.buycraft.plugin.shared.util;

import okhttp3.Dns;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class Ipv4PreferDns implements Dns {
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        List<InetAddress> addresses = new ArrayList<>(Arrays.asList(InetAddress.getAllByName(hostname)));
        List<InetAddress> v6Addresses = new ArrayList<>();
        for (ListIterator<InetAddress> it = addresses.listIterator(); it.hasNext(); ) {
            InetAddress next = it.next();
            if (next instanceof Inet6Address) {
                it.remove();
                v6Addresses.add(next);
            }
        }
        addresses.addAll(v6Addresses);
        return addresses;
    }
}
