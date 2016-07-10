/*
  ~ Copyright (c) 2016  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~ WSO2 Inc. licenses this file to you under the Apache License,
  ~ Version 2.0 (the "License"); you may not use this file except
  ~ in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied. See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
*/
package org.wso2.carbon.event.input.adapter.coap.internal.util;

import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.UnrecoverableKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

public class CoAPEventAdapterUtils {

    public static DTLSConnector getDTLSConnector() throws UnrecoverableKeyException, NoSuchAlgorithmException
            , KeyStoreException, CertificateException, IOException {
        InputStream trustStoreFile = null;
        InputStream keyStoreFile = null;
        try {
            String trustStorePassword = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(CoAPEventAdapterConstants.TRUST_STORE_PASSWORD);
            String trustStoreLocation = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(CoAPEventAdapterConstants.TRUST_STORE_LOCATION);
            String keyStorePassword = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(CoAPEventAdapterConstants.KEY_STORE_PASSWORD);
            String keyStoreLocation = CarbonUtils.getServerConfiguration()
                    .getFirstProperty(CoAPEventAdapterConstants.KEY_STORE_LOCATION);
            // Pre-shared secrets
            InMemoryPskStore pskStore = new InMemoryPskStore();
            pskStore.setKey(CoAPEventAdapterConstants.IN_MEMORY_PKS_STORE_PASSWORD_HOLDER
                    , CoAPEventAdapterConstants.IN_MEMORY_PKS_STORE_PASSWORD
                            .getBytes(Charset.forName(CoAPEventAdapterConstants.DEFAULT_CHARSET)));
            // load the trust store
            KeyStore trustStore = KeyStore.getInstance(CoAPEventAdapterConstants.KEY_STORE_TYPE);
            trustStoreFile = new FileInputStream(trustStoreLocation);
            trustStore.load(trustStoreFile, trustStorePassword.toCharArray());
            // You can load multiple certificates if needed
            Certificate[] trustedCertificates = new Certificate[1];
            trustedCertificates[0] = trustStore.getCertificate(CoAPEventAdapterConstants.CA_CERTIFICATE_NAME);
            // load the key store
            KeyStore keyStore = KeyStore.getInstance(CoAPEventAdapterConstants.KEY_STORE_TYPE);
            keyStoreFile = new FileInputStream(keyStoreLocation);
            keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
            DtlsConnectorConfig.Builder config = new DtlsConnectorConfig
                    .Builder(new InetSocketAddress(CoAPEventAdapterConstants.COAPS_PORT));
            config.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
            config.setPskStore(pskStore);
            config.setIdentity((PrivateKey) keyStore.getKey(CoAPEventAdapterConstants.SERVER_CERTIFICATE_NAME
                    , keyStorePassword.toCharArray()),
                    keyStore.getCertificateChain(CoAPEventAdapterConstants.SERVER_CERTIFICATE_NAME), true);
            config.setTrustStore(trustedCertificates);
            return new DTLSConnector(config.build());
        } finally {
            if (trustStoreFile != null) {
                trustStoreFile.close();
            }
            if (keyStoreFile != null) {
                keyStoreFile.close();
            }
        }
    }
}
