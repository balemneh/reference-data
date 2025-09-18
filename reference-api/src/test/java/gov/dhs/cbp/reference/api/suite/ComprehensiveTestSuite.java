package gov.dhs.cbp.reference.api.suite;

import gov.dhs.cbp.reference.api.controller.ChangeRequestControllerIntegrationTest;
import gov.dhs.cbp.reference.api.controller.CountriesControllerTest;
import gov.dhs.cbp.reference.api.performance.ChangeRequestWorkflowPerformanceTest;
import gov.dhs.cbp.reference.api.service.*;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Comprehensive test suite for the Change Request Workflow system.
 *
 * This suite includes:
 * - Unit tests for all services
 * - Integration tests for API endpoints
 * - Performance benchmarks
 * - Workflow integration tests
 *
 * Run with: mvn test -Dtest=ComprehensiveTestSuite
 */
@Suite
@SuiteDisplayName("Change Request Workflow - Comprehensive Test Suite")
@SelectClasses({
    // Service Unit Tests
    CountryChangeRequestServiceTest.class,
    ChangeRequestApplicationServiceTest.class,
    BulkImportServiceTest.class,
    CountryServiceTest.class,

    // Controller Tests
    CountriesControllerTest.class,

    // Integration Tests
    ChangeRequestControllerIntegrationTest.class,
    ChangeRequestWorkflowIntegrationTest.class,

    // Performance Tests
    ChangeRequestWorkflowPerformanceTest.class
})
@IncludeClassNamePatterns({
    ".*Test.*",
    ".*IntegrationTest.*",
    ".*PerformanceTest.*"
})
public class ComprehensiveTestSuite {

    // Test suite class - no implementation needed
    // JUnit 5 will automatically discover and run all included test classes

}