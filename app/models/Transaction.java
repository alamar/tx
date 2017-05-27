package models;

import java.time.Instant;
import java.util.UUID;

public class Transaction {
    public final UUID id;
    public final UUID from;
    public final UUID to;
    public final Balance amount;
    public final Instant created;

    public Transaction(UUID id, UUID from, UUID to, Balance amount, Instant created) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.created = created;
    }

    public Transaction invert() {
        return new Transaction(id, to, from, amount.invert(), created);
    }
}
