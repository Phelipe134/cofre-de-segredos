package com.cofredesegredos

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cofredesegredos.ui.MainActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.Matchers.allOf

@RunWith(AndroidJUnit4::class)
class Sprint41FlowInstrumentedTest {
    @Before
    fun clearAppState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteSharedPreferences("cofre_segredos_secure_store")
        context.deleteSharedPreferences("cofre_segredos_biometric_store")
        context.deleteSharedPreferences("cofre_credential_activity")
    }

    @Test
    fun masterPasswordCreationLoginAndPasswordFallbackWork() {
        ActivityScenario.launch(MainActivity::class.java).use {
            createMasterPassword()
            onView(withText("Credenciais")).check(matches(isDisplayed()))
            onView(withId(R.id.settings_open_button)).perform(click())
            onView(withId(R.id.lock_button)).perform(click())
            onView(withId(R.id.auth_password_input)).perform(replaceText(PASSWORD), closeSoftKeyboard())
            onView(withId(R.id.auth_submit_button)).perform(click())
            onView(withId(R.id.main_search_input)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun createAndEditVaultWork() {
        ActivityScenario.launch(MainActivity::class.java).use {
            createMasterPassword()
            createVault("Banco", "contas")
            onView(withText("Banco")).check(matches(isDisplayed()))
            onView(withId(R.id.vault_open_button)).perform(longClick())
            onView(withText("Editar categoria")).perform(click())
            onView(withId(R.id.vault_name_input)).perform(replaceText("Banco Principal"), closeSoftKeyboard())
            onView(withId(R.id.vault_description_input)).perform(replaceText("cartoes e contas"), closeSoftKeyboard())
            onView(withText("Salvar")).perform(click())
            onView(withText("Banco Principal")).check(matches(isDisplayed()))
            onView(withText("cartoes e contas")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun createEditSearchAndRevealSecretWork() {
        ActivityScenario.launch(MainActivity::class.java).use {
            createMasterPassword()
            createVault("Trabalho", "empresa")
            onView(withId(R.id.vault_open_button)).perform(click())
            createSecret("Email", "login1", "segredo123", "observacao azul", "token", "111")
            createSecret("VPN", "login2", "vpnpass", "rede interna", "pin", "222")

            onView(withId(R.id.vault_search_input)).perform(replaceText("login2"), closeSoftKeyboard())
            onView(withText("VPN")).check(matches(isDisplayed()))
            onView(withText("Email")).check(doesNotExist())

            onView(withId(R.id.vault_search_input)).perform(replaceText("azul"), closeSoftKeyboard())
            onView(withText("Email")).check(matches(isDisplayed()))
            onView(withId(R.id.secret_open_button)).perform(click())
            onView(withId(R.id.secret_more_button)).perform(click())
            onView(withText("Editar credencial")).perform(click())
            onView(withId(R.id.secret_title_input)).perform(replaceText("Email Corporativo"), closeSoftKeyboard())
            onView(withId(R.id.secret_password_input)).perform(replaceText("novo123"), closeSoftKeyboard())
            onView(withText("Salvar")).perform(click())
            onView(withText("Email Corporativo")).check(matches(isDisplayed()))

            onView(
                allOf(
                    withId(R.id.secret_open_button),
                    hasDescendant(withText("Email Corporativo"))
                )
            ).perform(click())
            onView(withId(R.id.secret_password_value)).check(matches(withText("********")))
            onView(withId(R.id.secret_password_toggle)).perform(click())
            onView(withId(R.id.secret_password_value)).check(matches(withText("novo123")))
            onView(withId(R.id.secret_password_toggle)).perform(click())
            onView(withId(R.id.secret_password_value)).check(matches(withText("********")))
        }
    }

    private fun createMasterPassword() {
        onView(withId(R.id.auth_password_input)).perform(replaceText(PASSWORD), closeSoftKeyboard())
        onView(withId(R.id.auth_confirm_input)).perform(replaceText(PASSWORD), closeSoftKeyboard())
        onView(withId(R.id.auth_submit_button)).perform(click())
    }

    private fun createVault(name: String, description: String) {
        onView(withId(R.id.settings_open_button)).perform(click())
        onView(withId(R.id.categories_button)).perform(click())
        onView(withId(R.id.new_vault_button)).perform(click())
        onView(withId(R.id.vault_name_input)).perform(replaceText(name), closeSoftKeyboard())
        onView(withId(R.id.vault_description_input)).perform(replaceText(description), closeSoftKeyboard())
        onView(withText("Salvar")).perform(click())
        onView(withId(R.id.settings_open_button)).perform(click())
        onView(withId(R.id.categories_button)).perform(click())
    }

    private fun createSecret(
        title: String,
        login: String,
        password: String,
        notes: String,
        customLabel: String,
        customValue: String
    ) {
        onView(withId(R.id.new_secret_button)).perform(click())
        onView(withId(R.id.secret_title_input)).perform(replaceText(title), closeSoftKeyboard())
        onView(withId(R.id.secret_login_input)).perform(replaceText(login), closeSoftKeyboard())
        onView(withId(R.id.secret_password_input)).perform(replaceText(password), closeSoftKeyboard())
        onView(withText("Adicionar informações opcionais")).perform(click())
        onView(withId(R.id.secret_notes_input)).perform(replaceText(notes), closeSoftKeyboard())
        onView(withId(R.id.secret_custom_label_input)).perform(replaceText(customLabel), closeSoftKeyboard())
        onView(withId(R.id.secret_custom_value_input)).perform(replaceText(customValue), closeSoftKeyboard())
        onView(withText("Salvar")).perform(click())
    }

    companion object {
        private const val PASSWORD = "SenhaMestre123"
    }
}
