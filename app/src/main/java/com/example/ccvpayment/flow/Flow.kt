package com.example.ccvpayment.flow

/**
 * Flow enum - Represents all payment and terminal operations.
 *
 * Each flow represents a specific operation that can be performed
 * on the CCV terminal. The activeFlow state is used to track
 * whether an operation is in progress.
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
enum class Flow(val description: String) {
    // Payment Operations
    PAYMENT("Payment"),
    REFUND("Refund"),
    VOID("Void"),
    AUTHORISE("Authorise"),
    CAPTURE("Capture"),

    // Terminal Operations
    STATUS("Status"),
    ABORT("Abort"),
    RETRIEVE_LAST_TICKET("Retrieve Last Ticket"),
    REPEAT_LAST_MESSAGE("Repeat Last Message"),

    // Report Operations
    PERIOD_CLOSING("Period Closing"),
    TRANSACTION_OVERVIEW("Transaction Overview"),

    // Card Operations
    CARD_READ("Card Read"),
    CARD_DETECTION("Card Detection"),

    // Token Operations
    TOKEN("Token"),
    REFUND_WITH_TOKEN("Refund with Token"),

    // Gift Card
    GIFT_CARD_BALANCE("Gift Card Balance"),
    GIFT_CARD_ACTIVATION("Gift Card Activation"),

    // Terminal Management
    INITIALISATION("Initialisation"),
    LOGIN("Login"),
    DIAGNOSIS("Diagnosis"),
    SERVICE_MENU("Service Menu"),
    FACTORY_RESET("Factory Reset");

    companion object {
        fun get(name: String): Flow? {
            return values().find { it.description.equals(name, ignoreCase = true) }
        }
    }

    override fun toString(): String = "Flow{name='$description'}"
}
