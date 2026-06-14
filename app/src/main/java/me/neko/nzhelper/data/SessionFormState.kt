package me.neko.nzhelper.data

data class SessionFormState(
    val remark: String = "",
    val location: String = "",
    val watchedMovie: Boolean = false,
    val climax: Boolean = false,
    val rating: Float = 3f,
    val mood: String = "平静",
    val props: String = "手"
)
