const API_BASE = "api"; // calls /Final_Final_web/api/...

const USER_ID = localStorage.getItem("clientId") || "client1";
const ACCOUNT_ID = localStorage.getItem("accountId") || ("ACC_" + USER_ID);

const tbody = document.getElementById("tbody");
const q = document.getElementById("q");
const reloadBtn = document.getElementById("reload");
const msg = document.getElementById("msg");

const currencySel = document.getElementById("currency");
const rateHint = document.getElementById("rateHint");

const dlg = document.getElementById("reviewsDlg");
const dlgTitle = document.getElementById("dlgTitle");
const dlgSub = document.getElementById("dlgSub");
const dlgBody = document.getElementById("dlgBody");
const dlgClose = document.getElementById("dlgClose");

let bikes = [];

// Offline FX (for display only). Server payment is EUR.
const FX_RATES = { EUR: 1.0, USD: 1.09, GBP: 0.85, TND: 3.35, MAD: 10.85 };

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function apiJson(url, options) {
  const res = await fetch(url, options);
  const txt = await res.text();
  if (!res.ok) throw new Error(txt || ("HTTP " + res.status));
  return txt ? JSON.parse(txt) : null;
}

async function apiText(url, options) {
  const res = await fetch(url, options);
  const txt = await res.text();
  if (!res.ok) throw new Error(txt || ("HTTP " + res.status));
  return txt;
}

function getCurrencySymbol(code) {
  const map = { EUR: "€", USD: "$", GBP: "£", TND: "DT", MAD: "DH" };
  return map[code] || code;
}

function fmtMoney(amount, cur) {
  const sym = getCurrencySymbol(cur);
  const n = Number(amount);
  const safe = Number.isFinite(n) ? n : 0;
  const decimals = cur === "TND" ? 3 : 2;
  return `${safe.toFixed(decimals)} ${sym}`;
}

function convertEurTo(cur, eurValue) {
  const rate = FX_RATES[cur] ?? 1.0;
  return eurValue * rate;
}

/**
 * ✅ Load budget from server and display it under title with user name
 * Expects endpoint:
 *   GET  /api/public/account?accountId=ACC_xxx
 * returns JSON like:
 *   { "balanceEur": 2500.0 }
 */
async function loadUserBudget() {
  const userInfo = document.getElementById("userInfo");
  if (!userInfo) return;

  try {
	const data = await apiJson(`${API_BASE}/public/auth/account?accountId=${encodeURIComponent(ACCOUNT_ID)}`);

    const balanceEur = Number(data?.balanceEur ?? 0);

    // Budget displayed in EUR (server truth)
    userInfo.textContent =
      `👤 ${USER_ID} — Budget: ${fmtMoney(balanceEur, "EUR")} | Account: ${ACCOUNT_ID} | Currency: ${currencySel.value}`;
  } catch (e) {
    userInfo.textContent =
      `👤 ${USER_ID} | Account: ${ACCOUNT_ID} | Currency: ${currencySel.value} — Budget unavailable`;
  }
}

function updateRateHint() {
  const cur = currencySel.value;
  const rate = FX_RATES[cur] ?? 1.0;

  // Only FX hint here (optional)
  rateHint.textContent = (cur === "EUR")
    ? ""
    : `Offline FX (display only): 1 EUR = ${rate} ${cur}`;
}

