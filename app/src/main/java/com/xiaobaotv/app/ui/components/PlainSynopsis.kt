package com.xiaobaotv.app.ui.components

import androidx.core.text.HtmlCompat

/** Display scraped descriptions as text, including HTML entities and paragraph breaks. */
fun plainSynopsis(html: String?): String =
    html?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim() }
        ?.takeIf { it.isNotBlank() } ?: "暂无简介"
