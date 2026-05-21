package com.opendroid.ai.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.opendroid.ai.accessibility.OpenDroidAccessibilityService
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationActions @Inject constructor() {

    fun getActions(): List<Action> = listOf(
        SendWhatsAppAction(),
        MakeCallAction(),
        SendSmsAction(),
        SendEmailAction()
    )

    private class SendWhatsAppAction : Action {
        override val name: String = "SEND_WHATSAPP"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val contact = params["contact"] ?: return ActionResult(false, null, "contact is missing")
            val message = params["message"] ?: return ActionResult(false, null, "message is missing")
            
            // Try to resolve phone number if nickname is provided
            var phone = contact.replace(" ", "").replace("-", "")
            if (!phone.startsWith("+") && phone.all { it.isDigit() }) {
                // assume some default code or raw
            }

            return try {
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg")
                val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                // Accessibility Automation Fallback: 
                // Wait for the window to load and trigger the accessibility automator to click "send" button
                val service = OpenDroidAccessibilityService.getInstance()
                if (service != null) {
                    kotlinx.coroutines.delay(2000) // wait for WhatsApp chat to open
                    // Try to click send button using text or content descriptions
                    val clicked = service.findAndClick("Send") || 
                                  service.findAndClick("send") || 
                                  service.findAndClick("SEND")
                    if (clicked) {
                        return ActionResult(true, "WhatsApp sent automatically via Accessibility service.", null)
                    }
                }
                
                ActionResult(true, "WhatsApp deep link opened. Accessibility service was not active to auto-click send.", null)
            } catch (e: Exception) {
                // Fallback to sending standard SMS if WhatsApp is not installed
                ActionResult(false, "WhatsApp not installed or failed. Triggering SMS fallback.", e.localizedMessage, true)
            }
        }
    }

    private class MakeCallAction : Action {
        override val name: String = "MAKE_CALL"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val contact = params["contact"] ?: return ActionResult(false, null, "contact parameter missing")
            return try {
                val callUri = Uri.parse("tel:$contact")
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Intent.ACTION_CALL, callUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "Direct call placed to $contact", null)
                } else {
                    // Fallback to DIAL if CALL permission is missing
                    val intent = Intent(Intent.ACTION_DIAL, callUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "CALL_PHONE permission missing. Opened dialer to $contact as fallback.", null, true)
                }
            } catch (e: Exception) {
                ActionResult(false, null, "Call failed: ${e.localizedMessage}")
            }
        }
    }

    private class SendSmsAction : Action {
        override val name: String = "SEND_SMS"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val contact = params["contact"] ?: return ActionResult(false, null, "contact parameter missing")
            val message = params["message"] ?: return ActionResult(false, null, "message parameter missing")
            return try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    val smsManager = context.getSystemService(SmsManager::class.java)
                    smsManager.sendTextMessage(contact, null, message, null, null)
                    ActionResult(true, "SMS sent to $contact", null)
                } else {
                    // Fallback to SMS compose intent
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$contact")
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ActionResult(true, "SEND_SMS permission missing. Opened SMS editor as fallback.", null, true)
                }
            } catch (e: Exception) {
                ActionResult(false, null, "SMS failed: ${e.localizedMessage}")
            }
        }
    }

    private class SendEmailAction : Action {
        override val name: String = "SEND_EMAIL"
        override suspend fun execute(params: Map<String, String>, context: Context): ActionResult {
            val to = params["to"] ?: return ActionResult(false, null, "to email is missing")
            val subject = params["subject"] ?: ""
            val body = params["body"] ?: ""
            return try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Email compose intent fired to $to", null)
            } catch (e: Exception) {
                ActionResult(false, null, "Email failed: ${e.localizedMessage}")
            }
        }
    }
}
