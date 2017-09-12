/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.driver.impl;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDataOperator {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDataOperator.class);

    private MountPointService mountPointService = null;

    private static final InstanceIdentifier<Topology> NETCONF_TOPO_IID = InstanceIdentifier
            .create(NetworkTopology.class).child(Topology.class,
                    new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public NetconfDataOperator(MountPointService mountPointService) {
        this.mountPointService = mountPointService;
    }

    public void write(String nodeId, InstanceIdentifier<InterfacesState> path) {
        LOG.info("Get dataBroker");
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            LOG.info("Data broker is null, return");
            return;
        }
        LOG.info("Construct data");
        InterfacesState data = constructInterfacesState();
        LOG.info("Process write data, path is {}, data is {}", path, data);
        writeData(nodeDataBroker, path, data);
    }

    public List<Interface> read(String nodeId, InstanceIdentifier<InterfacesState> path) {
        LOG.info("Get dataBroker");
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            LOG.info("Data broker is null, return");
            return null;
        }
        LOG.info("Process read data");
        return readData(nodeDataBroker, path);
    }

    private static DataBroker getDataBroker(String nodeId, MountPointService mountPointService) {
        LOG.info("Get mountPoint");
        MountPoint mountPoint = getMountPoint(nodeId, mountPointService);
        if (null == mountPoint) {
            LOG.info("MountPoint is null");
            return null;
        }
        LOG.info("Process get dataBroker");
        Optional<DataBroker> nodeDataBroker = mountPoint.getService(DataBroker.class);

        if (!nodeDataBroker.isPresent()) {
            LOG.info("DataBroker is not present");
            return null;
        }
        return nodeDataBroker.get();
    }

    private static MountPoint getMountPoint(String nodeId, MountPointService mountPointService) {
        if (null == mountPointService) {
            return null;
        }
        Optional<MountPoint> nodeMountPoint = mountPointService.getMountPoint(NETCONF_TOPO_IID
                .child(Node.class, new NodeKey(new NodeId(nodeId))));

        if (!nodeMountPoint.isPresent()) {
            return null;
        }
        return nodeMountPoint.get();
    }

    private InterfacesState constructInterfacesState() {
        InterfaceBuilder interfaceBuilderOne = new InterfaceBuilder();
        interfaceBuilderOne.setKey(new InterfaceKey("InterfaceOne"));
        interfaceBuilderOne.setName("InterfaceOne");
        interfaceBuilderOne.setOperStatus(Interface.OperStatus.Up);
        interfaceBuilderOne.setSpeed(new Gauge64(new BigInteger("819200")));
        interfaceBuilderOne.setIpv4(constructIpv4("10.38.38.38", "10.39.39.39"));
        //interfaceBuilderOne.setIpv6(constructIpv6("3ffe::1010:2a2a::0001", "fe81::7010::fe26::8171::36af"));

        InterfaceBuilder interfaceBuilderTwo = new InterfaceBuilder();
        interfaceBuilderTwo.setKey(new InterfaceKey("InterfaceTwo"));
        interfaceBuilderTwo.setName("InterfaceTwo");
        interfaceBuilderTwo.setOperStatus(Interface.OperStatus.Up);
        interfaceBuilderTwo.setSpeed(new Gauge64(new BigInteger("819200")));
        interfaceBuilderTwo.setIpv4(constructIpv4("10.40.40.40", "10.41.41.41"));
        //interfaceBuilderTwo.setIpv6(constructIpv6("fe82::7011::fe27::8172::36af", "fe83::7012::fe28::8173::36af"));

        List<Interface> list = new ArrayList<>();
        list.add(interfaceBuilderOne.build());
        list.add(interfaceBuilderTwo.build());

        InterfacesStateBuilder stateBuilder = new InterfacesStateBuilder();
        stateBuilder.setInterface(list);
        return stateBuilder.build();
    }

    private Ipv4 constructIpv4(String ipv4One, String ipv4Two) {
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        ipv4Builder.setForwarding(true);
        AddressBuilder addressBuilder1 = new AddressBuilder();
        addressBuilder1.setKey(new AddressKey(new Ipv4AddressNoZone(ipv4One)));
        addressBuilder1.setIp(new Ipv4AddressNoZone(ipv4One));
        AddressBuilder addressBuilder2 = new AddressBuilder();
        addressBuilder2.setKey(new AddressKey(new Ipv4AddressNoZone(ipv4Two)));
        addressBuilder2.setIp(new Ipv4AddressNoZone(ipv4Two));
        List<Address> addressList = new ArrayList<>();
        addressList.add(addressBuilder1.build());
        addressList.add(addressBuilder2.build());
        ipv4Builder.setAddress(addressList);
        return ipv4Builder.build();
    }

    private Ipv6 constructIpv6(String ipv6One, String ipv6Two) {
        Ipv6Builder ipv6Builder = new Ipv6Builder();
        ipv6Builder.setForwarding(true);
        org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv6.AddressBuilder
                addressBuilder1 = new org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces
                .state._interface.ipv6.AddressBuilder();
        addressBuilder1.setKey(new org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces
                .state._interface.ipv6.AddressKey(new Ipv6AddressNoZone(ipv6One)));
        addressBuilder1.setIp(new Ipv6AddressNoZone(ipv6One));
        org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv6.AddressBuilder
                addressBuilder2 = new org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces
                .state._interface.ipv6.AddressBuilder();
        addressBuilder2.setKey(new org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.
                state._interface.ipv6.AddressKey(new Ipv6AddressNoZone(ipv6Two)));
        addressBuilder2.setIp(new Ipv6AddressNoZone(ipv6Two));
        List<org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state._interface.ipv6.Address>
                addressList = new ArrayList<>();
        addressList.add(addressBuilder1.build());
        addressList.add(addressBuilder2.build());
        ipv6Builder.setAddress(addressList);
        return ipv6Builder.build();
    }

    private void writeData(DataBroker dataBroker, InstanceIdentifier<InterfacesState> path, InterfacesState data) {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("Start write data to dataStore");
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, path, data, true);
        LOG.info("Submit");
        writeTransaction.submit();
    }

    private List<Interface> readData(DataBroker dataBroker, InstanceIdentifier<InterfacesState> path) {
        final ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        Optional<InterfacesState> interfaces = null;
        InterfacesState interfacesData = null;
        try {
            interfaces = readTransaction.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
            if (interfaces.isPresent()) {
                LOG.info("InterfacesState is not null");
                interfacesData = interfaces.get();
                if (null != interfacesData.getInterface() && 0 != interfacesData.getInterface().size()) {
                    LOG.info("Interfaces are not null");
                    return interfacesData.getInterface();
                }
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        LOG.info("InterfacesState is null");
        return null;
    }
}
