const messagesEl = document.getElementById("messages");
const emptyEl = document.getElementById("empty");
const form = document.getElementById("composer");
const input = document.getElementById("input");
const sendBtn = document.getElementById("send");

let conversationId = null;

// Auto-grow textarea.
input.addEventListener("input", () => {
  input.style.height = "auto";
  input.style.height = input.scrollHeight + "px";
});

// Enter sends, Shift+Enter newline.
input.addEventListener("keydown", (e) => {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    form.requestSubmit();
  }
});

// Example chips.
document.querySelectorAll(".chip-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    input.value = btn.dataset.example;
    form.requestSubmit();
  });
});

form.addEventListener("submit", (e) => {
  e.preventDefault();
  const text = input.value.trim();
  if (!text) return;
  send(text);
});

async function send(text) {
  if (emptyEl && emptyEl.parentNode) emptyEl.remove();
  addBubble("user", text);
  input.value = "";
  input.style.height = "auto";
  setBusy(true);

  // Assistant bubble starts as a "using tools…" loading state until the first token.
  const stream = addStreamingAssistant();
  let firstToken = false;

  try {
    const res = await fetch("/api/chat/stream", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
      body: JSON.stringify({ message: text, conversationId }),
    });

    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error || body.message || `Request failed (${res.status})`);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    for (;;) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let boundary;
      while ((boundary = buffer.indexOf("\n\n")) >= 0) {
        const rawEvent = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        const event = parseSseData(rawEvent);
        if (!event) continue;

        if (event.type === "token") {
          if (!firstToken) { stream.beginText(); firstToken = true; }
          stream.appendText(event.text || "");
        } else if (event.type === "done") {
          conversationId = event.conversationId || conversationId;
          if (!firstToken) stream.beginText(); // model produced no tokens
          stream.finish(event.toolsUsed);
        } else if (event.type === "error") {
          stream.replaceWithError(event.error || "Something went wrong.", text);
        }
      }
    }
  } catch (err) {
    stream.replaceWithError(err.message, text);
  } finally {
    setBusy(false);
  }
}

// Extract the concatenated `data:` payload from one raw SSE event block.
function parseSseData(rawEvent) {
  let data = "";
  for (const line of rawEvent.split("\n")) {
    if (line.startsWith("data:")) data += line.slice(5).trim();
  }
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

function setBusy(busy) {
  sendBtn.disabled = busy;
}

function addBubble(role, text, tools) {
  const row = document.createElement("div");
  row.className = "row " + role;

  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = text; // textContent: never render model/tool output as HTML

  if (tools && tools.length) {
    const toolsEl = document.createElement("div");
    toolsEl.className = "tools";
    tools.forEach((t) => {
      const chip = document.createElement("span");
      chip.className = "tool-chip";
      chip.textContent = t;
      toolsEl.appendChild(chip);
    });
    bubble.appendChild(toolsEl);
  }

  row.appendChild(bubble);
  messagesEl.appendChild(row);
  scrollDown();
  return row;
}

// A streaming assistant bubble: starts with "using tools…" dots, then fills
// with text token-by-token, then shows tool chips, or converts to an error.
function addStreamingAssistant() {
  const row = document.createElement("div");
  row.className = "row assistant";
  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.innerHTML = '<span class="dots"><span></span><span></span><span></span></span>';
  row.appendChild(bubble);
  messagesEl.appendChild(row);
  scrollDown();

  let textNode = null;

  return {
    beginText() {
      bubble.textContent = ""; // clear the dots
      textNode = document.createTextNode("");
      bubble.appendChild(textNode); // textContent: never render model output as HTML
    },
    appendText(chunk) {
      if (!textNode) this.beginText();
      textNode.nodeValue += chunk;
      scrollDown();
    },
    finish(tools) {
      if (tools && tools.length) {
        const toolsEl = document.createElement("div");
        toolsEl.className = "tools";
        tools.forEach((t) => {
          const chip = document.createElement("span");
          chip.className = "tool-chip";
          chip.textContent = t;
          toolsEl.appendChild(chip);
        });
        bubble.appendChild(toolsEl);
      }
      scrollDown();
    },
    replaceWithError(message, originalText) {
      row.remove();
      addError(message, originalText);
    },
  };
}

function addError(message, originalText) {
  const row = document.createElement("div");
  row.className = "row error";

  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = message;

  const retry = document.createElement("button");
  retry.className = "retry";
  retry.textContent = "Retry";
  retry.addEventListener("click", () => {
    row.remove();
    send(originalText);
  });

  bubble.appendChild(document.createElement("br"));
  bubble.appendChild(retry);
  row.appendChild(bubble);
  messagesEl.appendChild(row);
  scrollDown();
}

function scrollDown() {
  messagesEl.scrollTop = messagesEl.scrollHeight;
}
