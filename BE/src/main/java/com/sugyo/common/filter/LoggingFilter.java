package com.sugyo.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;

        String method = req.getMethod();
        String uri = req.getRequestURI();
        String query = req.getQueryString();
        log.debug("[REQUEST] " + method + " " + uri + (query != null ? "?" + query : ""));

        chain.doFilter(request, response);
    }
}
