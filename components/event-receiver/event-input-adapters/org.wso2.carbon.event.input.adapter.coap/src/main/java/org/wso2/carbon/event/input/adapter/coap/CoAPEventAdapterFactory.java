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


import org.wso2.carbon.event.input.adapter.coap.internal.util.CoAPEventAdapterConstants;
import org.wso2.carbon.event.input.adapter.core.*;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.ResourceBundle;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * The CoAP event adapter factory class to create a CoAP input adapter
 */
public class CoAPEventAdapterFactory extends InputEventAdapterFactory {

    private ResourceBundle resourceBundle =
            ResourceBundle.getBundle("org.wso2.carbon.event.input.adapter.coap.i18n.Resources", Locale.getDefault());
    private int coapPort;
    private int coapsPort;


    public CoAPEventAdapterFactory() {
        int portOffset = getPortOffset();
        coapPort = CoAPEventAdapterConstants.DEFAULT_CaAP_PORT + portOffset;
        coapsPort = CoAPEventAdapterConstants.DEFAULT_CaAPS_PORT + portOffset;
    }

    @Override
    public String getType() {
        return CoAPEventAdapterConstants.ADAPTER_TYPE_CaAP;
    }

    @Override
    public List<String> getSupportedMessageFormats() {
        List<String> supportInputMessageTypes = new ArrayList<String>();
        supportInputMessageTypes.add(MessageType.XML);
        supportInputMessageTypes.add(MessageType.JSON);
        supportInputMessageTypes.add(MessageType.TEXT);
        return supportInputMessageTypes;
    }

    @Override
    public List<Property> getPropertyList() {

        List<Property> propertyList = new ArrayList<>();
        // Transport Exposed
        Property exposedTransportsProperty = new Property(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS);
        exposedTransportsProperty.setRequired(true);
        exposedTransportsProperty.setDisplayName(
                resourceBundle.getString(CoAPEventAdapterConstants.EXPOSED_TRANSPORTS));
        exposedTransportsProperty.setOptions(new String[]{CoAPEventAdapterConstants.CoAPS
                , CoAPEventAdapterConstants.CoAP});
        exposedTransportsProperty.setDefaultValue(CoAPEventAdapterConstants.CoAP);

        propertyList.add(exposedTransportsProperty);
        return propertyList;
    }

    @Override
    public String getUsageTips() {
        return resourceBundle.getString(CoAPEventAdapterConstants.ADAPTER_USAGE_TIPS_PREFIX) + coapPort
                + resourceBundle.getString(CoAPEventAdapterConstants.ADAPTER_USAGE_TIPS_MID1) + coapsPort
                + resourceBundle.getString(CoAPEventAdapterConstants.ADAPTER_USAGE_TIPS_MID2) + coapPort
                + resourceBundle.getString(CoAPEventAdapterConstants.ADAPTER_USAGE_TIPS_MID3) + coapsPort
                + resourceBundle.getString(CoAPEventAdapterConstants.ADAPTER_USAGE_TIPS_POSTFIX);
    }

    @Override
    public InputEventAdapter createEventAdapter(InputEventAdapterConfiguration eventAdapterConfiguration,
                                                Map<String, String> globalProperties) {
        return new CoAPEventAdapter(eventAdapterConfiguration, globalProperties);
    }

    private int getPortOffset() {
        return CarbonUtils.getPortFromServerConfig(CoAPEventAdapterConstants.CARBON_CONFIG_PORT_OFFSET_NODE) + 1;
    }
}
