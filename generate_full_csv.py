import csv

titles = {
    "CSE102": "Computer Science Fundamentals Sessional",
    "CSE103": "Structured Programming Language",
    "CSE104": "Structured Programming Language Sessional",
    "CSE105": "Discrete Mathematics",
    "EEE107": "Basic Electrical Engineering",
    "EEE108": "Basic Electrical Engineering Sessional",
    "MAT109": "Differential and Integral Calculus",
    "ENG111": "Functional English",
    "ENG112": "Functional English Sessional",
    "CSE201": "Data Structure",
    "CSE202": "Data Structure Sessional",
    "CSE203": "Object Oriented Programming II (Python)",
    "CSE204": "Object Oriented Programming II Sessional",
    "CSE205": "Digital Logic Design",
    "CSE206": "Digital Logic Design Sessional",
    "EEE207": "Electrical Drives and Instrumentation",
    "EEE208": "Electrical Drives and Instrumentation Sessional",
    "MAT209": "Vector Analysis and Linear Algebra",
    "BUS211": "Industrial Management and Accountancy",
    "CSE230": "Viva Voce III"
}

teachers = {
    "DMKB": "Dr. Mrinal Kanti Baowaly",
    "DSA": "Dr. Saleh Ahmed",
    "MNH": "Md. Nesarul Hoque",
    "AMA": "Abu-Bakar Muhammad Abdullah",
    "FH": "Faruk Hossen",
    "MF": "Md. Ferdous",
    "NI": "Nahida Islam",
    "MMI": "Md. Moinul Islam",
    "SY": "Sabina Yeasmin",
    "DMRI": "Dr. Mohammad Rafiqul Islam",
    "DHR": "Dr. Hasinat Raquiba",
    "DAR": "Dr. Arifuzzaman Rajib",
    "DATMSI": "Dr. A.T.M Saiful Islam",
    "MRI": "Dr. Md. Rabiul Islam",
    "DNF": "Dr. Naim Ferdous",
    "RI": "Rabiul Islam"
}

