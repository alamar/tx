package models;

import java.time.Instant;
import java.util.UUID;

public class User {
    public final UUID id;
    public final String login;
    public final Instant created;

    public User(UUID id, String login, Instant created) {
        this.id = id;
        this.login = login;
        this.created = created;
    }
}
