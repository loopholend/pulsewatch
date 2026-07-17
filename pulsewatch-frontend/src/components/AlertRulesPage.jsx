import React, { useState, useEffect, useCallback } from 'react';
import {
  Bell, Plus, ToggleLeft, ToggleRight, Send, CheckCircle,
  XCircle, RefreshCw, Trash2, Info, Pencil
} from 'lucide-react';
import { api } from '../api/axiosConfig';
import ConfirmDialog from './ui/ConfirmDialog';
import { RULE_TYPE_LABELS, ALERT_TYPE_LABELS, labelFor } from '../utils/enumLabels';

const AlertRulesPage = () => {
    const [monitors, setMonitors]     = useState([]);
    const [rules, setRules]           = useState([]);
    const [history, setHistory]       = useState([]);
    const [loading, setLoading]       = useState(true);
    const [showForm, setShowForm]     = useState(false);
    const [testStatus, setTestStatus] = useState(null);
    const [editingRuleId, setEditingRuleId] = useState(null);
    const [form, setForm] = useState({
        monitorId:         '',
        ruleType:          'INCIDENT_OPENED',
        notificationEmail: '',
        cooldownMinutes:   30,
        layoutType:        'DEFAULT',
        customSubject:     '',
        customBody:        '',
    });
    const [formError, setFormError]   = useState('');
    const [formLoading, setFormLoading] = useState(false);

    // Toast notification
    const [toast, setToast] = useState({ message: '', type: 'success' });
    const triggerToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast({ message: '', type: 'success' }), 4000);
    };

    // Confirm dialog state
    const [confirm, setConfirm] = useState({ open: false, ruleId: null });

    const fetchAll = useCallback(async () => {
        setLoading(true);
        try {
            const [monitorsRes, rulesRes, historyRes] = await Promise.all([
                api.get('/monitors?size=100'),
                api.get('/alerts/rules?size=100'),
                api.get('/alerts/history?size=100'),
            ]);
            setMonitors(monitorsRes.data.content || []);
            setRules(rulesRes.data.content || []);
            setHistory(historyRes.data.content || []);
        } catch (err) {
            console.error('Failed to fetch alert data', err);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { fetchAll(); }, [fetchAll]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError('');
        setFormLoading(true);

        if (!form.monitorId) {
            setFormError('Please select a monitor target.');
            setFormLoading(false);
            return;
        }

        try {
            if (editingRuleId) {
                await api.put(`/alerts/rules/${editingRuleId}`, form);
                triggerToast('Alert rule updated successfully');
            } else {
                await api.post('/alerts/rules', form);
                triggerToast('Alert rule created successfully');
            }
            setShowForm(false);
            setEditingRuleId(null);
            setForm({
                monitorId:         '',
                ruleType:          'INCIDENT_OPENED',
                notificationEmail: '',
                cooldownMinutes:   30,
                layoutType:        'DEFAULT',
                customSubject:     '',
                customBody:        '',
            });
            fetchAll();
        } catch (err) {
            setFormError(err.response?.data?.message || 'Failed to save alert rule.');
        } finally {
            setFormLoading(false);
        }
    };

    const handleEditRequest = (rule) => {
        setEditingRuleId(rule.id);
        setForm({
            monitorId:         rule.monitorId,
            ruleType:          rule.ruleType,
            notificationEmail: rule.notificationEmail,
            cooldownMinutes:   rule.cooldownMinutes,
            layoutType:        rule.layoutType || 'DEFAULT',
            customSubject:     rule.customSubject || '',
            customBody:        rule.customBody || '',
        });
        setShowForm(true);
    };

    const handleCancel = () => {
        setShowForm(false);
        setEditingRuleId(null);
        setForm({
            monitorId:         '',
            ruleType:          'INCIDENT_OPENED',
            notificationEmail: '',
            cooldownMinutes:   30,
            layoutType:        'DEFAULT',
            customSubject:     '',
            customBody:        '',
        });
        setFormError('');
    };

    const handleToggle = async (ruleId) => {
        try {
            await api.patch(`/alerts/rules/${ruleId}`);
            triggerToast('Alert rule updated');
            fetchAll();
        } catch (err) {
            console.error('Failed to toggle rule', err);
        }
    };

    const handleDeleteRequest = (ruleId) => {
        setConfirm({ open: true, ruleId });
    };

    const handleDeleteConfirm = async () => {
        const { ruleId } = confirm;
        setConfirm({ open: false, ruleId: null });
        try {
            await api.delete(`/alerts/rules/${ruleId}`);
            triggerToast('Alert rule deleted');
            fetchAll();
        } catch (err) {
            triggerToast('Failed to delete alert rule.', 'error');
        }
    };

    const handleTestAlert = async (rule) => {
        setTestStatus({ id: rule.id, loading: true });
        try {
            await api.post('/alerts/test', {
                monitorId: rule.monitorId,
                email:     rule.notificationEmail,
            });
            setTestStatus({ id: rule.id, success: true });
            triggerToast('Test alert email sent successfully');
            fetchAll();
        } catch (err) {
            setTestStatus({ id: rule.id, success: false });
            triggerToast('Failed to send test email.', 'error');
        }
        setTimeout(() => setTestStatus(null), 3000);
    };

    const getMonitorName = (monitorId) => {
        const m = monitors.find(m => m.id === monitorId);
        return m ? m.name : 'Unknown Monitor';
    };

    const formatTime = (timeStr) => {
        if (!timeStr) return 'Never';
        return new Date(timeStr).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
    };

    // ── Skeleton loader ──────────────────────────────────────────────────────
    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 p-6 md:p-8">
                <div className="max-w-7xl mx-auto space-y-6 animate-pulse">
                    <div className="h-20 bg-white rounded-xl border border-gray-200" />
                    <div className="h-64 bg-white rounded-xl border border-gray-200" />
                    <div className="h-64 bg-white rounded-xl border border-gray-200" />
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 p-6 md:p-8">
            {/* Toast */}
            {toast.message && (
                <div className={`fixed bottom-5 right-5 z-50 px-4 py-3 rounded-lg shadow-xl flex items-center gap-2 text-sm border animate-slide-in text-white ${
                    toast.type === 'success' ? 'bg-slate-900 border-white/10' : 'bg-red-600 border-red-700'
                }`}>
                    {toast.type === 'success'
                        ? <CheckCircle className="w-4 h-4 text-emerald-400" />
                        : <XCircle className="w-4 h-4 text-white" />}
                    {toast.message}
                </div>
            )}

            {/* Confirm delete dialog */}
            <ConfirmDialog
                isOpen={confirm.open}
                title="Delete Alert Rule"
                message="This alert rule will be permanently removed. Notifications to the configured email will stop immediately."
                confirmText="Delete Rule"
                onConfirm={handleDeleteConfirm}
                onCancel={() => setConfirm({ open: false, ruleId: null })}
            />

            <div className="max-w-7xl mx-auto space-y-6">

                {/* Header */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-white p-6 rounded-xl shadow-sm border border-gray-200 gap-4">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
                            <Bell className="w-7 h-7 text-blue-600" />
                            Alert Rules
                        </h1>
                        <p className="text-gray-500 mt-0.5 text-sm">
                            Get notified by email when a monitor goes down or recovers.
                        </p>
                    </div>
                    <div className="flex gap-2">
                        <button
                            onClick={fetchAll}
                            className="p-2 border border-gray-200 rounded-lg hover:bg-gray-100 text-gray-500"
                            title="Refresh"
                        >
                            <RefreshCw className="w-4 h-4" />
                        </button>
                        <button
                            onClick={() => setShowForm(!showForm)}
                            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2.5 rounded-lg transition-colors text-sm"
                        >
                            <Plus className="w-4 h-4" />
                            Create Rule
                        </button>
                    </div>
                </div>

                {/* Create/Edit Rule Form */}
                {showForm && (
                    <form onSubmit={handleSubmit} className="bg-white p-6 rounded-xl shadow-sm border border-blue-200 space-y-4 animate-fade-in">
                        <h2 className="text-base font-bold text-gray-900">{editingRuleId ? 'Edit Alert Rule' : 'New Alert Rule'}</h2>
                        {formError && (
                            <div className="p-3 bg-red-50 border border-red-200 text-red-800 text-xs rounded-lg">
                                {formError}
                            </div>
                        )}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Monitor</label>
                                <select
                                    required
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={form.monitorId}
                                    onChange={e => setForm({ ...form, monitorId: e.target.value })}
                                >
                                    <option value="">Select a monitor...</option>
                                    {monitors.map(m => (
                                        <option key={m.id} value={m.id}>{m.name}</option>
                                    ))}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Trigger When</label>
                                <select
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={form.ruleType}
                                    onChange={e => setForm({ ...form, ruleType: e.target.value })}
                                >
                                    <option value="INCIDENT_OPENED">{labelFor('INCIDENT_OPENED', RULE_TYPE_LABELS)}</option>
                                    <option value="INCIDENT_RESOLVED">{labelFor('INCIDENT_RESOLVED', RULE_TYPE_LABELS)}</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Recipient Email / Slack Webhook</label>
                                <input
                                    type="text"
                                    required
                                    placeholder="you@company.com or https://hooks.slack.com/services/..."
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={form.notificationEmail}
                                    onChange={e => setForm({ ...form, notificationEmail: e.target.value })}
                                />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Re-alert Cooldown (minutes)</label>
                                <input
                                    type="number"
                                    min="1"
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={form.cooldownMinutes}
                                    onChange={e => setForm({ ...form, cooldownMinutes: Number(e.target.value) })}
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 gap-4">
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Layout Style</label>
                                <select
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    value={form.layoutType}
                                    onChange={e => setForm({ ...form, layoutType: e.target.value })}
                                >
                                    <option value="DEFAULT">Default Layout (Standard Header & Telemetry Grid)</option>
                                    <option value="COMPACT">Compact Layout (Title and Description Only)</option>
                                    <option value="DETAILED">Detailed Layout (Diagnostics, Actions & Direct Links)</option>
                                </select>
                            </div>
                        </div>

                        <details className="group border border-gray-200 rounded-lg bg-gray-50/50 p-4 [&_summary::-webkit-details-marker]:hidden">
                            <summary className="flex items-center justify-between cursor-pointer focus:outline-none">
                                <span className="text-sm font-semibold text-gray-700 select-none flex items-center gap-2">
                                    ⚙️ Customize Notification Templates (Optional)
                                </span>
                                <span className="transition group-open:-rotate-180">
                                    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </span>
                            </summary>
                            <div className="mt-4 space-y-4 pt-4 border-t border-gray-200">
                                <div>
                                    <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">
                                        Custom Subject / Slack Title
                                    </label>
                                    <input
                                        type="text"
                                        placeholder="e.g., [Outage Alert] {{monitor_name}} is {{status}}!"
                                        className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                                        value={form.customSubject}
                                        onChange={e => setForm({ ...form, customSubject: e.target.value })}
                                    />
                                    <p className="text-[11px] text-gray-400 mt-1">Leave empty to use default subject line.</p>
                                </div>
                                <div>
                                    <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">
                                        Custom Message Body
                                    </label>
                                    <textarea
                                        rows="3"
                                        placeholder="e.g., Alert: {{monitor_name}} went down with severity {{severity}} at {{timestamp}}."
                                        className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                                        value={form.customBody}
                                        onChange={e => setForm({ ...form, customBody: e.target.value })}
                                    />
                                    <p className="text-[11px] text-gray-400 mt-1">Leave empty to use default text description.</p>
                                </div>
                                <div className="p-3 bg-blue-50/50 border border-blue-100 rounded-lg space-y-1">
                                    <h4 className="text-[11px] font-bold text-blue-800 uppercase tracking-wider">💡 Template Placeholders</h4>
                                    <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-[11px] text-blue-700">
                                        <div><code>{"{{monitor_name}}"}</code> — API/Service Name</div>
                                        <div><code>{"{{status}}"}</code> — Status (DOWN / RESOLVED)</div>
                                        <div><code>{"{{severity}}"}</code> — Severity (CRITICAL, HIGH, etc.)</div>
                                        <div><code>{"{{timestamp}}"}</code> — Alert Date & Time</div>
                                        <div><code>{"{{incident_id}}"}</code> — Incident ID</div>
                                        <div><code>{"{{duration}}"}</code> — Downtime duration (seconds)</div>
                                    </div>
                                </div>
                            </div>
                        </details>

                        <div className="flex gap-3 justify-end pt-2">
                            <button
                                type="button"
                                onClick={handleCancel}
                                className="border border-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium hover:bg-gray-50"
                            >
                                Cancel
                            </button>
                            <button
                                type="submit"
                                disabled={formLoading}
                                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-50"
                            >
                                {formLoading ? 'Saving...' : (editingRuleId ? 'Update Rule' : 'Create Rule')}
                            </button>
                        </div>
                    </form>
                )}

                {/* Rules List */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-200 bg-gray-50/50">
                        <h2 className="text-sm font-bold text-gray-700 uppercase tracking-wider">Active Alert Rules</h2>
                    </div>
                    {rules.length === 0 ? (
                        <div className="p-12 text-center text-gray-400 space-y-2">
                            <Info className="w-10 h-10 text-gray-300 mx-auto" />
                            <p className="font-semibold text-gray-600">No alert rules configured</p>
                            <p className="text-xs">Create a rule above to receive email notifications when a monitor changes state.</p>
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Monitor</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Trigger</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Layout</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Recipient</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Cooldown</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Last Sent</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Enabled</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-100">
                                    {rules.map(rule => (
                                        <tr key={rule.id} className="hover:bg-gray-50/50">
                                            <td className="px-6 py-4 font-semibold text-gray-900">{getMonitorName(rule.monitorId)}</td>
                                            <td className="px-6 py-4">
                                                <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                                                    rule.ruleType === 'INCIDENT_OPENED'
                                                        ? 'bg-red-50 text-red-700 border-red-200'
                                                        : 'bg-emerald-50 text-emerald-800 border-emerald-200'
                                                }`}>
                                                    {labelFor(rule.ruleType, RULE_TYPE_LABELS)}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold border bg-gray-50 text-gray-600 border-gray-200">
                                                    {rule.layoutType || 'DEFAULT'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 font-medium text-gray-700">{rule.notificationEmail}</td>
                                            <td className="px-6 py-4 text-gray-500 font-semibold">{rule.cooldownMinutes} min</td>
                                            <td className="px-6 py-4 text-xs font-mono text-gray-500">{formatTime(rule.lastAlertSentAt)}</td>
                                            <td className="px-6 py-4">
                                                <button onClick={() => handleToggle(rule.id)} className="text-gray-400 hover:text-blue-600 transition-colors">
                                                    {rule.enabled
                                                        ? <ToggleRight className="w-8 h-8 text-blue-600" />
                                                        : <ToggleLeft className="w-8 h-8" />}
                                                </button>
                                            </td>
                                            <td className="px-6 py-4">
                                                <div className="flex items-center justify-end gap-2">
                                                    <button
                                                        onClick={() => handleTestAlert(rule)}
                                                        className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-gray-200 text-xs font-bold text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                    >
                                                        {testStatus?.id === rule.id && testStatus?.loading ? (
                                                            <span className="animate-pulse">Sending...</span>
                                                        ) : testStatus?.id === rule.id && testStatus?.success === true ? (
                                                            <span className="text-green-600 flex items-center gap-1"><CheckCircle className="w-3.5 h-3.5" /> Sent</span>
                                                        ) : testStatus?.id === rule.id && testStatus?.success === false ? (
                                                            <span className="text-red-600 flex items-center gap-1"><XCircle className="w-3.5 h-3.5" /> Failed</span>
                                                        ) : (
                                                            <><Send className="w-3.5 h-3.5" /> Send Test Email</>
                                                        )}
                                                    </button>
                                                    <button
                                                        onClick={() => handleEditRequest(rule)}
                                                        className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                        title="Edit rule"
                                                    >
                                                        <Pencil className="w-4 h-4" />
                                                    </button>
                                                    <button
                                                        onClick={() => handleDeleteRequest(rule.id)}
                                                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                        title="Delete rule"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>

                {/* Alert History */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                    <div className="px-6 py-4 border-b border-gray-200 bg-gray-50/50">
                        <h2 className="text-sm font-bold text-gray-700 uppercase tracking-wider">Alert Delivery History</h2>
                    </div>
                    {history.length === 0 ? (
                        <div className="p-8 text-center text-gray-400 text-sm">
                            No alert deliveries yet. History will appear here once a rule fires.
                        </div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
                                <thead className="bg-gray-50">
                                    <tr>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Monitor</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Channel</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Recipient</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Sent At</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Status</th>
                                        <th className="px-6 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Details</th>
                                    </tr>
                                </thead>
                                <tbody className="bg-white divide-y divide-gray-100">
                                    {history.map(h => (
                                        <tr key={h.id} className="hover:bg-gray-50/50">
                                            <td className="px-6 py-4 font-semibold text-gray-900">{getMonitorName(h.monitorId)}</td>
                                            <td className="px-6 py-4 font-medium text-gray-600">
                                                {labelFor(h.alertType, ALERT_TYPE_LABELS)}
                                            </td>
                                            <td className="px-6 py-4 text-gray-600">{h.recipient}</td>
                                            <td className="px-6 py-4 font-mono text-xs text-gray-500">{formatTime(h.sentAt)}</td>
                                            <td className="px-6 py-4">
                                                <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${
                                                    h.status === 'SENT'
                                                        ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                                                        : 'bg-red-50 text-red-800 border-red-200'
                                                }`}>
                                                    {h.status === 'SENT' ? 'Delivered' : 'Failed'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-xs font-mono text-red-600 truncate max-w-xs" title={h.errorMessage}>
                                                {h.errorMessage || '—'}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>

            </div>
        </div>
    );
};

export default AlertRulesPage;
