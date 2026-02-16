import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Sidebar } from './shared/sidebar/sidebar';
import { Navbar } from './shared/navbar/navbar';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Sidebar, Navbar, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('Web');
  isSidebarOpen = false;
  showLayout = true;

  constructor(private router: Router) {}

  ngOnInit() {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      const hiddenRoutes = [
        '/login', 
        '/register', 
        '/forgot-password', 
        '/reset-password-sent', 
        '/reset-password',
        '/reset-password-success',
        '/register-verification-sent',
        '/activate',
        '/home',
        '/review'
      ];
      const url = event.urlAfterRedirects || event.url;
      // Hide layout for home page (root) and other auth routes
      this.showLayout = url !== '/' && !hiddenRoutes.some(route => url.startsWith(route));
    });
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  closeSidebar() {
    this.isSidebarOpen = false;
  }
}