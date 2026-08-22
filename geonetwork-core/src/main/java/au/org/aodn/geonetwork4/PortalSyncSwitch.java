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
 * Trigger: metadata saved / deleted (harvester, editor, PUT /srv/api/records)
 *   -> GenericEntityListener -> [this switch] -> es-indexer POST / DELETE /api/v1/indexer/index/{uuid}
 *
 * POST /{portal}/api/aodn/setup disables the switch when it loads harvesters. It is not re-enabled automatically:
 * once harvesting and a full reindex are done, enable it manually in Admin console > Settings > AODN Portal or via
 * POST /srv/api/site/settings aodn/portalSync/enabled=true.
 *
 * A restore, start to finish:
 *
 *   1. GeoNetwork starts. A fresh database starts disabled, an existing value is read from the Settings
 *      table and survives the server restart.
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
 *   5. Normal operation: each metadata change is sent to es-indexer within seconds.
 *
 * Admin console labels for this setting live in gnconfig/en-custom.json.
 */
public class PortalSyncSwitch {

    protected Logger logger = LogManager.getLogger(PortalSyncSwitch.class);

    public static final String KEY = "aodn/portalSync/enabled";

    // GeoNetwork's own settings end around 100192, so this is the last section in Admin console > Settings
    protected static final int POSITION = 100200;

    protected final SettingManager settingManager;
    protected final SettingRepository settingRepository;

    // Last value seen by isEnabled, so a change is logged once instead of every event
    protected final AtomicReference<Boolean> lastSeen = new AtomicReference<>();

    public PortalSyncSwitch(SettingManager settingManager, SettingRepository settingRepository) {
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

    // POST /setup calls this before loading the harvesters
    public void disable() {
        settingManager.setValue(KEY, false);
        logger.info("Setting '{}' disabled by setup", KEY);
    }

    // A fresh database starts disabled, an existing value is read from the Settings table and survives server restarts
    @PostConstruct
    public void init() {
        if(settingRepository.existsById(KEY)) {
            logger.info("Setting '{}' is {}", KEY, settingManager.getValueAsBool(KEY, false) ? "enabled" : "disabled");
            return;
        }
        settingRepository.save(new Setting().setName(KEY).setDataType(SettingDataType.BOOLEAN).setPosition(POSITION));
        // The value always goes through SettingManager, same path as GeoNetwork's own settings api
        settingManager.setValue(KEY, false);
        logger.info("Setting '{}' created, disabled", KEY);
    }
}
