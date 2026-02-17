import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { FeatureFlagsService } from '../services/feature-flags.service';
import { map, take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class FeatureFlagGuard implements CanActivate {

  constructor(
    private featureFlagsService: FeatureFlagsService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    
    const featureFlag = route.data['featureFlag'];
    if (!featureFlag) {
      // If no feature flag is specified on the route, allow access
      return true;
    }

    return this.featureFlagsService.flags$.pipe(
      take(1),
      map(flags => {
        let isEnabled = false;

        // Check in all relevant categories
        if (flags.referenceData.hasOwnProperty(featureFlag)) {
          isEnabled = flags.referenceData[featureFlag];
        } else if (flags.features.hasOwnProperty(featureFlag)) {
          isEnabled = flags.features[featureFlag];
        } else if (flags.experimental.hasOwnProperty(featureFlag)) {
          isEnabled = flags.experimental[featureFlag];
        } else if (flags.dashboard.hasOwnProperty(featureFlag)) {
          isEnabled = flags.dashboard[featureFlag];
        } else if (flags.admin.hasOwnProperty(featureFlag)) {
            isEnabled = flags.admin[featureFlag];
        }

        if (isEnabled) {
          return true;
        } else {
          // Redirect to a 'not found' or 'access denied' page if the feature is disabled
          return this.router.createUrlTree(['/dashboard']); // Or a dedicated 'access-denied' page
        }
      })
    );
  }
}
