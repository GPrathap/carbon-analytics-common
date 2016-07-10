/*
 * Copyright (c) 2005 - 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterConstants;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterListener;


public class CoAPResourcePath extends CoapResource {

    private static Log log = LogFactory.getLog(CoAPResourcePath.class);
    private static int parentResourceIndex = 0;
    private static int minimumResourcesLength = 2;
    private static int childResourceIndex = 1;

    public CoAPResourcePath(InputEventAdapterListener eventAdaptorListener, int tenantId, String exposedTransports
            , String[] endpoint) {
        super(endpoint[parentResourceIndex], CoAPEventAdapterConstants.COAP_RESOURCES_VISIBILITY);
        addResource(endpoint, eventAdaptorListener, tenantId, exposedTransports);
    }

    public CoAPResourcePath(String path) {
        super(path, CoAPEventAdapterConstants.COAP_RESOURCES_VISIBILITY);
        getAttributes().setTitle("Long path resource");
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        exchange.respond("hello world--->>><<<<"); // reply with 2.05 payload (text/plain)
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        exchange.respond("-->>><<<<");
    }

    private void addResource(String[] subDirectories, InputEventAdapterListener eventAdaptorListener, int tenantId
            , String exposedTransports) {
        String[] resources = subDirectories;
        Resource parentResource;
        Resource childResource;
        if (resources.length > minimumResourcesLength) {
            parentResource = new CoAPResourcePath(resources[childResourceIndex]);
            add(parentResource);
            for (int i = minimumResourcesLength; i < resources.length; i++) {
                if (i == resources.length - 1) {
                    childResource = new CoAPResourceServlet(eventAdaptorListener, tenantId, exposedTransports
                            , resources[i]);
                    parentResource.add(childResource);
                } else {
                    childResource = new CoAPResourcePath(resources[i]);
                    parentResource.add(childResource);
                }
                parentResource = childResource;
            }
        } else if (resources.length == minimumResourcesLength) {
            add(new CoAPResourceServlet(eventAdaptorListener, tenantId, exposedTransports
                    , resources[childResourceIndex]));
        }
    }
}
