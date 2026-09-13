#!/bin/bash
sed -i '' '/<ImageView/N;s/<ImageView\n *<ImageView/<ImageView/g' app/src/main/res/layout/fragment_pdf_library.xml
