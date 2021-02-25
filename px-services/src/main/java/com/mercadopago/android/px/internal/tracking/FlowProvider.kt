package com.mercadopago.android.px.internal.tracking

import android.content.SharedPreferences
import com.mercadopago.android.px.internal.util.JsonUtil

open class FlowProvider(private val sharedPreferences: SharedPreferences) {

    var flowId: String? = null
        get() {
            if (field == null) {
                field = sharedPreferences.getString(PREF_FLOW_ID, null)
            }
            return field
        }

    var flowDetail: Map<String, Any>? = null
        get() {
            if (field == null) {
                field = JsonUtil.getStringMapFromJson(sharedPreferences.getString(PREF_FLOW_DETAIL, null))
            }
            return field
        }

    fun configure(flowId: String? , flowDetail: Map<String, Any>?) {
        this.flowId = flowId
        this.flowDetail = flowDetail
        with(sharedPreferences.edit()) {
            putString(PREF_FLOW_ID, flowId)
            putString(PREF_FLOW_DETAIL, JsonUtil.toJson(flowDetail))
            apply()
        }
    }

    open fun reset() {
        flowId = null
        flowDetail = null
        with(sharedPreferences.edit()) {
            remove(PREF_FLOW_ID)
            remove(PREF_FLOW_DETAIL)
            apply()
        }
    }

    companion object {
        private const val PREF_FLOW_ID = "PREF_FLOW_ID"
        private const val PREF_FLOW_DETAIL = "PREF_FLOW_DETAIL"
    }
}