/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistro.elasticsearch.performanceanalyzer.rest;

import com.amazon.opendistro.elasticsearch.performanceanalyzer.collectors.StatExceptionCode;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.metrics.MetricsRestUtil;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.framework.util.SQLiteQueryUtils;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.rca.persistence.Persistable;
import com.amazon.opendistro.elasticsearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;

/**
 *  Request handler that supports querying RCAs
 *
 *  <p>To dump all RCA related tables from SQL :
 *  curl --url "localhost:9650/_opendistro/_performanceanalyzer/rca?all" -XGET
 *
 *  <p>To get response for all the available RCA, use:
 *  curl --url "localhost:9650/_opendistro/_performanceanalyzer/rca" -XGET
 *
 *  <p>To get response for a specific RCA, use:
 *  curl --url "localhost:9650/_opendistro/_performanceanalyzer/rca?name=HighHeapUsageClusterRca" -XGET
 */
public class QueryRcaRequestHandler extends MetricsHandler implements HttpHandler {

  private static final Logger LOG = LogManager.getLogger(QueryRcaRequestHandler.class);
  private static final int HTTP_CLIENT_CONNECTION_TIMEOUT = 200;
  private static final String DUMP_ALL = "all";
  private Persistable persistable;
  private MetricsRestUtil metricsRestUtil;

  public QueryRcaRequestHandler() {
    metricsRestUtil = new MetricsRestUtil();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();

    if (requestMethod.equalsIgnoreCase("GET")) {
      LOG.debug("RCA Query handler called.");
      exchange.getResponseHeaders().set("Content-Type", "application/json");

      try {
        synchronized (this) {
          String query = exchange.getRequestURI().getQuery();
          //first check if we want to dump all SQL tables for debugging purpose
          if (query != null && query.equals(DUMP_ALL)) {
            sendResponse(exchange, dumpAllRcaTables(), HttpURLConnection.HTTP_OK);
          }
          else {
            Map<String, String> params = getParamsMap(query);
            List<String> rcaList = metricsRestUtil.parseArrayParam(params, "name", true);
            // query all cluster level RCAs if no RCA is specified in name.
            if (rcaList.isEmpty()) {
              rcaList = SQLiteQueryUtils.getClusterLevelRca();
            }
            //check if RCA is valid
            if (!validParams(rcaList)) {
              JsonObject errResponse = new JsonObject();
              JsonArray errReason = new JsonArray();
              SQLiteQueryUtils.getClusterLevelRca().forEach(errReason::add);
              errResponse.addProperty("error", "Invalid RCA.");
              errResponse.add("valid_cluster_rca", errReason);
              sendResponse(exchange, errResponse.toString(),
                  HttpURLConnection.HTTP_BAD_REQUEST);
              return;
            }
            //check if we are querying from elected master
            if (!validNodeRole()) {
              JsonObject errResponse = new JsonObject();
              errResponse.addProperty("error", "Node being queried is not elected master.");
              sendResponse(exchange, errResponse.toString(),
                  HttpURLConnection.HTTP_BAD_REQUEST);
              return;
            }
            String response = getRcaData(persistable, rcaList).toString();
            sendResponse(exchange, response, HttpURLConnection.HTTP_OK);
          }
        }
      } catch (InvalidParameterException e) {
        LOG.error(
                (Supplier<?>)
                        () ->
                                new ParameterizedMessage(
                                        "QueryException {} ExceptionCode: {}.",
                                        e.toString(),
                                        StatExceptionCode.REQUEST_ERROR.toString()),
                e);
        String response = "{\"error\":\"" + e.getMessage() + "\"}";
        sendResponse(exchange, response, HttpURLConnection.HTTP_BAD_REQUEST);
      } catch (Exception e) {
        LOG.error(
                (Supplier<?>)
                        () ->
                                new ParameterizedMessage(
                                        "QueryException {} ExceptionCode: {}.",
                                        e.toString(),
                                        StatExceptionCode.REQUEST_ERROR.toString()),
                e);
        String response = "{\"error\":\"" + e.toString() + "\"}";
        sendResponse(exchange, response, HttpURLConnection.HTTP_INTERNAL_ERROR);
      }
    } else {
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
      exchange.close();
    }
  }

  // check whether RCAs are cluster level RCAs
  private boolean validParams(List<String> rcaList) {
    return rcaList.stream()
            .allMatch(SQLiteQueryUtils::isClusterLevelRca);
  }

  // check if we are querying from elected master
  private boolean validNodeRole() {
    ClusterDetailsEventProcessor.NodeDetails currentNode = ClusterDetailsEventProcessor
        .getCurrentNodeDetails();
    return currentNode.getIsMasterNode();
  }

  private JsonElement getRcaData(Persistable persistable, List<String> rcaList) {
    LOG.debug("RCA: in getRcaData");
    JsonObject jsonObject = new JsonObject();
    if (persistable != null) {
      rcaList.forEach(rca ->
          jsonObject.add(rca, persistable.read(rca))
      );
    }
    return jsonObject;
  }

  private String dumpAllRcaTables() {
    String jsonResponse = "";
    if (persistable != null) {
      jsonResponse = persistable.read();
    }
    return jsonResponse;
  }

  public void sendResponse(HttpExchange exchange, String response, int status) throws IOException {
    try (OutputStream os = exchange.getResponseBody()) {
      exchange.sendResponseHeaders(status, response.length());
      os.write(response.getBytes());
    } catch (Exception e) {
      response = e.toString();
      exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, response.length());
    }
  }

  public synchronized void setPersistable(Persistable persistable) {
    this.persistable = persistable;
  }
}
