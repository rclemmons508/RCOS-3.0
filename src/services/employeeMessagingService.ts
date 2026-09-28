import { 
  Employee, 
  Department, 
  DirectMessage, 
  PaperworkDocument, 
  JobDispatchPayload, 
  Job, 
  JobPriority, 
  JobStatus,
  PaperworkStatus
} from '../types';
import { db, auth } from './firebaseAuth';
import { 
  collection, 
  doc, 
  setDoc, 
  getDocs, 
  query, 
  orderBy, 
  onSnapshot 
} from 'firebase/firestore';

export const ENTERPRISE_DEPARTMENTS: Department[] = [
  'Operations & Field Services',
  'Dispatch & Logistics',
  'Client Support & Accounts',
  'Technical & Engineering',
  'Legal & Compliance',
  'Executive Leadership'
];

export const INITIAL_EMPLOYEES: Employee[] = [
  {
    id: 'emp-01',
    name: 'Operations Director',
    email: 'rcsoulutions@gmail.com',
    role: 'Executive Director',
    department: 'Executive Leadership',
    status: 'online',
    avatarSeed: 'director',
    phone: '',
    isCurrentUser: true
  }
];

const INITIAL_MESSAGES: DirectMessage[] = [];

const LOCAL_STORAGE_MSGS_KEY = 'rcos_employee_messages_v2';
const LOCAL_STORAGE_EMPLOYEES_KEY = 'rcos_employees_roster_v2';
const LOCAL_STORAGE_CURRENT_USER_KEY = 'rcos_current_active_employee_v2';

class EmployeeMessagingService {
  private messages: DirectMessage[] = [];
  private employees: Employee[] = [];
  private currentEmployeeId: string = 'emp-01';
  private listeners: Array<() => void> = [];

  constructor() {
    this.loadInitialState();
  }

  private loadInitialState() {
    try {
      const savedMsgs = localStorage.getItem(LOCAL_STORAGE_MSGS_KEY);
      if (savedMsgs) {
        this.messages = JSON.parse(savedMsgs);
      } else {
        this.messages = INITIAL_MESSAGES;
        this.persistMessages();
      }

      const savedEmployees = localStorage.getItem(LOCAL_STORAGE_EMPLOYEES_KEY);
      if (savedEmployees) {
        this.employees = JSON.parse(savedEmployees);
      } else {
        this.employees = INITIAL_EMPLOYEES;
        this.persistEmployees();
      }

      const savedCurrentEmp = localStorage.getItem(LOCAL_STORAGE_CURRENT_USER_KEY);
      if (savedCurrentEmp) {
        this.currentEmployeeId = savedCurrentEmp;
      }
    } catch (err) {
      console.warn('Fallback to memory state for employee messages:', err);
      this.messages = INITIAL_MESSAGES;
      this.employees = INITIAL_EMPLOYEES;
    }
  }

  private persistMessages() {
    try {
      localStorage.setItem(LOCAL_STORAGE_MSGS_KEY, JSON.stringify(this.messages));
    } catch (err) {
      console.warn('Failed to persist messages to localStorage:', err);
    }
    this.notify();
  }

  private persistEmployees() {
    try {
      localStorage.setItem(LOCAL_STORAGE_EMPLOYEES_KEY, JSON.stringify(this.employees));
    } catch (err) {
      console.warn('Failed to persist employees to localStorage:', err);
    }
    this.notify();
  }

