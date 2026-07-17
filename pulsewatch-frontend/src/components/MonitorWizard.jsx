import React, { useState } from 'react';
import {
    Globe, Shield, ShieldCheck, ChevronRight, ChevronLeft,
    CheckCircle2, AlertTriangle, X, Zap, Clock, ArrowRight,
    Lock, Unlock, Server, Plus, Trash2, Loader2,
} from 'lucide-react';
import { api } from '../api/axiosConfig';
import { formatInterval } from '../utils/enumLabels';

// ─── Constants ────────────────────────────────────────────────────────────────

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD'];
const INTERVAL_OPTIONS = [
    { label: '30 seconds', value: 30 },
    { label: '1 minute',   value: 60 },
    { label: '2 minutes',  value: 120 },
    { label: '5 minutes',  value: 300 },
    { label: '10 minutes', value: 600 },
    { label: '30 minutes', value: 1800 },
    { label: '1 hour',     value: 3600 },
];
const ASSERT_TYPES = [
    { value: 'STATUS_CODE',       label: 'Response code equals',          operator: 'EQ' },
    { value: 'BODY_CONTAINS',     label: 'Response body contains',        operator: 'CONTAINS' },
    { value: 'BODY_NOT_CONTAINS', label: 'Response body does NOT contain', operator: 'NOT_CONTAINS' },
];

const DEFAULT_FORM = {
    name:            '',
    url:             '',
    monitorType:     'HTTP',
    method:          'GET',
    intervalSeconds: 60,
    timeoutMs:       5000,
    expectedStatus:  200,
    headersJson:     null,
};

const STEPS = [
    { id: 1, label: 'Target',    Icon: Globe },
    { id: 2, label: 'Configure', Icon: Zap },
    { id: 3, label: 'Validate',  Icon: ShieldCheck },
    { id: 4, label: 'Review',    Icon: CheckCircle2 },
];

// ─── URL analysis helper ──────────────────────────────────────────────────────

