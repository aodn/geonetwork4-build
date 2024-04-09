package au.org.aodn.geonetwork4.controller;

import au.org.aodn.geonetwork4.Setup;
import au.org.aodn.geonetwork4.model.ConfigTypes;
import au.org.aodn.geonetwork4.model.GitConfig;
import au.org.aodn.geonetwork_api.openapi.api.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class Api {

    @Autowired
    protected Setup setup;

    @DeleteMapping("/setup/harvesters")
    public ResponseEntity<?> deleteAllHarvesters() {
        setup.deleteAllHarvesters();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/setup/github")
    public ResponseEntity<?> updateConfig(List<GitConfig> gitConfig) {

        // Group the config based on type
        Map<ConfigTypes, List<GitConfig>> groups = gitConfig.stream().collect(Collectors.groupingBy(GitConfig::getType));

        for(ConfigTypes type: groups.keySet()) {
            List<GitConfig> items = groups.get(type);

            switch (type) {
                case logo: {
                    setup.insertLogos(items);
                    break;
                }
                case user: {

                }
                case group: {

                }
                case harvester: {
                    setup.deleteAllHarvesters();
                    setup.insertHarvester(items);
                    break;
                }
            }
        }
    }

//
//    @GetMapping("/setup")
//    public ResponseEntity<?> setup() {
//        setup.getMe();
//
//        setup.insertLogos(
//                "/config/logos/aad_logo.json",
//                "/config/logos/ace_logo.json",
//                "/config/logos/aims_logo.json",
//                "/config/logos/aodn_logo.json",
//                "/config/logos/cdu_logo.json",
//                "/config/logos/csiro_logo.json",
//                "/config/logos/dsto_logo.json",
//                "/config/logos/ga_logo.json",
//                "/config/logos/imas_logo.json",
//                "/config/logos/imos_logo.json",
//                "/config/logos/mhl_logo.json",
//                "/config/logos/niwa_logo.json",
//                "/config/logos/noaa_logo.json",
//                "/config/logos/nsw_gov_logo.json",
//                "/config/logos/oeh_logo.json",
//                "/config/logos/ran_logo.json",
//                "/config/logos/rls_logo.json",
//                "/config/logos/tpac_logo.json",
//                "/config/logos/uwa_logo.json",
//                "/config/logos/wamsi_logo.json"
//        );
//
//        setup.insertCategories(
//                "/config/categories/aad.json",
//                "/config/categories/aad.json",
//                "/config/categories/ace.json",
//                "/config/categories/aims.json",
//                "/config/categories/aodn.json",
//                "/config/categories/applications.json",
//                "/config/categories/audioVideo.json",
//                "/config/categories/caseStudies.json",
//                "/config/categories/cdu.json",
//                "/config/categories/csiro.json",
//                "/config/categories/datasets.json",
//                "/config/categories/directories.json",
//                "/config/categories/dsto.json",
//                "/config/categories/ga.json",
//                "/config/categories/imas.json",
//                "/config/categories/imos.json",
//                "/config/categories/interactiveResources.json",
//                "/config/categories/maps.json",
//                "/config/categories/mhl.json",
//                "/config/categories/niwa.json",
//                "/config/categories/noaa.json",
//                "/config/categories/nsw_gov.json",
//                "/config/categories/oeh.json",
//                "/config/categories/otherResources.json",
//                "/config/categories/photo.json",
//                "/config/categories/physicalSamples.json",
//                "/config/categories/proceedings.json",
//                "/config/categories/ran.json",
//                "/config/categories/registers.json",
//                "/config/categories/rls.json",
//                "/config/categories/tpac.json",
//                "/config/categories/uwa.json",
//                "/config/categories/wamsi.json",
//                "/config/categories/z3950Servers.json"
//        );
//
//        setup.insertVocabularies(
//                "/config/vocabularies/aodn_instrument.json",
//                "/config/vocabularies/aodn_organisation.json",
//                "/config/vocabularies/aodn_organisation_category.json",
//                "/config/vocabularies/aodn_parameter_category.json",
//                "/config/vocabularies/aodn_platform.json",
//                "/config/vocabularies/aodn_platform_category.json",
//                "/config/vocabularies/aodn_sampling_parameter.json",
//                "/config/vocabularies/aodn_units_of_measure.json",
//                "/config/vocabularies/australian_discovery_parameter.json",
//                "/config/vocabularies/land_masses.json",
//                "/config/vocabularies/region.json",
//                "/config/vocabularies/water_bodies.json"
//        );
//
//        setup.insertSettings(
//                "/config/settings/imos_po.json"
//        );
//
//        setup.deleteAllHarvesters();
//        setup.insertHarvester(
//                // TODO: Noted discussion here
//                // https://www.notion.so/Harvester-Migration-2eec32ca6a654fe1bfac43c4feb37878?d=e764580fcba54a48a936341470c69efd&pvs=4#96a3dd8da31a476abb0f1b15429a5a12
//                // "/config/harvesters/catalogue_oeh_aodn_portal.json",
//                // "/config/harvesters/portal_catalogue_oeh_aodn_portal.json",
//
//                // TODO: Noted discussion here
//                // https://www.notion.so/Harvester-Migration-2eec32ca6a654fe1bfac43c4feb37878?d=04e1b1d653b742ac80588ea9635ff88c&pvs=4#96a3dd8da31a476abb0f1b15429a5a12
//                // "/config/harvesters/catalog_aodn/ga_marine1.json",
//                // "/config/harvesters/catalog_aodn/ga_marine2.json",
//
//                // TODO: Noted discussion here
//                // https://www.notion.so/Harvester-Migration-2eec32ca6a654fe1bfac43c4feb37878?d=55159a6a016f454cafe253c3ec8ce4a3&pvs=4#96a3dd8da31a476abb0f1b15429a5a12
//                // "/config/harvesters/portal_catalogue_uwa_aodn_portal.json"
//
//                "/config/harvesters/catalog_aodn/aad_waf.json",
//                "/config/harvesters/catalog_aodn/aims.json",
//                "/config/harvesters/catalog_aodn/csiro_oceans_atmosphere.json",
//                "/config/harvesters/catalogue_csiro_southern_surveyor.json",
//                "/config/harvesters/catalog_aodn/imos_geonetwork.json",
//                "/config/harvesters/catalog_aodn/nci_marine.json",
//                "/config/harvesters/catalog_aodn/nci_oceanography.json",
//                "/config/harvesters/catalog_aodn/nci_physical_oceanography.json",
//                "/config/harvesters/catalogue_full.json",
//                "/config/harvesters/portal_catalogue_niwa.json",
//                "/config/harvesters/portal_catalogue_systest.json"
//
////// copy prod to systest
//////                "/config/harvesters/catalogue_portal.json",
//        );
//
//        setup.insertUsers(
//                "/config/users/admin.json"
//        );
//
//        ResponseEntity<List<Status>> response = setup.insertGroups(
//                "/config/groups/aodn.json"
//        );
//
//        return ResponseEntity.of(Optional.of(response.getStatusCodeValue()));
//    }
}
