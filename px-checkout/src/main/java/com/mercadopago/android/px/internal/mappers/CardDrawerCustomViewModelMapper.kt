package com.mercadopago.android.px.internal.mappers

import com.meli.android.carddrawer.model.State
import com.meli.android.carddrawer.model.SwitchModel
import com.meli.android.carddrawer.model.SwitchOption
import com.meli.android.carddrawer.model.SwitchStates
import com.mercadopago.android.px.model.internal.CardDrawerSwitch

object CardDrawerCustomViewModelMapper {

    fun mapToSwitchModel(cardDrawerSwitch: CardDrawerSwitch?) = cardDrawerSwitch?.run {
        val states = SwitchStates(
            getStateForCardDrawer(states.checked),
            getStateForCardDrawer(states.unchecked),
            getStateForCardDrawer(states.disabled)
        )
        SwitchModel(states, getOptionsForCardDrawer(options), backgroundColor, default)
    }

    private fun getStateForCardDrawer(state: CardDrawerSwitch.SwitchStates.State) = state.run { State(textColor, backgroundColor, weight) }
    private fun getOptionsForCardDrawer(options: List<CardDrawerSwitch.Option>) = options.map { SwitchOption(it.id, it.name) }
}