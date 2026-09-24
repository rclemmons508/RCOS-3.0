import { 
  collection, 
  doc, 
  setDoc, 
  deleteDoc, 
  onSnapshot, 
  query, 
  where 
} from 'firebase/firestore';
import { db, auth, handleFirestoreError, OperationType } from './firebaseAuth';
import { Job, CallRecord, Agent, Client } from '../types';

export interface UserProfileData {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
  lastLoginAt: string;
  enterpriseName?: string;
  enterpriseDomain?: string;
}

export class UserDataService {
  private static instance: UserDataService;

  public static getInstance(): UserDataService {
    if (!UserDataService.instance) {
      UserDataService.instance = new UserDataService();
    }
    return UserDataService.instance;
  }

  // ---------- USER PROFILE ----------
  public async saveUserProfile(user: { 
    uid: string; 
    email: string | null; 
    displayName: string | null; 
    photoURL: string | null 
  }): Promise<void> {
    try {
      const userRef = doc(db, 'users', user.uid);
      await setDoc(userRef, {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL,
        lastLoginAt: new Date().toISOString()
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `users/${user.uid}`);
    }
  }

  // ---------- JOBS ----------
  public subscribeToJobs(userId: string, onUpdate: (jobs: Job[]) => void): () => void {
    try {
      const q = query(collection(db, 'jobs'), where('ownerId', '==', userId));
      return onSnapshot(q, (snapshot) => {
        const loaded: Job[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loaded.push({
            id: docSnap.id,
            title: data.title || 'Untitled Task',
            clientName: data.clientName || 'Internal',
            assignedAgent: data.assignedAgent || 'Aegis Core',
            priority: data.priority || 'Medium',
            status: data.status || 'Pending',
            dueDate: data.dueDate || 'Today',
            approvalRequired: Boolean(data.approvalRequired),
            isApproved: Boolean(data.isApproved),
            progress: typeof data.progress === 'number' ? data.progress : 0,
            summary: data.summary || '',
            budget: data.budget || '$0',
            tags: Array.isArray(data.tags) ? data.tags : []
          });
        });
        onUpdate(loaded);
      }, (error) => console.warn('Jobs subscription warning:', error));
    } catch {
      return () => {};
    }
  }

  public async saveJob(userId: string, job: Job): Promise<void> {
    if (!userId || !job) return;
    try {
      await setDoc(doc(db, 'jobs', job.id), { 
        ...job, 
        ownerId: userId, 
        updatedAt: new Date().toISOString() 
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `jobs/${job.id}`);
    }
  }

  public async removeJob(jobId: string): Promise<void> {
    if (!auth.currentUser) return;
    try {
      await deleteDoc(doc(db, 'jobs', jobId));
    } catch (err) {
      handleFirestoreError(err, OperationType.DELETE, `jobs/${jobId}`);
    }
  }

  // ---------- CALLS ----------
  public subscribeToCalls(userId: string, onUpdate: (calls: CallRecord[]) => void): () => void {
    try {
      const q = query(collection(db, 'calls'), where('ownerId', '==', userId));
      return onSnapshot(q, (snapshot) => {
        const loaded: CallRecord[] = [];
        snapshot.forEach((docSnap) => {
          const data = docSnap.data();
          loaded.push({
            id: docSnap.id,
            callerName: data.callerName || 'Unknown Caller',
            company: data.company || 'Direct Inbound',
            phoneNumber: data.phoneNumber || '+1 (555) 000-0000',
            timestamp: data.timestamp || 'Recent',
            duration: data.duration || '00:00',
            sentiment: data.sentiment || 'Neutral',
            summary: data.summary || '',
            agentRoutedTo: data.agentRoutedTo || 'AI Receptionist',
            status: data.status || 'Completed',
            answeredBy: data.answeredBy || 'ai',
            transcript: Array.isArray(data.transcript) ? data.transcript : [],
            actionItems: Array.isArray(data.actionItems) ? data.actionItems : [],
            audioDurationSec: typeof data.audioDurationSec === 'number' ? data.audioDurationSec : 0,
            ownerId: data.ownerId
          });
        });
        onUpdate(loaded);
      }, (error) => console.warn('Calls subscription warning:', error));
    } catch {
      return () => {};
    }
  }

  public async saveCall(userId: string, call: CallRecord): Promise<void> {
    if (!userId || !call) return;
    try {
      await setDoc(doc(db, 'calls', call.id), { 
        ...call, 
        ownerId: userId, 
        updatedAt: new Date().toISOString() 
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `calls/${call.id}`);
    }
  }

  public async removeCall(callId: string): Promise<void> {
    if (!auth.currentUser) return;
    try {
      await deleteDoc(doc(db, 'calls', callId));
    } catch (err) {
      handleFirestoreError(err, OperationType.DELETE, `calls/${callId}`);
    }
  }

  // ---------- AGENTS (NEW) ----------
  public subscribeToAgents(userId: string, onUpdate: (agents: Agent[]) => void): () => void {
    try {
      const q = query(collection(db, 'agents'), where('ownerId', '==', userId));
      return onSnapshot(q, (snapshot) => {
        const loaded: Agent[] = [];
        snapshot.forEach((docSnap) => {
          loaded.push({ id: docSnap.id, ...docSnap.data() } as Agent);
        });
        onUpdate(loaded);
      }, (error) => console.warn('Agents subscription warning:', error));
    } catch {
      return () => {};
    }
  }

  public async saveAgent(userId: string, agent: Agent): Promise<void> {
    if (!userId || !agent) return;
    try {
      await setDoc(doc(db, 'agents', agent.id), { 
        ...agent, 
        ownerId: userId, 
        updatedAt: new Date().toISOString() 
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `agents/${agent.id}`);
    }
  }

  // ---------- CLIENTS (NEW) ----------
  public subscribeToClients(userId: string, onUpdate: (clients: Client[]) => void): () => void {
    try {
      const q = query(collection(db, 'clients'), where('ownerId', '==', userId));
      return onSnapshot(q, (snapshot) => {
        const loaded: Client[] = [];
        snapshot.forEach((docSnap) => {
          loaded.push({ id: docSnap.id, ...docSnap.data() } as Client);
        });
        onUpdate(loaded);
      }, (error) => console.warn('Clients subscription warning:', error));
    } catch {
      return () => {};
    }
  }

  public async saveClient(userId: string, client: Client): Promise<void> {
    if (!userId || !client) return;
    try {
      await setDoc(doc(db, 'clients', client.id), { 
        ...client, 
        ownerId: userId, 
        updatedAt: new Date().toISOString() 
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `clients/${client.id}`);
    }
  }
}

export const userDataService = UserDataService.getInstance();
