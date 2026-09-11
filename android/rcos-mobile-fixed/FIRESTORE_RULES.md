# Firestore Security Rules

## Development Rules (Test Mode)

For initial setup and testing:

```javascript
rules_version = '3';

service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true; // Development only!
    }
  }
}
```

⚠️ **Never use in production!**

## Production Rules

Use these rules for customer-facing deployments:

```javascript
rules_version = '3';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // User Documents
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      
      // All user subcollections inherit permissions
      match /{subcollection}/{document=**} {
        allow read, write: if request.auth.uid == userId;
      }
    }
  }
}
```

## Applying Rules to Firebase

### Via Firebase Console
1. Go to https://console.firebase.google.com
2. Select RCOS-2.0 project
3. Go to Firestore Database → Rules
4. Paste production rules
5. Click Publish

### Via Firebase CLI
```bash
# Save rules to firestore.rules file
cat > firestore.rules << 'EOF'
rules_version = '3';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
      match /{subcollection}/{document=**} {
        allow read, write: if request.auth.uid == userId;
      }
    }
  }
}
EOF

# Deploy rules
firebase deploy --only firestore:rules
```

---

## Summary

✅ Development rules for testing
✅ Production rules for security
✅ Deployment instructions included
