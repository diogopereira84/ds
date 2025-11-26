import fox.fmc.partner.delivery.test.FmcPartnerDeliveryTestApplication
import fox.fmc.partner.delivery.test.enums.Action
import fox.fmc.partner.delivery.test.enums.LogEntryType
import fox.fmc.partner.delivery.test.enums.PartnerType
import fox.fmc.partner.delivery.test.enums.RequestType
import fox.fmc.partner.delivery.test.helper.AssetImmutabilityVerifier
import fox.fmc.partner.delivery.test.helper.CloudWatchLogTailHelper
import fox.fmc.partner.delivery.test.helper.LambdaEventHelper
import fox.fmc.partner.delivery.test.helper.ProgramStatusAwaiter
import fox.fmc.partner.delivery.test.model.response.BatchResponseLogEntry
import fox.fmc.partner.delivery.test.model.response.CommandErrorLogEntry
import fox.fmc.partner.delivery.test.model.response.IndexHandlerResponseLogEntry
import fox.fmc.partner.delivery.test.model.response.SqsNotificationLogEntry
import fox.fmc.partner.delivery.test.service.MediaCloudService
import fox.fmc.partner.delivery.test.service.PartnerDeliverySetupService
import fox.fmc.partner.delivery.test.utils.Utils
import fox.mc2.testrail.enums.TestRailsStatus
import fox.fmc.partner.delivery.test.aws.AwsLambdaUtils
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Unroll

@Slf4j
@Unroll
@SpringBootTest(classes = FmcPartnerDeliveryTestApplication.class)
class InvokeLambdaDeliveryCmsSpec extends TestRailsSpec {

    @Autowired
    AssetImmutabilityVerifier immutabilityVerifier

    @Autowired
    private MediaCloudService mediaCloudService

    @Shared
    private static String userAdmin = PartnerDeliverySetupService.getAdminUser()

    @Autowired
    AwsLambdaUtils awsLambdaUtils

    @Value('${partnerDelivery.lambda}')
    private String partnerDeliveryLambdaName;

    @Shared
    private String functionPdLambda;

    // Constants to avoid magic strings and ensure consistency
    private static final String STATUS_COMPLETED = "completed"
    private static final String STATUS_FAILED = "failed"
    private static final int STATUS_CODE_OK = 200

    @Override
    String getTitle() {
        return "Invoke CMS delivery Lambda to validate partner delivery functionality"
    }

    @Override
    String getReferences() {
        return "FMCTEST-1226"
    }

    @Override
    ArrayList getCaseIds() {
        return Arrays.asList(31496435,31496436,31496437,31496438,31496439,31496440,31496441,31496442,31496443,31496444,31496445)
    }

    def setup() {
        functionPdLambda = Utils.getTargetResource(partnerDeliveryLambdaName)
    }