raw_data = """Day|Batch|CourseCode|TeacherCode|Room|TimeSlot
Sunday|4-1|CSE404|MF|407|10:40 AM - 12:20 PM
Sunday|4-1|CSE403|MF|408|12:20 PM - 01:10 PM
Sunday|4-1|CSE405|DMKB|408|02:10 PM - 03:50 PM
Sunday|3-2|CSE359|FH|408|10:40 AM - 11:30 AM
Sunday|3-2|CSE356|DMKB|407|11:30 AM - 12:20 PM
Sunday|3-2|CSE353|NI|407|02:10 PM - 03:00 PM
Sunday|3-2|CSE355|DMKB|408|03:00 PM - 03:50 PM
Sunday|2-2|CSE256|NI|407|09:00 AM - 10:40 AM
Sunday|2-2|CSE255|NI|505|10:40 AM - 11:30 AM
Sunday|2-2|CSE253|AMA|411A|11:30 AM - 12:20 PM
Sunday|2-2|MAT265|SY|411A|12:20 PM - 01:10 PM
Sunday|2-1|CSE206|AMA|505|12:20 PM - 01:10 PM
Sunday|2-1|MAT209|SY|411A|02:10 PM - 03:00 PM
Sunday|1-1|CSE103|MF|505|09:50 AM - 10:40 AM
Sunday|1-1|CSE105|DMKB|411A|10:40 AM - 11:30 AM
Sunday|1-1|EEE108|DATMSI|EEE dept.|11:30 AM - 01:10 PM
Sunday|1-1|MAT109|DMRI|411|02:10 PM - 03:00 PM
Sunday|1-1|ENG111|MMI|505|03:00 PM - 03:50 PM
Monday|4-1|CSE406|DMKB|407|10:40 AM - 12:20 PM
Monday|4-1|CSE401|NI|408|02:10 PM - 03:00 PM
Monday|4-1|CSE413|MNH|408|03:00 PM - 03:50 PM
Monday|3-2|CSE355|DMKB|408|09:50 AM - 10:40 AM
Monday|3-2|CSE351|DSA|408|10:40 AM - 11:30 AM
Monday|3-2|CSE359|FH|408|11:30 AM - 01:10 PM
Monday|3-2|CSE357|MNH|407|02:10 PM - 03:00 PM
Monday|2-2|CSE254|AMA|407|09:00 AM - 10:40 AM
Monday|2-2|BUS263|DHR|407|10:40 AM - 11:30 AM
Monday|2-2|CSE255|NI|408|11:30 AM - 12:20 PM
Monday|2-2|CSE253|AMA|411A|12:20 PM - 01:10 PM
Monday|2-2|CSE259|AMA|407A|03:00 PM - 03:50 PM
Monday|2-1|CSE201|MNH|411A|09:50 AM - 10:40 AM
Monday|2-1|CSE205|AMA|505|10:40 AM - 11:30 AM
Monday|2-1|CSE203|DSA|411A|11:30 AM - 12:20 PM
Monday|2-1|CSE204|DSA|505|12:20 PM - 01:10 PM
Monday|2-1|EEE207|MRI|411A|03:00 PM - 03:50 PM
Monday|1-1|EEE107|DAR|411A|09:00 AM - 09:50 AM
Monday|1-1|CSE103|MF|505|09:50 AM - 10:40 AM
Monday|1-1|CSE105|DMKB|411A|10:40 AM - 11:30 AM
Monday|1-1|MAT109|DMRI|411A|02:10 PM - 03:00 PM
Tuesday|4-1|RESEARCH|RESEARCH|RESEARCH|09:00 AM - 10:40 AM
Tuesday|4-1|CSE414|MNH|407|10:40 AM - 11:30 AM
Tuesday|4-1|CSE403|MF|408|12:20 PM - 01:10 PM
Tuesday|4-1|CSE401|NI|408|02:10 PM - 03:00 PM
Tuesday|3-2|CSE351|DSA|408|10:40 AM - 11:30 AM
Tuesday|3-2|CSE353|NI|408|11:30 AM - 12:20 PM
Tuesday|3-2|CSE352|DSA|407|12:20 PM - 01:10 PM
Tuesday|3-2|CSE358|MNH|407|02:10 PM - 03:50 PM
Tuesday|2-2|CSE258|FH|407|09:00 AM - 10:40 AM
Tuesday|2-2|CSE255|NI|411A|12:20 PM - 01:10 PM
Tuesday|2-2|MAT265|SY|408|03:00 PM - 03:50 PM
Tuesday|2-1|CSE203|DSA|408|09:50 AM - 10:40 AM
Tuesday|2-1|BUS211|RI|411A|10:40 AM - 11:30 AM
Tuesday|2-1|EEE208|DNF|EEE Dept.|11:30 AM - 01:10 PM
Tuesday|2-1|MAT209|SY|411A|02:10 PM - 03:00 PM
Tuesday|2-1|EEE207|MRI|411A|03:00 PM - 03:50 PM
Tuesday|1-1|CSE104|MF|505|11:30 AM - 01:10 PM
Tuesday|1-1|ENG112|MMI|505|03:00 PM - 03:50 PM
Wednesday|4-1|CSE402|NI|407|09:50 AM - 11:30 AM
Wednesday|4-1|CSE413|MNH|408|11:30 AM - 12:20 PM
Wednesday|4-1|CSE403|MF|408|12:20 PM - 01:10 PM
Wednesday|4-1|CSE401|NI|408|02:10 PM - 03:00 PM
Wednesday|4-1|CSE405|DMKB|408|03:00 PM - 03:50 PM
Wednesday|3-2|CSE351|DSA|408|09:50 AM - 10:40 AM
Wednesday|3-2|CSE357|MNH|408|10:40 AM - 11:30 AM
Wednesday|3-2|CSE360|FH|407|12:20 PM - 01:10 PM
Wednesday|3-2|CSE359|FH|407|02:10 PM - 03:00 PM
Wednesday|2-2|CSE252|AMA|407|09:00 AM - 10:40 AM
Wednesday|2-2|BUS263|DHR|407A|11:30 AM - 01:10 PM
Wednesday|2-2|MAT265|SY|411A|02:10 PM - 03:00 PM
Wednesday|2-2|CSE253|AMA|407A|03:00 PM - 03:50 PM
Wednesday|2-1|CSE201|MNH|411A|09:50 AM - 10:40 AM
Wednesday|2-1|CSE203|DSA|411A|10:40 AM - 11:30 AM
Wednesday|2-1|BUS211|RI|505|11:30 AM - 12:20 PM
Wednesday|2-1|CSE205|AMA|505|12:20 PM - 01:10 PM
Wednesday|2-1|CSE202|MNH|505|02:10 PM - 03:50 PM
Wednesday|1-1|CSE104|MF|505|09:00 AM - 10:40 AM
Wednesday|1-1|CSE103|MF|505|10:40 AM - 11:30 AM
Wednesday|1-1|MAT109|DMRI|411A|11:30 AM - 12:20 PM
Wednesday|1-1|EEE107|DAR|411A|12:20 PM - 01:10 PM
Thursday|4-1|CSE413|MNH|408|02:10 PM - 03:00 PM
Thursday|4-1|CSE405|DMKB|408|03:00 PM - 03:50 PM
Thursday|3-2|CSE355|DMKB|408|09:50 AM - 10:40 AM
Thursday|3-2|CSE357|MNH|408|10:40 AM - 11:30 AM
Thursday|3-2|CSE353|FH|408|11:30 AM - 12:20 PM
Thursday|3-2|CSE354|NI|407|12:20 PM - 01:10 PM
Thursday|2-2|CSE254|AMA|407|09:00 AM - 10:40 AM
Thursday|2-2|CSE257|FH|505|10:40 AM - 11:30 AM
Thursday|2-2|CSE259|AMA|407A|11:30 AM - 01:10 PM
Thursday|2-1|CSE201|MNH|411A|09:00 AM - 10:40 AM
Thursday|2-1|CSE205|AMA|407A|10:40 AM - 11:30 AM
Thursday|2-1|CSE202|MNH|505|11:30 AM - 01:10 PM
Thursday|2-1|MAT209|SY|411A|02:10 PM - 03:00 PM
Thursday|1-1|CSE102|NI|505|09:00 AM - 10:40 AM
Thursday|1-1|CSE105|DMKB|411A|10:40 AM - 11:30 AM
"""

with open('routine_sample.csv', 'w', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(["Day","Batch","CourseCode","CourseTitle","Teacher","Room","TimeSlot"])
    
    for i, line in enumerate(raw_data.strip().split('\n')):
        if i == 0: continue # header
        
        parts = line.split('|')
        day = parts[0]
        batch = parts[1]
        code = parts[2]
        teacher_code = parts[3]
        room = parts[4]
        timeslot = parts[5]
        
        title = titles.get(code, "")
        teacher = teachers.get(teacher_code, teacher_code)
        
        writer.writerow([day, batch, code, title, teacher, room, timeslot])

print("Done")
