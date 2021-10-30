package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.Account;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.response.EconomyException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

interface AccountMigrator<T extends Account> {

    @NotNull String getBulkFailLog(@NotNull Throwable throwable);

    @NotNull String getInitLog(@NotNull UUID uuid);

    @NotNull String getErrorLog(@NotNull UUID uuid, @NotNull Throwable throwable);

    @NotNull Function<@NotNull EconomyProvider, @NotNull SimpleFuture<Collection<UUID>, EconomyException>> requestAccountIds();

    @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<T, EconomyException>> requestAccount();

    @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<Boolean, EconomyException>> checkAccountExistence();

    @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<T, EconomyException>> createAccount();

    default void migrate(
            @NotNull Phaser phaser,
            @NotNull T fromAccount,
            @NotNull T toAccount,
            @NotNull MigrationData migration) {
        for (Map.Entry<Currency, Currency> fromToCurrency : migration.migratedCurrencies().entrySet()) {
            Currency fromCurrency = fromToCurrency.getKey();
            Currency toCurrency = fromToCurrency.getValue();

            phaser.register();
            fromAccount.setBalance(0.0D, fromCurrency)
                    .handleError(exception -> {
                        phaser.arriveAndDeregister();
                        migration.debug(() -> getErrorLog(fromAccount.getUniqueId(), exception));
                    })
                    .flatMap(balance -> {
                        SimpleFuture<Double, EconomyException> balanceFuture;
                        if (balance < 0) {
                            balanceFuture = toAccount.withdrawBalance(balance, toCurrency);
                        } else {
                            balanceFuture = toAccount.depositBalance(balance, toCurrency);
                        }
                        balanceFuture.handleError(exception -> {
                            phaser.arriveAndDeregister();
                            migration.debug(() -> getErrorLog(fromAccount.getUniqueId(), exception));
                            migration.debug(() -> String.format(
                                    "Failed to recover from an issue transferring %s %s from %s, currency will be deleted!",
                                    balance,
                                    fromCurrency.getCurrencyName(),
                                    fromAccount.getUniqueId()));
                        });
                        return balanceFuture;
                    }).handle(value -> phaser.arriveAndDeregister());
        }
    }

    @NotNull AtomicInteger getSuccessfulMigrations(@NotNull MigrationData migration);

}
