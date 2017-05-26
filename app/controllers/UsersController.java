package controllers;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import models.User;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Result;
import services.DataStore;


public class UsersController extends UsersSupport {
    @Inject
    private DataStore dataStore = null;
    
    public Result getUser(String id) {
        return findUser(id).asJsonResult();
    }

    public Result createUser() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String login = form.get("login");
        if (login == null) {
            return badRequest("Absent login parameter");
        }
        if (!isValidLogin(login)) {
            return badRequest("Invalid login");
        }
        User newUser = dataStore.addUser(new User(UUID.randomUUID(), login, Instant.now()));
        return newUser == null ? status(CONFLICT, "Login already in use") : ok(Json.toJson(newUser));
    }
}
