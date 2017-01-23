package com.kurento.khc.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain chain) throws IOException, ServletException {
		Boolean isMessage = false;
		String url = ((HttpServletRequest) arg0).getRequestURL().toString();
		if (url.contains("message") && url.endsWith("content")) {
			isMessage = true;
			log.debug(" { \"log_type\"=\"StartedAttDownloading\", \"timestamp\"="
					+ System.currentTimeMillis() + "}");
		} else {
			log.debug("Request received to URL: {}", url);
		}

		chain.doFilter(arg0, arg1);
		if (isMessage) {
			log.debug(" { \"log_type\"=\"FinishedAttDownloading\", \"timestamp\"="
					+ System.currentTimeMillis() + "}");
		} else {
			log.debug("Response sent to URL: {}", url);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// Nothing to do
	}

}
