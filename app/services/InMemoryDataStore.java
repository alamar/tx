package services;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import models.User;

@Singleton
public class InMemoryDataStore implements DataStore {
    private final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersByLogin = new ConcurrentHashMap<>();

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
}
