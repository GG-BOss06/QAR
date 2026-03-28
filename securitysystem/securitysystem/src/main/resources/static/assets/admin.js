async function ensureAdmin() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  if (me.role !== "admin") {
    location.href = "/workbench"
    return null
  }
  document.getElementById("me-pill").textContent = me.emailOrUsername + " · " + me.role
  return me
}

async function refreshUsers() {
  const users = await apiFetch("/api/admin/users", { method: "GET" })
  const tbody = document.querySelector("#users tbody")
  tbody.innerHTML = ""
  for (const u of users) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td>"
    tr.children[0].textContent = u.emailOrUsername
    tr.children[1].textContent = u.role
    tr.children[2].textContent = (u.createdAt || "").replace("T", " ").replace("Z", "")
    tbody.appendChild(tr)
  }
}

async function refreshFiles() {
  const rows = await apiFetch("/api/admin/files", { method: "GET" })
  const tbody = document.querySelector("#files tbody")
  tbody.innerHTML = ""
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.id
    tr.children[1].textContent = r.ownerId
    tr.children[2].textContent = r.originalName
    tr.children[3].textContent = fmtBytes(r.sizeBytes)
    tr.children[4].textContent = r.policy || "-"
    tr.children[5].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
    const a = document.createElement("a")
    a.className = "btn"
    a.textContent = "下载"
    a.href = "/api/files/" + r.id + "/download"
    tr.children[6].appendChild(a)
    tbody.appendChild(tr)
  }
}

async function onExport() {
  showToast("开始导出", "服务器将解密并打包为zip", "success")
  window.location.href = "/api/admin/files/export"
}

async function onLogout() {
  const btn = document.getElementById("btn-logout")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    location.href = "/auth"
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function main() {
  const me = await ensureAdmin()
  if (!me) return
  document.getElementById("btn-export").addEventListener("click", onExport)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshUsers()
  await refreshFiles()
}

main()

