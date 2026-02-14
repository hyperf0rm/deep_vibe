import { useState, useRef } from 'react'
import Header from './Header'


function App() {
  const [username, setUsername] = useState('');
  const [progress, setProgress] = useState(0);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const [activeTab, setActiveTab] = useState('Sync');

  const eventSourceRef = useRef<EventSource | null>(null);


  const toUnix = (dateString: string) => {
    if (!dateString) return null;
    return Math.floor(new Date(dateString).getTime() / 1000);
  }

  const subscribeToProgress = (username: string) => {

    if (eventSourceRef.current) {
      eventSourceRef.current.close();
    }

    const eventSource = new EventSource(`http://localhost:8080/users/sync/${username}/stream`);
    eventSource.onmessage = (event)=> {
      const progressPercentage = parseInt(event.data);
      setProgress(progressPercentage);
      if (progressPercentage >= 100) {
        setStatusMessage("Sync finished!");
        eventSource.close();
      }
    };

    eventSource.onerror = (err) => {
      console.error("Event Stream error:", err);
      eventSource.close();
    };

    return eventSource;

  }

  const handleSync = async() => {

    const from = toUnix(dateFrom);
    const to = toUnix(dateTo);
    const params = new URLSearchParams();
    if (from) params.append('from', from.toString());
    if (to) params.append('to', to.toString());
    const baseUrl = `http://localhost:8080/users/sync/${username}`;
    const fullUrl = params.toString() ? `${baseUrl}?${params.toString()}` : baseUrl;

    setStatusMessage(null);

    try {
      setProgress(0);
      setIsSyncing(true);
      subscribeToProgress(username);
      const response = await fetch(fullUrl, {method: 'GET'});
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
    setIsSyncing(false);
    setStatusMessage(`${text}`);
  } catch (e) {
    setStatusMessage('Failed to stop sync');
  }
}

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <Header activeTab={activeTab} setActiveTab={setActiveTab} />
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

      <button style={{ margin: '10px', width: '100px'}} onClick={handleSync} disabled={!username.trim()}>Sync</button>
      <button style={{ margin: '10px', width: '100px'}} onClick={handleStopSync}>Stop sync</button>

      { isSyncing && (<div style={{
      display: 'flex',
      alignItems: 'center',
      width: '100%',
      maxWidth: '600px',
      borderRadius: '8px',
      marginTop: '20px' }}>

        <span style={{ fontWeight: 'bold', minWidth: '70px' }}>
          Progress:
        </span>

        <div style={{
          width: `${progress}%`,
          height: '20px',
          backgroundColor: '#4caf50',
          borderRadius: '8px',
          transition: 'width 0.3s ease-in-out',
          textAlign: 'center',
          color: 'white',
          fontSize: '12px',
          lineHeight: '20px'
        }}>
          {progress}%
        </div>
      </div>
      )}

      {statusMessage && (
        <div style={{ marginTop: '20px', fontWeight: 'bold' }} >
          {statusMessage}
        </div>
      )}
    </div>
  )
}

export default App
