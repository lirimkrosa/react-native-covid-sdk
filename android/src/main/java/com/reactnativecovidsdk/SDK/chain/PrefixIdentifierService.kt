package com.reactnativecovidsdk.SDK.chain

internal object PrefixIdentifierService {

  private const val PREFIX_FULL = "HC1:"
  private const val PREFIX_LIGHT = "LT1:"

  fun decode(input: String): String? = when {
    // Spec: https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7
    // "the base45 encoded data [...] SHALL be prefixed by the Context  Identifier string "HC1:""
    // => when the prefix is missing, the data is invalid
    input.startsWith(PREFIX_FULL) -> input.drop(PREFIX_FULL.length)
    input.startsWith(PREFIX_LIGHT) -> input.drop(PREFIX_LIGHT.length)
    else -> null
  }

}
