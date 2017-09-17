/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.node.interfaces.state.Node;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.P4pluginNetconfAdapterApiService;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;

public class NetconfAdapterServiceImpl implements P4pluginNetconfAdapterApiService{

    private static final Logger LOG = LoggerFactory.getLogger(NetconfAdapterServiceImpl.class);

    private final DataBroker dataBroker;
    private DeviceInterfaceDataOperator deviceInterfaceDataOperator;

    public NetconfAdapterServiceImpl(DataBroker dataBroker, DeviceInterfaceDataOperator deviceInterfaceDataOperator) {
        this.dataBroker = dataBroker;
        this.deviceInterfaceDataOperator = deviceInterfaceDataOperator;
    }

    @Override
    public Future<RpcResult<WriteInventoryOutput>> writeInventory(WriteInventoryInput var1) {
        LOG.info("Acquire interfaces data from ietf");
        NodeInterfacesState data = deviceInterfaceDataOperator.readInterfacesFromControllerDataStore();
        WriteInventoryOutputBuilder outputBuilder = new WriteInventoryOutputBuilder();
        if (null == data || null == data.getNode()) {
            outputBuilder.setMessage("No data in controller data store");
            return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
        }

        LOG.info("Ietf data is {}", data);
        String result = writeDataToInventory(data.getNode());
        outputBuilder.setMessage(result);
        return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
    }

    private String writeDataToInventory(List<Node> nodeList) {
        return null;
    }

}
