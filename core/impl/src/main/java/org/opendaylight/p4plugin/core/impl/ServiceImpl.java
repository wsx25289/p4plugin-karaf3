/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4info.proto.Action;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup;
import org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember;
import org.opendaylight.p4plugin.p4runtime.proto.TableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public final  class ServiceImpl implements P4pluginCoreService {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);

    public boolean doSetPipelineConfig(SetPipelineConfigInput input) {
        String ip = input.getGrpcServerIp().getValue();
        Integer port = input.getGrpcServerPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String node = input.getNodeId();
        String deviceConfig = input.getConfigFile();
        String runtimeInfo = input.getRuntimeFile();
        SetForwardingPipelineConfigResponse response = null;
        P4Device device = null;

        try {
            device = new ResourceManager().getDevice(node, ip, port, deviceId, runtimeInfo, deviceConfig);
            if (device != null) {
                if (device.getDeviceState() != P4Device.State.Unknown) {
                    response = device.setPipelineConfig();
                }

                if (response != null) {
                    device.setDeviceState(P4Device.State.Configured);
                }
            }
            return response != null;
        } catch (IOException e) {
            LOG.info("IO Exception, reason = {}", e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public String doGetPipelineConfig(GetPipelineConfigInput input) {
        String node = input.getNodeId();
        P4Device device = new ResourceManager().findDevice(node);
        String result = null;
        if ((device == null) || !device.isConfigured()) {
            return result;
        }
        GetForwardingPipelineConfigResponse response = device.getPipelineConfig();
        if (response != null) {
            ForwardingPipelineConfig config = response.getConfigs(0);
            result = TextFormat.printToString(config.getP4Info());
        }
        return result;
    }

    public boolean doAddTableEntry(AddTableEntryInput input) {
        String node = input.getNodeId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Add table entry error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.INSERT);
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(device.inputTableEntrytoMessage(input));
        updateBuilder.setEntity(entityBuilder);
        request.addUpdates(updateBuilder);
        response = device.write(request.build());
        return response != null;
    }

    public boolean doModifyTableEntry(ModifyTableEntryInput input) {
        String node = input.getNodeId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Modify table entry error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.MODIFY);
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(device.inputTableEntrytoMessage(input));
        updateBuilder.setEntity(entityBuilder);
        request.addUpdates(updateBuilder);
        response = device.write(request.build());
        return response != null;
    }

    public boolean doDeleteTableEntry(DeleteTableEntryInput input) {
        String node = input.getNodeId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Delete table entry error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.DELETE);
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(device.inputTableEntrytoMessage(input));
        updateBuilder.setEntity(entityBuilder);
        request.addUpdates(updateBuilder);
        response = device.write(request.build());
        return response != null;
    }

    public List<String> doReadTableEntry(ReadTableEntryInput input) {
        String node = input.getNodeId();
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Read table entry error, cannot find a configured target");
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        String tableName = input.getTable();

        if (tableName == null) {
            entryBuilder.setTableId(0);
        } else {
            int tableId = device.getTableId(tableName);
            entryBuilder.setTableId(tableId);
        }

        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        Iterator<ReadResponse> responses = device.read(request.build());
        List<TableEntry> readEntries = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                TableEntry entry = entity.getTableEntry();
                readEntries.add(entry);
            });
            if (isCompleted) break;
        }

        List<String> result = new ArrayList<>();
        readEntries.forEach(entry->{
            String str = device.tableEntryToString(entry);
            result.add(str);
        });

        return result;
    }

    public boolean doAddActionProfileMember(AddActionProfileMemberInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long memberId = input.getMemberId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Add action profile member error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.INSERT);
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        String actionName = input.getActionName();
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        actionBuilder.setActionId(device.getActionId(actionName));

        input.getActionParam().forEach(actionParam -> {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder(); ;
            String paramName = actionParam.getParamName();
            String paramValue = actionParam.getParamValue();
            int paramId = device.getParamId(actionName, paramName);
            int paramWidth = device.getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
            ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
            paramBuilder.setValue(valueByteStr);
            actionBuilder.addParams(paramBuilder);

        });

        memberBuilder.setAction(actionBuilder);
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileMember(memberBuilder);
        updateBuilder.setEntity(entityBuilder.build());
        requestBuilder.addUpdates(updateBuilder.build());
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public boolean doModifyActionProfileMember(ModifyActionProfileMemberInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long memberId = input.getMemberId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Modify action profile member error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.MODIFY);
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        String actionName = input.getActionName();
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        actionBuilder.setActionId(device.getActionId(actionName));

        input.getActionParam().forEach(actionParam -> {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder(); ;
            String paramName = actionParam.getParamName();
            String paramValue = actionParam.getParamValue();
            int paramId = device.getParamId(actionName, paramName);
            int paramWidth = device.getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
            ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
            paramBuilder.setValue(valueByteStr);
            actionBuilder.addParams(paramBuilder);

        });

        memberBuilder.setAction(actionBuilder);
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileMember(memberBuilder);
        updateBuilder.setEntity(entityBuilder.build());
        requestBuilder.addUpdates(updateBuilder.build());
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public boolean doDeleteActionProfileMember(DeleteActionProfileMemberInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long memberId = input.getMemberId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Delete action profile member error, cannot find a configured target");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.DELETE);
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileMember(memberBuilder);
        updateBuilder.setEntity(entityBuilder.build());
        requestBuilder.addUpdates(updateBuilder.build());
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public List<String> doReadActionProfileMember(ReadActionProfileMemberInput input) {
        String node = input.getNodeId();
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Read action profile member error, cannot find a configured target");
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Entity.Builder entityBuilder = Entity.newBuilder();
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        String actionProfileName = input.getActionProfile();

        if (actionProfileName == null) {
            memberBuilder.setActionProfileId(0);
        } else {
            memberBuilder.setActionProfileId(device.getActionProfileId(actionProfileName));
        }

        entityBuilder.setActionProfileMember(memberBuilder);
        request.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = device.read(request.build());
        List<ActionProfileMember> readMembers = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                ActionProfileMember member = entity.getActionProfileMember();
                readMembers.add(member);
            });
            if (isCompleted) break;
        }

        List<String> result = new ArrayList<>();
        readMembers.forEach(member->{
            String str = device.actionProfileMemberToString(member);
            result.add(str);
        });
        return result;
    }

    public boolean doAddActionProfileGroup(AddActionProfileGroupInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long groupId = input.getGroupId();
        Integer maxSize = input.getMaxSize();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Add action profile group error, cannot find a configured target.");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.INSERT);
        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        groupBuilder.setMaxSize(maxSize);
        groupBuilder.setType(ActionProfileGroup.Type.valueOf(input.getGroupType().toString()));
        input.getGroupMember().forEach(groupMember -> {
            ActionProfileGroup.Member.Builder builder = ActionProfileGroup.Member.newBuilder();
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileGroup(groupBuilder);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.addUpdates(updateBuilder);
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public boolean doModifyActionProfileGroup(ModifyActionProfileGroupInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long groupId = input.getGroupId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Modify action profile group error, cannot find a configured target.");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.MODIFY);
        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        //groupBuilder.setMaxSize(maxSize);
        groupBuilder.setType(ActionProfileGroup.Type.valueOf(input.getGroupType().toString()));
        input.getGroupMember().forEach(groupMember -> {
            ActionProfileGroup.Member.Builder builder = ActionProfileGroup.Member.newBuilder();
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileGroup(groupBuilder);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.addUpdates(updateBuilder);
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public boolean doDeleteActionProfileGroup(DeleteActionProfileGroupInput input) {
        String node = input.getNodeId();
        String actionProfile = input.getActionProfile();
        Long groupId = input.getGroupId();
        WriteResponse response = null;
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Delete action profile group error, cannot find a configured target.");
            return false;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        requestBuilder.setDeviceId(device.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.DELETE);
        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        //groupBuilder.setMaxSize(maxSize);
        groupBuilder.setType(ActionProfileGroup.Type.valueOf(input.getGroupType().toString()));
        input.getGroupMember().forEach(groupMember -> {
            ActionProfileGroup.Member.Builder builder = ActionProfileGroup.Member.newBuilder();
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileGroup(groupBuilder);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.addUpdates(updateBuilder);
        response = device.write(requestBuilder.build());
        return response != null;
    }

    public List<String> doReadActionProfileGroup(ReadActionProfileGroupInput input) {
        //String ip = input.getIp().getValue();
        //Integer port = input.getPort().getValue();
        //Long deviceId = input.getDeviceId().longValue();
        String node = input.getNodeId();
        P4Device device = new ResourceManager().findDevice(node);

        if ((device == null) || !device.isConfigured()) {
            LOG.info("Read action profile group error, cannot find a configured target.");
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(device.getDeviceId());
        Entity.Builder entityBuilder = Entity.newBuilder();

        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        String actionProfileName = input.getActionProfile();
        Long groupId = input.getGroupId();

        if (actionProfileName == null) {
            groupBuilder.setActionProfileId(0);
        } else {
            groupBuilder.setActionProfileId(device.getActionProfileId(actionProfileName));
            groupBuilder.setGroupId(groupId.intValue());
        }

        entityBuilder.setActionProfileGroup(groupBuilder);
        request.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = device.read(request.build());
        List<ActionProfileMember> readMembers = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                ActionProfileMember member = entity.getActionProfileMember();
                readMembers.add(member);
            });
            if (isCompleted) break;
        }

        List<String> result = new ArrayList<>();
        readMembers.forEach(member->{
            String str = device.actionProfileMemberToString(member);
            result.add(str);
        });
        return result;
    }


    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(doSetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        GetPipelineConfigOutputBuilder builder = new GetPipelineConfigOutputBuilder();
        String content = doGetPipelineConfig(input);
        builder.setResult(content != null);
        builder.setContent(content);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<AddTableEntryOutput>> addTableEntry(AddTableEntryInput input) {
        AddTableEntryOutputBuilder builder = new AddTableEntryOutputBuilder();
        builder.setResult(doAddTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyTableEntryOutput>> modifyTableEntry(ModifyTableEntryInput input) {
        ModifyTableEntryOutputBuilder builder = new ModifyTableEntryOutputBuilder();
        builder.setResult(doModifyTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteTableEntryOutput>> deleteTableEntry(DeleteTableEntryInput input) {
        DeleteTableEntryOutputBuilder builder = new DeleteTableEntryOutputBuilder();
        builder.setResult(doDeleteTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        ReadTableEntryOutputBuilder builder = new ReadTableEntryOutputBuilder();
        List<String> result = doReadTableEntry(input);
        builder.setContent(result);
        builder.setResult(result != null);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<AddActionProfileMemberOutput>> addActionProfileMember(AddActionProfileMemberInput input) {
        AddActionProfileMemberOutputBuilder builder = new AddActionProfileMemberOutputBuilder();
        builder.setResult(doAddActionProfileMember(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteActionProfileMemberOutput>> deleteActionProfileMember(DeleteActionProfileMemberInput input) {
        DeleteActionProfileMemberOutputBuilder builder = new DeleteActionProfileMemberOutputBuilder();
        builder.setResult(doDeleteActionProfileMember(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyActionProfileMemberOutput>> modifyActionProfileMember(ModifyActionProfileMemberInput input) {
        ModifyActionProfileMemberOutputBuilder builder = new ModifyActionProfileMemberOutputBuilder();
        builder.setResult(doModifyActionProfileMember(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadActionProfileMemberOutput>> readActionProfileMember(ReadActionProfileMemberInput input) {
        ReadActionProfileMemberOutputBuilder builder = new ReadActionProfileMemberOutputBuilder();
        List<String> members = doReadActionProfileMember(input);
        builder.setMember(members);
        builder.setResult(members != null);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<AddActionProfileGroupOutput>> addActionProfileGroup(AddActionProfileGroupInput input) {
        AddActionProfileGroupOutputBuilder builder = new AddActionProfileGroupOutputBuilder();
        builder.setResult(doAddActionProfileGroup(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyActionProfileGroupOutput>> modifyActionProfileGroup(ModifyActionProfileGroupInput input) {
        ModifyActionProfileGroupOutputBuilder builder = new ModifyActionProfileGroupOutputBuilder();
        builder.setResult(doModifyActionProfileGroup(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteActionProfileGroupOutput>> deleteActionProfileGroup(DeleteActionProfileGroupInput input) {
        DeleteActionProfileGroupOutputBuilder builder = new DeleteActionProfileGroupOutputBuilder();
        builder.setResult(doDeleteActionProfileGroup(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadActionProfileGroupOutput>> readActionProfileGroup(ReadActionProfileGroupInput input) {
        ReadActionProfileGroupOutputBuilder builder = new ReadActionProfileGroupOutputBuilder();
        List<String> groups = doReadActionProfileGroup(input);
        builder.setResult(groups != null);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> p4TransmitPacket(P4TransmitPacketInput input) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(input.getPayload()));
        requestBuilder.setPacket(packetOutBuilder);
        String node = input.getNodeId();
        new ResourceManager().findDevice(node).getGrpcChannel().getRequestStreamObserver()
            .onNext(requestBuilder.build());
        return Futures.immediateFuture(RpcResultBuilder.success((Void)null).build());
    }
}
