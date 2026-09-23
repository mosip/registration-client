/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher.common;

/**
 * Receives byte-level progress for a download so a caller (e.g. the launcher's progress dialog) can
 * render a percentage. Kept in {@code launcher.common} — with no Swing/UI dependency — so
 * {@link ResumableDownloader} can report progress without knowing who consumes it.
 * <p>
 * {@code total} is {@code -1} when the size is unknown (chunked transfer / no {@code Content-Length}),
 * in which case the consumer should stay indeterminate. The listener is invoked on the downloading
 * thread as bytes arrive, so an implementation that touches a UI toolkit must marshal to its own thread.
 */
@FunctionalInterface
public interface DownloadProgressListener {

    /**
     * @param bytesDone bytes written so far (includes any resumed offset)
     * @param total     the total expected size in bytes, or {@code -1} if unknown
     */
    void onProgress(long bytesDone, long total);
}
