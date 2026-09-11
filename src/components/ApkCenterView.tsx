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
git clone https://github.com/rclemmons508/RCOS.git
cd RCOS

# 2. Extract mobile build package
unzip RCOS-Mobile-Build-Ready.zip
cd rcos-mobile-fixed

# 3. Build APK with Gradle
./gradlew :app:assembleDebug

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
            <span>RCOS Mobile Android APK & CI/CD Center</span>
          </h1>
          <p className="text-xs text-slate-400">
            Export, download build artifacts, and inspect the automated GitHub Actions pipeline
          </p>
        </div>

        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-xs font-semibold text-emerald-400">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>CI/CD Workflow: Configured</span>
        </div>
      </div>

      {/* Main Download Card (Matching the original server.py design with enhanced elegance) */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 lg:p-8 space-y-6 shadow-xl max-w-3xl mx-auto">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-1">
            <span className="inline-block px-3 py-1 rounded-md bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold tracking-wider uppercase">
              Verified Mobile Build Package
            </span>
            <h2 className="text-2xl font-extrabold text-white">RCOS Master Mobile App</h2>
            <p className="text-xs text-slate-400 leading-relaxed max-w-lg">
              Enterprise Business Operating System with multi-agent orchestration, Firebase Cloud synchronization, VoIP telephony, and Gemini AI integration.
            </p>
          </div>

          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-700 p-0.5 shadow-lg shadow-emerald-500/20 ring-1 ring-emerald-400/40 shrink-0 overflow-hidden">
            <img 
              src="/rcos_app_icon_1786242465540.jpg" 
              alt="RCOS Icon" 
              className="w-full h-full object-cover"
              onError={(e) => { (e.target as HTMLElement).style.display = 'none'; }}
            />
          </div>
        </div>

        {/* Verification Meta Box */}
        <div className="bg-slate-950 border border-slate-800/90 rounded-xl p-4 font-mono text-xs space-y-2">
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Package:</span>
            <span className="text-emerald-400 font-bold">RCOS-Mobile-Build-Ready.zip</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Archive Size:</span>
            <span className="text-slate-200">2.38 MB (Compiles to ~32.27 MB APK)</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">Target Framework:</span>
            <span className="text-slate-200">Android 14 / Jetpack Compose / Gradle 9.3</span>
          </div>
          <div className="flex justify-between border-b border-slate-800/60 pb-2">
            <span className="text-slate-500">SHA-256 Checksum:</span>
            <span className="text-sky-400 text-[11px] break-all">02d26a8e967ce7aa3d2eb2fbb50e65f74d5efb96962368d6df226d9e8b64c13d</span>
          </div>
          <div className="flex justify-between pt-1">
            <span className="text-slate-500">GitHub Action:</span>
            <span className="text-emerald-400">.github/workflows/main.yml (Active)</span>
          </div>
        </div>

        {/* Download Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <a
            href="/RCOS-Mobile-Build-Ready.zip"
            download="RCOS-Mobile-Build-Ready.zip"
            className="flex-1 py-3 px-5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-lg shadow-emerald-900/40 text-center"
          >
            <Download className="w-4 h-4" />
            <span>Download RCOS Mobile Package (.zip)</span>
          </a>

          <a
            href="https://github.com/rclemmons508/RCOS/actions"
            target="_blank"
            rel="noopener noreferrer"
            className="py-3 px-5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold flex items-center justify-center gap-2 border border-slate-700 transition-all text-center"
          >
            <Github className="w-4 h-4 text-white" />
            <span>GitHub Actions Run</span>
            <ExternalLink className="w-3.5 h-3.5 text-slate-400" />
          </a>
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
            Your repository includes <code className="text-emerald-400 bg-slate-950 px-1 py-0.5 rounded">.github/workflows/main.yml</code> which automatically provisions JDK 17, sets up Gradle 9.3.1, extracts the project, builds the debug APK, and uploads the artifact.
          </p>
          <div className="p-3 rounded-lg bg-slate-950 border border-slate-800 text-[11px] font-mono text-slate-300 space-y-1">
            <div className="text-slate-500"># Trigger manually from GitHub:</div>
            <div>1. Go to repository <strong className="text-white">Actions</strong> tab</div>
            <div>2. Select <strong className="text-white">Build RCOS Mobile APK</strong></div>
            <div>3. Click <strong className="text-emerald-400">Run workflow</strong></div>
          </div>
        </div>

        {/* Local Build Instructions */}
        <div className="p-5 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Terminal className="w-4 h-4 text-sky-400" />
              <h3 className="text-sm font-bold text-white">Local Build with Android Studio</h3>
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
