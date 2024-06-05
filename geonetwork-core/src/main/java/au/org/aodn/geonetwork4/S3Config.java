package au.org.aodn.geonetwork4;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * https://geonetwork-opensource.org/manuals/4.0.x/en/install-guide/customizing-data-directory.html#using-a-s3-object-storage.
 *
 * This is NOT use in production as you need to set the s3 profile in order to start this bean, also you need to set
 * the GEONETWORK_STORE_TYPE: s3 in the Dockerfile so that the config-s3.xml is load and then you can replace the
 * S3Credentials here with the STS (the default S3Credentails use username/password only where assumeRole isn't support).
 *
 * The reason give up s3 in favor of EFS (elastic file system) is that GN4 do not work well with s3 and you will
 * see missing file error or file not found due to relative path.
 */
@Profile("s3")
@Configuration
public class S3Config {

//    protected Logger logger = LogManager.getLogger(S3Config.class);
//
//    @Value("${aodn.geonetwork4:DEV}")
//    protected Environment environment;
//    /**
//     * Use these ENV config and set in the Dockerfile environment
//     *
//     * GEONETWORK_STORE_TYPE: s3
//     * AWS_S3_BUCKET: s3 bucket name
//     * AWS_ACCESS_KEY_ID
//     * AWS_SECRET_ACCESS_KEY
//     * @return
//     */
//    @Primary
//    @Bean
//    public S3Credentials createS3Credentials(@Value("${AWS_ROLE_ARN:arn:aws:iam::615645230945:role/AodnDeveloperRole}") String assumeARN,
//                                             @Value("${AWS_ACCESS_KEY_ID}") String accessKeyId,
//                                             @Value("${AWS_SECRET_ACCESS_KEY}") String secretAccessKey,
//                                             @Value("${AWS_DEFAULT_REGION:ap-southeast-2}") String defaultRegion) {
//
//        // Override the default one and allow using roleArn with access token
//        return new S3Credentials() {
//
//            AmazonS3 client = null;
//
//            @Override
//            public void init() {
//                Random rand = new Random();
//
//                AWSSecurityTokenService stsClient = AWSSecurityTokenServiceAsyncClientBuilder
//                        .standard()
//                        .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
//                        .withRegion(defaultRegion)
//                        .build();
//
//                String sessionName = String.format("GN4_%s_%s", environment.name(), rand.nextInt());
//                AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
//                        .withRoleArn(assumeARN)
//                        .withRoleSessionName(sessionName);
//
//                logger.info("Created AWS STS session name {}, assume role {}.", sessionName, assumeRoleRequest);
//
//                // Assume the role and retrieve temporary credentials
//                AssumeRoleResult assumeRoleResult = stsClient.assumeRole(assumeRoleRequest);
//                Credentials sessionCredentials = assumeRoleResult.getCredentials();
//
//                // Create a BasicSessionCredentials object that contains the credentials you just retrieved.
//                BasicSessionCredentials awsCredentials = new BasicSessionCredentials(
//                        sessionCredentials.getAccessKeyId(),
//                        sessionCredentials.getSecretAccessKey(),
//                        sessionCredentials.getSessionToken());
//
//                // Use the temporary credentials to perform AWS operations
//                // For example, you can create an S3 client using these credentials
//                this.client = AmazonS3ClientBuilder.standard()
//                        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
//                        .withRegion(defaultRegion)
//                        .build();
//
//                this.setRegion(defaultRegion);
//                this.setAccessKey(accessKeyId);
//                this.setKeyPrefix("geonetwork");
//                this.setSecretKey(secretAccessKey);
//
//                logger.info("Init customized S3 client completed! Data directory now on S3.");
//            }
//
//            @Override
//            public AmazonS3 getClient() {
//                return client;
//            }
//        };
//    }
}
