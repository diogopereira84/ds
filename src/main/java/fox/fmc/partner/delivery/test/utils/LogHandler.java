package fox.fmc.partner.delivery.test.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fox.fmc.partner.delivery.test.constants.BaseConstants;
import fox.fmc.partner.delivery.test.model.map.MapOfObjects;
import fox.fmc.partner.delivery.test.model.map.MapOfStrings;
import io.restassured.path.json.JsonPath;
import io.restassured.path.xml.XmlPath;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class LogHandler {
    private static final Logger logger;
    private static final String MC_ASCII = "\n" +
            "    __  ____  _________  ___   __  ____________  __  ______  ______________  _   __\n" +
            "   /  |/  / \\/ ____/   /   | / / / /_  __/ __ \\/  |/  /   |/_  __/  _/ __ \\/ | / /\n" +
            "  / /|_/ / /  /   /    / /| |/ / / / / / / / / / /|_/ / /| | / /  / // / / /  |/ / \n" +
            " / /  / /  / /___/    / ___ / /_/ / / / / /_/ / /  / / ___ |/ / _/ // /_/ / /|  /  \n" +
            "/_/  /_/  _/\\____/  /_/  |_\\____/ /_/  \\____/_/  /_/_/  |_/_/ /___/\\____/_/ |_/   \n" +
            "                                                                                          \n";
    private enum mcUtilLinkType {
        JSON, XML
    }
    static {
        // Hack to swap the ASCII art
        String s = Thread.currentThread().getStackTrace()[12].getClassName();
        logger = LogManager.getLogger(s.getClass().getName());
        logger.debug(MC_ASCII);
    }
    /**
     * Printing to Debug file
     */
    public static void debugPrintTestInitHeader() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        logger.debug("**************************************");
        logger.debug(" Starting Test: {}", testName);
    }

    public static void debugPrintHeader(String header) {
        logger.debug("============= " + header + " =============");
    }

    public static void debugPrintLocalizationHeader(String language) {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        logger.debug("**************************************");
        logger.debug(" Starting Test: {}", testName);
        logger.debug("       Language: " + language);
    }

    public static void debugPrintTestFinishedHeader() {
        logger.debug(" Test Complete!");
        logger.debug("**************************************");
    }

    public static void debugPrettyPrintRequestHeaders(String headers) {
        logger.debug(" Request Value:");
        logger.debug(" Headers: " + BaseConstants.CR_LF + headers);
    }

    public static void debugPrint(String message) {
        debugPrint(0, message);
    }

    public static void debugPrint(String message, Object... args) {
        debugPrint(0, message, args);
    }

    public static void debugPrint(int indentLevel, String message) {
        if (indentLevel == 0) {
            logger.debug(message);
        } else {
            logger.debug(StringUtils.repeat("\t", indentLevel) + message);
        }
    }

    public static void debugPrint(int indentLevel, String message, Object... args) {
        if (indentLevel == 0) {
            logger.debug(message, args);
        } else {
            logger.debug(StringUtils.repeat("\t", indentLevel) + message, args);
        }
    }

    public static void debugPrintMap(MapOfObjects map) {
        String recordFormatted;
        try {
            recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
        logger.debug(BaseConstants.CR_LF + recordFormatted);
    }

    public static void debugPrintMap(MapOfStrings map) {
        String recordFormatted;
        try {
            recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
        logger.debug(BaseConstants.CR_LF + recordFormatted);
    }

    /*
      Print a single db record map (queryForMap)
     */
    public static void debugPrintDatabaseRecords(MapOfObjects results) {
        debugPrintMap(results);
    }

    /**
     * Print multiple db record maps (queryForList)
     */
    public static void debugPrintDatabaseRecords(List<MapOfObjects> resultList) {
        if (resultList.size() > 0) {
            int recordCnt = 0;
            for (Map recordMap : resultList) {
                recordCnt++;
                String recordFormatted;
                try {
                    recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                            .writerWithDefaultPrettyPrinter().writeValueAsString(recordMap);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
                logger.debug("Record Number: {}{}{}", recordCnt, BaseConstants.CR_LF, recordFormatted);
            }
        } else {
            logger.debug("WARNING: No database records found to print!");
        }
    }

    public static void debugPrintSql(String sql, String... param) {
        StringBuilder sb = new StringBuilder()
                .append("(")
                .append(sql)
                .append(";) Parameters: ");
        if (param != null) {
            sb.append(String.join(", ", param));
        }
        logger.debug(sb.toString());
    }

    public static void debugPrintSql(String sql, Map paramMap) {
        StringBuilder sb = new StringBuilder()
                .append("(")
                .append(sql)
                .append(";) Parameters: ");
        String delimiter = "";
        if (paramMap != null) {
            for (Object key : paramMap.keySet()) {
                sb.append(delimiter).append(paramMap.get(key));
                delimiter = ", ";
            }
        }
        logger.debug(sb.toString());
    }

    public static void debugPrintSimpleDatabaseUpdate(String headerMessage, MapOfObjects paramMap) {
        LogHandler.debugPrint(1, headerMessage);
        for (String columnName : paramMap.keySet()) {
            LogHandler.debugPrint(2, columnName + ": " + paramMap.get(columnName));
        }
    }

    /**
     * Printing to Report file
     */
    public static void reportPrintTestInitHeader() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        logger.info("**************************************");
        logger.info(" Starting Test: {}", testName);
    }

    public static void reportPrintTestFinishedHeader() {
        logger.info(" Test Complete!");
        logger.info("**************************************");
    }

    public static void reportPrettyPrintResponse(Response r) {
        logger.info(" Response Value:");
        logger.info(r.getStatusLine());
        try {
            JsonPath js = new JsonPath(r.asString());
            // Use JsonPath prettify() here to avoid double-printing the response to the console.
            logger.info(BaseConstants.CR_LF + js.prettify());
        } catch (Exception e) {
            logger.info(r.asString());
        }
    }

    public static void reportPrettyPrintResponse(Response r, Boolean showHeaders) {
        logger.info(" Response Value:");
        logger.info(r.getStatusLine());
        if (showHeaders) {
            logger.info(" Headers:" + BaseConstants.CR_LF + r.getHeaders());
        }
        try {
            JsonPath js = new JsonPath(r.asString());
            // Use JsonPath prettify() here to avoid double-printing the response to the console.
            logger.info(BaseConstants.CR_LF + js.prettify());
        } catch (Exception e) {
            logger.info(r.asString());
        }
    }

    public static void reportPrettyPrintResponseXml(Response r) {
        logger.info(" Response Value:");
        try {
            XmlPath xml = new XmlPath(r.asString());
            logger.info(BaseConstants.CR_LF + xml.prettify());
        } catch (Exception e) {
            logger.info(r.asString());
        }
    }

    public static void reportPrint(String message) {
        reportPrint(0, message);
    }

    public static void reportPrint(int indentLevel, String message) {
        logger.info(StringUtils.repeat("\t", indentLevel) + message);
    }

    public static void reportPrintMQMessageBody(String msg) {
        logger.info(" MQ Message Body:");
        JsonPath js = new JsonPath(msg);
        logger.info(BaseConstants.CR_LF + js.prettify());
    }

    /**
     * Print a single db record map (queryForMap)
     */
    public static void reportPrintDatabaseRecords(MapOfObjects results) {
        String recordFormatted = "";
        try {
            recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
        logger.info(BaseConstants.CR_LF + recordFormatted);
    }

    /**
     * Print a single db record map (queryForMap)
     */
    public static void reportPrintDatabaseRecords(MapOfObjects results, String tag) {
        String recordFormatted = "";
        try {
            recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                    .writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
        logger.info(BaseConstants.CR_LF + tag + " " + recordFormatted);
    }

    /**
     * Print multiple db record maps (queryForList)
     */
    public static void reportPrintDatabaseRecords(List<MapOfObjects> resultList) {
        if (resultList.size() > 0) {
            int recordCnt = 0;
            for (Map recordMap : resultList) {
                recordCnt++;
                String recordFormatted;
                try {
                    recordFormatted = new ObjectMapper().configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                            .writerWithDefaultPrettyPrinter().writeValueAsString(recordMap);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                recordFormatted = recordFormatted.replaceAll("\\\\\"", "");
                logger.info("Record Number: " + recordCnt + BaseConstants.CR_LF + recordFormatted);
            }
        } else {
            logger.info("WARNING: No database records found to print!");
        }
    }

    /**
     * Printing to Metrics file
     */
    public static void metricsPrint(String message) {
        metricsPrint(0, message);
    }

    public static void metricsPrint(int indentLevel, String message) {
        logger.trace(StringUtils.repeat("\t", indentLevel) + message);
    }

    /**
     * Test Headers/Footers
     */
    private static String getmcUtilitiesResourcePath(mcUtilLinkType type) {
        String resourcePath = null;
        switch (type) {
            case JSON -> resourcePath = "/format/json?text=";
            case XML -> resourcePath = "/format/xml?text=";
            default -> fail("Invalid mcUtilLinkType type!");
        }
        return resourcePath;
    }

    public static void debugPrintTestStartHeader() {
        String testName = Thread.currentThread().getStackTrace()[12].getClassName();
        logger.debug("#########################################################");
        logger.debug("#####  " + testName + " :: Initialize ####");
        logger.debug("#########################################################");
    }

    public static void debugPrintTestEndHeader() {
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        logger.debug("#########################################################");
        logger.debug("#####  " + testName + " :: Successful ####");
        logger.debug("#########################################################");
    }

}