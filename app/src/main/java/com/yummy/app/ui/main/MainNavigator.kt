package com.yummy.app.ui.main

/** Lo implementa MainActivity; los fragments lo usan para navegar sin acoplarse a ella. */
interface MainNavigator {
    fun navigateTo(tab: Tab)
    fun openAddMenu()
    fun logout()
}