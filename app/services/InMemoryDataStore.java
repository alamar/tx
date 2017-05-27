package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import models.Account;
import models.Transaction;
import models.User;

@Singleton
public class InMemoryDataStore implements DataStore {
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Account>> accountsByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Transaction>> transactionsByAccount = new ConcurrentHashMap<>();

    @Override
    public synchronized User addUser(User user) {
        if (users.containsKey(user.id) || usersByLogin.containsKey(user.login)) {
            return null;
        }
        users.put(user.id, user);
        usersByLogin.put(user.login, user);
        return user;
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

    @Override
    public Account getAccount(UUID id) {
        return accounts.get(id);
    }

    @Override
    public List<Transaction> getTransactionsOn(Account account) {
        return transactionsByAccount.getOrDefault(account.id, Collections.<Transaction>emptyList());
    }

    @Override
    public void addTransactionToAccount(Account account, Transaction tx) {
        List<Transaction> existing;
        List<Transaction> modified;
        do {
            existing = transactionsByAccount.get(account.id);
            if (existing == null) {
                modified = Collections.singletonList(tx);
            } else {
                List<Transaction> copy = new ArrayList<>(existing);
                copy.add(tx);
                modified = Collections.unmodifiableList(copy);
            }
        } while ((existing == null)
                    ? (transactionsByAccount.putIfAbsent(account.id, modified) != null)
                    : !transactionsByAccount.replace(account.id, existing, modified));
    }
}
