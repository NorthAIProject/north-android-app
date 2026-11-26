package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

/**
 * A `@Singleton` that holds account state outlives the account that filled it, so it must implement `SignOutCleaner`,
 * or carry `@Suppress("AccountStateNeedsCleaner")` with a comment saying why the state is not account-bound. State is a
 * `var`, a property typed `MutableStateFlow` or `MutableMap`, or one whose initializer or delegate (`by lazy`) calls
 * `MutableStateFlow(` or `mutableMapOf(`. Matches by name, without type resolution, which the `detekt` task does not
 * run.
 */
class AccountStateNeedsCleaner(config: Config) :
    Rule(config, "Account-bound state in a singleton must be cleared at sign-out: implement SignOutCleaner.") {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (klass.annotationEntries.none { it.shortName?.asString() == "Singleton" }) return
        if (klass.superTypeListEntries.any { it.typeReference?.text?.substringAfterLast('.') == CLEANER }) return
        klass.body
            ?.properties
            .orEmpty()
            .filter { it.holdsState() }
            .forEach {
                report(
                    Finding(
                        Entity.from(it),
                        "`${it.name}` is state in a @Singleton: implement $CLEANER, or suppress with a reason.",
                    )
                )
            }
    }

    private fun KtProperty.holdsState(): Boolean =
        isVar ||
            typeReference?.text?.let { type -> TYPES.any { it in type } } == true ||
            initializer.callsHolder() ||
            delegateExpression.callsHolder()

    private fun KtElement?.callsHolder(): Boolean {
        if (this == null) return false
        return (this as? KtCallExpression).isHolder() || anyDescendantOfType<KtCallExpression> { it.isHolder() }
    }

    private fun KtCallExpression?.isHolder(): Boolean =
        (this?.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() in HOLDERS

    private companion object {
        const val CLEANER = "SignOutCleaner"
        val HOLDERS = setOf("MutableStateFlow", "mutableMapOf")
        val TYPES = listOf("MutableStateFlow", "MutableMap")
    }
}
