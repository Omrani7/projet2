# 🔐 Admin Dashboard Access Instructions

## 📋 Overview
This document provides step-by-step instructions for accessing and using the comprehensive admin dashboard for your student housing platform.

## 🚀 Quick Start

### Step 1: Create Admin User
Run the SQL script to create an admin user:

```bash
# Navigate to your project directory
cd /c:/Users/MSI/Desktop/pfe

# Run the SQL script (use your preferred database client)
psql -h localhost -U your_username -d your_database -f create_admin_user.sql
```

**Admin Credentials:**
- **Email:** `admin@pfe.com`
- **Password:** `Admin123!`
- **Role:** `ADMIN`

### Step 2: Start Your Application

```bash
# Start the backend (Spring Boot)
cd spring-security
./mvnw spring-boot:run

# Start the frontend (Angular) - in a new terminal
cd frontend/ui
ng serve
```

### Step 3: Access Admin Dashboard

1. Open your browser and navigate to: `http://localhost:4200`
2. Click on **Login** button
3. Enter admin credentials:
   - Email: `admin@pfe.com`
   - Password: `Admin123!`
4. After successful login, navigate to: `http://localhost:4200/admin/dashboard`

## 🎛️ Dashboard Features

### 📊 Overview Statistics
- **Total Users:** Real-time count of all registered users
- **Active Properties:** Number of active property listings
- **Pending Inquiries:** Inquiries awaiting response
- **System Health:** Overall system status and uptime

### 📈 Analytics & Charts
- **User Distribution:** Students vs Owners vs Admins
- **User Growth:** Registration trends over time
- **System Resources:** Memory, CPU, and disk usage
- **Activity Summary:** Daily platform activity

### 👥 User Management
Click on "Manage Users" to:
- View all users with filtering and search
- Update user roles (STUDENT/OWNER/ADMIN)
- Enable/disable user accounts
- Delete users (with caution)
- View user details and activity

### 🏠 Property Management
Access property management to:
- View all property listings
- Filter by status (ACTIVE/PENDING/INACTIVE)
- Update property status
- Delete problematic listings
- Search properties by title/location

### 🔧 System Operations
Available system actions:
- **Clear Cache:** Refresh system cache
- **Database Backup:** Create database backup
- **View System Logs:** Monitor recent activity
- **Scraper Management:** Trigger property scrapers

### 🤖 Scraper Control
Manage automated property scrapers:
- **Immobilier Scraper:** Scrape Immobilier.tn listings
- **Tayara Scraper:** Scrape Tayara.tn listings
- Real-time status monitoring
- Manual trigger capabilities

## 🔐 Security Features

### Role-Based Access Control
- Only users with `ADMIN` role can access the dashboard
- Automatic redirection for non-admin users
- Session timeout protection
- Secure API endpoints with `@PreAuthorize("hasRole('ADMIN')")`

### Authentication
- JWT token-based authentication
- Automatic token validation
- Secure logout functionality

## 🌐 Multiple Account Access

### Opening Different Accounts Simultaneously

#### Method 1: Different Browsers
1. **Chrome:** Login as Admin (`admin@pfe.com`)
2. **Firefox:** Login as Student user
3. **Edge:** Login as Owner user

#### Method 2: Incognito/Private Windows
1. **Regular Window:** Login as Admin
2. **Incognito Window:** Login as different user
3. **Another Incognito:** Login as third user

#### Method 3: Browser Profiles
1. **Chrome Profile 1:** Admin account
2. **Chrome Profile 2:** Student account
3. **Chrome Profile 3:** Owner account

### Creating Test Users for Multi-Account Testing

```sql
-- Student User
INSERT INTO users (email, username, password, phone_number, auth_provider, enabled, role, created_at, updated_at) 
VALUES ('student@test.com', 'teststudent', '$2a$12$LjYKFXbGP5Nrw6/Eh9Qy8eC5ZGqhQFhI2Vk7x8y9z0A1B2C3D4E5F6', '+216 12 345 678', 'LOCAL', true, 'STUDENT', NOW(), NOW());

-- Owner User  
INSERT INTO users (email, username, password, phone_number, auth_provider, enabled, role, created_at, updated_at) 
VALUES ('owner@test.com', 'testowner', '$2a$12$LjYKFXbGP5Nrw6/Eh9Qy8eC5ZGqhQFhI2Vk7x8y9z0A1B2C3D4E5F6', '+216 87 654 321', 'LOCAL', true, 'OWNER', NOW(), NOW());
```

**Test Credentials:**
- **Student:** `student@test.com` / `Admin123!`
- **Owner:** `owner@test.com` / `Admin123!`

## 📱 Mobile Responsive Design
The admin dashboard is fully responsive and works on:
- Desktop computers (optimal experience)
- Tablets (landscape mode recommended)
- Mobile phones (portrait/landscape)

## 🛠️ Troubleshooting

### Common Issues

#### 1. "Access Denied" Error
**Solution:** Ensure user has ADMIN role
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@domain.com';
```

#### 2. Dashboard Not Loading
**Solutions:**
- Check if Angular dev server is running (`ng serve`)
- Verify Spring Boot backend is running
- Check browser console for errors
- Clear browser cache and cookies

#### 3. Statistics Not Showing
**Solutions:**
- Check database connectivity
- Verify admin endpoints are accessible
- Check browser network tab for API errors

#### 4. WebSocket Connection Issues
**Solutions:**
- Restart the Spring Boot application
- Check if port 8080 is available
- Verify WebSocket configuration

### Performance Tips
- The dashboard auto-refreshes every 30 seconds
- Use Chrome/Firefox for best performance
- Close unused browser tabs to save memory
- Use desktop for managing large datasets

## 🔄 Real-time Features
- **Live Statistics:** Updates every 30 seconds
- **System Health:** Real-time monitoring
- **User Activity:** Live user session tracking
- **Instant Notifications:** System alerts and warnings

## 📝 Admin Tasks Checklist

### Daily Tasks
- [ ] Check system health status
- [ ] Review new user registrations
- [ ] Monitor pending inquiries
- [ ] Check for system alerts

### Weekly Tasks  
- [ ] Review user activity reports
- [ ] Clean up inactive users
- [ ] Monitor property listing quality
- [ ] Run database backup

### Monthly Tasks
- [ ] Analyze user growth trends
- [ ] Review system performance metrics
- [ ] Update system configurations
- [ ] Generate comprehensive reports

## 🆘 Support & Contact
For technical issues or questions:
- Check system logs in the dashboard
- Review browser console errors
- Contact system administrator
- Document error details for debugging

## 🔒 Security Best Practices
1. **Change Default Password:** Update admin password immediately
2. **Regular Backups:** Schedule automated database backups
3. **Monitor Access:** Review admin access logs regularly
4. **Secure Environment:** Use HTTPS in production
5. **User Validation:** Verify user accounts before activation

---

**🎉 Congratulations!** You now have a fully functional admin dashboard with comprehensive system management capabilities. The dashboard provides real-time insights, user management, and system control features to effectively manage your student housing platform. 