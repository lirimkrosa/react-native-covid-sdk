/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState.*
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.*
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesError.*
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.isTruthy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class NationalRulesVerifier {

	private val validityRangeCalculator = DisplayValidityRangeCalculator()

	fun verify(dccCert: DccCert, ruleSet: RuleSet, certType: CertType, clock: Clock = Clock.systemUTC()): CheckNationalRulesState {
		val ruleSetData = getCertlogicData(dccCert, ruleSet.valueSets, clock)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		for (rule in ruleSet.rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, dccCert, ruleSet.displayRules, ruleSetData.external.valueSets, certType)
			}
		}

		val validityRange =
			getValidityRange(ruleSet.displayRules, data, certType)
		return if (validityRange != null) {
			SUCCESS(validityRange)
		} else {
			INVALID(VALIDITY_RANGE_NOT_FOUND)
		}
	}

	fun getCertlogicData(dccCert: DccCert, valueSets: Map<String, Array<String>>, clock: Clock = Clock.systemUTC()): CertLogicData {
		val payload = CertLogicPayload(dccCert.pastInfections, dccCert.tests, dccCert.vaccinations)
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(valueSets, validationClock, validationClockAtStartOfDay)
		return CertLogicData(payload, externalInfo)
	}

	private fun getErrorStateForRule(
		rule: Rule,
		dccCert: DccCert,
		displayRules: List<DisplayRule>,
		valueSets: Map<String, Array<String>>,
		certType: CertType,
		clock: Clock = Clock.systemUTC()
	): CheckNationalRulesState {
		val ruleSetData = getCertlogicData(dccCert, valueSets, clock)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		return when (rule.identifier) {
			"GR-CH-0001" -> INVALID(WRONG_DISEASE_TARGET, rule.identifier)
			"VR-CH-0000" -> INVALID(TOO_MANY_VACCINE_ENTRIES, rule.identifier)
			"VR-CH-0001" -> INVALID(NOT_FULLY_PROTECTED, rule.identifier)
			"VR-CH-0002" -> INVALID(NO_VALID_PRODUCT, rule.identifier)
			"VR-CH-0003" -> INVALID(NO_VALID_DATE, rule.identifier)
			"VR-CH-0004" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0005" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0006" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0000" -> INVALID(TOO_MANY_TEST_ENTRIES, rule.identifier)
			"TR-CH-0001" -> INVALID(POSITIVE_RESULT, rule.identifier)
			"TR-CH-0002" -> INVALID(WRONG_TEST_TYPE, rule.identifier)
			"TR-CH-0003" -> INVALID(NO_VALID_PRODUCT, rule.identifier)
			"TR-CH-0004" -> INVALID(NO_VALID_DATE, rule.identifier)
			"TR-CH-0005" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0006" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0007" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"RR-CH-0000" -> INVALID(TOO_MANY_RECOVERY_ENTRIES, rule.identifier)
			"RR-CH-0001" -> INVALID(NO_VALID_DATE, rule.identifier)
			"RR-CH-0002" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"RR-CH-0003" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			else -> INVALID(UNKNOWN_RULE_FAILED, rule.identifier)
		}
	}

	private fun getValidityRange(
		displayRules: List<DisplayRule>,
		data: JsonNode,
		certType: CertType
	): ValidityRange? {
		return validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(displayRules, data, certType)
	}


}
