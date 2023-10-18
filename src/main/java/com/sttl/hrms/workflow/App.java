package com.sttl.hrms.workflow;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        new SpringApplicationBuilder(App.class)
                .logStartupInfo(true)
                .build()
                .run(args);
    }

}
