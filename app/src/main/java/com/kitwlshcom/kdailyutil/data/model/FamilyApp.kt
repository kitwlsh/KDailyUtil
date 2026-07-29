package com.kitwlshcom.kdailyutil.data.model

/**
 * K-시리즈 자매앱 1건 — 원격 레지스트리 `family.json` 의 `apps[]` 항목. (2026-07-29)
 * 표준 정의 = doc/KLOTTO_CONNECT_HANDOFF.md §8-3.
 *
 * ⚠️ **표시용 데이터만** 담는다(패키지 id·스토어 URL·라벨). 임의 인텐트/딥링크는 스키마에 없으며,
 * `id`는 `openAppOrStore`(런처 인텐트 / 마켓 URL)에만 사용한다(§8-7 보안 규칙).
 */
data class FamilyApp(
    /** applicationId. `openAppOrStore` 인자 + 자기 자신 판정에 사용. 대소문자 그대로. */
    val id: String,
    /** 카드 제목 */
    val name: String,
    /** 한줄 소개 */
    val tagline: String = "",
    /** 아이콘 이미지 URL. 화이트리스트를 통과한 https URL만 담긴다(없으면 번들 아이콘 폴백). */
    val iconUrl: String? = null,
    /** Play 스토어 URL. 화이트리스트를 통과한 값만 담긴다(없으면 `id`로 생성). */
    val storeUrl: String? = null,
    /** true = 카드는 보이지만 '출시 예정'으로 비활성(미출시 앱). */
    val comingSoon: Boolean = false,
    /** 정렬 순서(작은 값 먼저) */
    val order: Int = 0
)
