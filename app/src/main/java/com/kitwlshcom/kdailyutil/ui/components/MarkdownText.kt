package com.kitwlshcom.kdailyutil.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AI 응답(브리핑·요약·대화)용 경량 마크다운 렌더러.
 * 외부 라이브러리 없이 자체 파싱(자립적). 지원 문법:
 *  - 제목: `# `, `## `, `### `
 *  - 굵게: `**텍스트**`, 기울임: `*텍스트*`, 인라인 코드: `` `코드` ``
 *  - 불릿: `- `, `* `, `• `
 * 표·링크·이미지 등 고급 문법은 평문으로 처리(안전한 폴백).
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.9f),
    fontSize: TextUnit = 14.sp
) {
    val lines = text.replace("\r\n", "\n").split("\n")
    Column(modifier = modifier) {
        lines.forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(6.dp))
                line.startsWith("### ") -> Text(
                    parseInline(line.removePrefix("### ")),
                    color = color, fontSize = (fontSize.value * 1.05f).sp, fontWeight = FontWeight.Bold
                )
                line.startsWith("## ") -> Text(
                    parseInline(line.removePrefix("## ")),
                    color = color, fontSize = (fontSize.value * 1.15f).sp, fontWeight = FontWeight.Bold
                )
                line.startsWith("# ") -> Text(
                    parseInline(line.removePrefix("# ")),
                    color = color, fontSize = (fontSize.value * 1.25f).sp, fontWeight = FontWeight.Bold
                )
                isBullet(line) -> Row(modifier = Modifier.fillMaxWidth()) {
                    Text("•", color = color, fontSize = fontSize, modifier = Modifier.padding(end = 6.dp))
                    Text(parseInline(stripBullet(line)), color = color, fontSize = fontSize, modifier = Modifier.weight(1f))
                }
                else -> Text(parseInline(line), color = color, fontSize = fontSize)
            }
        }
    }
}

private fun isBullet(line: String): Boolean =
    line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")

private fun stripBullet(line: String): String =
    line.removePrefix("- ").removePrefix("* ").removePrefix("• ").trimStart()

/** 인라인 문법(**굵게**, *기울임*, `코드`)을 AnnotatedString으로 변환. */
private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(text.substring(i)); i = text.length }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.White.copy(alpha = 0.08f))) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
