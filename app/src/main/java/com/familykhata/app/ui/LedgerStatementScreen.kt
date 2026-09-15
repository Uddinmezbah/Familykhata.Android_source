package com.familykhata.app.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.familykhata.app.FamilyKhataViewModel
import com.familykhata.app.report.StatementPdf
import com.familykhata.app.report.statementDayStart
import com.familykhata.app.report.statementNextDay
import com.familykhata.app.report.writeLedgerStatementPdf
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LedgerStatementScreen(
    personId: Long,
    personName: String,
    workspace: String,
    viewModel: FamilyKhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var start by rememberSaveable(personId) { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        timeInMillis = statementDayStart(timeInMillis)
    }.timeInMillis) }
    var end by rememberSaveable(personId) { mutableStateOf(statementDayStart(System.currentTimeMillis())) }
    var pdf by remember(personId) { mutableStateOf<StatementPdf?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Keep the exact previewed file during the document picker, including activity recreation.
    var pendingSavePath by rememberSaveable { mutableStateOf<String?>(null) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val path = pendingSavePath
        pendingSavePath = null
        if (uri != null && path != null) scope.launch {
            busy = true
            error = null
            try {
                withContext(Dispatchers.IO) {
                    File(path).inputStream().use { input ->
                        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { output -> input.copyTo(output) }
                    }
                }
                Toast.makeText(context, v15Text("PDF সেভ হয়েছে", "PDF saved"), Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                error = v15Text("PDF সেভ করা যায়নি। আবার চেষ্টা করুন।", "Unable to save PDF. Please try again.")
            } finally { busy = false }
        }
    }
    val goBack = { if (pdf != null) { pdf = null; error = null } else onBack() }
    BackHandler { goBack() }
    V15DeepScreenContainer(title = v15Text("PDF হিসাব বিবরণী", "PDF ledger statement"), onBack = goBack) {
        val generated = pdf
        if (generated == null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(personName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(v15Text("তারিখ বেছে নিন। আগের হিসাবসহ এই সময়ের লেনদেন PDF-এ দেখাবে।", "Choose dates to include the opening balance and this period's transactions."))
                StatementDateButton(v15Text("শুরুর তারিখ", "Start date"), start, !busy) { start = it; error = null }
                StatementDateButton(v15Text("শেষের তারিখ", "End date"), end, !busy) { end = it; error = null }
                Text(v15Text("শেষের তারিখের পুরো দিন অন্তর্ভুক্ত হবে।", "The entire end date is included."), style = MaterialTheme.typography.bodySmall)
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    if (start > end) {
                        error = v15Text("শেষের তারিখ শুরুর তারিখের আগে হতে পারে না।", "End date cannot be before start date.")
                    } else {
                        busy = true
                        error = null
                        val bangla = V15LanguageState.isBangla()
                        val currency = V14DisplayState.currencySymbol
                        scope.launch {
                            try {
                                val snapshot = viewModel.loadLedgerStatement(personId, workspace, start, statementNextDay(end))
                                pdf = withContext(Dispatchers.IO) {
                                    writeLedgerStatementPdf(context.applicationContext, snapshot, bangla, currency)
                                }
                            } catch (cancelled: CancellationException) { throw cancelled
                            } catch (_: Exception) {
                                error = v15Text("বিবরণী তৈরি করা যায়নি। খাতাটি খুলে আবার চেষ্টা করুন।", "Unable to create statement. Reopen the ledger and try again.")
                            } finally { busy = false }
                        }
                    }
                }) { Text(v15Text("PDF তৈরি ও দেখুন", "Create & preview PDF")) }
                if (busy) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && pendingSavePath == null, modifier = Modifier.weight(1f), onClick = {
                        pendingSavePath = generated.file.absolutePath
                        save.launch("HisabiKhata-ledger-$personId.pdf")
                    }) { Text(v15Text("সেভ", "Save")) }
                    Button(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.statements", generated.file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri("Ledger statement", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, v15Text("PDF শেয়ার করুন", "Share PDF")))
                        } catch (_: Exception) {
                            error = v15Text("শেয়ার খোলা যায়নি। PDF সেভ করে পাঠাতে পারেন।", "Unable to open sharing. Save the PDF to send it.")
                        }
                    }) { Text(v15Text("শেয়ার", "Share")) }
                }
                Text(v15Text("শেয়ার থেকে WhatsApp বা অন্য অ্যাপ বেছে নিন।", "Choose WhatsApp or another app from Share."), style = MaterialTheme.typography.bodySmall)
                if (busy) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                StatementPdfPreview(generated, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatementDateButton(label: String, value: Long, enabled: Boolean, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(enabled = enabled, modifier = Modifier.fillMaxWidth(), onClick = {
        val current = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(context, { _, year, month, day ->
            onChange(Calendar.getInstance().apply { clear(); set(year, month, day) }.timeInMillis)
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
    }) { Text("$label: ${v13Date(value)}") }
}

@Composable
private fun StatementPdfPreview(pdf: StatementPdf, modifier: Modifier) {
    var page by remember(pdf.file) { mutableStateOf(0) }
    var bitmap by remember(pdf.file) { mutableStateOf<Bitmap?>(null) }
    var error by remember(pdf.file) { mutableStateOf(false) }
    var scale by remember(page) { mutableStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }
    LaunchedEffect(pdf.file, page) {
        bitmap = null
        error = false
        try {
            bitmap = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        renderer.openPage(page).use { source ->
                            val width = 1190
                            Bitmap.createBitmap(width, width * source.height / source.width, Bitmap.Config.ARGB_8888).also {
                                it.eraseColor(android.graphics.Color.WHITE)
                                source.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { error = true }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(enabled = page > 0, onClick = { page-- }) { Text("←") }
            Text(v15Text("পৃষ্ঠা ${page + 1} / ${pdf.pageCount}", "Page ${page + 1} / ${pdf.pageCount}"))
            TextButton(enabled = page + 1 < pdf.pageCount, onClick = { page++ }) { Text("→") }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds().pointerInput(page) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 4f)
                val limitX = size.width * (scale - 1f) / 2
                val limitY = size.height * (scale - 1f) / 2
                offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
            }
        }, contentAlignment = Alignment.Center) {
            val rendered = bitmap
            if (rendered != null) Image(rendered.asImageBitmap(), contentDescription = v15Text("হিসাব বিবরণীর প্রিভিউ", "Ledger statement preview"), modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y
            }) else if (error) Text(v15Text("প্রিভিউ খোলা যায়নি। সেভ করে PDF খুলুন।", "Preview unavailable. Save and open the PDF."))
            else CircularProgressIndicator()
        }
        Text(v15Text("দুই আঙুলে বড় করে দেখুন।", "Pinch to zoom."), style = MaterialTheme.typography.bodySmall)
    }
}
