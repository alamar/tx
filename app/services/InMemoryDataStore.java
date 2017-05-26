package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import models.Account;
import models.User;

@Singleton
public class InMemoryDataStore implements DataStore {
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Account>> accountsByUser = new ConcurrentHashMap<>();

    @Override
    public synchronized User addUser(User newUser) {
        if (users.containsKey(newUser.id) || usersByLogin.containsKey(newUser.login)) {
            return null;
        }
        users.put(newUser.id, newUser);
        usersByLogin.put(newUser.login, newUser);
        return newUser;
    }

    @Override
    public User getUser(UUID id) {
        return users.get(id);
    }

    @Override
    public User getUser(String login) {
        return usersByLogin.get(login);
    }

    @Override
    public Account addAccount(Account account) {
        if (accounts.putIfAbsent(account.id, account) != null) {
            return null;
        }
        List<Account> existing;
        List<Account> modified;
        do {
            existing = accountsByUser.get(account.user.id);
            if (existing == null) {
                modified = Collections.singletonList(account);
            } else {
                List<Account> copy = new ArrayList<>(existing);
                copy.add(account);
                modified = Collections.unmodifiableList(copy);
            }
        } while ((existing == null)
                    ? (accountsByUser.putIfAbsent(account.user.id, modified) != null)
                    : !accountsByUser.replace(account.user.id, existing, modified));
        return account;
    }

    @Override
    public List<Account> getAccountsBy(User user) {
        return accountsByUser.getOrDefault(user.id, Collections.<Account>emptyList());
    }
}
