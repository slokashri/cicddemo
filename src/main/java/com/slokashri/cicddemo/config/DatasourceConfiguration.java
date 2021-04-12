package com.slokashri.cicddemo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.google.gson.Gson;
import com.slokashri.cicddemo.model.AWSSecret;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

@Configuration
public class DatasourceConfiguration extends HikariConfig {
    //Configuration parameters for the generation of the IAM Database Authentication token
    private static final DefaultAWSCredentialsProviderChain credentials = new DefaultAWSCredentialsProviderChain();
    private static final String AWS_ACCESS_KEY = credentials.getCredentials().getAWSAccessKeyId();
    private static final String AWS_SECRET_KEY = credentials.getCredentials().getAWSSecretKey();
    @Value("${aws.rds.secret.name}")
    private String secretName;
    @Value("${aws.rds.region.name:us-east-1}")
    private String REGION_NAME;
    @Value("${aws.rds.db.username:sagar_db_testappuser}")
    private String DB_USER ;
    @Value("${aws.rds.db.hostname:postgres-db.cqzwrojcmy2r.us-east-1.rds.amazonaws.com}")
    private String RDS_INSTANCE_HOSTNAME;
    @Value("${aws.rds.db.port:5432}")
    private int RDS_INSTANCE_PORT;
    @Value("${aws.rds.db.jdbcprotocol:jdbc:postgresql://}")
    private String JDBC_PROTOCOL;
    @Value("${aws.rds.db.name:jdbc:testdb}")
    private String DB_NAME;

    private static final String SSL_CERTIFICATE = "rds-ca-2019-us-east-1.pem";
    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_STORE_PROVIDER = "SUN";
    private static final String KEY_STORE_FILE_PREFIX = "sys-connect-via-ssl-test-cacerts";
    private static final String KEY_STORE_FILE_SUFFIX = ".jks";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "changeit";



    @Bean
    @Primary
    public HikariDataSource dataSource() throws IOException, Exception{
        this.setJdbcUrl(JDBC_PROTOCOL+RDS_INSTANCE_HOSTNAME+":"+RDS_INSTANCE_PORT+"/"+DB_NAME);
        //Un-comment below three lines for using AWS Secrets Manager
        /*
        AWSSecret userCredentials = getUserCredentialsUsingSecretsManager();
        this.setUsername(userCredentials.getUsername());
        this.setPassword(userCredentials.getPassword());
         */
        this.setUsername(DB_USER);
        this.setPassword(generateAuthToken());

        //clearSslProperties();
        return new HikariDataSource(this);
    }

    /**
      * Method for getting user credentials using AWS Secrets Manager (uses AWS SDK2 version)
     */

    /*
    public AWSSecret getUserCredentialsUsingSecretsManager(){
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
    */





    /**
     * This method generates the IAM Auth Token.
     * An example IAM Auth Token would look like follows:
     * btusi123.cmz7kenwo2ye.rds.cn-north-1.amazonaws.com.cn:3306/?Action=connect&DBUser=iamtestuser&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20171003T010726Z&X-Amz-SignedHeaders=host&X-Amz-Expires=899&X-Amz-Credential=AKIAPFXHGVDI5RNFO4AQ%2F20171003%2Fcn-north-1%2Frds-db%2Faws4_request&X-Amz-Signature=f9f45ef96c1f770cdad11a53e33ffa4c3730bc03fdee820cfdf1322eed15483b
     * @return
     */
    private String generateAuthToken() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY);
        RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
                .credentials(new AWSStaticCredentialsProvider(awsCredentials)).region(REGION_NAME).build();
        return generator.getAuthToken(GetIamAuthTokenRequest.builder()
                .hostname(RDS_INSTANCE_HOSTNAME).port(RDS_INSTANCE_PORT).userName(DB_USER).build());
    }

    /**
     * This method returns a connection to the db instance authenticated using IAM Database Authentication
     * @return
     * @throws Exception
     */
    private Connection getDBConnectionUsingIam() throws Exception {
        setSslProperties();
        return DriverManager.getConnection(JDBC_PROTOCOL+RDS_INSTANCE_HOSTNAME+":"+RDS_INSTANCE_PORT+"/"+DB_NAME, setPostgresConnectionProperties());
    }

    /**
     * This method sets the SSL properties which specify the key store file, its type and password:
     * @throws Exception
     */
    private void setSslProperties() throws Exception {
        String keyStoreFile = createKeyStoreFile();
        System.setProperty("javax.net.ssl.trustStore", keyStoreFile);
        System.setProperty("javax.net.ssl.trustStoreType", KEY_STORE_TYPE);
        System.setProperty("javax.net.ssl.trustStorePassword", DEFAULT_KEY_STORE_PASSWORD);
    }


    /**
     * This method returns the path of the Key Store File needed for the SSL verification during the IAM Database Authentication to
     * the db instance.
     * @return
     * @throws Exception
     */
    private String createKeyStoreFile() throws Exception {
        return createKeyStoreFile(createCertificate()).getPath();
    }

    /**
     *  This method generates the SSL certificate
     * @return
     * @throws Exception
     */
    private X509Certificate createCertificate() throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        URL url = new File(SSL_CERTIFICATE).toURI().toURL();
        if (url == null) {
            throw new Exception();
        }
        try (InputStream certInputStream = url.openStream()) {
            return (X509Certificate) certFactory.generateCertificate(certInputStream);
        }
    }

    /**
     * This method creates the Key Store File
     * @param rootX509Certificate - the SSL certificate to be stored in the KeyStore
     * @return
     * @throws Exception
     */
    private File createKeyStoreFile(X509Certificate rootX509Certificate) throws Exception {
        File keyStoreFile = File.createTempFile(KEY_STORE_FILE_PREFIX, KEY_STORE_FILE_SUFFIX);
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile.getPath())) {
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, KEY_STORE_PROVIDER);
            ks.load(null);
            ks.setCertificateEntry("rootCaCertificate", rootX509Certificate);
            ks.store(fos, DEFAULT_KEY_STORE_PASSWORD.toCharArray());
        }
        return keyStoreFile;
    }


    /**
     * This method sets the mysql connection properties which includes the IAM Database Authentication token
     * as the password. It also specifies that SSL verification is required.
     * @return
     */
    private Properties setPostgresConnectionProperties() {
        Properties postGresConnectionProperties = new Properties();
        postGresConnectionProperties.setProperty("verifyServerCertificate","true");
        postGresConnectionProperties.setProperty("useSSL", "true");
        postGresConnectionProperties.setProperty("user",DB_USER);
        String tempAuthTokenforRDS = generateAuthToken();
        postGresConnectionProperties.setProperty("password",tempAuthTokenforRDS);
        return postGresConnectionProperties;
    }

    /**
     * This method clears the SSL properties.
     * @throws Exception
     */
    private void clearSslProperties() throws Exception {
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStoreType");
        System.clearProperty("javax.net.ssl.trustStorePassword");
    }

}
