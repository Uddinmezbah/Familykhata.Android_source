package com.familykhata.app

enum class BusinessMode {
    RETAIL,
    COACHING,
    DIGITAL_AGENCY,
    DEALERSHIP
}

fun detectBusinessMode(shopType: String): BusinessMode {
    val value = shopType.lowercase()

    return when {
        listOf(
            "coaching",
            "education",
            "কোচিং",
            "শিক্ষা"
        ).any { value.contains(it) } ->
            BusinessMode.COACHING

        listOf(
            "digital agency",
            "agency",
            "ডিজিটাল এজেন্সি",
            "এজেন্সি"
        ).any { value.contains(it) } ->
            BusinessMode.DIGITAL_AGENCY

        listOf(
            "dealership",
            "distribution",
            "dealer",
            "ডিলারশিপ",
            "ডিস্ট্রিবিউশন",
            "ডিলার"
        ).any { value.contains(it) } ->
            BusinessMode.DEALERSHIP

        else ->
            BusinessMode.RETAIL
    }
}
