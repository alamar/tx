package services;

import java.util.UUID;

import models.User;

public interface DataStore {

    User addUser(User newUser);

    User getUser(UUID id);

    User getUser(String login);

}
