/*
 * Copyright 2013 the original author or authors.
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
package org.vertx.java.core.datagram.impl;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.datagram.BoundDatagramChannel;
import org.vertx.java.core.datagram.ConnectedDatagramChannel;
import org.vertx.java.core.datagram.DatagramEndpoint;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.impl.VertxInternal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public class DefaultDatagramEndpoint implements DatagramEndpoint {
  private final VertxInternal vertx;
  private final DefaultContext context;
  private Bootstrap bootstrap;
  private final Map<Channel, AbstractDatagramChannel> datagramMap = new ConcurrentHashMap<>();

  private int sendBufferSize = -1;
  private int receiveBufferSize = -1;
  private boolean reuseAddress;
  private int trafficClass = -1;
  private boolean broadcast;
  private boolean loopbackModeDisabled;
  private int ttl = 1;
  private InetAddress address;
  private NetworkInterface iface;
  private boolean configurable = true;

  public DefaultDatagramEndpoint(VertxInternal vertx) {
    this.vertx = vertx;
    context = vertx.getOrCreateContext();
    bootstrap = new Bootstrap();
    bootstrap.group(context.getEventLoop());
    bootstrap.channel(NioDatagramChannel.class);
  }

  private void checkConfigurable() {
    if (!configurable) {
      throw new IllegalStateException("Can't set property after connect or bind has been called");
    }
  }

  @Override
  public DatagramEndpoint bind(InetSocketAddress local, Handler<AsyncResult<BoundDatagramChannel>> handler) {
    configurable = false;
    ChannelFuture future = bootstrap.clone().handler(new BoundDatagramChannelHandler(vertx, datagramMap)).bind(local);
    DefaultBoundDatagramChannel channel = new DefaultBoundDatagramChannel(vertx, (DatagramChannel) future.channel(), context);
    datagramMap.put(future.channel(), channel);
    channel.addListener(future, handler);
    return this;
  }

  @Override
  public DatagramEndpoint connect(InetSocketAddress remote, Handler<AsyncResult<ConnectedDatagramChannel>> handler) {
    configurable = false;
    ChannelFuture future = bootstrap.clone().handler(new ConnectedDatagramChannelHandler(vertx, datagramMap)).connect(remote);
    DefaultConnectedDatagramChannel channel = new DefaultConnectedDatagramChannel(vertx, (DatagramChannel) future.channel(), context);
    datagramMap.put(future.channel(), channel);
    channel.addListener(future, handler);
    return this;
  }

  @Override
  public int getSendBufferSize() {
    return sendBufferSize;
  }

  @Override
  public DatagramEndpoint setSendBufferSize(int sendBufferSize) {
    checkConfigurable();

    this.sendBufferSize = sendBufferSize;
    bootstrap.option(ChannelOption.SO_SNDBUF, sendBufferSize);
    return this;
  }

  @Override
  public int getReceiveBufferSize() {
    return receiveBufferSize;
  }

  @Override
  public DatagramEndpoint setReceiveBufferSize(int receiveBufferSize) {
    checkConfigurable();

    this.receiveBufferSize = receiveBufferSize;
    bootstrap.option(ChannelOption.SO_RCVBUF, receiveBufferSize);
    return this;
  }

  @Override
  public int getTrafficClass() {
    return trafficClass;
  }

  @Override
  public DatagramEndpoint setTrafficClass(int trafficClass) {
    checkConfigurable();

    this.trafficClass = trafficClass;
    bootstrap.option(ChannelOption.IP_TOS, trafficClass);
    return this;
  }

  @Override
  public boolean isReuseAddress() {
    return reuseAddress;
  }

  @Override
  public DatagramEndpoint setReuseAddress(boolean reuseAddress) {
    checkConfigurable();

    this.reuseAddress = reuseAddress;
    bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddress);
    return this;
  }

  @Override
  public boolean isBroadcast() {
    return broadcast;
  }

  @Override
  public DatagramEndpoint setBroadcast(boolean broadcast) {
    checkConfigurable();

    this.broadcast = broadcast;
    bootstrap.option(ChannelOption.SO_BROADCAST, broadcast);
    return this;
  }

  @Override
  public boolean isLoopbackModeDisabled() {
    return loopbackModeDisabled;
  }

  @Override
  public DatagramEndpoint setLoopbackModeDisabled(boolean loopbackModeDisabled) {
    checkConfigurable();

    this.loopbackModeDisabled = loopbackModeDisabled;
    bootstrap.option(ChannelOption.IP_MULTICAST_LOOP_DISABLED, loopbackModeDisabled);
    return this;
  }

  @Override
  public int getTimeToLive() {
    return ttl;
  }

  @Override
  public DatagramEndpoint setTimeToLive(int ttl) {
    checkConfigurable();

    this.ttl = ttl;
    bootstrap.option(ChannelOption.IP_MULTICAST_TTL, ttl);
    return this;
  }

  @Override
  public InetAddress getInterface() {
    return address;
  }

  @Override
  public DatagramEndpoint setInterface(InetAddress interfaceAddress) {
    checkConfigurable();

    address = interfaceAddress;
    bootstrap.option(ChannelOption.IP_MULTICAST_ADDR, interfaceAddress);
    return this;
  }

  @Override
  public NetworkInterface getNetworkInterface() {
    return iface;
  }

  @Override
  public DatagramEndpoint setNetworkInterface(NetworkInterface iface) {
    checkConfigurable();

    this.iface = iface;
    bootstrap.option(ChannelOption.IP_MULTICAST_IF, iface);
    return this;
  }
}
