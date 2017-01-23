// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.kurento.khc.spring;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.kurento.jpa.CustomHibernateJpaDialect;
import com.kurento.khc.KhcInternalServerException;

@Configuration
@ComponentScan(basePackages = { "com.kurento.khc" })
@EnableTransactionManagement
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class KhcDatamodelConfig {

	// JPA Constants
	@Value("${kurento.persistence.unit:#{null}}")
	public String KURENTO_PERSISTENCE_UNIT = "kurentoagendapersistenceunit";

	@Value("${hibernate.id.new_generator_mappings:#{null}}")
	public String HIBERNATE_ID_NEW_GENERATOR_MAPPINGS = "true";
	@Value("${hibernate.archive.autodetection:#{null}}")
	public String HIBERNATE_ARCHIVE_AUTODETECTION = "class";
	@Value("${hibernate.hbm2ddl.auto:#{null}}")
	public String HIBERNATE_HBM2DDL_AUTO = "validate";
	@Value("${hibernate.format_sql:#{null}}")
	public String HIBERNATE_FORMAT_SQL = "true";
	@Value("${hibernate.use_sql_comments:#{null}}")
	public String HIBERNATE_USE_SQL_COMMENTS = "false";
	@Value("${hibernate.connection.autocommit:#{null}}")
	public String HIBERNATE_CONNECTION_AUTOCOMMIT = "true";
	@Value("${hibernate.bytecode.use_reflection_optimizer:#{null}}")
	public String HIBERNATE_BYTECODE_USE_REFLECTION_OPTIMIZER = "false";
	@Value("${hibernate.connection.driver_class:#{null}}")
	public String HIBERNATE_CONNECTION_DRIVER_CLASS = "com.mysql.jdbc.Driver";
	@Value("${hibernate.dialect:#{null}}")
	public String HIBERNATE_DIALECT = "org.hibernate.dialect.MySQL5InnoDBDialect";

	@Value("${kurento.persistence.datasource.driver:#{null}}")
	public String KURENTO_PERSISTENCE_DATASOURCE_DRIVER = "com.mysql.jdbc.Driver";
	@Value("${kurento.persistence.datasource.url:#{null}}")
	public String KURENTO_PERSISTENCE_DATASOURCE_URL = "jdbc:mysql://localhost:3306/kagenda?useUnicode=true&connectionCollation=utf8_general_ci&characterSetResults=utf8";
	@Value("${kurento.persistence.datasource.username:#{null}}")
	public String KURENTO_PERSISTENCE_DATASOURCE_USERNAME = "root";
	@Value("${kurento.persistence.datasource.password:#{null}}")
	public String KURENTO_PERSISTENCE_DATASOURCE_PASSWORD = "root";

	@Value("${kurento.persistence.datasource:#{null}}")
	public String KURENTO_PERSISTENCE_DATASOURCE = null;

	@Configuration
	@Profile("embed_db")
	@PropertySource(value = { "classpath:khc.properties",
			"classpath:${khc.config:#{null}}", "file:${khc.config:#{null}}",
			"classpath:khc-embedded.properties" }, ignoreResourceNotFound = true)
	static class TestConfig {
		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(
					EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer testPropertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

	@Configuration
	@Profile("embed_security")
	static class SecurityConfig extends WebSecurityConfigurerAdapter {

		@Bean
		@Override
		public AuthenticationManager authenticationManager() throws Exception {
			return super.authenticationManagerBean();
		}
	}

	@Configuration
	@Profile({ "!embed_db" })
	@PropertySource(value = { "classpath:khc.properties",
			"file:/etc/khc/khc.properties", "file:${khc.config:#{null}}" }, ignoreResourceNotFound = true)
	static class ProductionConfig {
		@Bean
		public static PropertySourcesPlaceholderConfigurer testPropertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}
	}

	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(emf);
		return txManager;
	}

	@Bean
	public FactoryBean<EntityManagerFactory> entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactory.setPersistenceUnitName(KURENTO_PERSISTENCE_UNIT);
		entityManagerFactory.setDataSource(dataSource());
		entityManagerFactory
				.setPersistenceXmlLocation("classpath:META-INF/agenda-datamodel-persistence.xml");
		entityManagerFactory.setJpaDialect(jpaDialect());

		Properties jpaProperties = new Properties();
		jpaProperties.setProperty("hibernate.id.new_generator_mappings",
				HIBERNATE_ID_NEW_GENERATOR_MAPPINGS);
		jpaProperties.setProperty("hibernate.archive.autodetection",
				HIBERNATE_ARCHIVE_AUTODETECTION);
		jpaProperties.setProperty("hibernate.hbm2ddl.auto",
				HIBERNATE_HBM2DDL_AUTO);
		jpaProperties.setProperty("hibernate.format_sql", HIBERNATE_FORMAT_SQL);
		jpaProperties.setProperty("hibernate.use_sql_comments",
				HIBERNATE_USE_SQL_COMMENTS);
		jpaProperties.setProperty("hibernate.connection.autocommit",
				HIBERNATE_CONNECTION_AUTOCOMMIT);
		jpaProperties.setProperty(
				"hibernate.bytecode.use_reflection_optimizer",
				HIBERNATE_BYTECODE_USE_REFLECTION_OPTIMIZER);
		jpaProperties.setProperty("hibernate.connection.driver_class",
				HIBERNATE_CONNECTION_DRIVER_CLASS);
		jpaProperties.setProperty("hibernate.dialect", HIBERNATE_DIALECT);

		entityManagerFactory.setJpaProperties(jpaProperties);

		return entityManagerFactory;
	}

	@Bean
	public CustomHibernateJpaDialect jpaDialect() {
		return new CustomHibernateJpaDialect();
	}

	@Bean
	@Profile("!embed_db")
	public DataSource dataSource() {
		if (KURENTO_PERSISTENCE_DATASOURCE != null) {
			JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
			dsLookup.setResourceRef(true);
			DataSource dataSource = dsLookup
					.getDataSource(KURENTO_PERSISTENCE_DATASOURCE);
			return dataSource;
		} else if (KURENTO_PERSISTENCE_DATASOURCE_URL != null) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource
					.setDriverClassName(KURENTO_PERSISTENCE_DATASOURCE_DRIVER);
			dataSource.setUrl(KURENTO_PERSISTENCE_DATASOURCE_URL);
			dataSource.setUsername(KURENTO_PERSISTENCE_DATASOURCE_USERNAME);
			dataSource.setPassword(KURENTO_PERSISTENCE_DATASOURCE_PASSWORD);
			return dataSource;
		} else {
			throw new KhcInternalServerException(
					"No datasource configuration found");
		}
	}

	@Bean
	public ResourceBundleMessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages/messages");
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

}
