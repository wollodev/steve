package de.rwth.idsg.steve.stanApi.rest;

import de.rwth.idsg.steve.stanApi.dto.UserDTO;
import de.rwth.idsg.steve.stanApi.repository.StanUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Wolfgang Kluth on 11/11/15.
 */

@RestController
@RequestMapping("/stan-api/users")
public class StanUserController {

    private static final String USER_PATH = "/{idTag}";

    @Autowired
    private StanUserRepository userRepository;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<UserDTO> getUsers() {
        return userRepository.findUsers();
    }

    @RequestMapping(value = USER_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    UserDTO getUser(@PathVariable("idTag") String idTag) {
        return userRepository.findUser(idTag);
    }

}