function render() {
  const term = (q.value || "").trim().toLowerCase();
  const cur = currencySel.value;

  const filtered = bikes.filter(b => {
    if (!term) return true;
    return (b.title || "").toLowerCase().includes(term) ||
           (b.owner || "").toLowerCase().includes(term);
  });

  if (!filtered.length) {
    tbody.innerHTML = "";
    msg.style.display = "block";
    msg.textContent = "No bikes to display.";
    return;
  }

  msg.style.display = "none";

  tbody.innerHTML = filtered.map(b => {
    const status = b.available
      ? `<span class="badge ok">Available</span>`
      : `<span class="badge bad">Not available</span>`;

    const eur = Number(b.priceEur ?? 0);
    const price = (cur === "EUR") ? eur : convertEurTo(cur, eur);

    return `
      <tr>
        <td>#${b.id}</td>
        <td>${escapeHtml(b.title)}</td>
        <td>${escapeHtml(b.owner)}</td>
        <td>${status}</td>
        <td>${b.rentedCount}</td>
        <td>${escapeHtml(fmtMoney(price, cur))}</td>
        <td>
          <button class="primary" onclick="openReviews(${b.id})">Reviews</button>
          <button class="ghost" onclick="addToBasket(${b.id})">Add to basket</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function loadBikes() {
  msg.style.display = "block";
  msg.textContent = "Loading...";
  tbody.innerHTML = "";

  // Reuse your existing endpoint /api/bikes
  const all = await apiJson(`${API_BASE}/bikes`);
  bikes = (all || []).filter(b => (b.rentedCount || 0) > 0 && !b.sold);

  msg.style.display = "none";
  render();
}

function displayBasket(basket) {
  const box = document.getElementById("basketBox");
  const cur = currencySel.value;

  if (!basket || !basket.bikes || basket.bikes.length === 0) {
    box.textContent = "Basket is empty.";
    return;
  }

  let total = 0;

  box.innerHTML = basket.bikes.map(b => {
    const eur = Number(b.priceEur || 0);
    const price = (cur === "EUR") ? eur : convertEurTo(cur, eur);
    total += price;

    return `
      <div style="display:flex;align-items:center;gap:10px;justify-content:space-between;">
        <div>• ${escapeHtml(b.title)} — ${fmtMoney(price, cur)}</div>
        <button class="ghost" onclick="removeFromBasket(${b.id})">❌</button>
      </div>
    `;
  }).join("");

  box.innerHTML += `<hr><b>Total: ${fmtMoney(total, cur)}</b>`;
}

async function loadBasket() {
  const basket = await apiJson(`${API_BASE}/public/basket?userId=${encodeURIComponent(USER_ID)}`);
  displayBasket(basket);
}

window.addToBasket = async function (bikeId) {
  try {
    const basket = await apiJson(`${API_BASE}/public/basket/add`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: USER_ID, bikeId })
    });
    displayBasket(basket);
  } catch (e) {
    alert("Error adding to basket: " + e.message);
  }
};

window.removeFromBasket = async function (bikeId) {
  try {
    const basket = await apiJson(`${API_BASE}/public/basket/remove`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: USER_ID, bikeId })
    });
    displayBasket(basket);
  } catch (e) {
    alert("Error removing from basket: " + e.message);
  }
};

window.purchase = async function () {
  try {
    const txt = await apiText(`${API_BASE}/public/basket/purchase`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userId: USER_ID,
        accountId: ACCOUNT_ID,
        currency: currencySel.value
      })
    });
    alert(txt);

    // Refresh page data + budget
    await loadBikes();
    await loadBasket();
    await loadUserBudget(); // ✅ refresh budget after purchase
  } catch (e) {
    alert("Purchase failed: " + e.message);
  }
};

// Reviews: keep your fixed version, fallback demo if endpoint missing
window.openReviews = async function (bikeId) {
  const b = bikes.find(x => x.id === bikeId);
  dlgTitle.textContent = b ? `Reviews for: ${b.title}` : `Reviews`;
  dlgSub.textContent = b ? `Owner: ${b.owner} | Bike #${b.id}` : `Bike #${bikeId}`;
  dlgBody.innerHTML = `<div class="empty">Loading reviews...</div>`;
  dlg.showModal();

  const demo = () => {
    const now = new Date();
    const list = [
      { userName: "Sarra", text: "Smooth ride, very comfortable 👍", condition: "GOOD", time: now.toISOString() },
      { userName: "Hamza", text: "Worth it for city commute.", condition: "OK", time: new Date(now - 86400000).toISOString() }
    ];
    dlgBody.innerHTML = `<div class="empty" style="margin-bottom:10px;">Reviews API not available, demo reviews:</div>` +
      list.map(r => `
        <div class="rev">
          <b>${escapeHtml(r.userName)}</b>: ${escapeHtml(r.text)}
          <div class="meta">${escapeHtml(r.time)} | Condition: ${escapeHtml(r.condition)}</div>
        </div>
      `).join("");
  };

  try {
    const list = await apiJson(`${API_BASE}/bikes/${bikeId}/reviews?limit=5`);
    if (!list || !list.length) {
      dlgBody.innerHTML = `<div class="empty">No reviews yet.</div>`;
    } else {
      dlgBody.innerHTML = list.map(r => `
        <div class="rev">
          <b>${escapeHtml(r.userName || "Anonymous")}</b>: ${escapeHtml(r.text || "")}
          <div class="meta">${escapeHtml(r.time || "")}${r.condition ? ` | Condition: ${escapeHtml(r.condition)}` : ""}</div>
        </div>
      `).join("");
    }
  } catch {
    demo();
  }
};

dlgClose?.addEventListener("click", () => dlg.close());

reloadBtn.addEventListener("click", async () => {
  await loadUserBudget();   // ✅ refresh budget too
  await loadBikes();
  await loadBasket();
  updateRateHint();
});

q.addEventListener("input", render);

currencySel.addEventListener("change", async () => {
  localStorage.setItem("currency", currencySel.value);
  updateRateHint();
  render();
  await loadBasket();      // refresh totals in new currency
  await loadUserBudget();  // ✅ update displayed currency label
});

(async function init() {
  const savedCur = localStorage.getItem("currency");
  if (savedCur) currencySel.value = savedCur;

  updateRateHint();
  await loadUserBudget(); // ✅ show budget under title
  await loadBikes();
  await loadBasket();
})();
