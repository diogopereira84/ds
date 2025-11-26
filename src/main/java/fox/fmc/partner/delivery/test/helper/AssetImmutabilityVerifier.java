package fox.fmc.partner.delivery.test.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fox.fmc.partner.delivery.test.service.MediaCloudService;
import io.restassured.response.Response;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Helper component to snapshot an asset state and verify it hasn't changed.
 * Leverages MediaCloudService for API calls and Jackson for deep comparison.
 */
@Component
public class AssetImmutabilityVerifier {

    private static final Logger log = LoggerFactory.getLogger(AssetImmutabilityVerifier.class);

    private final MediaCloudService mediaCloudService;
    private final ObjectMapper objectMapper;

    // State to hold the snapshot
    private JsonNode metadataBefore;
    private Integer versionBefore;
    @Getter
    private boolean snapshotTaken = false;

    @Autowired
    public AssetImmutabilityVerifier(MediaCloudService mediaCloudService, ObjectMapper objectMapper) {
        this.mediaCloudService = mediaCloudService;
        this.objectMapper = objectMapper;
    }

    /**
     * Captures the current state of the asset (Snapshot).
     * Safely skips execution if assetId is invalid or null to prevent 404s during pre-checks.
     */
    public void captureBeforeState(String auth, String assetId) {
        // Reset state
        this.snapshotTaken = false;
        this.metadataBefore = null;
        this.versionBefore = null;

        if (assetId == null || "INVALID".equals(assetId)) {
            log.info("Skipping immutability snapshot for invalid/null assetId: {}", assetId);
            return;
        }

        log.info("Attempting to snapshot Asset ID: {}", assetId);
        Response response = mediaCloudService.getAssetById(auth, assetId);

        if (response.getStatusCode() != 200) {
            // We throw an exception here because if we can't get the "before" state,
            // the test setup is flawed.
            throw new AssertionError("Precondition failed: Could not retrieve asset '" + assetId +
                    "' for snapshot. Status: " + response.getStatusCode());
        }

        parseAndStoreSnapshot(response.asString());
    }

    /**
     * Fetches the asset again and asserts it has not changed since the snapshot.
     */
    public void verifyUnchanged(String auth, String assetId) {
        if (!snapshotTaken || metadataBefore == null) {
            log.info("Skipping immutability verification (no snapshot taken).");
            return;
        }

        log.info("Verifying immutability for Asset ID: {}", assetId);
        Response response = mediaCloudService.getAssetById(auth, assetId);

        if (response.getStatusCode() != 200) {
            throw new AssertionError("Post-check failed: Could not retrieve asset '" + assetId +
                    "'. Status: " + response.getStatusCode());
        }

        verifyJsonAgainstSnapshot(response.asString());
    }

    // ---------- Internal Logic ----------

    private void parseAndStoreSnapshot(String jsonPayload) {
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            // Navigate to data.metadata based on your provided JSON structure
            this.metadataBefore = root.path("data").path("metadata");

            if (!metadataBefore.isMissingNode()) {
                this.versionBefore = metadataBefore.path("_version").asInt();
                this.snapshotTaken = true;
                log.info("Snapshot taken. Version: {}", versionBefore);
            } else {
                log.warn("Snapshot failed: 'data.metadata' node not found in response.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse snapshot JSON", e);
        }
    }

    private void verifyJsonAgainstSnapshot(String jsonPayload) {
        try {
            JsonNode root = objectMapper.readTree(jsonPayload);
            JsonNode metadataAfter = root.path("data").path("metadata");
            int versionAfter = metadataAfter.path("_version").asInt();

            // Verification 1: Version Check (Primary Gate)
            // In Akta/MAM, if _version matches, the object is identical.
            if (versionBefore != null && versionBefore != versionAfter) {
                throw new AssertionError(String.format(
                        "Immutability Failure: Asset version advanced from %d to %d. The invalid request modified the asset.",
                        versionBefore, versionAfter
                ));
            }

            // Verification 2: Deep Content Comparison
            // Compares the entire metadata tree node-by-node
            if (!metadataBefore.equals(metadataAfter)) {
                throw new AssertionError("Immutability Failure: Asset metadata content changed despite version match.");
            }

            log.info("Immutability Verified: Asset matches version {}", versionBefore);

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to parse verification JSON", e);
        }
    }
}