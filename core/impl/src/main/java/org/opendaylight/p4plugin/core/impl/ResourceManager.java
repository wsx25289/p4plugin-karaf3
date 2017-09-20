/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ResourceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

    static ConcurrentHashMap<String, GrpcChannel> channels = new ConcurrentHashMap<>();//ip:port <-> GrpcChannel
    static ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>();//ip:port:device-id <-> P4Device
    static ConcurrentHashMap<String, String> nodes = new ConcurrentHashMap<>();//node-id <-> ip:port:device-id
    public ResourceManager() {}

    public GrpcChannel findChannel(String ip, Integer port) {
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

    public GrpcChannel getChannel(String ip, Integer port) {
        GrpcChannel channel = findChannel(ip, port);
        if (channel == null) {
            channel = new GrpcChannel(ip, port);
        }

        if (channel.getChannelState()) {
            channels.put(String.format("%s:%d", ip, port), channel);
        } else {
            channel.shutdown();
            channel = null;
        }

        return channel;
    }

    public void removeChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        channels.keySet()
                .stream()
                .filter(k -> k.equals(key))
                .collect(Collectors.toList())
                .forEach(k->{
                    channels.get(k).shutdown();
                    channels.remove(k);
                });
    }
    
    public P4Device findDevice(String node) {
        P4Device device = null;
        String result = null;
        Optional<String> nodeKeyContainer = nodes.keySet()
                                                 .stream()
                                                 .filter(k->k.equals(node))
                                                 .findFirst();         
        if (nodeKeyContainer.isPresent()) {
            result = nodes.get(node);
        }
        
        final String deviceKey = result;
        
        Optional<String> deviceKeyContainer = devices.keySet()
                                                     .stream()
                                                     .filter(k->k.equals(deviceKey))
                                                     .findFirst();
        if (deviceKeyContainer.isPresent()) {
            device = devices.get(deviceKey);    
        }                  
        
        return device;
    }
    
    public P4Device findDevice(String ip, Integer port, Long deviceId) {
        String key = String.format("%s:%d:%d", ip, port, deviceId);
        P4Device device = null;
        Optional<String> keyContainer = devices.keySet().stream().filter(k->k.equals(key)).findFirst();

        if (keyContainer.isPresent()) {
            device = devices.get(key);
        }

        return device;
    }
    
    public P4Device getDevice(String node, String ip, Integer port, Long deviceId,
                              String runtimeInfo, String deviceConfig) throws IOException 
    {
        P4Device device = findDevice(node);

        if (device == null) {
            device = newDevice(ip, port, deviceId, runtimeInfo, deviceConfig);
            if (device != null) {
                device.setDeviceState(P4Device.State.Connected);
                devices.put(String.format("%s:%d:%d", ip, port, deviceId), device);
                nodes.put(node, String.format("%s:%d:%d", ip, port, deviceId));
                device.sendMasterArbitration();
            }
        } else {
            if (runtimeInfo != null) {
                device.setRuntimeInfo(runtimeInfo);
                device.setDeviceState(P4Device.State.Connected);
            }

            if (deviceConfig != null) {
                device.setDeviceConfig(deviceConfig);
                device.setDeviceState(P4Device.State.Connected);
            }
        }

        return device;
    }
    
    public P4Device getDevice(String ip, Integer port, Long deviceId,
                              String runtimeInfo, String deviceConfig) throws IOException {
        P4Device device = findDevice(ip, port, deviceId);

        if (device == null) {
            device = newDevice(ip, port, deviceId, runtimeInfo, deviceConfig);
            if (device != null) {
                device.setDeviceState(P4Device.State.Connected);
                devices.put(String.format("%s:%d:%d", ip, port, deviceId), device);
                device.sendMasterArbitration();
            }
        } else {
            if (runtimeInfo != null) {
                device.setRuntimeInfo(runtimeInfo);
                device.setDeviceState(P4Device.State.Connected);
            }

            if (deviceConfig != null) {
                device.setDeviceConfig(deviceConfig);
                device.setDeviceState(P4Device.State.Connected);
            }
        }

        return device;
    }

    public P4Device newDevice(String ip, Integer port, Long deviceId,
                                     String runtimeInfo, String deviceConfig) throws IOException {
        GrpcChannel channel = getChannel(ip, port);
        P4Device device = null;
        if (channel != null) {
            P4Device.Builder builder = P4Device.newBuilder()
                    .setChannel(channel)
                    .setDeviceId(deviceId)
                    .setRuntimeInfo(runtimeInfo)
                    .setDeviceConfig(deviceConfig);
            device = builder.build();
        }
        return device;
    }

    public void removeDevices(String ip, Integer port) {
        String deviceKey = String.format("%s:%d:.*", ip, port);
        devices.keySet()
                .stream()
                .filter(k->k.matches(deviceKey))
                .collect(Collectors.toList())
                .forEach(devices::remove);
    }
    
    public void removeNodes(String ip, Integer port) {
        String value = String.format("%s:%d:.*", ip, port);
        nodes.keySet()
             .stream()
             .filter(k->nodes.get(k).equals(value))
             .collect(Collectors.toList())
             .forEach(nodes::remove);
    }
}
