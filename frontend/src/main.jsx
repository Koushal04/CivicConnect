import React, { StrictMode, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api').replace(/\/$/, '');
const SESSION_KEY = 'civicconnect.citizen-session';
const CATEGORIES = ['POTHOLE', 'GARBAGE', 'WATER_SUPPLY', 'DRAINAGE', 'STREETLIGHT', 'OTHER'];

function readSession() {
  try {
    const value = localStorage.getItem(SESSION_KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

async function request(path, { token, ...options } = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    const error = new Error(payload?.message || `Request failed with status ${response.status}`);
    error.status = response.status;
    throw error;
  }

  return response.status === 204 ? null : response.json();
}

function App() {
  const [session, setSession] = useState(readSession);
  const [mode, setMode] = useState('login');
  const [registeredEmail, setRegisteredEmail] = useState('');

  function signIn(nextSession) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }

  function signOut() {
    localStorage.removeItem(SESSION_KEY);
    setSession(null);
  }

  return (
    <main className="landing-page">
      <section className="app-shell" aria-labelledby="page-title">
        <header className="hero">
          <p className="eyebrow">Public grievance reporting</p>
          <h1 id="page-title">CivicConnect</h1>
          <p className="summary">Report civic issues and follow their resolution.</p>
        </header>
        {session?.role === 'ADMIN' ? (
          <AdminDashboard session={session} onSignOut={signOut} />
        ) : session?.role === 'OFFICER' ? (
          <OfficerDashboard session={session} onSignOut={signOut} />
        ) : session?.role === 'CITIZEN' ? (
          <CitizenDashboard session={session} onSignOut={signOut} />
        ) : session ? (
          <section className="panel">
            <p>Your session is missing role information. Please sign in again.</p>
            <button className="primary" type="button" onClick={signOut}>Sign in again</button>
          </section>
        ) : (
          <AuthPanel mode={mode} onModeChange={setMode} onSignedIn={signIn}
            registeredEmail={registeredEmail} onRegistered={setRegisteredEmail} />
        )}
      </section>
    </main>
  );
}

function AuthPanel({ mode, onModeChange, onSignedIn, registeredEmail, onRegistered }) {
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);

  
async function submitLogin(event) {
  event.preventDefault();
  const data = new FormData(event.currentTarget);
  setBusy(true);
  setError('');

  try {
    const email = data.get('email');

    const result = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email,
        password: data.get('password'),
      }),
    });

    const profile = await request('/auth/me', {
      token: result.accessToken,
    });

    onSignedIn({
      token: result.accessToken,
      email: profile.email,
      role: profile.role,
    });
  } catch (requestError) {
    setError(requestError.message);
  } finally {
    setBusy(false);
  }
}


  async function submitRegistration(event) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const form = event.currentTarget;
    const phone = data.get('phone').trim();
    setBusy(true);
    setError('');
    setNotice('');
    try {
      await request('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName: data.get('fullName'), email: data.get('email'), password: data.get('password'),
          ...(phone ? { phone } : {}),
        }),
      });
      onRegistered(data.get('email'));
      setNotice('Registration complete. Sign in with your new account.');
      form.reset();
      onModeChange('login');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="panel auth-panel" aria-live="polite">
      <div className="tab-list" role="tablist" aria-label="Account access">
        <button className={mode === 'login' ? 'active' : ''} onClick={() => onModeChange('login')} type="button">Sign in</button>
        <button className={mode === 'register' ? 'active' : ''} onClick={() => onModeChange('register')} type="button">Register</button>
      </div>
      {notice && <p className="notice">{notice}</p>}
      {error && <p className="error" role="alert">{error}</p>}
      {mode === 'login' ? (
        <form className="form-grid" onSubmit={submitLogin}>
          <label>Email<input name="email" type="email" defaultValue={registeredEmail} autoComplete="email" required maxLength="254" /></label>
          <label>Password<input name="password" type="password" autoComplete="current-password" required maxLength="72" /></label>
          <button className="primary" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        </form>
      ) : (
        <form className="form-grid" onSubmit={submitRegistration}>
          <label>Full name<input name="fullName" autoComplete="name" required maxLength="120" /></label>
          <label>Email<input name="email" type="email" autoComplete="email" required maxLength="254" /></label>
          <label>Phone (optional)<input name="phone" type="tel" autoComplete="tel" maxLength="30" /></label>
          <label>Password<input name="password" type="password" autoComplete="new-password" minLength="12" maxLength="72" required /></label>
          <button className="primary" disabled={busy}>{busy ? 'Registering…' : 'Create citizen account'}</button>
        </form>
      )}
    </section>
  );
}

