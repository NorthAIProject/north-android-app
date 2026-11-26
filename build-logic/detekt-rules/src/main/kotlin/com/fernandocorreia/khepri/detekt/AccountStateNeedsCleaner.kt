package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * A `@Singleton` that holds a `MutableStateFlow` or a `mutableMapOf` outlives the account that filled it, so it must
 * implement `SignOutCleaner`, or carry `@Suppress("AccountStateNeedsCleaner")` with a comment saying why the state is
 * not account-bound. Matches by name, without type resolution, which the `detekt` task does not run.
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
            .filter { it.initializer.holdsState() }
            .forEach {
                report(
                    Finding(
                        Entity.from(it),
                        "`${it.name}` is state in a @Singleton: implement $CLEANER, or suppress with a reason.",
                    )
                )
            }
    }

    private fun KtExpression?.holdsState(): Boolean {
        val call = this as? KtCallExpression ?: (this as? KtDotQualifiedExpression)?.selectorExpression
        val callee = (call as? KtCallExpression)?.calleeExpression as? KtNameReferenceExpression
        return callee?.getReferencedName() in HOLDERS
    }

    private companion object {
        const val CLEANER = "SignOutCleaner"
        val HOLDERS = setOf("MutableStateFlow", "mutableMapOf")
    }
}
