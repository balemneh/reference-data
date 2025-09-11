import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ThemeService } from '../../services/theme.service';
import { ToastService } from '../../services/toast.service';

interface UserPreferences {
  notifications: {
    email: boolean;
    browser: boolean;
    changeRequests: boolean;
    systemAlerts: boolean;
  };
  display: {
    language: string;
    timezone: string;
    dateFormat: string;
    itemsPerPage: number;
  };
  accessibility: {
    highContrast: boolean;
    fontSize: 'small' | 'medium' | 'large';
    reduceMotion: boolean;
  };
}

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.html',
  styleUrl: './settings.scss'
})
export class SettingsComponent implements OnInit {
  preferences: UserPreferences = {
    notifications: {
      email: true,
      browser: true,
      changeRequests: true,
      systemAlerts: false
    },
    display: {
      language: 'en-US',
      timezone: 'America/New_York',
      dateFormat: 'MM/dd/yyyy',
      itemsPerPage: 25
    },
    accessibility: {
      highContrast: false,
      fontSize: 'medium',
      reduceMotion: false
    }
  };

  loading = false;
  currentTab = 'general';

  languageOptions = [
    { value: 'en-US', label: 'English (US)' },
    { value: 'es-ES', label: 'Spanish' },
    { value: 'fr-FR', label: 'French' }
  ];

  timezoneOptions = [
    { value: 'America/New_York', label: 'Eastern Time' },
    { value: 'America/Chicago', label: 'Central Time' },
    { value: 'America/Denver', label: 'Mountain Time' },
    { value: 'America/Los_Angeles', label: 'Pacific Time' },
    { value: 'UTC', label: 'UTC' }
  ];

  dateFormatOptions = [
    { value: 'MM/dd/yyyy', label: 'MM/DD/YYYY' },
    { value: 'dd/MM/yyyy', label: 'DD/MM/YYYY' },
    { value: 'yyyy-MM-dd', label: 'YYYY-MM-DD' }
  ];

  itemsPerPageOptions = [10, 25, 50, 100];

  constructor(
    private themeService: ThemeService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadUserPreferences();
  }

  loadUserPreferences() {
    // Load preferences from localStorage or API
    const saved = localStorage.getItem('cbp-user-preferences');
    if (saved) {
      try {
        this.preferences = { ...this.preferences, ...JSON.parse(saved) };
      } catch (error) {
        console.error('Error loading user preferences:', error);
      }
    }
  }

  savePreferences() {
    this.loading = true;
    
    // Save to localStorage (in real app, would save to API)
    localStorage.setItem('cbp-user-preferences', JSON.stringify(this.preferences));
    
    // Apply preferences
    this.applyPreferences();
    
    setTimeout(() => {
      this.loading = false;
      this.toastService.showSuccess('Settings saved successfully');
    }, 1000);
  }

  resetPreferences() {
    if (confirm('Are you sure you want to reset all preferences to default values?')) {
      localStorage.removeItem('cbp-user-preferences');
      this.ngOnInit(); // Reload default preferences
      this.toastService.showInfo('Preferences reset to default values');
    }
  }

  applyPreferences() {
    // Apply font size
    document.documentElement.style.setProperty(
      '--app-font-size', 
      this.getFontSizeValue(this.preferences.accessibility.fontSize)
    );

    // Apply high contrast
    if (this.preferences.accessibility.highContrast) {
      document.body.classList.add('high-contrast');
    } else {
      document.body.classList.remove('high-contrast');
    }

    // Apply reduced motion
    if (this.preferences.accessibility.reduceMotion) {
      document.body.classList.add('reduce-motion');
    } else {
      document.body.classList.remove('reduce-motion');
    }
  }

  getFontSizeValue(size: string): string {
    switch (size) {
      case 'small': return '14px';
      case 'large': return '18px';
      default: return '16px';
    }
  }

  setActiveTab(tab: string) {
    this.currentTab = tab;
  }

  exportSettings() {
    const dataStr = JSON.stringify(this.preferences, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'cbp-settings.json';
    link.click();
    URL.revokeObjectURL(url);
    
    this.toastService.showSuccess('Settings exported successfully');
  }

  importSettings(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const reader = new FileReader();
      
      reader.onload = (e) => {
        try {
          const imported = JSON.parse(e.target?.result as string);
          this.preferences = { ...this.preferences, ...imported };
          this.applyPreferences();
          this.toastService.showSuccess('Settings imported successfully');
        } catch (error) {
          this.toastService.showError('Invalid settings file');
        }
      };
      
      reader.readAsText(file);
    }
  }

  clearCache() {
    if (confirm('Are you sure you want to clear the application cache? This will refresh the page.')) {
      localStorage.clear();
      sessionStorage.clear();
      if ('caches' in window) {
        caches.keys().then(names => {
          names.forEach(name => caches.delete(name));
        });
      }
      window.location.reload();
    }
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}