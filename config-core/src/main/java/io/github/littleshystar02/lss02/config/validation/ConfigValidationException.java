package io.github.littleshystar02.lss02.config.validation;

import java.util.List;

/**
 * Thrown by {@link ConfigKeyValidator} when a {@link io.github.littleshystar02.lss02.config.ConfigKey}
 * enum and a YAML file are not consistent with each other.
 *
 * <p>The exception carries the offending paths in three categories so that callers can report or
 * handle them individually; the message lists all non empty categories in a human readable form.
 */
public class ConfigValidationException extends RuntimeException {

    /** Paths declared more than once in the key enum. */
    private final List<String> duplicatePaths;

    /** Paths present in the file that have no matching enum constant. */
    private final List<String> orphanPaths;

    /** Paths declared by the enum that are absent from the file (template validation only). */
    private final List<String> missingPaths;

    /**
     * Creates an exception for a validation that does not check for missing paths.
     *
     * <p>The list of missing paths is empty.
     *
     * @param enumClass      the key enum that was validated
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     */
    public ConfigValidationException(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths) {
        this(enumClass, duplicatePaths, orphanPaths, List.of());
    }

    /**
     * Creates an exception describing every kind of inconsistency.
     *
     * @param enumClass      the key enum that was validated
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     * @param missingPaths   enum paths that are absent from the file
     */
    public ConfigValidationException(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths, List<String> missingPaths) {
        super(buildMessage(enumClass, duplicatePaths, orphanPaths, missingPaths));
        this.duplicatePaths = duplicatePaths;
        this.orphanPaths = orphanPaths;
        this.missingPaths = missingPaths;
    }

    /**
     * Builds the exception message, mentioning only the categories that are not empty.
     *
     * @param enumClass      the key enum that was validated, named in the message header
     * @param duplicatePaths paths declared more than once in the enum
     * @param orphanPaths    paths in the file with no matching enum constant
     * @param missingPaths   enum paths that are absent from the file
     * @return the multi-line message
     */
    private static String buildMessage(Class<?> enumClass, List<String> duplicatePaths, List<String> orphanPaths, List<String> missingPaths) {
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

}