function CitizenDashboard({ session, onSignOut }) {
  const [grievances, setGrievances] = useState([]);
  const [selected, setSelected] = useState(null);
  const [trackingId, setTrackingId] = useState('');
  const [listState, setListState] = useState({ loading: true, error: '' });
  const [formState, setFormState] = useState({ busy: false, error: '', success: '' });
  const [trackingState, setTrackingState] = useState({ busy: false, error: '' });

  async function loadGrievances() {
    setListState({ loading: true, error: '' });
    try {
      setGrievances(await request('/grievances', { token: session.token }));
    } catch (requestError) {
      setListState({ loading: false, error: requestError.message });
      return;
    }
    setListState({ loading: false, error: '' });
  }

  useEffect(() => { loadGrievances(); }, []);

  async function submitGrievance(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const latitude = data.get('latitude').trim();
    const longitude = data.get('longitude').trim();
    setFormState({ busy: true, error: '', success: '' });
    try {
      const grievance = await request('/grievances', {
        method: 'POST', token: session.token,
        body: JSON.stringify({
          category: data.get('category'), title: data.get('title'), description: data.get('description'), address: data.get('address'),
          ...(latitude ? { latitude: Number(latitude) } : {}),
          ...(longitude ? { longitude: Number(longitude) } : {}),
        }),
      });
      form.reset();
      setFormState({ busy: false, error: '', success: `Submitted successfully. Tracking ID: ${grievance.trackingId}` });
      setSelected(grievance);
      setTrackingId(grievance.trackingId);
      await loadGrievances();
    } catch (requestError) {
      setFormState({ busy: false, error: requestError.message, success: '' });
    }
  }

  async function findGrievance(event) {
    event.preventDefault();
    if (!trackingId.trim()) return;
    setTrackingState({ busy: true, error: '' });
    try {
      setSelected(await request(`/grievances/${encodeURIComponent(trackingId.trim())}`, { token: session.token }));
      setTrackingState({ busy: false, error: '' });
    } catch (requestError) {
      setSelected(null);
      setTrackingState({ busy: false, error: requestError.message });
    }
  }

  return (
    <section className="dashboard">
      <div className="dashboard-header">
        <div><p className="eyebrow">Citizen workspace</p><p className="account">Signed in as {session.email}</p></div>
        <button className="secondary" type="button" onClick={onSignOut}>Sign out</button>
      </div>
      <div className="dashboard-grid">
        <section className="panel">
          <h2>Report an issue</h2>
          {formState.error && <p className="error" role="alert">{formState.error}</p>}
          {formState.success && <p className="notice">{formState.success}</p>}
          <form className="form-grid" onSubmit={submitGrievance}>
            <label>Category<select name="category" required>{CATEGORIES.map((category) => <option key={category} value={category}>{category.replace('_', ' ')}</option>)}</select></label>
            <label>Title<input name="title" required maxLength="160" /></label>
            <label>Description<textarea name="description" required maxLength="5000" rows="4" /></label>
            <label>Address<input name="address" required maxLength="500" /></label>
            <div className="coordinate-row"><label>Latitude (optional)<input name="latitude" type="number" min="-90" max="90" step="any" /></label><label>Longitude (optional)<input name="longitude" type="number" min="-180" max="180" step="any" /></label></div>
            <button className="primary" disabled={formState.busy}>{formState.busy ? 'Submitting…' : 'Submit grievance'}</button>
          </form>
        </section>
        <section className="panel">
          <h2>Track a grievance</h2>
          <form className="search-row" onSubmit={findGrievance}>
            <input value={trackingId} onChange={(event) => setTrackingId(event.target.value)} placeholder="CC-…" aria-label="Tracking ID" required />
            <button className="secondary" disabled={trackingState.busy}>{trackingState.busy ? 'Checking…' : 'View'}</button>
          </form>
          {trackingState.error && <p className="error" role="alert">{trackingState.error}</p>}
          {selected && <GrievanceCard grievance={selected} />}
          <h2 className="list-heading">My grievances</h2>
          {listState.loading && <p>Loading your grievances…</p>}
          {listState.error && <p className="error" role="alert">{listState.error}</p>}
          {!listState.loading && !listState.error && grievances.length === 0 && <p>No grievances submitted yet.</p>}
          <div className="grievance-list">{grievances.map((grievance) => <button className="grievance-link" type="button" key={grievance.trackingId} onClick={() => { setTrackingId(grievance.trackingId); setSelected(grievance); }}><strong>{grievance.title}</strong><span>{grievance.trackingId} · {grievance.status.replace('_', ' ')}</span></button>)}</div>
        </section>
      </div>
    </section>
  );
}


function DashboardHeader({ session, title, onSignOut }) {
  return (
    <div className="dashboard-header">
      <div>
        <p className="eyebrow">{title}</p>
        <p className="account">Signed in as {session.email}</p>
      </div>
      <button className="secondary" type="button" onClick={onSignOut}>Sign out</button>
    </div>
  );
}

