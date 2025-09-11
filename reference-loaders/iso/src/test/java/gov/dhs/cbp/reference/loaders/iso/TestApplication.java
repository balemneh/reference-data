package gov.dhs.cbp.reference.loaders.iso;

import gov.dhs.cbp.reference.loader.common.LoaderConfiguration;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import jakarta.validation.Validator;
import jakarta.validation.Validation;

@SpringBootApplication
@ComponentScan(basePackages = {
    "gov.dhs.cbp.reference.loaders.iso",
    "gov.dhs.cbp.reference.loader.common",
    "gov.dhs.cbp.reference.core",
    "gov.dhs.cbp.reference.events"
})
@EntityScan(basePackages = {
    "gov.dhs.cbp.reference.loaders.iso.entity",
    "gov.dhs.cbp.reference.core.entity",
    "gov.dhs.cbp.reference.loader.common"
})
@EnableJpaRepositories(basePackages = {
    "gov.dhs.cbp.reference.loaders.iso.repository",
    "gov.dhs.cbp.reference.core.repository"
})
@EnableBatchProcessing
public class TestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
    
    @Bean
    public LoaderConfiguration loaderConfiguration() {
        LoaderConfiguration config = new LoaderConfiguration();
        config.setEnableScheduling(false);
        config.setFailOnValidationError(false);
        config.setAutoApplyChanges(true);
        config.setBatchSize(100);
        return config;
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
    
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
    
    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType("POSTGRES");
        factory.afterPropertiesSet();
        return factory.getObject();
    }
    
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.afterPropertiesSet();
        return launcher;
    }
}