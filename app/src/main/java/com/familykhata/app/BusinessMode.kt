package com.familykhata.app

enum class BusinessMode {
    RETAIL,
    PHARMACY,
    EXPIRY_RETAIL,
    ELECTRONICS,
    FASHION,
    FOOD_SERVICE,
    SERVICE_JOB,
    MEMBERSHIP_SERVICE,
    BOOKING_RENTAL,
    PRODUCTION,
    AGRO,
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
            "ডিজিটাল এজেন্সি"
        ).any { value.contains(it) } ->
            BusinessMode.DIGITAL_AGENCY

        listOf(
            "dealership",
            "distribution",
            "ডিলারশিপ",
            "ডিস্ট্রিবিউশন"
        ).any { value.contains(it) } ->
            BusinessMode.DEALERSHIP

        listOf(
            "pharmacy",
            "medicine",
            "ফার্মেসি",
            "মেডিসিন"
        ).any { value.contains(it) } ->
            BusinessMode.PHARMACY

        listOf(
            "electronics",
            "mobile",
            "gadgets",
            "computer",
            "ইলেকট্রনিক্স",
            "মোবাইল",
            "গ্যাজেট",
            "কম্পিউটার"
        ).any { value.contains(it) } ->
            BusinessMode.ELECTRONICS

        listOf(
            "fashion",
            "clothing",
            "shoes",
            "bag",
            "luggage",
            "jewellery",
            "leather",
            "ফ্যাশন",
            "পোশাক",
            "জুতা",
            "ব্যাগ",
            "জুয়েলারি",
            "লেদার"
        ).any { value.contains(it) } ->
            BusinessMode.FASHION

        listOf(
            "restaurant",
            "catering",
            "রেস্টুরেন্ট",
            "ক্যাটারিং"
        ).any { value.contains(it) } ->
            BusinessMode.FOOD_SERVICE

        listOf(
            "service & repair",
            "service and repair",
            "laundry",
            "beauty",
            "salon",
            "spa",
            "mobile top-up",
            "সার্ভিস",
            "রিপেয়ার",
            "লন্ড্রি",
            "বিউটি",
            "সেলুন",
            "স্পা",
            "মোবাইল রিচার্জ"
        ).any { value.contains(it) } ->
            BusinessMode.SERVICE_JOB

        listOf(
            "gym",
            "fitness",
            "জিম",
            "ফিটনেস"
        ).any { value.contains(it) } ->
            BusinessMode.MEMBERSHIP_SERVICE

        listOf(
            "travel",
            "ticket",
            "car rental",
            "ট্রাভেল",
            "টিকেট",
            "কার রেন্টাল"
        ).any { value.contains(it) } ->
            BusinessMode.BOOKING_RENTAL

        listOf(
            "manufacturing",
            "raw materials",
            "handicraft",
            "ম্যানুফ্যাকচারিং",
            "কাঁচামাল",
            "হস্তশিল্প"
        ).any { value.contains(it) } ->
            BusinessMode.PRODUCTION

        listOf(
            "poultry",
            "agriculture",
            "agro",
            "পোল্ট্রি",
            "কৃষি",
            "এগ্রো"
        ).any { value.contains(it) } ->
            BusinessMode.AGRO

        listOf(
            "grocery",
            "fruit",
            "vegetable",
            "bakery",
            "cosmetics",
            "মুদি",
            "ফল",
            "সবজি",
            "বেকারি",
            "কসমেটিকস"
        ).any { value.contains(it) } ->
            BusinessMode.EXPIRY_RETAIL

        else ->
            BusinessMode.RETAIL
    }
}
