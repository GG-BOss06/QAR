async function apiFetch(path, opts) {
  const csrf = window.__csrf || null
  const headers = Object.assign({"Accept": "application/json"}, opts && opts.headers ? opts.headers : {})
  if (csrf && csrf.headerName && csrf.token) {
    headers[csrf.headerName] = csrf.token
  }
  const res = await fetch(path, Object.assign({ credentials: "include", headers }, opts || {}))
  const ct = res.headers.get("content-type") || ""
  const data = ct.includes("application/json") ? await res.json().catch(() => null) : await res.text().catch(() => "")
  if (!res.ok) {
    const msg = data && data.message ? data.message : (typeof data === "string" ? data : "request_failed")
    throw new Error(msg)
  }
  return data
}

function showToast(title, message, variant) {
  const el = document.getElementById("toast")
  if (!el) return
  el.classList.add("show")
  const t = el.querySelector(".t")
  const m = el.querySelector(".m")
  t.textContent = title
  m.textContent = message
  el.style.borderColor = variant === "danger" ? "rgba(220,38,38,.35)" : (variant === "success" ? "rgba(22,163,74,.35)" : "var(--border)")
  window.clearTimeout(window.__toastTimer)
  window.__toastTimer = window.setTimeout(() => el.classList.remove("show"), 2800)
}

async function initCsrf() {
  try {
    const token = await apiFetch("/api/csrf", { method: "GET" })
    window.__csrf = token
  } catch (e) {
    window.__csrf = null
  }
}

async function loadMe() {
  try {
    return await apiFetch("/api/auth/me", { method: "GET" })
  } catch (e) {
    return null
  }
}

function fmtBytes(n) {
  if (!Number.isFinite(n)) return "-"
  const units = ["B","KB","MB","GB"]
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v = v / 1024
    i++
  }
  const p = i === 0 ? 0 : 1
  return v.toFixed(p) + " " + units[i]
}

