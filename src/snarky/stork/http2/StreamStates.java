package snarky.stork.http2;

public enum StreamStates {

    IDLE,
    // Header's the only one with end stream - header changes the state, then end stream changes that to another state
    RESERVED_LOCAL,
    // see above...
    RESERVED_REMOTE,
    OPEN,
    HALF_CLOSED_LOCAL,
    HALF_CLOSED_REMOTE,
    // below -> closed stream.
    RESET_BY_PEER,
    RESET_BY_US,
    CLOSED_BY_PEER,
    CLOSED_BY_US
}