    def "Verify that the program is correctly published via CMS delivery Lambda"() {
        given: "The LambdaRequest wrapped in SQS event for asset ${assetId} and partner ${partner}"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("#Invoke Lambda to CMS delivery for asset ${assetId} and partner ${partner}")

        def event = LambdaEventHelper.buildSqsEvent(objectMapper, requestType, assetId, action, partner)

        addCommentAndLog("Invoking Lambda '${functionPdLambda}' with payload:")
        addJsonFormatComment(event)

        when: "The Lambda is invoked synchronously (RequestResponse) with log tail"
        def actualResult = awsLambdaUtils.invoke(functionPdLambda, event)

        then: "Invocation succeeded and returned a JSON payload (or empty)"
        addCommentAndLog("Lambda response payload (text):")
        addJsonOrTextComment(actualResult.payloadText)

        assert actualResult.isOk() : "Lambda reported error: ${actualResult.functionError} (status=${actualResult.statusCode})"

        addCommentAndLog("Lambda actualResult summary:")
        addJsonFormatComment([
                statusCode     : actualResult.statusCode,
                functionError  : actualResult.functionError,
                executedVersion: actualResult.executedVersion,
                requestId      : actualResult.requestId
        ])

        and: "Validate SQS notification"
        def sqsLog = CloudWatchLogTailHelper.readSqsNotification(actualResult.logTail, assetId, SqsNotificationLogEntry.class, objectMapper)

        // Guard clause: Ensure the log entry exists
        assert sqsLog : "Could not find SQS Notification log for asset ${assetId}"

        // Guard clause: Ensure context is not null
        assert sqsLog.context : "SQS Log Context is null"

        with(sqsLog) {
            assert it.partner.toString() == partner.toString() : "Partner in SQS log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in SQS log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.context.status == STATUS_COMPLETED : "SQS status mismatch, expected '${STATUS_COMPLETED}', got '${it.context.status}'"
            assert it.context.statusCode == STATUS_CODE_OK : "SQS statusCode mismatch, expected '${STATUS_CODE_OK}', got '${it.context.statusCode}'"
        }

        and: "Validate Batch response"
        def batchLog = CloudWatchLogTailHelper.readBatchResponse(actualResult.logTail, BatchResponseLogEntry.class, objectMapper)

        assert batchLog : "Could not find Batch Response log"
        // List safety check: Ensure list exists and is not empty before accessing index [0]
        assert batchLog.context && !batchLog.context.isEmpty() : "Batch log context list is null or empty"

        with(batchLog.context[0]) {
            assert it.partner.toString() == partner.toString() : "Partner in batch log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in batch log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.deliveryStatus == STATUS_COMPLETED : "Batch deliveryStatus mismatch, expected '${STATUS_COMPLETED}', got '${it.deliveryStatus}'"
            // Map safety check: use ?.get() or ?. access
            assert steps?.get("delivery") == STATUS_COMPLETED : "Batch step 'delivery' status mismatch, expected '${STATUS_COMPLETED}', got '${steps?.get("delivery")}'"
        }

        and: "Validate index.handler"
        def indexLog = CloudWatchLogTailHelper.readIndexHandlerResponse(actualResult.logTail, IndexHandlerResponseLogEntry.class, objectMapper)

        assert indexLog : "Could not find Index Handler log"
        // Nested list safety check
        assert indexLog.context?.response && !indexLog.context.response.isEmpty() : "Index log response list is null or empty"

        with(indexLog.context.response[0]) {
            assert it.partner.toString() == partner.toString() : "Partner in index log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in index log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.deliveryStatus == STATUS_COMPLETED : "Index deliveryStatus mismatch, expected '${STATUS_COMPLETED}', got '${it.deliveryStatus}'"
        }

        ProgramStatusAwaiter.awaitProgram(assetId, partner, ProgramStatusAwaiter.DesiredState.PUBLISH)
        setStatus(TestRailsStatus.PASSED)

        where:
        testCase | assetId     | action                   | requestType        | partner
        31496435 | "A-830056"  | Action.EPISODE_PUBLISH   | RequestType.MANUAL | PartnerType.SPARK_FTS_CLIP
        31496436 | "A-827476"  | Action.EPISODE_PUBLISH   | RequestType.MANUAL | PartnerType.SPARK_FTS_PROGRAM
    }

