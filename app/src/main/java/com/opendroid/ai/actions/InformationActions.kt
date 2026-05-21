package com.opendroid.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InformationActions @Inject constructor() {

    fun getActions(): List<Action> = listOf(
        WebSearchAction(),
        GetWeatherAction(),
        GetNewsAction(),
        CalculateAction(),
        TranslateAction(),
        DefineWordAction(),
        ConvertUnitsAction(),
        CurrencyConvertAction(),
        CheckStockAction(),
        SummarizeUrlAction(),
        FactCheckAction()
    )

    private class WebSearchAction : Action {
        override val name: String = "WEB_SEARCH"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val query = params["query"] ?: return ActionResult(false, null, "query parameter is missing")
            return try {
                val encQuery = URLEncoder.encode(query, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Searched the web for: '$query'", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Search failed: ${e.localizedMessage}")
            }
        }
    }

    private class GetWeatherAction : Action {
        override val name: String = "GET_WEATHER"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val location = params["location"] ?: "current location"
            return try {
                val query = URLEncoder.encode("weather in $location", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened weather info for: $location", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to get weather: ${e.localizedMessage}")
            }
        }
    }

    private class GetNewsAction : Action {
        override val name: String = "GET_NEWS"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val topic = params["topic"] ?: "latest news"
            return try {
                val query = URLEncoder.encode("news $topic", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened Google News search for: $topic", null)
            } catch (e: Exception) {
                ActionResult(false, null, "News query failed: ${e.localizedMessage}")
            }
        }
    }

    private class CalculateAction : Action {
        override val name: String = "CALCULATE"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val expression = params["expression"] ?: return ActionResult(false, null, "expression parameter is missing")
            return try {
                val sanitized = expression.replace(" ", "")
                val result = evaluateSimpleExpression(sanitized)
                ActionResult(true, "Result of $expression is $result", null)
            } catch (e: Exception) {
                // Fallback: Web search calculation
                val encExpr = URLEncoder.encode(expression, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encExpr")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(false, "Could not evaluate locally. Opened Google search as fallback.", e.localizedMessage, true)
            }
        }

        private fun evaluateSimpleExpression(expr: String): Double {
            // Very basic evaluator for +, -, *, /
            return when {
                expr.contains("+") -> {
                    val parts = expr.split("+")
                    parts[0].toDouble() + parts[1].toDouble()
                }
                expr.contains("-") -> {
                    val parts = expr.split("-")
                    parts[0].toDouble() - parts[1].toDouble()
                }
                expr.contains("*") -> {
                    val parts = expr.split("*")
                    parts[0].toDouble() * parts[1].toDouble()
                }
                expr.contains("/") -> {
                    val parts = expr.split("/")
                    parts[0].toDouble() / parts[1].toDouble()
                }
                else -> expr.toDouble()
            }
        }
    }

    private class TranslateAction : Action {
        override val name: String = "TRANSLATE"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val text = params["text"] ?: return ActionResult(false, null, "text is missing")
            val from = params["from"] ?: "auto"
            val to = params["to"] ?: "en"
            return try {
                val encText = URLEncoder.encode(text, "UTF-8")
                val uri = Uri.parse("https://translate.google.com/?sl=$from&tl=$to&text=$encText&op=translate")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened Google Translate from $from to $to", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Translation failed: ${e.localizedMessage}")
            }
        }
    }

    private class DefineWordAction : Action {
        override val name: String = "DEFINE_WORD"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val word = params["word"] ?: return ActionResult(false, null, "word parameter is missing")
            return try {
                val query = URLEncoder.encode("define $word", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Searched definition for word: $word", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to define word: ${e.localizedMessage}")
            }
        }
    }

    private class ConvertUnitsAction : Action {
        override val name: String = "CONVERT_UNITS"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val value = params["value"] ?: return ActionResult(false, null, "value is missing")
            val from = params["from"] ?: ""
            val to = params["to"] ?: ""
            return try {
                val query = URLEncoder.encode("convert $value $from to $to", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened unit converter for $value $from to $to", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to convert units: ${e.localizedMessage}")
            }
        }
    }

    private class CurrencyConvertAction : Action {
        override val name: String = "CURRENCY_CONVERT"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val amount = params["amount"] ?: return ActionResult(false, null, "amount is missing")
            val from = params["from"] ?: ""
            val to = params["to"] ?: ""
            return try {
                val query = URLEncoder.encode("convert $amount $from to $to", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened currency converter for $amount $from to $to", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to convert currency: ${e.localizedMessage}")
            }
        }
    }

    private class CheckStockAction : Action {
        override val name: String = "CHECK_STOCK"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val symbol = params["symbol"] ?: return ActionResult(false, null, "symbol is missing")
            return try {
                val query = URLEncoder.encode("stock $symbol", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened stock info for: $symbol", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Stock check failed: ${e.localizedMessage}")
            }
        }
    }

    private class SummarizeUrlAction : Action {
        override val name: String = "SUMMARIZE_URL"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val url = params["url"] ?: return ActionResult(false, null, "url is missing")
            return try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Opened URL in browser: $url. Please read page content.", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Failed to open URL: ${e.localizedMessage}")
            }
        }
    }

    private class FactCheckAction : Action {
        override val name: String = "FACT_CHECK"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val claim = params["claim"] ?: return ActionResult(false, null, "claim is missing")
            return try {
                val query = URLEncoder.encode("fact check $claim", "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Fact check search initiated for: '$claim'", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Fact check query failed: ${e.localizedMessage}")
            }
        }
    }
}
