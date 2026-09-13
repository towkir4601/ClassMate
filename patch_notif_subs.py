with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "r") as f:
    content = f.read()

content = content.replace('"include_player_ids"', '"include_subscription_ids"')

with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "w") as f:
    f.write(content)
