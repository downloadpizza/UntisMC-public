package net.downloadpizza.untiskt

import com.beust.klaxon.JsonObject

data class Period(
    val id: Int,
    val date: Int,
    val lessonTime: LessonTime,
    val code: String,
    val className: Array<FieldData>,
    val teacherName: Array<FieldData>,
    val subjectName: Array<FieldData>,
    val roomName: Array<FieldData>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Period) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}

fun jsonToPeriod(obj: JsonObject) = Period(
    obj.int("id")!!,
    obj.int("date")!!,
    LessonTime(obj.int("startTime")!!, obj.int("endTime")!!),
    obj.string("code") ?: "",
    obj.fieldName("kl"),
    obj.fieldName("te"),
    obj.fieldName("su"),
    obj.fieldName("ro")
)

data class LessonTime(val startTime: Int, val endTime: Int) : Comparable<LessonTime> {
    override fun compareTo(other: LessonTime): Int = if (startTime != other.startTime) {
        startTime - other.startTime
    } else {
        endTime - other.endTime
    }
}

data class FieldData(val name: String, val orgname: String?)

fun JsonObject.fieldName(field: String) = this.array<JsonObject>(field)
    ?.filterNot { it.string("name") == null || it.int("id") == null }
    ?.map { FieldData(it.string("name")!!, it.string("orgname")) }
    ?.toTypedArray() ?: emptyArray()