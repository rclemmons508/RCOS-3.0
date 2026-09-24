// src/firebaseConfig.ts
// Safe Firebase configuration - values come from environment variables only.
// Never put real API keys directly in this file.

export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || '',
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID || '',
  // Optional: only needed if you use a named Firestore database
  firestoreDatabaseId: import.meta.env.VITE_FIRESTORE_DATABASE_ID || undefined,
};
