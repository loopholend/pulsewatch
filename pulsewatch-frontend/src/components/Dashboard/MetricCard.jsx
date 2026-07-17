import React from 'react';

const MetricCard = ({ title, value, unit, icon: Icon, colorClass }) => {
    return (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex items-center space-x-4">
            <div className={`p-4 rounded-full ${colorClass} bg-opacity-10`}>
                <Icon className={`w-6 h-6 ${colorClass.replace('bg-', 'text-')}`} />
            </div>
            <div>
                <p className="text-sm font-medium text-gray-500">{title}</p>
                <div className="flex items-baseline space-x-1">
                    <h3 className="text-2xl font-bold text-gray-900">{value}</h3>
                    {unit && <span className="text-sm font-medium text-gray-500">{unit}</span>}
                </div>
            </div>
        </div>
    );
};

export default MetricCard;
