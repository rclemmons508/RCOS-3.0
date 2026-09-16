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
import { Job, CallRecord } from '../types';

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

  // Save / Update User Profile in Firestore
  public async syncUserProfile(user: { uid: string; email: string | null; displayName: string | null; photoURL: string | null }): Promise<void> {
    try {
      const userRef = doc(db, 'users', user.uid);
      const data: UserProfileData = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL,
        lastLoginAt: new Date().toISOString()
      };
      await setDoc(userRef, data, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `users/${user.uid}`);
    }
  }

  public async saveUserProfile(user: { uid: string; email: string | null; displayName: string | null; photoURL: string | null }): Promise<void> {
    return this.syncUserProfile(user);
  }

  // Subscribe to user's persistent jobs in Firestore
  public subscribeToJobs(userId: string, onUpdate: (jobs: Job[]) => void): () => void {
    try {
      const jobsCol = collection(db, 'jobs');
      const q = query(jobsCol, where('ownerId', '==', userId));

      const unsubscribe = onSnapshot(
        q,
        (snapshot) => {
          const loadedJobs: Job[] = [];
          snapshot.forEach((docSnap) => {
            const data = docSnap.data();
            loadedJobs.push({
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
          onUpdate(loadedJobs);
        },
        (error) => {
          console.warn('Firestore Jobs subscription warning (falling back to memory):', error);
        }
      );

      return unsubscribe;
    } catch (err) {
      console.warn('Firestore subscribeToJobs error:', err);
      return () => {};
    }
  }

  // Persist a new or updated Job
  public async saveJob(arg1: string | Job, arg2?: Job): Promise<void> {
    const job: Job = typeof arg1 === 'string' ? (arg2 as Job) : arg1;
    const targetUserId = typeof arg1 === 'string' ? arg1 : auth.currentUser?.uid;
    if (!job || !targetUserId) return; // Keep local if not signed in

    try {
      const jobDoc = doc(db, 'jobs', job.id);
      await setDoc(jobDoc, {
        ...job,
        ownerId: targetUserId,
        updatedAt: new Date().toISOString()
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `jobs/${job.id}`);
    }
  }

  // Delete a job from Firestore
  public async removeJob(jobId: string): Promise<void> {
    const user = auth.currentUser;
    if (!user) return;

    try {
      const jobDoc = doc(db, 'jobs', jobId);
      await deleteDoc(jobDoc);
    } catch (err) {
      handleFirestoreError(err, OperationType.DELETE, `jobs/${jobId}`);
    }
  }

  // Subscribe to user's telephone calls in Firestore
  public subscribeToCalls(userId: string, onUpdate: (calls: CallRecord[]) => void): () => void {
    try {
      const callsCol = collection(db, 'calls');
      const q = query(callsCol, where('ownerId', '==', userId));

      const unsubscribe = onSnapshot(
        q,
        (snapshot) => {
          const loadedCalls: CallRecord[] = [];
          snapshot.forEach((docSnap) => {
            const data = docSnap.data();
            loadedCalls.push({
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
          onUpdate(loadedCalls);
        },
        (error) => {
          console.warn('Firestore Calls subscription warning:', error);
        }
      );

      return unsubscribe;
    } catch (err) {
      console.warn('Firestore subscribeToCalls error:', err);
      return () => {};
    }
  }

  // Persist a new or updated Call Record
  public async saveCall(arg1: string | CallRecord, arg2?: CallRecord): Promise<void> {
    const call: CallRecord = typeof arg1 === 'string' ? (arg2 as CallRecord) : arg1;
    const targetUserId = typeof arg1 === 'string' ? arg1 : auth.currentUser?.uid;
    if (!call || !targetUserId) return;

    try {
      const callDoc = doc(db, 'calls', call.id);
      await setDoc(callDoc, {
        ...call,
        ownerId: targetUserId,
        updatedAt: new Date().toISOString()
      }, { merge: true });
    } catch (err) {
      handleFirestoreError(err, OperationType.WRITE, `calls/${call.id}`);
    }
  }

  // Delete a call record from Firestore
  public async removeCall(callId: string): Promise<void> {
    const user = auth.currentUser;
    if (!user) return;

    try {
      const callDoc = doc(db, 'calls', callId);
      await deleteDoc(callDoc);
    } catch (err) {
      handleFirestoreError(err, OperationType.DELETE, `calls/${callId}`);
    }
  }
}

export const userDataService = UserDataService.getInstance();
