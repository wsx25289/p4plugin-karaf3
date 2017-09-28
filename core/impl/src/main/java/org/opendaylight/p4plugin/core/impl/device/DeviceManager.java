/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.device;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);
    private static ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>(); //nodeId<->P4Device
    private DeviceManager() {}

    public static boolean isNodeExist(String nodeId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> k.equals(nodeId))
                .findFirst();
        return keyContainer.isPresent();
    }

    public static boolean isDeviceExist(String ip, Integer port, Long deviceId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> devices.get(k).getDeviceId().equals(deviceId)
                        && devices.get(k).getIp().equals(ip)
                        && devices.get(k).getPort().equals(port))
                .findFirst();
        return keyContainer.isPresent();
    }

    public static P4Device findDevice(String node) {
        P4Device device = null;
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k->k.equals(node))
                .findFirst();
        if (keyContainer.isPresent()) {
            device = devices.get(node);
        }
        return device;
    }

    public static P4Device findDevice(Long deviceId) {
        P4Device device = null;
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k->devices.get(k).getDeviceId().equals(deviceId))
                .findFirst();
        if (keyContainer.isPresent()) {
            device = devices.get(keyContainer.get());
        }
        return device;
    }

    public static P4Device addDevice(String nodeId, Long deviceId, String ip, Integer port,
                                     String runtimeFile, String configFile) throws IOException {
        String description = String.format("%s<->%s:%d:%d", nodeId, ip, port, deviceId);
        if (!isNodeExist(nodeId) && !isDeviceExist(ip, port, deviceId)) {
            P4Info p4Info = Utils.parseRuntimeInfo(runtimeFile);
            ByteString config = Utils.parseDeviceConfigInfo(configFile);
            P4Device.Builder builder = P4Device.newBuilder()
                    .setNodeId(nodeId)
                    .setDeviceId(deviceId)
                    .setRuntimeInfo(p4Info)
                    .setDeviceConfig(config)
                    .setIp(ip)
                    .setPort(port);
            P4Device device = builder.build();
            if (device.connectToDevice()) {
                device.setDeviceState(P4Device.State.Connected);
                devices.put(nodeId, device);
                LOG.info("Add device success, device-identifier = {}", description);
                return device;
            }
            LOG.info("Connect to device failed, device-identifier = {}.", description);
            return null;
        }
        LOG.info("Add device failed, reason: node id or connection is already existed, " +
                "device-identifier = {}.", description);
        return null;
    }

    public static void removeDevice(String nodeId) {
        P4Device device = findDevice(nodeId);
        if (device != null) {
            device.shutdown();
            devices.remove(nodeId);
        }
        LOG.info("Remove device, node = {}, find = {}.", nodeId, device != null);
    }

    public static P4Device findConfiguredDevice(String nodeId) {
        P4Device device = findDevice(nodeId);
        if (device != null && device.isConfigured()) {
            return device;
        }
        LOG.info("Cannot find a configured node = {}", nodeId);
        return null;
    }

    public static List<String> queryNodes() {
        List<String> result = new ArrayList<>();
        devices.keySet().forEach(node->{
            P4Device device = devices.get(node);
            result.add(device.getDescription());
        });
        return result;
    }
}
