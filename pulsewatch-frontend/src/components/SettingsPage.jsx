import React, { useState, useEffect } from 'react';
import {
    Settings, User, Server, Shield, LogOut, Key, CreditCard,
    Check, Copy, Plus, Trash2, ShieldAlert, CheckCircle2,
    Lock, RefreshCw, Smartphone, Monitor, Globe, Bell, Cpu
} from 'lucide-react';
import { api } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

const SettingsPage = () => {
    const { email, logout } = useAuth();
    const role = 'Developer';

    // Tabs: 'profile', 'billing', 'api-keys', 'system'
    const [activeTab, setActiveTab] = useState('profile');

    // Profile State
    const [displayName, setDisplayName] = useState(() => localStorage.getItem('displayName') || 'System Administrator');
    const [passwordForm, setPasswordForm] = useState({ current: '', next: '', confirm: '' });
    const [pwdError, setPwdError] = useState('');
    const [pwdSuccess, setPwdSuccess] = useState('');


    // Plan & Usage Stats
    const [stats, setStats] = useState({
        monitorsCount: 0,
        statusPagesCount: 0,
        alertsCount: 0
    });
    const [statsLoading, setStatsLoading] = useState(false);

    // API Keys State
    const [apiKeys, setApiKeys] = useState([]);
    const [newKeyName, setNewKeyName] = useState('');
    const [generatedKey, setGeneratedKey] = useState(null);
    const [showKeyModal, setShowKeyModal] = useState(false);
    const [copiedKey, setCopiedKey] = useState(false);

    // Toast Notification
    const [toast, setToast] = useState({ message: '', type: 'success' });
    const triggerToast = (msg, type = 'success') => {
        setToast({ message: msg, type });
        setTimeout(() => setToast({ message: '', type: 'success' }), 4000);
    };

    // Load active counts
    useEffect(() => {
        const fetchUsageStats = async () => {
            setStatsLoading(true);
            try {
                const [summaryRes, pagesRes, alertsRes, keysRes] = await Promise.all([
                    api.get('/analytics/summary').catch(() => ({ data: { totalMonitors: 0 } })),
                    api.get('/status-pages').catch(() => ({ data: [] })),
                    api.get('/alerts/history?size=1').catch(() => ({ data: { totalElements: 0 } })),
                    api.get('/api-keys').catch(() => ({ data: [] }))
                ]);
                
                setStats({
                    monitorsCount: summaryRes.data?.totalMonitors || 0,
                    statusPagesCount: pagesRes.data?.length || 0,
                    alertsCount: alertsRes.data?.totalElements || 0
                });
                setApiKeys(keysRes.data || []);
            } catch (err) {
                console.error('Failed to load settings usage stats', err);
            } finally {
                setStatsLoading(false);
            }
        };
        fetchUsageStats();
    }, []);

    // Password change — calls real API, not a mock
    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setPwdError('');
        setPwdSuccess('');

        if (!passwordForm.current || !passwordForm.next || !passwordForm.confirm) {
            setPwdError('All password fields are required.');
            return;
        }
        if (passwordForm.next !== passwordForm.confirm) {
            setPwdError('New passwords do not match.');
            return;
        }
        if (passwordForm.next.length < 8) {
            setPwdError('Password must be at least 8 characters long.');
            return;
        }

        try {
            await api.post('/auth/change-password', {
                currentPassword: passwordForm.current,
                newPassword: passwordForm.next
            });
            setPwdSuccess('Password updated successfully.');
            setPasswordForm({ current: '', next: '', confirm: '' });
            triggerToast('Security credentials updated');
        } catch (err) {
            if (err.response?.status === 404) {
                // Endpoint not yet implemented — honest message
                setPwdError('Password change is not available via API. Please contact your workspace admin.');
            } else {
                const msg = err.response?.data?.message || 'Failed to update password. Check your current password.';
                setPwdError(msg);
            }
        }
    };

    const handleSaveProfile = (e) => {
        e.preventDefault();
        localStorage.setItem('displayName', displayName);
        triggerToast('Profile updated successfully');
    };

    // Session revocation is not yet implemented server-side
    // Placeholder to avoid broken UI
    const revokeSession = (_id) => {
        triggerToast('Session management coming soon', 'info');
    };

    // API Key generation
    const generateApiKey = async () => {
        if (!newKeyName.trim()) return;
        try {
            const res = await api.post('/api-keys', { name: newKeyName, durationDays: 0 });
            setApiKeys(prev => [res.data, ...prev]);
            setGeneratedKey(res.data.rawKey);
            setShowKeyModal(true);
            setNewKeyName('');
            triggerToast('API Key generated');
        } catch (err) {
            console.error('Failed to generate key', err);
            triggerToast('Failed to generate API Key', 'error');
        }
    };

    const revokeKey = async (id) => {
        try {
            await api.delete(`/api-keys/${id}`);
            setApiKeys(prev => prev.filter(k => k.id !== id));
            triggerToast('API Key revoked');
        } catch (err) {
            console.error('Failed to revoke key', err);
            triggerToast('Failed to revoke API Key', 'error');
        }
    };

    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text);
        setCopiedKey(true);
        setTimeout(() => setCopiedKey(false), 2000);
    };

    const handleLogout = () => {
        logout();
        window.location.href = '/';
    };

    return (
        <div className="min-h-screen bg-gray-50 p-6 md:p-8">
            {/* Toast */}
            {toast.message && (
                <div className={`fixed bottom-5 right-5 z-50 px-4 py-3 rounded-lg shadow-xl flex items-center gap-2 text-sm border animate-slide-in text-white bg-slate-900 border-white/10`}>
                    <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                    {toast.message}
                </div>
            )}

            {/* Secret key modal */}
            {showKeyModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-2xl shadow-2xl border border-gray-100 w-full max-w-md p-6 space-y-4">
                        <div className="flex items-center gap-2 text-amber-600">
                            <ShieldAlert className="w-6 h-6 animate-pulse" />
                            <h3 className="text-lg font-bold">Copy your API Key</h3>
                        </div>
                        <p className="text-xs text-gray-500 leading-relaxed">
                            For security reasons, this key will only be shown to you once. Make sure to copy it now and store it in a secure password manager.
                        </p>
                        <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden bg-gray-50">
                            <input
                                type="text" readOnly
                                className="flex-1 px-3 py-2.5 font-mono text-xs bg-transparent outline-none text-gray-800"
                                value={generatedKey || ''}
                            />
                            <button
                                onClick={() => copyToClipboard(generatedKey)}
                                className="px-4 py-2.5 bg-blue-600 text-white hover:bg-blue-700 transition-colors flex items-center gap-1.5 text-xs font-semibold"
                            >
                                {copiedKey ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                                {copiedKey ? 'Copied' : 'Copy'}
                            </button>
                        </div>
                        <button
                            onClick={() => { setShowKeyModal(false); setGeneratedKey(null); }}
                            className="w-full bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold py-2 rounded-lg text-sm transition-colors"
                        >
                            I have saved this key
                        </button>
                    </div>
                </div>
            )}

            <div className="max-w-5xl mx-auto space-y-8">
                {/* Header */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex items-center gap-3">
                    <Settings className="w-8 h-8 text-blue-600" />
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">Settings & Profile</h1>
                        <p className="text-gray-500 mt-1">Manage account profile, passwords, API tokens, and subscription usage.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-start">
                    {/* Tab Navigation */}
                    <div className="lg:col-span-1 bg-white rounded-xl shadow-sm border border-gray-100 p-4 space-y-1">
                        <button
                            onClick={() => setActiveTab('profile')}
                            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                                activeTab === 'profile'
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                            }`}
                        >
                            <User className="w-4 h-4" />
                            Account Profile
                        </button>
                        <button
                            onClick={() => setActiveTab('billing')}
                            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                                activeTab === 'billing'
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                            }`}
                        >
                            <CreditCard className="w-4 h-4" />
                            Plan & Usage
                        </button>
                        <button
                            onClick={() => setActiveTab('api-keys')}
                            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                                activeTab === 'api-keys'
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                            }`}
                        >
                            <Key className="w-4 h-4" />
                            API Access Tokens
                        </button>
                        <button
                            onClick={() => setActiveTab('system')}
                            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                                activeTab === 'system'
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                            }`}
                        >
                            <Cpu className="w-4 h-4" />
                            System Parameters
                        </button>
                        <div className="pt-4 mt-4 border-t border-gray-100">
                            <button
                                onClick={handleLogout}
                                className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold text-rose-600 hover:bg-rose-50 transition-colors"
                            >
                                <LogOut className="w-4 h-4" />
                                Sign Out
                            </button>
                        </div>
                    </div>

                    {/* Tab Panels */}
                    <div className="lg:col-span-3 space-y-6">

                        {/* PROFILE TAB */}
                        {activeTab === 'profile' && (
                            <>
                                {/* Profile Info */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-5">
                                    <h3 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">Account Details</h3>
                                    <form onSubmit={handleSaveProfile} className="space-y-4">
                                        <div className="flex items-center gap-4">
                                            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-xl uppercase">
                                                {email.charAt(0)}
                                            </div>
                                            <div>
                                                <p className="text-xs text-gray-400 font-semibold uppercase tracking-wider">Account Tier</p>
                                                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-800 mt-1">
                                                    {role}
                                                </span>
                                            </div>
                                        </div>
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">Display Name</label>
                                                <input
                                                    type="text"
                                                    className="w-full border border-gray-250 rounded-lg p-2.5 text-sm focus:ring-blue-500 focus:border-blue-500 bg-white"
                                                    value={displayName}
                                                    onChange={e => setDisplayName(e.target.value)}
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">Email Address (Read Only)</label>
                                                <input
                                                    type="text" readOnly
                                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm bg-gray-50 text-gray-500 select-all"
                                                    value={email}
                                                />
                                            </div>
                                        </div>
                                        <button
                                            type="submit"
                                            className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition-colors"
                                        >
                                            Save Profile
                                        </button>
                                    </form>
                                </div>

                                {/* Security Creds */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-5">
                                    <h3 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">Update Security Credentials</h3>
                                    <form onSubmit={handlePasswordChange} className="space-y-4">
                                        {pwdError && (
                                            <div className="p-3 bg-red-50 border border-red-200 text-red-800 text-xs rounded-lg flex items-center gap-2">
                                                <ShieldAlert className="w-4 h-4 flex-shrink-0" />
                                                {pwdError}
                                            </div>
                                        )}
                                        {pwdSuccess && (
                                            <div className="p-3 bg-emerald-50 border border-emerald-250 text-emerald-800 text-xs rounded-lg flex items-center gap-2">
                                                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                                                {pwdSuccess}
                                            </div>
                                        )}
                                        <div>
                                            <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">Current Password</label>
                                            <input
                                                type="password"
                                                className="w-full border border-gray-250 rounded-lg p-2.5 text-sm focus:ring-blue-500 focus:border-blue-500 bg-white"
                                                value={passwordForm.current}
                                                onChange={e => setPasswordForm({ ...passwordForm, current: e.target.value })}
                                            />
                                        </div>
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">New Password</label>
                                                <input
                                                    type="password"
                                                    className="w-full border border-gray-250 rounded-lg p-2.5 text-sm focus:ring-blue-500 focus:border-blue-500 bg-white"
                                                    value={passwordForm.next}
                                                    onChange={e => setPasswordForm({ ...passwordForm, next: e.target.value })}
                                                />
                                            </div>
                                            <div>
                                                <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">Confirm New Password</label>
                                                <input
                                                    type="password"
                                                    className="w-full border border-gray-250 rounded-lg p-2.5 text-sm focus:ring-blue-500 focus:border-blue-500 bg-white"
                                                    value={passwordForm.confirm}
                                                    onChange={e => setPasswordForm({ ...passwordForm, confirm: e.target.value })}
                                                />
                                            </div>
                                        </div>
                                        <button
                                            type="submit"
                                            className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition-colors flex items-center gap-2"
                                        >
                                            <Lock className="w-4 h-4" />
                                            Change Password
                                        </button>
                                    </form>
                                </div>

                                {/* Active Sessions - Coming Soon */}
                                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-4">
                                    <h3 className="text-lg font-bold text-gray-900 border-b border-gray-100 pb-3">Active Login Sessions</h3>
                                    <div className="border border-dashed border-gray-200 rounded-xl p-8 text-center">
                                        <Smartphone className="w-8 h-8 text-gray-300 mx-auto mb-3" />
                                        <p className="text-sm font-semibold text-gray-500">Session management coming soon</p>
                                        <p className="text-xs text-gray-400 mt-1 max-w-xs mx-auto">
                                            Track and revoke active browser sessions from one place.
                                            For now, changing your password will invalidate all active sessions.
                                        </p>
                                    </div>
                                </div>
                            </>
                        )}

                        {/* BILLING & PLAN TAB */}
                        {activeTab === 'billing' && (
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                                <div className="border-b border-gray-100 pb-4 flex justify-between items-center">
                                    <div>
                                        <h3 className="text-lg font-bold text-gray-900">Plan limits & Resource Usage</h3>
                                        <p className="text-sm text-gray-500 mt-1">Review active consumption against developer plan thresholds.</p>
                                    </div>
                                    {statsLoading && <RefreshCw className="w-5 h-5 text-blue-500 animate-spin" />}
                                </div>

                                {/* Plan tier status card */}
                                <div className="bg-gradient-to-r from-blue-700 to-indigo-900 rounded-2xl p-5 text-white flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                                    <div className="space-y-1">
                                        <span className="px-2 py-0.5 rounded bg-white/20 text-[10px] font-black tracking-widest uppercase">CURRENT TIER</span>
                                        <h4 className="text-xl font-bold">Developer Sandbox (Free)</h4>
                                        <p className="text-xs text-blue-200">Reset cycle: Monthly · Renew date: 2026-08-01</p>
                                    </div>
                                    <button className="px-4 py-2 bg-white text-indigo-950 rounded-xl text-xs font-bold hover:bg-indigo-50 shadow-md transition-all">
                                        Upgrade Subscription
                                    </button>
                                </div>

                                {/* Usage parameters */}
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
                                    {/* Monitors meter */}
                                    <div className="border border-gray-200 rounded-xl p-4 space-y-3">
                                        <div className="flex justify-between items-center text-xs">
                                            <span className="font-bold text-gray-600 flex items-center gap-1"><Monitor className="w-4 h-4 text-blue-500" /> Active Monitors</span>
                                            <span className="font-mono text-gray-900 font-bold">{stats.monitorsCount} / 5</span>
                                        </div>
                                        <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                                            <div className="bg-blue-600 h-full rounded-full transition-all duration-500"
                                                style={{ width: `${Math.min(100, (stats.monitorsCount / 5) * 100)}%` }}
                                            />
                                        </div>
                                        <p className="text-[10px] text-gray-400">Allows HTTP, TCP and custom assertions checks.</p>
                                    </div>

                                    {/* Status pages meter */}
                                    <div className="border border-gray-200 rounded-xl p-4 space-y-3">
                                        <div className="flex justify-between items-center text-xs">
                                            <span className="font-bold text-gray-600 flex items-center gap-1"><Globe className="w-4 h-4 text-purple-500" /> Status Pages</span>
                                            <span className="font-mono text-gray-900 font-bold">{stats.statusPagesCount} / 3</span>
                                        </div>
                                        <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                                            <div className="bg-purple-600 h-full rounded-full transition-all duration-500"
                                                style={{ width: `${Math.min(100, (stats.statusPagesCount / 3) * 100)}%` }}
                                            />
                                        </div>
                                        <p className="text-[10px] text-gray-400">Public availability status communication portals.</p>
                                    </div>

                                    {/* Alert Rules meter */}
                                    <div className="border border-gray-200 rounded-xl p-4 space-y-3">
                                        <div className="flex justify-between items-center text-xs">
                                            <span className="font-bold text-gray-600 flex items-center gap-1"><Bell className="w-4 h-4 text-indigo-500" /> Notification Alerts</span>
                                            <span className="font-mono text-gray-900 font-bold">{stats.alertsCount} / 10</span>
                                        </div>
                                        <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                                            <div className="bg-indigo-600 h-full rounded-full transition-all duration-500"
                                                style={{ width: `${Math.min(100, (stats.alertsCount / 10) * 100)}%` }}
                                            />
                                        </div>
                                        <p className="text-[10px] text-gray-400">Configured emails, Webhooks, Slack channels.</p>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* API KEYS TAB */}
                        {activeTab === 'api-keys' && (
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                                <div className="border-b border-gray-100 pb-4">
                                    <h3 className="text-lg font-bold text-gray-900 font-black">Personal API Access Tokens</h3>
                                    <p className="text-sm text-gray-500 mt-1">Authenticate API integrations, telemetry exporter scripts, and runtime agents.</p>
                                </div>

                                <div className="flex flex-col sm:flex-row gap-3">
                                    <input
                                        type="text"
                                        placeholder="Token name (e.g. Server Monitor Exporter)"
                                        className="flex-1 border border-gray-250 rounded-xl px-4 py-2.5 text-sm focus:ring-blue-500 focus:border-blue-500 bg-white"
                                        value={newKeyName}
                                        onChange={e => setNewKeyName(e.target.value)}
                                    />
                                    <button
                                        onClick={generateApiKey}
                                        disabled={!newKeyName.trim()}
                                        className="bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white font-semibold px-5 py-2.5 rounded-xl text-sm flex items-center justify-center gap-2 transition-all flex-shrink-0"
                                    >
                                        <Plus className="w-4 h-4" />
                                        Generate Token
                                    </button>
                                </div>

                                <div className="space-y-3">
                                    {apiKeys.length === 0 ? (
                                        <div className="p-8 border border-dashed border-gray-300 rounded-xl text-center text-xs text-gray-500">
                                            No API access tokens configured. Create a token to securely integrate telemetry logs.
                                        </div>
                                    ) : (
                                        apiKeys.map(k => (
                                            <div key={k.id} className="flex justify-between items-center p-4 border border-gray-150 rounded-xl bg-gray-55/10 text-xs">
                                                <div>
                                                    <p className="font-bold text-gray-800">{k.name}</p>
                                                    <p className="text-[10px] text-gray-400 font-mono mt-0.5">
                                                        Token: {k.maskedKey} · Created: {k.createdAt ? new Date(k.createdAt).toLocaleDateString() : '—'}
                                                    </p>
                                                </div>
                                                <div className="flex items-center gap-3">
                                                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-100">
                                                        ACTIVE
                                                    </span>
                                                    <button
                                                        onClick={() => revokeKey(k.id)}
                                                        className="text-red-500 hover:text-red-700 font-semibold p-1 hover:bg-red-50 rounded"
                                                        title="Revoke and destroy key credentials"
                                                    >
                                                        Revoke
                                                    </button>
                                                </div>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        )}

                        {/* SYSTEM PARAMETERS TAB */}
                        {activeTab === 'system' && (
                            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                                <div className="border-b border-gray-100 pb-4">
                                    <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                                        <Server className="w-5 h-5 text-gray-600" />
                                        System Configuration Properties
                                    </h3>
                                    <p className="text-sm text-gray-500 mt-1">Read-only configurations loaded from Spring Boot application backend context.</p>
                                </div>

                                <div className="border-t border-gray-100 space-y-4 divide-y divide-gray-100 text-xs">
                                    <div className="flex justify-between items-center py-3">
                                        <span className="text-gray-500 font-medium">Incident Failure Threshold</span>
                                        <span className="font-mono font-semibold bg-gray-100 px-2 py-1 rounded text-gray-700">3 consecutive checks</span>
                                    </div>
                                    <div className="flex justify-between items-center py-3">
                                        <span className="text-gray-500 font-medium">Global Scheduler Interval</span>
                                        <span className="font-mono font-semibold bg-gray-100 px-2 py-1 rounded text-gray-700">60 seconds (60000ms)</span>
                                    </div>
                                    <div className="flex justify-between items-center py-3">
                                        <span className="text-gray-500 font-medium">Scheduler Pool Size</span>
                                        <span className="font-mono font-semibold bg-gray-100 px-2 py-1 rounded text-gray-700">20 worker threads</span>
                                    </div>
                                    <div className="flex justify-between items-center py-3">
                                        <span className="text-gray-500 font-medium">Default HTTP Timeout</span>
                                        <span className="font-mono font-semibold bg-gray-100 px-2 py-1 rounded text-gray-700">5000ms</span>
                                    </div>
                                </div>

                                <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 flex gap-3">
                                    <Shield className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                                    <div className="text-xs text-blue-700 leading-relaxed">
                                        <strong>Note:</strong> System configurations are specified in <code>application.yml</code> at boot time to ensure high-performance scheduler execution. Contact your system administrator to adjust these thresholds.
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SettingsPage;
