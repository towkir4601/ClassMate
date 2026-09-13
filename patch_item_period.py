import re

with open('app/src/main/res/layout/item_period.xml', 'r') as f:
    content = f.read()

bad = """            <TextView
                android:id="@+id/tvTypeBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:background="@drawable/bg_badge_blue"
                android:gravity="center"
                android:minWidth="58dp"
                android:paddingHorizontal="6dp"
                android:paddingVertical="5dp"
                android:textColor="@color/cm_primary"
                android:textSize="9.5sp"
                android:textStyle="bold" />"""
good = """            <TextView
                android:id="@+id/tvTypeBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:background="@drawable/bg_badge_blue"
                android:gravity="center"
                android:minWidth="58dp"
                android:paddingHorizontal="6dp"
                android:paddingVertical="5dp"
                android:textColor="@color/cm_primary"
                android:textSize="9.5sp"
                android:textStyle="bold" />
                
            <TextView
                android:id="@+id/tvBatchBadge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:layout_gravity="center_horizontal"
                android:background="@drawable/bg_badge_purple"
                android:gravity="center"
                android:minWidth="58dp"
                android:paddingHorizontal="6dp"
                android:paddingVertical="3dp"
                android:textColor="@color/cm_warning"
                android:textSize="8.5sp"
                android:textStyle="bold"
                android:visibility="gone" />"""
content = content.replace(bad, good)
with open('app/src/main/res/layout/item_period.xml', 'w') as f:
    f.write(content)