    def "Verify that the program is correctly unpublished via CMS delivery Lambda"() {
        given: "The LambdaRequest wrapped in SQS event for asset ${assetId} and partner ${partner}"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("#Invoke Lambda to CMS delivery for asset ${assetId} and partner ${partner}")

        def event = LambdaEventHelper.buildSqsEvent(objectMapper, requestType, assetId, action, partner)

        addCommentAndLog("Invoking Lambda '${functionPdLambda}' with payload:")
        addJsonFormatComment(event)

        when: "The Lambda is invoked synchronously (RequestResponse) with log tail"
        def actualResult = awsLambdaUtils.invoke(functionPdLambda, event)

        then: "Invocation succeeded and returned a JSON payload (or empty)"
        addCommentAndLog("Lambda response payload (text):")
        addJsonOrTextComment(actualResult.payloadText)

        assert actualResult.isOk() : "Lambda reported error: ${actualResult.functionError} (status=${actualResult.statusCode})"

        addCommentAndLog("Lambda actualResult summary:")
        addJsonFormatComment([
                statusCode     : actualResult.statusCode,
                functionError  : actualResult.functionError,
                executedVersion: actualResult.executedVersion,
                requestId      : actualResult.requestId
        ])

        and: "Validate SQS notification"
        def sqsLog = CloudWatchLogTailHelper.readSqsNotification(actualResult.logTail, assetId, SqsNotificationLogEntry.class, objectMapper)

        assert sqsLog : "Could not find SQS Notification log for asset ${assetId}"
        assert sqsLog.context : "SQS Log Context is null"

        with(sqsLog) {
            assert it.partner.toString() == partner.toString() : "Partner in SQS log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in SQS log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.context.status == STATUS_COMPLETED : "SQS mismatch, expected '${STATUS_COMPLETED}', got '${it.context.status}'"
            assert it.context.statusCode == STATUS_CODE_OK : "SQS statusCode mismatch, expected '${STATUS_CODE_OK}', got '${it.context.statusCode}'"
        }

        and: "Validate Batch response"
        def batchLog = CloudWatchLogTailHelper.readBatchResponse(actualResult.logTail, BatchResponseLogEntry.class, objectMapper)

        assert batchLog : "Could not find Batch Response log"
        assert batchLog.context && !batchLog.context.isEmpty() : "Batch log context list is null or empty"

        with(batchLog.context[0]) {
            assert it.partner.toString() == partner.toString() : "Partner in batch log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in batch log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.deliveryStatus == STATUS_COMPLETED : "Batch deliveryStatus mismatch, expected '${STATUS_COMPLETED}', got '${it.deliveryStatus}'"
            assert it.steps?.get("delivery") == STATUS_COMPLETED : "Batch step 'delivery' status mismatch, expected '${STATUS_COMPLETED}', got '${it.steps?.get("delivery")}'"
        }

        and: "Validate index.handler"
        def indexLog = CloudWatchLogTailHelper.readIndexHandlerResponse(actualResult.logTail, IndexHandlerResponseLogEntry.class, objectMapper)

        assert indexLog : "Could not find Index Handler log"
        assert indexLog.context?.response && !indexLog.context.response.isEmpty() : "Index log response list is null or empty"

        with(indexLog.context.response[0]) {
            assert it.partner.toString() == partner.toString() : "Partner in index log mismatch, expected '${partner}', got '${it.partner}'"
            assert it.assetId == assetId : "AssetId in index log mismatch, expected '${assetId}', got '${it.assetId}'"
            assert it.deliveryStatus == STATUS_COMPLETED : "Index deliveryStatus mismatch, expected '${STATUS_COMPLETED}', got '${it.deliveryStatus}'"
        }

        ProgramStatusAwaiter.awaitProgram(assetId, partner, ProgramStatusAwaiter.DesiredState.UNPUBLISH)
        setStatus(TestRailsStatus.PASSED)

        where:
        testCase | assetId     | action                    | requestType        | partner
        31496437 | "A-830056"  | Action.EPISODE_UNPUBLISH  | RequestType.MANUAL | PartnerType.SPARK_FTS_CLIP
        31496438 | "A-827476"  | Action.EPISODE_UNPUBLISH  | RequestType.MANUAL | PartnerType.SPARK_FTS_PROGRAM
    }

