import re

with open('app/src/main/java/com/shuaib/classmate/data/local/TimetableEntity.kt', 'r') as f:
    content = f.read()

bad_data = """    val overrideDate: String = "",
    val overrideRoom: String = "",
    val overrideStartTime: String = "",
    val overrideEndTime: String = "",
    val cachedAtMillis: Long = System.currentTimeMillis()
)"""
good_data = """    val overrideDate: String = "",
    val overrideRoom: String = "",
    val overrideStartTime: String = "",
    val overrideEndTime: String = "",
    val isTemporary: Boolean = false,
    val temporaryDate: String = "",
    val batch: String = "",
    val cachedAtMillis: Long = System.currentTimeMillis()
)"""
content = content.replace(bad_data, good_data)

bad_toPeriod = """            overrideDate = overrideDate,
            overrideRoom = overrideRoom,
            overrideStartTime = overrideStartTime,
            overrideEndTime = overrideEndTime
        )"""
good_toPeriod = """            overrideDate = overrideDate,
            overrideRoom = overrideRoom,
            overrideStartTime = overrideStartTime,
            overrideEndTime = overrideEndTime,
            isTemporary = isTemporary,
            temporaryDate = temporaryDate,
            batch = batch
        )"""
content = content.replace(bad_toPeriod, good_toPeriod)

bad_fromPeriod = """            overrideDate = period.overrideDate,
            overrideRoom = period.overrideRoom,
            overrideStartTime = period.overrideStartTime,
            overrideEndTime = period.overrideEndTime
        )"""
good_fromPeriod = """            overrideDate = period.overrideDate,
            overrideRoom = period.overrideRoom,
            overrideStartTime = period.overrideStartTime,
            overrideEndTime = period.overrideEndTime,
            isTemporary = period.isTemporary,
            temporaryDate = period.temporaryDate,
            batch = period.batch
        )"""
content = content.replace(bad_fromPeriod, good_fromPeriod)

with open('app/src/main/java/com/shuaib/classmate/data/local/TimetableEntity.kt', 'w') as f:
    f.write(content)
