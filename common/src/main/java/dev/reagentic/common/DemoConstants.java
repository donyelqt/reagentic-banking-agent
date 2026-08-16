package dev.reagentic.common;

import dev.reagentic.common.money.Money;

/**
 * Single source of seed truth for the demo user + their two accounts.
 * Used by auth (Flyway user seed), account (Flyway account seed), and ledger
 * (Flyway opening ledger entries) so the reconciliation invariant holds at t0
 * without any Kafka dependency.
 */
public final class DemoConstants {

    private DemoConstants() {
    }

    public static final String DEMO_USER_EMAIL = "demo@bank.dev";
    public static final String DEMO_USER_PASSWORD = "demo1234";
    public static final String DEMO_USER_ROLE = "USER";

    public static final String EMPLOYEE_USER_ID = "usr-ops-0001";
    public static final String EMPLOYEE_USER_EMAIL = "ops@bank.dev";
    public static final String EMPLOYEE_USER_PASSWORD = "ops1234";
    public static final String EMPLOYEE_USER_ROLE = "EMPLOYEE";

    public static final String CHECKING_ACCOUNT_ID = "acc-checking-0001";
    public static final String SAVINGS_ACCOUNT_ID = "acc-savings-0002";
    public static final String DEMO_USER_ID = "usr-demo-0001";

    public static final Money CHECKING_OPENING = Money.of("1000.00");
    public static final Money SAVINGS_OPENING = Money.of("500.00");

    public static final String CURRENCY = "USD";
}
