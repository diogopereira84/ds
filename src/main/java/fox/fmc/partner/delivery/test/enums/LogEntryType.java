package fox.fmc.partner.delivery.test.enums;

import fox.fmc.partner.delivery.test.model.response.BatchResponseLogEntry;
import fox.fmc.partner.delivery.test.model.response.CommandErrorLogEntry;
import fox.fmc.partner.delivery.test.model.response.IndexHandlerResponseLogEntry;
import fox.fmc.partner.delivery.test.model.response.SqsNotificationLogEntry;
import lombok.Getter;

@Getter
public enum LogEntryType {

    /** Success Path: Standard SQS Notification */
    SQS_NOTIFICATION("SQS notification response for", SqsNotificationLogEntry.class),

    /** Success Path: Batch Controller Response */
    BATCH_RESPONSE("Batch response", BatchResponseLogEntry.class),

    /** Success/Failure Path: Index Handler Response (Nested structure) */
    INDEX_HANDLER("index.handler response", IndexHandlerResponseLogEntry.class),

    /** Failure Path: Command Routing Error (Flat structure) */
    COMMAND_ROUTING_ERROR("No command found", CommandErrorLogEntry.class);

    private final String logIdentifier;
    private final Class<?> logClass;

    LogEntryType(String logIdentifier, Class<?> logClass) {
        this.logIdentifier = logIdentifier;
        this.logClass = logClass;
    }
}