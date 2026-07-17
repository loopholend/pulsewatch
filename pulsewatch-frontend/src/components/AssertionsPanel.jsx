import React, { useState, useEffect } from 'react';
import { ShieldCheck, Plus, Trash2 } from 'lucide-react';
import { api } from '../api/axiosConfig';
import { ASSERT_TYPE_LABELS } from '../utils/enumLabels';

const ASSERT_TYPES = ['STATUS_CODE', 'BODY_CONTAINS', 'BODY_NOT_CONTAINS'];
const OPERATORS = {
    STATUS_CODE:       ['EQ'],
    BODY_CONTAINS:     ['CONTAINS'],
    BODY_NOT_CONTAINS: ['NOT_CONTAINS'],
};

const AssertionsPanel = ({ monitorId }) => {
    const [assertions, setAssertions] = useState([]);
    const [showForm, setShowForm]     = useState(false);
    const [form, setForm]             = useState({ assertType: 'STATUS_CODE', operator: 'EQ', expected: '' });
    const [loading, setLoading]       = useState(false);
    const [error, setError]           = useState('');

    const fetchAssertions = async () => {
        try {
            const r = await api.get(`/monitors/${monitorId}/assertions`);
            setAssertions(r.data);
        } catch (e) { console.error(e); }
    };

    useEffect(() => { fetchAssertions(); }, [monitorId]);

    // Auto-select operator when assertType changes
    const handleTypeChange = (e) => {
        const type = e.target.value;
        const op   = OPERATORS[type][0];
        setForm(f => ({ ...f, assertType: type, operator: op }));
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setError('');
        if (!form.expected.trim()) { setError('Expected value is required.'); return; }
        setLoading(true);
        try {
            await api.post(`/monitors/${monitorId}/assertions`, form);
            setForm({ assertType: 'STATUS_CODE', operator: 'EQ', expected: '' });
            setShowForm(false);
            fetchAssertions();
        } catch (e) {
            setError(e.response?.data?.message || 'Failed to add assertion.');
        } finally { setLoading(false); }
    };

    const handleDelete = async (assertionId) => {
        try {
            await api.delete(`/monitors/${monitorId}/assertions/${assertionId}`);
            fetchAssertions();
        } catch (e) { console.error(e); }
    };

    const describeAssertion = (a) => {
        const labels = {
            STATUS_CODE:       `Status code == ${a.expected}`,
            BODY_CONTAINS:     `Body contains "${a.expected}"`,
            BODY_NOT_CONTAINS: `Body does NOT contain "${a.expected}"`,
        };
        return labels[a.assertType] || `${a.assertType} ${a.operator} ${a.expected}`;
    };

    return (
        <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-5">
            <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold text-gray-800 flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-blue-500" />
                    Assertions
                    <span className="text-xs text-gray-400 font-normal">— all must pass for a check to be UP</span>
                </h3>
                <button
                    onClick={() => setShowForm(!showForm)}
                    className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700"
                >
                    <Plus className="w-3.5 h-3.5" /> Add
                </button>
            </div>

            {showForm && (
                <form onSubmit={handleCreate} className="bg-blue-50 rounded-lg p-4 mb-4 space-y-3 border border-blue-100">
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                        <div>
                            <label className="text-xs font-medium text-gray-600">Type</label>
                            <select value={form.assertType} onChange={handleTypeChange}
                                className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                {ASSERT_TYPES.map(t => (
                                    <option key={t} value={t}>{ASSERT_TYPE_LABELS[t] ?? t}</option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="text-xs font-medium text-gray-600">Operator</label>
                            <input readOnly value={form.operator}
                                className="mt-1 w-full border border-gray-100 bg-gray-100 rounded-lg px-3 py-2 text-sm text-gray-500 cursor-not-allowed" />
                        </div>
                        <div>
                            <label className="text-xs font-medium text-gray-600">
                                {form.assertType === 'STATUS_CODE' ? 'Expected Status (e.g. 200)' : 'Expected String'}
                            </label>
                            <input type="text" value={form.expected}
                                onChange={e => setForm(f => ({...f, expected: e.target.value}))}
                                placeholder={form.assertType === 'STATUS_CODE' ? '200' : 'healthy'}
                                className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                            />
                        </div>
                    </div>
                    {error && <p className="text-red-600 text-xs">{error}</p>}
                    <div className="flex gap-2">
                        <button type="submit" disabled={loading}
                            className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
                            {loading ? 'Saving…' : 'Add Assertion'}
                        </button>
                        <button type="button" onClick={() => setShowForm(false)}
                            className="px-4 py-2 border border-gray-200 text-gray-600 rounded-lg text-sm hover:bg-gray-50">
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {assertions.length === 0 ? (
                <div className="p-4 bg-gray-50 border border-gray-150 rounded-xl space-y-2 text-center">
                    <p className="text-sm text-gray-700 font-medium">Assertions validate API responses beyond HTTP status.</p>
                    <div className="text-xs text-gray-500 max-w-sm mx-auto space-y-0.5">
                        <p>✓ Status = 200</p>
                        <p>✓ Body contains 'healthy'</p>
                        <p>✓ Header exists</p>
                    </div>
                    <p className="text-xs text-blue-600 font-bold mt-1">Add your first assertion.</p>
                </div>
            ) : (
                <div className="space-y-2">
                    {assertions.map(a => (
                        <div key={a.id} className="flex items-center justify-between p-3 bg-blue-50 rounded-lg border border-blue-100">
                            <span className="text-sm font-medium text-blue-900">{describeAssertion(a)}</span>
                            <button onClick={() => handleDelete(a.id)}
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

export default AssertionsPanel;
