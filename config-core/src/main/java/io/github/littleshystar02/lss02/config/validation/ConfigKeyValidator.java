// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.validation;

import io.github.littleshystar02.lss02.config.ConfigKey;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static checks that keep a {@link ConfigKey} enum and a YAML file in sync.
 *
 * <p>Three levels of validation are offered: {@link #validate} for a live configuration file
 * (where missing keys are acceptable because defaults apply), {@link #validateTemplate} for the
 * bundled default file (which must contain every key) and {@link #validateValues} for the type and
 * content of the values themselves. All fail with a {@link ConfigValidationException} that lists
 * every problem found at once. Each check exists for enums, for arbitrary key collections (such as
 * {@link io.github.littleshystar02.lss02.config.TypedKey} holders) and for both {@link YamlFile}
 * and {@link ConfigView}. The class is stateless and cannot be instantiated.
 */
public final class ConfigKeyValidator {

    /** Utility class, not meant to be instantiated. */
    private ConfigKeyValidator() {}

    /**
     * Result of one pass over the keys and the file.
     *
     * @param duplicates    paths declared more than once
     * @param orphans       leaf paths in the file that no key describes
     * @param missing       key paths absent from the file (template mode)
     * @param invalidValues rejected values (value mode)
     */
    private record Analysis(List<String> duplicates, List<String> orphans, List<String> missing,
                            List<String> invalidValues) {

        /**
         * Tells whether any problem was found.
         *
         * @return {@code true} if at least one list is not empty
         */
        boolean hasProblems() {
            return !duplicates.isEmpty() || !orphans.isEmpty() || !missing.isEmpty() || !invalidValues.isEmpty();
        }
    }

    /**
     * Validates a configuration file against a key enum.
     *
     * <p>Fails when the enum declares the same path twice, or when the file contains a leaf path
     * that no enum constant describes. Keys that are in the enum but not in the file are allowed.
     *
     * @param file      the loaded YAML file to check
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws ConfigValidationException if duplicate or orphan paths are found
     */
    public static <E extends Enum<E> & ConfigKey> void validate(YamlFile file, Class<E> enumClass) {
        validate(ConfigView.of(file), enumClass, constants(enumClass));
    }

    /**
     * Validates a view against a key enum, see {@link #validate(YamlFile, Class)}.
     *
     * @param view      the configuration to check
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws ConfigValidationException if duplicate or orphan paths are found
     */
    public static <E extends Enum<E> & ConfigKey> void validate(ConfigView view, Class<E> enumClass) {
        validate(view, enumClass, constants(enumClass));
    }

    /**
     * Validates a view against an arbitrary collection of keys.
     *
     * @param view  the configuration to check
     * @param owner the class named in the exception message
     * @param keys  the expected keys
     * @throws ConfigValidationException if duplicate or orphan paths are found
     */
    public static void validate(ConfigView view, Class<?> owner, Collection<? extends ConfigKey> keys) {
        report(owner, analyze(view, keys, false, false));
    }

    /**
     * Validates a template (default) configuration file against a key enum.
     *
     * <p>In addition to the checks of {@link #validate}, every enum path must be present in the
     * file, so that the file shipped to users documents all available options.
     *
     * @param templateFile the template YAML file to check
     * @param enumClass    the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>          the key enum type
     * @throws ConfigValidationException if duplicate, orphan or missing paths are found
     */
    public static <E extends Enum<E> & ConfigKey> void validateTemplate(YamlFile templateFile, Class<E> enumClass) {
        validateTemplate(ConfigView.of(templateFile), enumClass, constants(enumClass));
    }

    /**
     * Validates a template view against a key enum, see {@link #validateTemplate(YamlFile, Class)}.
     *
     * @param view      the template to check
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws ConfigValidationException if duplicate, orphan or missing paths are found
     */
    public static <E extends Enum<E> & ConfigKey> void validateTemplate(ConfigView view, Class<E> enumClass) {
        validateTemplate(view, enumClass, constants(enumClass));
    }

    /**
     * Validates a template view against an arbitrary collection of keys.
     *
     * @param view  the template to check
     * @param owner the class named in the exception message
     * @param keys  the expected keys
     * @throws ConfigValidationException if duplicate, orphan or missing paths are found
     */
    public static void validateTemplate(ConfigView view, Class<?> owner, Collection<? extends ConfigKey> keys) {
        report(owner, analyze(view, keys, true, false));
    }

    /**
     * Validates the values stored in a file: each present key must hold a value its
     * {@link ConfigKey#accepts(Object)} approves (right type, within range). Paths and keys are not
     * checked, use {@link #validate} for that. Open sections are skipped.
     *
     * @param file      the loaded YAML file to check
     * @param enumClass the enum implementing {@link ConfigKey} that describes the expected keys
     * @param <E>       the key enum type
     * @throws ConfigValidationException if a value is rejected
     */
    public static <E extends Enum<E> & ConfigKey> void validateValues(YamlFile file, Class<E> enumClass) {
        validateValues(ConfigView.of(file), enumClass, constants(enumClass));
    }

    /**
     * Validates the values of a view against an arbitrary collection of keys.
     *
     * @param view  the configuration to check
     * @param owner the class named in the exception message
     * @param keys  the expected keys
     * @throws ConfigValidationException if a value is rejected
     */
    public static void validateValues(ConfigView view, Class<?> owner, Collection<? extends ConfigKey> keys) {
        report(owner, analyze(view, keys, false, true));
    }

    /**
     * Lists the constants of an enum as a collection of keys.
     *
     * @param enumClass the key enum
     * @param <E>       the key enum type
     * @return the constants in declaration order
     */
    private static <E extends Enum<E> & ConfigKey> List<E> constants(Class<E> enumClass) {
        return Arrays.asList(enumClass.getEnumConstants());
    }

    /**
     * Throws the exception for a failed analysis.
     *
     * @param owner    the class named in the message
     * @param analysis the analysis result
     * @throws ConfigValidationException if the analysis found a problem
     */
    private static void report(Class<?> owner, Analysis analysis) {
        if (analysis.hasProblems()) {
            throw new ConfigValidationException(owner, analysis.duplicates(), analysis.orphans(),
                    analysis.missing(), analysis.invalidValues());
        }
    }

    /**
     * Runs the shared single pass over the keys and the file.
     *
     * <p>Duplicates are computed except in value mode. Orphans are computed unless only values are checked.
     * Missing paths are computed when {@code template} is set, invalid values when {@code values}
     * is set.
     *
     * @param view     the configuration to inspect
     * @param keys     the expected keys
     * @param template whether every key must be present
     * @param values   whether to check values only
     * @return the findings
     */
    private static Analysis analyze(ConfigView view, Collection<? extends ConfigKey> keys, boolean template, boolean values) {
        Map<String, ConfigKey> pathToKey = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        for (ConfigKey key : keys) {
            ConfigKey previous = pathToKey.put(key.getPath(), key);
            if (previous != null) {
                duplicates.add(key.getPath());
            }
        }

        List<String> orphans = values ? List.of() : findOrphanPaths(view, pathToKey);

        List<String> missing = new ArrayList<>();
        if (template) {
            for (ConfigKey key : pathToKey.values()) {
                if (!view.contains(key.getPath())) {
                    missing.add(key.getPath());
                }
            }
        }

        List<String> invalid = new ArrayList<>();
        if (values) {
            for (ConfigKey key : pathToKey.values()) {
                if (key.isOpenSection() || !view.contains(key.getPath())) {
                    continue;
                }
                if (view.isSection(key.getPath())) {
                    Class<?> expected = key.getType();
                    if (expected != null && !Map.class.isAssignableFrom(expected)) {
                        invalid.add(key.getPath() + ": is a section, expected " + expected.getSimpleName());
                    }
                    continue;
                }
                Object value = view.getValue(key.getPath());
                if (!key.accepts(value)) {
                    invalid.add(key.getPath() + ": "
                            + (value == null ? "null" : value + " (" + value.getClass().getSimpleName() + ")")
                            + " is not valid for type "
                            + (key.getType() == null ? "any" : key.getType().getSimpleName()));
                }
            }
        }

        return new Analysis(values ? List.of() : duplicates, orphans, missing, invalid);
    }

    /**
     * Finds leaf paths in the file that no key describes.
     *
     * <p>Intermediate configuration sections are skipped, since only values can be keys. A path
     * below an {@linkplain ConfigKey#isOpenSection() open section} key is considered known.
     *
     * @param view      the configuration to scan
     * @param pathToKey the known keys by path
     * @return the orphan leaf paths, in file order
     */
    private static List<String> findOrphanPaths(ConfigView view, Map<String, ConfigKey> pathToKey) {
        List<String> openPrefixes = new ArrayList<>();
        for (ConfigKey key : pathToKey.values()) {
            if (key.isOpenSection()) {
                openPrefixes.add(key.getPath() + ".");
            }
        }

        List<String> orphanPaths = new ArrayList<>();
        for (String path : view.getAllPaths()) {
            if (view.isSection(path) || pathToKey.containsKey(path)) {
                continue;
            }
            boolean underOpenSection = false;
            for (String prefix : openPrefixes) {
                if (path.startsWith(prefix)) {
                    underOpenSection = true;
                    break;
                }
            }
            if (!underOpenSection) {
                orphanPaths.add(path);
            }
        }
        return orphanPaths;
    }

}
