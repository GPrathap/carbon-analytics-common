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
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterConstants;
import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterUtils;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapter;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterConfiguration;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterListener;
import org.wso2.carbon.event.input.adapter.core.exception.InputEventAdapterException;
import org.wso2.carbon.event.input.adapter.core.exception.TestConnectionNotSupportedException;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class CoAPEventAdapter implements InputEventAdapter {

    private static final Log log = LogFactory.getLog(CoAPEventAdapter.class);
    private static final String id;
    public static volatile ExecutorService executorService;
    private static volatile CoapServer coapServer;
    private static int parentResourceIndex = 0;
    private static int minimumResourcesLength = 1;

    static {
        id = UUID.randomUUID().toString();
    }

    private final InputEventAdapterConfiguration eventAdapterConfiguration;
    private final Map<String, String> globalProperties;
    private InputEventAdapterListener eventAdaptorListener;
    private boolean isConnected = false;

    public CoAPEventAdapter(InputEventAdapterConfiguration eventAdapterConfiguration
            , Map<String, String> globalProperties) {
        this.eventAdapterConfiguration = eventAdapterConfiguration;
        this.globalProperties = globalProperties;
        coapInputAdpterInitializer();
    }

    public static String getServerUrl() {
        String hostName = ServerConfiguration.getInstance().getFirstProperty(CoAPEventAdapterConstants.HOST_NAME);
        try {
            if (hostName == null) {
                hostName = NetworkUtils.getLocalHostname();
            }
        } catch (SocketException e) {
            hostName = "localhost";
        }
        return hostName;
    }

    /**
     *
     */
    private void coapInputAdpterInitializer() {
        String protocol = eventAdapterConfiguration.getProperties()
                .get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS);
        int serverPort = CoAPEventAdapterConstants.COAP_PORT;
        if (protocol.equals(CoAPEventAdapterConstants.CoAP)) {
            serverPort = CoAPEventAdapterConstants.COAP_PORT;
        } else if (protocol.equals(CoAPEventAdapterConstants.CoAPS)) {
            serverPort = CoAPEventAdapterConstants.COAPS_PORT;
        }
        try {
            if ((coapServer == null) || (coapServer.getEndpoint(serverPort) == null) || (!coapServer.getEndpoint(serverPort).isStarted())) {
                if (coapServer == null) {
                    coapServer = new CoapServer();
                }
                if (protocol.equals(CoAPEventAdapterConstants.CoAP)) {
                    String addr = getServerUrl();
                    InetSocketAddress bindToAddress
                            = new InetSocketAddress(addr, CoAPEventAdapterConstants.COAP_PORT);
                    coapServer.addEndpoint(new CoapEndpoint(bindToAddress));
                } else if (protocol.equals(CoAPEventAdapterConstants.CoAPS)) {
                    DTLSConnector connector = CoAPEventAdapterUtils.getDTLSConnector();
                    coapServer.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));
                }
                // add special interceptor for message traces
                for (Endpoint ep : coapServer.getEndpoints()) {
                    ep.addInterceptor(new MessageTracer());
                }
                coapServer.start();
            }
        } catch (FileNotFoundException e) {
            log.error("Key store or trust store file can't be found, " + e);
        } catch (CertificateException e) {
            log.error("Error while retrieving certificates from keystore or truststore, " + e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Server certificate doesn't support for the cipher suite: TLS_PSK_WITH_AES_128_CCM_8" +
                    ",TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, " + e);
        } catch (UnrecoverableKeyException e) {
            log.error("Keymanager isn't using the correct password" + e);
        } catch (KeyStoreException e) {
            log.error("Private key does not exist in the key store," + e);
        } catch (IOException e) {
            log.error("Error while reading key store or trust store," + e);
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
        if (isConnected) {
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
        return true;
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
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR
                    + adapterName;
        } else {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR
                    + CoAPEventAdapterConstants.ENDPOINT_TENANT_KEY + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR
                    + tenantDomain + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + adapterName;
        }
        String[] resources = endpoint.split("/");
        Resource parentResource = coapServer.getRoot().getChild(resources[parentResourceIndex]);
        Resource childResource = parentResource;
        if (childResource == null && (resources.length > minimumResourcesLength)) {
            coapServer.add(new CoAPResourcePath(eventAdaptorListener, tenantId
                    , eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS)
                    , resources));
        }
        if (childResource == null && (resources.length == minimumResourcesLength)) {
            coapServer.add(new CoAPResourceServlet(eventAdaptorListener, tenantId
                    , eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS)
                    , resources[parentResourceIndex]));
        } else if (childResource != null && resources.length > minimumResourcesLength) {
            int j = minimumResourcesLength;
            for (; j < resources.length; j++) {
                if (parentResource != null) {
                    childResource = parentResource;
                    parentResource = parentResource.getChild(resources[j]);
                } else {
                    break;
                }
            }
            String[] coapEndpoint = Arrays.copyOfRange(resources, j - minimumResourcesLength, resources.length);
            if (coapEndpoint.length > minimumResourcesLength) {
                childResource.add(new CoAPResourcePath(eventAdaptorListener, tenantId
                        , eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS)
                        , coapEndpoint));
            } else if (coapEndpoint.length == minimumResourcesLength) {
                childResource.add(new CoAPResourceServlet(eventAdaptorListener, tenantId
                        , eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS)
                        , coapEndpoint[parentResourceIndex]));
            }
        } else if (childResource != null && resources.length == minimumResourcesLength) {
            childResource.add(new CoAPResourceServlet(eventAdaptorListener, tenantId
                    , eventAdapterConfiguration.getProperties().get(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS)
                    , resources[parentResourceIndex]));
        }
    }

    private void unregisterDynamicEndpoint(String adapterName) {

        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String endpoint;
        if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + adapterName;
        } else {
            endpoint = CoAPEventAdapterConstants.ENDPOINT_PREFIX + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR
                    + CoAPEventAdapterConstants.ENDPOINT_TENANT_KEY + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR
                    + tenantDomain + CoAPEventAdapterConstants.ENDPOINT_URL_SEPARATOR + adapterName;
        }
        try{
            if (coapServer != null) {
                String[] resources = endpoint.split("/");
                Resource child = coapServer.getRoot().getChild(resources[parentResourceIndex]);
                Resource parent = child;
                for (int i = 1; i < resources.length; i++) {
                    if (parent != null) {
                        parent = child;
                        child = child.getChild(resources[i]);
                    } else {
                        break;
                    }
                }
                if (parent != null && child != null){
                    if(!parent.delete(child)){
                        log.error("Requested resoure("+child+") doesn't exist");
                    }
                }else{
                    log.error("Requested resoure("+endpoint+") doesn't exist");
                }
            }
        }catch(NullPointerException e){
            log.error("Requested resoure("+endpoint+") can't be found, ", e);
        }
    }
}