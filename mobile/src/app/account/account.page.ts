import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonIcon,
   IonButtons,
} from '@ionic/angular/standalone';
import { Auth, signOut } from '@angular/fire/auth';
import { Router } from '@angular/router';

@Component({
  selector: 'app-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  public auth = inject(Auth);
  private router = inject(Router);
  constructor() {}

  signOut(): void {
    signOut(this.auth).then(() => {
      this.router.navigate(['login']);
    })
  }
}
