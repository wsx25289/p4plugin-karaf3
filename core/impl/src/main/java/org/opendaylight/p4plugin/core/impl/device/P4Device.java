/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.device;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.core.impl.connector.Connector;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.ActionProfile;
import org.opendaylight.p4plugin.p4info.proto.MatchField;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Table;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.match.fields.field.match.type.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.DIRECTACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

public class P4Device {
    private static final Logger LOG = LoggerFactory.getLogger(P4Device.class);
    private Connector connector;
    private P4Info runtimeInfo;
    private ByteString deviceConfig;
    private String ip;
    private Integer port;
    private Long deviceId;
    private String nodeId;
    private State state = State.Unknown;
    private P4Device() {}

    public int getTableId(String tableName) {
        Optional<Table> container = runtimeInfo.getTablesList().stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (container.isPresent()) {
            result = container.get().getPreamble().getId();
        }
        return result;
    }

    private String getTableName(int tableId) {
        Optional<Table> container = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (container.isPresent()) {
            result = container.get().getPreamble().getName();
        }
        return result;
    }

    private int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getName().equals(matchFieldName))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = matchFieldContainer.get().getId();
            }
        }
        return result;
    }

    private String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getId() == (matchFieldId))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = matchFieldContainer.get().getName();
            }
        }
        return result;
    }

    private int getMatchFieldWidth(String tableName, String matchFieldName) {
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getName().equals(matchFieldName))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = (matchFieldContainer.get().getBitwidth() + 7) / 8;
            }
        }
        return result;
    }

    private int getActionId(String actionName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getId();
        }
        return result;
    }

    private String getActionName(int actionId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getName();
        }
        return result;
    }

    private int getParamId(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getName().equals(paramName))
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = paramContainer.get().getId();
            }
        }
        return result;
    }

    private String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getId() == paramId)
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = paramContainer.get().getName();
            }
        }
        return result;
    }

    private int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getName().equals(paramName))
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = (paramContainer.get().getBitwidth() + 7) / 8;
            }
        }
        return result;
    }

    public int getActionProfileId(String actionProfileName) {
        int result = 0;
        Optional<ActionProfile> actionProfileContainer = runtimeInfo.getActionProfilesList().stream()
                .filter(actionProfile -> actionProfile.getPreamble().getName().equals(actionProfileName))
                .findFirst();
        if (actionProfileContainer.isPresent()) {
            result = actionProfileContainer.get().getPreamble().getId();
        }
        return result;
    }

    private String getActionProfileName(Integer actionProfileId) {
        String result = null;
        Optional<ActionProfile> actionProfileContainer = runtimeInfo.getActionProfilesList().stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        if (actionProfileContainer.isPresent()) {
            result = actionProfileContainer.get().getPreamble().getName();
        }
        return result;
    }
    public Long getDeviceId() {
        return deviceId;
    }
    public String getIp() {
        return ip;
    }
    public Integer getPort() {
        return port;
    }

    public boolean isConfigured() {
        return runtimeInfo != null &&
                deviceConfig != null &&
                state == State.Configured;
    }

    public boolean connectToDevice() {
        return connector.connect();
    }

    public String getDescription() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("nodeId = ").append(nodeId).append(" ")
                .append("deviceId = ").append(deviceId).append(" ")
                .append("ip = ").append(ip).append(" ")
                .append("port = ").append(port).append(" ")
                .append("configured = ").append(state).append(".");
        return new String(buffer);
    }

    public void setDeviceState(State state) {
        this.state = state;
    }

    public State getDeviceState() {
        return state;
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        if (deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(deviceConfig);
        }
        if (runtimeInfo != null) {
            configBuilder.setP4Info(runtimeInfo);
        }
        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        SetForwardingPipelineConfigRequest request =
                SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(VERIFY_AND_COMMIT)
                .addConfigs(configBuilder.build())
                .build();
        SetForwardingPipelineConfigResponse response;

        try {
            /* response is empty now */
            response = connector.setPipelineConfig(request);
            setDeviceState(State.Configured);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Set pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig() {
        GetForwardingPipelineConfigRequest request =
                GetForwardingPipelineConfigRequest.newBuilder()
                .addDeviceIds(deviceId)
                .build();
        GetForwardingPipelineConfigResponse response;
        try {
            /* response is empty now */
            response = connector.getPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Get pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response;
        try {
            response = connector.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        try {
            responses = connector.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Read RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    public void sendMasterArbitration() {
        connector.sendMasterArbitration();
    }

    public void transmitPacket(byte[] payload) {
        connector.transmitPacket(payload);
    }

    private TableAction BuildTableAction(ActionType actionType) {
        ActionParser parser = null;
        if (actionType instanceof DIRECTACTION) {
            parser = new DirectActionParser((DIRECTACTION) actionType);
        } else if (actionType instanceof ACTIONPROFILEMEMBER) {
            parser = new ActionProfileMemberParser((ACTIONPROFILEMEMBER) actionType);
        } else if (actionType instanceof ACTIONPROFILEGROUP) {
            parser = new ActionProfileGroupParser((ACTIONPROFILEGROUP) actionType);
        } else {
            LOG.info("Invalid action type.");
        }
        return parser == null ? null : parser.parse();
    }

    private FieldMatch BuildFieldMatch(org.opendaylight.yang.gen.v1.urn.opendaylight
                                              .p4plugin.core.table.rev170808.match.fields.Field field,
                                      String tableName) {
        MatchFieldsParser parser = null;
        org.opendaylight.yang.gen.v1.urn.opendaylight.
                p4plugin.core.table.rev170808.match.fields.field.MatchType matchType = field.getMatchType();
        String fieldName = field.getFieldName();
        if (matchType instanceof EXACT) {
            parser = new ExactMatchParser((EXACT) matchType, tableName, fieldName);
        } else if (matchType instanceof LPM) {
            parser = new LpmMatchParser((LPM) matchType, tableName, fieldName);
        } else if (matchType instanceof TERNARY) {
            parser = new TernaryMatchParser((TERNARY) matchType, tableName, fieldName);
        } else if (matchType instanceof RANGE) {
            parser = new RangeMatchParser((RANGE) matchType, tableName, fieldName);
        } else if (matchType instanceof VALID) {
            parser = new ValidMatchParser((VALID) matchType, tableName, fieldName);
        } else {
            LOG.info("Invalid match type.");
        }
        return parser == null ? null : parser.parse();
    }

    /**
     * Input table entry serialize to protobuf message, used for add/modify.
     * When this method is called, the device must be configured.
     */
    public TableEntry toTableEntryMessage(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                  .p4plugin.core.table.rev170808.TableEntry input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight
                .p4plugin.core.table.rev170808.match.fields.Field> fields = input.getField();
        fields.forEach(field -> {
            FieldMatch fieldMatch = BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        org.opendaylight.yang.gen.v1.urn.opendaylight
                .p4plugin.core.table.rev170808.table.entry.ActionType actionType = input.getActionType();
        TableAction tableAction = BuildTableAction(actionType);
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    /**
     * Used for delete table entry, when delete a table entry, only need table name
     * and match fields actually. BTW, this the only way for search a table entry,
     * not support table entry id.
     */
    public TableEntry toTableEntryMessage(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                  .p4plugin.core.table.rev170808.EntryKey input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight
                .p4plugin.core.table.rev170808.match.fields.Field> fields = input.getField();
        fields.forEach(field -> {
            FieldMatch fieldMatch = BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    public ActionProfileMember toActionProfileMemberMessage(
            org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.core.table.rev170808.ActionProfileMember member) {
        String actionName = member.getActionName();
        Long memberId = member.getMemberId();
        String actionProfile = member.getActionProfile();

        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        Action.Builder actionBuilder = Action.newBuilder();

        actionBuilder.setActionId(getActionId(actionName));
        member.getActionParam().forEach(actionParam -> {
            Action.Param.Builder paramBuilder = Action.Param.newBuilder();
            String paramName = actionParam.getParamName();
            String paramValue = actionParam.getParamValue();
            int paramId = getParamId(actionName, paramName);
            int paramWidth = getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
            ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
            paramBuilder.setValue(valueByteStr);
            actionBuilder.addParams(paramBuilder);
        });

        memberBuilder.setAction(actionBuilder);
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    public ActionProfileMember toActionProfileMemberMessage(
            org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.core.table.rev170808.MemberKey key) {
        Long memberId = key.getMemberId();
        String actionProfile = key.getActionProfile();
        ActionProfileMember.Builder memberBuilder = ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    public ActionProfileGroup toActionProfileGroupMessage(
            org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup group) {
        Long groupId = group.getGroupId();
        String actionProfile = group.getActionProfile();
        org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup.GroupType
                type = group.getGroupType();
        Integer maxSize = group.getMaxSize();

        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        groupBuilder.setType(ActionProfileGroup.Type.valueOf(type.toString()));
        groupBuilder.setMaxSize(maxSize);

        group.getGroupMember().forEach(groupMember -> {
            ActionProfileGroup.Member.Builder builder = ActionProfileGroup.Member.newBuilder();
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });
        return groupBuilder.build();
    }

    public ActionProfileGroup toActionProfileGroupMessage(
            org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.core.table.rev170808.GroupKey key) {
        Long groupId = key.getGroupId();
        String actionProfile = key.getActionProfile();
        ActionProfileGroup.Builder groupBuilder = ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        return groupBuilder.build();
    }

    /**
     * Table entry object to human-readable string, for read table entry.
     */
    public String toTableEntryString(TableEntry entry) {
        org.opendaylight.p4plugin.p4runtime.proto.Action action = entry.getAction().getAction();
        int tableId = entry.getTableId();
        int actionId = action.getActionId();
        int memberId = entry.getAction().getActionProfileMemberId();
        int groupId = entry.getAction().getActionProfileGroupId();

        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        String tableName = getTableName(tableId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(tableName).append(" ");

        List<FieldMatch> fieldList = entry.getMatchList();
        fieldList.forEach(field -> {
            int fieldId = field.getFieldId();
            switch (field.getFieldMatchTypeCase()) {
                case EXACT: {
                    FieldMatch.Exact exact = field.getExact();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(exact.getValue().toByteArray()));
                    buffer.append(":exact");
                    break;
                }

                case LPM: {
                    FieldMatch.LPM lpm = field.getLpm();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(lpm.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(lpm.getPrefixLen()));
                    buffer.append(":lpm");
                    break;
                }

                case TERNARY: {
                    FieldMatch.Ternary ternary = field.getTernary();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(ternary.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(ternary.getMask()));//TODO
                    break;
                }
                //TODO
                case RANGE:
                    break;
                case VALID:
                    break;
                default:
                    break;
            }
        });

        if (actionId != 0) {
            buffer.append(" ").append(getActionName(actionId)).append("(");
            paramList.forEach(param -> {
                int paramId = param.getParamId();
                buffer.append(String.format("%s", getParamName(actionId, paramId)));
                buffer.append(" = ");
                buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
            });
            buffer.append(")");
        }

        if (memberId != 0) {
            buffer.append(" member id = ").append(memberId);
        }

        if (groupId != 0) {
            buffer.append(" group id = ").append(groupId);
        }

        return new String(buffer);
    }

    public String toActionProfileMemberString(ActionProfileMember member) {
        int profileId = member.getActionProfileId();
        int memberId = member.getMemberId();
        org.opendaylight.p4plugin.p4runtime.proto.Action action = member.getAction();
        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        int actionId = action.getActionId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d", actionProfile, memberId));
        buffer.append(" ").append(getActionName(actionId)).append("(");
        paramList.forEach(param -> {
            int paramId = param.getParamId();
            buffer.append(String.format("%s", getParamName(actionId, paramId)));
            buffer.append(" = ");
            buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
        });
        buffer.append(")");
        return new String(buffer);
    }

    public String toActionProfileGroupString(ActionProfileGroup group) {
        int profileId = group.getActionProfileId();
        int groupId = group.getGroupId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d : ", actionProfile, groupId));
        group.getMembersList().forEach(member -> buffer.append(member.getMemberId()).append(" "));
        return new String(buffer);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private P4Info runtimeInfo_;
        private ByteString deviceConfig_;
        private Long deviceId_;
        private String nodeId_;
        private String ip_;
        private Integer port_;

        public Builder setIp(String ip) {
            this.ip_ = ip;
            return this;
        }

        public Builder setPort(Integer port) {
            this.port_ = port;
            return this;
        }

        public Builder setRuntimeInfo(P4Info p4Info) {
            this.runtimeInfo_ = p4Info;
            return this;
        }

        public Builder setDeviceConfig(ByteString config) {
            this.deviceConfig_ = config;
            return this;
        }

        public Builder setDeviceId(Long deviceId) {
            this.deviceId_ = deviceId;
            return this;
        }

        public Builder setNodeId(String nodeId) {
            this.nodeId_ = nodeId;
            return this;
        }

        public P4Device build() {
            P4Device device = new P4Device();
            device.deviceConfig = deviceConfig_;
            device.runtimeInfo = runtimeInfo_;
            device.deviceId = deviceId_;
            device.nodeId = nodeId_;
            device.ip = ip_;
            device.port = port_;
            device.connector = new Connector(nodeId_, deviceId_, ip_, port_);
            return device;
        }
    }

    public enum State {
        Unknown,
        Connected,
        Configured,
    }

    private interface ActionParser {
        TableAction parse();
    }

    private class DirectActionParser implements ActionParser {
        private DIRECTACTION action;
        private DirectActionParser(DIRECTACTION action) {
            this.action = action;
        }

        @Override
        public TableAction parse() {
            TableAction.Builder tableActionBuilder = TableAction.newBuilder();
            Action.Builder actionBuilder = Action.newBuilder();
            List<ActionParam> params = action.getActionParam();
            String actionName = action.getActionName();
            actionBuilder.setActionId(getActionId(actionName));
            for (ActionParam p : params) {
                org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
                String paramName = p.getParamName();
                String paramValue = p.getParamValue();
                int paramId = getParamId(actionName, paramName);
                int paramWidth = getParamWidth(actionName, paramName);
                paramBuilder.setParamId(paramId);
                byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
                ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
                paramBuilder.setValue(valueByteStr);
                actionBuilder.addParams(paramBuilder);
            }
            return tableActionBuilder.setAction(actionBuilder).build();
        }
    }

    private class ActionProfileMemberParser implements ActionParser {
        private ACTIONPROFILEMEMBER action;
        private ActionProfileMemberParser(ACTIONPROFILEMEMBER action) {
            this.action = action;
        }

        @Override
        public TableAction parse() {
            TableAction.Builder builder = TableAction.newBuilder();
            builder.setActionProfileMemberId(action.getMemberId().intValue());
            return builder.build();
        }
    }

    private class ActionProfileGroupParser implements ActionParser {
        private ACTIONPROFILEGROUP action;
        private ActionProfileGroupParser(ACTIONPROFILEGROUP action) {
            this.action = action;
        }

        @Override
        public TableAction parse() {
            TableAction.Builder builder = TableAction.newBuilder();
            builder.setActionProfileGroupId(action.getGroupId().intValue());
            return builder.build();
        }
    }

    private abstract class MatchFieldsParser {
        protected org.opendaylight.yang.gen.v1.urn.opendaylight
                .p4plugin.core.table.rev170808.match.fields.field.MatchType matchType;
        protected Integer matchFieldId;
        protected Integer matchFieldWidth;

        private MatchFieldsParser(
                org.opendaylight.yang.gen.v1.urn.opendaylight
                        .p4plugin.core.table.rev170808.match.fields.field.MatchType matchType,
                String tableName, String fieldName) {
            this.matchType = matchType;
            this.matchFieldId = getMatchFieldId(tableName, fieldName);
            this.matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        }

        public abstract FieldMatch parse();
    }

    private class ExactMatchParser extends MatchFieldsParser {
        private ExactMatchParser(EXACT exact, String tableName, String fieldName) {
            super(exact, tableName, fieldName);
        }

        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
            EXACT exact = (EXACT) matchType;
            String value = exact.getExactValue();
            exactBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
            fieldMatchBuilder.setExact(exactBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class LpmMatchParser extends MatchFieldsParser {
        private LpmMatchParser(LPM lpm, String tableName, String fieldName) {
            super(lpm, tableName, fieldName);
        }

        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
            LPM lpm = (LPM) matchType;
            String value = lpm.getLpmValue();
            int prefixLen = lpm.getLpmPrefixLen();
            // Value must match ipv4 address
            if (value.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                            + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                            + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                lpmBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
                lpmBuilder.setPrefixLen(prefixLen);
            }
            fieldMatchBuilder.setLpm(lpmBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class TernaryMatchParser extends MatchFieldsParser {
        private TernaryMatchParser(TERNARY ternary, String tableName, String fieldName) {
            super(ternary, tableName, fieldName);
        }

        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
            TERNARY ternary = (TERNARY) matchType;
            String mask = ternary.getTernaryMask();
            String value = ternary.getTernaryValue();

            if (value.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                            + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                            + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                ternaryBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
            }

            if (mask.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                           + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                           + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                ternaryBuilder.setMask(ByteString.copyFrom(Utils.strToByteArray(mask, 4)));
            } else if (mask.matches("([1-9]|[1-2][0-9]|3[0-2])")) {
                StringBuffer buffer = new StringBuffer(32);
                for (int i = 0; i < 32; i++) {
                    if (i < Integer.parseInt(mask)) {
                        buffer.append('1');
                    } else {
                        buffer.append('0');
                    }
                }

                String[] resultStr = new String[4];
                byte[] resultByte = new byte[4];
                for (int i = 0; i < resultStr.length; i++) {
                    resultStr[i] = buffer.substring(i * 8, i * 8 + 8);
                    for (int m = resultStr[i].length() - 1, n = 0; m >= 0; m--, n++) {
                        resultByte[i] += Byte.parseByte(resultStr[i].charAt(i) + "") * Math.pow(2, n);
                    }
                }
                ternaryBuilder.setMask(ByteString.copyFrom(resultByte));
            }
            fieldMatchBuilder.setTernary(ternaryBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class RangeMatchParser extends MatchFieldsParser {
        private RangeMatchParser(RANGE range, String tableName, String fieldName) {
            super(range, tableName, fieldName);
        }

        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Range.Builder rangeBuilder = FieldMatch.Range.newBuilder();
            RANGE range = (RANGE) matchType;
            Long high = range.getRangeHigh();
            Long low = range.getRangeLow();
            rangeBuilder.setHigh(ByteString.copyFrom(Utils.intToByteArray(high.intValue())));
            rangeBuilder.setLow(ByteString.copyFrom(Utils.intToByteArray(low.intValue())));
            fieldMatchBuilder.setFieldId(matchFieldId);
            fieldMatchBuilder.setRange(rangeBuilder);
            return fieldMatchBuilder.build();
        }
    }

    private class ValidMatchParser extends MatchFieldsParser {
        private ValidMatchParser(VALID valid, String tableName, String fieldName) {
            super(valid, tableName, fieldName);
        }

        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Valid.Builder validBuilder = FieldMatch.Valid.newBuilder();
            VALID valid = (VALID) matchType;
            validBuilder.setValue(valid.isValidValue());
            fieldMatchBuilder.setFieldId(matchFieldId);
            fieldMatchBuilder.setValid(validBuilder);
            return fieldMatchBuilder.build();
        }
    }

    public void shutdown() {
        connector.shutdown();
    }
}