  public subscribe(listener: () => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notify() {
    this.listeners.forEach(l => l());
  }

  public getEmployees(): Employee[] {
    return this.employees;
  }

  public getCurrentEmployee(): Employee {
    const found = this.employees.find(e => e.id === this.currentEmployeeId);
    return found || this.employees[0] || INITIAL_EMPLOYEES[0];
  }

  public setCurrentEmployee(id: string) {
    this.currentEmployeeId = id;
    try {
      localStorage.setItem(LOCAL_STORAGE_CURRENT_USER_KEY, id);
    } catch (_) {}
    this.notify();
  }

  public addEmployee(newEmp: Omit<Employee, 'id'>): Employee {
    const created: Employee = {
      ...newEmp,
      id: `emp-${Date.now()}`
    };
    this.employees.push(created);
    this.persistEmployees();
    return created;
  }

  public getMessages(recipientFilterId: string, isDepartment: boolean): DirectMessage[] {
    const currentEmp = this.getCurrentEmployee();

    if (isDepartment) {
      // Department channel messages
      return this.messages.filter(m => m.recipientId === recipientFilterId);
    } else {
      // 1-on-1 direct messages between current user and recipient
      return this.messages.filter(m => 
        (m.senderId === currentEmp.id && m.recipientId === recipientFilterId) ||
        (m.senderId === recipientFilterId && m.recipientId === currentEmp.id)
      );
    }
  }

  public getAllMessages(): DirectMessage[] {
    return this.messages;
  }

  public getAllPaperwork(): PaperworkDocument[] {
    const list: PaperworkDocument[] = [];
    this.messages.forEach(m => {
      if (m.paperwork) {
        list.push(m.paperwork);
      }
    });
    return list;
  }

  public getPendingApprovalCount(): number {
    return this.getAllPaperwork().filter(p => p.status === 'Pending Review').length;
  }

  public getUnreadCount(): number {
    const currentEmp = this.getCurrentEmployee();
    return this.messages.filter(m => 
      m.senderId !== currentEmp.id && 
      !m.readBy?.includes(currentEmp.id)
    ).length;
  }

  public markAsRead(messageIds: string[]) {
    const currentEmp = this.getCurrentEmployee();
    let updated = false;
    this.messages = this.messages.map(m => {
      if (messageIds.includes(m.id)) {
        const readBy = m.readBy || [];
        if (!readBy.includes(currentEmp.id)) {
          updated = true;
          return { ...m, readBy: [...readBy, currentEmp.id] };
        }
      }
      return m;
    });
    if (updated) {
      this.persistMessages();
    }
  }

  public sendDirectMessage(params: {
    recipientId: string;
    recipientName: string;
    recipientType: 'direct' | 'department';
    content: string;
    paperwork?: PaperworkDocument;
    jobDispatch?: JobDispatchPayload;
  }): DirectMessage {
    const currentEmp = this.getCurrentEmployee();
    const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const newMsg: DirectMessage = {
      id: `msg-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      senderId: currentEmp.id,
      senderName: currentEmp.name,
      senderRole: currentEmp.role,
      senderDepartment: currentEmp.department,
      recipientId: params.recipientId,
      recipientType: params.recipientType,
      recipientName: params.recipientName,
      content: params.content,
      timestamp: timeStr,
      paperwork: params.paperwork,
      jobDispatch: params.jobDispatch,
      readBy: [currentEmp.id]
    };

    this.messages.push(newMsg);
    this.persistMessages();

    // Async sync to Firestore if user is authenticated
    this.syncMessageToFirestore(newMsg);

    return newMsg;
  }

  public updatePaperworkApproval(
    paperworkId: string, 
    status: PaperworkStatus,
    feedback?: string,
    signature?: string
  ): { updatedPaperwork: PaperworkDocument; shouldDispatchJob: boolean } | null {
    const currentEmp = this.getCurrentEmployee();
    let targetDoc: PaperworkDocument | null = null;
    let autoDispatch = false;

    this.messages = this.messages.map(m => {
      if (m.paperwork && m.paperwork.id === paperworkId) {
        const reviewed: PaperworkDocument = {
          ...m.paperwork,
          status,
          reviewerFeedback: feedback || m.paperwork.reviewerFeedback,
          reviewedBy: currentEmp.name,
          reviewedAt: new Date().toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }),
          signature: signature || `${currentEmp.name} (Authorized Lead)`
        };
        targetDoc = reviewed;
        if (status === 'Approved' && reviewed.autoDispatchJobOnApproval && !reviewed.dispatchedJobId) {
          autoDispatch = true;
        }
        return {
          ...m,
          paperwork: reviewed
        };
      }
      return m;
    });

    if (targetDoc) {
      this.persistMessages();
      return { updatedPaperwork: targetDoc, shouldDispatchJob: autoDispatch };
    }
    return null;
  }

  public linkDispatchedJobToPaperwork(paperworkId: string, jobId: string) {
    this.messages = this.messages.map(m => {
      if (m.paperwork && m.paperwork.id === paperworkId) {
        return {
          ...m,
          paperwork: {
            ...m.paperwork,
            dispatchedJobId: jobId
          }
        };
      }
      return m;
    });
    this.persistMessages();
  }

  private async syncMessageToFirestore(msg: DirectMessage) {
    try {
      if (auth.currentUser) {
        const msgRef = doc(collection(db, 'employee_messages'), msg.id);
        await setDoc(msgRef, {
          senderId: msg.senderId,
          senderName: msg.senderName,
          senderRole: msg.senderRole,
          senderDepartment: msg.senderDepartment,
          recipientId: msg.recipientId,
          recipientType: msg.recipientType,
          recipientName: msg.recipientName,
          content: msg.content,
          timestamp: msg.timestamp,
          ownerId: auth.currentUser.uid
        });
      }
    } catch (e) {
      // Non-fatal, local state handles everything cleanly
      console.warn('Firestore message sync note:', e);
    }
  }
}

export const employeeMessagingService = new EmployeeMessagingService();
