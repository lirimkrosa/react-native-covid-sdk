package com.reactnativecovidsdk.SDK.chain

import COSE.MessageTag
import COSE.Sign1Message

internal object NoopVerificationCoseService {

  fun decode(input: ByteArray): ByteArray? {
    return try {
      val cose: Sign1Message = Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
      cose.GetContent()
    } catch (e: Throwable) {
      null
    }
  }

}
