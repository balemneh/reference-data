package gov.dhs.cbp.reference.workflow.config;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableProcessApplication
public class CamundaConfig {
    // Camunda auto-configuration handles most setup
    // Additional configuration can be added here if needed
}