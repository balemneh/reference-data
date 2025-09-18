import { Routes } from '@angular/router';
import { LayoutComponent } from './components/layout/layout';
import { DashboardComponent } from './components/dashboard/dashboard';
import { CountriesComponent } from './components/countries/countries';
import { PortsComponent } from './components/ports/ports';
import { AirportsComponent } from './components/airports/airports';
import { ChangeRequestsComponent } from './components/change-requests/change-requests';
import { ActivityLogComponent } from './components/activity-log/activity-log';
import { ImportExportComponent } from './components/import-export/import-export';
import { ReportsComponent } from './components/reports/reports';
import { AnalyticsComponent } from './components/analytics/analytics';
import { SettingsComponent } from './components/settings/settings';
import { SystemConfigComponent } from './components/system-config/system-config';
import { UsersComponent } from './components/users/users';
import { HelpUserGuideComponent } from './components/help/user-guide';
import { HelpApiDocsComponent } from './components/help/api-docs';
import { HelpTutorialsComponent } from './components/help/tutorials';
import { NotificationsComponent } from './components/notifications/notifications';
import { FeatureFlagsComponent } from './components/admin/feature-flags/feature-flags.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'countries', component: CountriesComponent },
      { path: 'ports', component: PortsComponent },
      { path: 'airports', component: AirportsComponent },
      { path: 'change-requests', component: ChangeRequestsComponent },
      { path: 'activity-log', component: ActivityLogComponent },
      { path: 'import-export', component: ImportExportComponent },
      { path: 'reports', component: ReportsComponent },
      { path: 'analytics', component: AnalyticsComponent },
      { path: 'settings', component: SettingsComponent },
      { path: 'system-config', component: SystemConfigComponent },
      { path: 'feature-flags', component: FeatureFlagsComponent },
      { path: 'users', component: UsersComponent },
      { path: 'help/user-guide', component: HelpUserGuideComponent },
      { path: 'help/api-docs', component: HelpApiDocsComponent },
      { path: 'help/tutorials', component: HelpTutorialsComponent },
      { path: 'notifications', component: NotificationsComponent }
    ]
  }
];
