package com.familykhata.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.familykhata.app.agency.AgencyDatabase
import com.familykhata.app.booking.BookingDatabase
import com.familykhata.app.coaching.CoachingDatabase
import com.familykhata.app.data.InventoryDatabase
import com.familykhata.app.membership.MembershipDatabase
import com.familykhata.app.servicejob.ServiceJobDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup/restore bridge for all specialised v1.5 business modules.
 *
 * Rows are exported directly from Room tables so entity fields and IDs
 * are preserved exactly. Restore order is parent -> child.
 *
 * Base inventory_products / inventory_batches are intentionally NOT
 * included here because InventoryBackupBridge already owns them.
 */
object V15BusinessBackupBridge {

    private data class Group(
        val key: String,
        val database: RoomDatabase,
        val tables: List<String>
    )

    private fun groups(
        context: Context
    ): List<Group> =
        listOf(
            Group(
                key = "coaching",
                database =
                    CoachingDatabase.get(context),
                tables = listOf(
                    "coaching_students",
                    "coaching_batches",
                    "coaching_enrollments",
                    "coaching_charges",
                    "coaching_payments"
                )
            ),
            Group(
                key = "agency",
                database =
                    AgencyDatabase.get(context),
                tables = listOf(
                    "agency_clients",
                    "agency_projects",
                    "agency_charges",
                    "agency_payments"
                )
            ),
            Group(
                key = "serviceJob",
                database =
                    ServiceJobDatabase.get(context),
                tables = listOf(
                    "service_customers",
                    "service_jobs",
                    "service_charges",
                    "service_payments"
                )
            ),
            Group(
                key = "membership",
                database =
                    MembershipDatabase.get(context),
                tables = listOf(
                    "membership_members",
                    "membership_plans",
                    "membership_subscriptions",
                    "membership_payments"
                )
            ),
            Group(
                key = "booking",
                database =
                    BookingDatabase.get(context),
                tables = listOf(
                    "booking_customers",
                    "bookings",
                    "booking_charges",
                    "booking_payments"
                )
            ),
            Group(
                key = "inventorySpecialized",
                database =
                    InventoryDatabase.get(context),
                tables = listOf(
                    // Production
                    "production_item_roles",
                    "production_batches",
                    "production_consumptions",
                    "production_costs",

                    // Dealership
                    "dealership_suppliers",
                    "dealership_territories",
                    "dealership_dealers",
                    "dealership_product_policies",
                    "dealership_stock_receipts",
                    "dealership_invoices",
                    "dealership_invoice_lines",
                    "dealership_stock_allocations",
                    "dealership_payments",
                    "dealership_returns",

                    // Dealer Business
                    "dealer_business_companies",
                    "dealer_business_areas",
                    "dealer_business_customers",
                    "dealer_business_purchases",
                    "dealer_business_purchase_lines",
                    "dealer_business_supplier_payments",
                    "dealer_business_supplier_payment_allocations",
                    "dealer_business_sales",
                    "dealer_business_sale_lines",
                    "dealer_business_stock_allocations",
                    "dealer_business_collections",
                    "dealer_business_collection_allocations",
                    "dealer_business_sales_returns",
                    "dealer_business_sales_return_allocations",
                    "dealer_business_purchase_returns",
                    "dealer_business_expenses",

                    // Agro
                    "agro_cycles",
                    "agro_costs",
                    "agro_losses",
                    "agro_harvests",

                    // Restaurant / Catering
                    "food_menu_items",
                    "food_recipe_ingredients",
                    "food_orders",
                    "food_order_lines",
                    "food_stock_allocations",
                    "food_payments"
                )
            )
        )

    suspend fun export(
        context: Context
    ): JSONObject =
        withContext(Dispatchers.IO) {
            JSONObject().apply {
                groups(context).forEach { group ->
                    put(
                        group.key,
                        exportGroup(
                            database =
                                group.database,
                            tables =
                                group.tables
                        )
                    )
                }
            }
        }

    /**
     * json == null means an older backup format.
     * In that case specialised data is cleared so restore behaves
     * like a full replacement rather than mixing old/new datasets.
     *
     * Returns number of restored specialised rows.
     */
    suspend fun restore(
        context: Context,
        json: JSONObject?
    ): Int =
        withContext(Dispatchers.IO) {
            var restoredRows = 0

            groups(context).forEach { group ->
                restoredRows +=
                    restoreGroup(
                        database =
                            group.database,
                        tables =
                            group.tables,
                        json =
                            json?.optJSONObject(
                                group.key
                            )
                    )
            }

            restoredRows
        }

