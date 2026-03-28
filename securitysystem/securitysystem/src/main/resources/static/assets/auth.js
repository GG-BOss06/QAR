async function switchTab(which) {
  document.getElementById("tab-login").classList.toggle("active", which === "login")
  document.getElementById("tab-register").classList.toggle("active", which === "register")
  document.getElementById("panel-login").style.display = which === "login" ? "flex" : "none"
  document.getElementById("panel-register").style.display = which === "register" ? "flex" : "none"
}

async function refreshMe() {
  const me = await loadMe()
  const logged = document.getElementById("logged-in")
  if (me && me.id) {
    logged.style.display = "flex"
    document.getElementById("me-badge").textContent = "已登录：" + me.emailOrUsername + "（" + me.role + "）"
    document.getElementById("panel-login").style.display = "none"
    document.getElementById("panel-register").style.display = "none"
    return
  }
  logged.style.display = "none"
  await switchTab("login")
}

async function onLogin() {
  const u = document.getElementById("login-username").value
  const p = document.getElementById("login-password").value
  const btn = document.getElementById("btn-login")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/login", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ emailOrUsername: u, password: p })
    })
    showToast("登录成功", "会话 Cookie 已写入", "success")
    location.href = "/workbench"
  } catch (e) {
    showToast("登录失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function onRegister() {
  const u = document.getElementById("reg-username").value
  const p = document.getElementById("reg-password").value
  const p2 = document.getElementById("reg-password2").value
  const btn = document.getElementById("btn-register")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/register", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({ emailOrUsername: u, password: p, passwordConfirm: p2 })
    })
    showToast("注册成功", "请使用新账号登录", "success")
    await switchTab("login")
  } catch (e) {
    showToast("注册失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function onLogout() {
  const btn = document.getElementById("btn-logout")
  btn.disabled = true
  try {
    await apiFetch("/api/auth/logout", { method: "POST" })
    showToast("已退出", "会话已失效", "success")
    await refreshMe()
  } catch (e) {
    showToast("退出失败", e.message, "danger")
  } finally {
    btn.disabled = false
  }
}

async function main() {
  await initCsrf()
  document.getElementById("tab-login").addEventListener("click", () => switchTab("login"))
  document.getElementById("tab-register").addEventListener("click", () => switchTab("register"))
  document.getElementById("btn-login").addEventListener("click", onLogin)
  document.getElementById("btn-register").addEventListener("click", onRegister)
  document.getElementById("btn-logout").addEventListener("click", onLogout)
  await refreshMe()
}

main()

