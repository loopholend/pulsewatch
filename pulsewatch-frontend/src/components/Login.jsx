import React, { useState, useEffect } from 'react';
import { api } from '../api/axiosConfig';
import { Lock, Mail, Activity, AlertCircle, ArrowRight, CheckCircle2 } from 'lucide-react';

// Inline GitHub mark — lucide-react installed version doesn't export Github
const GithubIcon = () => (
    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
        <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
    </svg>
);
import { useNavigate, useSearchParams } from 'react-router-dom';

const Login = ({ onLoginSuccess }) => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const [githubLoading, setGithubLoading] = useState(false);
    const [isRegistering, setIsRegistering] = useState(false);

    // Handle GitHub OAuth callback — code param comes back in the URL
    useEffect(() => {
        const code = searchParams.get('code');
        if (code) {
            handleGitHubCallback(code);
        }
    }, [searchParams]);

    const handleGitHubCallback = async (code) => {
        setGithubLoading(true);
        setError('');
        try {
            const res = await api.post('/auth/github/callback', { code });
            const token = res.data.token || res.data.accessToken;
            if (token) {
                localStorage.setItem('userEmail', res.data.email || '');
                onLoginSuccess(token);
                navigate('/');
            } else {
                setError('GitHub login failed — no token received.');
            }
        } catch (err) {
            setError(err.response?.data?.error || 'GitHub OAuth failed. Please try again.');
        } finally {
            setGithubLoading(false);
        }
    };

    const handleGitHubLogin = async () => {
        setGithubLoading(true);
        setError('');
        try {
            const res = await api.get('/auth/github/url');
            window.location.href = res.data.url;
        } catch (err) {
            setError('GitHub OAuth is not configured on the server.');
            setGithubLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);
        try {
            if (isRegistering) {
                await api.post('/auth/register', { email, password, role: 'USER' });
                setIsRegistering(false);
                setSuccess('Account created! You can now sign in.');
                setPassword('');
            } else {
                const res = await api.post('/auth/login', { email, password });
                const token = res.data.token || res.data.accessToken;
                if (token) {
                    localStorage.setItem('userEmail', email);
                    onLoginSuccess(token);
                    navigate('/');
                } else {
                    setError('Invalid token received from server.');
                }
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Authentication failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleMode = () => {
        setIsRegistering(!isRegistering);
        setError('');
        setSuccess('');
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 text-white flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-md text-center space-y-4">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold uppercase tracking-wider mx-auto">
                    <Activity className="w-4 h-4 animate-pulse" />
                    PulseWatch Observability
                </div>
                <h2 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
                    {isRegistering ? 'Create your developer account' : 'Sign in to PulseWatch'}
                </h2>
                <p className="text-sm text-slate-400">
                    {isRegistering
                        ? 'Get started with real-time API uptime & telemetry monitoring.'
                        : 'Access your monitoring dashboards and alert channels.'}
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white/5 backdrop-blur-md py-8 px-4 shadow-xl sm:rounded-2xl sm:px-10 border border-white/10">

                    {/* GitHub OAuth Button */}
                    {!isRegistering && (
                        <>
                            <button
                                onClick={handleGitHubLogin}
                                disabled={githubLoading}
                                className="w-full flex justify-center items-center gap-2.5 py-2.5 px-4 border border-white/20 rounded-lg shadow-sm text-sm font-semibold text-white bg-slate-800 hover:bg-slate-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-500 disabled:opacity-50 transition-all"
                            >
                                {githubLoading ? (
                                    <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                                    </svg>
                                ) : (
                                    <GithubIcon />
                                )}
                                {githubLoading ? 'Connecting to GitHub...' : 'Continue with GitHub'}
                            </button>

                            <div className="my-5 flex items-center gap-3">
                                <div className="flex-1 border-t border-white/10"></div>
                                <span className="text-xs text-slate-500 uppercase tracking-widest">or sign in with email</span>
                                <div className="flex-1 border-t border-white/10"></div>
                            </div>
                        </>
                    )}

                    <form className="space-y-5" onSubmit={handleSubmit}>
                        {/* Error banner */}
                        {error && (
                            <div className="p-3 text-xs rounded-lg flex items-center gap-2 border bg-red-500/10 text-red-400 border-red-500/20">
                                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                                {error}
                            </div>
                        )}

                        {/* Success banner */}
                        {success && (
                            <div className="p-3 text-xs rounded-lg flex items-center gap-2 border bg-emerald-500/10 text-emerald-400 border-emerald-500/20">
                                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                                {success}
                            </div>
                        )}

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                                Email Address
                            </label>
                            <div className="relative rounded-lg shadow-sm">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail className="h-4 w-4 text-slate-500" />
                                </div>
                                <input
                                    type="email"
                                    required
                                    value={email}
                                    onChange={e => setEmail(e.target.value)}
                                    className="block w-full pl-10 pr-3 py-2.5 text-sm bg-slate-950/40 border border-white/15 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                                    placeholder="developer@company.com"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                                Password
                            </label>
                            <div className="relative rounded-lg shadow-sm">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="h-4 w-4 text-slate-500" />
                                </div>
                                <input
                                    type="password"
                                    required
                                    value={password}
                                    onChange={e => setPassword(e.target.value)}
                                    className="block w-full pl-10 pr-3 py-2.5 text-sm bg-slate-950/40 border border-white/15 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                                    placeholder="••••••••"
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-transparent rounded-lg shadow-lg text-sm font-semibold text-white bg-blue-600 hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-all"
                        >
                            {loading ? 'Processing...' : isRegistering ? 'Create Account' : 'Sign In'}
                            {!loading && <ArrowRight className="w-4 h-4" />}
                        </button>
                    </form>

                    <div className="mt-6 text-center border-t border-white/10 pt-5">
                        <button
                            onClick={handleToggleMode}
                            className="text-sm text-blue-400 hover:text-blue-300 font-medium transition-all"
                        >
                            {isRegistering
                                ? 'Already have an account? Sign in'
                                : "Don't have an account? Register for free"}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;
