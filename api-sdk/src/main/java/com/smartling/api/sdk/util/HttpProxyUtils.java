/*
 * Copyright 2012 Smartling, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smartling.api.sdk.util;

import com.smartling.api.sdk.ProxyConfiguration;
import com.smartling.api.sdk.exceptions.SmartlingApiException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

class HttpProxyUtils
{
    HttpProxyUtils()
    {

    }

    /**
     * Get a request config given the applicable request and proxy config if any
     * @param httpRequest request
     * @param proxyConfiguration configuration of proxy to use
     * @return org.apache.http.client.config.RequestConfig
     */
    RequestConfig getProxyRequestConfig(HttpRequestBase httpRequest, final ProxyConfiguration proxyConfiguration)
    {
        if (hasActiveProxyConfiguration(proxyConfiguration))
        {
            HttpHost proxyHttpHost = new HttpHost(proxyConfiguration.getHost(), proxyConfiguration.getPort());
            return RequestConfig.custom().setProxy(proxyHttpHost).build();
        }
        return null;
    }

    private SSLContext getSSLConfig() throws SmartlingApiException {
        SSLContext sslContext = null;

        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            initSSLContext(sslContext);
        } catch (NoSuchAlgorithmException e) {
            throw new SmartlingApiException("Error while creating HTTPClient, can not initialize TLS version", e);
        }

        return sslContext;
    }

    private void initSSLContext(SSLContext sslContext) throws SmartlingApiException {
        try {
            sslContext.init(null, null, null);
        } catch (KeyManagementException e) {
            throw new SmartlingApiException("Error while creating HTTPClient", e);
        }
    }

    /**
     * Get an HttpClient given a proxy config if any
     * @param proxyConfiguration configuration of proxy to use
     * @return org.apache.http.impl.client.CloseableHttpClient
     */
    CloseableHttpClient getHttpClient(final ProxyConfiguration proxyConfiguration) throws SmartlingApiException {
        HttpClientBuilder httpClientBuilder = getHttpClientBuilder();

        if (proxyAuthenticationRequired(proxyConfiguration))
        {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                new AuthScope(proxyConfiguration.getHost(), proxyConfiguration.getPort()),
                new UsernamePasswordCredentials(proxyConfiguration.getUsername(), proxyConfiguration.getPassword()));

            httpClientBuilder = httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        httpClientBuilder.setSSLContext(getSSLConfig());

        return httpClientBuilder.build();
    }

    HttpClientBuilder getHttpClientBuilder()
    {
        return HttpClientBuilder.create();
    }

    private static boolean hasActiveProxyConfiguration(final ProxyConfiguration proxyConfiguration)
    {
        return proxyConfiguration != null && proxyConfiguration.getHost() != null && proxyConfiguration.getPort() != 0;
    }

    private static boolean proxyAuthenticationRequired(final ProxyConfiguration proxyConfiguration)
    {
        return hasActiveProxyConfiguration(proxyConfiguration)
                && StringUtils.isNotEmpty(proxyConfiguration.getUsername())
                && StringUtils.isNotEmpty(proxyConfiguration.getPassword());
    }
}
