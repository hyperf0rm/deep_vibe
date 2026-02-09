import { useState } from 'react'

function App() {
  const [username, setUsername] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);


  const toUnix = (dateString: string) => {
    if (!dateString) return null;
    return Math.floor(new Date(dateString).getTime() / 1000);
  }

  const handleSync = async() => {

    const from = toUnix(dateFrom);
    const to = toUnix(dateTo);
    let url = `http://localhost:8080/users/sync/${username}`;
    if (from) url += `?from=${from}&`;
    if (to) url += `to=${to}`

    setStatusMessage(null);

    try {
      const response = await fetch(url, {method: 'GET'});
      const text = await response.text();
      setStatusMessage(`${text}`);
    }
    catch (e) {
      console.log('Error', e);
      setStatusMessage('Failed to connect to server');
    }
  }

const handleStopSync = async() => {
    const url = `http://localhost:8080/users/sync/${username}/stop`;
  try {
    const response = await fetch(url, { method: 'POST' });
    const text = await response.text();
    setStatusMessage(`${text}`);
  } catch (e) {
    setStatusMessage('Failed to stop sync');
  }
}

  return (
    <div style={{ padding: '300px' }}>
      <h1>The Project</h1>

      <div style={{ marginBottom: '10px' }}>
        <label>From: </label>
        <input 
          type="date" 
          value={dateFrom} 
          onChange={(e) => setDateFrom(e.target.value)} 
        />
        <label style={{ marginLeft: '10px' }}>To: </label>
        <input 
          type="date" 
          value={dateTo} 
          onChange={(e) => setDateTo(e.target.value)} 
        />
      </div>

      <div style={{ marginBottom: '10px' }}>
        <input
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Your Last.fm username"
        />
      </div>

      <button style={{ margin: '10px '}} onClick={handleSync} disabled={!username.trim()}>Sync</button>
      <button style={{ margin: '10px '}} onClick={handleStopSync}>Stop sync</button>

      {statusMessage && (
        <div style={{ marginTop: '20px', fontWeight: 'bold' }} >
          {statusMessage}
        </div>
      )}
    </div>
  )
}

export default App
