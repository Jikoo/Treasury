package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.BankAccount;
import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.misc.EconomyAPIVersion;
import me.lokka30.treasury.api.economy.response.EconomyException;
import me.lokka30.treasury.api.economy.response.FailureReason;
import me.lokka30.treasury.plugin.Treasury;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * A dummy {@link EconomyProvider} used to prevent transactions during economy migration.
 *
 * @since v1.0.0
 */
class MigrationEconomy implements EconomyProvider {

    private final @NotNull Plugin plugin;
    private final @NotNull Currency currency;
    private final @NotNull EconomyException migrationException;

    MigrationEconomy(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.currency = new Currency() {
            private final UUID uuid = UUID.randomUUID();

            @Override
            public @NotNull UUID getCurrencyId() {
                return uuid;
            }

            @Override
            public @NotNull String getCurrencyName() {
                return "MigrationMoney";
            }

            @Override
            public int getRoundedDigits() {
                return 0;
            }

            @Override
            public double getStartingBalance(@Nullable UUID playerUUID) {
                return 0;
            }

            @Override
            public @NotNull String formatBalance(double amount, @NotNull Locale locale) {
                return String.valueOf(amount);
            }
        };
        this.migrationException = new EconomyException(FailureReason.MIGRATION, "Economy unavailable during migration process.");
    }

    @Override
    public @NotNull Plugin getProvider() {
        return plugin;
    }

    @Override
    public @NotNull EconomyAPIVersion getSupportedAPIVersion() {
        return Treasury.ECONOMY_API_VERSION;
    }

    @Override
    public @NotNull SimpleFuture<Boolean, EconomyException> hasPlayerAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<PlayerAccount, EconomyException> requestPlayerAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<PlayerAccount, EconomyException> createPlayerAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestPlayerAccountIds() {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Boolean, EconomyException> hasBankAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<BankAccount, EconomyException> requestBankAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<BankAccount, EconomyException> createBankAccount(@NotNull UUID accountId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestBankAccountIds() {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestCurrencyIds() {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Collection<String>, EconomyException> requestCurrencyNames() {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Currency, EconomyException> requestCurrency(@NotNull UUID currencyId) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull SimpleFuture<Currency, EconomyException> requestCurrency(@NotNull String currencyName) {
        return new FailureFuture<>(migrationException);
    }

    @Override
    public @NotNull Currency getPrimaryCurrency() {
        return currency;
    }

}
