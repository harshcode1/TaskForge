package com.projectmgmttool.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectManagementBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectManagementBackendApplication.class, args);
	}

}
