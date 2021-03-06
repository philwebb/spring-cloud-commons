/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.client.discovery;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;

/**
 * @author Dave Syer
 *
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnMissingClass(name = "com.netflix.discovery.EurekaClientConfig")
@ConditionalOnExpression("!${eureka.client.enabled:false}")
public class NoopDiscoveryClientConfiguration implements ApplicationListener<ContextRefreshedEvent> {

	private static final Logger logger = LoggerFactory
			.getLogger(NoopDiscoveryClientConfiguration.class);

	@Autowired(required = false)
	private ServerProperties server;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Environment environment;

	private DefaultServiceInstance serviceInstance;

	@PostConstruct
	public void init() {
		String host = "localhost";
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			logger.error("Cannot get host info", e);
		}
		int port = 0;
		if (server != null && server.getPort() != null) {
			port = server.getPort();
		}
		if (context instanceof EmbeddedWebApplicationContext) {
			EmbeddedServletContainer container = ((EmbeddedWebApplicationContext) context)
					.getEmbeddedServletContainer();
			if (container != null) {
				// TODO: why is it null
				port = container.getPort();
			}
		}
		serviceInstance = new DefaultServiceInstance(environment.getProperty(
				"spring.application.name", "application"), host, port);
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		context.publishEvent(new InstanceRegisteredEvent<Environment>(this, environment));
	}

	@Bean
	public DiscoveryClient discoveryClient() {
		return new NoopDiscoveryClient(serviceInstance);
	}

}
