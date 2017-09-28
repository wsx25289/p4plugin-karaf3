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
import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.core.impl.table.TableManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class TableServiceProvider implements P4pluginCoreTableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableServiceProvider.class);

    @Override
    public Future<RpcResult<AddTableEntryOutput>> addTableEntry(AddTableEntryInput input) {
        Preconditions.checkArgument(input != null, "Add table entry input is null.");
        AddTableEntryOutputBuilder builder = new AddTableEntryOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).addTableEntry(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyTableEntryOutput>> modifyTableEntry(ModifyTableEntryInput input) {
        Preconditions.checkArgument(input != null, "Modify table entry input is null.");
        ModifyTableEntryOutputBuilder builder = new ModifyTableEntryOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).modifyTableEntry(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteTableEntryOutput>> deleteTableEntry(DeleteTableEntryInput input) {
        Preconditions.checkArgument(input != null, "Delete table entry input is null.");
        DeleteTableEntryOutputBuilder builder = new DeleteTableEntryOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).deleteTableEntry(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        Preconditions.checkArgument(input != null, "Read table entry input is null.");
        ReadTableEntryOutputBuilder builder = new ReadTableEntryOutputBuilder();
        String nodeId = input.getNodeId();
        String tableName = input.getTable();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setContent(new TableManager(device).readTableEntry(tableName));
            builder.setResult(true);
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<AddActionProfileMemberOutput>> addActionProfileMember(AddActionProfileMemberInput input) {
        Preconditions.checkArgument(input != null, "Add action profile member input is null.");
        AddActionProfileMemberOutputBuilder builder = new AddActionProfileMemberOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).addActionProfileMember(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyActionProfileMemberOutput>> modifyActionProfileMember(ModifyActionProfileMemberInput input){
        Preconditions.checkArgument(input != null, "Modify action profile member input is null.");
        ModifyActionProfileMemberOutputBuilder builder = new ModifyActionProfileMemberOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).modifyActionProfileMember(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteActionProfileMemberOutput>> deleteActionProfileMember(DeleteActionProfileMemberInput input) {
        Preconditions.checkArgument(input != null, "Modify action profile member input is null.");
        DeleteActionProfileMemberOutputBuilder builder = new DeleteActionProfileMemberOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).deleteActionProfileMember(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadActionProfileMemberOutput>> readActionProfileMember(ReadActionProfileMemberInput input) {
        Preconditions.checkArgument(input != null, "Delete action profile member input is null.");
        ReadActionProfileMemberOutputBuilder builder = new ReadActionProfileMemberOutputBuilder();
        String nodeId = input.getNodeId();
        String actionProfile = input.getActionProfile();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setContent(new TableManager(device).readActionProfileMember(actionProfile));
            builder.setResult(true);
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<AddActionProfileGroupOutput>> addActionProfileGroup(AddActionProfileGroupInput input) {
        Preconditions.checkArgument(input != null, "Add action profile group input is null.");
        AddActionProfileGroupOutputBuilder builder = new AddActionProfileGroupOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).addActionProfileGroup(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ModifyActionProfileGroupOutput>> modifyActionProfileGroup(ModifyActionProfileGroupInput input) {
        Preconditions.checkArgument(input != null, "Add action profile group input is null.");
        ModifyActionProfileGroupOutputBuilder builder = new ModifyActionProfileGroupOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).modifyActionProfileGroup(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<DeleteActionProfileGroupOutput>> deleteActionProfileGroup(DeleteActionProfileGroupInput input) {
        Preconditions.checkArgument(input != null, "Add action profile group input is null.");
        DeleteActionProfileGroupOutputBuilder builder = new DeleteActionProfileGroupOutputBuilder();
        String nodeId = input.getNodeId();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setResult(new TableManager(device).deleteActionProfileGroup(input));
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadActionProfileGroupOutput>> readActionProfileGroup(ReadActionProfileGroupInput input) {
        Preconditions.checkArgument(input != null, "Read action profile group input is null.");
        ReadActionProfileGroupOutputBuilder builder = new ReadActionProfileGroupOutputBuilder();
        String nodeId = input.getNodeId();
        String actionProfile = input.getActionProfile();
        try {
            P4Device device = DeviceManager.findConfiguredDevice(nodeId);
            builder.setContent(new TableManager(device).readActionProfileGroup(actionProfile));
            builder.setResult(true);
        } catch (Exception e) {
            builder.setResult(false);
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
