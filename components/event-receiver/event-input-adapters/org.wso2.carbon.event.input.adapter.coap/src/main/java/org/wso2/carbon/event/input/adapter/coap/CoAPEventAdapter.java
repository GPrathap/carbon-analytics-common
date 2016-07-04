/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.event.input.adapter.coap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterConstants;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapter;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterConfiguration;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterListener;
import org.wso2.carbon.event.input.adapter.core.exception.InputEventAdapterException;
import org.wso2.carbon.event.input.adapter.core.exception.InputEventAdapterRuntimeException;
import org.wso2.carbon.event.input.adapter.core.exception.TestConnectionNotSupportedException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.ServletException;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public final class CoAPEventAdapter implements InputEventAdapter {

    private final InputEventAdapterConfiguration eventAdapterConfiguration;
    private final Map<String, String> globalProperties;
    private InputEventAdapterListener eventAdaptorListener;
    private final String id = UUID.randomUUID().toString();
    public static ExecutorService executorService;
    private static final Log log = LogFactory.getLog(CoAPEventAdapter.class);
    private boolean isConnected = false;
    private static CoapServer server;

    public CoAPEventAdapter(InputEventAdapterConfiguration eventAdapterConfiguration,
                            Map<String, String> globalProperties) {
        this.eventAdapterConfiguration = eventAdapterConfiguration;
        this.globalProperties = globalProperties;
        if(server == null){
            server = new CoapServer();
            server.start();
        }
    }

    @Override
    public void init(InputEventAdapterListener eventAdaptorListener) throws InputEventAdapterException {
        this.eventAdaptorListener = eventAdaptorListener;
        //ThreadPoolExecutor will be assigned  if it is null

        if (executorService == null) {
            int minThread;
            int maxThread;
            long defaultKeepAliveTime;
            int jobQueueSize;

            //If global properties are available those will be assigned else constant values will be assigned
            if (globalProperties.get(CoAPEventAdapterConstants.ADAPTER_MIN_THREAD_POOL_SIZE_NAME) != null) {
                minThread = Integer
                        .parseInt(globalProperties.get(CoAPEventAdapterConstants.ADAPTER_MIN_THREAD_POOL_SIZE_NAME));
            } else {
                minThread = CoAPEventAdapterConstants.ADAPTER_MIN_THREAD_POOL_SIZE;
            }

            if (globalProperties.get(CoAPEventAdapterConstants.ADAPTER_MAX_THREAD_POOL_SIZE_NAME) != null) {
                maxThread = Integer
                        .parseInt(globalProperties.get(CoAPEventAdapterConstants.ADAPTER_MAX_THREAD_POOL_SIZE_NAME));
            } else {
                maxThread = CoAPEventAdapterConstants.ADAPTER_MAX_THREAD_POOL_SIZE;
            }

            if (globalProperties.get(CoAPEventAdapterConstants.ADAPTER_KEEP_ALIVE_TIME_NAME) != null) {
                defaultKeepAliveTime = Integer
                        .parseInt(globalProperties.get(CoAPEventAdapterConstants.ADAPTER_KEEP_ALIVE_TIME_NAME));
            } else {
                defaultKeepAliveTime = CoAPEventAdapterConstants.DEFAULT_KEEP_ALIVE_TIME_IN_MILLS;
            }

            if (globalProperties.get(CoAPEventAdapterConstants.ADAPTER_EXECUTOR_JOB_QUEUE_SIZE_NAME) != null) {
                jobQueueSize = Integer
                        .parseInt(globalProperties.get(CoAPEventAdapterConstants.ADAPTER_EXECUTOR_JOB_QUEUE_SIZE_NAME));
            } else {
                jobQueueSize = CoAPEventAdapterConstants.ADAPTER_EXECUTOR_JOB_QUEUE_SIZE;
            }

            RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    try {
                        executor.getQueue().put(r);
                    } catch (InterruptedException e) {
                        log.error("Exception while adding event to executor queue : " + e.getMessage(), e);
                    }
                }

            };
            executorService = new ThreadPoolExecutor(minThread, maxThread, defaultKeepAliveTime, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(jobQueueSize), rejectedExecutionHandler);

        }
    }

    @Override
    public void testConnect() throws TestConnectionNotSupportedException {
        throw new TestConnectionNotSupportedException("not-supported");
    }

    @Override
    public void connect() {
        registerDynamicEndpoint(eventAdapterConfiguration.getName());
        isConnected = true;
    }

    @Override
    public void disconnect() {
        if (isConnected){
            unregisterDynamicEndpoint(eventAdapterConfiguration.getName());
            isConnected = false;
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CoAPEventAdapter))
            return false;

        CoAPEventAdapter that = (CoAPEventAdapter) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean isEventDuplicatedInCluster() {
        return false;
    }

    @Override
    public boolean isPolling() {
        return false;
    }

    private void registerDynamicEndpoint(String adapterName) {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

        String endpoint;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + adapterName;
        } else {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_TENANT_KEY
                    + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + tenantDomain
                    + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + adapterName;
        }
        server.add(new CoAPResourceServlet(eventAdaptorListener, tenantId,
                eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS),adapterName));
    }

    private void unregisterDynamicEndpoint(String adapterName) {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String endpoint;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + adapterName;
        } else {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_TENANT_KEY
                    + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + tenantDomain
                    + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + adapterName;
        }
        if (server != null) {
            server.remove(new CoAPResourceServlet(eventAdaptorListener, tenantId,
                    eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS),adapterName));
        }
    }
}