with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'r') as f:
    content = f.read()

bad_else = """                else -> {
                    bindStatusColor(context, b, accent)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_paused)"""

good_else = """                else -> {
                    bindStatusColor(context, b, accent)
                    b.cardRoot.setBackgroundResource(R.drawable.bg_card_primary)"""

content = content.replace(bad_else, good_else)

with open('app/src/main/java/com/shuaib/classmate/adapters/PeriodAdapter.kt', 'w') as f:
    f.write(content)
