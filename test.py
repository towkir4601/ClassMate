from datetime import datetime

t = datetime.strptime("14:10", "%H:%M")
print(t.strftime("%I:%M %p"))
