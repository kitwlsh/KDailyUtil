package com.kitwlshcom.kdailyutil.domain.util

class TextSplitter {

    /**
     * 문장 기호를 기준으로 텍스트를 분리합니다.
     * 너무 긴 문장은 추가적으로 쉼표 등을 기준으로 분절합니다.
     */
    fun splitIntoSentences(text: String): List<String> {
        // 1차 분리: 마침표, 물음표, 느낌표 기준 (뒤에 공백이 오는 경우)
        val firstSplit = text.split(Regex("(?<=[.?!])\\s+"))
        
        val result = mutableListOf<String>()
        
        firstSplit.forEach { sentence ->
            if (sentence.length > 40) {
                // 2차 분리: 문장이 너무 길면 쉼표 기준 분리 시도
                val subParts = sentence.split(Regex("(?<=[,])\\s+"))
                result.addAll(subParts)
            } else {
                result.add(sentence)
            }
        }
        
        return result.filter { it.isNotBlank() }.map { it.trim() }
    }

    /**
     * 문장 길이를 바탕으로 권장 대기 시간(밀리초)을 계산합니다.
     * 기본적으로 1글자당 약 300~500ms의 대기 시간을 부여합니다.
     */
    fun calculateWaitTime(sentence: String): Long {
        val baseTime = 1000L // 최소 대기 시간 1초
        val perCharTime = 350L // 글자당 시간
        return baseTime + (sentence.length * perCharTime)
    }
}
