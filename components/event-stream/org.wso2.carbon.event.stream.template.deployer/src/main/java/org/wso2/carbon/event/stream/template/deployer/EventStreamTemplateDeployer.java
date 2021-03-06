/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.event.stream.template.deployer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.event.execution.manager.core.DeployableTemplate;
import org.wso2.carbon.event.execution.manager.core.TemplateDeployer;
import org.wso2.carbon.event.execution.manager.core.TemplateDeploymentException;
import org.wso2.carbon.event.stream.core.exception.EventStreamConfigurationException;
import org.wso2.carbon.event.stream.core.exception.StreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.event.stream.template.deployer.internal.EventStreamTemplateDeployerValueHolder;
import org.wso2.carbon.event.stream.template.deployer.internal.util.EventStreamTemplateDeployerConstants;
import org.wso2.carbon.event.stream.template.deployer.internal.util.EventStreamTemplateDeployerHelper;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

public class EventStreamTemplateDeployer implements TemplateDeployer {

    private static final Log log = LogFactory.getLog(EventStreamTemplateDeployer.class);

    @Override
    public String getType() {
        return EventStreamTemplateDeployerConstants.EVENT_STREAM_DEPLOYER_TYPE;
    }


    @Override
    public void deployArtifact(DeployableTemplate template) throws TemplateDeploymentException {
        String stream = null;
        StreamDefinition streamDefinition = null;
        try {
            if (template == null) {
                throw new TemplateDeploymentException("No artifact received to be deployed.");
            }

            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            Registry registry = EventStreamTemplateDeployerValueHolder.getRegistryService()
                    .getConfigSystemRegistry(tenantId);

            if (!registry.resourceExists(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH)) {
                registry.put(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, registry.newCollection());
            }

            Collection infoCollection = registry.get(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, 0, -1);

            String artifactId = template.getArtifactId();
            String streamId = infoCollection.getProperty(artifactId);

            //~~~~~~~~~~~~~Cleaning up previously deployed stream, if any.

            if (streamId != null) {    //meaning, this particular template element has previously deployed a stream. We need to undeploy it if it has no other users.
                infoCollection.removeProperty(artifactId);    //cleaning up the map before undeploying
                registry.put(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, infoCollection);

                //Checking whether any other scenario configs/domains are using this stream....
                //this info is being kept in a map
                String mappingResourcePath = EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH + RegistryConstants.PATH_SEPARATOR + streamId;
                if (registry.resourceExists(mappingResourcePath)) {
                    EventStreamTemplateDeployerHelper.cleanMappingResourceAndUndeploy(registry, mappingResourcePath, artifactId, streamId);
                }
            }

            //~~~~~~~~~~~~~Deploying new stream

            stream = template.getArtifact();
            streamDefinition = EventDefinitionConverterUtils.convertFromJson(stream);
            streamId = streamDefinition.getStreamId();

            //if stream has not deployed already, we deploy it and update the maps in the registry.
            if (EventStreamTemplateDeployerValueHolder.getEventStreamService().
                    getStreamDefinition(streamId) == null) {

                EventStreamTemplateDeployerValueHolder.getEventStreamService().addEventStreamDefinition(streamDefinition);

                EventStreamTemplateDeployerHelper.updateRegistryMaps(registry, infoCollection, artifactId, streamId);

            } else {    //stream has already being deployed for another scenario/domain
                StreamDefinition existingStreamDef = EventStreamTemplateDeployerValueHolder.getEventStreamService().getStreamDefinition(streamId);
                if (streamDefinition.equals(existingStreamDef)) {    //if so, just update the registry maps.
                    EventStreamTemplateDeployerHelper.updateRegistryMaps(registry, infoCollection, artifactId, streamId);
                } else {
                    throw new TemplateDeploymentException("Failed to deploy Event Stream with ID: " + streamId +
                                                          ", as there exists another stream with the same ID but different Stream Definition. Artifact ID: " + artifactId);
                }
            }

        } catch (MalformedStreamDefinitionException e) {
            throw new TemplateDeploymentException("Stream definition given in the template is not in valid format. Stream definition: " + stream, e);
        } catch (EventStreamConfigurationException e) {
            throw new TemplateDeploymentException("Exception occurred when configuring stream " + streamDefinition.getName(), e);
        } catch (StreamDefinitionAlreadyDefinedException e) {
            throw new TemplateDeploymentException("Same template stream name " + streamDefinition.getName()
                                                  + " has been defined for another definition ", e);
        } catch (RegistryException e) {
            throw new TemplateDeploymentException("Could not load the Registry for Tenant Domain: "
                                                  + PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true)
                                                  + ", when deploying Event Stream with artifact ID: " + template.getArtifactId(), e);
        }
    }


    @Override
    public void deployIfNotDoneAlready(DeployableTemplate template)
            throws TemplateDeploymentException {
        String stream = null;
        StreamDefinition streamDefinition = null;
        try {
            if (template == null) {
                throw new TemplateDeploymentException("No artifact received to be deployed.");
            }

            stream = template.getArtifact();
            streamDefinition = EventDefinitionConverterUtils.convertFromJson(stream);

            if (EventStreamTemplateDeployerValueHolder.getEventStreamService().
                    getStreamDefinition(streamDefinition.getStreamId()) == null) {
                deployArtifact(template);
            }  else {
                log.info("Common Artifact: EventStream with Stream ID " + streamDefinition.getStreamId() + " of Domain " + template.getConfiguration().getDomain()
                         + " was not deployed as it is already being deployed.");
            }
        } catch (MalformedStreamDefinitionException e) {
            throw new TemplateDeploymentException("Stream definition given in the template is not in valid format. Stream definition: " + stream, e);
        } catch (EventStreamConfigurationException e) {
            throw new TemplateDeploymentException("Failed to get stream definition for StreamID: " + streamDefinition.getStreamId() + ", hence deployment failed.", e);
        }
    }


    @Override
    public void undeployArtifact(String artifactId) throws TemplateDeploymentException {
        String streamId;
        try {
            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            Registry registry = EventStreamTemplateDeployerValueHolder.getRegistryService()
                    .getConfigSystemRegistry(tenantId);

            if (!registry.resourceExists(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH)) {
                registry.put(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, registry.newCollection());
            }

            Collection infoCollection = registry.get(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, 0, -1);

            streamId = infoCollection.getProperty(artifactId);

            if (streamId != null) {
                infoCollection.removeProperty(artifactId);    //cleaning up the map
                registry.put(EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH, infoCollection);

                String mappingResourcePath = EventStreamTemplateDeployerConstants.META_INFO_COLLECTION_PATH + RegistryConstants.PATH_SEPARATOR + streamId;
                if (registry.resourceExists(mappingResourcePath)) {
                    EventStreamTemplateDeployerHelper.cleanMappingResourceAndUndeploy(registry, mappingResourcePath, artifactId, streamId);
                } else {
                    log.warn("Registry data in inconsistent. Resource '" + mappingResourcePath + "' which needs to be deleted is not found.");
                }
            } else {
                log.warn("Registry data in inconsistent. No stream ID associated to artifact ID: " + artifactId + ". Hence nothing to be undeployed.");
            }
        } catch (RegistryException e) {
            throw new TemplateDeploymentException("Could not load the Registry for Tenant Domain: "
                                                  + PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain(true)
                                                  + ", when trying to undeploy Event Stream with artifact ID: " + artifactId, e);
        }
    }
}
