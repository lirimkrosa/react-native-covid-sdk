package com.reactnativecovidsdk.SDK.chain

import com.reactnativecovidsdk.SDK.utils.Base45

internal object Base45Service {

  fun decode(input: String): ByteArray? {
    // Spec: https://ec.europa.eu/health/sites/default/files/ehealth/docs/digital-green-certificates_v1_en.pdf#page=7
    // "The Alphanumeric Mode [...] MUST be used in conjunction with Base45"
    // => data that is not compressed is invalid
    return try {
      Base45.getDecoder().decode(input)
    } catch (e: Throwable) {
      null
    }
  }

}
