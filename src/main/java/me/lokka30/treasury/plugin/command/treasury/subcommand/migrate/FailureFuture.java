package me.lokka30.treasury.plugin.command.treasury.subcommand.migrate;

import me.lokka30.treasury.api.core.util.SimpleFuture;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

class FailureFuture<T, U extends Throwable> implements SimpleFuture<T, U> {

    private final @NotNull U failure;

    FailureFuture(@NotNull U failure) {
        this.failure = failure;
    }

    @Override
    public SimpleFuture<T, U> defaultValue(@NotNull T value) {
        // Ignore default value set.
        return this;
    }

    @Override
    public SimpleFuture<T, U> handleError(
            @NotNull Consumer<@NotNull U> errorConsumer) {
        errorConsumer.accept(failure);
        return this;
    }

    @Override
    public void handle(@NotNull Consumer<@NotNull T> valueConsumer) {
        // Ignore value handling.
    }

    @Override
    public <V> SimpleFuture<V, U> flatMap(
            @NotNull Function<@NotNull T, @NotNull SimpleFuture<V, U>> function) {
        return new FailureFuture<>(failure);
    }

    @Override
    public <V> SimpleFuture<V, U> map(@NotNull Function<@NotNull T, @NotNull V> function) {
        return new FailureFuture<>(failure);
    }

}
