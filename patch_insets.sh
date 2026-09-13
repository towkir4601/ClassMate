#!/bin/bash
file1="app/src/main/java/com/shuaib/classmate/chat/DmChatFragment.kt"
file2="app/src/main/java/com/shuaib/classmate/chat/GroupChatFragment.kt"

# Insert imports
sed -i '' -e 's/import android.os.Bundle/import android.os.Bundle\nimport androidx.core.view.ViewCompat\nimport androidx.core.view.WindowInsetsCompat/g' $file1
sed -i '' -e 's/import android.os.Bundle/import android.os.Bundle\nimport androidx.core.view.ViewCompat\nimport androidx.core.view.WindowInsetsCompat/g' $file2

# Insert apply insets logic
sed -i '' -e '/super.onViewCreated(view, savedInstanceState)/a\
        \
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->\
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom\
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom\
            v.setPadding(0, 0, 0, java.lang.Math.max(imeHeight, navHeight))\
            insets\
        }\
' $file1

sed -i '' -e '/super.onViewCreated(view, savedInstanceState)/a\
        \
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->\
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom\
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom\
            v.setPadding(0, 0, 0, java.lang.Math.max(imeHeight, navHeight))\
            insets\
        }\
' $file2
