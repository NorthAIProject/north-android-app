package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression

/**
 * Reports every `!!`. detekt's `UnsafeCallOnNullableType` needs type resolution, which the `detekt` task does not run.
 */
class NoNotNullAssertion(config: Config) : Rule(config, "No `!!`: handle the null case.") {
    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)
        if (expression.operationToken == KtTokens.EXCLEXCL) {
            report(Finding(Entity.from(expression), "`!!` is not allowed; handle the null case."))
        }
    }
}
