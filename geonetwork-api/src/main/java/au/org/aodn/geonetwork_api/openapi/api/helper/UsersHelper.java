package au.org.aodn.geonetwork_api.openapi.api.helper;

import au.org.aodn.geonetwork_api.openapi.api.Status;
import au.org.aodn.geonetwork_api.openapi.api.UsersApi;
import au.org.aodn.geonetwork_api.openapi.model.User;
import au.org.aodn.geonetwork_api.openapi.model.UserDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

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
                            user.setAddresses(exist.getAddresses().stream().collect(Collectors.toList()));
                            user.setEmailAddresses(exist.getEmailAddresses().stream().collect(Collectors.toList()));
                            user.setEnabled(exist.getEnabled());
                            user.setKind(exist.getKind());
                            user.setOrganisation(exist.getOrganisation());
                            user.setSurname(exist.getSurname());
                            user.setGroupsUserAdmin(exist.);
                            response = this.api.updateUserWithHttpInfo(exist.getId(), user);

                        }
                        else {
                            response = this.api.createUserWithHttpInfo(user);
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
