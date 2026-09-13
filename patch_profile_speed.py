import re

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'r') as f:
    content = f.read()

# Fix Glide caching
bad_glide = """        Glide.with(this)
            .load(url)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_default_avatar)
            .into(imageView)"""
good_glide = """        Glide.with(this)
            .load(url)
            .circleCrop()
            .placeholder(R.drawable.ic_default_avatar)
            .into(imageView)"""
content = content.replace(bad_glide, good_glide)

# Fix fetchUserFavorites redundant network call
# First, change fetchUserFavorites signature and logic
bad_fetch_favs = """    private fun fetchUserFavorites() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (_binding == null || !isAdded) return@addOnSuccessListener
                val favoriteNames = doc.get("favoriteSubjects") as? List<String> ?: emptyList()
                val favoritePdfIds = doc.get("favoritePdfIds") as? List<String> ?: emptyList()
                fetchSavedResources(favoriteNames, favoritePdfIds)
            }
    }"""
good_fetch_favs = """    private fun fetchUserFavorites(favoriteNames: List<String>, favoritePdfIds: List<String>) {
        fetchSavedResources(favoriteNames, favoritePdfIds)
    }"""
content = content.replace(bad_fetch_favs, good_fetch_favs)

# Then pass lists from the snapshot
bad_update_call = """        applyRoleBadge(binding.tvRoleBadge, user.role)

        fetchUserFavorites()"""
good_update_call = """        applyRoleBadge(binding.tvRoleBadge, user.role)

        fetchUserFavorites(user.favoriteSubjects, user.favoritePdfIds)"""
content = content.replace(bad_update_call, good_update_call)

# Wait, `User` model has favoriteSubjects and favoritePdfIds! Let's ensure they are populated in fetchUserProfile
bad_parse = """                            oneSignalPlayerId = document.getString("oneSignalPlayerId") ?: ""
                        )"""
good_parse = """                            oneSignalPlayerId = document.getString("oneSignalPlayerId") ?: "",
                            favoriteSubjects = document.get("favoriteSubjects") as? List<String> ?: emptyList(),
                            favoritePdfIds = document.get("favoritePdfIds") as? List<String> ?: emptyList()
                        )"""
content = content.replace(bad_parse, good_parse)

with open('app/src/main/java/com/shuaib/classmate/fragments/ProfileFragment.kt', 'w') as f:
    f.write(content)
