/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.launcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Signals that one or more root-level artifacts failed their hash check against the signature-verified
 * root {@code MANIFEST.MF}, were re-downloaded from the upgrade server, verified, and restored in
 * {@code .artifacts/} — the Case A / Case D recovery for root-level files described in
 * {@code design/registration/registration-upgrade.md}.
 * <p>
 * This is <b>not</b> a failure: the installation is intact again. The migration is still abandoned for
 * this run because the design ends that recovery by asking the operator to restart ("Integrity restored.
 * Please exit &amp; restart the application manually."), so the next start re-evaluates from a clean,
 * verified state instead of continuing a flow whose inputs changed underneath it.
 * <p>
 * Modelled as an {@link IOException} so it travels the existing {@code stage()} signature unchanged.
 * Callers must catch it <b>before</b> their generic handler, or a successful restore is reported to the
 * operator as a migration failure.
 */
public class IntegrityRestoredException extends IOException {

    private static final long serialVersionUID = 1L;

    private final List<String> restored;

    public IntegrityRestoredException(List<String> restored) {
        super("Integrity restored from the upgrade server for: " + String.join(", ", restored));
        this.restored = Collections.unmodifiableList(new ArrayList<>(restored));
    }

    /** Names of the artifacts that were re-downloaded and verified, in check order. */
    public List<String> getRestored() {
        return restored;
    }
}
