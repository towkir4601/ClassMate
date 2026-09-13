with open("app/src/main/res/layout/activity_user_detail.xml", "r") as f:
    content = f.read()

old_admin = """                    <RadioButton
                        android:id="@+id/radioAdmin"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:buttonTint="@color/cm_primary"
                        android:text="Admin"
                        android:textColor="@color/cm_text_primary" />"""

new_teacher = """                    <RadioButton
                        android:id="@+id/radioAdmin"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:buttonTint="@color/cm_primary"
                        android:text="Admin"
                        android:textColor="@color/cm_text_primary" />

                    <RadioButton
                        android:id="@+id/radioTeacher"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:buttonTint="@color/cm_primary"
                        android:text="Teacher"
                        android:textColor="@color/cm_text_primary" />"""

content = content.replace(old_admin, new_teacher)

with open("app/src/main/res/layout/activity_user_detail.xml", "w") as f:
    f.write(content)
