import csv

data = [
    # SUN
    ["Sunday", "4-1", "CSE404", "", "Md. Ferdous", "407", "10:40 AM - 12:20 PM"],
    ["Sunday", "4-1", "CSE403", "", "Md. Ferdous", "408", "12:20 PM - 01:10 PM"],
    ["Sunday", "4-1", "CSE405", "", "Dr. Mrinal Kanti Baowaly", "408", "02:10 PM - 03:50 PM"],
    
    ["Sunday", "3-2", "CSE359", "", "Faruk Hossen", "408", "10:40 AM - 11:30 AM"],
    ["Sunday", "3-2", "CSE356", "", "Dr. Mrinal Kanti Baowaly", "407", "11:30 AM - 12:20 PM"],
    ["Sunday", "3-2", "CSE353", "", "Nahida Islam", "407", "02:10 PM - 03:00 PM"],
    ["Sunday", "3-2", "CSE355", "", "Dr. Mrinal Kanti Baowaly", "408", "03:00 PM - 03:50 PM"],
    
    ["Sunday", "2-2", "CSE256", "", "Nahida Islam", "407", "09:00 AM - 10:40 AM"],
    ["Sunday", "2-2", "CSE255", "", "Nahida Islam", "505", "10:40 AM - 11:30 AM"],
    ["Sunday", "2-2", "CSE253", "", "Abu-Bakar Muhammad Abdullah", "411A", "11:30 AM - 12:20 PM"],
    ["Sunday", "2-2", "MAT265", "", "Sabina Yeasmin", "411A", "12:20 PM - 01:10 PM"],
    
    ["Sunday", "2-1", "CSE206", "", "Abu-Bakar Muhammad Abdullah", "505", "12:20 PM - 01:10 PM"],
    ["Sunday", "2-1", "MAT209", "", "Sabina Yeasmin", "411A", "02:10 PM - 03:00 PM"],
    
    ["Sunday", "1-1", "CSE103", "", "Md. Ferdous", "505", "09:50 AM - 10:40 AM"],
    ["Sunday", "1-1", "CSE105", "", "Dr. Mrinal Kanti Baowaly", "411A", "10:40 AM - 11:30 AM"],
    ["Sunday", "1-1", "EEE108", "", "Dr. A.T.M Saiful Islam", "EEE dept.", "11:30 AM - 01:10 PM"],
    ["Sunday", "1-1", "MAT109", "", "Dr. Mohammad Rafiqul Islam", "411", "02:10 PM - 03:00 PM"],
    ["Sunday", "1-1", "ENG111", "", "Md. Moinul Islam", "505", "03:00 PM - 03:50 PM"],

    # MON
    ["Monday", "4-1", "CSE406", "", "Dr. Mrinal Kanti Baowaly", "407", "10:40 AM - 12:20 PM"],
    ["Monday", "4-1", "CSE401", "", "Nahida Islam", "408", "02:10 PM - 03:00 PM"],
    ["Monday", "4-1", "CSE413", "", "Md. Nesarul Hoque", "408", "03:00 PM - 03:50 PM"],

    ["Monday", "3-2", "CSE355", "", "Dr. Mrinal Kanti Baowaly", "408", "09:50 AM - 10:40 AM"],
    ["Monday", "3-2", "CSE351", "", "Dr. Saleh Ahmed", "408", "10:40 AM - 11:30 AM"],
    ["Monday", "3-2", "CSE359", "", "Faruk Hossen", "408", "11:30 AM - 01:10 PM"],
    ["Monday", "3-2", "CSE357", "", "Md. Nesarul Hoque", "407", "02:10 PM - 03:00 PM"],

    ["Monday", "2-2", "CSE254", "", "Abu-Bakar Muhammad Abdullah", "407", "09:00 AM - 10:40 AM"],
    ["Monday", "2-2", "BUS263", "", "Dr. Hasinat Raquiba", "407", "10:40 AM - 11:30 AM"],
    ["Monday", "2-2", "CSE255", "", "Nahida Islam", "408", "11:30 AM - 12:20 PM"],
    ["Monday", "2-2", "CSE253", "", "Abu-Bakar Muhammad Abdullah", "411A", "12:20 PM - 01:10 PM"],
    ["Monday", "2-2", "CSE259", "", "Abu-Bakar Muhammad Abdullah", "407A", "03:00 PM - 03:50 PM"],

    ["Monday", "2-1", "CSE201", "", "Md. Nesarul Hoque", "411A", "09:50 AM - 10:40 AM"],
    ["Monday", "2-1", "CSE205", "", "Abu-Bakar Muhammad Abdullah", "505", "10:40 AM - 11:30 AM"],
    ["Monday", "2-1", "CSE203", "", "Dr. Saleh Ahmed", "411A", "11:30 AM - 12:20 PM"],
    ["Monday", "2-1", "CSE204", "", "Dr. Saleh Ahmed", "505", "12:20 PM - 01:10 PM"],
    ["Monday", "2-1", "EEE207", "", "Dr. Md. Rabiul Islam", "411A", "03:00 PM - 03:50 PM"],

    ["Monday", "1-1", "EEE107", "", "Dr. Arifuzzaman Rajib", "411A", "09:00 AM - 09:50 AM"],
    ["Monday", "1-1", "CSE103", "", "Md. Ferdous", "505", "09:50 AM - 10:40 AM"],
    ["Monday", "1-1", "CSE105", "", "Dr. Mrinal Kanti Baowaly", "411A", "10:40 AM - 11:30 AM"],
    ["Monday", "1-1", "MAT109", "", "Dr. Mohammad Rafiqul Islam", "411A", "02:10 PM - 03:00 PM"]
]

with open('routine_sample.csv', 'w', newline='') as file:
    writer = csv.writer(file)
    writer.writerow(["Day", "Batch", "CourseCode", "CourseTitle", "Teacher", "Room", "TimeSlot"])
    writer.writerows(data)

print("CSV generated.")
