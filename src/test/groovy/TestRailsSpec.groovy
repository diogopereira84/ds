import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.google.common.base.Strings

import fox.mc2.testrail.constants.TestRailsConstants
import fox.mc2.testrail.enums.TestRailsStatus
import fox.mc2.testrail.service.TestRailsService
import fox.mc2.testrail.utils.SystemUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import spock.lang.Shared
import groovy.util.logging.Slf4j

import java.text.SimpleDateFormat

@Slf4j
abstract class TestRailsSpec extends PartnerDeliveryTestBaseSpec {
    //Shared fields to handle testRails integration
    @Shared
    protected JSONArray testRailsResults //Maintain test rails results between methods, send as bulk update at cleanup
    @Shared
    protected int runId
    @Shared
    protected List<Integer> casesId = []
    //Shared fields that are used for recording results
    private String comment
    private TestRailsStatus status
    private int testId
    @Shared
    protected long start

    public static int TEST_RAILS_USER_ID = 0

    @Shared
    protected boolean logAssertions = true
    @Shared
    protected boolean logBodies = false

    private static String TEST_RAILS_USER = SystemUtils.getProperty(TestRailsConstants.TEST_RAILS_USER)
    private static String TEST_RAILS_KEY = SystemUtils.getProperty(TestRailsConstants.TEST_RAILS_PASSWORD)
    private static String TEST_RAILS_PROJECT = SystemUtils.getProperty(TestRailsConstants.TEST_RAILS_PROJECT_ID)
    private static String TEST_RAILS_SUITE = SystemUtils.getProperty(TestRailsConstants.TEST_RAILS_SUITE_ID)
    private static String TEST_RAILS_VERSION = SystemUtils.getProperty(TestRailsConstants.VERSION)

    //true will report to TestRails. Anything else will not report.
    //test_rails_user,test_rails_key,test_rails_project,test_rails_suite need to be provided otherwise no reporting
    private static boolean REPORTING = (TEST_RAILS_USER != null) && (TEST_RAILS_KEY != null ) && (TEST_RAILS_PROJECT != null ) && (TEST_RAILS_SUITE != null)

    //extending classes will implement the abstract methods to provide these necessary details
    //TestRails Title
    abstract String getTitle()
    //JIRA/confluence user story, can be comma separated list
    abstract String getReferences()
    //List of TestRails case IDs for given test automation script
    abstract ArrayList getCaseIds()

    String getComment() {
        return comment
    }

    void addComment(String comment) {
        this.comment = this.comment + comment + " \n"
    }

    void addBoldComment(String comment) {
        this.comment = this.comment + "#" + comment + "\n"
    }

    // --- helpers to keep comments consistent ---

    void addCodeBlockComment(String text) {
        addComment("```text\n${text}\n```")
        log.info(text)
    }

    void addJsonOrTextComment(String text) {
        if (text != null && text.trim().startsWith("{")) {
            addJsonFormatComment(text)
        } else if (text != null && text.trim().startsWith("[")) {
            addJsonFormatComment(text)
        } else {
            addCodeBlockComment(text ?: "")
        }
    }

    void setComment(String comment) {
        this.comment = comment + " \n"
    }

    TestRailsStatus getStatus() {
        return status
    }

    void setStatus(TestRailsStatus status) {
        this.status = status
    }

    int getTestId() {
        return testId
    }

    void setTestId(int testId) {
        this.testId = testId
        if (runId > 0 && testId > 0) {
            casesId.add(testId)
            TestRailsService.addCaseIdsToTestRun(runId, casesId, getCaseIds().isEmpty())
        }
    }

    int getTestIDfromTestCase(int id) {
        if(REPORTING){
            id
        }
        else
            return - 1
    }

    def setupSpec() {
        if(REPORTING) {
            //setup TestRails
            String date = new SimpleDateFormat("MM/dd/yyyy - HH:mm:ss").format(new Date());
            String name = getTitle() + " - Test Automation - " + date
            String description = getTitle() + " - Integration Test"
            runId = TestRailsService.createTestRun(TEST_RAILS_PROJECT, TEST_RAILS_SUITE, name, description, getCaseIds(), TEST_RAILS_USER_ID, getReferences())
            testRailsResults = new JSONArray()
        }
        else{
            log.info("Test Rails Reporting Turned Off")
            log.info("Turn Reporting ON - Provide Environment Variables: test_rails_user, test_rails_key, test_rails_project, test_rails_suite")
        }

    }

    def cleanupSpec() {
        if(REPORTING){
            TestRailsService.addRunCasesAndResults(runId, casesId, testRailsResults, getCaseIds().isEmpty())
            TestRailsService.closeRun(runId, null)
        }
    }

    def setup() {
        if(REPORTING){
            //initialize TestRails case data
            start = System.currentTimeMillis()
            testId = -1
            status = TestRailsStatus.FAILED
            comment = null
        }
    }


    def cleanup() {
        if(REPORTING){
            JSONObject result = TestRailsService.addResultToTestRun(runId, status, testId, comment, TEST_RAILS_VERSION, start, getCaseIds(), TEST_RAILS_USER_ID)
            if(result.size() > 0) testRailsResults.add(result)
        }
    }

    void addCommentAndLog(String str) {
        if(!Strings.isNullOrEmpty(str)){
            addComment(str)
            log.info("LOG :: " + str)
        }
    }

    void addJsonFormatComment (Object object) {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

        String json = ow.writeValueAsString(object);
        addCommentAndLog(json)
    }
}