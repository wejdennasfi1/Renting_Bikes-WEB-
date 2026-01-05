const API_BASE = "api";
console.log("🔥 app.js LOADED (SOLD + WAITING LIST FIX)");

let users = [];
let bikes = [];

const userSelect = document.getElementById("userSelect");
const refreshUsersBtn = document.getElementById("refreshUsersBtn");

const ownedBikesBox = document.getElementById("ownedBikesBox");

const bikeSearch = document.getElementById("bikeSearch");
const reloadBikesBtn = document.getElementById("reloadBikesBtn");
const addBikeBtn = document.getElementById("addBikeBtn");
const bikesBody = document.getElementById("bikesBody");

const reloadRentalsBtn = document.getElementById("reloadRentalsBtn");
const rentalsBox = document.getElementById("rentalsBox");

const returnRentalId = document.getElementById("returnRentalId");
const returnCondition = document.getElementById("returnCondition");
const returnNote = document.getElementById("returnNote");
const returnBtn = document.getElementById("returnBtn");
const returnMsg = document.getElementById("returnMsg");

const reloadNotifBtn = document.getElementById("reloadNotifBtn");
const clearUiBtn = document.getElementById("clearUiBtn");
const notifBox = document.getElementById("notifBox");

// ========================
// Helpers
// ========================
function getSelectedUserId() {
  return parseInt(userSelect.value, 10);
}

function getSelectedUser() {
  const id = getSelectedUserId();
  return users.find(u => u.id === id);
}

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

// ========================
// ✅ The ONLY correct state logic
// sold=true ALWAYS wins
// ========================
function getState(b) {
  if (b.sold === true) return "SOLD";
  if (b.available === false) return "RENTED";
  return "AVAILABLE";
}

function statusBadge(b) {
  const st = getState(b);
  if (st === "SOLD") return `<span class="badge sold">Sold</span>`;
  if (st === "RENTED") return `<span class="badge bad">Rented</span>`;
  return `<span class="badge ok">Available</span>`;
}

// ========================
// Load
// ========================
async function loadUsers() {
  users = await apiJson(`${API_BASE}/users`);

  userSelect.innerHTML = (users || []).map(u =>
    `<option value="${u.id}">#${u.id} ${escapeHtml(u.name)} (${escapeHtml(u.type)})</option>`
  ).join("");

  await loadBikes();
  await loadRentals();
  await loadNotifications();
}

async function loadBikes() {
  bikes = await apiJson(`${API_BASE}/bikes`);
  bikes = bikes || [];
  renderOwnedBikes();
  renderBikes();
}

// ========================
// Owned bikes (owner sees SOLD)
// ========================
function renderOwnedBikes() {
  const selectedUser = getSelectedUser();
  if (!selectedUser) {
    ownedBikesBox.innerHTML = `<div class="empty">No user selected</div>`;
    return;
  }

  const owned = bikes.filter(b => b.owner === selectedUser.name);

  if (!owned.length) {
    ownedBikesBox.innerHTML = `<div class="empty">No owned bikes</div>`;
    return;
  }

  ownedBikesBox.innerHTML = owned.map(b => {
    const state = getState(b);
    const status = statusBadge(b);

    // who rents? only for RENTED (not SOLD, not AVAILABLE)
    const whoBtn =
      state === "RENTED"
        ? `<button class="btn" onclick="whoRentsMyBike(${b.id})">Who rents?</button>`
        : `<button class="btn" disabled title="${state === "SOLD" ? "Bike is sold" : "Bike is available"}">Who rents?</button>`;

    return `
      <div class="rentalItem">
        <div style="display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap;">
          <div><b>#${b.id}</b> ${escapeHtml(b.title)} ${status}</div>
          <div class="row">
            <button class="btn ghost" onclick="showWaiting(${b.id})">Waiting list</button>
            ${whoBtn}
            <button class="btn danger" onclick="removeBike(${b.id})">Remove</button>
          </div>
        </div>
      </div>
    `;
  }).join("");
}

// ========================
// Bikes (others)
// ✅ IMPORTANT:
// - hide SOLD bikes completely
// - allow click on RENTED to join waiting list
// ========================
function renderBikes() {
  const term = (bikeSearch.value || "").trim().toLowerCase();
  const selectedUser = getSelectedUser();

  const filtered = bikes
    // not owned by selected user
    .filter(b => !selectedUser || b.owner !== selectedUser.name)
    // hide sold bikes from others list
    .filter(b => getState(b) !== "SOLD")
    // search
    .filter(b => {
      if (!term) return true;
      return (b.title || "").toLowerCase().includes(term) ||
             (b.owner || "").toLowerCase().includes(term);
    });

  bikesBody.innerHTML = filtered.map(b => {
    const state = getState(b);
    const status = statusBadge(b);

    // ✅ clickable even if RENTED (to join waiting list)
    const rentLabel = (state === "AVAILABLE") ? "Rent" : "Join waiting";
    const rentBtn = `<button class="btn" onclick="rentBike(${b.id})">${rentLabel}</button>`;

    const actions = `
      ${rentBtn}
      <button class="btn ghost" onclick="showWaiting(${b.id})">Waiting list</button>
    `;

    return `
      <tr>
        <td>#${b.id}</td>
        <td>${escapeHtml(b.title)}</td>
        <td>${escapeHtml(b.owner)}</td>
        <td>${status}</td>
        <td>${b.rentedCount}</td>
        <td>${actions}</td>
      </tr>
    `;
  }).join("");
}

