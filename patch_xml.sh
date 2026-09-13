#!/bin/bash

# Function to add the button before ivArrow*
# Regular
sed -i '' '/android:id="@+id\/ivArrowRegular"/i\
                        <ImageView\
                            android:id="@+id/btnAddRegular"\
                            android:layout_width="32dp"\
                            android:layout_height="32dp"\
                            android:layout_marginEnd="8dp"\
                            android:padding="4dp"\
                            android:src="@drawable/ic_plus"\
                            android:background="?attr/selectableItemBackgroundBorderless"\
                            android:visibility="gone"\
                            app:tint="@color/cm_primary" />\
' app/src/main/res/layout/fragment_pdf_library.xml

# Lab
sed -i '' '/android:id="@+id\/ivArrowLab"/i\
                        <ImageView\
                            android:id="@+id/btnAddLab"\
                            android:layout_width="32dp"\
                            android:layout_height="32dp"\
                            android:layout_marginEnd="8dp"\
                            android:padding="4dp"\
                            android:src="@drawable/ic_plus"\
                            android:background="?attr/selectableItemBackgroundBorderless"\
                            android:visibility="gone"\
                            app:tint="@color/cm_primary" />\
' app/src/main/res/layout/fragment_pdf_library.xml

# Other
sed -i '' '/android:id="@+id\/ivArrowOther"/i\
                        <ImageView\
                            android:id="@+id/btnAddOther"\
                            android:layout_width="32dp"\
                            android:layout_height="32dp"\
                            android:layout_marginEnd="8dp"\
                            android:padding="4dp"\
                            android:src="@drawable/ic_plus"\
                            android:background="?attr/selectableItemBackgroundBorderless"\
                            android:visibility="gone"\
                            app:tint="@color/cm_primary" />\
' app/src/main/res/layout/fragment_pdf_library.xml
