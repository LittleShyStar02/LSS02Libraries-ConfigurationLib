// SPDX-License-Identifier: GPL-3.0-only
package io.github.littleshystar02.lss02.config.validation;

import java.util.List;

/**
 * Thrown by {@link ConfigKeyValidator} when a {@link io.github.littleshystar02.lss02.config.ConfigKey}
 * enum and a YAML file are not consistent with each other.
 *
 * <p>The exception carries the offending paths in four categories so that callers can report or
 * handle them individually; the message lists all non empty categories in a human readable form.
 * The lists are immutable copies taken at construction time.
 */
public class ConfigValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Paths declared more than once in the key enum. */
    private final List<String> duplicatePaths;

    /** Paths present in the file that have no matching enum constant. */
    private final List<String> orphanPaths;

    /** Paths declared by the enum that are absent from the file (template validation only). */
    private final List<String> missingPaths;

    /** Descriptions of values whose type or content is not acceptable for their key. */
    private final List<String> invalidValues;

    /**
     * Creates an exception for a validation that does not check for missing paths or values.
     *
     * @param enumClass      the key enum (or owner class) that was validated
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     */
    public ConfigValidationException(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths) {
        this(enumClass, duplicatePaths, orphanPaths, List.of(), List.of());
    }

    /**
     * Creates an exception for a template validation.
     *
     * @param enumClass      the key enum (or owner class) that was validated
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     * @param missingPaths   enum paths that are absent from the file
     */
    public ConfigValidationException(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths, List<String> missingPaths) {
        this(enumClass, duplicatePaths, orphanPaths, missingPaths, List.of());
    }

    /**
     * Creates an exception describing every kind of inconsistency.
     *
     * @param enumClass      the key enum (or owner class) that was validated
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     * @param missingPaths   enum paths that are absent from the file
     * @param invalidValues  descriptions of values rejected by their key, as {@code path: reason}
     */
    public ConfigValidationException(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths,
                                     List<String> missingPaths, List<String> invalidValues) {
        super(buildMessage(enumClass, duplicatePaths, orphanPaths, missingPaths, invalidValues));
        this.duplicatePaths = List.copyOf(duplicatePaths);
        this.orphanPaths = List.copyOf(orphanPaths);
        this.missingPaths = List.copyOf(missingPaths);
        this.invalidValues = List.copyOf(invalidValues);
    }

    /**
     * Builds the exception message, mentioning only the categories that are not empty.
     *
     * @param enumClass      the key enum that was validated, named in the message header
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     * @param missingPaths   enum paths that are absent from the file
     * @param invalidValues  rejected values
     * @return the multi-line message
     */
    private static String buildMessage(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths,
                                       List<String> missingPaths, List<String> invalidValues) {
        StringBuilder message = new StringBuilder("Config validation failed for ").append(enumClass.getName()).append(':');
        if (!duplicatePaths.isEmpty()) {
            message.append("\n  duplicate paths in enum: ").append(duplicatePaths);
        }
        if (!orphanPaths.isEmpty()) {
            message.append("\n  paths in file with no matching enum constant: ").append(orphanPaths);
        }
        if (!missingPaths.isEmpty()) {
            message.append("\n  enum constants with no matching path in file: ").append(missingPaths);
        }
        if (!invalidValues.isEmpty()) {
            message.append("\n  invalid values: ").append(invalidValues);
        }
        return message.toString();
    }

    /**
     * Returns the paths that the enum declares more than once.
     *
     * @return the duplicate paths, empty if there are none
     */
    public List<String> getDuplicatePaths() {
        return duplicatePaths;
    }

    /**
     * Returns the paths found in the file that no enum constant describes.
     *
     * @return the orphan paths, empty if there are none
     */
    public List<String> getOrphanPaths() {
        return orphanPaths;
    }

    /**
     * Returns the enum paths that the file does not contain.
     *
     * @return the missing paths, empty if there are none or the check was not performed
     */
    public List<String> getMissingPaths() {
        return missingPaths;
    }

    /**
     * Returns the values rejected by their key, each as {@code path: reason}.
     *
     * @return the invalid value descriptions, empty if there are none or the check was not performed
     */
    public List<String> getInvalidValues() {
        return invalidValues;
    }

}
