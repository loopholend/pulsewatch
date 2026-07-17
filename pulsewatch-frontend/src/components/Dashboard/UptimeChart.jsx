import React from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell
} from 'recharts';

const formatTime = (timeStr) => {
    const date = new Date(timeStr);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
        const isSuccess = payload[0].payload.success;
        return (
            <div className="bg-gray-900 text-white p-3 rounded-lg shadow-xl text-sm border-l-4 border-l-transparent" style={{ borderLeftColor: isSuccess ? '#10b981' : '#ef4444' }}>
                <p className="font-semibold mb-1">{formatTime(label)}</p>
                <p className="flex items-center space-x-2">
                    <span className={`w-2 h-2 rounded-full ${isSuccess ? 'bg-emerald-500' : 'bg-red-500'}`}></span>
                    <span>Status: <span className="font-semibold">{isSuccess ? 'UP' : 'DOWN'}</span></span>
                </p>
            </div>
        );
    }
    return null;
};

const UptimeChart = ({ data }) => {
    // Map success to 1 for the bar height, but we use Cell to color it
    const chartData = data.map(d => ({
        ...d,
        barHeight: 1
    }));

    return (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
            <h3 className="text-lg font-semibold text-gray-800 mb-6">Uptime (Last 24 Hours)</h3>
            <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }} barCategoryGap="10%">
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                        <XAxis 
                            dataKey="checkedAt" 
                            tickFormatter={formatTime} 
                            stroke="#94a3b8" 
                            fontSize={12} 
                            tickLine={false} 
                            axisLine={false} 
                            minTickGap={30}
                        />
                        <YAxis 
                            hide={true}
                            domain={[0, 1]}
                        />
                        <Tooltip content={<CustomTooltip />} cursor={{fill: '#f8fafc'}} />
                        <Bar dataKey="barHeight" radius={[4, 4, 0, 0]}>
                            {
                                chartData.map((entry, index) => (
                                    <Cell key={`cell-${index}`} fill={entry.success ? '#10b981' : '#ef4444'} />
                                ))
                            }
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default UptimeChart;
