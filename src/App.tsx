import { useEffect, useState } from 'react';
import { SpeechRecognition } from '@capacitor-community/speech-recognition';
import { TextToSpeech } from '@capacitor-community/text-to-speech';
import { Activity, Battery, Bluetooth, Cpu, Globe2, Grid3X3, History, Home, MapPin, Mic, Settings, ShieldCheck, Signal, Sparkles, Star, Wifi, Zap, Box, Crown, Send, Volume2 } from 'lucide-react';

export default function App() {
  const [time, setTime] = useState(new Date());
  const [listening, setListening] = useState(false);
  const [command, setCommand] = useState('');
  const [reply, setReply] = useState('I’m here, always. What can I do for you?');
  const [chatOpen, setChatOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('Home');
  const [messages, setMessages] = useState<{ role: 'user' | 'jarvis'; text: string }[]>(() => {
    try {
      const saved = localStorage.getItem('jarvis-chat-v1');
      return saved ? JSON.parse(saved) : [{ role: 'jarvis', text: 'Hello! I am JARVIS. How can I assist you?' }];
    } catch {
      return [{ role: 'jarvis', text: 'Hello! I am JARVIS. How can I assist you?' }];
    }
  });
  const [memories, setMemories] = useState<string[]>(() => {
    try { return JSON.parse(localStorage.getItem('jarvis-memory-v1') || '[]'); }
    catch { return []; }
  });

  useEffect(() => {
    try { localStorage.setItem('jarvis-chat-v1', JSON.stringify(messages.slice(-100))); }
    catch (error) { console.warn('Could not save JARVIS chat history:', error); }
  }, [messages]);

  useEffect(() => {
    try { localStorage.setItem('jarvis-memory-v1', JSON.stringify(memories.slice(-50))); }
    catch (error) { console.warn('Could not save JARVIS memory:', error); }
  }, [memories]);

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const speakJarvis = async (text: string) => {
    try {
      const result = await TextToSpeech.getSupportedLanguages();
      const language = result.languages.find(x => x.toLowerCase() === 'en-us') || result.languages.find(x => x.toLowerCase().startsWith('en')) || 'en-US';
      await TextToSpeech.speak({ text, lang: language, rate: 1, pitch: 1, volume: 1 });
    } catch (error) {
      console.error('JARVIS speech error:', error);
      setReply('I could not start voice output. Please check Android text-to-speech settings.');
    }
  };

  const askJarvis = async (message: string) => {
    const userMessage = message.trim();
    if (!userMessage) return;
    setMessages(prev => [...prev, { role: 'user', text: userMessage }]);
    setCommand('');
    const rememberMatch = userMessage.match(/^(?:jarvis[, ]+)?remember(?: that)?[: ]+(.+)$/i);
    if (rememberMatch) {
      const fact = rememberMatch[1].trim();
      setMemories(prev => [...prev.filter(item => item.toLowerCase() !== fact.toLowerCase()), fact].slice(-50));
      const confirmation = 'I will remember that on this device: ' + fact;
      setMessages(prev => [...prev, { role: 'jarvis', text: confirmation }]);
      setReply(confirmation);
      await speakJarvis(confirmation);
      return;
    }
    if (/^(?:jarvis[, ]+)?forget my memories$/i.test(userMessage)) {
      setMemories([]);
      const confirmation = 'Your saved JARVIS memories on this device have been cleared.';
      setMessages(prev => [...prev, { role: 'jarvis', text: confirmation }]);
      setReply(confirmation);
      await speakJarvis(confirmation);
      return;
    }
    setReply('JARVIS is thinking...');
    try {
      const memoryContext = memories.length ? '\\n\\nUser memories (use only when relevant):\\n' + memories.map(item => '- ' + item).join('\\n') : '';
      const response = await fetch('https://jarvis-ai.goru041212.workers.dev/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage + memoryContext })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.details || data.error || 'AI request failed.');
      const answer = String(data.reply || 'I could not generate a response.');
      setMessages(prev => [...prev, { role: 'jarvis', text: answer }]);
      setReply(answer);
      await speakJarvis(answer);
    } catch (error) {
      const message = 'I could not reach my AI brain: ' + String(error);
      setMessages(prev => [...prev, { role: 'jarvis', text: message }]);
      setReply(message);
    }
  };

  async function startListening() {
    try {
      const available = await SpeechRecognition.available();
      if (!available.available) { setReply('Speech recognition is unavailable on this device.'); return; }
      let permission = await SpeechRecognition.checkPermissions();
      if (permission.speechRecognition !== 'granted') permission = await SpeechRecognition.requestPermissions();
      if (permission.speechRecognition !== 'granted') { setReply('Microphone permission was not granted.'); return; }
      setListening(true);
      setReply('Listening... Speak now.');
      const result = await SpeechRecognition.start({ language: 'en-IN', maxResults: 1, popup: false, partialResults: false });
      if (result.matches?.length) await askJarvis(result.matches[0]);
    } catch (error) {
      setReply('Voice recognition error. Please try again.');
    } finally {
      setListening(false);
    }
  }

  const navItems = [
    { name: 'Home', icon: Home },
    { name: 'Smart Control', icon: Grid3X3 },
    { name: 'History', icon: History },
    { name: 'Favorites', icon: Star },
    { name: 'Pro Mode', icon: Crown }
  ];

  return (
    <>
      {chatOpen ? (
        <main className="chat-screen">
          <header className="chat-header"><div><span className="eyebrow">PERSONAL AI ASSISTANT</span><h2>J.A.R.V.I.S</h2></div><button onClick={() => setChatOpen(false)}>BACK</button></header>
          <section className="chat-messages">
            {messages.map((message, index) => <div key={index} className={`message ${message.role}`}><small>{message.role === 'jarvis' ? 'JARVIS' : 'YOU'}</small>{message.text}</div>)}
          </section>
          <form className="chat-input" onSubmit={e => { e.preventDefault(); void askJarvis(command); }}>
            <input value={command} onChange={e => setCommand(e.target.value)} placeholder="Ask JARVIS anything..." />
            <button type="submit"><Send size={17}/></button>
          </form>
        </main>
      ) : (
        <main className="hud">
          <header className="topbar">
            <div className="clock"><strong>{time.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: false })}</strong><small>{time.toLocaleDateString('en-IN', { weekday: 'short', day: '2-digit', month: 'short' })}</small></div>
            <div className="top-status"><Signal size={15}/><Wifi size={16}/><Battery size={18}/></div>
          </header>

          <section className="welcome-row">
            <div className="online-card"><i/><div><b>JARVIS ONLINE</b><small>PERSONAL AI ASSISTANT</small></div></div>
            <div className="greeting"><small>GOOD DAY,</small><b>SUPRATIM <span>♥</span></b></div>
          </section>

          <section className="hero">
            <div className="core-ring">
              <div className="ring ring-a"/><div className="ring ring-b"/><div className="ring ring-c"/>
              <div className="core-center"><Sparkles size={26}/><strong>JARVIS</strong><small>— AI ASSISTANT —</small></div>
            </div>
            <div className="globe-wrap"><div className="globe-orbit"/><Globe2 className="globe-icon"/><div className="globe-base"/></div>
          </section>

          <section className="reply-panel"><Volume2 size={22}/><p>{reply}</p></section>

          <section className="quick-actions">
            <button onClick={() => setActiveTab('Smart Control')}><Grid3X3/><span>Apps</span></button>
            <button onClick={() => setActiveTab('Smart Control')}><Box/><span>Tools</span></button>
            <button className={`mic-button ${listening ? 'listening' : ''}`} onClick={startListening}><Mic/><span>{listening ? 'Listening' : 'Tap to speak'}</span></button>
            <button onClick={() => setReply('Settings panel is coming next.')}><Settings/><span>Settings</span></button>
            <button onClick={() => setChatOpen(true)}><Activity/><span>More</span></button>
          </section>

          <div className="type-command"><input value={command} onChange={e => setCommand(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') void askJarvis(command); }} placeholder="Type a command for JARVIS..."/><button onClick={() => void askJarvis(command)} aria-label="Send command"><Send size={17}/></button></div>

          <nav className="bottom-nav">
            {navItems.map(({ name, icon: Icon }) => <button key={name} className={activeTab === name ? 'active' : ''} onClick={() => { setActiveTab(name); if (name === 'History') setChatOpen(true); else if (name !== 'Home') setReply(name + ' is being prepared.'); }}><Icon/><span>{name}</span></button>)}
          </nav>
          <div className="signature">YOUR PERSONAL AI • ALWAYS READY</div>
        </main>
      )}
    </>
  );
}
