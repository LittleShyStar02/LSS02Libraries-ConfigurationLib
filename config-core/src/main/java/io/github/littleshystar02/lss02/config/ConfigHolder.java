// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Publishes an immutable-by-convention snapshot of a configuration and swaps it atomically on
 * reload.
 *
 * <p>{@link AbstractConfiguration} (like {@code YamlFile}) is mutable and not thread-safe, and
 * reloading it in place can expose half-loaded content to concurrent readers. This holder avoids
 * that: {@link #reload()} builds a brand new instance through the supplied loader, validates it,
 * and only then replaces the current snapshot with a single volatile write. Readers call
 * {@link #get()} and always see a complete configuration. If loading or validation fails, the
 * previous snapshot stays in place and the exception is propagated.
 *
 * <p>Snapshots must be treated as read-only once published: do not call {@code set} or {@code save}
 * on them from other threads.
 *
 * @param <C> the configuration type
 */
public final class ConfigHolder<C extends AbstractConfiguration> {

    /** Creates and fully loads a fresh configuration instance. */
    private final Supplier<? extends C> loader;

    /** Optional check run on every new snapshot before it is published, may be {@code null}. */
    private final Consumer<? super C> validator;

    /** Current published snapshot. */
    private volatile C current;

    /**
     * Creates a holder and performs the first load.
     *
     * @param loader    creates a new instance and loads it from its file
     * @param validator checks a new snapshot before publication, or {@code null} for no check
     * @throws RuntimeException whatever the loader or validator throws on the first load
     */
    public ConfigHolder(Supplier<? extends C> loader, Consumer<? super C> validator) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.validator = validator;
        reload();
    }

    /**
     * Creates a holder without validation and performs the first load.
     *
     * @param loader creates a new instance and loads it from its file
     * @param <C>    the configuration type
     * @return the holder
     */
    public static <C extends AbstractConfiguration> ConfigHolder<C> of(Supplier<? extends C> loader) {
        return new ConfigHolder<>(loader, null);
    }

    /**
     * Returns the current snapshot.
     *
     * @return the last successfully published configuration
     */
    public C get() {
        return current;
    }

    /**
     * Loads a new snapshot, validates it and publishes it.
     *
     * <p>Concurrent reloads are serialized. On failure the previous snapshot is kept.
     *
     * @return the newly published snapshot
     * @throws RuntimeException whatever the loader or validator throws
     */
    public synchronized C reload() {
        C next = Objects.requireNonNull(loader.get(), "loader returned null");
        if (validator != null) {
            validator.accept(next);
        }
        current = next;
        return next;
    }

}
