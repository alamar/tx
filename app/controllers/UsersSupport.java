package controllers;

import java.util.UUID;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import models.ModelResult;
import models.User;
import play.mvc.Controller;
import services.DataStore;


public abstract class UsersSupport extends Controller {
    @Inject
    private DataStore usersSupportDataStore = null;

    protected ModelResult<User> findUser(String uuidOrLogin) {
        User user;
        try {
            UUID uuid = UUID.fromString(uuidOrLogin);
            user = usersSupportDataStore.getUser(uuid);
        } catch (IllegalArgumentException iae) {
            if (!isValidLogin(uuidOrLogin)) {
                return ModelResult.<User>fail(badRequest("Invalid login parameter"));
            }
            user = usersSupportDataStore.getUser(uuidOrLogin);
        }
        
        return (user == null)
                ? ModelResult.<User>fail(notFound("User not found"))
                : ModelResult.model(user);
    }

    protected boolean isValidLogin(String login) {
        if (login.matches("^[a-zA-Z][-_.a-zA-Z0-9]{0,34}$")) {
            return true;
        }
        try {
            new InternetAddress(login).validate();
            return true;
        } catch (AddressException ae) {
        }
        return false;
    }
}
