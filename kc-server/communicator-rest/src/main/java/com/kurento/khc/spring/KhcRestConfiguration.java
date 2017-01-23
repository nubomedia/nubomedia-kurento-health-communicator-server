package com.kurento.khc.spring;

import java.util.Arrays;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kurento.khc.AccessDeniedExceptionMapper;
import com.kurento.khc.FileNotFoundExceptionMapper;
import com.kurento.khc.KhcInvalidDataExceptionMapper;
import com.kurento.khc.KhcNotFoundExceptionMapper;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.rest.v2.AccountRestfulService;
import com.kurento.khc.rest.v2.ChannelRestfulService;
import com.kurento.khc.rest.v2.CommandRestfulService;
import com.kurento.khc.rest.v2.GroupRestfulService;
import com.kurento.khc.rest.v2.MessageRestfulService;
import com.kurento.khc.rest.v2.PasswordRestfulService;
import com.kurento.khc.rest.v2.SubscriptionRestfulService;
import com.kurento.khc.rest.v2.UserRestfulService;

@Configuration
public class KhcRestConfiguration implements ServletContextInitializer {

	// Register CXF servlet when running embedded
	@Override
	public void onStartup(ServletContext servletContext)
			throws ServletException {

		// Register CXF servlet
		ServletRegistration.Dynamic cxf = servletContext.addServlet("cxf",
				new CXFServlet());
		cxf.setLoadOnStartup(1);
		cxf.addMapping("/v2/*");
	}

	@Bean(destroyMethod = "shutdown")
	public SpringBus cxf() {
		return new SpringBus();
	}

	@Bean
	public Application khcRsApi() {
		return new Application();
	}

	@Bean
	public Server jaxRsServer() {
		JAXRSServerFactoryBean factory = RuntimeDelegate.getInstance()
				.createEndpoint(khcRsApi(), JAXRSServerFactoryBean.class);
		factory.setProviders(Arrays.asList(jacksonJsonProvider(),
				khcInvalidMapper(), khcNotFoundMapper(), fileNotFoundMapper(),
				accessDeniedMapper()));
		factory.setServiceBeans(Arrays.asList(accountRestfulService(),
				groupRestfulService(), userRestfulService(),
				passwordRestfulService(), channelRestfulService(),
				subscriptionRestfulService(), commandRestfulService(),
				messageRestfulService()));

		return factory.create();
	}

	// //// SERVICE BEANS //////
	@Bean
	public AccountRestfulService accountRestfulService() {
		return new AccountRestfulService();
	}

	@Bean
	public GroupRestfulService groupRestfulService() {
		return new GroupRestfulService();
	}

	@Bean
	public UserRestfulService userRestfulService() {
		return new UserRestfulService();
	}

	@Bean
	public PasswordRestfulService passwordRestfulService() {
		return new PasswordRestfulService();
	}

	@Bean
	public ChannelRestfulService channelRestfulService() {
		return new ChannelRestfulService();
	}

	@Bean
	public SubscriptionRestfulService subscriptionRestfulService() {
		return new SubscriptionRestfulService();
	}

	@Bean
	public CommandRestfulService commandRestfulService() {
		return new CommandRestfulService();
	}

	@Bean
	public MessageRestfulService messageRestfulService() {
		return new MessageRestfulService();
	}

	// //// Mappers /////

	@Bean
	public KhcInvalidDataExceptionMapper khcInvalidMapper() {
		return new KhcInvalidDataExceptionMapper();
	}

	@Bean
	public KhcNotFoundExceptionMapper khcNotFoundMapper() {
		return new KhcNotFoundExceptionMapper();
	}

	@Bean
	public FileNotFoundExceptionMapper fileNotFoundMapper() {
		return new FileNotFoundExceptionMapper();
	}

	@Bean
	public AccessDeniedExceptionMapper accessDeniedMapper() {
		return new AccessDeniedExceptionMapper();
	}

	// //// Providers /////
	@Bean
	public KhcObjectMapper khcObjectMapper() {
		return new KhcObjectMapper();
	}

	@Bean
	public JacksonJsonProvider jacksonJsonProvider() {
		return new JacksonJsonProvider(khcObjectMapper());
	}

}
