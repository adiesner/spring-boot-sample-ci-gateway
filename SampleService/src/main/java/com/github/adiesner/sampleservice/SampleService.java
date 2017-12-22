package com.github.adiesner.sampleservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleService {

    @Autowired
    Environment env;

    private Integer counter = 0;

    @RequestMapping(value = "/sampleservice")
    public String sampleService() {
        return "Application: "+env.getProperty("spring.application.name")+" has been called "+ ++counter + " times. branch: "+env.getProperty("eureka.instance.metadataMap.branch");
    }

    @RequestMapping(value = "/info")
    public String info() {
        return "Application: "+env.getProperty("spring.application.name")+" on branch: "+env.getProperty("eureka.instance.metadataMap.branch");
    }

}
