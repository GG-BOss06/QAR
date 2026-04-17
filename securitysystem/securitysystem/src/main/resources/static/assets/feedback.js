function qs(name) {
  const u = new URL(location.href)
  return u.searchParams.get(name)
}

function statusBadge(status) {
  const s = (status || "").toLowerCase()
  if (s === "resolved") return "<span class='badge ok'>已解决</span>"
  if (s === "in_progress") return "<span class='badge'>处理中</span>"
  return "<span class='badge warn'>新反馈</span>"
}

async function ensureMe() {
  await initCsrf()
  const me = await loadMe()
  if (!me) {
    location.href = "/auth"
    return null
  }
  const mePill = document.getElementById("me-pill")
  if (mePill) {
    const spanEl = mePill.querySelector("span")
    if (spanEl) {
      spanEl.textContent = me.emailOrUsername + " · " + me.role
    }
  }
  const dropdownUserName = document.getElementById("dropdown-user-name")
  const dropdownUserRole = document.getElementById("dropdown-user-role")
  if (dropdownUserName) {
    dropdownUserName.textContent = me.fullName || me.emailOrUsername || "-"
  }
  if (dropdownUserRole) {
    dropdownUserRole.textContent = me.role || "-"
  }
  const dropdownFullname = document.getElementById("dropdown-fullname")
  const dropdownPersonno = document.getElementById("dropdown-personno")
  const dropdownAirline = document.getElementById("dropdown-airline")
  const dropdownPosition = document.getElementById("dropdown-position")
  const dropdownDept = document.getElementById("dropdown-dept")
  if (dropdownFullname) dropdownFullname.textContent = me.fullName || "-"
  if (dropdownPersonno) dropdownPersonno.textContent = me.personNo || me.emailOrUsername || "-"
  if (dropdownAirline) dropdownAirline.textContent = me.airline || "-"
  if (dropdownPosition) dropdownPosition.textContent = me.positionTitle || "-"
  if (dropdownDept) dropdownDept.textContent = me.department || "-"
  if (me.role === "admin") {
    const adminLink = document.getElementById("admin-link")
    if (adminLink) {
      adminLink.style.display = "inline"
    }
    const adminDataLink = document.getElementById("admin-data-link")
    if (adminDataLink) {
      adminDataLink.style.display = "inline"
    }
    const adminMenu = document.getElementById("admin-menu")
    if (adminMenu) {
      adminMenu.style.display = "block"
    }
  }
  return me
}

async function refreshList() {
  const rows = await apiFetch("/api/feedback", { method: "GET" })
  const tbody = document.querySelector("#tbl tbody")
  tbody.innerHTML = ""
  document.getElementById("empty").style.display = rows.length ? "none" : "block"
  for (const r of rows) {
    const tr = document.createElement("tr")
    tr.innerHTML = "<td></td><td></td><td></td><td></td><td></td>"
    const title = r.subject || (r.message || "").slice(0, 16) || "(无主题)"
    tr.children[0].textContent = title
    tr.children[1].textContent = r.type || "-"
    tr.children[2].innerHTML = statusBadge(r.status)
    tr.children[3].textContent = r.relatedFileId || "-"
    tr.children[4].textContent = (r.updatedAt || r.createdAt || "").replace("T", " ").replace("Z", "")
    tbody.appendChild(tr)
  }
}

function fillFromQueryOrLast() {
  const fileId = qs("fileId") || localStorage.getItem("qar_last_upload_id") || ""
  if (fileId) {
    document.getElementById("relatedFileId").value = fileId
  }
  const preset = qs("subject")
  if (preset) {
    document.getElementById("subject").value = preset
  }
}

async function onSubmit() {
  const btn = document.getElementById("btn-submit")
  const hint = document.getElementById("submit-hint")
  btn.disabled = true
  hint.textContent = ""
  try {
    const payload = {
      type: document.getElementById("type").value,
      contact: document.getElementById("contact").value || null,
      subject: document.getElementById("subject").value || null,
      relatedFileId: document.getElementById("relatedFileId").value || null,
      message: document.getElementById("message").value || ""
    }
    const resp = await apiFetch("/api/feedback", { method: "POST", body: JSON.stringify(payload), headers: { "Content-Type": "application/json" } })
    showToast("提交成功", "反馈ID：" + resp.id, "success")
    hint.textContent = "我们已收到你的反馈，状态为“新反馈”。你可以在右侧看到处理进展。"
    document.getElementById("message").value = ""
    await refreshList()
  } catch (e) {
    showToast("提交失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function onLogout() {
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    location.href = "/auth"
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  }
}

function onFillLast() {
  const id = localStorage.getItem("qar_last_upload_id") || ""
  if (!id) {
    showToast("没有记录", "请先在工作台上传一次数据", "danger")
    return
  }
  document.getElementById("relatedFileId").value = id
  showToast("已填入", "关联记录ID：" + id, "success")
}

async function onCopyEnv() {
  const me = await loadMe()
  const env = {
    user: me ? me.emailOrUsername : "-",
    role: me ? me.role : "-",
    time: new Date().toISOString(),
    ua: navigator.userAgent,
    lastUploadId: localStorage.getItem("qar_last_upload_id") || ""
  }
  const text = Object.entries(env).map(([k, v]) => k + ": " + v).join("\n")
  try {
    await navigator.clipboard.writeText(text)
    showToast("已复制", "环境信息已复制到剪贴板", "success")
  } catch (e) {
    showToast("复制失败", "浏览器不支持剪贴板权限", "danger")
  }
}

async function main() {
  const me = await ensureMe()
  if (!me) return
  
  const mePill = document.getElementById("me-pill")
  const dropdownMenu = mePill ? mePill.querySelector(".dropdown-menu") : null
  
  if (mePill && dropdownMenu) {
    mePill.addEventListener("click", function(e) {
      e.stopPropagation()
      dropdownMenu.classList.toggle("show")
    })
    
    document.addEventListener("click", function(e) {
      if (!mePill.contains(e.target)) {
        dropdownMenu.classList.remove("show")
      }
    })
  }
  
  const btnLogout = document.getElementById("btn-logout")
  if (btnLogout) {
    btnLogout.addEventListener("click", onLogout)
  }
  
  document.getElementById("btn-submit").addEventListener("click", onSubmit)
  document.getElementById("btn-fill-last").addEventListener("click", fillFromQueryOrLast)
  document.getElementById("btn-copy-env").addEventListener("click", onCopyEnv)
  document.getElementById("btn-refresh").addEventListener("click", refreshList)
  await refreshList()
}

main()

