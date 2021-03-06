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
package org.wso2.carbon.event.input.adapter.coap.internal.ds;

import org.wso2.carbon.user.core.service.RealmService;

/**
 * common place to hold some OSGI service references.
 */
public final class CoAPEventAdapterServiceValueHolder {

    private static RealmService realmService;

    private CoAPEventAdapterServiceValueHolder() {
    }

    public static void registerRealmService(
            RealmService realmService) {
        CoAPEventAdapterServiceValueHolder.realmService = realmService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

}
