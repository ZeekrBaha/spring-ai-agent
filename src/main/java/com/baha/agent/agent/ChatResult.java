package com.baha.agent.agent;

import java.util.List;

/** Result of one agent turn: the reply plus the distinct tools it used. */
public record ChatResult(String reply, List<String> toolsUsed) {
}
