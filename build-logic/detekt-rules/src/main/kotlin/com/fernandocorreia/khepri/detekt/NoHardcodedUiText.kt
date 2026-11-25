package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Reports a string literal shown to the user: the first argument or `text =` of `Text`, and any `contentDescription =`
 * or `label =` argument. Lint's `HardcodedText` covers XML only. `@Preview` functions are exempt; test sources are
 * excluded in the detekt configuration.
 */
class NoHardcodedUiText(config: Config) :
    Rule(config, "User-visible text must come from string resources, in all four locales.") {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        expression.valueArguments.forEachIndexed { index, argument ->
            if (argument.isUiText(callee, index) && argument.getArgumentExpression() is KtStringTemplateExpression) {
                if (!expression.isInPreview()) {
                    report(Finding(Entity.from(argument), "Hard-coded UI text in $callee; use stringResource."))
                }
            }
        }
    }

    private fun KtValueArgument.isUiText(callee: String, index: Int): Boolean {
        val name = getArgumentName()?.asName?.asString()
        return when {
            name == "contentDescription" || name == "label" -> true
            callee == "Text" -> name == "text" || (name == null && index == 0)
            else -> false
        }
    }

    private fun KtCallExpression.isInPreview(): Boolean {
        var function = getParentOfType<KtNamedFunction>(strict = true)
        while (function != null) {
            if (function.annotationEntries.any { it.shortName?.asString()?.endsWith("Preview") == true }) return true
            function = function.getParentOfType<KtNamedFunction>(strict = true)
        }
        return false
    }
}
