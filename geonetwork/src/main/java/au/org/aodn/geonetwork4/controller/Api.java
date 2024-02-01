package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class Api {

    @Autowired
    protected Setup setup;

    @GetMapping("/setup")
    public ResponseEntity<?> setup() {
        setup.getMe();

        ResponseEntity<List<String>> response = setup.insertLogos(
                "/config/logos/aad_logo.json",
                "/config/logos/ace_logo.json",
                "/config/logos/aims_logo.json",
                "/config/logos/aodn_logo.json",
                "/config/logos/cdu_logo.json",
                "/config/logos/csiro_logo.json",
                "/config/logos/dsto_logo.json",
                "/config/logos/ga_logo.json",
                "/config/logos/imas_logo.json",
                "/config/logos/imos_logo.json",
                "/config/logos/mhl_logo.json",
                "/config/logos/niwa_logo.json",
                "/config/logos/noaa_logo.json",
                "/config/logos/nsw_gov_logo.json",
                "/config/logos/oeh_logo.json",
                "/config/logos/ran_logo.json",
                "/config/logos/rls_logo.json",
                "/config/logos/tpac_logo.json",
                "/config/logos/uwa_logo.json",
                "/config/logos/wamsi_logo.json"
        );
//        setup.deleteAllHarvesters();
//
//        ResponseEntity<List<HarvestersApiLegacyResponse>> response = setup.insertHarvester(
//                "/config/harvesters/catalogue_cdu_eretmochelys_imbricata.json",
//                "/config/harvesters/catalogue_cdu_lepidochelys_olivacea.json",
//                "/config/harvesters/catalogue_csiro_australian_weekly.json",
//                "/config/harvesters/catalogue_csiro_ocean_acid_recon.json",
//                "/config/harvesters/catalogue_csiro_southern_surveyor.json",
//                "/config/harvesters/catalogue_csiro_the_australian_phytoplankton_database.json",
//                "/config/harvesters/catalogue_csiro_wakmatha.json",
//                "/config/harvesters/catalogue_csiro_world_monthly.json",
//                "/config/harvesters/catalogue_dsto.json",
//                "/config/harvesters/catalogue_full.json",
//                "/config/harvesters/catalogue_full_from_geo2_to_geo3.json",
//                "/config/harvesters/catalogue_ga_mh370.json",
//                "/config/harvesters/catalogue_ga_seabed_sediments.json",
//                "/config/harvesters/catalogue_imas_aodn_portal.json",
//                "/config/harvesters/catalogue_imas_aqua_chlorophyll_concentration_monthly.json",
//                "/config/harvesters/catalogue_imas_aqua_chlorophyll_concentration_weekly.json",
//                "/config/harvesters/catalogue_imas_seaWIFS_chlorophyll_concentration_monthly.json",
//                "/config/harvesters/catalogue_imas_seaWIFS_chlorophyll_concentration_weekly.json",
//                "/config/harvesters/catalogue_imos.json",
//                "/config/harvesters/catalogue_imos_portal.json",
//                "/config/harvesters/catalogue_mhl_sea_surface_temperature_data.json",
//                "/config/harvesters/catalogue_mhl_waverider_buoys.json",
//                "/config/harvesters/catalogue_noaa.json",
//                "/config/harvesters/catalogue_nsw_oeh_bathy.json",
//                "/config/harvesters/catalogue_oeh_aodn_portal.json",
//                "/config/harvesters/catalogue_portal.json",
//                "/config/harvesters/catalogue_ran.json",
//                "/config/harvesters/catalogue_tpac_climate_futures.json",
//                "/config/harvesters/catalogue_wamsi_ningaloo_reef.json",
//                "/config/harvesters/portal_catalogue_aad.json",
//                "/config/harvesters/portal_catalogue_aims.json",
//                "/config/harvesters/portal_catalogue_aims_gbr_genomics_database_seawater_illumina_reads.json",
//                "/config/harvesters/portal_catalogue_aims_microdebris_contamination.json",
//                "/config/harvesters/portal_catalogue_aims_mmp.json",
//                "/config/harvesters/portal_catalogue_aims_weather_station.json",
//                "/config/harvesters/portal_catalogue_csiro_adcp.json",
//                "/config/harvesters/portal_catalogue_csiro_catch_operations.json",
//                "/config/harvesters/portal_catalogue_csiro_current_meter_mooring.json",
//                "/config/harvesters/portal_catalogue_csiro_mnf_voyage_tracks.json",
//                "/config/harvesters/portal_catalogue_csiro_o_and_a_ctd_data_overview.json",
//                "/config/harvesters/portal_catalogue_csiro_o_and_a_hydrology_data_overview.json",
//                "/config/harvesters/portal_catalogue_csiro_rv_franklin_xbt.json",
//                "/config/harvesters/portal_catalogue_csiro_rv_investigator_data_overview.json",
//                "/config/harvesters/portal_catalogue_csiro_rv_investigator_sst.json",
//                "/config/harvesters/portal_catalogue_csiro_rv_southern_surveyor.json",
//                "/config/harvesters/portal_catalogue_csiro_sediment_sampling.json",
//                "/config/harvesters/portal_catalogue_csiro_wildlife_observations.json",
//                "/config/harvesters/portal_catalogue_ga_marine_sediments_mars_database.json",
//                "/config/harvesters/portal_catalogue_ga_mh370_phase_1_150m_bathymetry.json",
//                "/config/harvesters/portal_catalogue_imas_aodn_portal.json",
//                "/config/harvesters/portal_catalogue_imos.json",
//                "/config/harvesters/portal_catalogue_niwa.json",
//                "/config/harvesters/portal_catalogue_oeh_aodn_portal.json",
//                "/config/harvesters/portal_catalogue_systest.json",
//                "/config/harvesters/portal_catalogue_uwa_aodn_portal.json"
//        );
        return ResponseEntity.ok(response);
    }
}
