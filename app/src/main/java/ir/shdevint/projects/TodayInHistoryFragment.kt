package ir.shdevint.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import ir.shdevint.projects.databinding.FragmentTodayInHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodayInHistoryFragment : Fragment() {

    private var _binding: FragmentTodayInHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayInHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendar = Calendar.getInstance()
        val gregorianDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // Display today's date in different formats
        displayTodaysDate(gregorianDate)

        binding.eventTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.worldEventsRadioButton.id -> loadWebView("https://www.onthisday.com/", gregorianDate)
                binding.iranEventsRadioButton.id -> loadWebView("https://rasekhoon.net/calender/", gregorianDate)
            }
        }
    }

    private fun displayTodaysDate(gregorianDate: String) {
        loadWebView("https://rasekhoon.net/calender/", gregorianDate)
    }

    private fun loadWebView(url: String, gregorianDate: String) {
        binding.webView.settings.javaScriptEnabled = true // Ensure JavaScript is enabled
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Add JavaScript interface
                binding.webView.addJavascriptInterface(object : Any() {
                    @android.webkit.JavascriptInterface
                    fun passPersianDate(date: String) {
                        // This method should receive the Persian date from JavaScript
                        val persianDate = date
                        val dateText = "تاریخ شمسی: $persianDate - Gregorian Date: $gregorianDate"
                        binding.dateTextView.text = dateText
                    }
                }, "JSInterface")

                // Execute your JavaScript here after the page has loaded
                binding.webView.evaluateJavascript("const options = { year: 'numeric', month: 'long', day: 'numeric' }; const persianDate = new Date().toLocaleDateString('fa-IR', options); JSInterface.passPersianDate(persianDate);", null)
            }
        }
        binding.webView.loadUrl(url) // Load the desired URL
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}