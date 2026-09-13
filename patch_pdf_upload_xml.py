with open("app/src/main/res/layout/activity_pdf_upload.xml", "r") as f:
    content = f.read()

old_add_btn = """            <ImageView
                android:id="@+id/btnAddSubject"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:layout_marginStart="8dp"
                android:layout_marginTop="4dp"
                android:padding="12dp"
                android:background="@drawable/bg_day_pill_selected"
                android:src="@drawable/ic_add"
                app:tint="@color/cm_on_primary"
                android:contentDescription="Add Subject" />"""

new_add_btn = """            <ImageView
                android:id="@+id/btnAddSubject"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:layout_marginStart="8dp"
                android:layout_marginTop="4dp"
                android:padding="12dp"
                android:background="@drawable/bg_day_pill_selected"
                android:src="@drawable/ic_add"
                app:tint="@color/cm_on_primary"
                android:contentDescription="Add Subject" />

            <ImageView
                android:id="@+id/btnDeleteSubject"
                android:layout_width="48dp"
                android:layout_height="48dp"
                android:layout_marginStart="8dp"
                android:layout_marginTop="4dp"
                android:padding="12dp"
                android:background="@drawable/bg_day_pill"
                android:src="@drawable/ic_delete"
                app:tint="@color/cm_primary"
                android:visibility="gone"
                android:contentDescription="Delete Subject" />"""

content = content.replace(old_add_btn, new_add_btn)

with open("app/src/main/res/layout/activity_pdf_upload.xml", "w") as f:
    f.write(content)
