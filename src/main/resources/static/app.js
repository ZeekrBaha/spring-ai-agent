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

function send(text) {
  if (emptyEl) emptyEl.remove();
  addBubble("user", text);
  input.value = "";
  input.style.height = "auto";
  setBusy(true);

  const loading = addLoading();

  fetch("/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message: text, conversationId }),
  })
    .then(async (res) => {
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || body.message || `Request failed (${res.status})`);
      }
      return res.json();
    })
    .then((data) => {
      conversationId = data.conversationId;
      loading.remove();
      addBubble("assistant", data.reply, data.toolsUsed);
    })
    .catch((err) => {
      loading.remove();
      addError(err.message, text);
    })
    .finally(() => setBusy(false));
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

function addLoading() {
  const row = document.createElement("div");
  row.className = "row assistant";
  row.innerHTML = '<div class="bubble"><span class="dots"><span></span><span></span><span></span></span></div>';
  messagesEl.appendChild(row);
  scrollDown();
  return row;
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
