/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.driver.impl;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
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

    public void write(String nodeId, InstanceIdentifier<Interfaces> path) {
        LOG.info("Get dataBroker");
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            LOG.info("Data broker is null, return");
            return;
        }
        LOG.info("Construct data");
        String data = constructData();
        LOG.info("Process write data");
        writeData(nodeDataBroker, path);
    }

    public void read(String nodeId, InstanceIdentifier<Interfaces> path) {
        LOG.info("Get dataBroker");
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            LOG.info("Data broker is null, return");
            return;
        }
        LOG.info("Process read data");
        readData(nodeDataBroker, path);
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

    private String constructData() {
        return null;
    }

    private void writeData(DataBroker dataBroker, InstanceIdentifier<Interfaces> path) {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        //writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, path, data, true);
        writeTransaction.submit();
    }

    private void readData(DataBroker dataBroker, InstanceIdentifier<Interfaces> path) {
        final ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        Optional<Interfaces> interfaces = null;
        Interfaces interfacesData = null;
        try {
            interfaces = readTransaction.read(LogicalDatastoreType.CONFIGURATION, path).checkedGet();
            if (interfaces.isPresent()) {
                interfacesData = interfaces.get();

            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        return;
    }
}
