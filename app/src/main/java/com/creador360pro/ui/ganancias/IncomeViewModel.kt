package com.creador360pro.ui.ganancias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creador360pro.data.dao.IncomeDao
import com.creador360pro.data.model.IncomeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class IncomeViewModel(private val incomeDao: IncomeDao) : ViewModel() {

    val allIncomes: Flow<List<IncomeRecord>> = incomeDao.getAllIncomes()

    fun addIncome(monto: Double, moneda: String, fuente: String) {
        val record = IncomeRecord(
            monto = monto,
            moneda = moneda,
            fuente = fuente,
            fecha = System.currentTimeMillis()
        )
        viewModelScope.launch {
            incomeDao.insertIncome(record)
        }
    }

    fun deleteIncome(record: IncomeRecord) {
        viewModelScope.launch {
            incomeDao.deleteIncome(record)
        }
    }
}
