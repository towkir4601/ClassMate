sed -i '' '/android:id="@+id\/chipDistrict"/,/app:closeIconVisible="false" \/>/a\
\
            <com.google.android.material.chip.Chip\
                android:id="@+id/chipBatch"\
                style="@style/Widget.MaterialComponents.Chip.Choice"\
                android:layout_width="wrap_content"\
                android:layout_height="wrap_content"\
                android:text="Batch"\
                app:closeIconVisible="false" />\
' app/src/main/res/layout/fragment_friends.xml
