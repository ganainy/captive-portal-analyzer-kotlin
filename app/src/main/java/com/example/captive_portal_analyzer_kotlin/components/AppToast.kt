package com.example.captive_portal_analyzer_kotlin.components

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.example.captive_portal_analyzer_kotlin.R
import kotlinx.coroutines.delay
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

sealed class ToastState {
    object Hidden : ToastState()
    data class Shown(
        var title: String? = null,
        val message: String,
        val style: ToastStyle = ToastStyle.SUCCESS,
        val duration: Long? = 2000L
    ) : ToastState()
}

enum class ToastStyle {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    DELETE
}


@Composable
fun AppToast(
    toastState: ToastState,
    context: Context = LocalContext.current,
    onDismissRequest: () -> Unit
) {
    LaunchedEffect(toastState) {



        if (toastState is ToastState.Shown) {

            if (toastState.title.isNullOrEmpty()) {
                toastState.title = when (toastState.style) {
                    ToastStyle.SUCCESS -> context.getString(R.string.success)
                    ToastStyle.ERROR -> context.getString(R.string.error)
                    ToastStyle.WARNING -> context.getString(R.string.warning)
                    ToastStyle.INFO -> context.getString(R.string.info)
                    ToastStyle.DELETE -> context.getString(R.string.delete)
                }
            }
            when (toastState.style) {
                ToastStyle.SUCCESS -> {
                    MotionToast.createToast(
                        context as Activity,
                        title = toastState.title,
                        message = toastState.message,
                        style = MotionToastStyle.SUCCESS,
                        position = MotionToast.GRAVITY_BOTTOM,
                        duration = toastState.duration!!,
                        font = ResourcesCompat.getFont(context, R.font.mon_regular)
                    )
                }
                ToastStyle.ERROR -> {
                    MotionToast.createToast(
                        context as Activity,
                        title = toastState.title,
                        message = toastState.message,
                        style = MotionToastStyle.ERROR,
                        position = MotionToast.GRAVITY_BOTTOM,
                        duration = toastState.duration!!,
                        font = ResourcesCompat.getFont(context, R.font.mon_regular)
                    )
                }
                ToastStyle.WARNING -> {
                    MotionToast.createToast(
                        context as Activity,
                        title = toastState.title,
                        message = toastState.message,
                        style = MotionToastStyle.WARNING,
                        position = MotionToast.GRAVITY_BOTTOM,
                        duration = toastState.duration!!,
                        font = ResourcesCompat.getFont(context, R.font.mon_regular)
                    )
                }
                ToastStyle.INFO -> {
                    MotionToast.createToast(
                        context as Activity,
                        title = toastState.title,
                        message = toastState.message,
                        style = MotionToastStyle.INFO,
                        position = MotionToast.GRAVITY_BOTTOM,
                        duration = toastState.duration!!,
                        font = ResourcesCompat.getFont(context, R.font.mon_regular)
                    )
                }
                ToastStyle.DELETE -> {
                    MotionToast.createToast(
                        context as Activity,
                        title = toastState.title,
                        message = toastState.message,
                        style = MotionToastStyle.DELETE,
                        position = MotionToast.GRAVITY_BOTTOM,
                        duration = toastState.duration!!,
                        font = ResourcesCompat.getFont(context, R.font.mon_regular)
                    )
                }
            }
            delay(toastState.duration)
            onDismissRequest()
        }
    }
}