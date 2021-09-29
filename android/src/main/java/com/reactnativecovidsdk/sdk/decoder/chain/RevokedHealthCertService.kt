/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificates

internal class RevokedHealthCertService(private val revokedList: RevokedCertificates) {

	/**
	 * Returns true if at least one of the test, vaccination or recovery entries in the certificate is revoked.
	 *
	 * An entry is considered revoked when its "certificate identifier" (CI) matches one of the CIs in the revocation list.
	 *
	 * There are multiple formats, defined
	 * [here](https://ec.europa.eu/health/sites/default/files/ehealth/docs/vaccination-proof_interoperability-guidelines_en.pdf#page=11).
	 * For our purposes however, it suffices to consider them as opaque strings.
	 */
	fun isRevoked(dccCert: DccCert): Boolean {
		for (entry in revokedList.revokedCerts) {
			if (!dccCert.tests.isNullOrEmpty()) {
				for (test in dccCert.tests) {
					if (entry == test.certificateIdentifier) return true
				}
			}

			if (!dccCert.vaccinations.isNullOrEmpty()) {
				for (vaccination in dccCert.vaccinations) {
					if (entry == vaccination.certificateIdentifier) return true
				}
			}

			if (!dccCert.pastInfections.isNullOrEmpty()) {
				for (recovery in dccCert.pastInfections) {
					if (entry == recovery.certificateIdentifier) return true
				}
			}
		}

		return false
	}

}