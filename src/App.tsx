
import { useEffect, useState } from 'react';
import { SpeechRecognition } from '@capacitor-community/speech-recognition';
import {
  Activity, Shield, Cpu, Globe, Mic, Power,
  Settings, Zap, Wifi, Volume2, Terminal
} from 'lucide-react';

export default function App() {
  const [time, setTime] = useState(new Date());
  const [listening, setListening] = useState(false);
  const [command, setCommand] = useState('');
  const [reply, setReply] = useState(
    'All systems online. How may I assist you?'
  );

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  
async function startListening() {
  try {
    const available = await SpeechRecognition.available();

    if (!available.available) {
      setReply('Speech recognition is unavailable on this device.');
      return;
    }

    let permission = await SpeechRecognition.checkPermissions();

    if (permission.speechRecognition !== 'granted') {
      permission = await SpeechRecognition.requestPermissions();
    }

    if (permission.speechRecognition !== 'granted') {
      setReply('Microphone permission was not granted.');
      return;
    }

    setListening(true);
    setReply('Listening... Speak now.');

    const result = await SpeechRecognition.start({
      language: 'en-IN',
      maxResults: 1,
      popup: true,
      partialResults: false
    });

    if (result.matches && result.matches.length > 0) {
      setCommand(result.matches[0]);
      setReply('Command received: ' + result.matches[0]);
    }
  } catch (error) {
    setReply('Voice recognition error. Please try again.');
  } finally {
    setListening(false);
  }
    
    

    const recognition = new Speech();
    recognition.lang = 'en-IN';
    recognition.onstart = () => setListening(true);
    recognition.onend = () => setListening(false);
    recognition.onresult = (event: any) => {
      const text = event.results[0][0].transcript;
      setCommand(text);
      setReply('Command received: ' + text);
    };
    recognition.onerror = () => {
      setListening(false);
      setReply('Voice recognition encountered an error.');
    };
    recognition.start();
  }

  return (
    <main className="hud">
      <header className="topbar">
        <div className="brand">
          <span className="brand-icon">◈</span>
          <div>
            <strong>J.A.R.V.I.S</strong>
            <small>PERSONAL AI SYSTEM</small>
          </div>
        </div>
        <div className="system-online"><i /> SYSTEM ONLINE</div>
        <div className="clock">
          {time.toLocaleTimeString('en-IN', { hour12: false })}
          <small>{time.toDateString()}</small>
        </div>
      </header>

      <section className="dashboard">
        <aside className="side-panel">
          <h3><Activity size={15}/> GLOBAL OVERVIEW</h3>
          <div className="metric"><span>CPU STATUS</span><b>OPTIMAL</b></div>
          <div className="meter"><i style={{width:'68%'}} /></div>
          <div className="metric"><span>MEMORY</span><b>68%</b></div>
          <div className="meter"><i style={{width:'68%'}} /></div>
          <div className="metric"><span>NETWORK</span><b>CONNECTED</b></div>
          <div className="meter"><i style={{width:'90%'}} /></div>

          <h3><Shield size={15}/> SECURITY</h3>
          <div className="security">
            <Shield size={27}/>
            <b>PROTECTED</b>
            <small>All systems secure</small>
          </div>

          <h3><Terminal size={15}/> SYSTEM LOG</h3>
          <p className="log">[OK] Core initialized</p>
          <p className="log">[OK] Interface ready</p>
          <p className="log">[OK] Awaiting command</p>
        </aside>

        <section className="core-section">
          <div className="core-heading">
            <span>ARTIFICIAL INTELLIGENCE</span>
            <h2>JARVIS CORE</h2>
            <small>NEURAL ENGINE ACTIVE</small>
          </div>

          <div className="reactor">
            <div className="orbit orbit-one" />
            <div className="orbit orbit-two" />
            <div className="orbit orbit-three" />
            <div className="core-glow">
              <div className="core-inner">J</div>
            </div>
          </div>

          <p className="core-status">◆ POWER SYSTEM STABLE ◆</p>
          <div className="assistant-reply">{reply}</div>
        </section>

        <aside className="side-panel right-panel">
          <h3><Cpu size={15}/> OPERATIONS</h3>
          <button onClick={() => setReply('System diagnostics requested.')}>
            <Activity size={17}/> SYSTEM DIAGNOSTICS
          </button>
          <button onClick={() => setReply('Security protocols are active.')}>
            <Shield size={17}/> SECURITY PROTOCOL
          </button>
          <button onClick={() => setReply('Power systems operational.')}>
            <Zap size={17}/> POWER CONTROL
          </button>
          <button onClick={() => setReply('Network connection status: online.')}>
            <Wifi size={17}/> NETWORK STATUS
          </button>

          <h3><Globe size={15}/> GLOBAL POSITION</h3>
          <div className="earth">
            <Globe size={95} strokeWidth={0.7}/>
            <span>GLOBAL NETWORK</span>
          </div>

          <h3><Settings size={15}/> POWER STATUS</h3>
          <div className="metric"><span>REACTOR</span><b>ACTIVE</b></div>
          <div className="metric"><span>SECURITY</span><b>ENABLED</b></div>
        </aside>
      </section>

      <footer className="bottom-bar">
        <div className="voice-info">
          <Volume2 size={17}/>
          <span>{listening ? 'LISTENING...' : 'VOICE SYSTEM READY'}</span>
        </div>
        <div className="command-box">
          <input
            value={command}
            onChange={e => setCommand(e.target.value)}
            placeholder="Enter your command..."
            onKeyDown={e => {
              if (e.key === 'Enter' && command.trim()) {
                setReply('Command received: ' + command);
              }
            }}
          />
          <button onClick={() => {
            if (command.trim()) setReply('Command received: ' + command);
          }}>SEND</button>
        </div>
        <button className="mic-button" onClick={startListening}>
          <Mic size={20}/>
          {listening ? 'STOP' : 'VOICE'}
        </button>
        <div className="power-button">
          <Power size={18}/> ONLINE
        </div>
      </footer>

      <div className="signature">I AM ALWAYS WITH YOU. ♥</div>
    </main>
  );
}
