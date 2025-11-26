package fox.fmc.partner.delivery.test.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fox.fmc.partner.delivery.test.enums.PartnerType;
import fox.fmc.partner.delivery.test.service.MediaCloudService;
import fox.fmc.partner.delivery.test.service.PartnerDeliverySetupService;
import io.restassured.response.Response;
import jakarta.annotation.PostConstruct;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Awaitility-based poller over MediaCloud getAssetById.
 */
@Component
public class ProgramStatusAwaiter {

    public enum DesiredState { PUBLISH, UNPUBLISH }

    private static final Logger LOG = LoggerFactory.getLogger(ProgramStatusAwaiter.class);

    // Assuming PartnerDeliverySetupService.getAdminUser() is a static utility method.
    // If it is a Bean, this should be refactored to use dependency injection.
    private static final String USER_ADMIN = PartnerDeliverySetupService.getAdminUser();

    private static final String PUBLISHED   = "PUBLISHED";
    private static final String UNPUBLISHED = "UNPUBLISHED";

    // ---- JSON Path Constants ----
    private static final class FtsProgramJson {
        static final String DATA = "data";
        static final String METADATA = "metadata";
        static final String ASSET_INFO = "Asset Info";
        static final String FTS_PROGRAM_INFO = "FTS Program Info";
        static final String DELIVERY_INFO = "Delivery Info";
        static final String DESTINATION = "custom_field_destination";
        static final String METADATA_STATUS = "custom_field_metadata_status";
        static final String MEDIA_STATUS = "custom_field_media_status";
        private FtsProgramJson() {}
    }

    // ---- Static Façade -------------------------------------------------------
    private static ProgramStatusAwaiter INSTANCE;

    private final MediaCloudService mediaCloudService;
    private final ObjectMapper objectMapper;

    @Value("${partnerDelivery.status.await.atMostSeconds:120}")
    private long atMostSeconds;

    @Value("${partnerDelivery.status.await.pollEverySeconds:5}")
    private long pollEverySeconds;

    /**
     * Constructor Injection ensures we use the GLOBAL configured ObjectMapper.
     */
    @Autowired
    public ProgramStatusAwaiter(MediaCloudService mediaCloudService, ObjectMapper objectMapper) {
        this.mediaCloudService = mediaCloudService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // Expose this bean instance to the static context
        INSTANCE = this;
    }

    // ---- Static API -----------------------------------------------------------

    public static void awaitProgramPublished(String assetId, PartnerType partner) {
        awaitProgram(assetId, partner, DesiredState.PUBLISH);
    }

    public static void awaitProgramUnpublished(String assetId, PartnerType partner) {
        awaitProgram(assetId, partner, DesiredState.UNPUBLISH);
    }

    public static void awaitProgram(String assetId, PartnerType partner, DesiredState desiredState) {
        Objects.requireNonNull(INSTANCE, "ProgramStatusAwaiter not initialized. Ensure Spring Context is loaded.");
        INSTANCE.awaitUntil(assetId, partner, predicateFor(desiredState));
    }

    // ---- Internals ------------------------------------------------------------

    private void awaitUntil(String assetId, PartnerType partner, Predicate<StatusSnapshot> predicate) {
        Duration atMost = Duration.ofSeconds(atMostSeconds);
        Duration pollEvery = Duration.ofSeconds(pollEverySeconds);

        LOG.info("Awaiting Program Status: Asset={}, Partner={}, DesiredState={} (Timeout={}s)",
                assetId, partner, predicate, atMostSeconds);

        Awaitility.await()
                .pollInterval(pollEvery)
                .atMost(atMost)
                .conditionEvaluationListener(condition -> {
                    // Optional: Log only on failure or periodically to reduce noise
                    if (!condition.isSatisfied()) {
                        LOG.debug("Waiting... current state unsatisfied.");
                    }
                })
                .until(() -> {
                    StatusSnapshot snap = snapshot(assetId, partner);
                    LOG.info("Poll: Asset={} Partner={} -> Meta={}, Media={}",
                            assetId, partner, snap.metadataStatus, snap.mediaStatus);
                    return predicate.test(snap);
                });
    }

