/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.driver.impl;

import com.google.common.util.concurrent.Futures;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.p4plugin.driver.api.rev170908.P4pluginDriverApiService;
import org.opendaylight.yang.gen.v1.urn.p4plugin.driver.api.rev170908.ReadDeviceInterfaceInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.driver.api.rev170908.ReadDeviceInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.driver.api.rev170908.ReadDeviceInterfaceOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverServiceImpl implements P4pluginDriverApiService {

    private static final Logger LOG = LoggerFactory.getLogger(DriverServiceImpl.class);

    private DeviceInterfaceProcess deviceInterfaceProcess;

    public DriverServiceImpl(DeviceInterfaceProcess deviceInterfaceProcess) {
        this.deviceInterfaceProcess = deviceInterfaceProcess;
    }

    @Override
    public Future<RpcResult<ReadDeviceInterfaceOutput>> readDeviceInterface(ReadDeviceInterfaceInput var1) {
        LOG.info("Start read interfaces");
        List<Interface> interfaceList = deviceInterfaceProcess.readDeviceInterfaces(var1.getDeviceId());
        if (null == interfaceList) {
            //return Futures.immediateFuture(RpcResultBuilder.<>failed().withError(RpcError.ErrorType.APPLICATION, "").build());
        }
        ReadDeviceInterfaceOutputBuilder outputBuilder = new ReadDeviceInterfaceOutputBuilder();
        //outputBuilder.set();
        return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
    }
}
