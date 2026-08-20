package dev.reagentic.common.money;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Exact-money value object. Money is ALWAYS BigDecimal scale 2, HALF_UP.
 * JSON wire format is a plain string (e.g. "1000.00"); values with more than
 * 2 decimal places or negative signs are rejected at the boundary.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** Plain non-negative decimal with at most 2 fractional digits. Rejects
     * scientific notation ("1e2"), signs ("+5", "-5"), grouping ("1,000"),
     * and anything with more than 2 decimal places. Matches the strict money
     * grammar enforced at every API boundary. */
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, ROUNDING);
    }

    @JsonCreator
    public static Money of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Money value is required");
        }
        String trimmed = value.trim();
        if (!PLAIN_DECIMAL.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Invalid money value (expected a non-negative number with at most 2 decimal places): " + value);
        }
        BigDecimal bd;
        try {
            bd = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid money value: " + value, e);
        }
        if (bd.scale() > SCALE) {
            throw new IllegalArgumentException("Money supports at most 2 decimal places: " + value);
        }
        if (bd.signum() < 0) {
            throw new IllegalArgumentException("Money must be non-negative: " + value);
        }
        return new Money(bd);
    }

    public static Money of(BigDecimal value) {
        return of(value.toPlainString());
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money o) {
        return new Money(amount.add(o.amount));
    }

    public Money subtract(Money o) {
        return new Money(amount.subtract(o.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public int compareTo(Money o) {
        return amount.compareTo(o.amount);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @JsonValue
    public String asString() {
        return amount.toPlainString();
    }

    public BigDecimal value() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        return amount.equals(((Money) o).amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
