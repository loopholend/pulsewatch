import React, { useState } from 'react';
import { Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom';
import {
    Activity, ShieldAlert, Bell, Globe, Settings, HelpCircle,
    Menu, X, LogOut, LayoutDashboard, MonitorUp
} from 'lucide-react';

import { useAuth } from './context/AuthContext';
import Dashboard from './components/Dashboard';
import MonitorsPage from './components/MonitorsPage';
import AlertRulesPage from './components/AlertRulesPage';
import StatusPagesManager from './components/StatusPagesManager';
import PublicStatusPage from './components/PublicStatusPage';
import IncidentsPage from './components/IncidentsPage';
import SettingsPage from './components/SettingsPage';
import HelpPage from './components/HelpPage';
import WelcomePage from './components/pages/WelcomePage';
import Login from './components/Login';
import RealtimeConnection from './components/RealtimeConnection';

const NAV_ITEMS = [
    { path: '/',             label: 'Dashboard',     icon: LayoutDashboard },
    { path: '/monitors',     label: 'Monitors',      icon: MonitorUp },
    { path: '/incidents',    label: 'Incidents',     icon: ShieldAlert },
    { path: '/alerts',       label: 'Alert Rules',   icon: Bell },
    { path: '/status-pages', label: 'Status Pages',  icon: Globe },
    { path: '/settings',     label: 'Settings',      icon: Settings },
    { path: '/help',         label: 'Help',          icon: HelpCircle },
];

function App() {
    const location = useLocation();
    const navigate = useNavigate();
    const pathname = location.pathname;
    const { isAuthenticated, email, login, logout } = useAuth();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    const isPublicStatusPage = pathname.startsWith('/status/');
    if (isPublicStatusPage) {
        return (
            <Routes>
                <Route path="/status/:slug" element={<PublicStatusPage />} />
            </Routes>
        );
    }

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    if (!isAuthenticated) {
        return (
            <Routes>
                <Route path="/" element={<WelcomePage />} />
                <Route path="/login" element={<Login onLoginSuccess={(token) => { login(token); navigate('/'); }} />} />
                <Route path="/oauth2/callback/github" element={<Login onLoginSuccess={(token) => { login(token); navigate('/'); }} />} />
                <Route path="/help" element={<HelpPage />} />
                <Route path="*" element={<WelcomePage />} />
            </Routes>
        );
    }

    return (
        <div className="h-screen overflow-hidden bg-gray-50 flex flex-col md:flex-row">
            {/* Sidebar Navigation */}
            <aside className="w-full md:w-64 bg-slate-900 text-white flex-shrink-0 flex flex-col justify-between border-r border-slate-800">
                <div>
                    {/* Brand Header */}
                    <div className="flex items-center justify-between px-6 h-16 border-b border-slate-800">
                        <div className="flex items-center gap-2">
                            <Activity className="w-6 h-6 text-blue-500 animate-pulse" />
                            <span className="text-lg font-bold tracking-tight bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">PulseWatch</span>
                        </div>
                        <button
                            className="md:hidden p-1 rounded hover:bg-slate-800"
                            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                        >
                            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
                        </button>
                    </div>

                    {/* Nav Items */}
                    <nav className={`px-4 py-4 space-y-1 md:block ${mobileMenuOpen ? 'block' : 'hidden'}`}>
                        {NAV_ITEMS.map(item => {
                            const Icon = item.icon;
                            const isActive = pathname === item.path;
                            return (
                                <Link
                                    key={item.path}
                                    to={item.path}
                                    onClick={() => setMobileMenuOpen(false)}
                                    className={`flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-blue-600 text-white shadow-sm'
                                            : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                                    }`}
                                >
                                    <Icon className="w-4 h-4 flex-shrink-0" />
                                    {item.label}
                                </Link>
                            );
                        })}
                    </nav>
                </div>

                {/* Footer: User Info + Logout + Realtime Status */}
                <div className={`p-4 border-t border-slate-800 md:block ${mobileMenuOpen ? 'block' : 'hidden'}`}>
                    <div className="flex items-center justify-between">
                        <div className="truncate pr-2">
                            <p className="text-xs text-slate-500 font-medium">Logged in as</p>
                            <p className="text-sm font-semibold text-slate-350 truncate">{email || 'User'}</p>
                        </div>
                        <button
                            onClick={handleLogout}
                            title="Sign Out"
                            className="p-2 text-slate-400 hover:text-red-400 hover:bg-slate-800 rounded-lg transition-colors"
                        >
                            <LogOut className="w-4 h-4" />
                        </button>
                    </div>
                    <RealtimeConnection />
                </div>
            </aside>

            {/* Main Content Area */}
            <main className="flex-1 overflow-y-auto">
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/monitors" element={<MonitorsPage />} />
                    <Route path="/monitors/:monitorId" element={<MonitorsPage />} />
                    <Route path="/incidents" element={<IncidentsPage />} />
                    <Route path="/alerts" element={<AlertRulesPage />} />
                    <Route path="/status-pages" element={<StatusPagesManager />} />
                    <Route path="/settings" element={<SettingsPage />} />
                    <Route path="/help" element={<HelpPage />} />
                    <Route path="*" element={<Dashboard />} />
                </Routes>
            </main>
        </div>
    );
}

export default App;
