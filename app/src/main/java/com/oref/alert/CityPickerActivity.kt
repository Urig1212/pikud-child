package com.oref.alert

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CityPickerActivity : FragmentActivity() {

    private lateinit var adapter: CityAdapter
    private val apiClient = OrefApiClient()
    private var allCities = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_picker)

        val etSearch  = findViewById<EditText>(R.id.et_search)
        val rvCities  = findViewById<RecyclerView>(R.id.rv_cities)
        val tvCurrent = findViewById<TextView>(R.id.tv_current_city)
        val progress  = findViewById<ProgressBar>(R.id.progress)

        tvCurrent.text = "ישוב נוכחי: ${Prefs.homeCity(this)}"

        adapter = CityAdapter { city ->
            Prefs.setHomeCity(this, city)
            tvCurrent.text = "ישוב נוכחי: $city"
            finish()
        }
        rvCities.layoutManager = LinearLayoutManager(this)
        rvCities.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                adapter.setData(if (q.isEmpty()) allCities else allCities.filter { it.contains(q) })
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        // Load cities from API in background
        CoroutineScope(Dispatchers.IO).launch {
            val cities = apiClient.getCities()
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                allCities = cities.ifEmpty { FALLBACK_CITIES }
                adapter.setData(allCities)
            }
        }
    }

    // Fallback list if API unreachable
    companion object {
        val FALLBACK_CITIES = listOf(
            "אבו גוש","אור יהודה","אור עקיבא","אילת","אלעד","אריאל","אשדוד","אשקלון",
            "באר שבע","בית שאן","בית שמש","בני ברק","בת ים","גבעת שמואל","גבעתיים",
            "דימונה","הוד השרון","הרצליה","חדרה","חולון","חיפה","טבריה","טירה","טירת כרמל",
            "יבנה","יהוד מונוסון","ירושלים","כפר סבא","כפר שמריהו","לוד","מודיעין",
            "מודיעין עילית","מעלה אדומים","מעלות תרשיחא","נהריה","נס ציונה","נצרת",
            "נצרת עלית","נתיבות","נתניה","עכו","עפולה","פתח תקווה","צפת","קרית אונו",
            "קרית אתא","קרית ביאליק","קרית גת","קרית ים","קרית מלאכי","קרית מוצקין",
            "קרית שמונה","ראש העין","ראשון לציון","רהט","רחובות","רמות השבים",
            "רמלה","רמת גן","רמת השרון","רעננה","שדרות","תל אביב","תל אביב - מזרח",
            "תל אביב - צפון","תל אביב - דרום"
        ).sorted()
    }
}

class CityAdapter(private val onSelect: (String) -> Unit) :
    RecyclerView.Adapter<CityAdapter.VH>() {

    private var items = listOf<String>()

    fun setData(list: List<String>) { items = list; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onSelect)
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = view.findViewById<TextView>(R.id.tv_city_name)
        fun bind(city: String, onSelect: (String) -> Unit) {
            tv.text = city
            itemView.setOnClickListener { onSelect(city) }
        }
    }
}
