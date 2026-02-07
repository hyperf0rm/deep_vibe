import { useState } from 'react'

function App() {
  const [username, setUsername] = useState('');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const handleSync = async() => {
    const url = `http://localhost:8080/users/sync/${username}`;

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
    const url = `http://localhost:8080/users/sync/${username}/stop`
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
      <input
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="Your Last.fm username"
      />
      <button onClick={handleSync} disabled={!username.trim()}>Sync</button>
      <button onClick={handleStopSync}>Stop sync</button>
      {statusMessage && (
        <div style={{ marginTop: '20px', fontWeight: 'bold' }} >
          {statusMessage}
        </div>
      )}
    </div>
  )
}

export default App
