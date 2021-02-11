package com.mercadopago.android.px.internal.audio

import android.content.Context
import android.media.MediaPlayer
import com.mercadopago.android.px.R

internal object AudioPlayer {

    fun play(context: Context, sound: Sound) {
        MediaPlayer.create(context, sound.id).also {
            it.start()
        }
    }

    enum class Sound(val id: Int) {
        SUCCESS(R.raw.congrats_success),
        FAILURE(R.raw.congrats_failure)
    }
}
