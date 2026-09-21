package com.familykhata.app.report

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class ThermalPrinterDevice(
    val name: String,
    val address: String
)

private val THERMAL_SPP_UUID:
    UUID =
    UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    )

@SuppressLint("MissingPermission")
fun pairedThermalPrinters(
    context: Context
): List<ThermalPrinterDevice> {
    val manager =
        context.getSystemService(
            BluetoothManager::class.java
        ) ?: return emptyList()

    val adapter =
        manager.adapter
            ?: return emptyList()

    if (!adapter.isEnabled) {
        return emptyList()
    }

    return adapter.bondedDevices
        .map { device ->
            ThermalPrinterDevice(
                name =
                    device.name
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: device.address,
                address =
                    device.address
            )
        }
        .sortedBy {
            it.name.lowercase()
        }
}

@SuppressLint("MissingPermission")
suspend fun printThermalTest(
    context: Context,
    address: String,
    paperWidthMm: Int
) {
    withContext(Dispatchers.IO) {
        require(
            paperWidthMm == 58 ||
                paperWidthMm == 80
        ) {
            "Unsupported paper width"
        }

        val manager =
            requireNotNull(
                context.getSystemService(
                    BluetoothManager::class.java
                )
            ) {
                "Bluetooth unavailable"
            }

        val adapter =
            requireNotNull(
                manager.adapter
            ) {
                "Bluetooth unavailable"
            }

        require(adapter.isEnabled) {
            "Bluetooth is turned off"
        }

        val device =
            adapter.getRemoteDevice(
                address
            )

        val socket =
            device.createRfcommSocketToServiceRecord(
                THERMAL_SPP_UUID
            )

        try {
            socket.connect()

            val output =
                socket.outputStream

            val columns =
                if (paperWidthMm == 80) {
                    48
                } else {
                    32
                }

            val separator =
                "-".repeat(columns)

            output.write(
                byteArrayOf(
                    0x1B,
                    0x40
                )
            )

            output.write(
                byteArrayOf(
                    0x1B,
                    0x61,
                    0x01
                )
            )

            output.write(
                byteArrayOf(
                    0x1B,
                    0x45,
                    0x01
                )
            )

            output.write(
                "HISABI KHATA\n"
                    .toByteArray(
                        Charsets.US_ASCII
                    )
            )

            output.write(
                byteArrayOf(
                    0x1B,
                    0x45,
                    0x00
                )
            )

            output.write(
                byteArrayOf(
                    0x1B,
                    0x61,
                    0x00
                )
            )

            val receipt =
                buildString {
                    appendLine(separator)
                    appendLine(
                        "Bluetooth Thermal Printer"
                    )
                    appendLine(
                        "Paper: ${paperWidthMm}mm"
                    )
                    appendLine(separator)
                    appendLine(
                        "Connection: OK"
                    )
                    appendLine(
                        "ESC/POS test: OK"
                    )
                    appendLine(separator)
                    appendLine(
                        "Hisabi Khata"
                    )
                    appendLine()
                    appendLine()
                    appendLine()
                }

            output.write(
                receipt.toByteArray(
                    Charsets.US_ASCII
                )
            )

            output.flush()
        } finally {
            runCatching {
                socket.close()
            }
        }
    }
}