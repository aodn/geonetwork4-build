package au.org.aodn.geonetwork4;

import org.fao.geonet.domain.Setting;
import org.fao.geonet.domain.SettingDataType;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.repository.SettingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Optional;

import static au.org.aodn.geonetwork4.PortalSyncSwitch.KEY;
import static au.org.aodn.geonetwork4.PortalSyncSwitch.POSITION;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Expected behaviour of the portal sync switch, one group per step of the sequence described in
 * {@link PortalSyncSwitch}. Step 3 (full reindex) is es-indexer's job and step 5 (calls to es-indexer)
 * is covered by GenericEntityListenerTest.
 */
public class PortalSyncSwitchTest {

    protected SettingManager settingManager;
    protected SettingRepository settingRepository;

    @Before
    public void setUp() {
        settingManager = Mockito.mock(SettingManager.class);
        settingRepository = Mockito.mock(SettingRepository.class);

        when(settingRepository.save(any(Setting.class))).thenAnswer(i -> i.getArgument(0));
    }

    protected PortalSyncSwitch createSwitch(boolean enabledOnStart) {
        return new PortalSyncSwitch(enabledOnStart, settingManager, settingRepository);
    }

    // ---- Step 1: GeoNetwork starts, the switch is reset ----

    /**
     * Fresh install, the Settings table has no row yet: one is created as a BOOLEAN at the last
     * position (so it shows last in the admin console) and disabled.
     */
    @Test
    public void verifyDisabledOnStart() {
        when(settingRepository.findById(KEY)).thenReturn(Optional.empty());

        createSwitch(false).resetOnStart();

        ArgumentCaptor<Setting> saved = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository, times(1)).save(saved.capture());

        assertEquals("Row name", KEY, saved.getValue().getName());
        assertEquals("Row type", SettingDataType.BOOLEAN, saved.getValue().getDataType());
        assertEquals("Row shows last in admin console", POSITION, saved.getValue().getPosition());

        verify(settingManager, times(1)).setValue(KEY, false);
    }
    /**
     * Db restored from a production backup, where the switch was enabled: it is disabled anyway,
     * so the restore cannot push half-loaded metadata to the live portal.
     */
    @Test
    public void verifyDisabledOnStartAfterRestore() {
        Setting fromBackup = new Setting()
                .setName(KEY)
                .setDataType(SettingDataType.BOOLEAN)
                .setPosition(0)
                .setValue("true");

        when(settingRepository.findById(KEY)).thenReturn(Optional.of(fromBackup));

        createSwitch(false).resetOnStart();

        verify(settingRepository, times(1)).save(fromBackup);
        assertEquals("Position corrected on existing row", POSITION, fromBackup.getPosition());

        verify(settingManager, times(1)).setValue(KEY, false);
        verify(settingManager, never()).setValue(KEY, true);
    }
    /**
     * The start value is whatever application.properties says, so an environment that sets it
     * to true starts enabled.
     */
    @Test
    public void verifyStartValueFromProperties() {
        when(settingRepository.findById(KEY)).thenReturn(Optional.empty());

        createSwitch(true).resetOnStart();

        verify(settingManager, times(1)).setValue(KEY, true);
    }

    // ---- Step 2: every metadata event asks the switch ----

    /**
     * The switch is read inside GeoNetwork's save of the metadata, so a db error means disabled and
     * must never break that save.
     */
    @Test
    public void verifyDbErrorMeansDisabled() {
        when(settingManager.getValueAsBool(KEY, false)).thenThrow(new RuntimeException("db gone"));

        assertFalse("Disabled on error", createSwitch(false).isEnabled());
    }

    // ---- Step 4: an admin enables it, applies at once ----

    /**
     * The switch is read from GeoNetwork settings on every call, so a change by the admin applies
     * to the next metadata event, no restart needed.
     */
    @Test
    public void verifyEnabledWithoutRestart() {
        PortalSyncSwitch portalSyncSwitch = createSwitch(false);

        when(settingManager.getValueAsBool(KEY, false)).thenReturn(false);
        assertFalse("Disabled", portalSyncSwitch.isEnabled());

        when(settingManager.getValueAsBool(KEY, false)).thenReturn(true);
        assertTrue("Enabled after the change", portalSyncSwitch.isEnabled());
        assertEquals("Last seen follows the change", Boolean.TRUE, portalSyncSwitch.lastSeen.get());
    }
}