    private fun exportGroup(
        database: RoomDatabase,
        tables: List<String>
    ): JSONObject {
        val output =
            JSONObject()

        val db =
            database
                .openHelper
                .readableDatabase

        tables.forEach { table ->
            val rows =
                JSONArray()

            db.query(
                "SELECT * FROM `$table`"
            ).use { cursor ->

                while (
                    cursor.moveToNext()
                ) {
                    rows.put(
                        cursorRowToJson(
                            cursor
                        )
                    )
                }
            }

            output.put(
                table,
                rows
            )
        }

        return output
    }

    private suspend fun restoreGroup(
        database: RoomDatabase,
        tables: List<String>,
        json: JSONObject?
    ): Int {
        var restoredRows = 0

        database.withTransaction {
            val db =
                database
                    .openHelper
                    .writableDatabase

            /*
             * Child -> parent delete avoids foreign-key conflicts.
             */
            tables
                .asReversed()
                .forEach { table ->
                    db.execSQL(
                        "DELETE FROM `$table`"
                    )
                }

            /*
             * Parent -> child insert preserves original IDs and FKs.
             */
            if (json != null) {
                tables.forEach { table ->
                    val rows =
                        json.optJSONArray(
                            table
                        ) ?: JSONArray()

                    for (
                        index in
                        0 until rows.length()
                    ) {
                        val row =
                            rows.getJSONObject(
                                index
                            )

                        val values =
                            jsonToContentValues(
                                row
                            )

                        val result =
                            db.insert(
                                table,
                                SQLiteDatabase.CONFLICT_ABORT,
                                values
                            )

                        require(
                            result != -1L
                        ) {
                            "Restore failed for table $table"
                        }

                        restoredRows++
                    }
                }
            }
        }

        return restoredRows
    }

    private fun cursorRowToJson(
        cursor: Cursor
    ): JSONObject =
        JSONObject().apply {

            for (
                index in
                0 until cursor.columnCount
            ) {
                val name =
                    cursor.getColumnName(
                        index
                    )

                when (
                    cursor.getType(
                        index
                    )
                ) {
                    Cursor.FIELD_TYPE_NULL ->
                        put(
                            name,
                            JSONObject.NULL
                        )

                    Cursor.FIELD_TYPE_INTEGER ->
                        put(
                            name,
                            cursor.getLong(
                                index
                            )
                        )

                    Cursor.FIELD_TYPE_FLOAT ->
                        put(
                            name,
                            cursor.getDouble(
                                index
                            )
                        )

                    Cursor.FIELD_TYPE_STRING ->
                        put(
                            name,
                            cursor.getString(
                                index
                            )
                        )

                    Cursor.FIELD_TYPE_BLOB ->
                        put(
                            name,
                            JSONObject().apply {
                                put(
                                    "__blob__",
                                    Base64.encodeToString(
                                        cursor.getBlob(
                                            index
                                        ),
                                        Base64.NO_WRAP
                                    )
                                )
                            }
                        )

                    else ->
                        error(
                            "Unsupported SQLite field type"
                        )
                }
            }
        }

    private fun jsonToContentValues(
        json: JSONObject
    ): ContentValues {
        val values =
            ContentValues()

        val keys =
            json.keys()

        while (
            keys.hasNext()
        ) {
            val key =
                keys.next()

            val value =
                json.get(key)

            when {
                value === JSONObject.NULL ->
                    values.putNull(
                        key
                    )

                value is Boolean ->
                    values.put(
                        key,
                        if (value) {
                            1
                        } else {
                            0
                        }
                    )

                value is Float ||
                    value is Double ->
                    values.put(
                        key,
                        (value as Number)
                            .toDouble()
                    )

                value is Number ->
                    values.put(
                        key,
                        value.toLong()
                    )

                value is String ->
                    values.put(
                        key,
                        value
                    )

                value is JSONObject &&
                    value.has("__blob__") ->
                    values.put(
                        key,
                        Base64.decode(
                            value.getString(
                                "__blob__"
                            ),
                            Base64.DEFAULT
                        )
                    )

                else ->
                    error(
                        "Unsupported backup value for $key"
                    )
            }
        }

        return values
    }
}
