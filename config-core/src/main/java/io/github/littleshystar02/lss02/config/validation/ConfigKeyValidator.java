package io.github.littleshystar02.lss02.config.validation;

import io.github.littleshystar02.lss02.config.ConfigKey;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static checks that keep a {@link ConfigKey} enum and a YAML file in sync.
 *
 * <p>Two levels of validation are offered: {@link #validate} for a live configuration file (where
 * missing keys are acceptable because defaults apply) and {@link #validateTemplate} for the
 * bundled default file (which must contain every key). Both fail with a
 * {@link ConfigValidationException} that lists every problem found at once. The class is stateless
 * and cannot be instantiated.
 */
public final class ConfigKeyValidator {

    /** Utility class, not meant to be instantiated. */
    private ConfigKeyValidator() {}

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
        Map<String, E> pathToKey = collectPathToKey(enumClass);
        List<String> duplicatePaths = findDuplicatePaths(enumClass);
        List<String> orphanPaths = findOrphanPaths(file, pathToKey);

        if (!duplicatePaths.isEmpty() || !orphanPaths.isEmpty()) {
            throw new ConfigValidationException(enumClass, duplicatePaths, orphanPaths);
        }
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
        Map<String, E> pathToKey = collectPathToKey(enumClass);
        List<String> duplicatePaths = findDuplicatePaths(enumClass);
        List<String> orphanPaths = findOrphanPaths(templateFile, pathToKey);

        List<String> missingPaths = new ArrayList<>();
        for (String path : pathToKey.keySet()) {
            if (!templateFile.contains(path)) {
                missingPaths.add(path);
            }
        }

        if (!duplicatePaths.isEmpty() || !orphanPaths.isEmpty() || !missingPaths.isEmpty()) {
            throw new ConfigValidationException(enumClass, duplicatePaths, orphanPaths, missingPaths);
        }
    }

    /**
     * Indexes the enum constants by path, preserving declaration order.
     *
     * <p>If two constants share a path the later one wins; duplicates are reported separately by
     * {@link #findDuplicatePaths}.
     *
     * @param enumClass the key enum
     * @param <E>       the key enum type
     * @return a map from path to the constant that declares it
     */
    private static <E extends Enum<E> & ConfigKey> Map<String, E> collectPathToKey(Class<E> enumClass) {
        Map<String, E> pathToKey = new LinkedHashMap<>();
        for (E key : enumClass.getEnumConstants()) {
            pathToKey.put(key.getPath(), key);
        }
        return pathToKey;
    }

    /**
     * Finds paths declared by more than one enum constant.
     *
     * @param enumClass the key enum
     * @param <E>       the key enum type
     * @return the paths that appear again after their first declaration, in declaration order
     */
    private static <E extends Enum<E> & ConfigKey> List<String> findDuplicatePaths(Class<E> enumClass) {
        Map<String, E> seen = new LinkedHashMap<>();
        List<String> duplicatePaths = new ArrayList<>();
        for (E key : enumClass.getEnumConstants()) {
            E previous = seen.put(key.getPath(), key);
            if (previous != null) {
                duplicatePaths.add(key.getPath());
            }
        }
        return duplicatePaths;
    }

    /**
     * Finds leaf paths in the file that no enum constant describes.
     *
     * <p>Intermediate configuration sections are skipped, since only values can be keys.
     *
     * @param file      the YAML file to scan
     * @param pathToKey the known paths, as produced by {@link #collectPathToKey}
     * @return the orphan leaf paths, in file order
     */
    private static List<String> findOrphanPaths(YamlFile file, Map<String, ?> pathToKey) {
        List<String> orphanPaths = new ArrayList<>();
        for (String path : file.getKeys(true)) {
            if (file.isConfigurationSection(path)) {
                continue;
            }
            if (!pathToKey.containsKey(path)) {
                orphanPaths.add(path);
            }
        }
        return orphanPaths;
    }

}
