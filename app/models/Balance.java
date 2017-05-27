package models;

public class Balance {
    public final long value;

    public Balance(long value) {
        this.value = value;
    }


    public Balance invert() {
        return new Balance(-value);
    }

    public static Balance zero() {
        return new Balance(0L);
    }

    public static Balance fromValue(long value) {
        return new Balance(value);
    }
}