function AdminDashboard({ session, onSignOut }) {
  const [trackingId, setTrackingId] = useState('');
  const [departmentId, setDepartmentId] = useState('1');
  const [officerId, setOfficerId] = useState('4');
  const [state, setState] = useState({ busy: false, error: '', success: '' });

  async function assignGrievance(event) {
    event.preventDefault();
    setState({ busy: true, error: '', success: '' });
    try {
      await request(`/admin/grievances/${encodeURIComponent(trackingId.trim())}/assignment`, {
        method: 'PATCH',
        token: session.token,
        body: JSON.stringify({
          departmentId: Number(departmentId),
          ...(officerId.trim() ? { officerId: Number(officerId) } : {}),
        }),
      });
      setState({ busy: false, error: '', success: `Assignment updated for ${trackingId.trim()}.` });
    } catch (error) {
      setState({ busy: false, error: error.message, success: '' });
    }
  }

  return (
    <section className="dashboard">
      <DashboardHeader session={session} title="Admin workspace" onSignOut={onSignOut} />
      <div className="dashboard-grid">
        <section className="panel">
          <h2>Assign a grievance</h2>
          <p>Assign a grievance to an active department and an officer belonging to that department.</p>
          {state.error && <p className="error" role="alert">{state.error}</p>}
          {state.success && <p className="notice">{state.success}</p>}
          <form className="form-grid" onSubmit={assignGrievance}>
            <label>Grievance tracking ID
              <input value={trackingId} onChange={(event) => setTrackingId(event.target.value)} placeholder="CC-…" required />
            </label>
            <label>Department ID
              <input type="number" min="1" value={departmentId} onChange={(event) => setDepartmentId(event.target.value)} required />
            </label>
            <label>Officer user ID
              <input type="number" min="1" value={officerId} onChange={(event) => setOfficerId(event.target.value)} required />
            </label>
            <button className="primary" disabled={state.busy}>{state.busy ? 'Assigning…' : 'Assign grievance'}</button>
          </form>
        </section>
        <section className="panel">
          <h2>Administration</h2>
          <p>Route grievances to the appropriate department and officer.</p>
          <p className="muted">Default IDs match the local demo setup: department 1 and officer user 4. Change them if your database IDs differ.</p>
        </section>
      </div>
    </section>
  );
}

function OfficerDashboard({ session, onSignOut }) {
  const [grievances, setGrievances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [notes, setNotes] = useState({});

  async function loadGrievances() {
    setLoading(true);
    setError('');
    try {
      const result = await request('/officers/me/grievances', { token: session.token });
      setGrievances(Array.isArray(result) ? result : []);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadGrievances(); }, [session.token]);

  async function updateStatus(trackingId, status) {
    setError('');
    setMessage('');
    const note = (notes[trackingId] || '').trim();
    if (status === 'RESOLVED' && !note) {
      setError('Enter a resolution note before resolving this grievance.');
      return;
    }

    try {
      await request(`/officers/me/grievances/${encodeURIComponent(trackingId)}/status`, {
        method: 'PATCH',
        token: session.token,
        body: JSON.stringify({ status, note }),
      });
      setMessage(`${trackingId} updated to ${status}.`);
      await loadGrievances();
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  return (
    <section className="dashboard">
      <DashboardHeader session={session} title="Officer workspace" onSignOut={onSignOut} />
      <section className="panel">
        <h2>Assigned grievances</h2>
        {error && <p className="error" role="alert">{error}</p>}
        {message && <p className="notice">{message}</p>}
        {loading && <p>Loading assigned grievances…</p>}
        {!loading && !error && grievances.length === 0 && <p>No grievances are currently assigned to you.</p>}
        <div className="grievance-list">
          {grievances.map((grievance) => (
            <article className="grievance-card" key={grievance.trackingId}>
              <h3>{grievance.title}</h3>
              <p className="status">{grievance.status?.replaceAll('_', ' ')}</p>
              <p><strong>Tracking ID:</strong> {grievance.trackingId}</p>
              <p><strong>Category:</strong> {grievance.category}</p>
              <p>{grievance.description}</p>
              <p className="muted">{grievance.address}</p>
              <label>Resolution note
                <textarea rows="3" value={notes[grievance.trackingId] || ''}
                  onChange={(event) => setNotes((previous) => ({ ...previous, [grievance.trackingId]: event.target.value }))}
                  placeholder="Describe the work completed" />
              </label>
              {grievance.status === 'ASSIGNED' && (
                <button className="primary" type="button" onClick={() => updateStatus(grievance.trackingId, 'IN_PROGRESS')}>Mark in progress</button>
              )}
              {['ASSIGNED', 'IN_PROGRESS'].includes(grievance.status) && (
                <button className="secondary" type="button" onClick={() => updateStatus(grievance.trackingId, 'RESOLVED')}>Mark resolved</button>
              )}
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

function GrievanceCard({ grievance }) {
  return <article className="grievance-card"><h3>{grievance.title}</h3><p className="status">{grievance.status.replace('_', ' ')}</p><dl><div><dt>Tracking ID</dt><dd>{grievance.trackingId}</dd></div><div><dt>Category</dt><dd>{grievance.category.replace('_', ' ')}</dd></div><div><dt>Priority</dt><dd>{grievance.priority}</dd></div><div><dt>Submitted</dt><dd>{new Date(grievance.createdAt).toLocaleString()}</dd></div></dl><p>{grievance.description}</p><p className="muted">{grievance.address}</p></article>;
}

createRoot(document.getElementById('root')).render(<StrictMode><App /></StrictMode>); 