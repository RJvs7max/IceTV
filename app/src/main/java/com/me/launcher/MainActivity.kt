package com.me.launcher


import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


object Config {
    const val COLUMNS = 6
    const val FOCUS_SCALE = 1.12f
    val HIDDEN = setOf("com.me.launcher")
    val PINNED = listOf<String>()
}


data class AppItem(
    val label: String,
    val pkg: String,
    val icon: Drawable,
    val launchIntent: Intent
)


class MainActivity : Activity() {
    private lateinit var grid: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        grid = findViewById(R.id.grid)
        grid.layoutManager = GridLayoutManager(this, Config.COLUMNS)
        grid.setHasFixedSize(true)


        refreshApps()
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        grid.scrollToPosition(0)
        refreshApps()
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }


    private fun refreshApps() {
        Thread {
            val apps = loadApps()
            runOnUiThread {
                grid.adapter = AppAdapter(apps) { app ->
                    try {
                        startActivity(app.launchIntent)
                    } catch (_: Exception) {
                    }
                }
                grid.post {
                    grid.getChildAt(0)?.requestFocus()
                }
            }
        }.start()
    }


    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val out = LinkedHashMap<String, AppItem>()


        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg in Config.HIDDEN || out.containsKey(pkg)) continue


                val launch = Intent(Intent.ACTION_MAIN)
                    .addCategory(cat)
                    .setClassName(pkg, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)


                val drawable = ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)
                out[pkg] = AppItem(
                    label = ri.loadLabel(pm).toString(),
                    pkg = pkg,
                    icon = drawable,
                    launchIntent = launch
                )
            }
        }


        return out.values.sortedWith(
            compareBy(
                { val idx = Config.PINNED.indexOf(it.pkg); if (idx == -1) Int.MAX_VALUE else idx },
                { it.label.lowercase() }
            )
        )
    }
}


class AppAdapter(
    private val items: List<AppItem>,
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {


    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return VH(view)
    }


    override fun getItemCount() = items.size


    override fun onBindViewHolder(h: VH, position: Int) {
        val app = items[position]
        h.icon.setImageDrawable(app.icon)
        h.label.text = app.label
        h.itemView.setOnClickListener { onClick(app) }
        h.itemView.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) Config.FOCUS_SCALE else 1f
            v.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(120)
                .start()
        }
    }
}
