package fox.fmc.partner.delivery.test.aws;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwsSqsUtils {
    private final String profileName;
    private final String awsRegion;

    @Autowired
    private AwsUtils awsUtils;

    public AwsSqsUtils(
            @Value("${aws.profile.name}") String profileName,
            @Value("${aws.region}") String awsRegion) {
        this.profileName = profileName;
        this.awsRegion = awsRegion;
    }

    private AmazonSQS buildClient() {
        return AmazonSQSClient.builder()
                .withRegion(awsRegion)
                .withCredentials(awsUtils.getAWSCredentialsProvider(profileName))
                .build();
    }

    public void sendSQSMessage(String queueName, String messageBody) {
        AmazonSQS amazonSqsClient = buildClient();

        GetQueueUrlResult getQueueResult = amazonSqsClient.getQueueUrl(queueName);
        SendMessageRequest request = new SendMessageRequest()
                .withQueueUrl(getQueueResult.getQueueUrl())
                .withMessageBody(messageBody)
                .withDelaySeconds(10);
        amazonSqsClient.sendMessage(request);
    }
}