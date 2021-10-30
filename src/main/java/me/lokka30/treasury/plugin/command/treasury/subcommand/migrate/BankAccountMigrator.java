package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.BankAccount;
import me.lokka30.treasury.api.economy.response.EconomyException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

class BankAccountMigrator implements AccountMigrator<BankAccount> {

    @Override
    public @NotNull String getBulkFailLog(@NotNull Throwable throwable) {
        return "Unable to fetch bank account UUIDs for migration: " + throwable.getMessage();
    }

    @Override
    public @NotNull String getInitLog(@NotNull UUID uuid) {
        return "Migrating bank account of UUID '&b" + uuid + "&7'.";
    }

    @Override
    public @NotNull String getErrorLog(@NotNull UUID uuid, @NotNull Throwable throwable) {
        return "Error migrating bank account UUID '&b" + uuid + "&7': &b" + throwable.getMessage();
    }

    @Override
    public @NotNull Function<@NotNull EconomyProvider, @NotNull SimpleFuture<Collection<UUID>, EconomyException>> requestAccountIds() {
        return EconomyProvider::requestBankAccountIds;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<BankAccount, EconomyException>> requestAccount() {
        return EconomyProvider::requestBankAccount;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<Boolean, EconomyException>> checkAccountExistence() {
        return EconomyProvider::hasBankAccount;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<BankAccount, EconomyException>> createAccount() {
        return EconomyProvider::createBankAccount;
    }

    @Override
    public void migrate(
            @NotNull Phaser phaser,
            @NotNull BankAccount fromAccount,
            @NotNull BankAccount toAccount,
            @NotNull MigrationData migration) {
        AccountMigrator.super.migrate(phaser, fromAccount, toAccount, migration);

        migrateUsers(phaser, fromAccount::requestBankMembersIds, toAccount::addBankMember, fromAccount.getUniqueId(), migration);
        migrateUsers(phaser, fromAccount::requestBankOwnersIds, toAccount::addBankOwner, fromAccount.getUniqueId(), migration);
    }

    private void migrateUsers(
            @NotNull Phaser phaser,
            @NotNull Supplier<SimpleFuture<Collection<UUID>, EconomyException>> getAllUsers,
            @NotNull Function<UUID, SimpleFuture<Boolean, EconomyException>> addUser,
            @NotNull UUID accountId,
            @NotNull MigrationData migration) {
        phaser.register();
        getAllUsers.get()
                .handleError(exception -> {
                    migration.debug(() -> getErrorLog(accountId, exception));
                    phaser.arriveAndDeregister();
                })
                .handle(uuids -> {
                    uuids.forEach(uuid -> {
                        phaser.register();
                        addUser.apply(uuid)
                                .handleError(exception -> {
                                    phaser.arriveAndDeregister();
                                    migration.debug(() -> getErrorLog(accountId, exception));
                                })
                                .handle(value -> phaser.arriveAndDeregister());
                    });
                    phaser.arriveAndDeregister();
                });
    }

    @Override
    public @NotNull AtomicInteger getSuccessfulMigrations(@NotNull MigrationData migration) {
        return migration.bankAccountsProcessed();
    }

}
