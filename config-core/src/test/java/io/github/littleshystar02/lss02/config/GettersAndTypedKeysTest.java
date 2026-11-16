// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config;

import io.github.littleshystar02.lss02.config.validation.ConfigValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Error handling, conversions and typed keys of the key based accessors. */
class GettersAndTypedKeysTest {

    /** Keys whose defaults exercise every getter. */
    private enum Key implements ConfigKey {
        INT_AS_LONG("n.long", 7),
        INT_AS_DOUBLE("n.double", 3),
        INT_AS_FLOAT("n.float", 2),
        INT_AS_SHORT("n.short", 5),
        INT_AS_BYTE("n.byte", 4),
        INT("n.int", 9),
        BOOL("b", true),
        CHAR("c", 'z'),
        STRING("s", "text"),
        NULL_INT("null.int", null),
        NULL_BOOL("null.bool", null),
        NULL_STRING("null.string", null),
        WRONG_TYPE_STRING("wrong.string", 12),
        WRONG_TYPE_LIST("wrong.list", "not-a-list"),
        BOOLS("l.bool", List.of(true)),
        BYTES("l.byte", List.of((byte) 1)),
        CHARS("l.char", List.of('a')),
        DOUBLES("l.double", List.of(1.5)),
        FLOATS("l.float", List.of(2.5f)),
        INTS("l.int", List.of(1, 2)),
        LONGS("l.long", List.of(10L)),
        SHORTS("l.short", List.of((short) 3)),
        STRINGS("l.string", List.of("x")),
        MAPS("l.map", List.of(Map.of("k", "v"))),
        ANY_LIST("l.any", List.of("q"));

        /** YAML path. */
        private final String path;
        /** Default value. */
        private final Object defaultValue;

        /**
         * Creates a key.
         *
         * @param path         the path
         * @param defaultValue the default
         */
        Key(String path, Object defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }

        /** {@inheritDoc} */
        @Override
        public String getPath() {
            return path;
        }

        /** {@inheritDoc} */
        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }

    /** Concrete configuration used by the tests. */
    private static class Cfg extends AbstractConfiguration {
        /**
         * Creates a configuration bound to a file.
         *
         * @param file the file
         */
        Cfg(File file) {
            super(file);
        }
    }

    /**
     * Creates and loads a configuration from YAML text.
     *
     * @param dir  the temporary directory
     * @param yaml the file content
     * @return the loaded configuration
     * @throws IOException if the file cannot be written or read
     */
    private static Cfg load(Path dir, String yaml) throws IOException {
        Path file = dir.resolve("cfg.yml");
        Files.writeString(file, yaml);
        Cfg cfg = new Cfg(file.toFile());
        cfg.load();
        return cfg;
    }

    /** An Integer default is convertible by every numeric getter, no ClassCastException. */
    @Test
    void numericDefaultsAreConvertedBetweenNumericTypes(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, "");

