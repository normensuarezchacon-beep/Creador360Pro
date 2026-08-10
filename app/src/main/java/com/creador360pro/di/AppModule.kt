package com.creador360pro.di

import com.creador360pro.data.db.AppDatabase
import com.creador360pro.data.dao.*
import com.creador360pro.ui.ganancias.IncomeViewModel
import com.creador360pro.ui.ideas.IdeasViewModel
import com.creador360pro.ui.planificar.CalendarViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Base de datos
    single { AppDatabase.getInstance(get()) }

    // DAOs
    single { get<AppDatabase>().ideaDao() }
    single { get<AppDatabase>().calendarDao() }
    single { get<AppDatabase>().incomeDao() }
    single { get<AppDatabase>().contactDao() }
    single { get<AppDatabase>().collaborationDao() }
    single { get<AppDatabase>().designProjectDao() }
    single { get<AppDatabase>().videoProjectDao() }

    // ViewModels
    viewModel { IncomeViewModel(get()) }
    viewModel { IdeasViewModel(get()) }
    viewModel { CalendarViewModel(get()) }
}
