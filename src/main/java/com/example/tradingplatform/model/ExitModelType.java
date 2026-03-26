package com.example.tradingplatform.model;

/**
 * Defines how a trade should be exited.
 */
public enum ExitModelType {

    /** Exit only when strategy sends a signal */
    SIGNAL_ONLY,

    /** Exit using fixed risk-reward ratio */
    FIXED_RR
}