package com.zendesk.belvedere;

/**
 * Enum containing a list of sources where
 * Belvedere is able to acquire media files.
 * <br>
 * #enumsmatter
 */
public enum BelvedereSource {

    /**
     * Media should be requested from an installed
     * camera app.
     * <br>
     * If the
     */
    Camera,

    /**
     * As a parameter Tells Belvedere to request media files from
     * an installed gallery app or the android document
     * picker.
     *
     */
    Gallery
}