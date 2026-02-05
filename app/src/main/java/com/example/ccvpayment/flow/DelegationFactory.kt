package com.example.ccvpayment.flow

import eu.ccvlab.mapi.core.api.response.delegate.PaymentDelegate
import eu.ccvlab.mapi.core.api.response.delegate.TerminalDelegate

/**
 * DelegationFactory - Interface for creating SDK delegates.
 *
 * This interface is implemented by activities that need to handle
 * payment and terminal operations. It provides factory methods
 * for creating delegates that handle SDK callbacks.
 *
 * The delegates created by this factory will call finishFlow()
 * when operations complete (success or error).
 *
 * @author Erkan Kaplan
 * @since 1.0
 */
interface DelegationFactory {

    /**
     * Creates a PaymentDelegate for payment operations.
     *
     * @param context The context string for logging (e.g., "Payment", "Refund")
     * @return A PaymentDelegate that handles payment callbacks
     */
    fun createPaymentDelegate(context: String): PaymentDelegate

    /**
     * Creates a TerminalDelegate for terminal administration operations.
     *
     * @param context The context string for logging (e.g., "Status", "Period Closing")
     * @return A TerminalDelegate that handles terminal callbacks
     */
    fun createTerminalDelegate(context: String): TerminalDelegate
}
