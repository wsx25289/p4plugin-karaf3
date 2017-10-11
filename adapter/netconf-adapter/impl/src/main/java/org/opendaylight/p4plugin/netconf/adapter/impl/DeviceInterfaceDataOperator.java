/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.node.interfaces.state.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.node.interfaces.state.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DeviceInterfaceDataOperator {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceInterfaceDataOperator.class);

    private final DataProcess dataProcess;
    private final RpcConsumerRegistry rpcConsumerRegistry;

    private static final InstanceIdentifier<NodeInterfacesState> NODE_INTERFACE_IID = InstanceIdentifier
            .create(NodeInterfacesState.class);


    public DeviceInterfaceDataOperator(DataProcess dataProcess, RpcConsumerRegistry rpcConsumerRegistry) {
        this.dataProcess = dataProcess;
        this.rpcConsumerRegistry = rpcConsumerRegistry;
    }

    public void writeInterfacesToDevice(String nodeId) {
        LOG.info("Start write data to device");
        dataProcess.writeToDevice(nodeId, NODE_INTERFACE_IID);
    }

    public NodeInterfacesState readInterfacesFromDevice(String nodeId) {
        LOG.info("Start read data from device");
        return dataProcess.readFromDevice(nodeId, NODE_INTERFACE_IID);
    }

    public void sendP4DeviceInfo(List<Node> nodeList) {
        for (Node node : nodeList) {
            try {
                LOG.info("Call rpc addNode");
                Future<RpcResult<AddNodeOutput>> addNodeRpcResult =  rpcConsumerRegistry
                        .getRpcService(P4pluginCoreDeviceService.class)
                        .addNode(constructRpcAddNodeInput(node.getNodeId(), node.getGrpcServerIp(),
                                node.getGrpcServerPort(), node.getDeviceId()));
                if (!addNodeRpcResult.get().isSuccessful()) {
                    LOG.info("Rpc addNode called failed, node: {}", node.getNodeId());
                    continue;
                }
                if (!addNodeRpcResult.get().getResult().isResult()) {
                    LOG.info("Add node {} failed, do not call rpc setPipelineConfig", node.getNodeId());
                    continue;
                }

                LOG.info("Call rpc setPipelineConfig");
                Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfigRpcResult = rpcConsumerRegistry
                        .getRpcService(P4pluginCoreDeviceService.class)
                        .setPipelineConfig(constructRpcSetPipelineConfigInput(node.getNodeId()));
                if (!setPipelineConfigRpcResult.get().isSuccessful()) {
                    LOG.info("Rpc setPipelineConfig called failed, node: {}", node.getNodeId());
                    continue;
                }
                if (!setPipelineConfigRpcResult.get().getResult().isResult()) {
                    LOG.info("Set node {} forwarding pipeline config failed", node.getNodeId());
                    continue;
                }
                LOG.info("Set node {} forwarding pipeline config success", node.getDeviceId());
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Rpc interrupted by {}", e);
                continue;
            }
        }
    }

    private AddNodeInput constructRpcAddNodeInput(String nodeId, Ipv4Address ip, PortNumber port, BigInteger deviceId) {
        AddNodeInputBuilder builder = new AddNodeInputBuilder();
        builder.setNodeId(nodeId.substring(nodeId.length() - 1, nodeId.length()));
        builder.setGrpcServerIp(ip);
        builder.setGrpcServerPort(port);
        builder.setDeviceId(deviceId);
        builder.setRuntimeFile("/home/opendaylight/odl/p4src/switch.proto.txt");
        builder.setConfigFile(null);
        return builder.build();
    }

    private SetPipelineConfigInput constructRpcSetPipelineConfigInput(String nodeId) {
        SetPipelineConfigInputBuilder builder = new SetPipelineConfigInputBuilder();
        builder.setNodeId(nodeId.substring(nodeId.length() - 1, nodeId.length()));
        return builder.build();
    }

    public void writeInterfacesToControllerDataStore(List<Node> nodeList) {
        LOG.info("Start write data to controller data store");
        for (Node node : nodeList) {
            InstanceIdentifier path = getNodePath(node.getNodeId());
            dataProcess.writeToDataStore(node, path);
        }
    }

    public NodeInterfacesState readInterfacesFromControllerDataStore() {
        LOG.info("Read data from controller data store");
        return dataProcess.readFromDataStore(NODE_INTERFACE_IID);
    }

    private InstanceIdentifier<Node> getNodePath(String nodeId) {
        return InstanceIdentifier.create(NodeInterfacesState.class).child(Node.class, new NodeKey(nodeId));
    }
}
