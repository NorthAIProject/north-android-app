package com.fernandocorreia.khepri.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class KhepriRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("khepri")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            mapOf(
                RuleName("NoHardcodedUiText") to ::NoHardcodedUiText,
                RuleName("NoNotNullAssertion") to ::NoNotNullAssertion,
            ),
        )
}
