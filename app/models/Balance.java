package models;

public class Balance {
    public final long value;

    public Balance(long value) {
        this.value = value;
    }

    public static Balance zero() {
        return new Balance(0L);
    }
}
