package com.kurento.khc.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;

import com.kurento.khc.security.KhcUserDetailsService;

@Configuration
@EnableWebSecurity
@Order(99)
public class KhcSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Bean
	public KhcUserDetailsService userDetailsService() {
		return new KhcUserDetailsService();
	}

	@Bean
	public DigestAuthenticationFilter authenticationFilter() {
		DigestAuthenticationFilter authenticationFilter = new DigestAuthenticationFilter();
		authenticationFilter.setUserDetailsService(userDetailsService());
		authenticationFilter
				.setAuthenticationEntryPoint(authenticationEntryPoint());

		return authenticationFilter;
	}

	@Bean
	public DigestAuthenticationEntryPoint authenticationEntryPoint() {
		DigestAuthenticationEntryPoint authenticationEntryPoint = new DigestAuthenticationEntryPoint();
		authenticationEntryPoint.setRealmName("KurentoAgenda");
		authenticationEntryPoint.setKey("agenda");
		return authenticationEntryPoint;
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.userDetailsService(userDetailsService())
				.addFilterAfter(authenticationFilter(),
						BasicAuthenticationFilter.class).exceptionHandling()
				.authenticationEntryPoint(authenticationEntryPoint()).and()
				.authorizeRequests()
				.antMatchers("/v2/password/**", "/v2/account/**", "/sync/**")
				.permitAll().anyRequest().authenticated().and().csrf()
				.disable();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.userDetailsService(userDetailsService());
	}

	@Bean
	@Override
	public AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManagerBean();
	}

}
