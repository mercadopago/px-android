package com.mercadopago.android.px.core.internal

import com.mercadopago.android.px.core.CheckoutLazyInit
import com.mercadopago.android.px.core.MercadoPagoCheckout
import com.mercadopago.android.px.internal.di.Session
import com.mercadopago.android.px.internal.util.ApiUtil
import com.mercadopago.android.px.model.exceptions.ApiException
import com.mercadopago.android.px.model.exceptions.MercadoPagoError
import com.mercadopago.android.px.model.internal.CheckoutResponse
import com.mercadopago.android.px.tracking.internal.events.FrictionEventTracker
import com.mercadopago.android.px.tracking.internal.events.FrictionEventTracker.Id
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

internal class PrefetchService(private val checkout: MercadoPagoCheckout, private val session: Session,
    private var internalCallback: CheckoutLazyInit) {

    var checkoutResponse: CheckoutResponse? = null
        private set

    fun prefetch() {
        QUEUE.add(this)
        triggerQueue()
    }

    fun cancel() {
        triggerCancel()
    }

    private fun doFetch() {
        initCall()
    }

    private fun doCancel() {
        internalCallback = object : CheckoutLazyInit(MercadoPagoCheckout.Builder("", "")) {
            override fun fail(mercadoPagoCheckout: MercadoPagoCheckout) {
                // do nothing
            }

            override fun success(mercadoPagoCheckout: MercadoPagoCheckout) {
                // do nothing
            }
        }
    }

    private fun initCall() {
        CoroutineScope(Dispatchers.IO).launch {
            val response = session.getPrefetchInitService(checkout).get()
            withContext(Dispatchers.Main) { response.resolve(success = {
                checkoutResponse = it
                success()
            }, error = { error(it) }) }
        }
    }

    private fun success() {
        checkout.prefetch = this
        internalCallback.success(checkout)
    }

    private fun error(apiException: ApiException?) {
        FrictionEventTracker.with("/px_checkout/lazy_init", Id.GENERIC, FrictionEventTracker.Style.NON_SCREEN,
            MercadoPagoError(apiException!!, ApiUtil.RequestOrigin.POST_INIT))
        internalCallback.failure()
    }

    companion object {
        private val QUEUE: Queue<PrefetchService> = LinkedList()
        private var PROCESSING = false

        private fun triggerQueue() {
            if (!PROCESSING && QUEUE.isNotEmpty()) {
                PROCESSING = true
                QUEUE.poll()!!.doFetch()
            }
        }

        private fun triggerCancel() {
            for(prefetch in QUEUE) {
                prefetch.doCancel()
            }
            QUEUE.clear()
            PROCESSING = false
        }

        @JvmStatic fun onCheckoutStarted() {
            PROCESSING = false
            triggerQueue()
        }
    }
}
