package au.org.aodn.geonetwork4;

import au.org.aodn.geonetwork4.enumeration.Environment;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.api.records.attachments.ResourceLoggerStore;
import org.fao.geonet.api.records.attachments.S3Store;
import org.fao.geonet.resources.S3Credentials;
import org.fao.geonet.resources.S3Resources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Random;

/**
 * Need these bean for connect to S3, https://geonetwork-opensource.org/manuals/4.0.x/en/install-guide/customizing-data-directory.html#using-a-s3-object-storage.
 *
 * This is an experiment and leave it here for documentation, it is not recommend turning on this profile due to the
 * fact that it run on aws v1 java sdk.
 */
@Configuration
public class S3Config {

    protected Logger logger = LogManager.getLogger(S3Config.class);

    @Value("${aodn.geonetwork4:DEV}")
    protected Environment environment;

    /**
     * Use these ENV config
     *
     * AWS_S3_PREFIX
     * AWS_S3_BUCKET
     * AWS_DEFAULT_REGION
     * AWS_S3_ENDPOINT
     * AWS_ACCESS_KEY_ID
     * AWS_SECRET_ACCESS_KEY
     * @return
     */
    @Bean
    public S3Credentials createS3Credentials(@Value("${AWS_ROLE_ARN:arn:aws:iam::615645230945:role/AodnAdminRole}") String assumeARN,
                                             @Value("${AWS_ACCESS_KEY_ID}") String accessKeyId,
                                             @Value("${AWS_SECRET_ACCESS_KEY}") String secretAccessKey,
                                             @Value("${AWS_S3_PREFIX:geonetwork4-data/}")  String prefix,
                                             @Value("${AWS_DEFAULT_REGION:ap-southeast-2}") Regions defaultRegion) {

        return new S3Credentials() {

            AmazonS3 client = null;

            @Override
            public void init() {
                Random rand = new Random();

                AWSSecurityTokenService stsClient = AWSSecurityTokenServiceAsyncClientBuilder
                        .standard()
                        .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                        .withRegion(defaultRegion)
                        .build();

                String sessionName = String.format("GN4_%s_%s", environment.name(), rand.nextInt());
                AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                        .withDurationSeconds(3600)
                        .withRoleArn(assumeARN)
                        .withRoleSessionName(sessionName);

                logger.info("Created AWS STS session name {}, assume role {}.", sessionName, assumeRoleRequest);

                // Assume the role and retrieve temporary credentials
                AssumeRoleResult assumeRoleResult = stsClient.assumeRole(assumeRoleRequest);
                Credentials sessionCredentials = assumeRoleResult.getCredentials();

                // Create a BasicSessionCredentials object that contains the credentials you just retrieved.
                BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
                        sessionCredentials.getAccessKeyId(),
                        sessionCredentials.getSecretAccessKey(),
                        sessionCredentials.getSessionToken());

                // Use the temporary credentials to perform AWS operations
                // For example, you can create an S3 client using these credentials
                this.client = AmazonS3ClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                        .withRegion(defaultRegion)
                        .build();

                this.setRegion(defaultRegion.name());
                this.setKeyPrefix(prefix);
                this.setAccessKey(accessKeyId);
                this.setSecretKey(secretAccessKey);

                logger.info("Init customized S3 client completed!");
            }

            @Override
            public AmazonS3 getClient() {
                return client;
            }
        };
    }


    @Bean
    @Primary
    public S3Store createS3Store() {
        return new S3Store();
    }

    @Bean
    @Primary
    public ResourceLoggerStore createResourceLoggerStore(S3Store s3Store) {
        return new ResourceLoggerStore(s3Store);
    }

    @Bean
    @Primary
    public S3Resources createS3Resources() {
        return new S3Resources();
    }
}
