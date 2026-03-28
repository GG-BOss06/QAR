async function ensureMe() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  document.getElementById("me-pill").textContent = me.emailOrUsername + " · " + me.role
  if (me.role === "admin") {
    document.getElementById("admin-link").style.display = "inline"
  }
  return me
}

async function refreshList() {
  const rows = await apiFetch("/api/files", { method: "GET" })
  const tbody = document.querySelector("#tbl tbody")
  tbody.innerHTML = ""
  document.getElementById("empty").style.display = rows.length ? "none" : "block"
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td class='actions'></td>"
    tr.children[0].textContent = r.originalName
    tr.children[1].textContent = fmtBytes(r.sizeBytes)
    tr.children[2].textContent = r.policy || "-"
    tr.children[3].textContent = (r.createdAt || "").replace("T", " ").replace("Z", "")
    const a = document.createElement("a")
    a.className = "btn"
    a.textContent = "下载"
    a.href = "/api/files/" + r.id + "/download"
    tr.children[4].appendChild(a)
    tbody.appendChild(tr)
  }
}

async function onUpload() {
  const fileInput = document.getElementById("file")
  const policy = document.getElementById("policy").value
  const btn = document.getElementById("btn-upload")
  const out = document.getElementById("upload-result")
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择一个文件", "danger")
    return
  }
  btn.disabled = true
  out.textContent = ""
  try {
    const fd = new FormData()
    fd.append("file", fileInput.files[0])
    if (policy) fd.append("policy", policy)
    const resp = await apiFetch("/api/files", { method: "POST", body: fd, headers: {} })
    showToast("上传成功", "已加密并写入数据库：" + resp.id, "success")
    out.textContent = "记录ID：" + resp.id + " · 策略：" + (resp.policy || "-")
    fileInput.value = ""
    await refreshList()
  } catch (e) {
    showToast("上传失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
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
  const me = await ensureMe()
  if (!me) return
  document.getElementById("btn-upload").addEventListener("click", onUpload)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshList()
}

main()

