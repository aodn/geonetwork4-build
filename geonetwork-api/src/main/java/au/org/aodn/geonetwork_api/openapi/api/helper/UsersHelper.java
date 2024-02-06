package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.UsersApi;
import au.org.aodn.geonetwork_api.openapi.model.User;
import au.org.aodn.geonetwork_api.openapi.model.UserDto;
import au.org.aodn.geonetwork_api.openapi.model.UserGroup;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UsersHelper {

    protected Logger logger = LogManager.getLogger(UsersHelper.class);
    protected UsersApi api;

    protected static final String ID = "id";
    protected static final String USERNAME = "username";
    protected static final String NAME = "name";
    protected static final String PROFILE = "profile";
    protected static final String PASSWORD = "password";

    public UsersHelper(UsersApi api) {
        this.api = api;
    }

    public UsersApi getApi() {return api;}

    public List<Status> createOrUpdateUsers(List<String> config) {

        return config
                .stream()
                .map(m -> {
                    JSONObject jsonObject = new JSONObject(m);

                    Status status = new Status();
                    status.setFileContent(m);

                    UserDto user = new UserDto();
                    user.setId(jsonObject.getString(ID));
                    user.setUsername(jsonObject.getString(USERNAME));
                    user.setPassword(jsonObject.getString(PASSWORD));
                    user.setName(jsonObject.getString(NAME));
                    user.setProfile(jsonObject.getString(PROFILE));

                    try {
                        List<User> userDtoList = this.api.getUsers();

                        // Search if our target user exist?
                        Optional<User> t = userDtoList
                                .stream()
                                .filter(u -> u.getName().equals(user.getName()))
                                .findFirst();

                        ResponseEntity<String> response = null;

                        if(t.isPresent()) {
                            User exist = t.get();

                            // If you changed the gn4-api.json, please make sure the email input type is
                            // list<string>, this is a bug on the server side that the DTO use setEmail() to setup
                            // the email and inside https://github.com/geonetwork/core-geonetwork/blob/04af77734ef558e38e7e06f701f6f04cbd68556b/services/src/main/java/org/fao/geonet/api/users/UsersApi.java#L90
                            // a check with getEmailAddress().isEmpty() is getting the email instead of the emailAddress variable.
                            //
                            // Therefore, if you do not set the email in the json sent to server, this value will be
                            // null and the isEmpty will throw null pointer exception.
                            user.addresses(Collections.emptyList())
                                    .email(Collections.emptyList())
                                    .emailAddresses(Collections.emptyList())
                                    .enabled(exist.getEnabled())
                                    .kind(exist.getKind())
                                    .organisation(exist.getOrganisation())
                                    .surname(exist.getSurname())
                                    .groupsEditor(Collections.emptyList())
                                    .groupsRegisteredUser(Collections.emptyList())
                                    .groupsReviewer(Collections.emptyList())
                                    .groupsUserAdmin(Collections.emptyList());

                            logger.info("User to update {}", user);
                            // TODO: We need to understand how to update user with the UserDTO as some value like group
                            // cannot be null
//                            ResponseEntity<List<UserGroup>> groups = this.getApi().retrieveUserGroupsWithHttpInfo(exist.getId());
//                            logger.info("Group for user {}, {}", user.getName(), groups.getBody());

                            response = this.getApi().updateUserWithHttpInfo(exist.getId(), user);

                        }
                        else {
                            response = this.getApi().createUserWithHttpInfo(user);
                        }

                        status.setStatus(response.getStatusCode());
                        status.setMessage(response.getBody());
                    }
                    catch (HttpClientErrorException.BadRequest badRequest) {
                        status.setStatus(HttpStatus.BAD_REQUEST);
                    }
                    finally {
                        return status;
                    }

                })
                .collect(Collectors.toList());
    }
}
