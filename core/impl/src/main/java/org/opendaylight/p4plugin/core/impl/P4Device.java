/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.ActionProfile;
import org.opendaylight.p4plugin.p4info.proto.MatchField;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Table;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.DeleteTableEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.match.fields.match.field.FieldMatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.match.fields.match.field.field.match.type.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.action.type.DIRECTACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

public class P4Device {
    private static final Logger LOG = LoggerFactory.getLogger(P4Device.class);
    private GrpcChannel channel;
    private P4Info runtimeInfo;
    private ByteString deviceConfig;
    private Long deviceId;
    private State state = State.Unknown;

    private P4Device() {
    }

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

    public String getTableName(int tableId) {
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

    public int getMatchFieldId(String tableName, String matchFieldName) {
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

    public String getMatchFieldName(int tableId, int matchFieldId) {
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

    public int getMatchFieldWidth(String tableName, String matchFieldName) {
        if (runtimeInfo == null) {
            throw new NullPointerException("P4Device runtime info is null.");
        }
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

    public int getActionId(String actionName) {
        if (runtimeInfo == null) {
            throw new NullPointerException("P4Device runtime info is null.");
        }
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getId();
        }
        return result;
    }

    public String getActionName(int actionId) {
        if (runtimeInfo == null) throw new NullPointerException("P4Device runtime info is null.");
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getName();
        }
        return result;
    }

    public int getParamId(String actionName, String paramName) {
        if (runtimeInfo == null) throw new NullPointerException("P4Device runtime info is null.");
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
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

    public String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
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

    public int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
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

    public String getActionProfileName(Integer actionProfileId) {
        String result = null;
        Optional<ActionProfile> actionProfileContainer = runtimeInfo.getActionProfilesList().stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        if (actionProfileContainer.isPresent()) {
            result = actionProfileContainer.get().getPreamble().getName();
        }
        return result;
    }

    public boolean isConfigured() {
        return state == State.Configured;
    }

    private org.opendaylight.p4plugin.p4runtime.proto.Action.Builder newActionBuilder() {
        return org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder newParamBuilder() {
        return org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
    }

    public TableAction BuildTableAction(ActionType actionType) {
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

    public FieldMatch BuildFieldMatch(org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin
                                              .core.rev170808.match.fields.MatchField field,
                                      String tableName) {
        MatchFieldsParser parser = null;
        FieldMatchType matchType = field.getFieldMatchType();
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
     * Input table entry serialize to protobuf message.
     */
    public TableEntry inputTableEntrytoMessage(org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.TableEntry input) {
        if (runtimeInfo == null) {
            throw new NullPointerException("Runtime info is null.");
        }

        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();

        List<org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.match.fields.MatchField> fields =
                input.getMatchField();
        fields.forEach(field->{
            FieldMatch fieldMatch = BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });

        ActionType actionType = input.getActionType();
        TableAction tableAction = BuildTableAction(actionType);

        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    public TableEntry inputTableEntrytoMessage(DeleteTableEntryInput input) {
        if (runtimeInfo == null) {
            throw new NullPointerException("Runtime info is null.");
        }

        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();

        List<org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.match.fields.MatchField> fields =
                input.getMatchField();
        fields.forEach(field->{
            FieldMatch fieldMatch = BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    /**
     * Table entry object to human-readable string.
     */
    public String tableEntryToString(TableEntry entry) {
        if (runtimeInfo == null) {
            throw new NullPointerException("Runtime info is null.");
        }
        org.opendaylight.p4plugin.p4runtime.proto.Action action = entry.getAction().getAction();
        int tableId = entry.getTableId();
        int actionId = action.getActionId();
        int memberId = entry.getAction().getActionProfileMemberId();
        int groupId = entry.getAction().getActionProfileGroupId();

        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        String tableName = getTableName(tableId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(tableName + " ");

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
            buffer.append(" " + getActionName(actionId) + "(");
            paramList.forEach(param -> {
                int paramId = param.getParamId();
                buffer.append(String.format("%s", getParamName(actionId, paramId)));
                buffer.append(" = ");
                buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
            });
            buffer.append(")");
        }

        if (memberId != 0) {
            buffer.append(" member id = " + memberId);
        }

        if (groupId != 0) {
            buffer.append(" group id = " + groupId);
        }

        return new String(buffer);
    }

    public String actionProfileMemberToString(ActionProfileMember member) {
        int profileId = member.getActionProfileId();
        int memberId = member.getMemberId();
        org.opendaylight.p4plugin.p4runtime.proto.Action action = member.getAction();
        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        int actionId = action.getActionId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d", actionProfile, memberId));
        buffer.append(" " + getActionName(actionId) + "(");
        paramList.forEach(param -> {
            int paramId = param.getParamId();
            buffer.append(String.format("%s", getParamName(actionId, paramId)));
            buffer.append(" = ");
            buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
        });
        buffer.append(")");
        return new String(buffer);
    }

    public void setDeviceState(State state) {
        this.state = state;
    }

    public State getDeviceState() {
        return state;
    }

    public P4Info getRuntimeInfo() {
        return runtimeInfo;
    }

    public void setRuntimeInfo(String file) throws IOException {
        runtimeInfo  = Utils.parseRuntimeInfo(file);
    }

    public void setRuntimeInfo(P4Info runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    public ByteString getDeviceConfig() {
        return this.deviceConfig;
    }

    public void setDeviceConfig(String file) throws IOException {
        deviceConfig = Utils.parseDeviceConfigInfo(file);
    }

    public GrpcChannel getGrpcChannel() {
        return this.channel;
    }
    
    public Long getDeviceId() {
        return deviceId;
    }
    
    public void sendMasterArbitration() {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        masterArbitrationBuilder.setDeviceId(deviceId);
        requestBuilder.setArbitration(masterArbitrationBuilder);
        channel.getRequestStreamObserver().onNext(requestBuilder.build());
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        configBuilder.setDeviceId(deviceId);

        if(this.runtimeInfo != null) {
            configBuilder.setP4Info(this.runtimeInfo);
        }

        if(this.deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(this.deviceConfig);
        }

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                        .setAction(VERIFY_AND_COMMIT)
                                                        .addConfigs(configBuilder.build())
                                                        .build();
        SetForwardingPipelineConfigResponse response;

        try {
            /* response is empty now */
            response = channel.getBlockingStub().setForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Set pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig() {
        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest.newBuilder()
                                                        .addDeviceIds(deviceId)
                                                        .build();
        GetForwardingPipelineConfigResponse response;
        try {
            /* response is empty now */
            response = channel.getBlockingStub().getForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Get pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  write RPC, unary call;
     */
    public WriteResponse write(WriteRequest request) {
        @Nullable WriteResponse response;
        try {
            response = channel.getBlockingStub().write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    /**
     *  read RPC, server stream;
     */
    public Iterator<ReadResponse> read(ReadRequest request) {
        @Nullable Iterator<ReadResponse> responses;
        try {
            responses = channel.getBlockingStub().read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Read RPC failed: {}", e.getStatus());
        }
        return null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private GrpcChannel channel_;
        private P4Info runtimeInfo_;
        private ByteString deviceConfig_;
        private Long deviceId_;

        public Builder setChannel(GrpcChannel channel) {
            this.channel_ = channel;
            return this;
        }

        public Builder setRuntimeInfo(String file) throws IOException {
            runtimeInfo_ = Utils.parseRuntimeInfo(file);
            return this;
        }

        public Builder setDeviceConfig(String file) throws IOException {
            deviceConfig_ = Utils.parseDeviceConfigInfo(file);
            return this;
        }

        public Builder setDeviceId(Long deviceId) {
            this.deviceId_ = deviceId;
            return this;
        }

        public P4Device build() {
            P4Device target = new P4Device();
            target.deviceConfig = deviceConfig_;
            target.runtimeInfo = runtimeInfo_;
            target.channel = channel_;
            target.deviceId = deviceId_;
            return target;
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
        public DirectActionParser(DIRECTACTION action) {
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
                org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder = newParamBuilder();
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
        public ActionProfileMemberParser(ACTIONPROFILEMEMBER action) {
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
        public ActionProfileGroupParser(ACTIONPROFILEGROUP action) {
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
        public FieldMatchType matchType;
        public Integer matchFieldId;
        public Integer matchFieldWidth;

        public MatchFieldsParser(FieldMatchType matchType, String tableName, String fieldName) {
            this.matchType = matchType;
            this.matchFieldId = getMatchFieldId(tableName, fieldName);
            this.matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        }

        public abstract FieldMatch parse();
    }

    private class ExactMatchParser extends MatchFieldsParser {
        public ExactMatchParser(EXACT exact, String tableName, String fieldName) {
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
        public LpmMatchParser(LPM lpm, String tableName, String fieldName) {
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
        public TernaryMatchParser(TERNARY ternary, String tableName, String fieldName) {
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
        public RangeMatchParser(RANGE range, String tableName, String fieldName) {
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
        public ValidMatchParser(VALID valid, String tableName, String fieldName) {
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
}

