package com.kurento.khc.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.kurento.khc")
@EnableAutoConfiguration(exclude = { HibernateJpaAutoConfiguration.class })
public class KhcApp extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder app) {
		return app.sources(KhcApp.class);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(KhcApp.class, args);
	}
}
