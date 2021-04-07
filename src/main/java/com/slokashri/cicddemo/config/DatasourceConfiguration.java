package com.slokashri.cicddemo.config;

import com.google.gson.Gson;
import com.slokashri.cicddemo.model.AWSSecret;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import java.io.IOException;

@Configuration
public class DatasourceConfiguration extends HikariConfig {
    @Value("${spring.datasource.hikari.jdbc-url}")
    private String jdbcUrl;
    @Value("${aws.rds.secret.name}")
    private String secretName;
    @Bean
    @Primary
    public HikariDataSource dataSource() throws IOException{
        AWSSecret userCredentials = getUserCredentials();
        this.setJdbcUrl(jdbcUrl);
        this.setUsername(userCredentials.getUsername());
        this.setPassword(userCredentials.getPassword());
        return new HikariDataSource(this);
    }
    public AWSSecret getUserCredentials(){
        Region region = Region.US_EAST_1;

        AWSSecret awsSecret = null;
        // Create a Secrets Manager client
        SecretsManagerClient secretsClient  = SecretsManagerClient.builder().
                region(region)
                .build();
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
            String secretString = valueResponse.secretString();
            Gson gson = new Gson();
            awsSecret = gson.fromJson(secretString,AWSSecret.class);
        } catch (SecretsManagerException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
        return awsSecret;
    }
}
