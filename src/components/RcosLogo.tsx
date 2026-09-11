import React from 'react';

interface RcosLogoProps {
  className?: string;
  size?: number;
}

export const RcosLogo: React.FC<RcosLogoProps> = ({ className = '', size = 36 }) => {
  return (
    <div 
      className={`relative rounded-xl overflow-hidden shadow-lg flex-shrink-0 flex items-center justify-center ${className}`}
      style={{ 
        width: size, 
        height: size,
        background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
        boxShadow: '0 4px 12px rgba(0,0,0,0.5), inset 0 1px 1px rgba(255,255,255,0.2)'
      }}
    >
      {/* Glossy swirl SVG that matches the colorful emblem in the screenshots */}
      <svg 
        viewBox="0 0 100 100" 
        className="w-full h-full p-1"
        fill="none" 
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="swirlGrad1" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#38bdf8" />
            <stop offset="50%" stopColor="#22c55e" />
            <stop offset="100%" stopColor="#eab308" />
          </linearGradient>
          <linearGradient id="swirlGrad2" x1="100%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#ec4899" />
            <stop offset="50%" stopColor="#f97316" />
            <stop offset="100%" stopColor="#84cc16" />
          </linearGradient>
        </defs>

        {/* Outer glossy rounded boundary */}
        <rect x="4" y="4" width="92" height="92" rx="24" fill="#0f172a" stroke="rgba(255,255,255,0.15)" strokeWidth="2" />
        
        {/* Dynamic S-curve dual swirl ribbons */}
        <path 
          d="M20 35 C 20 20, 50 15, 65 30 C 80 45, 60 70, 45 75 C 30 80, 20 65, 20 50 Z" 
          fill="url(#swirlGrad1)" 
          opacity="0.95"
        />
        <path 
          d="M80 65 C 80 80, 50 85, 35 70 C 20 55, 40 30, 55 25 C 70 20, 80 35, 80 50 Z" 
          fill="url(#swirlGrad2)" 
          opacity="0.9"
        />
        <circle cx="50" cy="50" r="14" fill="#0b1318" />
        <circle cx="50" cy="50" r="8" fill="#76d418" />
      </svg>
    </div>
  );
};
