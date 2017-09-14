/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.NodeInterfacesState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceInterfaceDataOperator {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceInterfaceDataOperator.class);

    private DataProcess dataProcess;

    public static final InstanceIdentifier<NodeInterfacesState> NODE_INTERFACE_IID = InstanceIdentifier
            .create(NodeInterfacesState.class);

    public DeviceInterfaceDataOperator(DataProcess dataProcess) {
        this.dataProcess = dataProcess;
    }

    public void writeInterfacesToDevice(String nodeId) {
        LOG.info("Start write data to device");
        dataProcess.writeToDevice(nodeId, NODE_INTERFACE_IID);
    }

    public NodeInterfacesState readInterfacesFromDevice(String nodeId) {
        LOG.info("Start read data from device");
        return dataProcess.readFromDevice(nodeId, NODE_INTERFACE_IID);
    }

    public NodeInterfacesState readInterfacesFromControllerDataStore() {
        LOG.info("Start read data from controller data store");
        return dataProcess.readFromDataStore(NODE_INTERFACE_IID);
    }
}
