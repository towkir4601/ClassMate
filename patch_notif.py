with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "r") as f:
    content = f.read()

# I will log the response body if it fails, so we can see the exact error.
if "val responseMessage = connection.inputStream.bufferedReader().use { it.readText() }" not in content:
    old_catch = """                val responseCode = connection.responseCode
                withContext(Dispatchers.Main) {
                    if (responseCode == 200 || responseCode == 201 || responseCode == 204) onSuccess()
                    else onFailure("HTTP $responseCode")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onFailure(e.message ?: "Unknown error") }
            }"""
            
    new_catch = """                val responseCode = connection.responseCode
                val responseMessage = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }
                android.util.Log.e("ONESIGNAL", "Response $responseCode: $responseMessage")
                withContext(Dispatchers.Main) {
                    if (responseCode in 200..299) onSuccess()
                    else onFailure("HTTP $responseCode: $responseMessage")
                }
            } catch (e: Exception) {
                android.util.Log.e("ONESIGNAL", "Exception", e)
                withContext(Dispatchers.Main) { onFailure(e.message ?: "Unknown error") }
            }"""
    content = content.replace(old_catch, new_catch)

with open("app/src/main/java/com/shuaib/classmate/utils/NotificationSender.kt", "w") as f:
    f.write(content)
