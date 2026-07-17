import React, { useState, useEffect } from 'react';
import { Wrench, Plus, Trash2, Clock } from 'lucide-react';
import { api } from '../api/axiosConfig';

/**
 * MaintenancePanel — shown inside a monitor's detail view.
 * Lets users schedule and delete maintenance windows.
 */
const MaintenancePanel = ({ monitorId }) => {
    const [windows, setWindows]   = useState([]);
    const [loading, setLoading]   = useState(false);
    const [showForm, setShowForm] = useState(false);
    const [form, setForm]         = useState({ startsAt: '', endsAt: '', reason: '' });
    const [error, setError]       = useState('');

    const fetchWindows = async () => {
        try {
            const r = await api.get(`/monitors/${monitorId}/maintenance`);
            setWindows(r.data);
        } catch (e) { console.error(e); }
    };

    useEffect(() => { fetchWindows(); }, [monitorId]);

    const handleCreate = async (e) => {
        e.preventDefault();
        setError('');
        if (!form.startsAt || !form.endsAt) { setError('Start and end times are required.'); return; }
        if (new Date(form.endsAt) <= new Date(form.startsAt)) { setError('End must be after start.'); return; }
        setLoading(true);
        try {
            await api.post(`/monitors/${monitorId}/maintenance`, form);
            setForm({ startsAt: '', endsAt: '', reason: '' });
            setShowForm(false);
            fetchWindows();
        } catch (e) {
            setError(e.response?.data?.message || 'Failed to schedule window.');
        } finally { setLoading(false); }
    };

    const handleDelete = async (id) => {
        if (!confirm('Delete this maintenance window?')) return;
        try {
            await api.delete(`/monitors/maintenance/${id}`);
            fetchWindows();
        } catch (e) { console.error(e); }
    };

    const fmt = (dt) => new Date(dt).toLocaleString();
    const isActive = (w) => {
        const now = new Date();
        return new Date(w.startsAt) <= now && new Date(w.endsAt) >= now;
    };

    return (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-800 flex items-center gap-2">
                    <Wrench className="w-4 h-4 text-amber-500" />
                    Maintenance Windows
                </h3>
                <button
                    onClick={() => setShowForm(!showForm)}
                    className="flex items-center gap-1 px-3 py-1.5 bg-amber-500 text-white rounded-lg text-sm font-medium hover:bg-amber-600"
                >
                    <Plus className="w-3.5 h-3.5" /> Schedule
                </button>
            </div>

            {showForm && (
                <form onSubmit={handleCreate} className="bg-amber-50 rounded-lg p-4 mb-4 space-y-3 border border-amber-100">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div>
                            <label className="text-xs font-medium text-gray-600">Starts At</label>
                            <input type="datetime-local" value={form.startsAt}
                                onChange={e => setForm(f => ({...f, startsAt: e.target.value}))}
                                className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400"
                            />
                        </div>
                        <div>
                            <label className="text-xs font-medium text-gray-600">Ends At</label>
                            <input type="datetime-local" value={form.endsAt}
                                onChange={e => setForm(f => ({...f, endsAt: e.target.value}))}
                                className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400"
                            />
                        </div>
                    </div>
                    <div>
                        <label className="text-xs font-medium text-gray-600">Reason (optional)</label>
                        <input type="text" value={form.reason} placeholder="e.g. Scheduled database upgrade"
                            onChange={e => setForm(f => ({...f, reason: e.target.value}))}
                            className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400"
                        />
                    </div>
                    {error && <p className="text-red-600 text-xs">{error}</p>}
                    <div className="flex gap-2">
                        <button type="submit" disabled={loading}
                            className="px-4 py-2 bg-amber-500 text-white rounded-lg text-sm font-medium hover:bg-amber-600 disabled:opacity-50">
                            {loading ? 'Saving…' : 'Save'}
                        </button>
                        <button type="button" onClick={() => setShowForm(false)}
                            className="px-4 py-2 border border-gray-200 text-gray-600 rounded-lg text-sm hover:bg-gray-50">
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {windows.length === 0 ? (
                <div className="p-4 bg-gray-50 border border-gray-150 rounded-xl text-center space-y-1.5">
                    <p className="text-sm text-gray-700 font-medium">No maintenance windows scheduled.</p>
                    <p className="text-xs text-gray-500 max-w-sm mx-auto">
                        Schedule maintenance windows before deployments so alerts are temporarily suppressed.
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {windows.map(w => (
                        <div key={w.id} className={`flex items-start justify-between p-3 rounded-lg border ${
                            isActive(w) ? 'bg-amber-50 border-amber-200' : 'bg-gray-50 border-gray-100'
                        }`}>
                            <div className="text-sm">
                                <div className="flex items-center gap-2 font-medium text-gray-800">
                                    <Clock className="w-3.5 h-3.5 text-amber-500" />
                                    {fmt(w.startsAt)} → {fmt(w.endsAt)}
                                    {isActive(w) && (
                                        <span className="px-2 py-0.5 bg-amber-200 text-amber-800 rounded-full text-xs">ACTIVE</span>
                                    )}
                                </div>
                                {w.reason && <p className="text-gray-500 text-xs mt-1">{w.reason}</p>}
                            </div>
                            <button onClick={() => handleDelete(w.id)}
                                className="text-red-400 hover:text-red-600 p-1">
                                <Trash2 className="w-4 h-4" />
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MaintenancePanel;
