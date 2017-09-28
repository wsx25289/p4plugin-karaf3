/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.table;

import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.TableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TableManager {
    private static final Logger LOG = LoggerFactory.getLogger(TableManager.class);
    private final P4Device device;
    public TableManager(P4Device device ) {
        this.device = device;
    }

    public boolean addTableEntry(TableEntry entry) {
        return new AddTableEntryOperator(entry).operate();
    }

    public boolean modifyTableEntry(TableEntry entry) {
        return new ModifyTableEntryOperator(entry).operate();
    }

    public boolean deleteTableEntry(EntryKey entry) {
        return new DeleteTableEntryOperator(entry).operate();
    }

    public List<String> readTableEntry(String tableName) {
        ReadRequest.Builder request = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        entryBuilder.setTableId(tableName == null ? 0 : device.getTableId(tableName));
        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        request.setDeviceId(device.getDeviceId());

        Iterator<ReadResponse> responses = device.read(request.build());
        List<String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = device.toTableEntryString(entity.getTableEntry());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    private abstract class TableEntryOperator {
        private org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry;
        private Update.Type type;

        protected void setEntry(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
            this.entry = entry;
        }

        protected void setType(Update.Type type) {
            this.type = type;
        }

        protected boolean operate() {
            WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
            Update.Builder updateBuilder = Update.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setTableEntry(entry);
            updateBuilder.setType(type);
            updateBuilder.setEntity(entityBuilder);
            requestBuilder.setDeviceId(device.getDeviceId());
            requestBuilder.addUpdates(updateBuilder);
            return device.write(requestBuilder.build()) != null;
        }
    }

    private class AddTableEntryOperator extends TableEntryOperator{
        private AddTableEntryOperator(TableEntry entry) {
            setEntry(device.toTableEntryMessage(entry));
            setType(Update.Type.INSERT);
        }
    }

    private class ModifyTableEntryOperator extends TableEntryOperator {
        private ModifyTableEntryOperator(TableEntry entry ) {
            setEntry(device.toTableEntryMessage(entry));
            setType(Update.Type.MODIFY);
        }
    }

    private class DeleteTableEntryOperator extends TableEntryOperator {
        private DeleteTableEntryOperator(EntryKey key ) {
            setEntry(device.toTableEntryMessage(key));
            setType(Update.Type.DELETE);
        }
    }


    public boolean addActionProfileMember(ActionProfileMember actionProfileMember) {
        return new AddActionProfileMemberOperator(actionProfileMember).operate();
    }

    public boolean modifyActionProfileMember(ActionProfileMember actionProfileMember) {
        return new ModifyActionProfileMemberOperator(actionProfileMember).operate();
    }

    public boolean deleteActionProfileMember(MemberKey key) {
        return new DeleteActionProfileMemberOperator(key).operate();
    }

    public List<String> readActionProfileMember(String actionProfileName) {
        ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();

        memberBuilder.setActionProfileId(actionProfileName == null ? 0 : device.getActionProfileId(actionProfileName));
        entityBuilder.setActionProfileMember(memberBuilder);
        requestBuilder.setDeviceId(device.getDeviceId());
        requestBuilder.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = device.read(requestBuilder.build());
        List<String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = device.toActionProfileMemberString(entity.getActionProfileMember());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    private abstract class ActionProfileMemberOperator {
        private org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member;
        private Update.Type type;

        protected void setMember(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member) {
            this.member = member;
        }

        protected void setType(Update.Type type) {
            this.type = type;
        }

        protected boolean operate() {
            WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
            Update.Builder updateBuilder = Update.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setActionProfileMember(member);
            updateBuilder.setType(type);
            updateBuilder.setEntity(entityBuilder);
            requestBuilder.addUpdates(updateBuilder);
            requestBuilder.setDeviceId(device.getDeviceId());
            return device.write(requestBuilder.build()) != null;
        }
    }

    private class AddActionProfileMemberOperator extends ActionProfileMemberOperator {
        private AddActionProfileMemberOperator(ActionProfileMember member) {
            setMember(device.toActionProfileMemberMessage(member));
            setType(Update.Type.INSERT);
        }
    }

    private class ModifyActionProfileMemberOperator extends ActionProfileMemberOperator {
        private ModifyActionProfileMemberOperator(ActionProfileMember member) {
            setMember(device.toActionProfileMemberMessage(member));
            setType(Update.Type.MODIFY);
        }
    }

    private class DeleteActionProfileMemberOperator extends ActionProfileMemberOperator {
        private DeleteActionProfileMemberOperator(MemberKey key) {
            setMember(device.toActionProfileMemberMessage(key));
            setType(Update.Type.DELETE);
        }
    }

    public boolean addActionProfileGroup(ActionProfileGroup actionProfileGroup) {
        return new AddActionProfileGroupOperator(actionProfileGroup).operate();
    }

    public boolean modifyActionProfileGroup(ActionProfileGroup actionProfileGroup) {
        return new ModifyActionProfileGroupOperator(actionProfileGroup).operate();
    }

    public boolean deleteActionProfileGroup(GroupKey key) {
        return new DeleteActionProfileGroupOperator(key).operate();
    }

    public List<String> readActionProfileGroup(String actionProfileName) {
        ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(actionProfileName == null ? 0 : device.getActionProfileId(actionProfileName));
        entityBuilder.setActionProfileGroup(groupBuilder);
        requestBuilder.setDeviceId(device.getDeviceId());
        requestBuilder.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = device.read(requestBuilder.build());
        List<String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = device.toActionProfileGroupString(entity.getActionProfileGroup());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    private abstract class ActionProfileGroupOperator {
        private org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group;
        private Update.Type type;

        protected void setGroup(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group) {
            this.group = group;
        }

        protected void setType(Update.Type type) {
            this.type = type;
        }

        protected boolean operate() {
            WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
            Update.Builder updateBuilder = Update.newBuilder();
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setActionProfileGroup(group);
            updateBuilder.setEntity(entityBuilder);
            updateBuilder.setType(type);
            requestBuilder.addUpdates(updateBuilder);
            requestBuilder.setDeviceId(device.getDeviceId());
            return device.write(requestBuilder.build()) != null;
        }
    }

    private class AddActionProfileGroupOperator extends ActionProfileGroupOperator {
        private AddActionProfileGroupOperator(ActionProfileGroup group) {
            setGroup(device.toActionProfileGroupMessage(group));
            setType(Update.Type.INSERT);
        }
    }

    private class ModifyActionProfileGroupOperator extends ActionProfileGroupOperator {
        private ModifyActionProfileGroupOperator(ActionProfileGroup group) {
            setGroup(device.toActionProfileGroupMessage(group));
            setType(Update.Type.MODIFY);
        }
    }

    private class DeleteActionProfileGroupOperator extends ActionProfileGroupOperator {
        private DeleteActionProfileGroupOperator(GroupKey key) {
            setGroup(device.toActionProfileGroupMessage(key));
            setType(Update.Type.DELETE);
        }
    }
}
