# Demo Data Setup

This guide loads realistic sample data into Firestore for customer demos.

## Overview

We'll create:
- Demo user account
- Sample company profile
- Pre-built workflows
- Agent registry
- Sample jobs with results

## Step 1: Create Demo User in Firebase

### Via Firebase Console
1. Go to https://console.firebase.google.com
2. Select RCOS-2.0 project
3. Go to Authentication → Users
4. Click "Add user"
5. Create demo account:
   - Email: `demo@rcos.demo`
   - Password: `Demo@12345`
6. Note the User ID (copy it)

## Step 2: Create Firestore Sample Data

Create `firestore_seed.js`:

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('./service-account-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

// IMPORTANT: Get the actual user ID from Firebase Console
const DEMO_USER_ID = 'demo-user-001'; // Replace with actual ID

async function seedData() {
  console.log('🌱 Seeding Firestore with demo data...');

  try {
    // 1. Create User Profile
    console.log('📝 Creating user profile...');
    await db.collection('users').doc(DEMO_USER_ID).set({
      email: 'demo@rcos.demo',
      displayName: 'Demo User',
      companyName: 'Acme Corp Demo',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      lastLogin: admin.firestore.FieldValue.serverTimestamp(),
      plan: 'demo',
      status: 'active'
    });

    // 2. Create Company Profile
    console.log('🏢 Creating company profile...');
    await db.collection('users').doc(DEMO_USER_ID)
      .collection('companyProfile').doc('main').set({
        name: 'Acme Corp Demo',
        industry: 'Technology',
        size: '50-100 employees',
        bottleneck: 'Customer support ticket volume',
        targetReduction: 60,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

    // 3. Create Agent Registry
    console.log('🤖 Creating agent registry...');
    const agents = [
      {
        id: 'email-handler',
        name: 'Email Handler',
        description: 'Processes and routes incoming emails',
        type: 'communication',
        capabilities: ['email_parsing', 'routing', 'categorization'],
        status: 'active'
      },
      {
        id: 'ticket-resolver',
        name: 'Ticket Resolver',
        description: 'Resolves support tickets automatically',
        type: 'support',
        capabilities: ['ticket_analysis', 'solution_generation', 'escalation'],
        status: 'active'
      },
      {
        id: 'knowledge-bot',
        name: 'Knowledge Bot',
        description: 'Searches knowledge base for answers',
        type: 'knowledge',
        capabilities: ['search', 'context_retrieval', 'answer_generation'],
        status: 'active'
      }
    ];

    for (const agent of agents) {
      await db.collection('users').doc(DEMO_USER_ID)
        .collection('agentRegistry').doc(agent.id).set(agent);
    }

    // 4. Create Workflows
    console.log('⚙️ Creating workflows...');
    const workflows = [
      {
        id: 'workflow-support',
        name: 'Customer Support Automation',
        description: 'Automates customer support ticket handling',
        industry: 'Technology',
        agents: ['email-handler', 'ticket-resolver', 'knowledge-bot'],
        expectedReduction: 60,
        status: 'active',
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      }
    ];

    for (const workflow of workflows) {
      await db.collection('users').doc(DEMO_USER_ID)
        .collection('workflows').doc(workflow.id).set(workflow);
    }

    // 5. Create Sample Jobs with Results
    console.log('✅ Creating sample jobs...');
    const jobs = [
      {
        id: 'job-001',
        workflowId: 'workflow-support',
        status: 'completed',
        createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000),
        completedAt: new Date(Date.now() - 23 * 60 * 60 * 1000),
        results: [
          {
            agentType: 'email-handler',
            ticketsProcessed: 47,
            categorized: 47,
            timeSpent: '12 minutes'
          },
          {
            agentType: 'ticket-resolver',
            resolved: 31,
            escalated: 16,
            avgSolveTime: '4 minutes'
          },
          {
            agentType: 'knowledge-bot',
            queriesRun: 28,
            solutionsFound: 28,
            accuracy: '94%'
          }
        ],
        stats: {
          ticketsProcessed: 47,
          ticketsResolved: 31,
          resolutionRate: 66,
          timeReducedPercent: 58,
          costSavings: '$234'
        }
      }
    ];

    for (const job of jobs) {
      const id = job.id;
      delete job.id;
      await db.collection('users').doc(DEMO_USER_ID)
        .collection('jobs').doc(id).set(job);
    }

    console.log('\n✨ Demo data seeded successfully!');
    console.log('📧 Demo credentials:');
    console.log(`   Email: demo@rcos.demo`);
    console.log(`   Password: Demo@12345`);

    process.exit(0);
  } catch (error) {
    console.error('❌ Error seeding data:', error);
    process.exit(1);
  }
}

seedData();
```

## Step 3: Download Service Account Key

1. Go to Firebase Console → Project Settings (gear icon)
2. Click "Service Accounts" tab
3. Click "Generate new private key"
4. Save as `service-account-key.json` in RCOS-2.0 directory
5. **Add to .gitignore**:
   ```bash
   echo "service-account-key.json" >> .gitignore
   ```

## Step 4: Run Data Seeding Script

```bash
# From RCOS-2.0 directory
node firestore_seed.js

# Output:
# 🌱 Seeding Firestore with demo data...
# 📝 Creating user profile...
# 🏢 Creating company profile...
# 🤖 Creating agent registry...
# ⚙️ Creating workflows...
# ✅ Creating sample jobs...
#
# ✨ Demo data seeded successfully!
# 📧 Demo credentials:
#    Email: demo@rcos.demo
#    Password: Demo@12345
```

## Step 5: Test with App

1. Build and run the app
2. Login with:
   - Email: `demo@rcos.demo`
   - Password: `Demo@12345`
3. You should see demo workflows and job history

---

## Summary

✅ Demo user created
✅ Workflows pre-built
✅ Sample jobs with results
✅ Ready for customer demo!
