/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.internal.ComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ServiceResourceProvider;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * AmbariContext unit tests
 */
//todo: switch over to EasyMockSupport
public class AmbariContextTest {

  private static final String CLUSTER_NAME = "testCluster";
  private static final String STACK_NAME = "testStack";
  private static final String STACK_VERSION = "testVersion";

  private static final AmbariContext context = new AmbariContext();
  private static final AmbariManagementController controller = createStrictMock(AmbariManagementController.class);
  private static final HostResourceProvider hostResourceProvider = createStrictMock(HostResourceProvider.class);
  private static final ServiceResourceProvider serviceResourceProvider = createStrictMock(ServiceResourceProvider.class);
  private static final ComponentResourceProvider componentResourceProvider = createStrictMock(ComponentResourceProvider.class);
  private static final HostComponentResourceProvider hostComponentResourceProvider = createStrictMock(HostComponentResourceProvider.class);
  private static final ClusterTopology topology = createNiceMock(ClusterTopology.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final Stack stack = createNiceMock(Stack.class);
  private static final Clusters clusters = createStrictMock(Clusters.class);
  private static final Cluster cluster = createStrictMock(Cluster.class);

  private static final Collection<String> blueprintServices = new HashSet<String>();
  private static final Map<String, Service> clusterServices = new HashMap<String, Service>();

  @Before
  public void setUp() throws Exception {
    // "inject" context state
    Class clazz = AmbariContext.class;
    Field f = clazz.getDeclaredField("controller");
    f.setAccessible(true);
    f.set(null, controller);

    f = clazz.getDeclaredField("hostResourceProvider");
    f.setAccessible(true);
    f.set(null, hostResourceProvider);

    f = clazz.getDeclaredField("serviceResourceProvider");
    f.setAccessible(true);
    f.set(null, serviceResourceProvider);

    f = clazz.getDeclaredField("componentResourceProvider");
    f.setAccessible(true);
    f.set(null, componentResourceProvider);

    f = clazz.getDeclaredField("hostComponentResourceProvider");
    f.setAccessible(true);
    f.set(null, hostComponentResourceProvider);

    blueprintServices.add("service1");
    blueprintServices.add("service2");

    expect(topology.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getServices()).andReturn(blueprintServices).anyTimes();
    expect(blueprint.getComponents("service1")).andReturn(Arrays.asList("s1Component1", "s1Component2"));
    expect(blueprint.getComponents("service2")).andReturn(Collections.singleton("s2Component1"));

    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();

  }

  @After
  public void tearDown() throws Exception {
    verify(controller, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, topology, blueprint, stack, clusters, cluster);

    reset(controller, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, topology, blueprint, stack, clusters, cluster);
  }

  private void replayAll() {
    replay(controller, hostResourceProvider, serviceResourceProvider, componentResourceProvider,
        hostComponentResourceProvider, topology, blueprint, stack, clusters, cluster);
  }

  @Test
  public void testCreateAmbariResources() throws Exception {
    // expectations
    Capture<ClusterRequest> clusterRequestCapture = new Capture<ClusterRequest>();
    controller.createCluster(capture(clusterRequestCapture));
    expectLastCall().once();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).anyTimes();
    expect(cluster.getServices()).andReturn(clusterServices).anyTimes();

    Capture<Set<ServiceRequest>> serviceRequestCapture = new Capture<Set<ServiceRequest>>();
    Capture<Set<ServiceComponentRequest>> serviceComponentRequestCapture = new Capture<Set<ServiceComponentRequest>>();

    serviceResourceProvider.createServices(capture(serviceRequestCapture));
    expectLastCall().once();
    componentResourceProvider.createComponents(capture(serviceComponentRequestCapture));
    expectLastCall().once();

    Capture<Request> serviceInstallRequestCapture = new Capture<Request>();
    Capture<Request> serviceStartRequestCapture = new Capture<Request>();
    Capture<Predicate> installPredicateCapture = new Capture<Predicate>();
    Capture<Predicate> startPredicateCapture = new Capture<Predicate>();

    expect(serviceResourceProvider.updateResources(capture(serviceInstallRequestCapture),
        capture(installPredicateCapture))).andReturn(null).once();
    expect(serviceResourceProvider.updateResources(capture(serviceStartRequestCapture),
        capture(startPredicateCapture))).andReturn(null).once();

    replayAll();

    // test
    context.createAmbariResources(topology);

    // assertions
    ClusterRequest clusterRequest = clusterRequestCapture.getValue();
    assertEquals(CLUSTER_NAME, clusterRequest.getClusterName());
    assertEquals(String.format("%s-%s", STACK_NAME, STACK_VERSION), clusterRequest.getStackVersion());

    Collection<ServiceRequest> serviceRequests = serviceRequestCapture.getValue();
    assertEquals(2, serviceRequests.size());
    Collection<String> servicesFound = new HashSet<String>();
    for (ServiceRequest serviceRequest : serviceRequests) {
      servicesFound.add(serviceRequest.getServiceName());
      assertEquals(CLUSTER_NAME, serviceRequest.getClusterName());
    }
    assertTrue(servicesFound.size() == 2 &&
        servicesFound.containsAll(Arrays.asList("service1", "service2")));

    Collection<ServiceComponentRequest> serviceComponentRequests = serviceComponentRequestCapture.getValue();
    assertEquals(3, serviceComponentRequests.size());
    Map<String, Collection<String>> foundServiceComponents = new HashMap<String, Collection<String>>();
    for (ServiceComponentRequest componentRequest : serviceComponentRequests) {
      assertEquals(CLUSTER_NAME, componentRequest.getClusterName());
      String serviceName = componentRequest.getServiceName();
      Collection<String> serviceComponents = foundServiceComponents.get(serviceName);
      if (serviceComponents == null) {
        serviceComponents = new HashSet<String>();
        foundServiceComponents.put(serviceName, serviceComponents);
      }
      serviceComponents.add(componentRequest.getComponentName());
    }
    assertEquals(2, foundServiceComponents.size());

    Collection<String> service1Components = foundServiceComponents.get("service1");
    assertEquals(2, service1Components.size());
    assertTrue(service1Components.containsAll(Arrays.asList("s1Component1", "s1Component2")));

    Collection<String> service2Components = foundServiceComponents.get("service2");
    assertEquals(1, service2Components.size());
    assertTrue(service2Components.contains("s2Component1"));

    Request installRequest = serviceInstallRequestCapture.getValue();
    Set<Map<String, Object>> installPropertiesSet = installRequest.getProperties();
    assertEquals(1, installPropertiesSet.size());
    Map<String, Object> installProperties = installPropertiesSet.iterator().next();
    assertEquals(CLUSTER_NAME, installProperties.get(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
    assertEquals("INSTALLED", installProperties.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
    assertEquals(new EqualsPredicate<String>(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME),
        installPredicateCapture.getValue());

    Request startRequest = serviceStartRequestCapture.getValue();
    Set<Map<String, Object>> startPropertiesSet = startRequest.getProperties();
    assertEquals(1, startPropertiesSet.size());
    Map<String, Object> startProperties = startPropertiesSet.iterator().next();
    assertEquals(CLUSTER_NAME, startProperties.get(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
    assertEquals("STARTED", startProperties.get(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID));
    assertEquals(new EqualsPredicate<String>(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME),
        installPredicateCapture.getValue());
  }
}