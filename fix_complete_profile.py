with open('app/src/main/java/com/shuaib/classmate/activities/CompleteProfileActivity.kt', 'r') as f:
    content = f.read()

old_nav = '''    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }'''

new_nav = '''    private fun navigateToMain() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }'''

if old_nav in content:
    content = content.replace(old_nav, new_nav)
    with open('app/src/main/java/com/shuaib/classmate/activities/CompleteProfileActivity.kt', 'w') as f:
        f.write(content)
    print("Patched CompleteProfileActivity routing")
else:
    print("Could not find old routing in CompleteProfileActivity")

