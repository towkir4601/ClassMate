import re

with open('app/src/main/res/layout/fragment_friends.xml', 'r') as f:
    content = f.read()

bad = """            <com.google.android.material.chip.Chip
                android:id="@+id/chipDistrict"
                style="@style/Widget.MaterialComponents.Chip.Choice"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Home District"
                app:closeIconVisible="false" />"""
good = """            <com.google.android.material.chip.Chip
                android:id="@+id/chipDistrict"
                style="@style/Widget.MaterialComponents.Chip.Choice"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Home District"
                app:closeIconVisible="false" />
                
            <com.google.android.material.chip.Chip
                android:id="@+id/chipBatch"
                style="@style/Widget.MaterialComponents.Chip.Choice"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Batch"
                app:closeIconVisible="false" />"""
content = content.replace(bad, good)
with open('app/src/main/res/layout/fragment_friends.xml', 'w') as f:
    f.write(content)
