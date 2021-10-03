package com.reactnativecovidsdk.SDK.chain


import ch.admin.bag.covidcertificate.sdk.core.data.moshi.TrimmedStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import com.facebook.react.bridge.WritableArray
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.upokecenter.cbor.CBORObject
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import com.facebook.react.bridge.WritableNativeArray
import com.google.gson.Gson





internal object CborService {

  private val keyDccCertV1 = CBORObject.FromObject(1)
  private val keyChLightCertV1 = CBORObject.FromObject(1)

  // Takes qrCodeData to directly construct a Bagdgc AND keep the field in the DCC a val
  fun decode(input: ByteArray, qrCodeData: String): String? {

    val moshi = Moshi.Builder()
      .add(Date::class.java, Rfc3339DateJsonAdapter())
      .add(String::class.java, TrimmedStringAdapter())
      .build()
    val dccCertAdapter = moshi.adapter(DccCert::class.java)
    val chLightCertAdapter = moshi.adapter(ChLightCert::class.java)

    try {
      val map = CBORObject.DecodeFromBytes(input)

      val expirationTime: Instant? = map[CwtHeaderKeys.EXPIRATION.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
      val issuedAt: Instant? = map[CwtHeaderKeys.ISSUED_AT.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
      val issuer: String? = map[CwtHeaderKeys.ISSUER.asCBOR()]?.AsString()

      val hcert = map[CwtHeaderKeys.HCERT.asCBOR()]
      val light = map[CwtHeaderKeys.LIGHT.asCBOR()]

      when {
        hcert != null -> {
          hcert[keyDccCertV1]?.let {
            val gson = Gson()
            val dccCert = dccCertAdapter.fromJson(it.ToJSONString()) ?: return null
            return gson.toJson(CertificateHolder(dccCert, qrCodeData, expirationTime, issuedAt, issuer))
          } ?: return null
        }
        light != null -> {
          light[keyChLightCertV1]?.let {
            val gson = Gson()
            val chLightCert = chLightCertAdapter.fromJson(it.ToJSONString()) ?: return null
            return gson.toJson(CertificateHolder(chLightCert, qrCodeData, expirationTime, issuedAt, issuer))
          } ?: return null
        }
        else -> return null
      }

    } catch (e: Throwable) {
      return null
    }
  }

}
