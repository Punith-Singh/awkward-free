package com.punith.awkwardfree.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.punith.awkwardfree.MainActivity
import com.punith.awkwardfree.R
import com.punith.awkwardfree.data.Store
import com.punith.awkwardfree.util.money
import java.math.BigDecimal

class BalanceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val uid = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance().currentUser?.uid
        val open = Store(context, uid).read().filter { !it.settled && !it.owedByMe }
        val amount = open.fold(BigDecimal.ZERO) { sum, due -> sum + due.remaining() }
        val openApp = PendingIntent.getActivity(context, 2100, Intent(context, MainActivity::class.java).setAction("com.punith.awkwardfree.action.OPEN_WIDGET"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val add = PendingIntent.getActivity(context, 2101, Intent(context, MainActivity::class.java).setAction("com.punith.awkwardfree.action.QUICK_ADD"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        for (id in ids) {
            val view = RemoteViews(context.packageName, R.layout.widget_balance)
            view.setTextViewText(R.id.widget_amount, money(amount.toPlainString()))
            view.setTextViewText(R.id.widget_open_count, "${open.size} open ${if (open.size == 1) "tab" else "tabs"}")
            view.setOnClickPendingIntent(R.id.widget_amount, openApp)
            view.setOnClickPendingIntent(R.id.widget_add, add)
            manager.updateAppWidget(id, view)
        }
    }
}

internal fun updateBalanceWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, BalanceWidget::class.java))
    if (ids.isNotEmpty()) BalanceWidget().onUpdate(context, manager, ids)
}
