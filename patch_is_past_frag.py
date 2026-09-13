import re

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'r') as f:
    content = f.read()

is_today_line = "val isToday = day.lowercase() == days[getTodayIndex()].lowercase()"
is_past_line = "val isPastDay = days.indexOf(day.lowercase()) < getTodayIndex()"
content = content.replace(is_today_line, is_today_line + "\n        " + is_past_line)

adapter_inst_old = """                periodAdapter = PeriodAdapter(
                    periods = periods,
                    isPausedByCalendarException = shouldPauseSelectedDay(),
                    isViewingToday = isToday,"""
adapter_inst_new = """                periodAdapter = PeriodAdapter(
                    periods = periods,
                    isPausedByCalendarException = shouldPauseSelectedDay(),
                    isViewingToday = isToday,
                    isPastDay = isPastDay,"""
content = content.replace(adapter_inst_old, adapter_inst_new)

adapter_upd_old = "periodAdapter?.updateList(periods, isToday, isDaySwitch)"
adapter_upd_new = "periodAdapter?.updateList(periods, isToday, isPastDay, isDaySwitch)"
content = content.replace(adapter_upd_old, adapter_upd_new)

with open('app/src/main/java/com/shuaib/classmate/fragments/TimetableFragment.kt', 'w') as f:
    f.write(content)
