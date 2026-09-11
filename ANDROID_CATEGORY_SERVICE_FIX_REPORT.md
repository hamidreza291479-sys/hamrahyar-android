# ANDROID_CATEGORY_SERVICE_FIX_REPORT

## 1. Root Cause
The issue of categories and services not displaying was caused by several factors:
- **Wrong DTO mapping:** `CategoryDto` and `ServiceDto` were using `icon_name` instead of the backend's `icon` column. Mandatory columns like `id` and `name` were correctly mapped, but additional optional columns were missing.
- **Silent Failures:** The repository was catching all exceptions during Supabase queries and returning an `emptyList()`. Any mismatch in the DTO schema caused a decoding error that was hidden from the UI.
- **Missing Filters:** The `is_active` filter was not being applied in the `getCategories` query.
- **Missing Sorting:** The `sort_order` column was ignored, leading to potentially inconsistent ordering or issues if the backend expected it.

## 2. Files Created
- None (Only modified existing files and updated the root report).

## 3. Files Modified
- `app/src/main/java/com/lucifer/hamrahyar/ui/theme/data/model/SupabaseDtos.kt`: Updated `CategoryDto` and `ServiceDto` to match the real backend schema (`icon`, `is_active`, `sort_order`, etc.).
- `app/src/main/java/com/lucifer/hamrahyar/ui/theme/data/repository/OnlineServiceRepositoryImpl.kt`: 
    - Updated queries to include `is_active = true` filter and `sort_order` sorting.
    - Added `android.util.Log` for better debugging of Supabase operations.
    - Fixed mapping logic to use updated DTO fields.
- `app/src/main/java/com/lucifer/hamrahyar/ui/theme/utils/IconMapper.kt`: Updated to use the correct parameter name (`icon`).
- `app/src/main/java/com/lucifer/hamrahyar/ui/theme/home/HomeScreen.kt`: 
    - Added an empty state message ("هیچ خدمتی در حال حاضر فعال نیست.").
    - Removed unused `ServiceRepository` import.
- `app/src/main/java/com/lucifer/hamrahyar/ui/theme/home/CategoryScreen.kt`: 
    - Added an empty state message ("موردی یافت نشد.").
    - Added missing imports for UI components.

## 4. Supabase Queries
- **Categories:** `postgrest["categories"].select { filter { eq("is_active", true) }; order("sort_order", Order.ASCENDING) }`
- **Services:** `postgrest["services"].select { filter { eq("category_id", categoryId); eq("is_active", true) }; order("sort_order", Order.ASCENDING) }`

## 5. Mock Removed
- No additional mocks were removed in this task as they were supposedly removed in Phase 2B, but the repository was updated to ensure no hardcoded data is used and queries are properly filtered.

## 6. Build Result
**SUCCESS** ✅

## 7. Test Result
- **Categories:** Now fetch `is_active` categories sorted by `sort_order`. If decoding fails, details are logged in Logcat.
- **Services:** Corrected `category_id` filtering and schema mapping.
- **Navigation:** Preserved the existing flow (Mobile -> Security -> Priority -> Form -> Chat).

## 8. Remaining Blockers
- **BACKEND DATA BLOCKER:** If categories/services are still not visible, please verify that the `categories` and `services` tables in Supabase contain data with `is_active = true`.
- **RLS/BACKEND BLOCKER:** Ensure that the `anon` role has `SELECT` permission on `categories` and `services` tables.

## 9. Architecture Impact
**Architecture Freeze v1.0 preserved.** 
The layered architecture (UI -> Navigator -> Repository -> Supabase) remains intact. Error handling and logging were improved at the repository level.
