package com.kitwlshcom.kdailyutil.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * AI(ChatGPT·Gemini·Claude 등)에게 "이 형식대로 퀴즈를 만들어줘"라고 요청해
 * 앱에 바로 가져올 수 있는 .kquiz 파일을 만들도록 돕는 가이드.
 * - PROMPT_TEMPLATE: AI에 그대로 붙여넣는 프롬프트
 * - GUIDE_MARKDOWN: 내려받기(공유)용 전체 가이드 문서
 * - exportGuide(): 가이드를 파일로 저장해 공유 URI 반환
 */
object QuizAiGuide {

    private const val TAG = "QuizAiGuide"
    private const val SHARED_DIR_NAME = "shared_quizzes"
    private const val GUIDE_FILE_NAME = "KDailyUtil_퀴즈만들기_가이드.md"

    /** AI에 그대로 복사해 넣는 프롬프트 (사용자가 [주제] 등만 바꿔 사용) */
    val PROMPT_TEMPLATE: String = """
너는 한국어 퀴즈 출제 전문가야. 아래 JSON 형식에 '정확히' 맞춰 퀴즈를 만들어줘.
설명이나 마크다운 코드블록(```) 없이 순수 JSON 하나만 출력해.

[주제] 여기에 원하는 주제를 적으세요 (예: 한국사 조선시대 / 기초 영어 단어 / 컴퓨터 상식)
[문항 수] 10개
[난이도] 중
[유형] 객관식 위주 (필요하면 주관식 섞어도 됨)

## 출력 JSON 형식
{
  "category": "여기에 카테고리 이름 (예: 조선시대 한국사)",
  "creator": "내 이름 또는 별명",
  "creatorId": "personal",
  "version": 1,
  "questions": [
    {
      "type": "MULTIPLE_CHOICE",
      "subCategory": "세부 분류 (예: 인물)",
      "question": "질문 내용",
      "options": ["정답", "오답1", "오답2", "오답3"],
      "answer": "정답",
      "explanation": "왜 정답인지 친절한 해설",
      "semanticHint": "정답을 유추할 힌트"
    },
    {
      "type": "SUBJECTIVE",
      "subCategory": "세부 분류",
      "question": "주관식 질문",
      "answer": "정답(1~3단어 이내의 명사/단어/수치)",
      "explanation": "해설",
      "semanticHint": "힌트"
    }
  ]
}

## 반드시 지킬 규칙
1. 객관식(MULTIPLE_CHOICE)은 options 보기를 정확히 4개 만들고, 그 안에 정답(answer)과 '완전히 똑같은' 문자열을 반드시 포함해.
2. 주관식(SUBJECTIVE)은 options 항목을 아예 넣지 마. 정답은 1~3단어 이내의 짧은 단어/명사/수치로.
3. 같은 정답(answer)이 두 번 이상 나오지 않게 해. 질문만 살짝 바꾼 같은 답 반복은 금지.
4. 정답은 명확하고 논란이 없어야 하며, 오답 보기도 그럴듯하게 만들어.
5. question, answer, explanation은 반드시 채워. (비우면 안 됨)
6. 순수 한글 위주로 작성하고, 최종 출력은 위 형식의 JSON 하나만.

이제 위 [주제]에 맞춰 [문항 수]개를 만들어줘.
""".trim()

    /** 이미지/교과서 사진을 첨부해 만드는 경우 덧붙이는 안내 문구 */
    val IMAGE_PROMPT_TIP: String = """
[이미지·교과서 사진으로 만들기]
- 사진을 볼 수 있는 AI(ChatGPT-4o, Gemini, Claude 등)에 교과서/문제집/필기 사진을 첨부하고,
  위 프롬프트에 다음을 덧붙이세요:
  "첨부한 사진의 내용을 바탕으로 위 형식대로 퀴즈를 만들어줘. 사진 속 문장을 그대로 베끼지 말고,
   핵심 개념을 이해해서 새로운 문제로 재구성해줘."
- 저작권 주의: 촬영·입력한 자료의 이용 권한 확인은 본인 책임이며, 개인 학습 용도로만 사용하세요.
""".trim()

    /** 내려받기(공유)용 전체 가이드 문서 (마크다운) */
    val GUIDE_MARKDOWN: String = """
# KDailyUtil — AI로 개인 퀴즈 만들기 가이드

이 가이드를 AI(ChatGPT·Gemini·Claude 등)에 넣으면, 앱에 바로 가져올 수 있는
개인 퀴즈 파일(.kquiz)을 만들 수 있습니다.

## 1) 사용 순서 (요약)
1. 아래 '프롬프트'를 복사해 AI에 붙여넣습니다.
2. [주제]·[문항 수] 등을 원하는 대로 바꿔 요청합니다.
3. AI가 출력한 JSON을 복사해 텍스트 파일로 저장합니다.
4. 파일 이름을 `주제이름.kquiz`로 바꿉니다. (확장자를 .kquiz 로)
5. 그 파일을 휴대폰으로 옮긴 뒤, 앱 → 배움터 → 우리말 퀴즈 →
   '📥 외부 퀴즈 패키지(.kquiz) 가져오기'로 불러옵니다.

## 2) AI에 붙여넣을 프롬프트
$PROMPT_TEMPLATE

## 3) 이미지·교과서 사진으로 만들기
$IMAGE_PROMPT_TIP

## 4) 사용 예시
- "이 형식대로 퀴즈 10개 만들어줘. 주제는 '세계사 로마', 난이도는 중."
- "형식대로 문제 만들어줘. 주제는 '기초 경제 용어', 객관식만."
- (사진 첨부) "첨부한 교과서 사진 내용으로 위 형식대로 퀴즈를 만들어줘."

## 5) 자주 나는 오류
- 앱에서 안 열려요 → JSON 형식이 깨졌을 수 있습니다. AI에게
  "코드블록 없이 순수 JSON만, 형식 오류 없이 다시 줘"라고 요청하세요.
- 문제가 중복돼요 → 앱이 같은 정답/질문은 자동으로 한 번만 보여줍니다(정상).
- 확장자 → 파일 끝이 반드시 .kquiz 여야 합니다. (.txt 이면 이름 변경)

— 이 파일은 KDailyUtil 앱의 '퀴즈 만들기 가이드'에서 내려받았습니다.
""".trim()

    /**
     * 가이드를 .md 파일로 저장하고 공유용 content:// URI를 반환한다.
     */
    fun exportGuide(context: Context): Uri? {
        return try {
            val sharedDir = File(context.cacheDir, SHARED_DIR_NAME).apply { if (!exists()) mkdirs() }
            val file = File(sharedDir, GUIDE_FILE_NAME)
            file.writeText(GUIDE_MARKDOWN)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e(TAG, "가이드 내보내기 실패: ${e.message}", e)
            null
        }
    }

    /** 안드로이드 공유 시트로 가이드 파일을 내보낸다. */
    fun shareGuide(context: Context) {
        val uri = exportGuide(context) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "KDailyUtil 퀴즈 만들기 가이드")
            putExtra(Intent.EXTRA_TEXT, "AI로 개인 퀴즈(.kquiz)를 만드는 가이드입니다.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "가이드 내보내기").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
