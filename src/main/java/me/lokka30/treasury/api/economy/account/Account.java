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

package me.lokka30.treasury.api.economy.account;

import me.lokka30.treasury.api.economy.EconomyProvider;
import me.lokka30.treasury.api.economy.currency.Currency;
import me.lokka30.treasury.api.economy.response.EconomyResponse;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author lokka30, Geolykt
 * @since v1.0.0
 * @see EconomyProvider
 * @see PlayerAccount
 * @see BankAccount
 * An Account is something that holds a balance and is associated with
 * something bound by a UUID. For example, a PlayerAccount is bound to
 * a Player on a server by their UUID.
 */
@SuppressWarnings({"unused", "RedundantThrows"})
public interface Account {

    /**
     * @author lokka30
     * @since v1.0.0
     * @see UUID
     * Get the UUID of the Account.
     * @return uuid of the Account.
     */
    @NotNull UUID getUniqueId();

    /**
     * @author lokka30, Geolykt
     * @since v1.0.0
     * @see Account#setBalance(double, Currency)
     * Get the balance of the Account.
     * @param currency of the balance being requested.
     * @return the balance of the account with specified currency.
     */
    @NotNull
    EconomyResponse<Double> getBalance(@NotNull Currency currency);

    /**
     * @author lokka30, Geolykt
     * @since v1.0.0
     * @see Account#getBalance(Currency)
     * Set the balance of the Account.
     * Specified amounts must be AT OR ABOVE zero.
     * @param amount of money the new balance will be.
     * @param currency of the balance being set.
     * @return the account's new balance
     */
    @NotNull
    EconomyResponse<Double> setBalance(double amount, @NotNull Currency currency);

    /**
     * @author lokka30, Geolykt
     * @since v1.0.0
     * @see Account#setBalance(double, Currency)
     * Withdraw an amount from the Account's balance.
     * Specified amounts must be ABOVE zero.
     * @param amount of money the account's current balance should be reduced by.
     * @param currency of the balance being set.
     * @return the account's new balance
     */
    @NotNull
    EconomyResponse<Double> withdrawBalance(double amount, @NotNull Currency currency);

    /**
     * @author lokka30
     * @since v1.0.0
     * @see Account#setBalance(double, Currency)
     * Deposit an amount into the Account's balance.
     * Specified amounts must be ABOVE zero.
     * @param amount of money the account's current balance should be increased by.
     * @param currency of the balance being set.
     * @return the account's new balance
     */
    @NotNull
    EconomyResponse<Double> depositBalance(double amount, @NotNull Currency currency);

    /**
     * @author lokka30, Geolykt
     * @since v1.0.0
     * @see PlayerAccount#resetBalance(Currency)
     * @see Account#setBalance(double, Currency)
     * Sets the Account's balance to `BigDecimal.ZERO`.
     * PlayerAccounts, by default, do not reset to `BigDecimal.ZERO` as they are overriden.
     * @param currency of the balance being set.
     * @return the account's new balance
     */
    @NotNull
    default EconomyResponse<Double> resetBalance(@NotNull Currency currency) {
        final EconomyResponse<Double> initialResponse = setBalance(0.0d, currency);
        return new EconomyResponse<>(0.0d, initialResponse.getResult(), initialResponse.getErrorMessage());
    }

    /**
     * @author lokka30, Geolykt
     * @since v1.0.0
     * @see Account#getBalance(Currency)
     * Check if the Account can afford a withdrawal of a certain amount.
     * Specified amounts must be ABOVE zero.
     * @param amount of money proposed for withdrawal.
     * @param currency of the balance being requested.
     * @return whether the Account can afford the withdrawal.
     */
    @NotNull
    default EconomyResponse<Boolean> canAfford(double amount, @NotNull Currency currency) {
        final EconomyResponse<Double> initialResponse = getBalance(currency);
        return new EconomyResponse<>(initialResponse.getValue() >= amount, initialResponse.getResult(), initialResponse.getErrorMessage());
    }

    /**
     * @author lokka30
     * @since v1.0.0
     * Deletes the Account's data.
     * Providers should consider storing backups of deleted accounts.
     * @return whether the deletion was successful or not.
     */
    @NotNull
    EconomyResponse<Boolean> deleteAccount();

}
