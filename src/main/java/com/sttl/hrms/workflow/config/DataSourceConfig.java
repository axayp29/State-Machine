package com.sttl.hrms.workflow.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configure data source bean and Jdbc Template
 */
@Configuration
@EntityScan(basePackages = {"com.sttl.hrms.workflow.data.model.entity"})
@EnableJpaRepositories(basePackages = {"com.sttl.hrms.workflow.data.model.repository"})
public class DataSourceConfig {

    /**
     * Create a DataSourceProperties bean by reading the application properties at given location
     *
     * @return DataSourceProperties dataSourceProperties
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Create a DataSource from the specified DataSourceProperties object.
     *
     * @return DataSource dataSource
     */
    @Bean
    public DataSource dataSource() {
        return dataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    /**
     * Create a JDBCTemplate bean from a given DataSource object.
     *
     * @param dataSource A DataSource object
     * @return JdbcTemplate
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
