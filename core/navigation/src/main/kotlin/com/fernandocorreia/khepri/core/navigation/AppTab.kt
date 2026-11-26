package com.fernandocorreia.khepri.core.navigation

import kotlinx.serialization.Serializable

/** The five tabs. The web's sidebar has more sections than a tab bar should hold, so the rest live under More. */
@Serializable
public enum class AppTab {
    Today,
    Coach,
    Training,
    Progress,
    More,
}
