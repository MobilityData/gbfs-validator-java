/*
 *
 *  *
 *  *
 *  *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  *  * You may not use this work except in compliance with the Licence.
 *  *  * You may obtain a copy of the Licence at:
 *  *  *
 *  *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the Licence for the specific language governing permissions and
 *  *  * limitations under the Licence.
 *  *
 *
 */

package org.mobilitydata.gbfs.validator.api.handler;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "loader")
public class LoaderProperties {

  private Http http = new Http();
  private ThreadPool threadPool = new ThreadPool();

  public Http getHttp() {
    return http;
  }

  public void setHttp(Http http) {
    this.http = http;
  }

  public ThreadPool getThreadPool() {
    return threadPool;
  }

  public void setThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  public static class Http {

    private int maxTotalConnections = 50;
    private int maxConnectionsPerRoute = 20;
    private int connectTimeoutSeconds = 5;
    private int responseTimeoutSeconds = 5;
    private Map<String, String> headers = new HashMap<>();

    public int getMaxTotalConnections() {
      return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
      this.maxTotalConnections = maxTotalConnections;
    }

    public int getMaxConnectionsPerRoute() {
      return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
      this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public int getConnectTimeoutSeconds() {
      return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
      this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getResponseTimeoutSeconds() {
      return responseTimeoutSeconds;
    }

    public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
      this.responseTimeoutSeconds = responseTimeoutSeconds;
    }

    public Map<String, String> getHeaders() {
      return headers;
    }

    public void setHeaders(Map<String, String> headers) {
      this.headers = headers;
    }
  }

  public static class ThreadPool {

    private int size = 20;

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }
  }
}
