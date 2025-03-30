import { inject, Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  storageService = inject(StorageService);

  constructor(private router: Router) {}

  async canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean> {
    const apiKey = await this.storageService.get('api_key');
    if (apiKey) {
      return true; // Allow access to the route
    }

    // Redirect to login if api_key is not found
    this.router.navigate(['/login']);
    return false;
  }
}
