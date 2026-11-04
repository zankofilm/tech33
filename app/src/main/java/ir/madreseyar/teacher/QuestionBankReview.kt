package ir.madreseyar.teacher

/** Small, platform-independent validation model for reviewing teacher bank questions. */
internal data class QuestionReviewMeta(
    val text: String,
    val type: String,
    val score: Double,
    val choices: List<String> = emptyList(),
    val answerIndex: Int? = null,
    val answerText: String = "",
    val boolAnswerKnown: Boolean = true,
    val imageDataUrl: String = "",
    val imageRequired: Boolean = false,
)

/** Empty means suitable for inclusion. Missing MCQ keys never become option A. */
internal fun questionReviewIssue(q: QuestionReviewMeta): String = when {
    q.text.isBlank() -> "متن سؤال خالی است."
    !q.score.isFinite() || q.score <= 0.0 || q.score > 20.0 -> "بارم باید بیشتر از صفر و حداکثر ۲۰ باشد."
    q.imageRequired && q.imageDataUrl.isBlank() -> "تصویر سؤال موجود نیست؛ تصویر جدید اضافه کنید."
    q.imageDataUrl.isNotBlank() &&
        !Regex("^data:image/(jpeg|png|webp);base64,", RegexOption.IGNORE_CASE).containsMatchIn(q.imageDataUrl) ->
        "فرمت تصویر پشتیبانی نمی‌شود؛ آن را جایگزین کنید."
    q.type == "mcq" && (q.choices.size != 4 || q.choices.any { it.isBlank() }) -> "چهار گزینه کامل لازم است."
    q.type == "mcq" && q.choices.map { it.trim().lowercase() }.distinct().size != 4 -> "گزینه‌ها تکراری هستند."
    q.type == "mcq" && q.answerIndex !in 0..3 -> "کلید صحیح چهارگزینه‌ای مشخص نشده است."
    q.type == "true_false" && !q.boolAnswerKnown -> "پاسخ صحیح/غلط مشخص نشده است."
    q.type == "fill_blank" && q.answerText.isBlank() -> "پاسخ جای خالی مشخص نشده است."
    q.type == "short_answer" && q.answerText.isBlank() -> "پاسخ مرجع کوتاه مشخص نشده است."
    else -> ""
}

/** Search Persian/Arabic orthographic variants, without modifying original question text. */
internal fun normalizeBankSearch(value: String): String = value.trim().lowercase()
    .replace('ي', 'ی').replace('ك', 'ک')
    .replace('أ', 'ا').replace('إ', 'ا')
    .replace('\u200c', ' ')
    .replace(Regex("\\s+"), " ")

internal fun bankQuestionMatchesSearch(needle: String, fields: List<String>): Boolean {
    val term = normalizeBankSearch(needle)
    return term.isEmpty() || fields.any { normalizeBankSearch(it).contains(term) }
}
