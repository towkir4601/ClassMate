with open('app/src/main/res/layout/fragment_timetable.xml', 'r') as f:
    content = f.read()

old_header = '''                <TextView
                    android:id="@+id/tvPeriodCount"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:background="@drawable/bg_badge_blue"
                    android:fontFamily="sans-serif"
                    android:paddingHorizontal="10dp"
                    android:paddingVertical="3dp"
                    android:textColor="@color/cm_primary"
                    android:textSize="11sp"
                    android:textStyle="bold"/>'''

new_header = '''                <TextView
                    android:id="@+id/tvPeriodCount"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:background="@drawable/bg_badge_blue"
                    android:fontFamily="sans-serif"
                    android:paddingHorizontal="10dp"
                    android:paddingVertical="3dp"
                    android:textColor="@color/cm_primary"
                    android:textSize="11sp"
                    android:textStyle="bold"/>

                <Spinner
                    android:id="@+id/spinnerBatchFilter"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="8dp"
                    android:minHeight="24dp"
                    android:background="@drawable/bg_spinner"
                    android:paddingStart="8dp"
                    android:paddingEnd="24dp"
                    android:popupBackground="@color/cm_surface"
                    android:visibility="gone" />'''

if old_header in content:
    content = content.replace(old_header, new_header)
    with open('app/src/main/res/layout/fragment_timetable.xml', 'w') as f:
        f.write(content)
    print("Patched XML")
else:
    print("Could not patch XML")
