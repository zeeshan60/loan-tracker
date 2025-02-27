import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonButton,
  IonIcon,
  IonButtons,
  IonCard,
  IonCardContent,
  IonAvatar,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
  IonList,
  IonItem,
  IonLabel,
} from '@ionic/angular/standalone';
import { AuthStore } from '../login/auth.store';
import { HelperService } from '../helper.service';

type Profile = {
  displayName: string,
  email: string,
  photoURL: string,
  phoneNumber: string|null,
}

@Component({
  selector: 'app-account',
  templateUrl: 'account.page.html',
  styleUrls: ['account.page.scss'],
  standalone: true,
  imports: [IonHeader, IonToolbar, IonTitle, IonContent, IonButton, IonIcon, IonButtons, IonCard, IonCardContent, IonAvatar, IonCardHeader, IonCardTitle, IonCardSubtitle, IonList, IonItem, IonLabel],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountPage {
  readonly helperService = inject(HelperService);
  readonly authStore = inject(AuthStore);
  readonly user = signal<Profile | null>(null)
  constructor() {
    this.helperService.getUser().then((user) => {
      this.user.set({
        displayName: user?.displayName!,
        email: user?.email!,
        photoURL: user?.photoURL!,
        phoneNumber: user?.phoneNumber || null,
      });
    })
  }
}
