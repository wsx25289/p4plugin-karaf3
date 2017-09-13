/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4runtime.proto.*;

import org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember;
import org.opendaylight.p4plugin.p4runtime.proto.TableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.action.profile.member.input.ReadMemberType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.action.profile.member.input.read.member.type.ALLMEMBERS;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.action.profile.member.input.read.member.type.ONEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.table.entry.input.ReadEntryType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.table.entry.input.read.entry.type.ALLTABLES;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.table.entry.input.read.entry.type.ONETABLE;
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
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String deviceConfig = input.getConfigFile();
        String runtimeInfo = input.getRuntimeFile();
        SetForwardingPipelineConfigResponse response = null;
        P4Device device = null;

        try {
            device = ResourceManager.getDevice(ip, port, deviceId, runtimeInfo, deviceConfig);
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
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        P4Device device = ResourceManager.findDevice(ip, port, deviceId);
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

    public boolean doWriteTableEntry(WriteTableEntryInput input) {
        String host = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        WriteResponse response = null;
        P4Device device = ResourceManager.findDevice(host, port, deviceId);

        if ((device != null) && !device.isConfigured()) {
            WriteRequest.Builder request = WriteRequest.newBuilder();
            request.setDeviceId(input.getDeviceId().longValue());
            Update.Builder updateBuilder = Update.newBuilder();
            updateBuilder.setType(Update.Type.valueOf(input.getOperation().toString()));
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setTableEntry(device.toMessage(input));
            updateBuilder.setEntity(entityBuilder.build());
            request.addUpdates(updateBuilder.build());
            response = device.write(request.build());
        } else {
            LOG.info("Set table entry error, cannot find configured target");
        }

        return response != null;
    }

    public List<String> doGetTableEntry(ReadTableEntryInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        P4Device device = ResourceManager.findDevice(ip, port, deviceId);

        if ((device == null) || !device.isConfigured()) {
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(deviceId);
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        ReadEntryType readType = input.getReadEntryType();

        if ((readType instanceof ALLTABLES) || (readType == null)) {
            entryBuilder.setTableId(0);
        } else if (readType instanceof ONETABLE) {
            ONETABLE oneTable = (ONETABLE)readType;
            String tableName = oneTable.getTable();
            int tableId = device.getTableId(tableName);
            entryBuilder.setTableId(tableId);
        } else {
            return null;
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

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(doSetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    public boolean doWriteActionProfileEntry(WriteActionProfileMemberInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String actionProfile = input.getActionProfile();
        Long memberId = input.getMemberId();
        WriteResponse response = null;
        P4Device device = ResourceManager.findDevice(ip, port, deviceId);

        if ((device == null) || !device.isConfigured()) {
            return false;
        }

        if ((device != null) && !device.isConfigured()) {
            WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
            requestBuilder.setDeviceId(deviceId);
            Update.Builder updateBuilder = Update.newBuilder();
            updateBuilder.setType(Update.Type.valueOf(input.getOperation().toString()));
            ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
            memberBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
            memberBuilder.setMemberId(memberId.intValue());
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setActionProfileMember(memberBuilder);
            updateBuilder.setEntity(entityBuilder.build());
            requestBuilder.addUpdates(updateBuilder.build());
            response = device.write(requestBuilder.build());
        } else {
            LOG.info("Set table entry error, cannot find a configured target.");
        }

        return true;
    }

    public List<String> doReadActionProfileMember(ReadActionProfileMemberInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        P4Device device = ResourceManager.findDevice(ip, port, deviceId);

        if ((device == null) || !device.isConfigured()) {
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(deviceId);
        Entity.Builder entityBuilder = Entity.newBuilder();
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        ReadMemberType type = input.getReadMemberType();

        if ((type == null) || (type instanceof ALLMEMBERS)) {
            memberBuilder.setActionProfileId(0);
        } else if (type instanceof ONEMEMBER) {
            String actionProfile = ((ONEMEMBER)type).getMemberName();
            memberBuilder.setActionProfileId(device.getActionProfileId(actionProfile));
        } else {
            LOG.info("Invalid read action profile member type.");
            return null;
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

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        GetPipelineConfigOutputBuilder builder = new GetPipelineConfigOutputBuilder();
        String content = doGetPipelineConfig(input);
        builder.setResult(content != null);
        builder.setContent(content);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<WriteTableEntryOutput>> writeTableEntry(WriteTableEntryInput input) {
        WriteTableEntryOutputBuilder builder = new WriteTableEntryOutputBuilder();
        builder.setResult(doWriteTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        ReadTableEntryOutputBuilder builder = new ReadTableEntryOutputBuilder();
        List<String> result = doGetTableEntry(input);
        builder.setContent(result);
        builder.setResult(result != null);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<WriteActionProfileMemberOutput>> writeActionProfileMember(WriteActionProfileMemberInput input) {
        WriteActionProfileMemberOutputBuilder builder = new WriteActionProfileMemberOutputBuilder();
        builder.setResult(doWriteActionProfileEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadActionProfileMemberOutput>> readActionProfileMember(ReadActionProfileMemberInput input) {
        ReadActionProfileMemberOutputBuilder builder = new ReadActionProfileMemberOutputBuilder();
        List<String> result = doReadActionProfileMember(input);
        builder.setMember(result);
        builder.setResult(result != null);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
