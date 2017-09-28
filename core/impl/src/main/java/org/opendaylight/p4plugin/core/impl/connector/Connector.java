/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.connector;

import org.opendaylight.p4plugin.core.impl.channel.GrpcChannel;
import org.opendaylight.p4plugin.core.impl.channel.SessionPool;
import org.opendaylight.p4plugin.core.impl.channel.StreamChannel;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class Connector {
    private static final Logger LOG = LoggerFactory.getLogger(Connector.class);
    private final GrpcChannel gRPCchannel;
    private final StreamChannel streamChannel;
    public Connector(String nodeId, Long deviceId, String ip, Integer port) {
        gRPCchannel = SessionPool.getChannel(ip, port);
        streamChannel = new StreamChannel(nodeId, deviceId, gRPCchannel);
    }

    public GrpcChannel getGrpcChannel() {
        return gRPCchannel;
    }

    public StreamChannel getStreamChannel() {
        return streamChannel;
    }

    public boolean connect() {
        streamChannel.openStreamChannel();
        streamChannel.sendMasterArbitration();
        return streamChannel.getStreamChannelState();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig(SetForwardingPipelineConfigRequest request) {
        return gRPCchannel.getBlockingStub().setForwardingPipelineConfig(request);
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig(GetForwardingPipelineConfigRequest request) {
        return gRPCchannel.getBlockingStub().getForwardingPipelineConfig(request);
    }

    public WriteResponse write(WriteRequest request) {
        return gRPCchannel.getBlockingStub().write(request);
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        return gRPCchannel.getBlockingStub().read(request);
    }

    public void transmitPacket(byte[] payload) {
        streamChannel.transmitPacket(payload);
    }

    public void sendMasterArbitration() {
        streamChannel.sendMasterArbitration();
    }

    public void shutdown() {
        streamChannel.shutdown();
    }
}