// ========================
// Waiting list / renter info
// ========================
async function showWaiting(bikeId) {
  const listIds = await apiJson(`${API_BASE}/bikes/${bikeId}/waiting`);
  const names = (listIds || []).map(id => {
    const u = users.find(x => x.id === id);
    return u ? u.name : ("#" + id);
  });
  alert(`Waiting list for bike ${bikeId}: ` + (names.length ? names.join(", ") : "empty"));
}

async function whoRentsMyBike(bikeId) {
  const txt = await apiText(`${API_BASE}/bikes/${bikeId}/renter`);
  alert(txt);
}

// ========================
// Rent (WAITING LIST works here)
// ========================
async function rentBike(bikeId) {
  const b = bikes.find(x => x.id === bikeId);

  // ✅ ONLY block sold
  if (b && b.sold === true) {
    alert("This bike is SOLD and cannot be rented.");
    return;
  }

  // ✅ call API even if rented => server will reply WAITING_LIST
  const userId = getSelectedUserId();
  const body = { bikeId, userId };

  const result = await apiText(`${API_BASE}/rentals/rent`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });

  if (result.startsWith("RENT_OK:")) {
    alert("Rent OK. RentalId=" + result.split(":")[1]);
  } else if (result === "WAITING_LIST") {
    alert("Bike not available. You were added to waiting list ✅");
  } else if (result === "CANNOT_RENT_OWN_BIKE") {
    alert("You cannot rent your own bike.");
  } else {
    alert(result);
  }

  await loadBikes();
  await loadRentals();
  await loadNotifications();
}

// ========================
// Rentals
// ========================
async function loadRentals() {
  const userId = getSelectedUserId();
  const rentals = await apiJson(`${API_BASE}/rentals/user/${userId}`);

  const active = (rentals || []).filter(r => r.endTime == null);

  if (!active.length) {
    rentalsBox.innerHTML = `<div class="empty">No active rentals</div>`;
    return;
  }

  rentalsBox.innerHTML = active.map(r => `
    <div class="rentalItem">
      <div><b>Rental #${r.id}</b> | Bike #${r.bikeId} | started: ${escapeHtml(r.startTime)}</div>
      <button class="btn" onclick="quickReturn(${r.id})">Return this</button>
    </div>
  `).join("");
}

async function quickReturn(rentalId) {
  returnRentalId.value = rentalId;
  returnCondition.value = "Good";
  returnNote.value = "";
  await doReturn();
}

async function doReturn() {
  const rentalId = parseInt(returnRentalId.value, 10);
  if (!rentalId) {
    returnMsg.textContent = "Enter a valid Rental ID.";
    returnMsg.className = "msg bad";
    return;
  }

  if (!returnNote.value || returnNote.value.trim() === "") {
    returnMsg.textContent = "Review is required (write a note).";
    returnMsg.className = "msg bad";
    return;
  }

  const body = {
    rentalId,
    condition: returnCondition.value,
    note: returnNote.value
  };

  const result = await apiText(`${API_BASE}/rentals/return`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });

  if (result === "REVIEW_REQUIRED") {
    returnMsg.textContent = "Review is required (write a note).";
    returnMsg.className = "msg bad";
    return;
  }

  returnMsg.textContent = result;
  returnMsg.className = "msg ok";

  await loadBikes();
  await loadRentals();
  await loadNotifications();
}

// ========================
// Notifications
// ========================
async function loadNotifications() {
  const userId = getSelectedUserId();
  const list = await apiJson(`${API_BASE}/users/${userId}/notifications`);

  if (!list || !list.length) {
    notifBox.innerHTML = `<div class="empty">No notifications</div>`;
    return;
  }

  notifBox.innerHTML = list.slice().reverse().map(n =>
    `<div class="notifItem">${escapeHtml(n)}</div>`
  ).join("");
}

// ========================
// Remove / Add bike
// ========================
async function removeBike(bikeId) {
  const userId = getSelectedUserId();
  if (!confirm("Remove this bike?")) return;

  const result = await apiText(`${API_BASE}/bikes/${bikeId}?userId=${userId}`, {
    method: "DELETE"
  });

  if (result !== "REMOVE_OK") alert(result);

  await loadBikes();
  await loadRentals();
  await loadNotifications();
}

async function addBike() {
  const userId = getSelectedUserId();
  const title = prompt("Bike title?");
  if (!title) return;

  const priceStr = prompt("Price in EUR?", "300");
  const priceEur = parseFloat(priceStr || "0");

  await apiJson(`${API_BASE}/bikes`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, title, priceEur })
  });

  await loadBikes();
  await loadNotifications();
}

// ========================
// Events
// ========================
refreshUsersBtn.addEventListener("click", loadUsers);
reloadBikesBtn.addEventListener("click", loadBikes);
bikeSearch.addEventListener("input", renderBikes);

reloadRentalsBtn.addEventListener("click", loadRentals);
returnBtn.addEventListener("click", doReturn);

reloadNotifBtn.addEventListener("click", loadNotifications);

clearUiBtn.addEventListener("click", () => {
  rentalsBox.innerHTML = "";
  notifBox.innerHTML = "";
  returnMsg.textContent = "";
});

addBikeBtn.addEventListener("click", addBike);

userSelect.addEventListener("change", async () => {
  renderOwnedBikes();
  renderBikes();
  await loadRentals();
  await loadNotifications();
});

// Start
loadUsers().catch(err => alert("Error: " + err.message));
