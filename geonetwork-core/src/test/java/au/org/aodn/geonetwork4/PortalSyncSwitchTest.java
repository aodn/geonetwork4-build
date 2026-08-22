package au.org.aodn.geonetwork4;

import org.fao.geonet.domain.Setting;
import org.fao.geonet.domain.SettingDataType;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.repository.SettingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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

    protected PortalSyncSwitch createSwitch() {
        return new PortalSyncSwitch(settingManager, settingRepository);
    }

    // ---- Step 1: GeoNetwork starts ----

    /**
     * Fresh install, the Settings table has no row yet: one is created as a BOOLEAN at the last
     * position (so it shows last in the admin console) and disabled.
     */
    @Test
    public void verifyDisabledOnFreshDb() {
        when(settingRepository.existsById(KEY)).thenReturn(false);

        createSwitch().init();

        ArgumentCaptor<Setting> saved = ArgumentCaptor.forClass(Setting.class);
        verify(settingRepository, times(1)).save(saved.capture());

        assertEquals("Row name", KEY, saved.getValue().getName());
        assertEquals("Row type", SettingDataType.BOOLEAN, saved.getValue().getDataType());
        assertEquals("Row shows last in admin console", POSITION, saved.getValue().getPosition());

        verify(settingManager, times(1)).setValue(KEY, false);
    }
    /**
     * Row already there, e.g. a Fargate restart: whatever value the admin saved is kept, nothing is written.
     */
    @Test
    public void verifyValueKeptOnRestart() {
        when(settingRepository.existsById(KEY)).thenReturn(true);
        when(settingManager.getValueAsBool(KEY, false)).thenReturn(true);

        createSwitch().init();

        verify(settingRepository, never()).save(any(Setting.class));
        verify(settingManager, never()).setValue(eq(KEY), anyBoolean());
    }

    // ---- Step 2: every metadata event asks the switch ----

    /**
     * The switch is read inside GeoNetwork's save of the metadata, so a db error means disabled and
     * must never break that save.
     */
    @Test
    public void verifyDbErrorMeansDisabled() {
        when(settingManager.getValueAsBool(KEY, false)).thenThrow(new RuntimeException("db gone"));

        assertFalse("Disabled on error", createSwitch().isEnabled());
    }

    // ---- Step 4: an admin enables it, applies at once ----

    /**
     * The switch is read from GeoNetwork settings on every call, so a change by the admin applies
     * to the next metadata event, no restart needed.
     */
    @Test
    public void verifyEnabledWithoutRestart() {
        PortalSyncSwitch portalSyncSwitch = createSwitch();

        when(settingManager.getValueAsBool(KEY, false)).thenReturn(false);
        assertFalse("Disabled", portalSyncSwitch.isEnabled());

        when(settingManager.getValueAsBool(KEY, false)).thenReturn(true);
        assertTrue("Enabled after the change", portalSyncSwitch.isEnabled());
        assertEquals("Last seen follows the change", Boolean.TRUE, portalSyncSwitch.lastSeen.get());
    }
}
