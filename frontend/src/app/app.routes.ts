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
import { UserGuideComponent } from './components/user-guide/user-guide';
import { NotificationsComponent } from './components/notifications/notifications';
import { FeatureFlagsComponent } from './components/admin/feature-flags/feature-flags.component';
import { BulkImportWizardComponent } from './components/bulk-import/bulk-import-wizard.component';
import { AuthGuard } from './guards/auth.guard';
import { LogoutComponent } from './components/logout/logout';
import { FeatureFlagGuard } from './guards/feature-flag.guard';

export const routes: Routes = [
  { path: 'logout', component: LogoutComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'countries', component: CountriesComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'countries' } },
      { path: 'ports', component: PortsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'ports' } },
      { path: 'airports', component: AirportsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'airports' } },
      { path: 'change-requests', component: ChangeRequestsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'changeRequests' } },
      { path: 'activity-log', component: ActivityLogComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'activityLog' } },
      { path: 'import-export', component: ImportExportComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'import' } },
      { path: 'bulk-import', component: BulkImportWizardComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'import' } },
      { path: 'reports', component: ReportsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'analytics' } },
      { path: 'analytics', component: AnalyticsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'analytics' } },
      { path: 'settings', component: SettingsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'admin' } },
      { path: 'system-config', component: SystemConfigComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'admin' } },
      { path: 'feature-flags', component: FeatureFlagsComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'admin' } },
      { path: 'users', component: UsersComponent, canActivate: [FeatureFlagGuard], data: { featureFlag: 'admin' } },
      { path: 'user-guide', component: UserGuideComponent },
      { path: 'notifications', component: NotificationsComponent }
    ]
  }
];
