import re

with open('app/src/main/res/layout/fragment_profile.xml', 'r') as f:
    content = f.read()

bad = """        <!-- 2. See Friends Card (Community) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardSeeFriends\""""
good = """        <!-- 2. See Friends Card (Community) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardSeeFriends\""""
            
new_card = """        <!-- Teachers Directory Card -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardTeacherDirectory"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="20dp"
            app:cardBackgroundColor="@color/cm_surface"
            app:cardCornerRadius="20dp"
            app:cardElevation="2dp"
            app:strokeWidth="1dp"
            app:strokeColor="@color/cm_border"
            android:clickable="true"
            android:focusable="true"
            android:foreground="?android:attr/selectableItemBackground">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:padding="20dp">

                <ImageView
                    android:layout_width="28dp"
                    android:layout_height="28dp"
                    android:src="@drawable/ic_library_books"
                    app:tint="@color/cm_primary"
                    android:importantForAccessibility="no" />

                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="16dp"
                    android:text="Teacher Directory"
                    android:textColor="@color/cm_text_primary"
                    android:textSize="16sp"
                    android:textStyle="bold" />

                <ImageView
                    android:layout_width="24dp"
                    android:layout_height="24dp"
                    android:src="@drawable/ic_chevron_right"
                    app:tint="@color/cm_text_disabled"
                    android:importantForAccessibility="no" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- 2. See Friends Card (Community) -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardSeeFriends\""""

content = content.replace(bad, new_card)

with open('app/src/main/res/layout/fragment_profile.xml', 'w') as f:
    f.write(content)
