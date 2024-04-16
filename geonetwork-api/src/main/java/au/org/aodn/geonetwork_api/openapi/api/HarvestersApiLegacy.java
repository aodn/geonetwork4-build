package au.org.aodn.geonetwork_api.openapi.api;

import au.org.aodn.geonetwork_api.openapi.api.helper.GroupsHelper;
import au.org.aodn.geonetwork_api.openapi.api.helper.TagsHelper;
import au.org.aodn.geonetwork_api.openapi.invoker.ApiClient;
import au.org.aodn.geonetwork_api.openapi.model.Group;
import au.org.aodn.geonetwork_api.openapi.model.HarvestersApiLegacyResponse;
import au.org.aodn.geonetwork_api.openapi.model.MetadataCategory;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The current api provided limited support on harvester operation, hence we implement the missing. We can demise
 * the code here in the future when api get mature.
 * We suffix it with Legacy because it used the legacy geonetwork4 api call.
 */
public class HarvestersApiLegacy extends HarvestersApi {

    protected final static String ENDPOINT_HARVESTER_ADD = "/admin.harvester.add";
    protected final static String ENDPOINT_HARVESTER_REMOVE = "/admin.harvester.remove";
    protected final static String ENDPOINT_HARVESTER_LIST = "/admin.harvester.list";

    protected Logger logger = LogManager.getLogger(HarvestersApiLegacy.class);

    protected GroupsHelper groupsHelper;
    protected TagsHelper tagsHelper;

    // We use aop proxy, so we cannot use "this" for any function call but call the bean
    @Autowired
    protected HarvestersApiLegacy proxyHarvestersApiLegacy;

    public HarvestersApiLegacy(ApiClient client, GroupsApi groupApi, TagsApi tagsApi) {
        super(client);
        this.groupsHelper = new GroupsHelper(groupApi);
        this.tagsHelper = new TagsHelper(tagsApi);
        client.setBasePath("http://localhost:8080/geonetwork/srv/eng");
    }

