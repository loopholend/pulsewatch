import React, { useState, useEffect } from 'react';
import { Globe, Plus, Trash2, ExternalLink, AlertCircle, CheckCircle, Edit2, Copy, Check } from 'lucide-react';
import { api } from '../api/axiosConfig';
import ConfirmDialog from './ui/ConfirmDialog';

/** Convert a title string into a URL-safe slug */
const slugify = (str) =>
    str.toLowerCase().trim()
        .replace(/[^a-z0-9\s-]/g, '')
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');

const EMPTY_FORM = { slug: '', title: '', description: '', monitorIds: [] };

const StatusPagesManager = () => {
    const [monitors, setMonitors] = useState([]);
    const [pages, setPages] = useState([]);
    const [loading, setLoading] = useState(true);

    // Create form
    const [showForm, setShowForm] = useState(false);
    const [form, setForm] = useState(EMPTY_FORM);
    const [slugManuallyEdited, setSlugManuallyEdited] = useState(false);
    const [formError, setFormError] = useState('');

    // Edit modal
    const [editPage, setEditPage] = useState(null); // the status page being edited
    const [editForm, setEditForm] = useState(EMPTY_FORM);
    const [editSlugManuallyEdited, setEditSlugManuallyEdited] = useState(false);
    const [editError, setEditError] = useState('');

    // Clipboard copy state
    const [copiedSlug, setCopiedSlug] = useState(null);

    // Toast
    const [toast, setToast] = useState({ message: '', type: 'success' });
    const triggerToast = (message, type = 'success') => {
        setToast({ message, type });
        setTimeout(() => setToast({ message: '', type: 'success' }), 4000);
    };
    // Confirm delete dialog
    const [confirmDialog, setConfirmDialog] = useState({ open: false, pageId: null });

    useEffect(() => { fetchAll(); }, []);

    const fetchAll = async () => {
        setLoading(true);
        try {
            const [monitorsRes, pagesRes] = await Promise.all([
                api.get('/monitors?size=100'),
                api.get('/status-pages')
            ]);
            setMonitors(monitorsRes.data.content || []);
            setPages(pagesRes.data);
        } catch (err) {
            console.error('Failed to fetch data', err);
        } finally {
            setLoading(false);
        }
    };

    // ── Create helpers ────────────────────────────────────────────────────────

    const handleTitleChange = (e) => {
        const title = e.target.value;
        setForm(f => ({
            ...f,
            title,
            slug: slugManuallyEdited ? f.slug : slugify(title),
        }));
    };

    const handleSlugChange = (e) => {
        setSlugManuallyEdited(true);
        setForm(f => ({ ...f, slug: e.target.value.toLowerCase() }));
    };

    const toggleMonitor = (monitorId) => {
        setForm(f => ({
            ...f,
            monitorIds: f.monitorIds.includes(monitorId)
                ? f.monitorIds.filter(id => id !== monitorId)
                : [...f.monitorIds, monitorId]
        }));
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        setFormError('');
        if (!/^[a-z0-9-]+$/.test(form.slug)) {
            setFormError('Slug must contain only lowercase letters, numbers, and hyphens.');
            return;
        }
        try {
            await api.post('/status-pages', form);
            setShowForm(false);
            setForm(EMPTY_FORM);
            setSlugManuallyEdited(false);
            triggerToast('Status page created successfully');
            fetchAll();
        } catch (err) {
            setFormError(err.response?.data?.message || 'Failed to create status page. Please try again.');
        }
    };

    // ── Edit helpers ──────────────────────────────────────────────────────────

    const openEditModal = (page) => {
        const attachedIds = page.services?.map(s => s.monitorId) || [];
        setEditForm({
            slug: page.slug,
            title: page.title,
            description: page.description || '',
            monitorIds: attachedIds,
        });
        setEditSlugManuallyEdited(true); // existing slug should not auto-change
        setEditError('');
        setEditPage(page);
    };

    const handleEditTitleChange = (e) => {
        const title = e.target.value;
        setEditForm(f => ({
            ...f,
            title,
            slug: editSlugManuallyEdited ? f.slug : slugify(title),
        }));
    };

    const toggleEditMonitor = (monitorId) => {
        setEditForm(f => ({
            ...f,
            monitorIds: f.monitorIds.includes(monitorId)
                ? f.monitorIds.filter(id => id !== monitorId)
                : [...f.monitorIds, monitorId]
        }));
    };

    const handleEditSave = async (e) => {
        e.preventDefault();
        setEditError('');
        if (!/^[a-z0-9-]+$/.test(editForm.slug)) {
            setEditError('Slug must contain only lowercase letters, numbers, and hyphens.');
            return;
        }
        try {
            await api.put(`/status-pages/${editPage.id}`, editForm);
            setEditPage(null);
            triggerToast('Status page updated successfully');
            fetchAll();
        } catch (err) {
            setEditError(err.response?.data?.message || 'Failed to update status page.');
        }
    };

    // ── Delete helpers ────────────────────────────────────────────────────────

    const handleDeleteRequest = (pageId) => setConfirmDialog({ open: true, pageId });

    const handleDeleteConfirm = async () => {
        const { pageId } = confirmDialog;
        setConfirmDialog({ open: false, pageId: null });
        try {
            await api.delete(`/status-pages/${pageId}`);
            triggerToast('Status page deleted');
            fetchAll();
        } catch (err) {
            triggerToast('Failed to delete status page.', 'error');
        }
    };

    // ── Clipboard ─────────────────────────────────────────────────────────────

    const copyUrl = (slug) => {
        navigator.clipboard.writeText(`${window.location.origin}/status/${slug}`);
        setCopiedSlug(slug);
        setTimeout(() => setCopiedSlug(null), 2000);
    };

    const publicUrl = (slug) => `${window.location.origin}/status/${slug}`;

    // ── Render ────────────────────────────────────────────────────────────────

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 p-8">
                <div className="max-w-5xl mx-auto space-y-8 animate-pulse">
                    <div className="h-20 bg-white rounded-xl border border-gray-200" />
                    <div className="h-48 bg-white rounded-xl border border-gray-200" />
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 p-8">
            {/* Toast */}
            {toast.message && (
                <div className={`fixed bottom-5 right-5 z-50 px-4 py-3 rounded-lg shadow-xl flex items-center gap-2 text-sm border animate-slide-in text-white ${
                    toast.type === 'success' ? 'bg-slate-900 border-white/10' : 'bg-red-600 border-red-700'
                }`}>
                    {toast.type === 'success'
                        ? <CheckCircle className="w-4 h-4 text-emerald-400" />
                        : <AlertCircle className="w-4 h-4" />}
                    {toast.message}
                </div>
            )}

            {/* Confirm delete */}
            <ConfirmDialog
                isOpen={confirmDialog.open}
                title="Delete Status Page"
                message="This public status page will be permanently removed. Your monitors will not be affected."
                confirmText="Delete Status Page"
                onConfirm={handleDeleteConfirm}
                onCancel={() => setConfirmDialog({ open: false, pageId: null })}
            />

            {/* Edit Modal */}
            {editPage && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-2xl shadow-2xl border border-gray-100 w-full max-w-lg max-h-[90vh] overflow-y-auto">
                        <div className="flex items-center justify-between p-6 border-b border-gray-100">
                            <h2 className="text-lg font-bold text-gray-900">Edit Status Page</h2>
                            <button
                                onClick={() => setEditPage(null)}
                                className="text-gray-400 hover:text-gray-700 transition-colors p-1 rounded-lg hover:bg-gray-100"
                            >✕</button>
                        </div>
                        <form onSubmit={handleEditSave} className="p-6 space-y-5">
                            {editError && (
                                <div className="p-3 bg-red-50 border border-red-200 text-red-800 text-xs rounded-lg flex items-center gap-2">
                                    <AlertCircle className="w-4 h-4 flex-shrink-0" />
                                    {editError}
                                </div>
                            )}

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                                <input
                                    type="text" required
                                    className="w-full border border-gray-200 rounded-lg p-2.5 focus:ring-blue-500 focus:border-blue-500 text-sm"
                                    value={editForm.title}
                                    onChange={handleEditTitleChange}
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
                                <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500">
                                    <span className="px-3 py-2.5 bg-gray-50 text-gray-400 text-sm border-r border-gray-200">/status/</span>
                                    <input
                                        type="text" required pattern="[a-z0-9-]+"
                                        className="flex-1 px-3 py-2.5 text-sm focus:outline-none"
                                        value={editForm.slug}
                                        onChange={e => {
                                            setEditSlugManuallyEdited(true);
                                            setEditForm(f => ({ ...f, slug: e.target.value.toLowerCase() }));
                                        }}
                                    />
                                </div>
                                <p className="text-xs text-gray-400 mt-1">Lowercase letters, numbers, hyphens only.</p>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Description (optional)</label>
                                <textarea
                                    rows={2}
                                    className="w-full border border-gray-200 rounded-lg p-2.5 text-sm focus:ring-blue-500 focus:border-blue-500"
                                    value={editForm.description}
                                    onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))}
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Monitored Services</label>
                                <div className="flex flex-wrap gap-2">
                                    {monitors.map(m => (
                                        <button
                                            key={m.id} type="button"
                                            onClick={() => toggleEditMonitor(m.id)}
                                            className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors ${
                                                editForm.monitorIds.includes(m.id)
                                                    ? 'bg-blue-600 text-white border-blue-600'
                                                    : 'bg-white text-gray-600 border-gray-200 hover:border-blue-400'
                                            }`}
                                        >{m.name}</button>
                                    ))}
                                </div>
                            </div>

                            <div className="flex gap-3 pt-2">
                                <button type="submit"
                                    className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg font-medium text-sm transition-colors">
                                    Save Changes
                                </button>
                                <button type="button" onClick={() => setEditPage(null)}
                                    className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-5 py-2 rounded-lg font-medium text-sm transition-colors">
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <div className="max-w-5xl mx-auto space-y-8">

                {/* Header */}
                <div className="flex justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
                            <Globe className="w-8 h-8 text-blue-600" />
                            Status Pages
                        </h1>
                        <p className="text-gray-500 mt-1">Public pages displaying real-time system health</p>
                    </div>
                    <button
                        onClick={() => { setShowForm(!showForm); setForm(EMPTY_FORM); setSlugManuallyEdited(false); }}
                        className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2 rounded-lg transition-colors"
                    >
                        <Plus className="w-4 h-4" />
                        New Status Page
                    </button>
                </div>

                {/* Create Form */}
                {showForm && (
                    <form onSubmit={handleCreate} className="bg-white p-6 rounded-xl shadow-sm border border-blue-100 space-y-5">
                        <h2 className="text-lg font-semibold text-gray-800">Create Status Page</h2>

                        {formError && (
                            <div className="p-3 bg-red-50 border border-red-200 text-red-800 text-xs rounded-lg flex items-center gap-2">
                                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                                {formError}
                            </div>
                        )}

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Title</label>
                                <input
                                    type="text" required
                                    placeholder="My Company Status"
                                    className="w-full border border-gray-200 rounded-lg p-2.5 focus:ring-blue-500 focus:border-blue-500 text-sm"
                                    value={form.title}
                                    onChange={handleTitleChange}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Slug <span className="text-gray-400 font-normal">(auto-generated)</span></label>
                                <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500">
                                    <span className="px-3 py-2.5 bg-gray-50 text-gray-400 text-sm border-r border-gray-200">/status/</span>
                                    <input
                                        type="text" required pattern="[a-z0-9-]+"
                                        className="flex-1 px-3 py-2.5 text-sm focus:outline-none"
                                        value={form.slug}
                                        onChange={handleSlugChange}
                                        placeholder="my-company"
                                    />
                                </div>
                                <p className="text-xs text-gray-400 mt-1">Lowercase letters, numbers, hyphens only.</p>
                            </div>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Description (optional)</label>
                            <textarea
                                rows={2}
                                placeholder="This page shows the real-time status of our services."
                                className="w-full border border-gray-200 rounded-lg p-2.5 focus:ring-blue-500 focus:border-blue-500 text-sm"
                                value={form.description}
                                onChange={e => setForm({ ...form, description: e.target.value })}
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Add Monitors as Services</label>
                            <div className="flex flex-wrap gap-2">
                                {monitors.map(m => (
                                    <button
                                        key={m.id} type="button"
                                        onClick={() => toggleMonitor(m.id)}
                                        className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-colors ${
                                            form.monitorIds.includes(m.id)
                                                ? 'bg-blue-600 text-white border-blue-600'
                                                : 'bg-white text-gray-600 border-gray-200 hover:border-blue-400'
                                        }`}
                                    >{m.name}</button>
                                ))}
                            </div>
                        </div>

                        <div className="flex gap-3 pt-2">
                            <button type="submit"
                                className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2 rounded-lg font-medium text-sm transition-colors">
                                Create
                            </button>
                            <button type="button" onClick={() => setShowForm(false)}
                                className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-5 py-2 rounded-lg font-medium text-sm transition-colors">
                                Cancel
                            </button>
                        </div>
                    </form>
                )}

                {/* Pages List */}
                {pages.length === 0 ? (
                    <div className="bg-white rounded-xl border border-gray-200 p-12 text-center space-y-3 shadow-sm">
                        <Globe className="w-12 h-12 text-blue-500/20 mx-auto" />
                        <h3 className="text-sm font-bold text-gray-700">No status pages created yet</h3>
                        <p className="text-xs text-gray-500 max-w-sm mx-auto">
                            Publish a public status page to communicate incidents to your users.
                        </p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {pages.map(page => {
                            const serviceCount = page.services?.length || 0;
                            const downCount = page.services?.filter(s => s.status === 'DOWN').length || 0;
                            return (
                                <div key={page.id} className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
                                    <div className="flex justify-between items-start gap-4">
                                        <div className="min-w-0 flex-1">
                                            <div className="flex items-center gap-3 flex-wrap">
                                                <h3 className="text-lg font-semibold text-gray-900">{page.title}</h3>
                                                {/* Health summary badge */}
                                                {serviceCount > 0 && (
                                                    <span className={`px-2 py-0.5 rounded-full text-xs font-semibold border ${
                                                        downCount === 0
                                                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                                            : 'bg-red-50 text-red-700 border-red-200'
                                                    }`}>
                                                        {downCount === 0
                                                            ? `${serviceCount} / ${serviceCount} operational`
                                                            : `${downCount} down · ${serviceCount - downCount} operational`}
                                                    </span>
                                                )}
                                            </div>
                                            {page.description && (
                                                <p className="text-gray-500 text-sm mt-1">{page.description}</p>
                                            )}
                                            <div className="flex items-center gap-3 mt-3 flex-wrap">
                                                <span className="text-xs font-mono bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                                                    /status/{page.slug}
                                                </span>
                                                <button
                                                    onClick={() => copyUrl(page.slug)}
                                                    className="text-xs text-gray-500 hover:text-gray-700 flex items-center gap-1 transition-colors"
                                                    title="Copy URL"
                                                >
                                                    {copiedSlug === page.slug
                                                        ? <><Check className="w-3 h-3 text-emerald-500" /> Copied</>
                                                        : <><Copy className="w-3 h-3" /> Copy URL</>}
                                                </button>
                                                <a
                                                    href={publicUrl(page.slug)}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="text-xs text-blue-600 hover:text-blue-800 flex items-center gap-1"
                                                >
                                                    <ExternalLink className="w-3 h-3" /> View public page
                                                </a>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-1 flex-shrink-0">
                                            <button
                                                onClick={() => openEditModal(page)}
                                                className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                                title="Edit status page"
                                            >
                                                <Edit2 className="w-4 h-4" />
                                            </button>
                                            <button
                                                onClick={() => handleDeleteRequest(page.id)}
                                                className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                title="Delete status page"
                                            >
                                                <Trash2 className="w-4 h-4" />
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </div>
    );
};

export default StatusPagesManager;
