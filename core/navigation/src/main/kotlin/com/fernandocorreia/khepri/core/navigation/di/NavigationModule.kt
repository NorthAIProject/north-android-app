package com.fernandocorreia.khepri.core.navigation.di

import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.navigation.NavigationSignOutCleaner
import com.fernandocorreia.khepri.core.navigation.Navigator
import com.fernandocorreia.khepri.core.navigation.QueuedNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NavigationModule {
    @Binds abstract fun navigator(navigator: QueuedNavigator): Navigator

    @Binds @IntoSet abstract fun signOutCleaner(cleaner: NavigationSignOutCleaner): SignOutCleaner
}
