package com.mercadopago.android.px.model

import android.os.Parcel
import com.mercadopago.android.px.internal.core.extensions.readDate
import com.mercadopago.android.px.internal.core.extensions.readNullableInt
import com.mercadopago.android.px.internal.core.extensions.writeDate
import com.mercadopago.android.px.internal.core.extensions.writeNullableInt
import com.mercadopago.android.px.internal.util.KParcelable
import com.mercadopago.android.px.internal.helper.SecurityCodeHelper
import com.mercadopago.android.px.internal.util.parcelableCreator
import java.util.*

class Card @Deprecated("") constructor() : CardInformation, KParcelable {

    constructor(parcel: Parcel) : this() {
        cardHolder = parcel.readParcelable(Cardholder::class.java.classLoader)
        customerId = parcel.readString()
        dateCreated = parcel.readDate()
        dateLastUpdated = parcel.readDate()
        expirationMonth = parcel.readNullableInt()
        expirationYear = parcel.readNullableInt()
        firstSixDigits = parcel.readString()
        id = parcel.readString()
        issuer = parcel.readParcelable(Issuer::class.java.classLoader)
        lastFourDigits = parcel.readString()
        paymentMethod = parcel.readParcelable(PaymentMethod::class.java.classLoader)
        securityCode = parcel.readParcelable(SecurityCode::class.java.classLoader)
        escStatus = parcel.readString()
    }

    @set:Deprecated("")
    override var cardHolder: Cardholder? = null

    @get:Deprecated("")
    @set:Deprecated("")
    var customerId: String? = null

    @get:Deprecated("")
    @set:Deprecated("")
    var dateCreated: Date? = null

    @get:Deprecated("")
    @set:Deprecated("")
    var dateLastUpdated: Date? = null

    @set:Deprecated("")
    override var expirationMonth: Int? = null

    @set:Deprecated("")
    override var expirationYear: Int? = null

    @set:Deprecated("")
    override var firstSixDigits: String? = null

    var id: String? = null
    var issuer: Issuer? = null
    override var lastFourDigits: String? = null
    var paymentMethod: PaymentMethod? = null
    var securityCode: SecurityCode? = null
    var escStatus: String? = null

    fun isSecurityCodeRequired() = SecurityCodeHelper.isRequired(securityCode)
    fun getSecurityCodeLocation() = securityCode?.cardLocation ?: CARD_DEFAULT_SECURITY_CODE_LOCATION
    override fun getSecurityCodeLength() = securityCode?.length

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(cardHolder, flags)
        parcel.writeString(customerId)
        parcel.writeDate(dateCreated)
        parcel.writeDate(dateLastUpdated)
        parcel.writeNullableInt(expirationMonth)
        parcel.writeNullableInt(expirationYear)
        parcel.writeString(firstSixDigits)
        parcel.writeString(id)
        parcel.writeParcelable(issuer, flags)
        parcel.writeString(lastFourDigits)
        parcel.writeParcelable(paymentMethod, flags)
        parcel.writeParcelable(securityCode, flags)
        parcel.writeString(escStatus)
    }

    override fun toString() = """Card{
        cardHolder=$cardHolder,
        customerId=$customerId,
        dateCreated=$dateCreated,
        dateLastUpdated=$dateLastUpdated,
        expirationMonth=$expirationMonth,
        expirationYear=$expirationYear,
        firstSixDigits=$firstSixDigits,
        id=$id,
        issuer=$issuer,
        lastFourDigits=$lastFourDigits,
        paymentMethod=$paymentMethod,
        securityCode=$securityCode,
        escStatus=$escStatus}""".trimIndent()

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Card)
        const val CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH = 12
        const val CARD_DEFAULT_SECURITY_CODE_LENGTH = 4
        const val CARD_NUMBER_MAX_LENGTH = 16
        private const val CARD_DEFAULT_SECURITY_CODE_LOCATION = "back"
    }
}