// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: inter_node_rpc_service.proto

package com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc;

public interface MetricsRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:com.amazon.opendistro.elasticsearch.performanceanalyzer.grpc.MetricsRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>repeated string metric_list = 1;</code>
   */
  java.util.List<java.lang.String>
      getMetricListList();
  /**
   * <code>repeated string metric_list = 1;</code>
   */
  int getMetricListCount();
  /**
   * <code>repeated string metric_list = 1;</code>
   */
  java.lang.String getMetricList(int index);
  /**
   * <code>repeated string metric_list = 1;</code>
   */
  com.google.protobuf.ByteString
      getMetricListBytes(int index);

  /**
   * <code>repeated string agg_list = 2;</code>
   */
  java.util.List<java.lang.String>
      getAggListList();
  /**
   * <code>repeated string agg_list = 2;</code>
   */
  int getAggListCount();
  /**
   * <code>repeated string agg_list = 2;</code>
   */
  java.lang.String getAggList(int index);
  /**
   * <code>repeated string agg_list = 2;</code>
   */
  com.google.protobuf.ByteString
      getAggListBytes(int index);

  /**
   * <code>repeated string dim_list = 3;</code>
   */
  java.util.List<java.lang.String>
      getDimListList();
  /**
   * <code>repeated string dim_list = 3;</code>
   */
  int getDimListCount();
  /**
   * <code>repeated string dim_list = 3;</code>
   */
  java.lang.String getDimList(int index);
  /**
   * <code>repeated string dim_list = 3;</code>
   */
  com.google.protobuf.ByteString
      getDimListBytes(int index);
}