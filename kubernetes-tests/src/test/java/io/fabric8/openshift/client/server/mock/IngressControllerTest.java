/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.openshift.client.server.mock;

import io.fabric8.openshift.api.model.operator.v1.IngressController;
import io.fabric8.openshift.api.model.operator.v1.IngressControllerBuilder;
import io.fabric8.openshift.api.model.operator.v1.IngressControllerList;
import io.fabric8.openshift.api.model.operator.v1.IngressControllerListBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableRuleMigrationSupport
class IngressControllerTest {
  @Rule
  public OpenShiftServer server = new OpenShiftServer();

  @Test
  void create() {
    // Given
    IngressController dnsrecord = getIngressController();
    server.expect().post()
      .withPath("/apis/operator.openshift.io/v1/namespaces/ns1/ingresscontrollers")
      .andReturn(HttpURLConnection.HTTP_OK, dnsrecord)
      .once();
    OpenShiftClient client = server.getOpenshiftClient();

    // When
    dnsrecord = client.operator().ingressControllers().inNamespace("ns1").create(dnsrecord);

    // Then
    assertNotNull(dnsrecord);
    assertEquals("foo", dnsrecord.getMetadata().getName());
  }

  @Test
  void get() {
    // Given
    server.expect().get()
      .withPath("/apis/operator.openshift.io/v1/namespaces/ns1/ingresscontrollers/foo")
      .andReturn(HttpURLConnection.HTTP_OK, getIngressController())
      .once();
    OpenShiftClient client = server.getOpenshiftClient();

    // When
    IngressController f = client.operator().ingressControllers().inNamespace("ns1").withName("foo").get();

    // Then
    assertNotNull(f);
    assertEquals("foo", f.getMetadata().getName());
  }

  @Test
  void list() {
    // Given
    server.expect().get()
      .withPath("/apis/operator.openshift.io/v1/namespaces/ns1/ingresscontrollers")
      .andReturn(HttpURLConnection.HTTP_OK, new IngressControllerListBuilder().withItems(getIngressController()).build())
      .once();
    OpenShiftClient client = server.getOpenshiftClient();

    // When
    IngressControllerList fgList = client.operator().ingressControllers().inNamespace("ns1").list();

    // Then
    assertNotNull(fgList);
    assertNotNull(fgList.getItems());
    assertEquals(1, fgList.getItems().size());
  }

  @Test
  void delete() {
    // Given
    server.expect().delete()
      .withPath("/apis/operator.openshift.io/v1/namespaces/ns1/ingresscontrollers/foo")
      .andReturn(HttpURLConnection.HTTP_OK, getIngressController())
      .once();
    OpenShiftClient client = server.getOpenshiftClient();

    // When
    Boolean deleted = client.operator().ingressControllers().inNamespace("ns1").withName("foo").delete();

    // Then
    assertTrue(deleted);
  }

  private IngressController getIngressController() {
    return new IngressControllerBuilder()
      .withNewMetadata()
      .withName("foo")
      .withNamespace("ns1")
      .endMetadata()
      .withNewSpec()
      .withDomain("example.fabric8.io")
      .withNewNodePlacement()
      .withNewNodeSelector()
      .addToMatchLabels("node-role.kubernetes.io/worker", "")
      .endNodeSelector()
      .endNodePlacement()
      .withNewRouteSelector()
      .addToMatchLabels("type", "sharded")
      .endRouteSelector()
      .endSpec()
      .build();
  }
}
