package controllers;

import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import models.Account;
import models.ModelResult;
import models.User;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Result;
import services.DataStore;


public class AccountsController extends UsersSupport {
    @Inject
    private DataStore dataStore = null;
    
    public Result getAccounts(String id) {
        ModelResult<User> modelUser = findUser(id);
        for (User user : modelUser) {
            return ok(Json.toJson(dataStore.getAccountsBy(user)));
        }
        return modelUser.asJsonResult();
    }

    public Result createAccount(String id) {
        ModelResult<User> modelUser = findUser(id);
        for (User user : modelUser) {
            DynamicForm form = DynamicForm.form().bindFromRequest();
            String title = form.get("title");
            if (title == null) {
                return badRequest("Absent title parameter");
            }
            if (!title.matches("^[-_.a-zA-Z0-9 ]{0,35}$")) {
                return badRequest("Invalid title");
            }
            Account account = new Account(UUID.randomUUID(), user, title, Instant.now());
            boolean success = (dataStore.addAccount(account) != null);
            assert (success);
            return ok(Json.toJson(account));
        }
        return modelUser.asJsonResult();
    }
}
