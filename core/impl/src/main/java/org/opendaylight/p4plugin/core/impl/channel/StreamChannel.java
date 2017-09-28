/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.channel;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.core.impl.NotificationProvider;
import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.packet.rev170808.P4PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StreamChannel {
    private static final Logger LOG = LoggerFactory.getLogger(StreamChannel.class);
    private final Long deviceId;
    private final String nodeId;
    private final GrpcChannel channel;
    private StreamObserver<StreamMessageRequest> observer;
    private CountDownLatch countDownLatch;

    public StreamChannel(String nodeId, Long deviceId, GrpcChannel channel) {
        this.deviceId = deviceId;
        this.channel = channel;
        this.nodeId = nodeId;
    }

    public boolean getStreamChannelState() {
        boolean state = true;
        try {
            state = !countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.info("Get stream channel state exception.");
            e.printStackTrace();
        }
        return state;
    }

    public void sendMasterArbitration() {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        masterArbitrationBuilder.setDeviceId(deviceId);
        requestBuilder.setArbitration(masterArbitrationBuilder);
        observer.onNext(requestBuilder.build());
    }

    public void transmitPacket(byte[] payload) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(payload));
        //metadataList.forEach(packetOutBuilder::addMetadata);
        requestBuilder.setPacket(packetOutBuilder);
        observer.onNext(requestBuilder.build());
        //For debug
        LOG.info("Transmit packet = {}.", Utils.bytesToHexString(payload));
    }

    private void onPacketReceived(StreamMessageResponse response) {
        switch(response.getUpdateCase()) {
            case PACKET: {
                P4PacketReceivedBuilder builder = new P4PacketReceivedBuilder();
                byte[] payload = response.getPacket().getPayload().toByteArray();
                builder.setNodeId(nodeId);
                builder.setPayload(payload);
                NotificationProvider.getInstance().notify(builder.build());
                //For debug
                LOG.info("Receive packet-in from node = {}, body = {}.", nodeId,
                        Utils.bytesToHexString(payload));
                break;
            }
            case ARBITRATION:
            case UPDATE_NOT_SET:
            default:break;
        }
    }

    private void onStreamChannelError(Throwable t) {
        channel.unRegisterStreamChannel(this);
        SessionPool.garbageCollection();
        DeviceManager.removeDevice(nodeId);
        countDownLatch.countDown();
        LOG.info("Stream channel on error, reason = {}, backtrace = {}.", t.getMessage(), t);
    }

    private void onStreamChannelCompleted() {
        channel.unRegisterStreamChannel(this);
        SessionPool.garbageCollection();
        countDownLatch.countDown();
        LOG.info("Stream channel on complete.");
    }

    public void openStreamChannel() {
        channel.registerStreamChannel(this);
        countDownLatch = new CountDownLatch(1);
        StreamObserver<StreamMessageResponse> response = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse response) {
                onPacketReceived(response);
            }
            @Override
            public void onError(Throwable t) {
                onStreamChannelError(t);
            }
            @Override
            public void onCompleted() {
                onStreamChannelCompleted();
            }
        };
        observer = channel.getAsyncStub().streamChannel(response);
    }

    public void shutdown() {
        observer.onCompleted();
    }
}