    def "Verify CMS delivery Lambda request with missing mandatory fields"() {
        given: "LambdaRequest with missing mandatory fields"
        addCommentAndLog("Build Payload with Missing Fields")
        setTestId(getTestIDfromTestCase(testCase))
        setComment("Negative Lambda to CMS delivery: ${desc}")

        addCommentAndLog("Authenticate and Snapshot Asset State")
        def auth = mediaCloudService.getMamIdToken(userAdmin)
        assert auth : "Failed to obtain auth token for admin user: Token is null or empty"

        // Captures state before the invalid request.
        addCommentAndLog("Capturing 'Before' state for Asset ID: " + assetId)
        immutabilityVerifier.captureBeforeState(auth, assetId)


        // This helper allows nulls, creating the "missing field" scenario
        def event = LambdaEventHelper.buildSqsEvent(objectMapper, requestType, assetId, action, partner)

        addCommentAndLog("Invoking Lambda '${functionPdLambda}' with negative payload:")
        addJsonFormatComment(event)

        when: "Invoke synchronously and capture log tail"
        def actualResult = awsLambdaUtils.invoke(functionPdLambda, event)

        then: "Invocation succeeded (technically) but returned logical error"
        addCommentAndLog("Verify Lambda Response")
        addCommentAndLog("Lambda response payload (text):")
        addJsonOrTextComment(actualResult.payloadText)

        assert actualResult.isOk() : "Lambda reported error: ${actualResult.functionError} (status=${actualResult.statusCode})"

        addCommentAndLog("Lambda actualResult summary:")
        addJsonFormatComment([
                statusCode     : actualResult.statusCode,
                functionError  : actualResult.functionError,
                executedVersion: actualResult.executedVersion,
                requestId      : actualResult.requestId
        ])

        and: "Validate the specific error log using LogEntryType strategy"
        addCommentAndLog("Search CloudWatch Logs for Expected Validation Error")

        def logEntry = CloudWatchLogTailHelper.readLogEntry(
                actualResult.logTail,
                logType.getLogIdentifier(),
                logType.getLogClass(),
                objectMapper
        )

        assert logEntry : "Expected '${logType.getLogIdentifier()}' log entry was not found."
        addCommentAndLog("Found expected log entry: ${logType.getLogIdentifier()}")

        // Switch logic: Cast and validate based on the specific type structure
        switch (logType) {

            case LogEntryType.INDEX_HANDLER:
                def indexLog = (IndexHandlerResponseLogEntry) logEntry
                addCommentAndLog("Validating Index Handler Schema Validation...")

                def responseItem = indexLog.context?.response?.get(0)
                assert responseItem : "Index log context response is empty"

                // Validate Status
                assert responseItem.deliveryStatus == deliveryStatus : "Delivery status mismatch: expected '${deliveryStatus}', got '${responseItem.deliveryStatus}'"

                // Validate Reason Message (Safe Navigation)
                String actualReason = responseItem.reason?.message
                assert actualReason?.contains(reason) : "Reason mismatch: expected to contain '${reason}', got '${actualReason}'"

                addCommentAndLog("Verified status '${deliveryStatus}' and reason '${reason}'")
                break

            default:
                throw new IllegalArgumentException("Unsupported negative test log type: ${logType}")
        }

        and: "Verify Immutability: Ensure Asset was NOT modified"
        addCommentAndLog("Verify Asset Immutability")
        immutabilityVerifier.verifyUnchanged(auth, assetId)
        addCommentAndLog("Verified: Asset was NOT modified by the invalid request.")

        setStatus(TestRailsStatus.PASSED)

        where:
        testCase | desc                  | assetId    | action                   | requestType        | partner                   | reason                                | deliveryStatus | logType
        31496439 | "missing partner"     | "A-827476" | Action.EPISODE_PUBLISH   | RequestType.MANUAL | null                      | "Record is missing 'partner' field"   | STATUS_FAILED  | LogEntryType.INDEX_HANDLER
        31496440 | "missing action"      | "A-827476" | null                     | RequestType.MANUAL | PartnerType.SPARK_FTS_PROGRAM | "Record is missing 'action' field"    | STATUS_FAILED  | LogEntryType.INDEX_HANDLER
        31496441 | "missing requestType" | "A-827476" | Action.EPISODE_PUBLISH   | null               | PartnerType.SPARK_FTS_PROGRAM | "Record is missing 'type' field"      | STATUS_FAILED  | LogEntryType.INDEX_HANDLER
        31496442 | "missing asset id"    | null       | Action.EPISODE_PUBLISH   | RequestType.MANUAL | PartnerType.SPARK_FTS_PROGRAM | "Record is missing 'assetId' field"   | STATUS_FAILED  | LogEntryType.INDEX_HANDLER
    }

