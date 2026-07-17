import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Activity, ShieldCheck, ArrowRight, BookOpen, Clock, Mail, Globe, Sparkles } from 'lucide-react';

const WelcomePage = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 text-white flex flex-col justify-between">
      {/* Navbar */}
      <nav className="border-b border-white/10 px-6 py-4 backdrop-blur-md sticky top-0 z-50 bg-slate-900/60">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <Activity className="w-8 h-8 text-blue-500 animate-pulse" />
            <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">PulseWatch</span>
          </div>
          <button
            onClick={() => navigate('/login')}
            className="px-4 py-2 text-sm font-semibold rounded-lg bg-white/10 hover:bg-white/15 border border-white/20 transition-all"
          >
            Sign In
          </button>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="flex-1 flex items-center py-20 px-6">
        <div className="max-w-4xl mx-auto text-center space-y-8">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold uppercase tracking-wider mx-auto">
            <Sparkles className="w-3.5 h-3.5" />
            Production-grade API Observability
          </div>
          
          <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight leading-none bg-gradient-to-b from-white to-slate-350 bg-clip-text text-transparent">
            PulseWatch
          </h1>
          
          <p className="text-lg md:text-xl text-slate-300 max-w-2xl mx-auto leading-relaxed">
            Monitor APIs, websites and backend services from one dashboard. Keep your endpoints healthy and users happy.
          </p>

          {/* Features Checkbox */}
          <div className="flex flex-wrap justify-center gap-x-8 gap-y-3 py-6 max-w-3xl mx-auto">
            {[
              { text: 'Uptime Monitoring', icon: Clock },
              { text: 'Latency Tracking', icon: Activity },
              { text: 'Incident Detection', icon: ShieldCheck },
              { text: 'Email Alerts', icon: Mail },
              { text: 'Public Status Pages', icon: Globe }
            ].map((f, i) => (
              <div key={i} className="flex items-center gap-2 text-slate-300">
                <span className="text-blue-500">✓</span>
                <span className="text-sm font-medium">{f.text}</span>
              </div>
            ))}
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-4">
            <button
              onClick={() => navigate('/login')}
              className="group flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-blue-600 text-white font-semibold shadow-lg shadow-blue-500/20 hover:bg-blue-500 hover:shadow-blue-500/35 active:scale-98 transition-all"
            >
              Create Your First Monitor
              <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
            </button>
            <button
              onClick={() => navigate('/help')}
              className="flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 font-semibold transition-all"
            >
              <BookOpen className="w-4 h-4" />
              Learn How PulseWatch Works
            </button>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-white/5 py-6 text-center text-xs text-slate-500">
        &copy; {new Date().getFullYear()} PulseWatch Inc. All rights reserved.
      </footer>
    </div>
  );
};

export default WelcomePage;
