---
name: kmp-compose-i18n
description: Scans screens, ViewModels, and components to extract hardcoded strings into composeResource string resources for multi-language support (es/en). Use this when the user asks to translate the texts of the application.
---

# KMP Compose Internationalization (i18n)

This skill provides the standard operating procedure for extracting hardcoded text strings from UI components (Compose screens) and business logic (ViewModels) and defining them as localized resources in Kotlin Multiplatform.

## Trigger
Use this skill when the user requests to "translate the texts of the application" or extract hardcoded strings to resources.

## Target Resource Files
When extracting a string, you must define it in both the default (Spanish) and English resource files:
- **Default (Spanish):** `shared/src/commonMain/composeResources/values/strings.xml`
- **English:** `shared/src/commonMain/composeResources/values-en/strings.xml`

## Extraction Workflow
1. **Identify Hardcoded Strings:** Analyze the target Kotlin files (Screens, ViewModels, UI components) to find hardcoded string literals used for user-facing text (titles, descriptions, error messages, button labels).
2. **Duplicate Check:** **CRITICAL:** Before adding a new key, you MUST search both `strings.xml` files for the text value or similar keys. If a key with the same meaning exists, reuse it.
3. **Determine Resource Name & Grouping:** 
   - **Key Naming:** Create a descriptive `lower_snake_case` key. You **MUST** use prefixes based on the screen or category (e.g., `login_title`, `dashboard_balance`).
   - **Conflict Handling:** If you encounter a naming conflict or are unsure how to name a key, **STOP and ask the user** for confirmation, providing at least 2-3 suggestions.
   - **Common Strings:** If a string is highly generic, check the "Common" section first.
   - **XML Organization:** You **MUST** maintain the file structure using XML comments as headers.
4. **Add Resources:** Add definitions to `values/strings.xml` (Spanish) and `values-en/strings.xml` (English).
5. **Replace in Code:**
   - **In Composable Screens:** Use `stringResource(Res.string.key)`. 
     - **CRITICAL:** Ensure the file has the correct imports:
       ```kotlin
       import condominio.shared.generated.resources.*
       import org.jetbrains.compose.resources.stringResource
       ```
   - **In ViewModels / Logic:** Use `UiText.StringResource(Res.string.key)`.
     - **CRITICAL:** Ensure the file has the correct imports:
       ```kotlin
       import condominio.shared.generated.resources.*
       import com.example.condominio.ui.utils.UiText
       ```
6. **Rebuild:** Run a Gradle sync or build to ensure accessors in the `Res` object are generated.

## ViewModel Specific Rules
- Always use `UiText.StringResource` for messages emitted by ViewModels (e.g., state errors, success messages).
- Avoid `UiText.DynamicString` for static text; reserve it only for API error messages that are already localized by the backend or for debugging.
- Cross-reference existing keys in `strings.xml` before creating new ones to avoid duplicates.
- If a key exists but has a different prefix (e.g., `common_save` vs `login_save`), prefer reusing the more generic one or the existing one to maintain consistency.
