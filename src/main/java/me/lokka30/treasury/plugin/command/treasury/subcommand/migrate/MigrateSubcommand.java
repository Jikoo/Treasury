/*
 * Copyright (c) 2021-2021 lokka30.
 *
 * This code is part of Treasury, an Economy API for Minecraft servers. Please see <https://github.com/lokka30/Treasury> for more information on this resource.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.microlib.messaging.MultiMessage;
import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.account.Account;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.plugin.Treasury;
import me.lokka30.treasury.plugin.command.Subcommand;
import me.lokka30.treasury.plugin.debug.DebugCategory;
import me.lokka30.treasury.plugin.misc.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class MigrateSubcommand implements Subcommand {

    /*
    inf: Migrates accounts from one economy plugin to another
    cmd: /treasury migrate <providerFrom> <providerTo>
    arg:         |       0              1            2
    len:         0       1              2            3
     */

    private final @NotNull Treasury main;
    public MigrateSubcommand(@NotNull final Treasury main) { this.main = main; }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        final boolean debugEnabled = main.debugHandler.isCategoryEnabled(DebugCategory.MIGRATE_SUBCOMMAND);

        if(!Utils.checkPermissionForCommand(main, sender, "treasury.command.treasury.migrate")) return;

        if(args.length != 3) {
            new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.invalid-usage"), Arrays.asList(
                    new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true),
                    new MultiMessage.Placeholder("label", label, false)
            ));
            return;
        }

        Collection<RegisteredServiceProvider<EconomyProvider>> serviceProviders = main.getServer().getServicesManager().getRegistrations(EconomyProvider.class);
        RegisteredServiceProvider<EconomyProvider> from = null;
        RegisteredServiceProvider<EconomyProvider> to = null;

        if(serviceProviders.size() < 2) {
            new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.requires-two-providers"), Collections.singletonList(
                    new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true)
            ));
            return;
        }

        final HashSet<String> serviceProvidersNames = new HashSet<>();

        for(RegisteredServiceProvider<EconomyProvider> serviceProvider : serviceProviders) {
            serviceProvidersNames.add(serviceProvider.getPlugin().getName());
            if(debugEnabled) {
                main.debugHandler.log(DebugCategory.MIGRATE_SUBCOMMAND, "Found service provider: " + serviceProvider.getPlugin().getName());
            }
        }

        if(args[1].equalsIgnoreCase(args[2])) {
            new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.providers-match"), Arrays.asList(
                    new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true),
                    new MultiMessage.Placeholder("providers", Utils.formatListMessage(main, new ArrayList<>(serviceProvidersNames)), false)
            ));
            return;
        }

        for(RegisteredServiceProvider<EconomyProvider> serviceProvider : serviceProviders) {
            final String serviceProviderPluginName = serviceProvider.getPlugin().getName();

            if(args[1].equalsIgnoreCase(serviceProviderPluginName)) {
                from = serviceProvider;
            } else if(args[2].equalsIgnoreCase(serviceProviderPluginName)) {
                to = serviceProvider;
            }
        }

        if(from == null) {
            new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.requires-valid-from"), Arrays.asList(
                    new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true),
                    new MultiMessage.Placeholder("providers", Utils.formatListMessage(main, new ArrayList<>(serviceProvidersNames)), false)
            ));
            return;
        }

        if(to == null) {
            new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.requires-valid-to"), Arrays.asList(
                    new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true),
                    new MultiMessage.Placeholder("providers", Utils.formatListMessage(main, new ArrayList<>(serviceProvidersNames)), false)
            ));
            return;
        }

        if(debugEnabled) {
            main.debugHandler.log(DebugCategory.MIGRATE_SUBCOMMAND, "Migrating from '&b" + from.getPlugin().getName() + "&7' to '&b" + to.getPlugin().getName() + "&7'.");
        }

        new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.starting-migration"), Collections.singletonList(
                new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true)
        ));

        // Override economies with dummy economy that doesn't support any operations.
        MigrationEconomy dummyEconomy = new MigrationEconomy(main);
        main.getServer().getServicesManager().register(EconomyProvider.class, dummyEconomy, main, ServicePriority.Highest);

        // Re-register economies to ensure target economy will override migrated economy.
        reregister(from, ServicePriority.Low);
        reregister(to, ServicePriority.High);

        MigrationData migration = new MigrationData(main, from.getProvider(), to.getProvider(), debugEnabled);

        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {

            // Block until currencies have been populated.
            establishCurrencies(migration).arriveAndAwaitAdvance();

            if (migration.migratedCurrencies().isEmpty()) {
                // Nothing to migrate. Maybe a special message?
                sendMigrationMessage(sender, migration);
                return;
            }

            // Initialize account migration.
            Phaser playerMigration = migrateAccounts(migration, new PlayerAccountMigrator());
            if (migration.from().hasBankAccountSupport()) {
                if (migration.to().hasBankAccountSupport()) {
                    Phaser bankMigration = migrateAccounts(migration, new BankAccountMigrator());
                    bankMigration.arriveAndAwaitAdvance();
                } else {
                    migration.debug(() -> "'&b" + migration.to().getProvider().getName() + "&7' does not offer bank support, cannot transfer accounts.");
                }
            }

            // Block until migration is complete.
            playerMigration.arriveAndAwaitAdvance();

            // Unregister economy override.
            main.getServer().getServicesManager().unregister(dummyEconomy);

            sendMigrationMessage(sender, migration);
        });
    }

    private void reregister(RegisteredServiceProvider<EconomyProvider> serviceProvider, ServicePriority priority) {
        if (serviceProvider.getPriority() == priority) {
            return;
        }

        Plugin plugin = serviceProvider.getPlugin();
        ServicesManager servicesManager = plugin.getServer().getServicesManager();
        EconomyProvider provider = serviceProvider.getProvider();

        servicesManager.unregister(provider);
        servicesManager.register(EconomyProvider.class, provider, plugin, priority);
    }

    private void sendMigrationMessage(@NotNull CommandSender sender, @NotNull MigrationData migration) {
        new MultiMessage(main.messagesCfg.getConfig().getStringList("commands.treasury.subcommands.migrate.finished-migration"), Arrays.asList(
                new MultiMessage.Placeholder("prefix", main.messagesCfg.getConfig().getString("common.prefix"), true),
                new MultiMessage.Placeholder("time", migration.timer().getTimer() + "", false),
                new MultiMessage.Placeholder("player-accounts", migration.playerAccountsProcessed().toString(), false),
                new MultiMessage.Placeholder("bank-accounts", migration.bankAccountsProcessed().toString(), false),
                new MultiMessage.Placeholder("migrated-currencies", Utils.formatListMessage(main, migration.migratedCurrencies().keySet().stream().map(Currency::getCurrencyName).collect(Collectors.toList())), false),
                new MultiMessage.Placeholder("non-migrated-currencies", Utils.formatListMessage(main, migration.nonMigratedCurrencies()), false)
        )).send(sender);
    }

    private Phaser establishCurrencies(@NotNull MigrationData migration) {
        // Initialize phaser with 2 parties to be used to block for currency mapping completion.
        Phaser phaser = new Phaser(2);

        migration.from()
                .requestCurrencyIds()
                .handleError(exception -> {
                    phaser.arriveAndDeregister();
                    migration.debug(() -> "Failed to fetch currencies from economy '&b" + migration.from().getProvider().getName() + "&7'.");
                })
                .handle(fromCurrencyIds -> {
                    fromCurrencyIds.forEach(fromCurrencyId -> {

                        // Fetch from currency.
                        phaser.register();
                        migration.from().requestCurrency(fromCurrencyId)
                                .handleError(exception -> {
                                    // Currency not found.
                                    migration.debug(() -> "Unable to locate reported currency with ID '&b" + fromCurrencyId + "&7'.");
                                    phaser.arriveAndDeregister();
                                })
                                .handle(fromCurrency -> migration.to().requestCurrency(fromCurrency.getCurrencyName())
                                        .handleError(exception -> {
                                            // Currency not found.
                                            migration.nonMigratedCurrencies().add(fromCurrency.getCurrencyName());
                                            migration.debug(() -> "Currency of ID '&b" + fromCurrency.getCurrencyName() + "&7' will not be migrated.");
                                            phaser.arriveAndDeregister();
                                        })
                                        .handle(toCurrency -> {
                                            // Currency located, map.
                                            migration.migratedCurrencies().put(fromCurrency, toCurrency);
                                            migration.debug(() -> "Currency of ID '&b" + fromCurrency.getCurrencyName() + "&7' will be migrated.");
                                            phaser.arriveAndDeregister();
                                        }));
                    });
                    phaser.arriveAndDeregister();
                });

        return phaser;
    }

    private <T extends Account> Phaser migrateAccounts(@NotNull MigrationData migration, @NotNull AccountMigrator<T> migrator) {
        // Initialize phaser with 2 parties to be used to block for migration completion.
        Phaser phaser = new Phaser(2);

        migrator.requestAccountIds()
                .apply(migration.from())
                .handleError(exception -> {
                    migration.debug(() -> migrator.getBulkFailLog(exception));
                    phaser.arriveAndDeregister();
                })
                .handle(uuids -> {
                    uuids.forEach(uuid -> migrateAccount(phaser, uuid, migration, migrator));
                    phaser.arriveAndDeregister();
                });

        return phaser;
    }

    private <T extends Account> void migrateAccount(
            @NotNull Phaser phaser,
            @NotNull UUID uuid,
            @NotNull MigrationData migration,
            @NotNull AccountMigrator<T> migrator) {
        migration.debug(() -> migrator.getInitLog(uuid));

        // Set up logging for failure.
        // Because from and to accounts are requested in parallel, guard against duplicate failure logging.
        AtomicBoolean failed = new AtomicBoolean();
        BiConsumer<T, Throwable> failureConsumer = (account, throwable) -> {
            if (throwable != null && failed.compareAndSet(false, true)) {
                migration.debug(() -> migrator.getErrorLog(uuid, throwable));
            }
        };

        CompletableFuture<T> fromAccountFuture = new CompletableFuture<>();
        phaser.register();
        migrator.requestAccount()
                .apply(migration.from(), uuid)
                .handleError(exception -> {
                    fromAccountFuture.completeExceptionally(exception);
                    phaser.arriveAndDeregister();
                })
                .handle(fromAccount -> {
                    fromAccountFuture.complete(fromAccount);
                    phaser.arriveAndDeregister();
                });
        fromAccountFuture.whenComplete(failureConsumer);

        CompletableFuture<T> toAccountFuture = new CompletableFuture<>();
        migrator.checkAccountExistence()
                .apply(migration.to(), uuid)
                .handleError(exception -> {
                    toAccountFuture.completeExceptionally(exception);
                    phaser.arriveAndDeregister();
                })
                .handle(hasAccount -> {
                    if (hasAccount) {
                        migrator.requestAccount().apply(migration.to(), uuid)
                                .handleError(exception -> {
                                    toAccountFuture.completeExceptionally(exception);
                                    phaser.arriveAndDeregister();
                                })
                                .handle(account -> {
                                    toAccountFuture.complete(account);
                                    phaser.arriveAndDeregister();
                                });
                    }
                });
        toAccountFuture.whenComplete(failureConsumer);

        fromAccountFuture.thenAcceptBoth(toAccountFuture, (fromAccount, toAccount) -> {
            migrator.migrate(phaser, fromAccount, toAccount, migration);
            migrator.getSuccessfulMigrations(migration).incrementAndGet();
        });
    }

}
