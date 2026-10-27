package com.kitwlshcom.kdailyutil

import com.kitwlshcom.kdailyutil.data.repository.StockRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 로그에 DART 키가 남지 않게 가리기 (2026-10-27).
 *
 * 🔴 Jsoup의 HTTP 오류 문구는 요청 URL을 통째로 담는다 — 가리지 않으면 실패할 때마다
 * 공용 키가 logcat에 찍힌다. 가리는 것이 «키만»이어야 나머지 단서(회사·연도)가 로그에 남는다.
 */
class DartKeyRedactTest {

    private val key = "0123456789abcdef0123456789abcdef01234567"

    @Test
    fun `Jsoup 오류 문구의 키를 가리고 나머지 매개변수는 남긴다`() {
        val msg = "HTTP error fetching URL. Status=500, URL=[https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json" +
            "?crtfc_key=$key&corp_code=00126380&bsns_year=2026]"

        val out = StockRepository.redactDartKey(msg)!!

        assertFalse(out.contains(key))
        assertEquals(
            "HTTP error fetching URL. Status=500, URL=[https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json" +
                "?crtfc_key=***&corp_code=00126380&bsns_year=2026]",
            out
        )
    }

    @Test
    fun `키가 URL 끝에 있어도 닫는 괄호는 남긴다`() {
        val msg = "URL=[https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=$key]"

        assertEquals("URL=[https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=***]", StockRepository.redactDartKey(msg))
    }

    @Test
    fun `키가 없는 문구와 null은 그대로다`() {
        assertEquals("connect timed out", StockRepository.redactDartKey("connect timed out"))
        assertNull(StockRepository.redactDartKey(null))
    }
}
