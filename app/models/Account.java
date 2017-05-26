package models;

import java.time.Instant;
import java.util.UUID;

public class Account {
    public final UUID id;
    public final User user;
    public final String title;
    public final Instant created;
    public Balance balance;

    public Account(UUID id, User user, String title, Instant created) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.created = created;
        this.balance = Balance.zero();
    }
}
