/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.P4pluginCoreDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Future;

public class DeviceServiceProvider implements P4pluginCoreDeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceServiceProvider.class);

    @Override
    public Future<RpcResult<AddNodeOutput>> addNode(AddNodeInput input) {
        Preconditions.checkArgument(input != null, "Add node input is null.");
        AddNodeOutputBuilder builder = new AddNodeOutputBuilder();
        String node = input.getNodeId();
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String runtimeFile = input.getRuntimeFile();
        String configFile = input.getConfigFile();
        try {
            P4Device device = DeviceManager.addDevice(node, deviceId, ip, port, runtimeFile, configFile);
            builder.setResult(device != null);
        } catch (IOException e) {
            builder.setResult(false);
            LOG.info("Add node exception, node = {}, device-identifier = {}, " +
                            "runtime file = {}, config file = {}, reason = {}.",
                    node,
                    String.format("%s:%d:%d", ip, port, deviceId),
                    runtimeFile,
                    configFile,
                    e.getMessage());
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<RemoveNodeOutput>> removeNode(RemoveNodeInput input) {
        Preconditions.checkArgument(input != null, "Remove node input is null.");
        RemoveNodeOutputBuilder builder = new RemoveNodeOutputBuilder();
        String nodeId = input.getNodeId();
        builder.setResult(DeviceManager.isNodeExist(nodeId));
        DeviceManager.removeDevice(nodeId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        Preconditions.checkArgument(input != null, "Set pipeline config input is null.");
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            builder.setResult(DeviceManager.findDevice(nodeId).setPipelineConfig() != null);
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        Preconditions.checkArgument(input != null, "Get pipeline config input is null.");
        GetPipelineConfigOutputBuilder builder = new GetPipelineConfigOutputBuilder();
        String nodeId = input.getNodeId();
        String content;
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            content = TextFormat.printToString(device.getPipelineConfig().getConfigs(0).getP4Info());
            builder.setP4Info(content);
            builder.setResult(true);
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<QueryNodesOutput>> queryNodes() {
        QueryNodesOutputBuilder builder = new QueryNodesOutputBuilder();
        builder.setResult(true);
        builder.setNode(DeviceManager.queryNodes());
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
