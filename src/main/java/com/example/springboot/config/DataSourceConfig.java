package com.example.springboot.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据源配置类
 * 支持 MongoDB 和 MySQL 的配置与切换
 *
 * 配置属性说明：
 * - datasource.type: 数据源类型 (mongodb 或 mysql)
 * - datasource.primary: 主数据源 (mongodb 或 mysql)
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(DataSourceConfig.DatasourceProperties.class)
public class DataSourceConfig {

    /**
     * 数据源配置属性
     */
    @ConfigurationProperties(prefix = "datasource")
    public static class DatasourceProperties {

        private String type = "mysql"; // 默认使用 MySQL
        private String primary = "mysql"; // 主数据源
        private boolean enableMigration = false; // 是否启用迁移

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPrimary() {
            return primary;
        }

        public void setPrimary(String primary) {
            this.primary = primary;
        }

        public boolean isEnableMigration() {
            return enableMigration;
        }

        public void setEnableMigration(boolean enableMigration) {
            this.enableMigration = enableMigration;
        }
    }

    /**
     * 启用 MongoDB Repository
     * 仅当配置中包含 MongoDB 时启用
     */
    @Configuration
    @ConditionalOnProperty(
        name = "datasource.type",
        havingValue = "mongodb",
        matchIfMissing = false
    )
    @EnableMongoRepositories(
        basePackages = "com.example.springboot.repository.mongo",
        mongoTemplateRef = "mongoTemplate"
    )
    public static class MongoDbConfig {

        /**
         * 配置 MongoClient
         */
        @Bean
        @ConditionalOnMissingBean
        public MongoClient mongoClient(
            org.springframework.boot.autoconfigure.mongo.MongoProperties mongoProperties
        ) {
            return MongoClients.create(mongoProperties.getUri());
        }

        /**
         * 配置 MongoDB 数据库工厂
         */
        @Bean
        public MongoDatabaseFactory mongoDatabaseFactory(
            MongoClient mongoClient,
            org.springframework.boot.autoconfigure.mongo.MongoProperties mongoProperties
        ) {
            return new SimpleMongoClientDatabaseFactory(
                mongoClient,
                mongoProperties.getDatabase()
            );
        }

        /**
         * 配置 MongoTemplate
         */
        @Bean
        public MongoTemplate mongoTemplate(
            MongoDatabaseFactory mongoDatabaseFactory
        ) {
            return new MongoTemplate(mongoDatabaseFactory);
        }
    }

    /**
     * 启用 JPA Repository for MySQL
     */
    @Configuration
    @ConditionalOnProperty(
        name = "datasource.type",
        havingValue = "mysql",
        matchIfMissing = true
    )
    @EnableJpaRepositories(
        basePackages = "com.example.springboot.repository.jpa",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
    )
    public static class MySqlConfig {

        /**
         * 配置 JPA 实体管理工厂
         */
        @Bean
        @Primary
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            org.springframework.boot.orm.jpa.EntityManagerFactoryBuilderCustomizer... customizers
        ) {
            LocalContainerEntityManagerFactoryBean em =
                new LocalContainerEntityManagerFactoryBean();
            em.setDataSource(dataSource);
            em.setPackagesToScan("com.example.springboot.model");

            HibernateJpaVendorAdapter vendorAdapter =
                new HibernateJpaVendorAdapter();
            em.setJpaVendorAdapter(vendorAdapter);

            Map<String, Object> properties = new HashMap<>();
            properties.put(
                "hibernate.dialect",
                "org.hibernate.dialect.MySQL8Dialect"
            );
            properties.put("hibernate.show_sql", true);
            properties.put("hibernate.format_sql", true);
            properties.put("hibernate.use_sql_comments", true);
            properties.put("hibernate.jdbc.batch_size", 20);
            properties.put("hibernate.order_inserts", true);
            properties.put("hibernate.order_updates", true);

            em.setJpaPropertyMap(properties);
            return em;
        }

        /**
         * 配置事务管理器
         */
        @Bean
        @Primary
        public org.springframework.orm.jpa.JpaTransactionManager transactionManager(
            LocalContainerEntityManagerFactoryBean entityManagerFactory
        ) {
            org.springframework.orm.jpa.JpaTransactionManager tm =
                new org.springframework.orm.jpa.JpaTransactionManager();
            tm.setEntityManagerFactory(entityManagerFactory.getObject());
            return tm;
        }
    }

    /**
     * 双数据源配置（MongoDB 和 MySQL 同时启用）
     */
    @Configuration
    @ConditionalOnProperty(
        name = "datasource.enable-migration",
        havingValue = "true"
    )
    public static class DualDataSourceConfig {

        /**
         * 配置 MongoClient
         */
        @Bean(name = "mongoClient")
        public MongoClient mongoClient(
            org.springframework.boot.autoconfigure.mongo.MongoProperties mongoProperties
        ) {
            return MongoClients.create(mongoProperties.getUri());
        }

        /**
         * 配置 MongoDB 数据库工厂
         */
        @Bean(name = "mongoDatabaseFactory")
        public MongoDatabaseFactory mongoDatabaseFactory(
            MongoClient mongoClient,
            org.springframework.boot.autoconfigure.mongo.MongoProperties mongoProperties
        ) {
            return new SimpleMongoClientDatabaseFactory(
                mongoClient,
                mongoProperties.getDatabase()
            );
        }

        /**
         * 配置 MongoTemplate
         */
        @Bean(name = "mongoTemplate")
        public MongoTemplate mongoTemplate(
            MongoDatabaseFactory mongoDatabaseFactory
        ) {
            return new MongoTemplate(mongoDatabaseFactory);
        }

        /**
         * 启用 MongoDB Repositories
         */
        @Configuration
        @EnableMongoRepositories(
            basePackages = "com.example.springboot.repository.mongo",
            mongoTemplateRef = "mongoTemplate"
        )
        public static class MongoRepoConfig {}

        /**
         * 启用 JPA Repositories
         */
        @Configuration
        @EnableJpaRepositories(
            basePackages = "com.example.springboot.repository.jpa",
            entityManagerFactoryRef = "entityManagerFactory",
            transactionManagerRef = "transactionManager"
        )
        public static class JpaRepoConfig {}
    }
}
