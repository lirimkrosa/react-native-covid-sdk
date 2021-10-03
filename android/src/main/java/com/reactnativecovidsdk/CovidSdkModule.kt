package com.reactnativecovidsdk

import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.reactnativecovidsdk.SDK.chain.*
import com.reactnativecovidsdk.SDK.chain.Base45Service
import com.reactnativecovidsdk.SDK.chain.CborService
import com.reactnativecovidsdk.SDK.chain.DecompressionService
import com.reactnativecovidsdk.SDK.chain.NoopVerificationCoseService
import com.reactnativecovidsdk.SDK.chain.PrefixIdentifierService

class CovidSdkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "CovidSdk"
    }

    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @ReactMethod
    fun multiply(a: Int, b: Int, promise: Promise) {

      promise.resolve(a * b)

    }

    @ReactMethod
    fun decode(qrcode: String, promise: Promise){
      val encoded = PrefixIdentifierService.decode(qrcode) ?: return  promise.reject("Prefix is not HCM1")
      val compressed = Base45Service.decode(encoded) ?: return promise.reject("Base45", "Error while decoding Base45");
      val cose = DecompressionService.decode(compressed) ?: return promise.reject("DECODE_Z_LIB", "Error while decoding Z_LIB");
      val cbor = NoopVerificationCoseService.decode(cose) ?: return promise.reject("DECODE_COSE", "Error while decoding COSE");
      val certificateHolder = CborService.decode(cbor, qrcode) ?: return promise.reject("DECODE_CBOR", "Error while decoding CBOR");
      promise.resolve(certificateHolder)

    }


}
