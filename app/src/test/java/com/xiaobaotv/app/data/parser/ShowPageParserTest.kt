package com.xiaobaotv.app.data.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class ShowPageParserTest {

    @Test
    fun buildShowPageUrl_usesSitePaginationFormat() {
        assertEquals(
            "https://www.xiaobaotv.tv/movie/show/5.html",
            ShowPageParser.buildShowPageUrl(typeId = 5, page = 1)
        )
        assertEquals(
            "https://www.xiaobaotv.tv/movie/show/5/page/2.html",
            ShowPageParser.buildShowPageUrl(typeId = 5, page = 2)
        )
    }
}
