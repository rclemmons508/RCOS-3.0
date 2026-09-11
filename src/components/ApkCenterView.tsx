import React, { useState } from 'react';
import { 
  Smartphone, 
  Download, 
  CheckCircle2, 
  Github, 
  Terminal, 
  Copy, 
  ShieldCheck, 
  AlertCircle,
  FolderArchive,
  ExternalLink
} from 'lucide-react';

export const ApkCenterView: React.FC = () => {
  const [copiedCmd, setCopiedCmd] = useState(false);

  const localBuildSnippet = `# 1. Clone repository
git clone https://github.com/rclemmons508/RCOS-3.0.git
cd RCOS-3.0/android/rcos-mobile-fixed

# 2. Ensure Gradle wrapper has execute permission
chmod +x gradlew

# 3. Build APK directly from current RCOS 3.0 source
./gradlew clean :app:assembleDebug

# 4. Built APK output location:
# app/build/outputs/apk/debug/app-debug.apk`;

  const handleCopyCmd = () => {
    navigator.clipboard.writeText(localBuildSnippet);
    setCopiedCmd(true);
    setTimeout(() => setCopiedCmd(false), 2000);
  };

  return (
    <div id="apk-center-container" className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
            <Smartphone className="w-5 h-5 text-emerald-400" />
            <span>RCOS 3.0 Mobile Android APK & CI/CD Center</span>
          </h1>
          <p className="text-xs text-slate-400">
            Automated GitHub Actions CI/CD pipeline and source-based Gradle build
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>CI/CD Workflow: .github/workflows/build-apk.yml</span>
        </div>
      </div>

      {/* Main Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:p-8 space-y-6 shadow-xl max-w-3xl mx-auto">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <span className="inline-block px-3 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold tracking-wider uppercase">
              Current RCOS 3.0 Source Build
            </span>
            <h2 className="text-2xl font-extrabold text-white">RCOS 3.0 Enterprise Mobile App</h2>
            <p className="text-xs text-slate-400 leading-relaxed max-w-lg">
              Enterprise Business Operating System with autonomous multi-agent orchestration, Firebase Cloud synchronization, VoIP telephony, and Gemini AI integration. Built directly from source in <code className="text-emerald-400">android/rcos-mobile-fixed</code>.
            </p>
          </div>

          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-700 p-0.5 shadow-lg shadow-emerald-500/20 ring-1 ring-emerald-400/40 shrink-0 overflow-hidden">
            <img 
              src="/rcos_app_icon_1786242465540.jpg" 
              alt="RCOS 3.0 Icon" 
              className="w-full h-full object-cover"
              onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
            />
          </div>
        </div>

        {/* Verification Meta Box */}
        <div className="bg-slate-950 border border-slate-800/90 rounded-xl p-4 font-mono text-xs space-y-2">
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Source Location:</span>
            <span className="text-emerald-400 font-bold">android/rcos-mobile-fixed/app</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Application ID:</span>
            <span className="text-slate-200">com.rcsolutions.rcos.app</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Build Target:</span>
            <span className="text-slate-200">Android 14 (API 34) / Jetpack Compose / Gradle</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Output APK:</span>
            <span className="text-sky-400 text-[11px] break-all">android/rcos-mobile-fixed/app/build/outputs/apk/debug/app-debug.apk</span>
          </div>
          <div className="flex justify-between pt-1">
            <span className="text-slate-500">GitHub Action:</span>
            <span className="text-emerald-400">.github/workflows/build-apk.yml (Direct Source)</span>
          </div>
        </div>

        {/* Action Links */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <a
            href="https://github.com/rclemmons508/RCOS-3.0/actions"
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 py-3 px-5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-lg shadow-emerald-900/40 text-center"
          >
            <Github className="w-4 h-4 text-white" />
            <span>Open GitHub Actions (Build APK)</span>
            <ExternalLink className="w-3.5 h-3.5 text-emerald-200" />
          </a>

          <button
            onClick={handleCopyCmd}
            className="py-3 px-5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold flex items-center justify-center gap-2 border border-slate-700 transition-all text-center cursor-pointer"
          >
            <Copy className="w-3.5 h-3.5 text-sky-400" />
            <span>{copiedCmd ? 'Commands Copied!' : 'Copy Gradle Commands'}</span>
          </button>
        </div>
      </div>

      {/* CI/CD & Build Instructions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 max-w-4xl mx-auto">
        {/* GitHub Actions Pipeline */}
        <div className="p-5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
          <div className="flex items-center gap-2">
            <Github className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-bold text-white">Automated GitHub Actions Workflow</h3>
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            Your repository workflow <code className="text-emerald-400 bg-slate-950 px-1 py-0.5 rounded">.github/workflows/build-apk.yml</code> builds directly from repository source in <code className="text-slate-300">android/rcos-mobile-fixed</code>. No ZIP files or cached older copies are used.
          </p>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 space-y-1">
            <div className="text-slate-500"># Trigger from GitHub:</div>
            <div>1. Go to repository <strong className="text-white">Actions</strong> tab</div>
            <div>2. Select <strong className="text-white">Build RCOS 3.0 Mobile APK</strong></div>
            <div>3. Click <strong className="text-emerald-400">Run workflow</strong></div>
            <div>4. Download the generated <strong className="text-white">RCOS-3.0-Debug-APK</strong></div>
          </div>
        </div>

        {/* Local Build Instructions */}
        <div className="p-5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Terminal className="w-4 h-4 text-sky-400" />
              <h3 className="text-sm font-bold text-white">Direct Source Build</h3>
            </div>
            <button
              onClick={handleCopyCmd}
              className="text-xs text-sky-400 hover:text-sky-300 flex items-center gap-1 cursor-pointer"
            >
              <Copy className="w-3.5 h-3.5" />
              <span>{copiedCmd ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <pre className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto leading-relaxed">
            {localBuildSnippet}
          </pre>
        </div>
      </div>
    </div>
  );
};
