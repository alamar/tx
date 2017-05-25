package controllers;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import models.User;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.DataStore;

import static play.data.Form.*;

public class UsersController extends Controller {
    @Inject
    private DataStore dataStore = null;

    public Result getUser(UUID id) {
        User user = dataStore.getUser(id);
        return user == null ? notFound("User not found") : ok(Json.toJson(user));
    }
    
    public Result getUser(String login) {
        if (!isValidLogin(login)) {
            return badRequest("Invalid login parameter");
        }
        User user = dataStore.getUser(login);
        return user == null ? notFound("User not found") : ok(Json.toJson(user));
    }

    public Result createUser() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String login = form.get("login");
        if (login == null) {
            return badRequest("Absent login parameter");
        }
        if (!isValidLogin(login)) {
            return badRequest("Invalid login parameter");
        }
        User newUser = dataStore.addUser(new User(UUID.randomUUID(), login, Instant.now()));
        return newUser == null ? status(CONFLICT, "Login already in use") : ok(Json.toJson(newUser));
    }

    private boolean isValidLogin(String login) {
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