    @Override
    public String assignHarvestedRecordToSource(String harvesterUuid, String source) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public ResponseEntity<String> assignHarvestedRecordToSourceWithHttpInfo(String harvesterUuid, String source) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public String checkHarvesterPropertyExist(String property, String exist) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }

    @Override
    public ResponseEntity<String> checkHarvesterPropertyExistWithHttpInfo(String property, String exist) throws RestClientException {
        throw new NotImplementedException("Please use HarvesterApi instead");
    }
    /**
     * Delete all haverters found in the geonetwork4
     */
    public void deleteAllHarvesters() {
        deleteHarvester(null);
    }

    /**
     * Delete harvesters based on name
     * @param title - Contains a list of harvester name to be deleted. Null means delete all
     */
    public void deleteHarvester(String title) {
        ResponseEntity<String> harvesters = proxyHarvestersApiLegacy.getHarvestersWithHttpInfo();

        if(harvesters.getStatusCode().is2xxSuccessful() && harvesters.getBody() != null) {
            JSONObject jsonObject = XML.toJSONObject(harvesters.getBody());

            if (jsonObject.optJSONObject("nodes") != null && jsonObject.optJSONObject("nodes").optJSONArray("node") != null) {
                JSONArray nodes = jsonObject.getJSONObject("nodes").getJSONArray("node");

                for (int i = 0; i < nodes.length(); i++) {
                    JSONObject node = nodes.getJSONObject(i);

                    String name = node.getJSONObject("site") != null ?
                            node.getJSONObject("site").optString("name")
                            : null;

                    if(title == null || title.equalsIgnoreCase(name)) {
                        int id = node.getInt("id");
                        logger.info("Delete harvester id {} - {}",
                                id,
                                node.getJSONObject("site").getString("name"));

                        proxyHarvestersApiLegacy.deleteHarvesters(id);
                    }
                }
            }
        }
    }

    public void deleteHarvesters(Integer id) {
        proxyHarvestersApiLegacy.deleteHarvesterWithHttpInfo(id);
    }
    /**
     * It won't check duplicate
     * @param config - List of configuration in JSON format
     * @return The responses from server
     */
    public List<HarvestersApiLegacyResponse> createHarvesters(List<String> config) {

         return config
                .stream()
                .map(v -> {
                    Parser.Parsed parsed = null;
                    try {
                        Parser parser = new Parser();
                        parsed = parser.parseHarvestersConfig(v);

                        // Logic copy from legacy code, if group exist in the config, we need to setup the group id.
                        Optional<JSONObject> groupAttr = groupsHelper.getHarvestersOwnerGroup(parsed.getJsonObject());
                        if(groupAttr.isPresent()) {
                            // Check if group name already exist, if yes we get the group id and set the
                            // group id.
                            String groupName = groupAttr.get().optString("name");
                            Optional<Group> group = Optional.empty();

                            if(!groupName.isEmpty()) {
                                // Name have higher prefer over id as it is more accurate across different instance
                                // of geonetwork
                                group = groupsHelper.findGroupByName(groupName);
                            }

                            if(groupName.isEmpty()) {
                                // Try to find group by id, but it may fail due to different if your config
                                // is export form another instance
                                String id = groupAttr.get().optString("id");
                                if (id != null) {
                                    group = groupsHelper.findGroupById(Integer.parseInt(id));
                                }
                            }

                           if (group.isPresent()) {
                               logger.info("Group found with either name or id -> {}", group.get());
                                // Re-parse the jsonobject due to value updated
                                parsed = parser.parseHarvestersConfig(
                                        groupsHelper.updateHarvestersOwnerGroup(parsed.getJsonObject(), group.get()).toString());
                            }
                        }

                        Optional<JSONArray> categories = tagsHelper.getHarvestersCategories(parsed.getJsonObject());
                        if(categories.isPresent()) {
                            // TODO: In the legacy, there is a comment to said support first category only, Why???
                            // @craig may have info
                            Optional<MetadataCategory> category = tagsHelper.findTag(
                                    categories
                                            .get()
                                            .getJSONObject(0)
                                            .getJSONObject("category")
                                            .getString("@id"));

                            if(category.isPresent()) {
                                parsed = parser.parseHarvestersConfig(
                                        tagsHelper.updateHarvestersCategories(parsed.getJsonObject(), category.get()).toString());
                            }
                        }

                        String name = parsed.getJsonObject().getString("name");
                        logger.info("Adding harvestor config : {}", name);

                        // Delete before add to avoid duplicates
                        deleteHarvester(name);

                        ResponseEntity<Map<String, Object>> r = proxyHarvestersApiLegacy.createHarvesterWithHttpInfo(parsed);

                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(r.getStatusCode());
                        hr.setName(parsed.getJsonObject().getString("name"));

                        if(hr.getStatus().is2xxSuccessful()) {
                            hr.setId(r.getBody().get("id").toString());
                        }

                        logger.info("Done insert harvestor config : {}", parsed.getJsonObject().getString("name"));
                        return hr;
                    }
                    catch (HttpServerErrorException.InternalServerError | HttpClientErrorException.BadRequest i) {
                        logger.error("Fail to add the config, error is {} {}",
                                i.getStatusCode(),
                                i.getResponseBodyAsString());

                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(i.getStatusCode());
                        hr.setError(i.getMessage());

                        if(parsed != null) {
                            hr.setName(parsed.getJsonObject().getString("name"));
                        }

                        return hr;
                    }
                    catch (Exception e) {
                        HarvestersApiLegacyResponse hr = new HarvestersApiLegacyResponse();
                        hr.setStatus(HttpStatus.BAD_REQUEST);
                        hr.setError(String.format("Cannot parse config -> %s", v));

                        logger.error("Cannot parse config {}", v, e);

                        return hr;
                    }
                })
                .collect(Collectors.toList());
    }
    /**
     * This method is needed to allow AspectJ intercept with function ends with HttpInfo
     * @param parsed
     * @return
     */
    public ResponseEntity<Map<String, Object>> createHarvesterWithHttpInfo(Parser.Parsed parsed) {

        HttpHeaders localVarHeaderParams = new HttpHeaders();

        MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

        String[] localVarAuthNames = new String[0];
        ParameterizedTypeReference<Map<String, Object>> localReturnType = new ParameterizedTypeReference<>() {};

        String[] localVarAccepts = new String[]{"*/*", "application/json"};
        List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);

        return proxyHarvestersApiLegacy.getApiClient()
                .invokeAPI(
                        ENDPOINT_HARVESTER_ADD,
                        HttpMethod.POST,
                        Collections.EMPTY_MAP,
                        localVarQueryParams,
                        parsed.getXml(),
                        localVarHeaderParams,
                        localVarCookieParams,
                        localVarFormParams,
                        localVarAccept,
                        MediaType.APPLICATION_XML,
                        localVarAuthNames,
                        localReturnType
                );
    }

    public ResponseEntity<Map<String, Object>> deleteHarvesterWithHttpInfo(Integer id) {

        HttpHeaders localVarHeaderParams = new HttpHeaders();

        MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

        String[] localVarAuthNames = new String[0];
        ParameterizedTypeReference<Map<String, Object>> localReturnType = new ParameterizedTypeReference<>() {};

        String[] localVarAccepts = new String[]{"*/*", "application/json"};
        List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);

        localVarQueryParams.add("id", String.valueOf(id));

        return proxyHarvestersApiLegacy.getApiClient()
                .invokeAPI(
                        ENDPOINT_HARVESTER_REMOVE,
                        HttpMethod.GET,
                        Collections.EMPTY_MAP,
                        localVarQueryParams,
                        null,
                        localVarHeaderParams,
                        localVarCookieParams,
                        localVarFormParams,
                        localVarAccept,
                        MediaType.APPLICATION_JSON,
                        localVarAuthNames,
                        localReturnType
                );
    }
    /**
     * Get all harvesters, i
     *
     * @return -- An xml which looks like:
     * <?xml version="1.0" encoding="UTF-8"?>
     * <nodes>
     *     <node id="224" type="csw">
     *         <owner>
     *             <id>1</id>
     *         </owner>
     *         <ownerGroup>
     *             <id />
     *         </ownerGroup>
     *         <ownerUser>
     *             <id />
     *         </ownerUser>
     *         <site>
     *             <name>CDU Catalogue - Satellite tagging of female hawksbill sea turtles (Eretmochelys imbricata) nesting on Groote Eylandt, Northern Territory</name>
     *             <uuid>fd1c44c8-7d4d-4d4e-b9af-269fe2067631</uuid>
     *             <account>
     *                 <use>false</use>
     *                 <username />
     *                 <password />
     *             </account>
     *             <capabilitiesUrl>https://catalogue-imos.aodn.org.au/geonetwork/srv/eng/csw?request=GetCapabilities&amp;service=CSW&amp;acceptVersions=2.0.2</capabilitiesUrl>
     *             <icon>CDU_logo.gif</icon>
     *             <rejectDuplicateResource>false</rejectDuplicateResource>
     *             <hopCount>2</hopCount>
     *             <xpathFilter />
     *             <xslfilter>linkage-updater?pattern=http://geoserver-123.aodn.org.au&amp;replacement=http://geoserver-portal.aodn.org.au</xslfilter>
     *             <queryScope>local</queryScope>
     *             <outputSchema />
     *             <sortBy />
     *         </site>
     *         <content>
     *             <validate>NOVALIDATION</validate>
     *             <importxslt>none</importxslt>
     *             <batchEdits />
     *         </content>
     *         <options>
     *             <every>0 45 23 ? * *</every>
     *             <oneRunOnly>false</oneRunOnly>
     *             <overrideUuid>SKIP</overrideUuid>
     *             <status>inactive</status>
     *         </options>
     *         <privileges>
     *             <group id="1">
     *                 <operation name="view" />
     *                 <operation name="dynamic" />
     *                 <operation name="featured" />
     *             </group>
     *         </privileges>
     *         <ifRecordExistAppendPrivileges>false</ifRecordExistAppendPrivileges>
     *         <info>
     *             <lastRun />
     *             <running>false</running>
     *         </info>
     *         ....
     *     </node>
     *  </nodes>
     */
    public ResponseEntity<String> getHarvestersWithHttpInfo() {

        MultiValueMap<String, String> localVarQueryParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> localVarCookieParams = new LinkedMultiValueMap<>();
        MultiValueMap<String, Object> localVarFormParams = new LinkedMultiValueMap<>();

        String[] localVarAuthNames = new String[0];
        ParameterizedTypeReference<String> localReturnType = new ParameterizedTypeReference<>() {};

        String[] localVarAccepts = new String[]{"application/xml"};
        List<MediaType> localVarAccept = this.getApiClient().selectHeaderAccept(localVarAccepts);

        return proxyHarvestersApiLegacy.getApiClient()
                .invokeAPI(
                        ENDPOINT_HARVESTER_LIST,
                        HttpMethod.GET,
                        Collections.EMPTY_MAP,
                        localVarQueryParams,
                        null,
                        new HttpHeaders(),
                        localVarCookieParams,
                        localVarFormParams,
                        localVarAccept,
                        MediaType.APPLICATION_XML,
                        localVarAuthNames,
                        localReturnType
                );
    }
}
