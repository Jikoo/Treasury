package me.lokka30.treasury.api.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SimpleFuture<T, U extends Throwable> {

    @Contract("_ -> this")
    SimpleFuture<T, U> defaultValue(@NotNull T value);

    @Contract("_ -> this")
    SimpleFuture<T, U> handleError(@NotNull Consumer<@NotNull U> errorConsumer);

    void handle(@NotNull Consumer<@NotNull T> valueConsumer);

    <V> SimpleFuture<V, U> map(@NotNull Function<@NotNull T, @NotNull V> function);

    <V> SimpleFuture<V, U> flatMap(@NotNull Function<@NotNull T, @NotNull SimpleFuture<V, U>> function);

}
