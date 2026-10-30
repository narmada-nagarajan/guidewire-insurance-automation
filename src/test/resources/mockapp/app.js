/*
 * InsureSuite Mock - a tiny single page app that imitates the three core Guidewire applications:
 *   PolicyCenter  : submission -> quote -> underwriting -> bind/issue -> change -> cancel -> renew
 *   ClaimCenter   : FNOL -> coverage verification -> exposure -> reserve -> payment -> close
 *   BillingCenter : invoices -> payments
 * State is kept in localStorage so every browser session starts clean after localStorage.clear().
 * Every action runs behind a short "Loading" overlay to simulate AJAX so explicit waits are required.
 */
(function () {
  'use strict';

  /* ------------------------------------------------------------------ users & reference data */
  const PASSWORD = 'gw';
  const USERS = {
    agent1: { role: 'Producer',          name: 'Alice Agent',      authority: 0 },
    uw1:    { role: 'Underwriter',       name: 'Uma Underwriter',  authority: 0 },
    adj1:   { role: 'Claims Adjuster',   name: 'Adam Adjuster',    authority: 5000 },
    sup1:   { role: 'Claims Supervisor', name: 'Sara Supervisor',  authority: 100000 },
    bill1:  { role: 'Billing Clerk',     name: 'Bill Clerk',       authority: 0 }
  };
  const LIAB  = { '50/100': 1.0, '100/300': 1.15, '250/500': 1.35 };
  const STATE = { CA: 1.2, TX: 1.1, OH: 0.9, NY: 1.3 };
  const COLL  = { '250': 1.1, '500': 1.0, '1000': 0.9 };
  const BASE_RATE = 800;
  const LOSS_TYPES = ['Collision', 'Comprehensive - Theft', 'Glass Breakage', 'Liability - Third Party'];

  /* ------------------------------------------------------------------ state */
  const KEY = 'gwMockState';
  let S;
  function fresh() {
    return { session: null, seq: { sub: 1000, pol: 1000000000, clm: 100000, inv: 50000, pay: 0 },
             subs: {}, pols: {}, clms: {}, invs: {}, acts: [] };
  }
  function load() { try { return JSON.parse(localStorage.getItem(KEY)) || fresh(); } catch (e) { return fresh(); } }
  function save() { localStorage.setItem(KEY, JSON.stringify(S)); }

  /* ------------------------------------------------------------------ helpers */
  const $ = (id) => document.getElementById(id);
  const v = (id) => { const e = $(id); return e ? e.value.trim() : ''; };
  const checked = (id) => !!($(id) && $(id).checked);
  const pad = (n) => String(n).padStart(2, '0');
  const DAY = 86400000;
  const round2 = (x) => Math.round((x + Number.EPSILON) * 100) / 100;
  const num = (s) => parseFloat(String(s).replace(/[$,]/g, ''));
  const esc = (s) => String(s == null ? '' : s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  function money(n) { const a = Math.abs(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); return (n < 0 ? '-' : '') + '$' + a; }

  function parseD(s) {
    const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec((s || '').trim());
    if (!m) return null;
    const mo = +m[1], d = +m[2], y = +m[3];
    const t = Date.UTC(y, mo - 1, d); const dt = new Date(t);
    return (dt.getUTCMonth() === mo - 1 && dt.getUTCDate() === d) ? t : null;
  }
  function fmtD(t) { const d = new Date(t); return pad(d.getUTCMonth() + 1) + '/' + pad(d.getUTCDate()) + '/' + d.getUTCFullYear(); }
  function todayT() { const n = new Date(); return Date.UTC(n.getFullYear(), n.getMonth(), n.getDate()); }
  function addMonths(t, m) {
    const d = new Date(t); const y = d.getUTCFullYear(); const mo = d.getUTCMonth() + m;
    const last = new Date(Date.UTC(y, mo + 1, 0)).getUTCDate();
    return Date.UTC(y, mo, Math.min(d.getUTCDate(), last));
  }
  const daysBetween = (a, b) => Math.round((b - a) / DAY);
  function ageOn(dobT, onT) {
    const d = new Date(dobT), o = new Date(onT);
    let a = o.getUTCFullYear() - d.getUTCFullYear();
    if (o.getUTCMonth() < d.getUTCMonth() || (o.getUTCMonth() === d.getUTCMonth() && o.getUTCDate() < d.getUTCDate())) a--;
    return a;
  }
  const user = () => USERS[S.session];
  const fmtOpts = (list, sel) => list.map((o) => `<option${String(o) === String(sel) ? ' selected' : ''}>${o}</option>`).join('');
  const kv = (rows) => '<dl class="kv">' + rows.map((r) => `<dt>${r[1]}</dt><dd id="${r[0]}">${esc(r[2])}</dd>`).join('') + '</dl>';

  /* ------------------------------------------------------------------ UI plumbing */
  let pending = 0;
  function busy(fn, ms) {
    pending++; $('Loading').hidden = false;
    setTimeout(() => { try { fn(); } finally { pending--; if (pending <= 0) { pending = 0; $('Loading').hidden = true; } } }, ms == null ? 250 : ms);
  }
  function alertMsg(msg, type) { const a = $('Alert'); a.textContent = msg; a.className = type || 'info'; }
  function clearAlert() { const a = $('Alert'); a.textContent = ''; a.className = ''; }
  const fail = (m) => alertMsg(m, 'error');
  const ok = (m) => alertMsg(m, 'success');
  const go = (hash) => { location.hash = hash; };
  const notFound = (what) => `<div class="card"><h3 id="NotFound">${what} not found</h3></div>`;

  /* ------------------------------------------------------------------ rating & underwriting */
  function rate(p) { // p: age, state, liability, collision, comprehensive, violations, term, vehicleYear, effYear
    const ageF = p.age < 21 ? 1.8 : p.age <= 25 ? 1.4 : p.age < 65 ? 1.0 : 1.2;
    const vehF = (p.effYear - p.vehicleYear) < 3 ? 1.1 : 1.0;
    let prem = BASE_RATE * STATE[p.state] * LIAB[p.liability] * ageF * (1 + 0.15 * p.violations) * vehF * COLL[p.collision];
    if (p.comprehensive) prem += 120;
    return round2(prem * p.term / 12);
  }
  function uwIssues(age, violations) {
    const r = [];
    if (age < 21 && violations >= 1) r.push('Young driver with prior violations');
    if (violations >= 3) r.push('Three or more violations in the last 3 years');
    return r;
  }

  /* ------------------------------------------------------------------ billing helpers */
  function createInvoices(polNo, amount, startT, plan, desc) {
    const mk = (amt, due) => {
      const no = 'INV-' + (++S.seq.inv);
      S.invs[no] = { no, pol: polNo, due, amount: round2(amt), paid: 0, desc };
    };
    if (plan === 'Quarterly') {
      const part = Math.floor(amount * 100 / 4) / 100;
      for (let i = 0; i < 4; i++) mk(i === 3 ? amount - part * 3 : part, addMonths(startT, 3 * i));
    } else { mk(amount, startT); }
  }
  const invsFor = (polNo) => Object.values(S.invs).filter((i) => i.pol === polNo);
  const invStatus = (i) => i.paid >= i.amount - 0.001 ? 'Paid' : i.paid > 0 ? 'Partially Paid' : 'Billed';

  /* ------------------------------------------------------------------ views */
  const views = {};
  let fnol = null;     // FNOL wizard state
  let searchHtml = ''; // keeps search results while re-rendering

  views.login = () => `
    <h2 id="Login:Title">InsureSuite Login</h2>
    <div class="card" style="max-width:420px">
      <div class="grid" style="grid-template-columns:1fr">
        <label>Username <input id="Login:LoginScreen:LoginDV:username" autocomplete="off"></label>
        <label>Password <input id="Login:LoginScreen:LoginDV:password" type="password"></label>
      </div><br>
      <button id="Login:LoginScreen:LoginDV:submit" data-action="login">Log In</button>
      <p class="hint">Demo users (password gw): agent1 Producer | uw1 Underwriter | adj1 Adjuster | sup1 Supervisor | bill1 Billing</p>
    </div>`;

  views.desktop = () => {
    const u = user();
    const acts = S.acts.filter((a) => a.role === u.role && a.status === 'Open');
    const rows = acts.map((a) => `<tr class="activity-row"><td class="act-type">${esc(a.type)}</td><td class="act-subject">${esc(a.subject)}</td>
      <td><a class="activity-link" href="${a.link}">Open</a></td></tr>`).join('');
    return `<h2 id="Desktop:Title">Desktop</h2>
      <div class="card"><h3>Welcome</h3>${kv([['Desktop:UserName', 'User', u.name], ['Desktop:UserRole', 'Role', u.role]])}</div>
      <div class="card"><h3>My Activities</h3>
        ${acts.length ? `<table id="Desktop:Activities"><thead><tr><th>Type</th><th>Subject</th><th></th></tr></thead><tbody>${rows}</tbody></table>`
                      : '<p id="Desktop:NoActivities">No open activities</p>'}
      </div>`;
  };

  views.submission = () => {
    const t = fmtD(todayT());
    return `<h2 id="NewSubmission:Title">New Submission - Personal Auto</h2>
      <div class="card"><h3>Account &amp; Driver</h3><div class="grid">
        <label>First Name <input id="NewSubmission:firstName"></label>
        <label>Last Name <input id="NewSubmission:lastName"></label>
        <label>Date of Birth (MM/DD/YYYY) <input id="NewSubmission:dob" placeholder="MM/DD/YYYY"></label>
        <label>Garaging State <select id="NewSubmission:state">${fmtOpts(Object.keys(STATE), 'OH')}</select></label>
        <label>Violations (last 3 years) <select id="NewSubmission:violations">${fmtOpts([0, 1, 2, 3, 4, 5], 0)}</select></label>
      </div></div>
      <div class="card"><h3>Policy Info</h3><div class="grid">
        <label>Effective Date <input id="NewSubmission:effectiveDate" value="${t}"></label>
        <label>Term <select id="NewSubmission:term">${fmtOpts(['12 Months', '6 Months'], '12 Months')}</select></label>
        <label>Payment Plan <select id="NewSubmission:paymentPlan">${fmtOpts(['Full Pay', 'Quarterly'], 'Full Pay')}</select></label>
      </div></div>
      <div class="card"><h3>Vehicle</h3><div class="grid">
        <label>Year <input id="NewSubmission:vehicleYear"></label>
        <label>Make <select id="NewSubmission:vehicleMake">${fmtOpts(['Toyota', 'Honda', 'Ford', 'BMW'], 'Toyota')}</select></label>
        <label>Model <input id="NewSubmission:vehicleModel"></label>
      </div></div>
      <div class="card"><h3>Coverages</h3><div class="grid">
        <label>Liability Limit <select id="NewSubmission:liability">${fmtOpts(Object.keys(LIAB), '100/300')}</select></label>
        <label>Collision Deductible <select id="NewSubmission:collision">${fmtOpts(Object.keys(COLL), '500')}</select></label>
        <label class="inline"><input type="checkbox" id="NewSubmission:comprehensive"> Comprehensive</label>
      </div></div>
      <button id="NewSubmission:Quote" data-action="quote">Quote</button>
      <button class="secondary" data-nav="#/desktop" id="NewSubmission:Cancel">Cancel</button>`;
  };

  views.quote = (parts) => {
    const s = S.subs[parts[1]]; if (!s) return notFound('Submission');
    const u = user();
    const canBind = s.status === 'Quoted' || s.status === 'Approved';
    const canDecide = u.role === 'Underwriter' && s.status === 'Referred';
    const issues = s.issues.length ? s.issues.map((i) => `<li>${esc(i)}</li>`).join('') : '<li>No underwriting issues</li>';
    return `<h2 id="QuoteScreen:Title">Quote</h2>
      <div class="card">${kv([
        ['QuoteScreen:submissionNumber', 'Submission #', s.id], ['QuoteScreen:status', 'Status', s.status],
        ['QuoteScreen:insured', 'Insured', s.first + ' ' + s.last], ['QuoteScreen:driverAge', 'Driver Age', s.age],
        ['QuoteScreen:state', 'State', s.state],
        ['QuoteScreen:effective', 'Effective', fmtD(s.eff)], ['QuoteScreen:expiration', 'Expiration', fmtD(s.exp)],
        ['QuoteScreen:paymentPlan', 'Payment Plan', s.plan],
        ['QuoteScreen:vehicle', 'Vehicle', `${s.vehicle.year} ${s.vehicle.make} ${s.vehicle.model}`],
        ['QuoteScreen:liability', 'Liability', s.liability], ['QuoteScreen:collision', 'Collision Deductible', s.collision],
        ['QuoteScreen:comprehensive', 'Comprehensive', s.comprehensive ? 'Yes' : 'No'],
        ['QuoteScreen:premium', 'Total Premium', money(s.premium)]])}</div>
      <div class="card"><h3>Underwriting Issues</h3><ul id="QuoteScreen:UWIssues">${issues}</ul></div>
      ${s.policy ? `<p>Policy issued: <a id="QuoteScreen:PolicyLink" href="#/policy/${s.policy}">${s.policy}</a></p>` : ''}
      <button id="QuoteScreen:Bind" data-action="bind" data-id="${s.id}" ${canBind ? '' : 'disabled'}>Bind Options &gt; Issue Policy</button>
      ${canDecide ? `<button id="QuoteScreen:Approve" data-action="uwApprove" data-id="${s.id}">Approve</button>
                     <button id="QuoteScreen:Reject" class="danger" data-action="uwReject" data-id="${s.id}">Reject</button>` : ''}`;
  };

  views.policy = (parts) => {
    const p = S.pols[parts[1]]; if (!p) return notFound('Policy');
    if (parts[2] === 'change') return changeView(p);
    if (parts[2] === 'cancel') return cancelView(p);
    const inForce = p.status === 'In Force';
    const veh = p.vehicles.map((x) => `<tr class="vehicle-row"><td>${x.year}</td><td>${x.make}</td><td>${esc(x.model)}</td><td>${money(x.premium)}</td></tr>`).join('');
    const tx = p.tx.map((x) => `<tr class="tx-row"><td>${x.type}</td><td>${fmtD(x.eff)}</td><td>${money(x.amount)}</td></tr>`).join('');
    const cancelRows = p.cancel ? [['PolicySummary:cancelDate', 'Cancellation Date', fmtD(p.cancel.date)],
      ['PolicySummary:cancelMethod', 'Cancellation Method', p.cancel.method], ['PolicySummary:refund', 'Return Premium', money(p.cancel.refund)]] : [];
    return `<h2 id="PolicySummary:Title">Policy Summary</h2>
      <div class="card">${kv([
        ['PolicySummary:policyNumber', 'Policy #', p.number], ['PolicySummary:status', 'Status', p.status],
        ['PolicySummary:insured', 'Insured', p.insured], ['PolicySummary:state', 'State', p.state],
        ['PolicySummary:term', 'Term #', p.term],
        ['PolicySummary:effective', 'Effective', fmtD(p.eff)], ['PolicySummary:expiration', 'Expiration', fmtD(p.exp)],
        ['PolicySummary:premium', 'Total Premium', money(p.premium)]].concat(cancelRows))}</div>
      <div class="card"><h3>Vehicles</h3><table id="PolicySummary:Vehicles"><thead><tr><th>Year</th><th>Make</th><th>Model</th><th>Premium</th></tr></thead><tbody>${veh}</tbody></table></div>
      <div class="card"><h3>Transactions</h3><table id="PolicySummary:Transactions"><thead><tr><th>Type</th><th>Effective</th><th>Premium Change</th></tr></thead><tbody>${tx}</tbody></table></div>
      <button id="PolicySummary:ChangePolicy" data-nav="#/policy/${p.number}/change" ${inForce ? '' : 'disabled'}>Change Policy</button>
      <button id="PolicySummary:CancelPolicy" class="danger" data-nav="#/policy/${p.number}/cancel" ${inForce ? '' : 'disabled'}>Cancel Policy</button>
      <button id="PolicySummary:RenewPolicy" data-action="renew" data-id="${p.number}" ${inForce ? '' : 'disabled'}>Renew Policy</button>
      <button id="PolicySummary:FileClaim" class="secondary" data-nav="#/claim/new/${p.number}">Report Claim (FNOL)</button>
      <button id="PolicySummary:ViewBilling" class="secondary" data-nav="#/billing/${p.number}">View Billing</button>`;
  };

  function changeView(p) {
    return `<h2 id="PolicyChange:Title">Change Policy ${p.number} - Add Vehicle</h2>
      <div class="card"><div class="grid">
        <label>Change Effective Date <input id="PolicyChange:effectiveDate" value="${fmtD(todayT())}"></label><span></span>
        <label>Vehicle Year <input id="PolicyChange:vehicleYear"></label>
        <label>Make <select id="PolicyChange:vehicleMake">${fmtOpts(['Toyota', 'Honda', 'Ford', 'BMW'], 'Toyota')}</select></label>
        <label>Model <input id="PolicyChange:vehicleModel"></label>
      </div><p class="hint">Premium is pro-rated for the remaining days of the policy period.</p></div>
      <button id="PolicyChange:Submit" data-action="change" data-id="${p.number}">Quote &amp; Issue Change</button>
      <button id="PolicyChange:Back" class="secondary" data-nav="#/policy/${p.number}">Back</button>`;
  }

  function cancelView(p) {
    return `<h2 id="CancelPolicy:Title">Cancel Policy ${p.number}</h2>
      <div class="card"><div class="grid">
        <label>Reason <select id="CancelPolicy:reason">${fmtOpts(['Insured Request', 'Non-Payment', 'Underwriting Reasons', 'Vehicle Sold'], 'Insured Request')}</select></label>
        <label>Refund Method <select id="CancelPolicy:method">${fmtOpts(['Pro Rata', 'Short Rate', 'Flat'], 'Pro Rata')}</select></label>
        <label>Cancellation Effective Date <input id="CancelPolicy:effectiveDate" value="${fmtD(todayT())}"></label>
      </div><p class="hint">Flat cancellation is effective from inception and refunds 100% of premium. Short rate keeps a 10% penalty on unearned premium.</p></div>
      <button id="CancelPolicy:Submit" class="danger" data-action="cancel" data-id="${p.number}">Start Cancellation</button>
      <button id="CancelPolicy:Back" class="secondary" data-nav="#/policy/${p.number}">Back</button>`;
  }

  views.claim = (parts) => {
    if (parts[1] === 'new') return fnolView(parts[2]);
    const c = S.clms[parts[1]]; if (!c) return notFound('Claim');
    const u = user();
    const pay = c.payments.map((x) => `<tr class="payment-row"><td>${x.no}</td><td>${money(x.gross)}</td><td>${money(x.deductible)}</td><td>${money(x.net)}</td><td>${x.status}</td></tr>`).join('');
    const resStatus = c.pending ? 'Pending Approval' : (c.reserve > 0 || c.grossPaid > 0 ? 'Approved' : 'Not Set');
    return `<h2 id="ClaimSummary:Title">Claim Summary</h2>
      <div class="card">${kv([
        ['ClaimSummary:claimNumber', 'Claim #', c.no], ['ClaimSummary:status', 'Status', c.status],
        ['ClaimSummary:policyNumber', 'Policy #', c.pol], ['ClaimSummary:lossDate', 'Loss Date', fmtD(c.loss)],
        ['ClaimSummary:lossType', 'Loss Type', c.lossType], ['ClaimSummary:severity', 'Severity', c.severity],
        ['ClaimSummary:adjuster', 'Assigned Adjuster', c.adjuster], ['ClaimSummary:exposureType', 'Exposure', c.exposure],
        ['ClaimSummary:deductible', 'Deductible', money(c.deductible)],
        ['ClaimSummary:reserve', 'Open Reserve', money(c.reserve)], ['ClaimSummary:reserveStatus', 'Reserve Status', resStatus],
        ['ClaimSummary:paid', 'Total Paid (net)', money(c.paid)]])}</div>
      <div class="card"><h3>Financials</h3><div class="grid">
        <label>Reserve Amount <input id="ClaimSummary:reserveAmount"></label>
        <label>Payment Amount (gross) <input id="ClaimSummary:paymentAmount"></label>
      </div><br>
        <button id="ClaimSummary:SetReserve" data-action="setReserve" data-id="${c.no}">Set Reserve</button>
        ${c.pending && u.role === 'Claims Supervisor' ? `<button id="ClaimSummary:ApproveReserve" data-action="approveReserve" data-id="${c.no}">Approve Reserve (${money(c.pending)})</button>` : ''}
        <button id="ClaimSummary:MakePayment" data-action="pay" data-id="${c.no}">Make Payment</button>
        <button id="ClaimSummary:CloseClaim" class="danger" data-action="closeClaim" data-id="${c.no}">Close Claim</button>
      </div>
      <div class="card"><h3>Payments</h3><table id="ClaimSummary:Payments"><thead><tr><th>#</th><th>Gross</th><th>Deductible</th><th>Net</th><th>Status</th></tr></thead><tbody>${pay}</tbody></table></div>`;
  };

  function fnolView(prefPol) {
    const step1 = `<div class="card"><h3>Step 1 - Find Policy &amp; Verify Coverage</h3><div class="grid">
        <label>Policy Number <input id="FNOL:policyNumber" value="${esc(fnol ? fnol.pol : (prefPol || ''))}"></label>
        <label>Date of Loss (MM/DD/YYYY) <input id="FNOL:lossDate" value="${esc(fnol ? fnol.loss : fmtD(todayT()))}"></label>
      </div><br><button id="FNOL:Search" data-action="fnolSearch">Search / Verify Policy</button></div>`;
    let step2 = '';
    if (fnol && S.pols[fnol.pol]) {
      const p = S.pols[fnol.pol];
      step2 = `<div class="card"><p id="FNOL:CoverageStatus" class="badge">Policy verified - ${p.status} on date of loss</p>
        <h3>Step 2 - Loss Details</h3><div class="grid">
        <label>Loss Type <select id="FNOL:lossType">${fmtOpts(LOSS_TYPES, 'Collision')}</select></label>
        <label>Vehicle <select id="FNOL:vehicle">${p.vehicles.map((x) => `<option>${x.year} ${x.make} ${esc(x.model)}</option>`).join('')}</select></label>
        <label>Loss Description <textarea id="FNOL:description" rows="3"></textarea></label>
        <label class="inline"><input type="checkbox" id="FNOL:injuries"> Injuries reported</label>
      </div><br><button id="FNOL:Finish" data-action="fnolFinish">Finish - Create Claim</button></div>`;
    }
    return `<h2 id="FNOL:Title">First Notice of Loss</h2>${step1}${step2}`;
  }

  views.billing = (parts) => {
    if (!parts[1]) {
      return `<h2 id="Billing:Title">Billing Center</h2><div class="card"><div class="grid">
        <label>Policy Number <input id="Billing:searchPolicyNumber"></label></div><br>
        <button id="Billing:Search" data-action="billingSearch">Find Account</button></div>`;
    }
    const p = S.pols[parts[1]]; if (!p) return notFound('Billing account');
    const invs = invsFor(p.number);
    const billed = round2(invs.reduce((a, i) => a + i.amount, 0)), paid = round2(invs.reduce((a, i) => a + i.paid, 0));
    const bal = round2(billed - paid);
    const st = bal <= 0 ? 'Paid in Full' : paid > 0 ? 'Partially Paid' : 'Unpaid';
    const rows = invs.map((i) => `<tr class="invoice-row"><td>${i.no}</td><td>${esc(i.desc)}</td><td>${fmtD(i.due)}</td><td>${money(i.amount)}</td><td>${money(i.paid)}</td><td>${invStatus(i)}</td></tr>`).join('');
    return `<h2 id="Billing:Title">Billing Account</h2>
      <div class="card">${kv([['Billing:account', 'Policy #', p.number], ['Billing:totalBilled', 'Total Billed', money(billed)],
        ['Billing:totalPaid', 'Total Paid', money(paid)], ['Billing:balance', 'Outstanding Balance', money(bal)], ['Billing:status', 'Billing Status', st]])}</div>
      <div class="card"><h3>Invoices</h3><table id="Billing:Invoices"><thead><tr><th>Invoice</th><th>Description</th><th>Due</th><th>Amount</th><th>Paid</th><th>Status</th></tr></thead><tbody>${rows}</tbody></table></div>
      <div class="card"><h3>Make Payment</h3><div class="grid"><label>Amount <input id="Billing:paymentAmount"></label>
        <label>Method <select id="Billing:paymentMethod">${fmtOpts(['Check', 'Credit Card', 'ACH'], 'Check')}</select></label></div><br>
        <button id="Billing:MakePayment" data-action="billPay" data-id="${p.number}">Submit Payment</button></div>`;
  };

  views.search = () => `<h2 id="Search:Title">Policy Search</h2>
    <div class="card"><div class="grid"><label>Policy Number <input id="Search:policyNumber"></label>
      <label>Insured Last Name <input id="Search:lastName"></label></div><br>
      <button id="Search:Run" data-action="search">Search</button></div>
    <div id="Search:Panel">${searchHtml}</div>`;

  /* ------------------------------------------------------------------ actions */
  const actions = {};

  actions.login = () => {
    S = load();
    const u = v('Login:LoginScreen:LoginDV:username'), pw = $('Login:LoginScreen:LoginDV:password').value;
    if (!USERS[u] || pw !== PASSWORD) return fail('Invalid username or password');
    S.session = u; save(); go('#/desktop');
  };
  actions.logout = () => { S = load(); S.session = null; save(); go('#/login'); render(); };

  actions.quote = () => {
    S = load();
    const first = v('NewSubmission:firstName'), last = v('NewSubmission:lastName');
    if (!first || !last) return fail('First name and last name are required');
    const dob = parseD(v('NewSubmission:dob'));
    if (dob == null) return fail('Date of birth must be in MM/DD/YYYY format');
    const eff = parseD(v('NewSubmission:effectiveDate'));
    if (eff == null) return fail('Effective date must be in MM/DD/YYYY format');
    if (eff < todayT()) return fail('Effective date cannot be in the past');
    const age = ageOn(dob, eff);
    if (age < 18) return fail('Driver must be at least 18 years old to be quoted');
    const year = parseInt(v('NewSubmission:vehicleYear'), 10), maxY = new Date().getFullYear() + 1;
    if (!(year >= 1990 && year <= maxY)) return fail('Vehicle year must be between 1990 and ' + maxY);
    if (!v('NewSubmission:vehicleModel')) return fail('Vehicle model is required');
    const term = parseInt(v('NewSubmission:term'), 10), violations = parseInt(v('NewSubmission:violations'), 10);
    const s = { id: 'SUB-' + (++S.seq.sub), first, last, age, dob, state: v('NewSubmission:state'), eff, term, exp: addMonths(eff, term),
      plan: v('NewSubmission:paymentPlan'), violations, liability: v('NewSubmission:liability'), collision: v('NewSubmission:collision'),
      comprehensive: checked('NewSubmission:comprehensive'), vehicle: { year, make: v('NewSubmission:vehicleMake'), model: v('NewSubmission:vehicleModel') },
      createdBy: S.session };
    s.premium = rate({ age, state: s.state, liability: s.liability, collision: s.collision, comprehensive: s.comprehensive, violations, term, vehicleYear: year, effYear: new Date(eff).getUTCFullYear() });
    s.issues = uwIssues(age, violations);
    s.status = s.issues.length ? 'Referred' : 'Quoted';
    if (s.issues.length) {
      S.acts.push({ id: S.acts.length + 1, role: 'Underwriter', type: 'UW Referral', subject: `Review referred submission ${s.id} (${first} ${last})`, link: '#/quote/' + s.id, status: 'Open', ref: s.id });
    }
    S.subs[s.id] = s; save(); go('#/quote/' + s.id);
  };

  const closeActs = (ref) => S.acts.forEach((a) => { if (a.ref === ref) a.status = 'Closed'; });

  actions.uwApprove = (el) => { S = load(); const s = S.subs[el.dataset.id]; s.status = 'Approved'; s.approvedBy = S.session; closeActs(s.id); save(); render(); ok('Submission approved by underwriter'); };
  actions.uwReject = (el) => { S = load(); const s = S.subs[el.dataset.id]; s.status = 'Rejected'; closeActs(s.id); save(); render(); fail('Submission rejected by underwriter'); };

  actions.bind = (el) => {
    S = load(); const s = S.subs[el.dataset.id];
    const number = 'PA-' + (++S.seq.pol);
    S.pols[number] = { number, insured: s.first + ' ' + s.last, state: s.state, term: 1, months: s.term, eff: s.eff, exp: s.exp, status: 'In Force', plan: s.plan,
      age: s.age, violations: s.violations, coverages: { liability: s.liability, collision: s.collision, comprehensive: s.comprehensive },
      vehicles: [Object.assign({}, s.vehicle, { premium: s.premium })], premium: s.premium, terms: [{ eff: s.eff, exp: s.exp }],
      tx: [{ type: 'Issuance', eff: s.eff, amount: s.premium }], cancel: null, sub: s.id };
    s.status = 'Bound'; s.policy = number;
    createInvoices(number, s.premium, s.eff, s.plan, 'Premium - Issuance');
    save(); go('#/policy/' + number);
  };

  actions.change = (el) => {
    S = load(); const p = S.pols[el.dataset.id];
    const eff = parseD(v('PolicyChange:effectiveDate'));
    if (eff == null) return fail('Effective date must be in MM/DD/YYYY format');
    if (eff < p.eff || eff > p.exp) return fail('Change effective date must be within the policy period');
    const year = parseInt(v('PolicyChange:vehicleYear'), 10), maxY = new Date().getFullYear() + 1;
    if (!(year >= 1990 && year <= maxY)) return fail('Vehicle year must be between 1990 and ' + maxY);
    if (!v('PolicyChange:vehicleModel')) return fail('Vehicle model is required');
    const full = rate({ age: p.age, state: p.state, liability: p.coverages.liability, collision: p.coverages.collision, comprehensive: p.coverages.comprehensive,
      violations: p.violations, term: p.months, vehicleYear: year, effYear: new Date(p.eff).getUTCFullYear() });
    const amount = round2(full * daysBetween(eff, p.exp) / daysBetween(p.eff, p.exp));
    p.vehicles.push({ year, make: v('PolicyChange:vehicleMake'), model: v('PolicyChange:vehicleModel'), premium: amount });
    p.premium = round2(p.premium + amount);
    p.tx.push({ type: 'Endorsement', eff, amount });
    createInvoices(p.number, amount, eff, 'Full Pay', 'Premium - Endorsement');
    save(); go('#/policy/' + p.number);
  };

  actions.cancel = (el) => {
    S = load(); const p = S.pols[el.dataset.id];
    const method = v('CancelPolicy:method'); let c = parseD(v('CancelPolicy:effectiveDate'));
    if (method === 'Flat') c = p.eff;
    else if (c == null) return fail('Cancellation date must be in MM/DD/YYYY format');
    if (c < p.eff || c > p.exp) return fail('Cancellation date must be within the policy period');
    if (!window.confirm('Are you sure you want to cancel policy ' + p.number + '?')) return; // JS confirm -> Selenium Alert handling
    const frac = daysBetween(c, p.exp) / daysBetween(p.eff, p.exp);
    const refund = method === 'Flat' ? p.premium : method === 'Short Rate' ? round2(p.premium * frac * 0.9) : round2(p.premium * frac);
    p.status = 'Cancelled'; p.cancel = { date: c, reason: v('CancelPolicy:reason'), method, refund };
    p.tx.push({ type: 'Cancellation', eff: c, amount: -refund });
    save(); go('#/policy/' + p.number);
  };

  actions.renew = (el) => {
    S = load(); const p = S.pols[el.dataset.id];
    const eff = p.exp, exp = addMonths(eff, p.months), prem = round2(p.premium * 1.05);
    p.term += 1; p.eff = eff; p.exp = exp; p.premium = prem; p.terms.push({ eff, exp });
    p.tx.push({ type: 'Renewal', eff, amount: prem });
    createInvoices(p.number, prem, eff, 'Full Pay', 'Premium - Renewal');
    save(); render(); ok('Policy renewed for term ' + p.term);
  };

  function verifyPolicy(polNo, lossStr) {
    const p = S.pols[polNo.toUpperCase()];
    if (!p) return { err: 'Policy not found: ' + polNo };
    const l = parseD(lossStr);
    if (l == null) return { err: 'Date of loss must be in MM/DD/YYYY format' };
    if (l > todayT()) return { err: 'Date of loss cannot be in the future' };
    const inTerm = p.terms.some((t) => l >= t.eff && l <= t.exp);
    const cancelled = p.cancel && l >= p.cancel.date;
    if (!inTerm || cancelled) return { err: 'Policy is not in force on the date of loss' };
    return { p, l };
  }
  actions.fnolSearch = () => {
    S = load(); fnol = null;
    const r = verifyPolicy(v('FNOL:policyNumber'), v('FNOL:lossDate'));
    if (r.err) return fail(r.err);
    fnol = { pol: r.p.number, loss: v('FNOL:lossDate') };
    render();
  };
  function coverageFor(p, lossType) {
    if (lossType === 'Collision') return { ok: true, ded: num(p.coverages.collision), exp: 'Vehicle Damage' };
    if (lossType === 'Comprehensive - Theft' || lossType === 'Glass Breakage') {
      return p.coverages.comprehensive ? { ok: true, ded: 250, exp: lossType === 'Glass Breakage' ? 'Glass Damage' : 'Vehicle Damage' } : { ok: false };
    }
    return { ok: true, ded: 0, exp: 'Third Party Property Damage' };
  }
  actions.fnolFinish = () => {
    S = load();
    const r = verifyPolicy(fnol.pol, fnol.loss); if (r.err) return fail(r.err);
    const lossType = v('FNOL:lossType'), desc = v('FNOL:description');
    if (!desc) return fail('Loss description is required');
    const cov = coverageFor(r.p, lossType);
    if (!cov.ok) return fail('No applicable coverage found on the policy for loss type: ' + lossType);
    const no = 'CLM-' + (++S.seq.clm), injuries = checked('FNOL:injuries');
    S.clms[no] = { no, pol: r.p.number, loss: r.l, lossType, desc, vehicle: v('FNOL:vehicle'), injuries, severity: injuries ? 'Complex' : 'Standard',
      status: 'Open', adjuster: user().name, exposure: cov.exp, deductible: cov.ded, reserve: 0, pending: null, paid: 0, grossPaid: 0, dedApplied: false, payments: [] };
    save(); fnol = null; go('#/claim/' + no);
  };

  actions.setReserve = (el) => {
    S = load(); const c = S.clms[el.dataset.id];
    if (c.status === 'Closed') return fail('Claim is closed');
    const amt = num(v('ClaimSummary:reserveAmount'));
    if (!(amt > 0)) return fail('Enter a valid reserve amount');
    const auth = user().authority;
    if (auth === 0) return fail('You do not have authority to set reserves');
    if (amt > auth) {
      c.pending = amt;
      S.acts.push({ id: S.acts.length + 1, role: 'Claims Supervisor', type: 'Reserve Approval', subject: `Approve reserve of ${money(amt)} on ${c.no}`, link: '#/claim/' + c.no, status: 'Open', ref: c.no });
      save(); render(); return alertMsg(`Reserve exceeds your authority of ${money(auth)} - sent to supervisor for approval`, 'info');
    }
    c.reserve = amt; c.pending = null; save(); render(); ok('Reserve set to ' + money(amt));
  };
  actions.approveReserve = (el) => {
    S = load(); const c = S.clms[el.dataset.id];
    c.reserve = c.pending; c.pending = null; closeActs(c.no); save(); render(); ok('Reserve approved');
  };
  actions.pay = (el) => {
    S = load(); const c = S.clms[el.dataset.id];
    if (c.status === 'Closed') return fail('Claim is closed');
    const gross = num(v('ClaimSummary:paymentAmount'));
    if (!(gross > 0)) return fail('Enter a valid payment amount');
    if (gross > c.reserve) return fail('Payment exceeds the open reserve');
    const ded = c.dedApplied ? 0 : c.deductible;
    if (gross <= ded) return fail('Payment must exceed the deductible of ' + money(ded));
    const net = round2(gross - ded);
    c.payments.push({ no: 'PAY-' + (++S.seq.pay), gross, deductible: ded, net, status: 'Issued' });
    c.dedApplied = true; c.reserve = round2(c.reserve - gross); c.grossPaid = round2(c.grossPaid + gross); c.paid = round2(c.paid + net);
    save(); render(); ok('Payment issued: ' + money(net));
  };
  actions.closeClaim = (el) => {
    S = load(); const c = S.clms[el.dataset.id];
    if (c.reserve > 0 || c.pending) return fail('Cannot close a claim while reserves are open');
    c.status = 'Closed'; save(); render(); ok('Claim closed');
  };

  actions.billingSearch = () => {
    S = load(); const n = v('Billing:searchPolicyNumber').toUpperCase();
    if (!S.pols[n]) return fail('Policy not found: ' + n);
    go('#/billing/' + n);
  };
  actions.billPay = (el) => {
    S = load(); const n = el.dataset.id; const invs = invsFor(n);
    const amt = num(v('Billing:paymentAmount'));
    if (!(amt > 0)) return fail('Enter a valid payment amount');
    const bal = round2(invs.reduce((a, i) => a + i.amount - i.paid, 0));
    if (amt > bal) return fail('Payment exceeds the outstanding balance of ' + money(bal));
    let rem = amt;
    for (const i of invs) {
      const due = round2(i.amount - i.paid); if (due <= 0) continue;
      const x = Math.min(due, rem); i.paid = round2(i.paid + x); rem = round2(rem - x); if (rem <= 0) break;
    }
    save(); render(); ok('Payment of ' + money(amt) + ' received');
  };

  actions.search = () => {
    S = load();
    const n = v('Search:policyNumber').toUpperCase(), ln = v('Search:lastName').toLowerCase();
    const res = Object.values(S.pols).filter((p) => (!n || p.number === n) && (!ln || p.insured.toLowerCase().split(' ').pop().includes(ln)) && (n || ln));
    searchHtml = res.length
      ? `<div class="card"><table id="Search:Results"><thead><tr><th>Policy #</th><th>Insured</th><th>Status</th><th>Premium</th></tr></thead><tbody>${res.map((p) =>
          `<tr class="result-row"><td><a class="policy-link" href="#/policy/${p.number}">${p.number}</a></td><td>${esc(p.insured)}</td><td>${p.status}</td><td>${money(p.premium)}</td></tr>`).join('')}</tbody></table></div>`
      : '<div class="card"><p id="Search:NoResults">No policies found</p></div>';
    render();
  };

  /* ------------------------------------------------------------------ router */
  function render() {
    S = load();
    const parts = (location.hash || '#/login').replace(/^#\//, '').split('/');
    clearAlert();
    if (!S.session && parts[0] !== 'login') { go('#/login'); return; }
    if (S.session && parts[0] === 'login') { go('#/desktop'); return; }
    $('TabBar').hidden = !S.session;
    if (S.session) $('TabBar:UserInfo').textContent = `${USERS[S.session].name} (${USERS[S.session].role})`;
    const view = views[parts[0]];
    $('content').innerHTML = view ? view(parts) : notFound('Page');
  }
  window.addEventListener('hashchange', () => { fnol = null; searchHtml = ''; busy(render, 150); });
  document.addEventListener('click', (e) => {
    const el = e.target.closest('[data-action],[data-nav]');
    if (!el || el.disabled) return;
    e.preventDefault();
    if (el.dataset.nav) { go(el.dataset.nav); return; }
    busy(() => actions[el.dataset.action](el), 300);
  });
  if (!location.hash) { go('#/login'); } else { busy(render, 150); }
})();
