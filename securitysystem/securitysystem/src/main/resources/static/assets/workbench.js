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
    const ql = document.getElementById("admin-qar-link")
    if (ql) ql.style.display = "inline"
    const adminMenu = document.getElementById("admin-menu")
    if (adminMenu) {
      adminMenu.style.display = "block"
    }
  } else {
    const uc = document.getElementById("upload-card")
    if (uc) uc.style.display = "none"
    const grid = document.getElementById("grid")
    if (grid) grid.style.gridTemplateColumns = "1fr"
  }
  return me
}

async function checkTransportCrypto() {
  const statusEl = document.getElementById("crypto-status")
  if (!statusEl) return false
  
  statusEl.textContent = "检查中..."
  statusEl.className = "muted"
  
  try {
    await TransportCrypto.ensureSession()
    statusEl.textContent = "✓ TLS传输加密已建立"
    statusEl.className = "badge ok"
    return true
  } catch (e) {
    statusEl.textContent = "✗ TLS传输加密不可用"
    statusEl.className = "badge danger"
    return false
  }
}

async function refreshList() {
  const rows = await TransportCrypto.fetch("/api/files", { method: "GET" })
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
    
    const downloadBtn = document.createElement("button")
    downloadBtn.className = "btn"
    downloadBtn.textContent = "下载"
    downloadBtn.onclick = () => onDownload(r.id)
    tr.children[4].appendChild(downloadBtn)
    
    tbody.appendChild(tr)
  }
}

async function refreshStats() {
  try {
    const stats = await TransportCrypto.fetch("/api/files/stats", { method: "GET" })
    const totalUploadsEl = document.getElementById("total-uploads")
    const availableDataEl = document.getElementById("available-data")
    if (totalUploadsEl) {
      totalUploadsEl.textContent = stats.totalUploads || 0
    }
    if (availableDataEl) {
      availableDataEl.textContent = stats.availableData || 0
    }
  } catch (e) {
    console.error("Failed to refresh stats:", e)
  }
}

function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const base64 = (reader.result || "").toString().split(",")[1] || ""
      resolve(base64)
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function base64ToBlob(base64, contentType) {
  const bin = atob(base64 || "")
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return new Blob([bytes], { type: contentType || "application/octet-stream" })
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement("a")
  a.href = url
  a.download = filename || "download.bin"
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

async function onUpload() {
  const fileInput = document.getElementById("file")
  let policy = document.getElementById("policy").value || "role:user"
  const targetPersonNo = (document.getElementById("target-person-no")?.value || "").trim()
  const btn = document.getElementById("btn-upload")
  const out = document.getElementById("upload-result")
  
  if (!fileInput.files || !fileInput.files.length) {
    showToast("缺少文件", "请选择一个文件", "danger")
    return
  }
  
  const me = await loadMe()
  if (!me || me.role !== "admin") {
    showToast("权限不足", "只有管理员可以上传文件，当前角色: " + (me ? me.role : "未登录"), "danger")
    return
  }
  if (!targetPersonNo) {
    showToast("缺少归属工号", "请填写要分配下载权限的工号", "danger")
    return
  }
  if (!policy.includes("personNo:")) {
    policy = `${policy} personNo:${targetPersonNo}`.trim()
  }
  
  await initCsrf()
  const ready = await checkTransportCrypto()
  if (!ready) {
    showToast("传输异常", "TLS传输加密未建立，请刷新后重试", "danger")
    return
  }
  
  btn.disabled = true
  out.textContent = "准备上传..."
  
  try {
    const file = fileInput.files[0]
    out.textContent = "正在读取文件..."
    const fileBase64 = await fileToBase64(file)
    out.textContent = "正在上传..."
    const uploadData = {
      encryptedData: fileBase64,
      wrappedKey: "",
      originalName: file.name,
      contentType: file.type || "application/octet-stream",
      sizeBytes: file.size,
      policy: policy,
      personNo: targetPersonNo
    }
    
    const resp = await TransportCrypto.fetch("/api/files/encrypted", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(uploadData)
    })
    
    localStorage.setItem("qar_last_upload_id", resp.id)
    showToast("上传成功", "已加密保存：" + resp.id, "success")
    out.innerHTML = "记录ID：<b></b> · 策略：<span></span> · <a class='link' href='/feedback?fileId=" + encodeURIComponent(resp.id) + "&subject=" + encodeURIComponent("关于记录 " + resp.id + " 的问题") + "'>就此记录提交反馈</a>"
    out.querySelector("b").textContent = resp.id
    out.querySelector("span").textContent = resp.policy || "-"
    fileInput.value = ""
    const targetInput = document.getElementById("target-person-no")
    if (targetInput) targetInput.value = ""
    await refreshList()
    await refreshStats()
  } catch (e) {
    showToast("上传失败", e.message, "danger")
    out.textContent = ""
  } finally {
    btn.disabled = false
  }
}

async function onDownload(fileId) {
  try {
    await checkTransportCrypto()
    showToast("下载中", "正在获取数据...", "info")
    const resp = await TransportCrypto.fetch(`/api/files/${fileId}/payload`, { method: "GET" })
    const blob = base64ToBlob(resp.dataBase64, resp.contentType)
    downloadBlob(blob, resp.originalName || "download.bin")
    showToast("下载完成", "文件已保存", "success")
  } catch (e) {
    showToast("下载失败", e.message, "danger")
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
  
  await checkTransportCrypto()
  await refreshStats()
  
  if (me.role === "admin") {
    const btnUpload = document.getElementById("btn-upload")
    if (btnUpload) {
      btnUpload.addEventListener("click", onUpload)
    }
  }
  await refreshList()
}

main()
