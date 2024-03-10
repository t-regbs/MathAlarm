package com.timilehinaregbesola.mathalarm.utils

import android.app.AlertDialog
import android.content.Context
import com.timilehinaregbesola.mathalarm.R

fun confirmationDialog(
    context: Context,
    message: String,
    positive: String,
    negative: String? = null,
    callback: () -> Unit
) {
    AlertDialog.Builder(context).setTitle(context.getString(R.string.alert)).apply {
        setMessage(message)
        setPositiveButton(positive) { _, _ ->
            callback()
        }
        negative?.let {
            setNegativeButton(
                negative
            ) { _, _ -> callback() }
        }
    }.show()
}

fun permissionRequiredDialog(
    context: Context,
    message: String,
    onPositive: () -> Unit,
    onNegative: (() -> Unit)? = null
) {
    AlertDialog.Builder(context).setTitle(context.getString(R.string.alert)).apply {
        setMessage(message)
        setPositiveButton("Grant Permission") { _, _ ->
            onPositive()
        }
        setNegativeButton(
            "Cancel"
        ) { _, _ -> onNegative?.invoke() }
    }.show()
}