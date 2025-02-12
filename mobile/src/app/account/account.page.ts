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
import { AuthStore } from '../login/auth.store';

@Component({
  selector: 'app-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  readonly authStore = inject(AuthStore);
  constructor() {}
}