    def "Verify CMS delivery Lambda request with invalid data fields"() {
        given: "LambdaRequest with invalid data"
        setTestId(getTestIDfromTestCase(testCase))
        setComment("Negative Lambda to CMS delivery: ${desc}")

        def auth = mediaCloudService.getMamIdToken(userAdmin)

        // Validates that auth is NOT null and NOT empty
        assert auth : "Failed to obtain auth token for admin user: Token is null or empty"

        // This captures the state of the asset before the invalid request.
        immutabilityVerifier.captureBeforeState(auth, assetId)

        addComment("AssetId: " + assetId)
        // We skip the explicit getAssetById assertion here because captureBeforeState handles it
        // and throws an exception if a valid asset ID cannot be retrieved.

        def event = LambdaEventHelper.buildSqsEvent(objectMapper, requestType, assetId, action, partner)

        addCommentAndLog("Invoking Lambda '${functionPdLambda}' with negative payload:")
        addJsonFormatComment(event)

        when: "Invoke synchronously and capture log tail"
        def actualResult = awsLambdaUtils.invoke(functionPdLambda, event)

        then: "Invocation succeeded (technically) but returned logical error"
        addCommentAndLog("Lambda response payload (text):")
        addJsonOrTextComment(actualResult.payloadText)

        assert actualResult.isOk() : "Lambda reported error: ${actualResult.functionError} (status=${actualResult.statusCode})"

        addCommentAndLog("Lambda actualResult summary:")
        addJsonFormatComment([
                statusCode     : actualResult.statusCode,
                functionError  : actualResult.functionError,
                executedVersion: actualResult.executedVersion,
                requestId      : actualResult.requestId
        ])

        and: "Validate the specific error log using LogEntryType strategy"
        // generic lookup: Use the Enum to get both the String ID and the Class
        def logEntry = CloudWatchLogTailHelper.readLogEntry(
                actualResult.logTail,
                logType.getLogIdentifier(),
                logType.getLogClass(),
                objectMapper
        )

        assert logEntry : "Expected '${logType.getLogIdentifier()}' log entry was not found."

        // switch logic: cast and validate based on the specific type structure
        switch (logType) {

            case LogEntryType.COMMAND_ROUTING_ERROR:
                // Cast to specific type to access fields
                def commandLog = (CommandErrorLogEntry) logEntry
                assert commandLog.message.contains(reason) : "Reason mismatch: expected to contain '${reason}', got '${commandLog.message}'"
                break

            case LogEntryType.INDEX_HANDLER:
                def indexLog = (IndexHandlerResponseLogEntry) logEntry
                def responseItem = indexLog.context?.response?.get(0)
                assert responseItem : "Index log context response is empty"
                assert responseItem.deliveryStatus == "failed" : "Delivery status mismatch: expected 'failed', got '${responseItem.deliveryStatus}'"

                // Safe navigation for nested reason object
                String actualReason = responseItem.reason?.message
                assert actualReason?.contains(reason) : "Reason mismatch: expected to contain '${reason}', got '${actualReason}'"
                break

            default:
                throw new IllegalArgumentException("Unsupported negative test log type: ${logType}")
        }

        and: "Verify Immutability: Ensure Asset was NOT modified"
        immutabilityVerifier.verifyUnchanged(auth, assetId)

        setStatus(TestRailsStatus.PASSED)

        where:
        testCase | desc                  | assetId    | action                    | requestType         | partner                       | reason                                                              | logType
        31496443 | "invalid partner"     | "A-827476" | Action.EPISODE_PUBLISH    | RequestType.MANUAL  | PartnerType.INVALID           | "No command found for partner '${partner}'"                         | LogEntryType.COMMAND_ROUTING_ERROR
        31496444 | "invalid action"      | "A-827476" | Action.INVALID_ACTION     | RequestType.MANUAL  | PartnerType.SPARK_FTS_PROGRAM | "No command found for action '${action}' on partner '${partner}'"   | LogEntryType.COMMAND_ROUTING_ERROR
        31496445 | "invalid requestType" | "A-827476" | Action.EPISODE_PUBLISH    | RequestType.INVALID | PartnerType.SPARK_FTS_PROGRAM | "invalid not a defined validator"                                   | LogEntryType.INDEX_HANDLER
    }
}