        assertEquals(7L, cfg.getLong(Key.INT_AS_LONG));
        assertEquals(3.0, cfg.getDouble(Key.INT_AS_DOUBLE));
        assertEquals(2.0f, cfg.getFloat(Key.INT_AS_FLOAT));
        assertEquals((short) 5, cfg.getShort(Key.INT_AS_SHORT));
        assertEquals((byte) 4, cfg.getByte(Key.INT_AS_BYTE));
        assertEquals(9, cfg.getInt(Key.INT));
    }

    /** Boolean, char and string defaults are returned as they are. */
    @Test
    void scalarDefaultsAreReturned(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, "");

        assertTrue(cfg.getBoolean(Key.BOOL));
        assertEquals('z', cfg.getCharacter(Key.CHAR));
        assertEquals("text", cfg.getString(Key.STRING));
        assertNull(cfg.getString(Key.NULL_STRING));
        assertNull(cfg.get(Key.NULL_STRING));
    }

    /** A null default read through a primitive getter fails with a message naming the path. */
    @Test
    void nullDefaultOnPrimitiveGetterNamesThePath(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, "");

        IllegalStateException intFailure = assertThrows(IllegalStateException.class, () -> cfg.getInt(Key.NULL_INT));
        assertTrue(intFailure.getMessage().contains("null.int"));
        assertThrows(IllegalStateException.class, () -> cfg.getBoolean(Key.NULL_BOOL));
        assertThrows(IllegalStateException.class, () -> cfg.getLong(Key.NULL_INT));
    }

    /** A default of the wrong type fails with a ClassCastException naming the path. */
    @Test
    void wrongDefaultTypeNamesThePath(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, "");

        ClassCastException stringFailure = assertThrows(ClassCastException.class, () -> cfg.getString(Key.WRONG_TYPE_STRING));
        assertTrue(stringFailure.getMessage().contains("wrong.string"));
        assertThrows(ClassCastException.class, () -> cfg.getStringList(Key.WRONG_TYPE_LIST));
        assertThrows(ClassCastException.class, () -> cfg.getBoolean(Key.INT));
    }

    /** Every list getter returns its default when the file has no entry. */
    @Test
    void listDefaultsAreReturned(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, "");

        assertEquals(List.of(true), cfg.getBooleanList(Key.BOOLS));
        assertEquals(List.of((byte) 1), cfg.getByteList(Key.BYTES));
        assertEquals(List.of('a'), cfg.getCharacterList(Key.CHARS));
        assertEquals(List.of(1.5), cfg.getDoubleList(Key.DOUBLES));
        assertEquals(List.of(2.5f), cfg.getFloatList(Key.FLOATS));
        assertEquals(List.of(1, 2), cfg.getIntegerList(Key.INTS));
        assertEquals(List.of(10L), cfg.getLongList(Key.LONGS));
        assertEquals(List.of((short) 3), cfg.getShortList(Key.SHORTS));
        assertEquals(List.of("x"), cfg.getStringList(Key.STRINGS));
        assertEquals(List.of(Map.of("k", "v")), cfg.getMapList(Key.MAPS));
        assertEquals(List.of("q"), cfg.getList(Key.ANY_LIST));
    }

    /** Values present in the file win over the defaults for every scalar and list getter. */
    @Test
    void fileValuesWinOverDefaults(@TempDir Path dir) throws IOException {
        Cfg cfg = load(dir, """
                n:
                  long: 100
                  double: 2.5
                  float: 1.5
                  short: 8
                  byte: 9
                  int: 11
                b: false
                c: q
                s: hello
                l:
                  bool: [false]
                  byte: [5]
                  char: [x]
                  double: [0.5]
                  float: [0.25]
                  int: [3, 4]
                  long: [20]
                  short: [6]
                  string: [m, n]
                  any: [1]
                """);

        assertEquals(100L, cfg.getLong(Key.INT_AS_LONG));
        assertEquals(2.5, cfg.getDouble(Key.INT_AS_DOUBLE));
        assertEquals(1.5f, cfg.getFloat(Key.INT_AS_FLOAT));
        assertEquals((short) 8, cfg.getShort(Key.INT_AS_SHORT));
        assertEquals((byte) 9, cfg.getByte(Key.INT_AS_BYTE));
        assertEquals(11, cfg.getInt(Key.INT));
        assertFalse(cfg.getBoolean(Key.BOOL));
        assertEquals('q', cfg.getCharacter(Key.CHAR));
        assertEquals("hello", cfg.getString(Key.STRING));
        assertEquals(List.of(false), cfg.getBooleanList(Key.BOOLS));
        assertEquals(List.of((byte) 5), cfg.getByteList(Key.BYTES));
        assertEquals(List.of('x'), cfg.getCharacterList(Key.CHARS));
        assertEquals(List.of(0.5), cfg.getDoubleList(Key.DOUBLES));
        assertEquals(List.of(0.25f), cfg.getFloatList(Key.FLOATS));
        assertEquals(List.of(3, 4), cfg.getIntegerList(Key.INTS));
        assertEquals(List.of(20L), cfg.getLongList(Key.LONGS));
        assertEquals(List.of((short) 6), cfg.getShortList(Key.SHORTS));
        assertEquals(List.of("m", "n"), cfg.getStringList(Key.STRINGS));
        assertEquals(List.of(1), cfg.getList(Key.ANY_LIST));
        assertEquals(9, cfg.get(Key.INT_AS_BYTE));
    }

    /** Typed keys return defaults, stored values, and convert numbers losslessly. */
    @Test
    void typedKeyReadsDefaultStoredAndConvertedValues(@TempDir Path dir) throws IOException {
        TypedKey<Integer> port = TypedKey.of("app.port", Integer.class, 8080);
        TypedKey<Long> big = TypedKey.of("app.big", Long.class, 1L);
        TypedKey<String> name = TypedKey.of("app.name", String.class, "srv");
        TypedKey<Double> ratio = TypedKey.of("app.ratio", Double.class, 0.5);

        Cfg empty = load(dir, "");
        assertEquals(8080, empty.get(port));
        assertEquals("srv", empty.get(name));

        Cfg filled = load(dir, "app:\n  port: 9090\n  big: 5\n  name: 123\n  ratio: 2\n");
        assertEquals(9090, filled.get(port));
        assertEquals(5L, filled.get(big));
        assertEquals("123", filled.get(name));
        assertEquals(2.0, filled.get(ratio));
    }

    /** A wrongly typed or out-of-range value is reported by the typed getter, not hidden. */
    @Test
    void typedKeyRejectsInvalidValuesWithTheirPath(@TempDir Path dir) throws IOException {
        TypedKey<Integer> port = TypedKey.of("app.port", Integer.class, 8080)
                .withValidator(p -> p > 0 && p < 65536);

        Cfg text = load(dir, "app:\n  port: abc\n");
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> text.get(port));
        assertTrue(failure.getMessage().contains("app.port"));

        Cfg range = load(dir, "app:\n  port: 70000\n");
        assertThrows(IllegalStateException.class, () -> range.get(port));

        Cfg fractional = load(dir, "app:\n  port: 1.5\n");
        assertThrows(IllegalStateException.class, () -> fractional.get(port));
    }

    /** Typed key construction rejects a default that does not match its type. */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void typedKeyRejectsMismatchedDefault() {
        assertThrows(IllegalArgumentException.class, () -> TypedKey.of("p", (Class) Integer.class, "text"));
        assertThrows(NullPointerException.class, () -> TypedKey.of(null, Integer.class, 1));
        TypedKey<String> key = TypedKey.of("a.b", String.class, null);
        assertNull(key.getDefaultValue());
        assertEquals(String.class, key.getType());
        assertEquals("TypedKey[a.b, String]", key.toString());
    }

    /** Typed keys integrate with the generic value validation. */
    @Test
    void typedKeysAreCheckedByValidateValues(@TempDir Path dir) throws IOException {
        TypedKey<Integer> port = TypedKey.of("app.port", Integer.class, 8080).withValidator(p -> p > 0);
        Cfg cfg = load(dir, "app:\n  port: -4\n");

        assertThrows(ConfigValidationException.class,
                () -> cfg.validateValues(GettersAndTypedKeysTest.class, List.of(port)));
    }

    /** The legacy string based setter and addDefault still work with typed keys. */
    @Test
    void typedKeysWorkWithSetAndAddDefault(@TempDir Path dir) throws IOException {
        TypedKey<Integer> port = TypedKey.of("app.port", Integer.class, 8080);
        Cfg cfg = load(dir, "");

        cfg.addDefault(port);
        assertEquals(8080, cfg.getInt("app.port"));
        cfg.set(port, 1234);
        assertEquals(1234, cfg.get(port));
    }

    /** A default that does not fit the numeric getter without loss is reported with its path. */
    @Test
    void lossyNumericDefaultIsRejected(@TempDir Path dir) throws IOException {
        TypedKey<Long> huge = TypedKey.of("n.huge", Long.class, 5_000_000_000L);
        TypedKey<Integer> three = TypedKey.of("n.three", Integer.class, 300);
        Cfg cfg = load(dir, "");

        ClassCastException overflow = assertThrows(ClassCastException.class, () -> cfg.getInt(huge));
        assertTrue(overflow.getMessage().contains("n.huge"));
        assertThrows(ClassCastException.class, () -> cfg.getByte(three));
        assertEquals(300L, cfg.getLong(three));
    }

    /** A section where a scalar is expected is reported by value validation. */
    @Test
    void validateValuesReportsSectionWhereScalarExpected(@TempDir Path dir) throws IOException {
        TypedKey<Integer> port = TypedKey.of("app", Integer.class, 1);
        Cfg cfg = load(dir, "app:\n  port: 1\n");

        assertThrows(ConfigValidationException.class,
                () -> cfg.validateValues(GettersAndTypedKeysTest.class, List.of(port)));
    }

    /** A holder publishes a new snapshot atomically and keeps the old one when reload fails. */
    @Test
    void holderKeepsPreviousSnapshotWhenReloadFails(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("holder.yml");
        Files.writeString(file, "n:\n  int: 1\n");
        TypedKey<Integer> value = TypedKey.of("n.int", Integer.class, 0);
        AtomicInteger loads = new AtomicInteger();

        ConfigHolder<Cfg> holder = new ConfigHolder<>(() -> {
            loads.incrementAndGet();
            Cfg cfg = new Cfg(file.toFile());
            try {
                cfg.load();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return cfg;
        }, cfg -> cfg.get(value));

        Cfg first = holder.get();
        assertEquals(1, first.get(value));

        Files.writeString(file, "n:\n  int: 2\n");
        Cfg second = holder.reload();
        assertSame(second, holder.get());
        assertEquals(2, holder.get().get(value));
        assertEquals(1, first.get(value));

        Files.writeString(file, "n:\n  int: oops\n");
        assertThrows(IllegalStateException.class, holder::reload);
        assertSame(second, holder.get());
        assertEquals(3, loads.get());
    }

    /** A holder without validator works, and a null loader result is rejected. */
    @Test
    void holderFactoryAndNullLoader(@TempDir Path dir) {
        Cfg cfg = new Cfg(dir.resolve("a.yml").toFile());
        ConfigHolder<Cfg> holder = ConfigHolder.of(() -> cfg);
        assertSame(cfg, holder.get());

        assertThrows(NullPointerException.class, () -> ConfigHolder.<Cfg>of(() -> null));
        assertThrows(NullPointerException.class, () -> new ConfigHolder<Cfg>(null, null));
    }

}
