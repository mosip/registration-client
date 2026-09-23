/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.registration.update;

/**
 * Result of an in-app upgrade attempt.
 * <p>
 * {@link #ALREADY_IN_PROGRESS} is deliberately separate from {@link #FAILED}: a rejected duplicate
 * request must not be reported to the operator as an error, because doing so tears down the display
 * of the upgrade that is still running.
 */
public enum UpgradeOutcome {

	/** Every artifact downloaded and the new manifest was adopted; a restart will apply it. */
	COMPLETED,

	/** The attempt failed and was rolled back. Nothing was applied. */
	FAILED,

	/** Another upgrade is already running; this request was ignored and nothing changed. */
	ALREADY_IN_PROGRESS
}
