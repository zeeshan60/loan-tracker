import { inject, Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

@Injectable({
  providedIn: 'root'
})
export class LoginGuard implements CanActivate {
  storageService = inject(StorageService);
  router = inject(Router);

  constructor() {}

  async canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Promise<boolean> {
    const apiKey = await this.storageService.get('api_key');

    if (apiKey) {
        this.router.navigate(['']); // Redirect logged-in users away from login
        return false;
    }
    return true;
  }
}
