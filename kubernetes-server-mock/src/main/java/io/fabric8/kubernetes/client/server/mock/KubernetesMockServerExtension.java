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

package io.fabric8.kubernetes.client.server.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.mockwebserver.Context;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


public class KubernetesMockServerExtension implements AfterEachCallback, AfterAllCallback, BeforeEachCallback, BeforeAllCallback {

  private KubernetesMockServer mock;
  private NamespacedKubernetesClient client;

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    context.getTestClass()
      .flatMap(testClass -> findKubernetesClientField(testClass, false))
      .ifPresent(field -> destroy());
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    destroy();
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    setKubernetesClientField(context, false);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    setKubernetesClientField(context, true);
  }

  private void setKubernetesClientField(ExtensionContext context, boolean isStatic) throws IllegalAccessException {
    Optional<Class<?>> optClass = context.getTestClass();
    if (optClass.isPresent()) {
      Class<?> testClass = optClass.get();
      Optional<Field> optField = findKubernetesClientField(testClass, isStatic);
      if (optField.isPresent()) {
        Field field = optField.get();
        createAndInitClient(testClass);
        if (isStatic) {
          field.set(null, client);
        } else {
          Optional<Object> optTestInstance = context.getTestInstance();
          if (optTestInstance.isPresent()) {
            field.set(optTestInstance.get(), client);
          }
        }
      }
    }
  }

  private void createAndInitClient(Class<?> testClass) {
    EnableKubernetesMockClient a = testClass.getAnnotation(EnableKubernetesMockClient.class);
    mock = a.crud()
      ? new KubernetesMockServer(new Context(), new MockWebServer(), new HashMap<>(), new KubernetesCrudDispatcher(), a.https())
      : new KubernetesMockServer(a.https());
    mock.init();
    client = mock.createClient();
  }

  private Optional<Field> findKubernetesClientField(Class<?> testClass, boolean isStatic) {
    Field[] fields = testClass.getDeclaredFields();
    for (Field f : fields) {
      if (f.getType() == KubernetesClient.class && Modifier.isStatic(f.getModifiers()) == isStatic) {
        return Optional.of(f);
      }
    }
    return Optional.empty();
  }

  private void destroy() {
    mock.destroy();
    client.close();
  }
}
