package com.baha.agent.agent;

/**
 * Signals that the failure came from the upstream model / tool-call loop (e.g.
 * OpenAI unreachable, auth, rate limit) — not a bug in our own code. The web
 * layer maps this to a friendly 502; any OTHER exception is a genuine defect
 * and surfaces as 500 rather than being masked.
 */
public class AgentUpstreamException extends RuntimeException {

    public AgentUpstreamException(Throwable cause) {
        super(cause);
    }
}
