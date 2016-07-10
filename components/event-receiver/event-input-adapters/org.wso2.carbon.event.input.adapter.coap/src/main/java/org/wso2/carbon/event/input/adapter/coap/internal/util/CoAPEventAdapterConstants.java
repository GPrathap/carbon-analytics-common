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
package org.wso2.carbon.event.input.adapter.coap.internal.util;


import org.eclipse.californium.core.network.config.NetworkConfig;

public final class CoAPEventAdapterConstants {

    private CoAPEventAdapterConstants() {

    }

    public static final String ADAPTER_TYPE_CaAP = "coap";
    public static final String ADAPTER_USAGE_TIPS_PREFIX = "coap.usage.tips_prefix";
    public static final String ADAPTER_USAGE_TIPS_MID1 = "coap.usage.tips_mid1";
    public static final String ADAPTER_USAGE_TIPS_MID2 = "coap.usage.tips_mid2";
    public static final String ADAPTER_USAGE_TIPS_MID3 = "coap.usage.tips_mid3";
    public static final String ADAPTER_USAGE_TIPS_POSTFIX = "coap.usage.tips_postfix";
    public static final int ADAPTER_MIN_THREAD_POOL_SIZE = 8;
    public static final int ADAPTER_MAX_THREAD_POOL_SIZE = 100;
    public static final int ADAPTER_EXECUTOR_JOB_QUEUE_SIZE = 10000;
    public static final long DEFAULT_KEEP_ALIVE_TIME_IN_MILLS = 20000;
    public static final String ENDPOINT_PREFIX = "coap-endpoints";
    public static final String ENDPOINT_URL_SEPARATOR = "/";
    public static final String ENDPOINT_TENANT_KEY = "t";
    public static final String ADAPTER_MIN_THREAD_POOL_SIZE_NAME = "minThread";
    public static final String ADAPTER_MAX_THREAD_POOL_SIZE_NAME = "maxThread";
    public static final String ADAPTER_KEEP_ALIVE_TIME_NAME = "keepAliveTimeInMillis";
    public static final String ADAPTER_EXECUTOR_JOB_QUEUE_SIZE_NAME = "jobQueueSize";
    public static final String EXPOSED_TRANSPORTS = "transports";
    public static final String CoAPS = "coaps";
    public static final String CoAP = "coap";
    public static final String CARBON_CONFIG_PORT_OFFSET_NODE = "Ports.Offset";
    public static final int DEFAULT_CaAP_PORT = 5683;
    public static final int DEFAULT_CaAPS_PORT = 5684;
    public static final int COAPS_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_SECURE_PORT);
    public static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    public static final String HOST_NAME = "HostName";
    public static final String IN_MEMORY_PKS_STORE_PASSWORD = "sesame";
    public static final String IN_MEMORY_PKS_STORE_PASSWORD_HOLDER = "password";
    public static final String KEY_STORE_TYPE = "JKS";
    public static final String CA_CERTIFICATE_NAME = "root";
    public static final String TRUST_STORE_PASSWORD = "Security.TrustStore.Password";
    public static final String TRUST_STORE_LOCATION = "Security.TrustStore.Location";
    public static final String KEY_STORE_LOCATION = "Security.KeyStore.Location";
    public static final String KEY_STORE_PASSWORD = "Security.KeyStore.Password";
    public static final String SERVER_CERTIFICATE_NAME = "server";
    public static final boolean COAP_RESOURCES_VISIBILITY = true;
    public static final String DEFAULT_CHARSET = "UTF-8";



}
