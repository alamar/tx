package services;

import java.util.List;
import java.util.UUID;

import models.Account;
import models.Transaction;
import models.User;

public interface DataStore {

    User addUser(User newUser);

    User getUser(UUID id);

    User getUser(String login);

    Account addAccount(Account account);

    Account getAccount(UUID uuid);

    List<Account> getAccountsBy(User user);

    List<Transaction> getTransactionsOn(Account account);

    void addTransactionToAccount(Account fromAccount, Transaction tx);

}