    private static Predicate<StatusSnapshot> predicateFor(DesiredState desired) {
        return switch (desired) {
            case PUBLISH    -> StatusSnapshot::isPublished;
            case UNPUBLISH  -> StatusSnapshot::isUnpublished;
        };
    }

    private StatusSnapshot snapshot(String assetId, PartnerType partner) {
        try {
            // Use the instance service
            String auth = mediaCloudService.getMamIdToken(USER_ADMIN);
            Response resp = mediaCloudService.getAssetById(auth, assetId);

            int http = (resp != null) ? resp.getStatusCode() : -1;
            if (http != 200) {
                LOG.warn("getAssetById failed. HTTP={} for asset={}", http, assetId);
                return StatusSnapshot.unknown();
            }

            String body = resp.asString();
            // Use the injected, configured ObjectMapper
            JsonNode root = objectMapper.readTree(body);

            JsonNode deliveryInfo = findDeliveryInfo(root);
            if (deliveryInfo == null || !deliveryInfo.isObject()) {
                return StatusSnapshot.unknown();
            }

            String partnerName = (partner != null) ? partner.toString() : "";
            Iterator<Map.Entry<String, JsonNode>> it = deliveryInfo.fields();

            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                JsonNode node = e.getValue();

                if (!containsDestination(node.path(FtsProgramJson.DESTINATION), partnerName)) {
                    continue;
                }

                String meta  = textOrNull(node, FtsProgramJson.METADATA_STATUS);
                String media = textOrNull(node, FtsProgramJson.MEDIA_STATUS);
                return new StatusSnapshot(meta, media);
            }
            return StatusSnapshot.unknown();
        } catch (Exception ex) {
            LOG.warn("Snapshot failed for asset={}: {}", assetId, ex.getMessage());
            return StatusSnapshot.unknown();
        }
    }

    private static boolean containsDestination(JsonNode destArray, String expected) {
        if (destArray == null || !destArray.isArray()) return false;
        for (JsonNode d : destArray) {
            if (expected.equalsIgnoreCase(d.asText())) return true;
        }
        return false;
    }

    private static String textOrNull(JsonNode node, String field) {
        return (node != null && node.has(field) && !node.get(field).isNull())
                ? node.get(field).asText()
                : null;
    }

    private static JsonNode findDeliveryInfo(JsonNode root) {
        JsonNode metadata = root.path(FtsProgramJson.DATA).path(FtsProgramJson.METADATA);
        if (metadata.isMissingNode()) return null;

        // Check Asset Info location
        JsonNode a = metadata.path(FtsProgramJson.ASSET_INFO).path(FtsProgramJson.DELIVERY_INFO);
        if (a.isObject()) return a;

        // Check FTS Program Info location
        JsonNode f = metadata.path(FtsProgramJson.FTS_PROGRAM_INFO).path(FtsProgramJson.DELIVERY_INFO);
        return f.isObject() ? f : null;
    }

    // ---- Value Object ---------------------------------------------------------
    static final class StatusSnapshot {
        final String metadataStatus;
        final String mediaStatus;

        StatusSnapshot(String metadataStatus, String mediaStatus) {
            this.metadataStatus = metadataStatus;
            this.mediaStatus = mediaStatus;
        }

        static StatusSnapshot unknown() { return new StatusSnapshot(null, null); }

        boolean isPublished() {
            return PUBLISHED.equalsIgnoreCase(metadataStatus)
                    && PUBLISHED.equalsIgnoreCase(mediaStatus);
        }

        boolean isUnpublished() {
            return UNPUBLISHED.equalsIgnoreCase(metadataStatus)
                    && UNPUBLISHED.equalsIgnoreCase(mediaStatus);
        }
    }
}