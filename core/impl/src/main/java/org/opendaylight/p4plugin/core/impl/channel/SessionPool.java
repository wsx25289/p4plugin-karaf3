/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class used for managing gRPC sessions, one gRPC session can be used for
 * many stream channel by multiplex.
 */
public class SessionPool {
    private static final Logger LOG = LoggerFactory.getLogger(SessionPool.class);
    private static final ConcurrentHashMap<String, GrpcChannel> channels = new ConcurrentHashMap<>();

    public static GrpcChannel findChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        GrpcChannel channel = null;
        Optional<String> keyContainer = channels.keySet()
                .stream()
                .filter(k->k.equals(key))
                .findFirst();
        if (keyContainer.isPresent()) {
            channel = channels.get(key);
        }
        return channel;
    }

    public static GrpcChannel getChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        GrpcChannel channel = findChannel(ip, port);
        if (channel == null) {
            channel = new GrpcChannel(ip, port);
            channels.put(key, channel);
        }
        return channel;
    }

    public static void removeChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        channels.keySet().stream().filter(k -> k.equals(key))
                .collect(Collectors.toList())
                .forEach(k->{
                    channels.get(k).shutdown();
                    channels.remove(k);
                    LOG.info("gRPC channel = {} removed from pool.", k);
                });
    }

    public static void garbageCollection() {
        channels.keySet().stream().filter(k -> channels.get(k).getStreamChannelCount() == 0)
                .collect(Collectors.toList())
                .forEach(k->{
                    channels.get(k).shutdown();
                    channels.remove(k);
                    LOG.info("gRPC channel = {} removed from pool.", k);
                });
    }
}
