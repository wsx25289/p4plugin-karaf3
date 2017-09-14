/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.P4pluginNetconfAdapterApiService;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadDeviceInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadDeviceInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadDeviceInterfaceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.DeviceInterface;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.DeviceInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.DeviceInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.PortIpv4;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.PortIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.PortIpv6;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.PortIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv4.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv4.Ipv4AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv4.Ipv4AddressKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv6.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv6.Ipv6AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.read.device._interface.output.device._interface.port.ipv6.Ipv6AddressKey;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfAdapterServiceImpl implements P4pluginNetconfAdapterApiService {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfAdapterServiceImpl.class);

    private DeviceInterfaceProcess deviceInterfaceProcess;

    public NetconfAdapterServiceImpl(DeviceInterfaceProcess deviceInterfaceProcess) {
        this.deviceInterfaceProcess = deviceInterfaceProcess;
    }

    @Override
    public Future<RpcResult<ReadDeviceInterfaceOutput>> readDeviceInterface(ReadDeviceInterfaceInput var1) {
        LOG.info("Start read interfaces");
        List<Interface> interfaceList = deviceInterfaceProcess.readDeviceInterfaces(var1.getDeviceId());
        ReadDeviceInterfaceOutputBuilder outputBuilder = new ReadDeviceInterfaceOutputBuilder();
        if (null == interfaceList) {
            outputBuilder.setDeviceInterface(null);
            return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
        }
        List<DeviceInterface> deviceInterfaceList = new ArrayList<>();
        for (Interface interFace :interfaceList) {
            deviceInterfaceList.add(constructOutputInterface(interFace));
        }
        outputBuilder.setDeviceInterface(deviceInterfaceList);
        return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
    }

    private DeviceInterface constructOutputInterface(Interface m) {
        DeviceInterfaceBuilder builder = new DeviceInterfaceBuilder();
        builder.setKey(new DeviceInterfaceKey(m.getKey().getName()));
        builder.setInterfaceName(m.getName());
        builder.setPortOperStatus(DeviceInterface.PortOperStatus.forValue(m.getOperStatus().getIntValue()));
        builder.setBandWidth(m.getSpeed());
        builder.setPortIpv4(constructOutputIpv4(m.getIpv4()));
        builder.setPortIpv6(constructOutputIpv6(m.getIpv6()));
        return builder.build();
    }

    private PortIpv4 constructOutputIpv4(Ipv4 ipv4) {
        PortIpv4Builder builder = new PortIpv4Builder();
        builder.setIpv4Forwarding(ipv4.isForwarding());
        builder.setIpv4Address(constructOutputIpv4Address(ipv4.getAddress()));
        return builder.build();
    }

    private PortIpv6 constructOutputIpv6(Ipv6 ipv6) {
        PortIpv6Builder builder = new PortIpv6Builder();
        builder.setIpv6Forwarding(ipv6.isForwarding());
        builder.setIpv6Address(constructOutputIpv6Address(ipv6.getAddress()));
        return builder.build();
    }

    private List<Ipv4Address> constructOutputIpv4Address(List<Address> list) {
        List<Ipv4Address> ipv4AddressList = new ArrayList<>();
        for (Address address : list) {
            Ipv4AddressBuilder builder = new Ipv4AddressBuilder();
            builder.setKey(new Ipv4AddressKey(address.getKey().getIp()));
            builder.setIp(address.getIp());
            ipv4AddressList.add(builder.build());
        }
        return ipv4AddressList;
    }

    private List<Ipv6Address> constructOutputIpv6Address(List<org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test
            .rev170908.interfaces.state._interface.ipv6.Address> list) {
        List<Ipv6Address> ipv6AddressList = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test
                .rev170908.interfaces.state._interface.ipv6.Address address : list) {
            Ipv6AddressBuilder builder = new Ipv6AddressBuilder();
            builder.setKey(new Ipv6AddressKey(address.getKey().getIp()));
            builder.setIp(address.getIp());
            ipv6AddressList.add(builder.build());
        }
        return ipv6AddressList;
    }
}
