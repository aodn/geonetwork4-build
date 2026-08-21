package au.org.aodn.geonetwork4;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fao.geonet.domain.Setting;
import org.fao.geonet.domain.SettingDataType;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.repository.SettingRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Switch for the call to es-indexer after a metadata change, so the portal index can be protected while
 * GeoNetwork is being restored.
 *
 * Trigger: POST /{portal}/api/aodn/setup loads the harvesters (a db restore wakes them up the same way), the
 * harvesters then save thousands of metadata and each one goes down this chain, where the switch sits:
 *
 *   metadata saved / deleted (harvester, editor, PUT /srv/api/records)
 *     -> GenericEntityListener -> [this switch] -> es-indexer POST / DELETE /api/v1/indexer/index/{uuid}
 *
 * A restore, start to finish:
 *
 *   1. GeoNetwork starts. This class sets row aodn/portalSync/enabled in the Settings table to
 *      aodn.geonetwork4.portalSync.enabledOnStart (false): created when the db is empty, overwritten when the
 *      db came from a backup (where it is probably true). The switch is disabled before any harvester can run.
 *
 *   2. The harvesters run and save thousands of metadata. For each one GenericEntityListener asks isEnabled(),
 *      gets false and drops it: no call to es-indexer, the live portal index is untouched.
 *
 *   3. The harvesters finish. es-indexer runs a full reindex, which brings the portal index in line with
 *      everything dropped in step 2.
 *
 *   4. An admin enables the switch, either
 *        POST /srv/api/site/settings   aodn/portalSync/enabled=true      (admin + X-XSRF-TOKEN)
 *        or Admin console > Settings > AODN Portal > tick the box
 *      isEnabled() reads the db on every event, so it applies immediately, no restart needed.
 *
 *   5. Normal operation: each metadata change is sent to es-indexer within seconds, until the next restart
 *      (deploy, crash) repeats step 1 and disables the switch again.
 *
 * Admin console labels for this setting live in gnconfig/en-custom.json.
 */
public class PortalSyncSwitch {

    protected Logger logger = LogManager.getLogger(PortalSyncSwitch.class);

    public static final String KEY = "aodn/portalSync/enabled";

    // GeoNetwork's own settings end around 100192, so this is the last section in Admin console > Settings
    protected static final int POSITION = 100200;

    protected final boolean enabledOnStart;
    protected final SettingManager settingManager;
    protected final SettingRepository settingRepository;

    // Last value seen by isEnabled, so a change is logged once instead of every event
    protected final AtomicReference<Boolean> lastSeen = new AtomicReference<>();

    public PortalSyncSwitch(boolean enabledOnStart,
                            SettingManager settingManager,
                            SettingRepository settingRepository) {
        this.enabledOnStart = enabledOnStart;
        this.settingManager = settingManager;
        this.settingRepository = settingRepository;
    }

    /**
     * Read on every event so a change takes effect immediately, no row means disabled. This runs inside GeoNetwork's
     * JPA callback for the metadata save, so it must never throw: a db error means disabled, not a failed save.
     */
    public boolean isEnabled() {
        boolean enabled;
        try {
            enabled = settingManager.getValueAsBool(KEY, false);
        }
        catch (Exception e) {
            logger.warn("Cannot read setting '{}', treat as disabled: {}", KEY, e.getMessage());
            enabled = false;
        }
        if(!Boolean.valueOf(enabled).equals(lastSeen.getAndSet(enabled))) {
            logger.info(enabled
                    ? "Setting '{}' enabled, metadata changes are sent to es-indexer"
                    : "Setting '{}' disabled, metadata changes are not sent to es-indexer", KEY);
        }
        return enabled;
    }

    /**
     * Deliberately an overwrite and not "create if missing": a db restored from a production backup
     * carries the old value.
     */
    @PostConstruct
    public void resetOnStart() {
        Setting row = settingRepository.findById(KEY)
                .orElseGet(() -> new Setting().setName(KEY).setDataType(SettingDataType.BOOLEAN));
        settingRepository.save(row.setPosition(POSITION));

        // The value always goes through SettingManager, same path as GeoNetwork's own settings api
        settingManager.setValue(KEY, enabledOnStart);
        logger.info("Setting '{}' reset to {} on start", KEY, enabledOnStart);
    }
}
