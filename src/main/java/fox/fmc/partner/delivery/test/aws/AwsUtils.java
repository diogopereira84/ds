package fox.fmc.partner.delivery.test.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AwsUtils {

    public AWSCredentialsProvider getAWSCredentialsProvider(String profileName) {
        AWSCredentialsProvider awsCredentialsProvider;

        String awsAccessKey = SystemUtils.getProperty("AWS_ACCESS_KEY_ID");
        String awsSecretKey = SystemUtils.getProperty("AWS_SECRET_ACCESS_KEY");

        if (StringUtils.hasText(awsAccessKey) && StringUtils.hasText(awsSecretKey)) {
            awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
            return awsCredentialsProvider;
        }

        if (!StringUtils.hasText(profileName)) {
            throw new IllegalStateException("AWS profile name is required");
        }

        awsCredentialsProvider = new ProfileCredentialsProvider(profileName);
        return awsCredentialsProvider;
    }

}
