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

package me.lokka30.treasury.api.economy;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import me.lokka30.treasury.api.economy.account.BankAccount;
import me.lokka30.treasury.api.economy.account.PlayerAccount;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.misc.EconomyAPIVersion;
import me.lokka30.treasury.api.economy.response.EconomyException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * {@link Plugin Plugins} providing and managing economy data create a class
 * which implements this interface to be registered via the
 * {@link org.bukkit.plugin.ServicesManager ServicesManager} as a
 * {@link org.bukkit.plugin.RegisteredServiceProvider RegisteredServiceProvider&lt;EconomyProvider&gt;}.
 *
 * @author lokka30
 * @since v1.0.0
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface EconomyProvider {

    /**
     * Get the "Economy Provider" - the {@link Plugin} facilitating the economy.
     *
     * @author lokka30
     * @return the {@code Plugin} facilitating the economy
     * @since v1.0.0
     */
    @NotNull
    Plugin getProvider();

    /**
     * Get the version of the Treasury API the {@code EconomyProvider} is based on.
     *
     * <p>Please note that the Treasury API version is not the same as the Spigot API version!
     *
     * @author lokka30
     * @return the API version
     * @since v1.0.0
     */
    @NotNull
    EconomyAPIVersion getSupportedAPIVersion();

    /**
     * Check whether the economy provides {@link BankAccount} implementations.
     *
     * <p>This should be checked before using bank-related methods.
     *
     * @author lokka30, NoahvdAa
     * @return whether the economy supports bank accounts
     * @since v1.0.0
     */
    default boolean hasBankAccountSupport() { return false; }

    /**
     * Check whether the {@code EconomyProvider} calls Treasury's in-built
     * transaction events.
     *
     * <p>This should be checked before relying on Treasury's events as
     * the {@code EconomyProvider} may not have transaction event support,
     * and thus the events will never be called.
     *
     * @author lokka30, NoahvdAa
     * @return whether the economy calls Treasury's transaction events
     * @see me.lokka30.treasury.api.economy.event
     * @see me.lokka30.treasury.api.economy.event.AccountTransactionEvent
     * @since v1.0.0
     */
    default boolean hasTransactionEventSupport() { return false; }

    /**
     * Check whether the {@code EconomyProvider} supports negative
     * or below-zero balances.
     *
     * @author lokka30, NoahvdAa
     * @return whether the economy supports negative balances
     * @since v1.0.0
     */
    default boolean hasNegativeBalanceSupport() { return false; }

    /**
     * Request whether a user has an associated {@link PlayerAccount}.
     *
     * @param accountId the {@link UUID} of the account owner
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Boolean, EconomyException> hasPlayerAccount(@NotNull UUID accountId);

    /**
     * Request an existing {@link PlayerAccount} for a user.
     *
     * @param accountId the {@link UUID} of the account owner
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<PlayerAccount, EconomyException> requestPlayerAccount(@NotNull UUID accountId);

    /**
     * Request the creation of a {@link PlayerAccount} for a user.
     *
     * @param accountId the {@link UUID} of the account owner
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<PlayerAccount, EconomyException> createPlayerAccount(@NotNull UUID accountId);

    /**
     * Request all {@link UUID UUIDs} with associated {@link PlayerAccount PlayerAccounts}.
     *
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestPlayerAccountIds();

    /**
     * Request whether a {@link UUID} has an associated {@link BankAccount}.
     *
     * @param accountId the {@code UUID} of the account
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Boolean, EconomyException> hasBankAccount(@NotNull UUID accountId);

    /**
     * Request an existing {@link BankAccount} for a {@link UUID}.
     *
     * @param accountId the {@code UUID} of the account
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<BankAccount, EconomyException> requestBankAccount(@NotNull UUID accountId);

    /**
     * Request the creation of a {@link BankAccount} for a {@link UUID}.
     *
     * @param accountId the {@code UUID} of the account
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<BankAccount, EconomyException> createBankAccount(@NotNull UUID accountId);

    /**
     * Request all {@link UUID UUIDs} with associated {@link BankAccount BankAccounts}.
     *
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestBankAccountIds();

    /**
     * Request all {@link UUID UUIDs} for valid {@link Currency Currencies}.
     *
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Collection<UUID>, EconomyException> requestCurrencyIds();

    /**
     * Request all names for valid {@link Currency Currencies}.
     *
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Collection<String>, EconomyException> requestCurrencyNames();

    /**
     * Request a {@link Currency} by {@link UUID}.
     *
     * @param currencyId the {@code UUID} identifying the {@code Currency}
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Currency, EconomyException> requestCurrency(@NotNull UUID currencyId);

    /**
     * Request a {@link Currency} by name.
     *
     * @param currencyName the name of the {@code Currency}
     * @param subscription the {@link EconomySubscriber} accepting the resulting value
     * @since v1.0.0
     */
    @NotNull SimpleFuture<Currency, EconomyException> requestCurrency(@NotNull String currencyName);

    /**
     * Get the primary or main {@link Currency} of the economy.
     *
     * @return the primary currency
     * @since v1.0.0
     */
    @NotNull Currency getPrimaryCurrency();

    /**
     * Get the {@link UUID} of the primary or main {@link Currency} of the economy.
     *
     * @return the {@code UUID} identifying the primary currency
     * @since v1.0.0
     */
    default @NotNull UUID getPrimaryCurrencyId() {
        return getPrimaryCurrency().getCurrencyId();
    }

}
