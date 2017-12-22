package com.github.adiesner.zuulgateway;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class ServiceViaBranchCookiePreFilter extends ZuulFilter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ServiceViaBranchCookiePreFilter.class);

    private static final String DEFAULT_BRANCH = "master";

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1; // run before PreDecoration
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    private String getAppName() {
        RequestContext ctx = RequestContext.getCurrentContext();
        final String[] split = ctx.getRequest().getRequestURI().split("/");
        final String requestedService = split[1];
        return requestedService;
    }

    private Optional<URI> searchRegistry(String serviceId, String branch) {
        final List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        for (ServiceInstance instance : serviceInstances) {
            Map<String, String> metadata = instance.getMetadata();
            for (String key : metadata.keySet()) {
                final String val = metadata.get(key);
                if ((StringUtils.equalsIgnoreCase(key, "branch")) &&
                    (StringUtils.equalsIgnoreCase(val, branch))) {
                    return Optional.of(instance.getUri());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getServiceBranchCookie(String serviceId) {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        final Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (StringUtils.equalsIgnoreCase(cookie.getName(), serviceId)) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Object run() {
        final String requestedServiceId = getAppName();
        final Optional<String> serviceBranchCookie = getServiceBranchCookie(requestedServiceId);

        Optional<URI> targetUri;
        if (serviceBranchCookie.isPresent()) {
            targetUri = searchRegistry(requestedServiceId, serviceBranchCookie.get());
        } else {
            log.info("No cookie for serviceId {} present", requestedServiceId);
            targetUri = searchRegistry(requestedServiceId, DEFAULT_BRANCH);
        }

        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.put(SERVICE_ID_KEY, requestedServiceId);
        if (targetUri.isPresent()) {
            log.info("Routing request to URL: {}", targetUri.get());
            try {
                ctx.setRouteHost(targetUri.get().toURL());
            } catch (MalformedURLException e) {
                log.error("MalformedURLException", e);
            }
        } else {
            log.info("Routing request via SERVICE_ID_KEY: {}", requestedServiceId);
        }

        return null;
    }
}
