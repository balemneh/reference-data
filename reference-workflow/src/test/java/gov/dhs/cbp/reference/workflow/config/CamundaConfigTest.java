package gov.dhs.cbp.reference.workflow.config;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CamundaConfigTest {

    @Test
    void testCamundaConfigIsAnnotatedWithConfiguration() {
        // Verify that CamundaConfig has @Configuration annotation
        assertTrue(CamundaConfig.class.isAnnotationPresent(Configuration.class),
                "CamundaConfig should be annotated with @Configuration");
    }

    @Test
    void testCamundaConfigIsAnnotatedWithEnableProcessApplication() {
        // Verify that CamundaConfig has @EnableProcessApplication annotation
        assertTrue(CamundaConfig.class.isAnnotationPresent(EnableProcessApplication.class),
                "CamundaConfig should be annotated with @EnableProcessApplication");
    }

    @Test
    void testCamundaConfigCanBeInstantiated() {
        // Verify that CamundaConfig can be instantiated
        CamundaConfig config = new CamundaConfig();
        assertNotNull(config, "CamundaConfig should be instantiable");
    }

    @Test
    void testCamundaConfigClassName() {
        // Verify the class name
        assertEquals("CamundaConfig", CamundaConfig.class.getSimpleName());
    }

    @Test
    void testCamundaConfigPackage() {
        // Verify the package
        assertEquals("gov.dhs.cbp.reference.workflow.config", 
                    CamundaConfig.class.getPackage().getName());
    }
}