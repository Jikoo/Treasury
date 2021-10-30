package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.response.EconomyException;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

class PlayerAccountMigrator implements AccountMigrator<PlayerAccount> {

    @Override
    public @NotNull String getBulkFailLog(@NotNull Throwable throwable) {
        return "Unable to fetch player account UUIDs for migration: " + throwable.getMessage();
    }

    @Override
    public @NotNull String getInitLog(@NotNull UUID uuid) {
        return "Migrating player account of UUID '&b" + uuid + "&7'.";
    }

    @Override
    public @NotNull String getErrorLog(@NotNull UUID uuid, @NotNull Throwable throwable) {
        return "Error migrating account of player UUID '&b" + uuid + "&7': &b" + throwable.getMessage();
    }

    @Override
    public @NotNull Function<@NotNull EconomyProvider, @NotNull SimpleFuture<Collection<UUID>, EconomyException>> requestAccountIds() {
        return EconomyProvider::requestPlayerAccountIds;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<PlayerAccount, EconomyException>> requestAccount() {
        return EconomyProvider::requestPlayerAccount;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<Boolean, EconomyException>> checkAccountExistence() {
        return EconomyProvider::hasPlayerAccount;
    }

    @Override
    public @NotNull BiFunction<@NotNull EconomyProvider, @NotNull UUID, @NotNull SimpleFuture<PlayerAccount, EconomyException>> createAccount() {
        return EconomyProvider::createPlayerAccount;
    }

    @Override
    public @NotNull AtomicInteger getSuccessfulMigrations(@NotNull MigrationData migration) {
        return migration.playerAccountsProcessed();
    }

}
