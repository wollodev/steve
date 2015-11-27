package de.rwth.idsg.steve.stanApi.rest;

import de.rwth.idsg.steve.stanApi.dto.UserDTO;
import de.rwth.idsg.steve.stanApi.repository.StanUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Wolfgang Kluth on 11/11/15.
 */

@RestController
@RequestMapping(value = "/stan-api/users")
public class StanUserController {

    private static final String USER_PATH = "/{idTag}";
    private static final String DELETE_USER_PATH = "/{idTag}/delete";

    @Autowired
    private StanUserRepository userRepository;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserDTO> getUsers() {
        return userRepository.findUsers();
    }

    @RequestMapping(value = USER_PATH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO getUser(@PathVariable("idTag") String idTag) {
        return userRepository.findUser(idTag);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public void addUser(@RequestBody UserDTO user) {
        userRepository.addUser(user);
    }

    @RequestMapping(value = DELETE_USER_PATH, method = RequestMethod.POST)
    public void removeUser(@PathVariable("idTag") String idTag) {
        userRepository.deleteUser(idTag);
    }
}
