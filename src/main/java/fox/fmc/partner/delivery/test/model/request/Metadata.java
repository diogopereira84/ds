package fox.fmc.partner.delivery.test.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {
    private List<String> collections;
    private String title;
    private String description;
    @JsonProperty("metadata")
    private Map<String, Object> metadataDetails;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EdgeInfo {
        @JsonProperty("custom_field_story_category")
        private String storyCategory;

        @JsonProperty("custom_field_edge_modified_date")
        private String edgeModifiedDate;

        @JsonProperty("custom_field_edge_station_collection")
        private String edgeStationCollection;

        @JsonProperty("custom_field_edge_status")
        private String edgeStatus;

        @JsonProperty("custom_field_story_id")
        private String storyId;

        @JsonProperty("custom_field_story_slug")
        private String storySlug;

        @JsonProperty("custom_field_edge_source_station")
        private String edgeSourceStation;

        @JsonProperty("custom_field_edge_desk")
        private String edgeDesk;

        @JsonProperty("custom_field_summary")
        private String summary;

        @JsonProperty("custom_field_edge_media_type")
        private String edgeMediaType;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowInfo {
        @JsonProperty("custom_field_transcode_job_status")
        private String transcodeJobStatus;

        @JsonProperty("custom_field_qc_status")
        private String qcStatus;

        @JsonProperty("custom_field_job_preset")
        private String jobPreset;

        @JsonProperty("custom_field_approved_date")
        private String approvedDate;

        @JsonProperty("custom_field_workflow_type")
        private String workflowType;

        @JsonProperty("custom_field_encoder_type")
        private String encoderType;

        @JsonProperty("custom_field_transcode_job_id")
        private String transcodeJobId;

        @JsonProperty("custom_field_transcoded_date")
        private String transcodedDate;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetInfo {
        @JsonProperty("Workflow Info")
        private Map<String, WorkflowInfo> workflowInfo;
    }
}
