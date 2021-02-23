package com.mercadopago.android.px.internal.features.payment_result.remedies.view

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mercadopago.android.px.R
import com.mercadopago.android.px.internal.di.MapperProvider
import com.mercadopago.android.px.internal.experiments.BadgeVariant
import com.mercadopago.android.px.internal.extensions.gone
import com.mercadopago.android.px.internal.extensions.visible
import com.mercadopago.android.px.internal.features.express.slider.PaymentMethodFragment
import com.mercadopago.android.px.internal.features.express.slider.PaymentMethodLowResDrawer
import com.mercadopago.android.px.internal.features.payment_result.remedies.RemediesPayerCost
import com.mercadopago.android.px.internal.view.PaymentMethodDescriptorView
import com.mercadopago.android.px.model.internal.OneTapItem

internal class RetryPaymentFragment : Fragment(), PaymentMethodFragment.DisabledDetailDialogLauncher {

    private lateinit var message: TextView
    private lateinit var cvvRemedy: CvvRemedy
    private lateinit var paymentMethodDescriptor: PaymentMethodDescriptorView
    private lateinit var paymentMethodTitle: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.px_remedies_retry_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        message = view.findViewById(R.id.message)
        cvvRemedy = view.findViewById(R.id.cvv_remedy)
        paymentMethodDescriptor = view.findViewById(R.id.payment_method_descriptor)
        paymentMethodTitle = view.findViewById(R.id.payment_method_title)
    }

    fun init(model: Model, methodData: OneTapItem?) {
        message.text = model.message
        methodData?.let {
            addCard(it)
            if (model.isAnotherMethod) {
                showPaymentMethodDescriptor(it, model.payerCost)
            }
        }
        model.cvvModel?.let { cvvRemedy.init(it) } ?: cvvRemedy.gone()
    }

    fun setListener(listener: CvvRemedy.Listener) {
        cvvRemedy.listener = listener
    }

    private fun addCard(methodData: OneTapItem) {
        childFragmentManager.beginTransaction().apply {
            val drawableFragmentItem = MapperProvider.getPaymentMethodDrawableItemMapper().map(methodData)!!
            val paymentMethodFragment = drawableFragmentItem.draw(PaymentMethodLowResDrawer()) as PaymentMethodFragment<*>
            paymentMethodFragment.onFocusIn()
            replace(R.id.card_container, paymentMethodFragment)
            commitAllowingStateLoss()
        }
    }

    private fun showPaymentMethodDescriptor(methodData: OneTapItem, payerCost: RemediesPayerCost?) {
        paymentMethodDescriptor.visible()
        paymentMethodTitle.visible()
        if (!paymentMethodTitle.text.contains(":")) paymentMethodTitle.append(":") // FIXME
        val model = MapperProvider.getPaymentMethodDescriptorMapper().map(methodData)
        model.formatForRemedy()
        payerCost?.let { model.setCurrentPayerCost(it.payerCostIndex) }
        paymentMethodDescriptor.configureExperiment(BadgeVariant().default)
        paymentMethodDescriptor.update(model)
    }

    internal data class Model(val message: String, val isAnotherMethod: Boolean,
        val cvvModel: CvvRemedy.Model?) : Parcelable {

        var payerCost: RemediesPayerCost? = null

        constructor(parcel: Parcel) : this(parcel.readString()!!,
            parcel.readInt() != 0,
            parcel.readParcelable(CvvRemedy.Model::class.java.classLoader)) {
            payerCost = parcel.readParcelable(RemediesPayerCost::class.java.classLoader)
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(message)
            parcel.writeInt(if (isAnotherMethod) 1 else 0)
            parcel.writeParcelable(cvvModel, flags)
            parcel.writeParcelable(payerCost, flags)
        }

        override fun describeContents() = 0

        companion object CREATOR : Parcelable.Creator<Model> {
            override fun createFromParcel(parcel: Parcel) = Model(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Model>(size)
        }
    }
}