function analyzeUrl(url) {
    if (!url) return null;
    const hasProtocol = /^https?:\/\//i.test(url);
    const isHttps = /^https:\/\//i.test(url);
    let suggestedName = '';
    try {
        const parsed = new URL(hasProtocol ? url : 'http://' + url);
        let host = parsed.hostname.replace(/^www\./i, '');
        const parts = host.split('.');
        if (host === 'localhost' || host === '127.0.0.1') {
            suggestedName = 'Local Service';
        } else if (parts.length > 0) {
            suggestedName = parts[0].charAt(0).toUpperCase() + parts[0].slice(1);
            if (parts[0].toLowerCase() === 'api' && parts.length > 2) {
                suggestedName = parts[1].charAt(0).toUpperCase() + parts[1].slice(1) + ' API';
            }
        }
    } catch {
        const m = url.match(/^(?:https?:\/\/)?([^/?#:]+)/i);
        if (m?.[1]) {
            suggestedName = m[1].replace(/^www\./i, '').split('.')[0];
            suggestedName = suggestedName.charAt(0).toUpperCase() + suggestedName.slice(1);
        }
    }
    return { hasProtocol, isHttps, suggestedName: suggestedName || 'New Monitor' };
}

// ─── Step progress bar ────────────────────────────────────────────────────────

const StepBar = ({ current }) => (
    <div className="flex items-center px-6 py-4 border-b border-gray-100 bg-gray-50/50">
        {STEPS.map((step, idx) => {
            const done   = step.id < current;
            const active = step.id === current;
            const { Icon } = step;
            return (
                <React.Fragment key={step.id}>
                    <div className={`flex items-center gap-2 transition-all ${active ? 'opacity-100' : done ? 'opacity-70' : 'opacity-35'}`}>
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 text-xs font-bold border-2 transition-all ${
                            done   ? 'bg-emerald-500 border-emerald-500 text-white' :
                            active ? 'bg-blue-600 border-blue-600 text-white scale-110' :
                                     'bg-white border-gray-300 text-gray-400'
                        }`}>
                            {done ? <CheckCircle2 className="w-4 h-4" /> : <Icon className="w-3.5 h-3.5" />}
                        </div>
                        <span className={`text-xs font-semibold hidden sm:block ${active ? 'text-blue-700' : done ? 'text-emerald-700' : 'text-gray-400'}`}>
                            {step.label}
                        </span>
                    </div>
                    {idx < STEPS.length - 1 && (
                        <div className={`flex-1 h-0.5 mx-2 rounded-full transition-all ${done ? 'bg-emerald-400' : 'bg-gray-200'}`} />
                    )}
                </React.Fragment>
            );
        })}
    </div>
);

// ─── Step 1 — Target URL ──────────────────────────────────────────────────────

const Step1Target = ({ form, setForm, onNext, onClose }) => {
    const [input, setInput] = useState(form.url);
    const analysis = analyzeUrl(input);

    const handleNext = (e) => {
        e.preventDefault();
        const trimmed = input.trim();
        if (!trimmed) return;
        const a = analyzeUrl(trimmed);
        setForm(f => ({ ...f, url: trimmed, name: f.name || a?.suggestedName || '' }));
        onNext();
    };

    return (
        <form onSubmit={handleNext} className="flex flex-col flex-1">
            <div className="flex-1 p-6 space-y-5 overflow-y-auto">
                <div className="text-center space-y-1 pb-1">
                    <div className="w-12 h-12 bg-blue-50 border border-blue-100 rounded-2xl flex items-center justify-center mx-auto mb-3">
                        <Globe className="w-6 h-6 text-blue-600" />
                    </div>
                    <h2 className="text-xl font-bold text-gray-900">What do you want to monitor?</h2>
                    <p className="text-sm text-gray-500">Enter the URL of the endpoint, API, or website you want to track.</p>
                </div>

                <input
                    type="text" autoFocus required
                    placeholder="https://api.yourservice.com/health"
                    className="w-full border-2 border-gray-200 focus:border-blue-500 rounded-xl px-4 py-3.5 text-sm font-mono transition-colors outline-none"
                    value={input}
                    onChange={e => setInput(e.target.value)}
                />

                {/* Live analysis card */}
                {input && (
                    <div className={`rounded-xl border p-4 space-y-2 ${
                        analysis?.isHttps     ? 'bg-emerald-50 border-emerald-200' :
                        analysis?.hasProtocol ? 'bg-amber-50 border-amber-200' :
                                               'bg-red-50 border-red-200'
                    }`}>
                        <h4 className="text-[10px] font-bold uppercase tracking-wider text-gray-500">URL Analysis</h4>
                        <div className="space-y-1.5 text-xs">
                            {analysis?.isHttps ? (
                                <div className="flex items-center gap-2"><Lock className="w-3.5 h-3.5 text-emerald-600" />
                                    <span className="font-semibold text-emerald-800">HTTPS — encrypted transport</span></div>
                            ) : analysis?.hasProtocol ? (
                                <div className="flex items-center gap-2"><Unlock className="w-3.5 h-3.5 text-amber-600" />
                                    <span className="font-semibold text-amber-800">HTTP — unencrypted. Consider HTTPS if available.</span></div>
                            ) : (
                                <div className="flex items-center gap-2"><AlertTriangle className="w-3.5 h-3.5 text-red-600" />
                                    <span className="font-semibold text-red-800">Missing protocol — add https:// prefix.</span></div>
                            )}
                            {analysis?.suggestedName && (
                                <div className="text-gray-600">
                                    Suggested name: <strong className="text-gray-900">{analysis.suggestedName}</strong>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Quick-start templates */}
                {!input && (
                    <div className="space-y-2">
                        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Quick-start templates</p>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            {[
                                { label: 'REST API',        url: 'https://api.github.com',                       badge: 'API' },
                                { label: 'Health endpoint', url: 'https://httpbin.org/status/200',               badge: 'Health' },
                                { label: 'JSON API',        url: 'https://jsonplaceholder.typicode.com/posts/1', badge: 'JSON' },
                                { label: 'Static site',     url: 'https://github.com',                          badge: 'Website' },
                            ].map(t => (
                                <button key={t.url} type="button" onClick={() => setInput(t.url)}
                                    className="flex items-center justify-between p-3 bg-white border border-gray-200 rounded-xl text-xs hover:border-blue-300 hover:bg-blue-50/30 transition-all text-left group">
                                    <span className="font-semibold text-gray-800 group-hover:text-blue-700">{t.label}</span>
                                    <span className="px-1.5 py-0.5 rounded bg-gray-100 text-gray-500 font-mono group-hover:bg-blue-100 group-hover:text-blue-700">{t.badge}</span>
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </div>

            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50/50">
                <button type="button" onClick={onClose} className="text-sm text-gray-500 hover:text-gray-800 px-3 py-2 transition-colors">
                    Cancel
                </button>
                <button type="submit" disabled={!input.trim()}
                    className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white rounded-xl text-sm font-semibold transition-all">
                    Next <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </form>
    );
};

// ─── Step 2 — Configure ───────────────────────────────────────────────────────

const Step2Configure = ({ form, setForm, onNext, onBack }) => {
    const [nameManuallyEdited, setNameManuallyEdited] = useState(!!form.name);
    const analysis = analyzeUrl(form.url);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!form.name.trim()) return;
        onNext();
    };

    const maxTimeout = Math.min(30000, form.intervalSeconds * 1000 - 1000);

    return (
        <form onSubmit={handleSubmit} className="flex flex-col flex-1">
            <div className="flex-1 p-6 space-y-5 overflow-y-auto">
                <div className="text-center space-y-1 pb-1">
                    <div className="w-12 h-12 bg-purple-50 border border-purple-100 rounded-2xl flex items-center justify-center mx-auto mb-3">
                        <Zap className="w-6 h-6 text-purple-600" />
                    </div>
                    <h2 className="text-xl font-bold text-gray-900">Configure the check</h2>
                    <p className="text-sm text-gray-500 font-mono text-xs truncate px-4">{form.url}</p>
                </div>

                {/* Name */}
                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">Monitor Name</label>
                    <input type="text" required autoFocus
                        className="w-full border-2 border-gray-200 focus:border-blue-500 rounded-xl px-4 py-2.5 text-sm transition-colors outline-none"
                        value={form.name}
                        placeholder="e.g. Core API Health"
                        onChange={e => { setNameManuallyEdited(true); setForm(f => ({ ...f, name: e.target.value })); }}
                    />
                    {!nameManuallyEdited && analysis?.suggestedName && (
                        <p className="text-xs text-gray-400 mt-1">
                            Suggested: <button type="button" className="text-blue-500 hover:underline font-medium"
                                onClick={() => { setForm(f => ({ ...f, name: analysis.suggestedName })); setNameManuallyEdited(true); }}>
                                Use "{analysis.suggestedName}"
                            </button>
                        </p>
                    )}
                </div>

                <div className="grid grid-cols-2 gap-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">HTTP Method</label>
                        <select className="w-full border-2 border-gray-200 focus:border-blue-500 rounded-xl px-3 py-2.5 text-sm bg-white outline-none"
                            value={form.method} onChange={e => setForm(f => ({ ...f, method: e.target.value }))}>
                            {HTTP_METHODS.map(m => <option key={m} value={m}>{m}</option>)}
                        </select>
                        <p className="text-[10px] text-gray-400 mt-1">GET is best for health checks.</p>
                    </div>
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">Expected Status</label>
                        <input type="number"
                            className="w-full border-2 border-gray-200 focus:border-blue-500 rounded-xl px-3 py-2.5 text-sm outline-none"
                            value={form.expectedStatus}
                            onChange={e => setForm(f => ({ ...f, expectedStatus: Number(e.target.value) }))}
                        />
                        <p className="text-[10px] text-gray-400 mt-1">HTTP code that means "healthy".</p>
                    </div>
                </div>

                {/* Interval pills */}
                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-2">Check Interval</label>
                    <div className="flex flex-wrap gap-2">
                        {INTERVAL_OPTIONS.map(opt => (
                            <button key={opt.value} type="button"
                                onClick={() => setForm(f => ({ ...f, intervalSeconds: opt.value, timeoutMs: Math.min(f.timeoutMs, opt.value * 1000 - 1000) }))}
                                className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
                                    form.intervalSeconds === opt.value
                                        ? 'bg-blue-600 text-white border-blue-600 scale-105 shadow-sm'
                                        : 'bg-white text-gray-600 border-gray-200 hover:border-blue-300 hover:bg-blue-50'
                                }`}>
                                {opt.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Timeout slider */}
                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1.5">
                        Request Timeout <span className="text-blue-600 font-bold">{form.timeoutMs / 1000}s</span>
                    </label>
                    <input type="range" min={1000} max={maxTimeout} step={1000}
                        className="w-full accent-blue-600"
                        value={Math.min(form.timeoutMs, maxTimeout)}
                        onChange={e => setForm(f => ({ ...f, timeoutMs: Number(e.target.value) }))}
                    />
                    <div className="flex justify-between text-[10px] text-gray-400 mt-0.5">
                        <span>1s</span><span>{maxTimeout / 1000}s max</span>
                    </div>
                </div>
            </div>

            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50/50">
                <button type="button" onClick={onBack}
                    className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors px-3 py-2">
                    <ChevronLeft className="w-4 h-4" /> Back
                </button>
                <button type="submit" disabled={!form.name.trim()}
                    className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white rounded-xl text-sm font-semibold transition-all">
                    Next <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </form>
    );
};

// ─── Step 3 — Assertions ──────────────────────────────────────────────────────

const Step3Validate = ({ assertions, setAssertions, onNext, onBack }) => {
    const [showForm, setShowForm] = useState(false);
    const [newA, setNewA] = useState({ assertType: 'STATUS_CODE', expected: '' });

    const addAssertion = () => {
        if (!newA.expected.trim()) return;
        const typeDef = ASSERT_TYPES.find(t => t.value === newA.assertType);
        setAssertions(prev => [...prev, { id: Date.now(), assertType: newA.assertType, operator: typeDef.operator, expected: newA.expected }]);
        setNewA({ assertType: 'STATUS_CODE', expected: '' });
        setShowForm(false);
    };

    const removeAssertion = (id) => setAssertions(prev => prev.filter(a => a.id !== id));

    const describeAssertion = (a) => {
        if (a.assertType === 'STATUS_CODE')       return `Status code = ${a.expected}`;
        if (a.assertType === 'BODY_CONTAINS')     return `Body contains "${a.expected}"`;
        if (a.assertType === 'BODY_NOT_CONTAINS') return `Body does NOT contain "${a.expected}"`;
        return `${a.assertType} ${a.operator} ${a.expected}`;
    };

    return (
        <div className="flex flex-col flex-1">
            <div className="flex-1 p-6 space-y-5 overflow-y-auto">
                <div className="text-center space-y-1 pb-1">
                    <div className="w-12 h-12 bg-emerald-50 border border-emerald-100 rounded-2xl flex items-center justify-center mx-auto mb-3">
                        <ShieldCheck className="w-6 h-6 text-emerald-600" />
                    </div>
                    <h2 className="text-xl font-bold text-gray-900">Add validation rules</h2>
                    <p className="text-sm text-gray-500">Assertions catch failures the status code alone can't detect. <span className="text-blue-500 font-medium">Optional.</span></p>
                </div>

                {assertions.length === 0 && !showForm && (
                    <div className="bg-gray-50 border border-dashed border-gray-300 rounded-xl p-5 text-center space-y-3">
                        <div className="text-xs text-gray-500 space-y-1">
                            <p className="font-semibold text-gray-700">No assertions yet</p>
                            <p>Example: ensure body contains <code className="bg-white px-1 py-0.5 rounded border border-gray-200">"status":"ok"</code></p>
                        </div>
                        <button type="button" onClick={() => setShowForm(true)}
                            className="flex items-center gap-1.5 mx-auto px-4 py-2 bg-white border border-gray-200 text-gray-700 rounded-xl text-xs font-semibold hover:border-blue-400 hover:bg-blue-50 transition-all">
                            <Plus className="w-3.5 h-3.5" /> Add assertion
                        </button>
                    </div>
                )}

                {assertions.length > 0 && (
                    <div className="space-y-2">
                        {assertions.map(a => (
                            <div key={a.id} className="flex items-center justify-between p-3 bg-emerald-50 border border-emerald-100 rounded-xl text-sm">
                                <div className="flex items-center gap-2">
                                    <CheckCircle2 className="w-4 h-4 text-emerald-500 flex-shrink-0" />
                                    <span className="font-medium text-emerald-900">{describeAssertion(a)}</span>
                                </div>
                                <button onClick={() => removeAssertion(a.id)} className="text-red-400 hover:text-red-600 p-1 transition-colors">
                                    <Trash2 className="w-4 h-4" />
                                </button>
                            </div>
                        ))}
                        {!showForm && (
                            <button type="button" onClick={() => setShowForm(true)}
                                className="flex items-center gap-1.5 text-blue-600 text-xs font-semibold hover:text-blue-800 transition-colors px-1 py-1">
                                <Plus className="w-3.5 h-3.5" /> Add another
                            </button>
                        )}
                    </div>
                )}

                {showForm && (
                    <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 space-y-3">
                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 mb-1">Rule Type</label>
                                <select className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs bg-white outline-none focus:ring-2 focus:ring-blue-400"
                                    value={newA.assertType}
                                    onChange={e => setNewA(a => ({ ...a, assertType: e.target.value, expected: '' }))}>
                                    {ASSERT_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-gray-600 mb-1">
                                    {newA.assertType === 'STATUS_CODE' ? 'Status Code' : 'String Value'}
                                </label>
                                <input
                                    type={newA.assertType === 'STATUS_CODE' ? 'number' : 'text'}
                                    placeholder={newA.assertType === 'STATUS_CODE' ? '200' : '"status":"ok"'}
                                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs outline-none focus:ring-2 focus:ring-blue-400"
                                    value={newA.expected}
                                    onChange={e => setNewA(a => ({ ...a, expected: e.target.value }))}
                                />
                            </div>
                        </div>
                        <div className="flex gap-2">
                            <button type="button" onClick={addAssertion} disabled={!newA.expected.trim()}
                                className="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-semibold hover:bg-blue-700 disabled:opacity-40 transition-all">
                                Add Rule
                            </button>
                            <button type="button" onClick={() => { setShowForm(false); setNewA({ assertType: 'STATUS_CODE', expected: '' }); }}
                                className="px-3 py-1.5 border border-gray-200 text-gray-600 rounded-lg text-xs hover:bg-gray-50 transition-all">
                                Cancel
                            </button>
                        </div>
                    </div>
                )}
            </div>

            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50/50">
                <button type="button" onClick={onBack}
                    className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors px-3 py-2">
                    <ChevronLeft className="w-4 h-4" /> Back
                </button>
                <button type="button" onClick={onNext}
                    className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-all">
                    {assertions.length > 0 ? `Continue with ${assertions.length} rule${assertions.length > 1 ? 's' : ''}` : 'Skip for now'}
                    <ChevronRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};

// ─── Step 4 — Review & Launch ─────────────────────────────────────────────────

const Step4Review = ({ form, assertions, onBack, onClose, onSuccess }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError]     = useState('');
    const analysis = analyzeUrl(form.url);

    const handleLaunch = async () => {
        setError('');
        setLoading(true);
        try {
            const res = await api.post('/monitors', {
                name:            form.name,
                url:             form.url,
                monitorType:     form.monitorType,
                method:          form.method,
                intervalSeconds: form.intervalSeconds,
                timeoutMs:       form.timeoutMs,
                expectedStatus:  form.expectedStatus,
                headersJson:     form.headersJson,
            });
            const monitorId = res.data.id;
            for (const a of assertions) {
                await api.post(`/monitors/${monitorId}/assertions`, {
                    assertType: a.assertType,
                    operator:   a.operator,
                    expected:   String(a.expected),
                });
            }
            onSuccess(monitorId);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create monitor. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col flex-1">
            <div className="flex-1 p-6 space-y-5 overflow-y-auto">
                <div className="text-center space-y-1 pb-1">
                    <div className="w-12 h-12 bg-blue-50 border border-blue-100 rounded-2xl flex items-center justify-center mx-auto mb-3">
                        <CheckCircle2 className="w-6 h-6 text-blue-600" />
                    </div>
                    <h2 className="text-xl font-bold text-gray-900">Ready to launch</h2>
                    <p className="text-sm text-gray-500">Review your configuration, then activate monitoring.</p>
                </div>

                {/* Dark summary card */}
                <div className="bg-gradient-to-br from-slate-900 to-indigo-950 rounded-2xl p-5 text-white space-y-4">
                    <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                            <p className="text-xs text-slate-400 uppercase tracking-wider mb-1">Monitor</p>
                            <p className="text-lg font-bold">{form.name}</p>
                            <p className="text-xs font-mono text-slate-300 mt-0.5 break-all">{form.url}</p>
                        </div>
                        <span className={`flex items-center gap-1.5 px-2 py-1 rounded-full text-xs font-semibold flex-shrink-0 ${
                            analysis?.isHttps ? 'bg-emerald-500/20 text-emerald-300' : 'bg-amber-500/20 text-amber-300'
                        }`}>
                            {analysis?.isHttps ? <Lock className="w-3 h-3" /> : <Unlock className="w-3 h-3" />}
                            {analysis?.isHttps ? 'HTTPS' : 'HTTP'}
                        </span>
                    </div>

                    <div className="grid grid-cols-3 gap-3 border-t border-white/10 pt-4">
                        <div>
                            <p className="text-[10px] text-slate-400 uppercase tracking-wider">Interval</p>
                            <p className="font-bold text-sm mt-0.5">{formatInterval(form.intervalSeconds)}</p>
                        </div>
                        <div>
                            <p className="text-[10px] text-slate-400 uppercase tracking-wider">Timeout</p>
                            <p className="font-bold text-sm mt-0.5">{form.timeoutMs / 1000}s</p>
                        </div>
                        <div>
                            <p className="text-[10px] text-slate-400 uppercase tracking-wider">Method / Code</p>
                            <p className="font-bold text-sm mt-0.5">{form.method} → {form.expectedStatus}</p>
                        </div>
                    </div>

                    {assertions.length > 0 && (
                        <div className="border-t border-white/10 pt-3 space-y-1.5">
                            <p className="text-[10px] text-slate-400 uppercase tracking-wider">Assertions ({assertions.length})</p>
                            {assertions.map(a => (
                                <div key={a.id} className="flex items-center gap-2 text-xs text-slate-200">
                                    <ShieldCheck className="w-3 h-3 text-emerald-400 flex-shrink-0" />
                                    {a.assertType === 'STATUS_CODE'       && `Status code = ${a.expected}`}
                                    {a.assertType === 'BODY_CONTAINS'     && `Body contains "${a.expected}"`}
                                    {a.assertType === 'BODY_NOT_CONTAINS' && `Body NOT contains "${a.expected}"`}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="bg-blue-50 border border-blue-100 rounded-xl p-4 space-y-1.5">
                    <p className="text-xs font-bold text-blue-800">What happens next</p>
                    <ul className="text-xs text-blue-700 space-y-1">
                        <li className="flex items-center gap-2"><ArrowRight className="w-3 h-3" /> Monitor is created and immediately scheduled</li>
                        <li className="flex items-center gap-2"><ArrowRight className="w-3 h-3" /> First check runs within a few seconds</li>
                        <li className="flex items-center gap-2"><ArrowRight className="w-3 h-3" /> Alert rules can be added from the monitor detail page</li>
                    </ul>
                </div>

                {error && (
                    <div className="p-3 bg-red-50 border border-red-200 text-red-800 text-sm rounded-xl flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 flex-shrink-0 text-red-600" />
                        {error}
                    </div>
                )}
            </div>

            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 bg-gray-50/50">
                <button type="button" onClick={onBack} disabled={loading}
                    className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-800 transition-colors px-3 py-2 disabled:opacity-40">
                    <ChevronLeft className="w-4 h-4" /> Back
                </button>
                <button type="button" onClick={handleLaunch} disabled={loading}
                    className="flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 disabled:opacity-60 text-white rounded-xl text-sm font-bold shadow-md shadow-blue-200 transition-all">
                    {loading
                        ? <><Loader2 className="w-4 h-4 animate-spin" /> Creating…</>
                        : <><Zap className="w-4 h-4" /> Launch Monitor</>
                    }
                </button>
            </div>
        </div>
    );
};

// ─── Success screen ───────────────────────────────────────────────────────────

const SuccessScreen = ({ monitorName, onClose }) => (
    <div className="flex flex-col flex-1 items-center justify-center p-8 text-center space-y-5">
        <div className="relative">
            <div className="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center animate-bounce">
                <CheckCircle2 className="w-10 h-10 text-emerald-500" />
            </div>
            <div className="absolute -top-1 -right-1 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center">
                <Zap className="w-3.5 h-3.5 text-white" />
            </div>
        </div>
        <div className="space-y-1.5">
            <h2 className="text-2xl font-black text-gray-900">Monitor Live!</h2>
            <p className="text-gray-500 text-sm">
                <span className="font-semibold text-gray-800">{monitorName}</span> is now active.<br />
                The first check will run in a few seconds.
            </p>
        </div>
        <button onClick={onClose}
            className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-sm font-semibold transition-all">
            View Monitor
        </button>
    </div>
);

// ─── Main Wizard ──────────────────────────────────────────────────────────────

const MonitorWizard = ({ initialUrl = '', initialName = '', onClose, onSuccess }) => {
    const [step, setStep]             = useState(1);
    const [form, setForm]             = useState({ ...DEFAULT_FORM, url: initialUrl, name: initialName });
    const [assertions, setAssertions] = useState([]);
    const [createdId, setCreatedId]   = useState(null);

    const handleSuccess = (monitorId) => {
        setCreatedId(monitorId);
        setStep(5);
    };

    const handleClose = () => {
        onSuccess?.(createdId);
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
            <div className="bg-white rounded-2xl shadow-2xl border border-gray-100 w-full max-w-lg flex flex-col overflow-hidden"
                style={{ maxHeight: '90vh', minHeight: '560px' }}>

                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div className="flex items-center gap-2">
                        <Server className="w-5 h-5 text-blue-600" />
                        <span className="font-bold text-gray-900 text-sm">New Monitor</span>
                    </div>
                    <button onClick={onClose}
                        className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400 hover:text-gray-700 transition-colors">
                        <X className="w-4 h-4" />
                    </button>
                </div>

                {/* Step bar */}
                {step <= 4 && <StepBar current={step} />}

                {/* Steps */}
                {step === 1 && <Step1Target    form={form} setForm={setForm} onNext={() => setStep(2)} onClose={onClose} />}
                {step === 2 && <Step2Configure form={form} setForm={setForm} onNext={() => setStep(3)} onBack={() => setStep(1)} />}
                {step === 3 && <Step3Validate  assertions={assertions} setAssertions={setAssertions} onNext={() => setStep(4)} onBack={() => setStep(2)} />}
                {step === 4 && <Step4Review    form={form} assertions={assertions} onBack={() => setStep(3)} onClose={onClose} onSuccess={handleSuccess} />}
                {step === 5 && <SuccessScreen  monitorName={form.name} onClose={handleClose} />}
            </div>
        </div>
    );
};

export default MonitorWizard;
