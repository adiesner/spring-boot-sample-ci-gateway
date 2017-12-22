package com.github.adiesner.serviceconfig;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ServiceConfig {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ServiceConfig.class);

    @Autowired
    Environment env;

    public static class App {
        private String name;
        private List<String> branch;

        public App(String name, List<String> branch) {
            this.name = name;
            this.branch = branch;
        }

        public String getName() {
            return this.name;
        }

        public List<String> getBranch() {
            return this.branch;
        }
    }

    @Autowired
    private DiscoveryClient discoveryClient;

    @RequestMapping("/serviceconfig/instances")
    public List<App> getListOfInstances() {
        final List<String> services = this.discoveryClient.getServices();
        List<ServiceInstance> serviceInstanceList = new ArrayList<>();
        services.forEach(service -> serviceInstanceList.addAll(discoveryClient.getInstances(service)));

        final List<App> appList = new ArrayList<>();
        for (ServiceInstance instance: serviceInstanceList) {
            if (instance.getMetadata() != null) {
                final String branch = instance.getMetadata().get("branch");
                if (branch != null) {
                    addInstanceToAppList(appList, instance);
                }
            } else {
                log.debug("App {} has no metadata branch.", instance.getServiceId());
            }
        }
        return appList;
    }

    private void addInstanceToAppList(List<App> appList, ServiceInstance instance) {
        App app = null;
        for (App a : appList) {
            if (StringUtils.equalsIgnoreCase(a.getName(), instance.getServiceId())) {
                app = a;
                break;
            }
        }
        if (app == null) {
            List<String> branches = new ArrayList<>();
            branches.add(instance.getMetadata().get("branch"));
            app = new App(instance.getServiceId(), branches);
            appList.add(app);
        } else {
            final String branch = instance.getMetadata().get("branch");
            if (!app.getBranch().contains(branch)) {
                app.getBranch().add(branch);
            }
        }
    }

    @RequestMapping("/serviceconfig")
    public void localRedirect(HttpServletResponse response) {
        response.setStatus(HttpStatus.FOUND.value());
        response.setHeader("Location", "/serviceconfig/index.html");
    }

    @RequestMapping(value = "/info")
    public String info() {
        return "Application: "+env.getProperty("spring.application.name")+" on branch: "+env.getProperty("eureka.instance.metadataMap.branch");
    }

}
