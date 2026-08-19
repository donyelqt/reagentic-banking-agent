import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const mockTrendData = [
  { month: 'Mar', spending: 12000, budget: 15000 },
  { month: 'Apr', spending: 18000, budget: 15000 },
  { month: 'May', spending: 14000, budget: 15000 },
  { month: 'Jun', spending: 16500, budget: 15000 },
];

export const SpendingTrendChart = () => {
  return (
    <div className="h-80 w-full bg-white p-4 rounded-xl shadow-sm border border-gray-100">
      <h3 className="text-lg font-semibold mb-4 text-gray-800">Monthly Trend vs Budget</h3>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={mockTrendData}
          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
        >
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
          <XAxis dataKey="month" axisLine={false} tickLine={false} />
          <YAxis axisLine={false} tickLine={false} tickFormatter={(value) => `₱${value}`} />
          <Tooltip formatter={(value) => `₱${value}`} cursor={{fill: '#f3f4f6'}} />
          <Legend />
          <Bar dataKey="spending" fill="#3b82f6" name="Actual Spending" radius={[4, 4, 0, 0]} />
          <Bar dataKey="budget" fill="#64a3b8" name="Budget Limit" radius={[4, 4, 0, 0]} />        
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};