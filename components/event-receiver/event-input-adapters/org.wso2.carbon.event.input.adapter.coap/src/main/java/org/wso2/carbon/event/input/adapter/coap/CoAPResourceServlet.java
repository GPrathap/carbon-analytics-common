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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterConstants;
import org.wso2.carbon.event.input.adapter.core.InputEventAdapterListener;


public class CoAPResourceServlet extends CoapResource {

    private static Log log = LogFactory.getLog(CoAPResourceServlet.class);

    private InputEventAdapterListener eventAdaptorListener;
    private int tenantId;

    public CoAPResourceServlet(InputEventAdapterListener eventAdaptorListener, int tenantId, String exposedTransports
            , String recourseName) {
        super(recourseName, CoAPEventAdapterConstants.COAP_RESOURCES_VISIBILITY);
        this.eventAdaptorListener = eventAdaptorListener;
        this.tenantId = tenantId;
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        exchange.respond("hello world------>>>"); // reply with 2.05 payload (text/plain)
    }
    @Override
    public void handlePOST(CoapExchange exchange) {
        exchange.respond("--00-00-00-00-00--->>><<<<");
        exchange.accept(); // make it a separate response
        exchange.getRequestOptions();
        String data = exchange.getRequestText();
        if (data == null) {
            log.warn("Event Object is empty/null");
            return;
        }
        log.info("Message : " + data);
        exchange.respond("POST: "+ data);
        CoAPEventAdapter.executorService.submit(new CoAPRequestProcessor(eventAdaptorListener, data, tenantId));
    }

    public static class CoAPRequestProcessor implements Runnable {
        private InputEventAdapterListener inputEventAdapterListener;
        private String payload;
        private int tenantId;

        public CoAPRequestProcessor(InputEventAdapterListener inputEventAdapterListener, String payload, int tenantId) {
            this.inputEventAdapterListener = inputEventAdapterListener;
            this.payload = payload;
            this.tenantId = tenantId;
        }

        public void run() {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

                if (log.isDebugEnabled()) {
                    log.debug("Event received in CoAP Event Adapter - " + payload);
                }
                if (payload.trim() != null) {
                    inputEventAdapterListener.onEvent(payload);
                } else {
                    log.warn("Dropping the empty/null event received through CoAP adapter");
                }
            } catch (Exception e) {
                log.error("Error while parsing CoAP request for processing: " + e.getMessage(), e);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }
}
