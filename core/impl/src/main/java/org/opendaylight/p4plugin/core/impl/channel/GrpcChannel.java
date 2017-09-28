/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.channel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opendaylight.p4plugin.p4runtime.proto.P4RuntimeGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrpcChannel {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcChannel.class);
    private static final List<StreamChannel> streamChannels = new ArrayList<>();
    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private String ip;
    private Integer port;

    public GrpcChannel(String ip, Integer port) {
        this(ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true));
        this.ip = ip;
        this.port = port;
    }

    private GrpcChannel(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        asyncStub = P4RuntimeGrpc.newStub(channel);
    }

    public P4RuntimeGrpc.P4RuntimeBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public P4RuntimeGrpc.P4RuntimeStub getAsyncStub() {
        return asyncStub;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public void registerStreamChannel(StreamChannel streamChannel) {
        streamChannels.add(streamChannel);
    }

    public void unRegisterStreamChannel(StreamChannel streamChannel) {
        streamChannels.remove(streamChannel);
    }

    public Integer getStreamChannelCount() {
        return streamChannels.size();
    }

    public void shutdown() {
        LOG.info("Shutdown gRPC channel, ip = {}, port = {}.", ip, port);
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
