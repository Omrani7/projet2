import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminService, AdminUser } from '../../services/admin.service';
import { Subscription } from 'rxjs';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent implements OnInit, OnDestroy {
  
  users: AdminUser[] = [];
  filteredUsers: AdminUser[] = [];
  isLoading = true;
  
  // Filters and search
  searchTerm = '';
  selectedRole = '';
  selectedStatus = '';
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  
  // Edit modal
  showEditModal = false;
  editingUser: AdminUser | null = null;
  newRole = '';
  newStatus = true;
  
  // Make Math available in template
  Math = Math;
  
  private subscriptions: Subscription[] = [];
  
  constructor(
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadUsers();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  loadUsers() {
    this.isLoading = true;
    
    this.subscriptions.push(
      this.adminService.getAllUsers(
        this.currentPage, 
        this.pageSize, 
        'id', 
        'desc',
        this.searchTerm || undefined,
        this.selectedRole || undefined
      ).subscribe({
        next: (response) => {
          this.users = response.content;
          this.filteredUsers = response.content;
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading users:', error);
          this.isLoading = false;
        }
      })
    );
  }

  onSearch() {
    this.currentPage = 0;
    this.loadUsers();
  }

  onRoleFilter() {
    this.currentPage = 0;
    this.loadUsers();
  }

  onStatusFilter() {
    if (this.selectedStatus === '') {
      this.filteredUsers = this.users;
    } else {
      const isEnabled = this.selectedStatus === 'active';
      this.filteredUsers = this.users.filter(user => user.enabled === isEnabled);
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadUsers();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadUsers();
    }
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadUsers();
  }

  editUser(user: AdminUser) {
    this.editingUser = { ...user };
    this.newRole = user.role;
    this.newStatus = user.enabled;
    this.showEditModal = true;
  }

  saveUserChanges() {
    if (!this.editingUser) return;

    const updateOperations: Observable<any>[] = [];

    // Update role if changed
    if (this.newRole !== this.editingUser.role) {
      updateOperations.push(
        this.adminService.updateUserRole(this.editingUser.id, this.newRole)
      );
    }

    // Update status if changed
    if (this.newStatus !== this.editingUser.enabled) {
      updateOperations.push(
        this.adminService.updateUserStatus(this.editingUser.id, this.newStatus)
      );
    }

    if (updateOperations.length > 0) {
      // Execute all updates
      let completedOperations = 0;
      const totalOperations = updateOperations.length;
      
      updateOperations.forEach(operation => {
        this.subscriptions.push(
          operation.subscribe({
            next: () => {
              completedOperations++;
              if (completedOperations === totalOperations) {
                this.showEditModal = false;
                this.editingUser = null;
                this.loadUsers();
                alert('User updated successfully!');
              }
            },
            error: (error) => {
              console.error('Error updating user:', error);
              alert('Error updating user: ' + error.message);
            }
          })
        );
      });
    } else {
      this.showEditModal = false;
      this.editingUser = null;
    }
  }

  deleteUser(user: AdminUser) {
    if (confirm(`Are you sure you want to delete user "${user.username}"? This action cannot be undone.`)) {
      this.subscriptions.push(
        this.adminService.deleteUser(user.id).subscribe({
          next: () => {
            this.loadUsers();
            alert('User deleted successfully!');
          },
          error: (error) => {
            console.error('Error deleting user:', error);
            alert('Error deleting user: ' + error.message);
          }
        })
      );
    }
  }

  closeModal() {
    this.showEditModal = false;
    this.editingUser = null;
  }

  goBack() {
    this.router.navigate(['/admin/dashboard']);
  }

  getRoleColor(role: string): string {
    switch (role) {
      case 'ADMIN': return 'bg-red-100 text-red-800';
      case 'OWNER': return 'bg-green-100 text-green-800';
      case 'STUDENT': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusColor(enabled: boolean): string {
    return enabled ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString();
  }
} 