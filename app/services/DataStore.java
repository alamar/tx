package services;

import java.util.List;
import java.util.UUID;

import models.Account;
import models.User;

public interface DataStore {

    User addUser(User newUser);

    User getUser(UUID id);

    User getUser(String login);

    Account addAccount(Account account);

    List<Account> getAccountsBy(User user);


}
