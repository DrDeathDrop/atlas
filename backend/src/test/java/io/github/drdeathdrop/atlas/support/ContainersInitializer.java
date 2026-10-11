package io.github.drdeathdrop.atlas.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class ContainersInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String DATABASE = "atlas";
    private static final String OWNER = "atlas";
    private static final String OWNER_PASSWORD = "atlas";
    private static final String BROKER_USER = "atlas";
    private static final String BROKER_PASSWORD = "atlas";
    private static final int POSTGRES_PORT = 5432;
    private static final int RABBIT_PORT = 5672;

    private static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgis/postgis:17-3.5"))
                    .withEnv("POSTGRES_DB", DATABASE)
                    .withEnv("POSTGRES_USER", OWNER)
                    .withEnv("POSTGRES_PASSWORD", OWNER_PASSWORD)
                    .withExposedPorts(POSTGRES_PORT)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    private static final GenericContainer<?> RABBIT =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:4"))
                    .withEnv("RABBITMQ_DEFAULT_USER", BROKER_USER)
                    .withEnv("RABBITMQ_DEFAULT_PASS", BROKER_PASSWORD)
                    .withExposedPorts(RABBIT_PORT)
                    .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\s", 1)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String databaseUrl = "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(POSTGRES_PORT) + "/" + DATABASE;
        TestPropertyValues.of(
                "spring.datasource.url=" + databaseUrl,
                "spring.liquibase.user=" + OWNER,
                "spring.liquibase.password=" + OWNER_PASSWORD,
                "spring.rabbitmq.host=" + RABBIT.getHost(),
                "spring.rabbitmq.port=" + RABBIT.getMappedPort(RABBIT_PORT),
                "spring.rabbitmq.username=" + BROKER_USER,
                "spring.rabbitmq.password=" + BROKER_PASSWORD
        ).applyTo(context);
    }
}
